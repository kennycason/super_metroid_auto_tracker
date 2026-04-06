package com.supermetroid.service

import com.supermetroid.autosplits.AutoSplitsEngine
import com.supermetroid.autosplits.SplitProfiles
import com.supermetroid.model.SplitProfile
import com.supermetroid.storage.FileStorageService
import com.supermetroid.util.Logging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * Service for managing the selected split profile
 * Persists the selection to app config and notifies AutoSplitsEngine of changes
 */
class SplitProfileService(
    private val fileStorageService: FileStorageService,
    private val autoSplitsEngine: AutoSplitsEngine
) : Logging {

    // Callback to notify SplitFormatService when profile changes
    // Set after construction to avoid circular dependency
    private var onProfileChanged: (suspend (String) -> Unit)? = null

    fun setOnProfileChangedCallback(callback: suspend (String) -> Unit) {
        onProfileChanged = callback
    }
    
    private val _currentProfile = MutableStateFlow(SplitProfiles.DEFAULT)
    val currentProfile: StateFlow<SplitProfile> = _currentProfile.asStateFlow()

    // Re-entrancy guard: setProfile triggers onProfileChanged which triggers onFormatChanged
    // which may call setProfile again. Prevent the recursive chain.
    private var settingProfile = false
    
    init {
        // Register callback for when AutoSplitsEngine changes the profile (e.g., loading a replay run)
        autoSplitsEngine.setOnProfileChangedCallback { profile ->
            updateProfileFromEngine(profile)
        }
    }
    
    /**
     * Initialize the service by loading the saved profile selection
     * Also notifies the AutoSplitsEngine to use the loaded profile
     */
    suspend fun initialize() {
        try {
            val config = fileStorageService.loadAppConfig()
            val savedProfileId = config.selectedProfileId
            val profile = SplitProfiles.getProfileById(savedProfileId)
            _currentProfile.value = profile
            
            // Notify AutoSplitsEngine of the profile
            autoSplitsEngine.loadProfile(profile)
            
            logger.info { "📋 Loaded split profile: ${profile.name}" }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to load split profile, using default" }
            _currentProfile.value = SplitProfiles.DEFAULT
            autoSplitsEngine.loadProfile(SplitProfiles.DEFAULT)
        }
    }
    
    /**
     * Called when AutoSplitsEngine changes the profile (e.g., loading a replay run)
     * Updates our state and persists, but does NOT call back to engine (already set there)
     */
    private suspend fun updateProfileFromEngine(profile: SplitProfile) {
        try {
            _currentProfile.value = profile
            
            // Persist to config
            val config = fileStorageService.loadAppConfig()
            fileStorageService.saveAppConfig(config.copy(selectedProfileId = profile.id))
            
            logger.info { "📋 Profile synced from engine: ${profile.name}" }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to sync profile from engine" }
        }
    }
    
    /**
     * Set the current profile and persist it.
     * Also notifies the AutoSplitsEngine to use the new profile.
     *
     * @param notifyFormatService If true, notify SplitFormatService to switch LSS for this profile.
     *   Set to false when the profile switch was triggered BY SplitFormatService (e.g., loading an
     *   LSS file that resolved to a different profile) to avoid re-loading a different LSS.
     */
    suspend fun setProfile(profile: SplitProfile, notifyFormatService: Boolean = true) {
        if (settingProfile) return
        settingProfile = true
        try {
            _currentProfile.value = profile

            // Notify AutoSplitsEngine of the profile change
            autoSplitsEngine.loadProfile(profile)

            // Persist to config
            val config = fileStorageService.loadAppConfig()
            fileStorageService.saveAppConfig(config.copy(selectedProfileId = profile.id))

            // Notify SplitFormatService to switch LSS file for this profile
            if (notifyFormatService) {
                onProfileChanged?.invoke(profile.id)
            }

            logger.info { "📋 Set split profile: ${profile.name}" }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to save split profile selection" }
        } finally {
            settingProfile = false
        }
    }
    
    /**
     * Get the current profile ID
     */
    fun getCurrentProfileId(): String = _currentProfile.value.id
}
