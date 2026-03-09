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
import com.supermetroid.model.IconSize
import com.supermetroid.util.Logging
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * Service for managing icon size configuration
 */
class IconSizeService(private val fileStorageService: FileStorageService) : Logging {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _currentIconSize = MutableStateFlow(IconSize.MEDIUM)
    val currentIconSize: StateFlow<IconSize> = _currentIconSize.asStateFlow()

    /**
     * Initialize icon size service and load saved size from config
     */
    suspend fun initialize() {
        try {
            val config = fileStorageService.loadAppConfig()
            val savedIconSize = IconSize.fromSize(config.iconSize)
            _currentIconSize.value = savedIconSize
            logger.info { "📏 Loaded icon size: ${savedIconSize.displayName}" }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to load icon size from config, using default" }
        }
    }

    /**
     * Set icon size and save to config
     */
    suspend fun setIconSize(iconSize: IconSize) {
        _currentIconSize.value = iconSize

        // Save icon size to config synchronously
        try {
            val currentConfig = fileStorageService.loadAppConfig()
            val updatedConfig = currentConfig.copy(iconSize = iconSize.size)
            fileStorageService.saveAppConfig(updatedConfig)
            logger.info { "💾 Saved icon size preference: ${iconSize.displayName}" }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to save icon size preference" }
        }
    }

    /**
     * Get current icon size in pixels
     */
    fun getCurrentIconSizePixels(): Int {
        return _currentIconSize.value.size
    }
}
