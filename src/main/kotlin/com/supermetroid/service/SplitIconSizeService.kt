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
 * Service for managing split icon size configuration
 */
class SplitIconSizeService(private val fileStorageService: FileStorageService) : Logging {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _currentSplitIconSize = MutableStateFlow(IconSize.MEDIUM)
    val currentSplitIconSize: StateFlow<IconSize> = _currentSplitIconSize.asStateFlow()

    /**
     * Initialize split icon size service and load saved size from config
     */
    suspend fun initialize() {
        try {
            val config = fileStorageService.loadAppConfig()
            val savedSplitIconSize = IconSize.fromSize(config.splitIconSize)
            _currentSplitIconSize.value = savedSplitIconSize
            logger.info { "📏 Loaded split icon size: ${savedSplitIconSize.displayName}" }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to load split icon size from config, using default" }
        }
    }

    /**
     * Set split icon size and save to config
     */
    suspend fun setSplitIconSize(iconSize: IconSize) {
        _currentSplitIconSize.value = iconSize

        // Save split icon size to config synchronously
        try {
            val currentConfig = fileStorageService.loadAppConfig()
            val updatedConfig = currentConfig.copy(splitIconSize = iconSize.size)
            fileStorageService.saveAppConfig(updatedConfig)
            logger.info { "💾 Saved split icon size preference: ${iconSize.displayName}" }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to save split icon size preference" }
        }
    }

    /**
     * Get current split icon size in pixels
     */
    fun getCurrentSplitIconSizePixels(): Int {
        return _currentSplitIconSize.value.size
    }
}
