package com.supermetroid.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.supermetroid.storage.FileStorageService
import com.supermetroid.model.SplitDisplayMode
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * Service for managing split display mode configuration
 */
class SplitDisplayModeService(private val fileStorageService: FileStorageService) {
    private val logger = KotlinLogging.logger {}
    
    private val _currentDisplayMode = MutableStateFlow(SplitDisplayMode.BOTH)
    val currentDisplayMode: StateFlow<SplitDisplayMode> = _currentDisplayMode.asStateFlow()
    
    private val _showSplitIcons = MutableStateFlow(true)
    val showSplitIcons: StateFlow<Boolean> = _showSplitIcons.asStateFlow()
    
    private val _showSplitNames = MutableStateFlow(true)
    val showSplitNames: StateFlow<Boolean> = _showSplitNames.asStateFlow()

    /**
     * Initialize split display mode service and load saved settings from config
     */
    suspend fun initialize() {
        try {
            val config = fileStorageService.loadAppConfig()
            _showSplitIcons.value = config.showSplitIcons
            _showSplitNames.value = config.showSplitNames
            _currentDisplayMode.value = SplitDisplayMode.fromBooleans(config.showSplitIcons, config.showSplitNames)
            logger.info { "📋 Loaded split display mode: ${_currentDisplayMode.value.displayName}" }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to load split display mode from config, using default" }
        }
    }

    /**
     * Set split display mode and save to config
     */
    suspend fun setDisplayMode(mode: SplitDisplayMode) {
        _currentDisplayMode.value = mode
        _showSplitIcons.value = mode.showIcons
        _showSplitNames.value = mode.showNames
        
        // Save to config
        try {
            val currentConfig = fileStorageService.loadAppConfig()
            val updatedConfig = currentConfig.copy(
                showSplitIcons = mode.showIcons,
                showSplitNames = mode.showNames
            )
            fileStorageService.saveAppConfig(updatedConfig)
            logger.info { "💾 Saved split display mode: ${mode.displayName}" }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to save split display mode" }
        }
    }
}

