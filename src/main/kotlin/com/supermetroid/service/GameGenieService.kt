package com.supermetroid.service

import com.supermetroid.model.AppConfig
import com.supermetroid.storage.FileStorageService
import com.supermetroid.util.Logging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Service for managing Game Genie settings and functionality
 */
class GameGenieService(private val fileStorageService: FileStorageService) : Logging {
    
    private val _gameGenieEnabled = MutableStateFlow(false)
    val gameGenieEnabled: StateFlow<Boolean> = _gameGenieEnabled.asStateFlow()
    
    /**
     * Initialize the service and load settings
     */
    suspend fun initialize() {
        try {
            val config = fileStorageService.loadAppConfig()
            _gameGenieEnabled.value = config.gameGenieEnabled
            logger.info { "🎮 Game Genie service initialized: ${if (_gameGenieEnabled.value) "enabled" else "disabled"}" }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to initialize Game Genie service" }
            _gameGenieEnabled.value = false
        }
    }
    
    /**
     * Enable or disable Game Genie functionality
     */
    suspend fun setGameGenieEnabled(enabled: Boolean) {
        try {
            _gameGenieEnabled.value = enabled
            val config = fileStorageService.loadAppConfig()
            val updatedConfig = config.copy(gameGenieEnabled = enabled)
            fileStorageService.saveAppConfig(updatedConfig)
            logger.info { "🎮 Game Genie ${if (enabled) "enabled" else "disabled"}" }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to update Game Genie setting" }
        }
    }
}
