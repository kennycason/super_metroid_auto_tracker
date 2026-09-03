package com.supermetroid.service

import com.supermetroid.autosplits.AutoSplitsEngine
import com.supermetroid.autosplits.SplitProfiles
import com.supermetroid.model.Split
import com.supermetroid.model.SplitImageAsset
import com.supermetroid.model.SplitProfile
import com.supermetroid.model.SplitProfilesConfig
import com.supermetroid.model.StoredSplitProfile
import com.supermetroid.storage.FileStorageService
import com.supermetroid.util.Logging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

sealed interface SplitProfileSaveResult {
    data class Success(
        val profile: SplitProfile,
        val versionChanged: Boolean
    ) : SplitProfileSaveResult

    data class Failure(val message: String) : SplitProfileSaveResult
}

sealed interface SplitProfileDeleteResult {
    data object Success : SplitProfileDeleteResult
    data class Failure(val message: String) : SplitProfileDeleteResult
}

/**
 * Owns the complete split-profile registry: immutable built-ins, persisted
 * built-in display-name overrides, active custom profiles, and archived custom
 * revisions used by historical runs.
 */
class SplitProfileService(
    private val fileStorageService: FileStorageService,
    private val autoSplitsEngine: AutoSplitsEngine,
    private val familyIdGenerator: () -> String = { "custom-${UUID.randomUUID()}" }
) : Logging {

    companion object {
        const val MAX_PROFILE_NAME_LENGTH = 80
        const val MAX_SPLIT_NAME_LENGTH = 80
    }

    private var onProfileChanged: (suspend (String) -> Unit)? = null
    private var onProfileDefinitionChanged: (suspend (SplitProfile) -> Unit)? = null
    private var storedConfig = SplitProfilesConfig()
    private var initialized = false

    private val _availableProfiles = MutableStateFlow(SplitProfiles.ALL_PROFILES)
    val availableProfiles: StateFlow<List<SplitProfile>> = _availableProfiles.asStateFlow()

    private val _currentProfile = MutableStateFlow(SplitProfiles.DEFAULT)
    val currentProfile: StateFlow<SplitProfile> = _currentProfile.asStateFlow()

    val splitCatalog: List<Split> = SplitProfiles.SPLIT_CATALOG

    // Profile changes can cause LSS reloads that resolve the profile again.
    private var settingProfile = false

    init {
        autoSplitsEngine.setOnProfileChangedCallback { profile ->
            updateProfileFromEngine(profile)
        }
        autoSplitsEngine.setProfileResolver { profileId ->
            findProfileById(profileId, includeArchived = true)
        }
    }

    fun setOnProfileChangedCallback(callback: suspend (String) -> Unit) {
        onProfileChanged = callback
    }

    fun setOnProfileDefinitionChangedCallback(callback: suspend (SplitProfile) -> Unit) {
        onProfileDefinitionChanged = callback
    }

    suspend fun initialize() {
        if (initialized) return
        try {
            storedConfig = fileStorageService.loadSplitProfilesConfig().let { config ->
                if (config.schemaVersion < SplitProfilesConfig.CURRENT_SCHEMA_VERSION) {
                    config.copy(schemaVersion = SplitProfilesConfig.CURRENT_SCHEMA_VERSION)
                } else {
                    config
                }
            }
            refreshAvailableProfiles()

            val appConfig = fileStorageService.loadAppConfig()
            val profile = findProfileById(appConfig.selectedProfileId, includeArchived = false)
                ?: SplitProfiles.DEFAULT
            _currentProfile.value = profile
            autoSplitsEngine.loadProfile(profile)

            if (profile.id != appConfig.selectedProfileId) {
                fileStorageService.saveAppConfig(appConfig.copy(selectedProfileId = profile.id))
            }
            logger.info { "Loaded split profile: ${profile.name}" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to load split profiles, using default" }
            storedConfig = SplitProfilesConfig()
            refreshAvailableProfiles()
            _currentProfile.value = SplitProfiles.DEFAULT
            autoSplitsEngine.loadProfile(SplitProfiles.DEFAULT)
        } finally {
            initialized = true
        }
    }

    fun isBuiltIn(profileId: String): Boolean = profileId in SplitProfiles.BY_ID

    fun findProfileById(profileId: String, includeArchived: Boolean = true): SplitProfile? {
        _availableProfiles.value.firstOrNull { it.id == profileId }?.let { return it }
        if (!includeArchived) return null
        return storedConfig.archivedProfiles
            .firstOrNull { it.id == profileId }
            ?.let { resolveStoredProfile(it, preferSnapshot = true) }
            ?: SplitProfiles.BY_ID[profileId]?.let(::resolveBuiltInProfile)
    }

    fun splitNameOverrides(profileId: String): Map<String, String> {
        if (isBuiltIn(profileId)) {
            return storedConfig.builtInSplitNameOverrides[profileId].orEmpty()
        }
        return (storedConfig.customProfiles + storedConfig.archivedProfiles)
            .firstOrNull { it.id == profileId }
            ?.splitNameOverrides
            .orEmpty()
    }

    fun splitImageOverrides(profileId: String): Map<String, SplitImageAsset> {
        if (isBuiltIn(profileId)) {
            return storedConfig.builtInSplitImageOverrides[profileId].orEmpty()
        }
        return (storedConfig.customProfiles + storedConfig.archivedProfiles)
            .firstOrNull { it.id == profileId }
            ?.splitImageOverrides
            .orEmpty()
    }

    fun resolveSplitImageFile(asset: SplitImageAsset, original: Boolean = false): java.io.File? =
        fileStorageService.resolveSplitProfileImage(
            if (original) asset.originalPath else asset.previewPath
        )

    fun validateProfileName(name: String, excludingProfileId: String? = null): String? {
        val normalized = normalizeProfileName(name)
        if (normalized.isBlank()) return "Profile name is required"
        if (normalized.length > MAX_PROFILE_NAME_LENGTH) {
            return "Profile name must be $MAX_PROFILE_NAME_LENGTH characters or fewer"
        }

        val excludedFamilyId = storedConfig.customProfiles
            .firstOrNull { it.id == excludingProfileId }
            ?.familyId
        val conflict = _availableProfiles.value.any { profile ->
            val sameEditedFamily = excludedFamilyId != null && profile.familyId == excludedFamilyId
            !sameEditedFamily && profile.name.trim().equals(normalized, ignoreCase = true)
        }
        return if (conflict) "A split profile named '$normalized' already exists" else null
    }

    suspend fun createCustomProfile(
        name: String,
        splitIds: List<String>,
        splitNameOverrides: Map<String, String>,
        splitImageSources: Map<String, String> = emptyMap()
    ): SplitProfileSaveResult {
        validateProfileName(name)?.let { return SplitProfileSaveResult.Failure(it) }
        validateSplitList(splitIds)?.let { return SplitProfileSaveResult.Failure(it) }
        validateOverrides(splitIds, splitNameOverrides)?.let { return SplitProfileSaveResult.Failure(it) }
        validateImageChanges(splitIds, splitImageSources, emptySet())?.let {
            return SplitProfileSaveResult.Failure(it)
        }

        val familyId = generateUniqueFamilyId()
        val imageOverrides = try {
            applyImageChanges(familyId, splitIds, emptyMap(), splitImageSources, emptySet())
        } catch (e: Exception) {
            return SplitProfileSaveResult.Failure(
                "Could not save split image: ${e.message ?: "unknown error"}"
            )
        }
        val stored = StoredSplitProfile(
            id = versionedId(familyId, 1),
            familyId = familyId,
            version = 1,
            name = normalizeProfileName(name),
            splitIds = splitIds,
            splitDefinitions = splitIds.mapNotNull(SplitProfiles.SPLIT_CATALOG_BY_ID::get),
            splitNameOverrides = normalizeOverrides(splitIds, splitNameOverrides),
            splitImageOverrides = imageOverrides,
            updatedAtEpochMs = System.currentTimeMillis()
        )

        return persistMutation(
            newConfig = storedConfig.copy(
                schemaVersion = SplitProfilesConfig.CURRENT_SCHEMA_VERSION,
                customProfiles = storedConfig.customProfiles + stored
            ),
            profileId = stored.id,
            versionChanged = true,
            selectAfterSave = true
        )
    }

    /**
     * Save an editor draft. A changed split list creates a new structural
     * version; profile-name and display-name-only changes retain timing history.
     */
    suspend fun saveProfile(
        profileId: String,
        name: String,
        splitIds: List<String>,
        splitNameOverrides: Map<String, String>,
        splitImageSources: Map<String, String> = emptyMap(),
        removedSplitImageIds: Set<String> = emptySet()
    ): SplitProfileSaveResult {
        val builtIn = SplitProfiles.BY_ID[profileId]
        if (builtIn != null) {
            if (splitIds != builtIn.splits.map { it.id }) {
                return SplitProfileSaveResult.Failure("Built-in split order cannot be changed")
            }
            validateOverrides(splitIds, splitNameOverrides)?.let {
                return SplitProfileSaveResult.Failure(it)
            }
            validateImageChanges(splitIds, splitImageSources, removedSplitImageIds)?.let {
                return SplitProfileSaveResult.Failure(it)
            }
            val imageOverrides = try {
                applyImageChanges(
                    profileId,
                    splitIds,
                    storedConfig.builtInSplitImageOverrides[profileId].orEmpty(),
                    splitImageSources,
                    removedSplitImageIds
                )
            } catch (e: Exception) {
                return SplitProfileSaveResult.Failure(
                    "Could not save split image: ${e.message ?: "unknown error"}"
                )
            }
            val normalizedOverrides = storedConfig.builtInSplitNameOverrides.toMutableMap().apply {
                val overrides = normalizeOverrides(
                    splitIds,
                    splitNameOverrides,
                    builtIn.splits.associate { it.id to it.name }
                )
                if (overrides.isEmpty()) remove(profileId) else put(profileId, overrides)
            }
            val normalizedImageOverrides = storedConfig.builtInSplitImageOverrides.toMutableMap().apply {
                if (imageOverrides.isEmpty()) remove(profileId) else put(profileId, imageOverrides)
            }
            return persistMutation(
                newConfig = storedConfig.copy(
                    schemaVersion = SplitProfilesConfig.CURRENT_SCHEMA_VERSION,
                    builtInSplitNameOverrides = normalizedOverrides,
                    builtInSplitImageOverrides = normalizedImageOverrides
                ),
                profileId = profileId,
                versionChanged = false,
                selectAfterSave = _currentProfile.value.id == profileId,
                notifyDefinitionChanged = true
            )
        }

        val existing = storedConfig.customProfiles.firstOrNull { it.id == profileId }
            ?: return SplitProfileSaveResult.Failure("Custom profile no longer exists")

        validateProfileName(name, excludingProfileId = profileId)?.let {
            return SplitProfileSaveResult.Failure(it)
        }
        validateSplitList(splitIds)?.let { return SplitProfileSaveResult.Failure(it) }
        validateOverrides(splitIds, splitNameOverrides)?.let {
            return SplitProfileSaveResult.Failure(it)
        }
        validateImageChanges(splitIds, splitImageSources, removedSplitImageIds)?.let {
            return SplitProfileSaveResult.Failure(it)
        }

        val structureChanged = existing.splitIds != splitIds
        val activeRun = autoSplitsEngine.splitsState.value.currentRun
        if (structureChanged && activeRun?.profileId == existing.id && activeRun.endTime == null) {
            return SplitProfileSaveResult.Failure(
                "Finish, reset, or discard the active run before changing this profile's split order"
            )
        }

        val imageOverrides = try {
            applyImageChanges(
                existing.familyId,
                splitIds,
                existing.splitImageOverrides,
                splitImageSources,
                removedSplitImageIds
            )
        } catch (e: Exception) {
            return SplitProfileSaveResult.Failure(
                "Could not save split image: ${e.message ?: "unknown error"}"
            )
        }

        val updated = if (structureChanged) {
            existing.copy(
                id = versionedId(existing.familyId, existing.version + 1),
                version = existing.version + 1,
                name = normalizeProfileName(name),
                splitIds = splitIds,
                splitDefinitions = splitIds.mapNotNull(SplitProfiles.SPLIT_CATALOG_BY_ID::get),
                splitNameOverrides = normalizeOverrides(splitIds, splitNameOverrides),
                splitImageOverrides = imageOverrides,
                updatedAtEpochMs = System.currentTimeMillis()
            )
        } else {
            existing.copy(
                name = normalizeProfileName(name),
                splitNameOverrides = normalizeOverrides(splitIds, splitNameOverrides),
                splitImageOverrides = imageOverrides,
                updatedAtEpochMs = System.currentTimeMillis()
            )
        }

        val newActive = storedConfig.customProfiles.map { if (it.id == existing.id) updated else it }
        val newArchived = if (structureChanged) {
            storedConfig.archivedProfiles.filterNot { it.id == existing.id } + existing
        } else {
            storedConfig.archivedProfiles
        }

        return persistMutation(
            newConfig = storedConfig.copy(
                schemaVersion = SplitProfilesConfig.CURRENT_SCHEMA_VERSION,
                customProfiles = newActive,
                archivedProfiles = newArchived
            ),
            profileId = updated.id,
            versionChanged = structureChanged,
            selectAfterSave = _currentProfile.value.id == existing.id,
            notifyDefinitionChanged = !structureChanged
        )
    }

    suspend fun deleteCustomProfile(profileId: String): SplitProfileDeleteResult {
        val existing = storedConfig.customProfiles.firstOrNull { it.id == profileId }
            ?: return SplitProfileDeleteResult.Failure("Only custom profiles can be deleted")

        val activeRun = autoSplitsEngine.splitsState.value.currentRun
        if (activeRun?.profileId == profileId && activeRun.endTime == null) {
            return SplitProfileDeleteResult.Failure(
                "Finish, reset, or discard the active run before deleting this profile"
            )
        }

        return try {
            // Ensure a source file exists, then require a successful backup before deletion.
            fileStorageService.saveSplitProfilesConfig(storedConfig)
            if (!fileStorageService.backupSplitProfilesConfig()) {
                return SplitProfileDeleteResult.Failure("Could not create the required profile backup")
            }

            storedConfig = storedConfig.copy(
                schemaVersion = SplitProfilesConfig.CURRENT_SCHEMA_VERSION,
                customProfiles = storedConfig.customProfiles.filterNot { it.id == profileId },
                archivedProfiles = storedConfig.archivedProfiles.filterNot { it.id == profileId } + existing
            )
            fileStorageService.saveSplitProfilesConfig(storedConfig)
            refreshAvailableProfiles()

            if (_currentProfile.value.id == profileId) {
                setProfile(resolveBuiltInProfile(SplitProfiles.DEFAULT))
            }
            logger.info { "Deleted custom split profile ${existing.name}; definition archived" }
            SplitProfileDeleteResult.Success
        } catch (e: Exception) {
            logger.error(e) { "Failed to delete custom split profile $profileId" }
            SplitProfileDeleteResult.Failure("Could not delete the profile: ${e.message ?: "unknown error"}")
        }
    }

    /**
     * Set the current profile and persist the selection.
     *
     * Returns false when switching would discard an unfinished run or when the
     * selection could not be persisted. Re-selecting the current profile is
     * always safe.
     */
    suspend fun setProfile(profile: SplitProfile, notifyFormatService: Boolean = true): Boolean {
        if (settingProfile) return false
        val activeRun = autoSplitsEngine.splitsState.value.currentRun
        if (
            profile.id != _currentProfile.value.id &&
            activeRun != null &&
            activeRun.endTime == null
        ) {
            logger.warn { "Blocked profile switch while run ${activeRun.id} is unfinished" }
            return false
        }
        settingProfile = true
        return try {
            _currentProfile.value = profile
            autoSplitsEngine.loadProfile(profile)

            val config = fileStorageService.loadAppConfig()
            fileStorageService.saveAppConfig(config.copy(selectedProfileId = profile.id))

            if (notifyFormatService) {
                onProfileChanged?.invoke(profile.id)
            }
            logger.info { "Set split profile: ${profile.name} (v${profile.version})" }
            true
        } catch (e: Exception) {
            logger.error(e) { "Failed to save split profile selection" }
            false
        } finally {
            settingProfile = false
        }
    }

    fun getCurrentProfileId(): String = _currentProfile.value.id

    private suspend fun updateProfileFromEngine(profile: SplitProfile) {
        try {
            _currentProfile.value = profile
            val config = fileStorageService.loadAppConfig()
            fileStorageService.saveAppConfig(config.copy(selectedProfileId = profile.id))
            logger.info { "Profile synced from engine: ${profile.name}" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to sync profile from engine" }
        }
    }

    private suspend fun persistMutation(
        newConfig: SplitProfilesConfig,
        profileId: String,
        versionChanged: Boolean,
        selectAfterSave: Boolean,
        notifyDefinitionChanged: Boolean = false
    ): SplitProfileSaveResult {
        return try {
            fileStorageService.saveSplitProfilesConfig(newConfig)
            storedConfig = newConfig
            refreshAvailableProfiles()
            val profile = findProfileById(profileId, includeArchived = false)
                ?: return SplitProfileSaveResult.Failure("Saved profile could not be resolved")

            if (selectAfterSave) {
                setProfile(profile, notifyFormatService = versionChanged)
            }
            if (notifyDefinitionChanged) {
                onProfileDefinitionChanged?.invoke(profile)
            }
            SplitProfileSaveResult.Success(profile, versionChanged)
        } catch (e: Exception) {
            logger.error(e) { "Failed to save split profile $profileId" }
            SplitProfileSaveResult.Failure("Could not save the profile: ${e.message ?: "unknown error"}")
        }
    }

    private fun refreshAvailableProfiles() {
        val builtIns = SplitProfiles.ALL_PROFILES.map(::resolveBuiltInProfile)
        val custom = storedConfig.customProfiles.mapNotNull { resolveStoredProfile(it) }
        _availableProfiles.value = builtIns + custom.sortedBy { it.name.lowercase() }

        val currentId = _currentProfile.value.id
        _availableProfiles.value.firstOrNull { it.id == currentId }?.let {
            _currentProfile.value = it
        }
    }

    private fun resolveBuiltInProfile(profile: SplitProfile): SplitProfile {
        val overrides = storedConfig.builtInSplitNameOverrides[profile.id].orEmpty()
        return profile.copy(
            splits = profile.splits.map { split ->
                overrides[split.id]?.let { split.copy(name = it) } ?: split
            },
            splitImageOverrides = storedConfig.builtInSplitImageOverrides[profile.id].orEmpty()
        )
    }

    private fun resolveStoredProfile(
        stored: StoredSplitProfile,
        preferSnapshot: Boolean = false
    ): SplitProfile? {
        val snapshotById = stored.splitDefinitions.associateBy { it.id }
        val splits = stored.splitIds.map { splitId ->
            val definition = if (preferSnapshot) {
                snapshotById[splitId] ?: SplitProfiles.SPLIT_CATALOG_BY_ID[splitId]
            } else {
                SplitProfiles.SPLIT_CATALOG_BY_ID[splitId] ?: snapshotById[splitId]
            } ?: return null
            stored.splitNameOverrides[splitId]?.let { definition.copy(name = it) } ?: definition
        }
        return SplitProfile(
            id = stored.id,
            name = stored.name,
            splits = splits,
            version = stored.version,
            familyId = stored.familyId,
            splitImageOverrides = stored.splitImageOverrides.filterKeys { it in stored.splitIds }
        )
    }

    private fun validateSplitList(splitIds: List<String>): String? {
        if (splitIds.isEmpty()) return "Select at least one split"
        if (splitIds.distinct().size != splitIds.size) return "A split can only appear once"
        val unknown = splitIds.firstOrNull { it !in SplitProfiles.SPLIT_CATALOG_BY_ID }
        if (unknown != null) return "Unknown split: $unknown"
        if (splitIds.lastOrNull() != "ship") return "Ship must be the final split"
        return null
    }

    private fun validateOverrides(
        splitIds: List<String>,
        overrides: Map<String, String>
    ): String? {
        val invalidId = overrides.keys.firstOrNull { it !in splitIds }
        if (invalidId != null) return "A name override refers to an unselected split: $invalidId"
        val tooLong = overrides.entries.firstOrNull { it.value.trim().length > MAX_SPLIT_NAME_LENGTH }
        if (tooLong != null) {
            return "Split names must be $MAX_SPLIT_NAME_LENGTH characters or fewer"
        }
        return null
    }

    private fun validateImageChanges(
        splitIds: List<String>,
        imageSources: Map<String, String>,
        removedImageIds: Set<String>
    ): String? {
        val invalidId = (imageSources.keys + removedImageIds).firstOrNull { it !in splitIds }
        return invalidId?.let { "An image override refers to an unselected split: $it" }
    }

    private suspend fun applyImageChanges(
        profileStorageKey: String,
        splitIds: List<String>,
        existing: Map<String, SplitImageAsset>,
        imageSources: Map<String, String>,
        removedImageIds: Set<String>
    ): Map<String, SplitImageAsset> {
        val images = existing
            .filterKeys { it in splitIds && it !in removedImageIds }
            .toMutableMap()
        imageSources.forEach { (splitId, sourcePath) ->
            images[splitId] = fileStorageService.saveSplitProfileImage(
                profileStorageKey = profileStorageKey,
                splitId = splitId,
                sourcePath = sourcePath
            )
        }
        return splitIds.mapNotNull { splitId -> images[splitId]?.let { splitId to it } }.toMap()
    }

    private fun normalizeOverrides(
        splitIds: List<String>,
        overrides: Map<String, String>,
        defaultNames: Map<String, String> = SplitProfiles.SPLIT_CATALOG_BY_ID
            .mapValues { it.value.name }
    ): Map<String, String> {
        return splitIds.mapNotNull { splitId ->
            val value = overrides[splitId]?.trim().orEmpty()
            val defaultName = defaultNames[splitId]
            if (value.isBlank() || value == defaultName) null else splitId to value
        }.toMap()
    }

    private fun normalizeProfileName(name: String): String =
        name.trim().replace(Regex("\\s+"), " ")

    private fun generateUniqueFamilyId(): String {
        var candidate: String
        do {
            candidate = familyIdGenerator().trim().ifBlank { "custom-${UUID.randomUUID()}" }
        } while ((storedConfig.customProfiles + storedConfig.archivedProfiles).any { it.familyId == candidate })
        return candidate
    }

    private fun versionedId(familyId: String, version: Int): String = "$familyId-v$version"
}
