package com.supermetroid.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.supermetroid.storage.FileStorageService
import com.supermetroid.model.AppConfig
import com.supermetroid.util.Logging
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * Service for managing room name visibility configuration
 */
class RoomNameService(private val fileStorageService: FileStorageService) : Logging {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _showRoomName = MutableStateFlow(true)
    val showRoomName: StateFlow<Boolean> = _showRoomName.asStateFlow()

    /**
     * Initialize room name service and load saved preference from config
     */
    suspend fun initialize() {
        try {
            val config = fileStorageService.loadAppConfig()
            _showRoomName.value = config.showRoomName
            logger.info { "🏠 Loaded room name visibility: ${config.showRoomName}" }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to load room name preference from config, using default" }
        }
    }

    /**
     * Set room name visibility and save to config
     */
    fun setShowRoomName(show: Boolean) {
        _showRoomName.value = show

        // Save room name preference to config asynchronously
        scope.launch {
            try {
                val currentConfig = fileStorageService.loadAppConfig()
                val updatedConfig = currentConfig.copy(showRoomName = show)
                fileStorageService.saveAppConfig(updatedConfig)
                logger.debug { "💾 Saved room name preference: $show" }
            } catch (e: Exception) {
                logger.error(e) { "❌ Failed to save room name preference" }
            }
        }
    }

    /**
     * Get current room name visibility setting
     */
    fun getShowRoomName(): Boolean {
        return _showRoomName.value
    }
}
