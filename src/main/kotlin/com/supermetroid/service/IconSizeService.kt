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
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * Service for managing icon size configuration
 */
class IconSizeService(private val fileStorageService: FileStorageService) {
    private val logger = KotlinLogging.logger {}
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
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
    fun setIconSize(iconSize: IconSize) {
        _currentIconSize.value = iconSize
        
        // Save icon size to config asynchronously
        scope.launch {
            try {
                val currentConfig = fileStorageService.loadAppConfig()
                val updatedConfig = currentConfig.copy(iconSize = iconSize.size)
                fileStorageService.saveAppConfig(updatedConfig)
                logger.debug { "💾 Saved icon size preference: ${iconSize.displayName}" }
            } catch (e: Exception) {
                logger.error(e) { "❌ Failed to save icon size preference" }
            }
        }
    }
    
    /**
     * Get current icon size in pixels
     */
    fun getCurrentIconSizePixels(): Int {
        return _currentIconSize.value.size
    }
}

