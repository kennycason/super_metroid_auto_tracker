package com.supermetroid.service

import com.supermetroid.storage.FileStorageService
import com.supermetroid.util.Logging
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Service to manage UI visibility state (splits, icons, timer, settings)
 */
class UIVisibilityService(private val fileStorageService: FileStorageService) : Logging {

    private val _showSplits = MutableStateFlow(true)
    val showSplits: StateFlow<Boolean> = _showSplits.asStateFlow()

    private val _showIcons = MutableStateFlow(true)
    val showIcons: StateFlow<Boolean> = _showIcons.asStateFlow()

    private val _showTimer = MutableStateFlow(true)
    val showTimer: StateFlow<Boolean> = _showTimer.asStateFlow()

    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()

    private val _showMapRandoInfo = MutableStateFlow(false)
    val showMapRandoInfo: StateFlow<Boolean> = _showMapRandoInfo.asStateFlow()

    private val _showStats = MutableStateFlow(false)
    val showStats: StateFlow<Boolean> = _showStats.asStateFlow()

    suspend fun initialize() {
        try {
            val config = fileStorageService.loadAppConfig()
            _showSplits.value = config.showSplits
            _showIcons.value = config.showIcons
            _showTimer.value = config.showTimer
            _showSettings.value = config.showSettings
            _showMapRandoInfo.value = config.showMapRandoInfo
            _showStats.value = config.showStats
            logger.info { "📋 Loaded UI visibility state: splits=${_showSplits.value}, icons=${_showIcons.value}, timer=${_showTimer.value}, settings=${_showSettings.value}, mapRandoInfo=${_showMapRandoInfo.value}, stats=${_showStats.value}" }
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

    suspend fun setShowMapRandoInfo(show: Boolean) {
        _showMapRandoInfo.value = show
        saveToConfig()
        logger.info { "💾 Saved showMapRandoInfo: $show" }
    }

    suspend fun setShowStats(show: Boolean) {
        _showStats.value = show
        saveToConfig()
        logger.info { "💾 Saved showStats: $show" }
    }

    private suspend fun saveToConfig() {
        try {
            val cfg = fileStorageService.loadAppConfig()
            fileStorageService.saveAppConfig(cfg.copy(
                showSplits = _showSplits.value,
                showIcons = _showIcons.value,
                showTimer = _showTimer.value,
                showSettings = _showSettings.value,
                showMapRandoInfo = _showMapRandoInfo.value,
                showStats = _showStats.value
            ))
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to save UI visibility state" }
        }
    }
}
