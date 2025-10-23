package com.supermetroid.service

import com.supermetroid.model.AmmoNumberMode
import com.supermetroid.model.IconViewMode
import com.supermetroid.storage.FileStorageService
import com.supermetroid.util.Logging
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Service to manage icon view layout and ammo number display mode
 */
class IconViewModeService(private val fileStorageService: FileStorageService) : Logging {
    private val _iconViewMode = MutableStateFlow(IconViewMode.DEFAULT)
    val iconViewMode: StateFlow<IconViewMode> = _iconViewMode.asStateFlow()

    private val _ammoNumberMode = MutableStateFlow(AmmoNumberMode.AMOUNT)
    val ammoNumberMode: StateFlow<AmmoNumberMode> = _ammoNumberMode.asStateFlow()

    suspend fun initialize() {
        try {
            val config = fileStorageService.loadAppConfig()
            _iconViewMode.value = runCatching { IconViewMode.valueOf(config.iconViewMode) }.getOrDefault(IconViewMode.DEFAULT)
            _ammoNumberMode.value = runCatching { AmmoNumberMode.valueOf(config.ammoNumberMode) }.getOrDefault(AmmoNumberMode.AMOUNT)
            logger.info { "📋 Loaded icon view settings: ${_iconViewMode.value.displayName}, ${_ammoNumberMode.value.displayName}" }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to load icon view settings" }
        }
    }

    suspend fun setIconViewMode(mode: IconViewMode) {
        _iconViewMode.value = mode
        try {
            val cfg = fileStorageService.loadAppConfig()
            fileStorageService.saveAppConfig(cfg.copy(iconViewMode = mode.name))
            logger.info { "💾 Saved icon view mode: ${mode.displayName}" }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to save icon view mode" }
        }
    }

    suspend fun setAmmoNumberMode(mode: AmmoNumberMode) {
        _ammoNumberMode.value = mode
        try {
            val cfg = fileStorageService.loadAppConfig()
            fileStorageService.saveAppConfig(cfg.copy(ammoNumberMode = mode.name))
            logger.info { "💾 Saved ammo number mode: ${mode.displayName}" }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to save ammo number mode" }
        }
    }
}
