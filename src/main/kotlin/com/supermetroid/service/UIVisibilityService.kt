package com.supermetroid.service

import com.supermetroid.storage.FileStorageService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Service to manage UI visibility state (splits, icons, timer, settings)
 */
class UIVisibilityService(private val fileStorageService: FileStorageService) {
    private val logger = KotlinLogging.logger {}

    private val _showSplits = MutableStateFlow(true)
    val showSplits: StateFlow<Boolean> = _showSplits.asStateFlow()

    private val _showIcons = MutableStateFlow(true)
    val showIcons: StateFlow<Boolean> = _showIcons.asStateFlow()

    private val _showTimer = MutableStateFlow(true)
    val showTimer: StateFlow<Boolean> = _showTimer.asStateFlow()

    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()

    suspend fun initialize() {
        try {
            val config = fileStorageService.loadAppConfig()
            _showSplits.value = config.showSplits
            _showIcons.value = config.showIcons
            _showTimer.value = config.showTimer
            _showSettings.value = config.showSettings
            logger.info { "📋 Loaded UI visibility state: splits=${_showSplits.value}, icons=${_showIcons.value}, timer=${_showTimer.value}, settings=${_showSettings.value}" }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to load UI visibility state" }
        }
    }

    suspend fun setShowSplits(show: Boolean) {
        _showSplits.value = show
        saveToConfig()
        logger.info { "💾 Saved showSplits: $show" }
    }

    suspend fun setShowIcons(show: Boolean) {
        _showIcons.value = show
        saveToConfig()
        logger.info { "💾 Saved showIcons: $show" }
    }

    suspend fun setShowTimer(show: Boolean) {
        _showTimer.value = show
        saveToConfig()
        logger.info { "💾 Saved showTimer: $show" }
    }

    suspend fun setShowSettings(show: Boolean) {
        _showSettings.value = show
        saveToConfig()
        logger.info { "💾 Saved showSettings: $show" }
    }

    private suspend fun saveToConfig() {
        try {
            val cfg = fileStorageService.loadAppConfig()
            fileStorageService.saveAppConfig(cfg.copy(
                showSplits = _showSplits.value,
                showIcons = _showIcons.value,
                showTimer = _showTimer.value,
                showSettings = _showSettings.value
            ))
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to save UI visibility state" }
        }
    }
}
