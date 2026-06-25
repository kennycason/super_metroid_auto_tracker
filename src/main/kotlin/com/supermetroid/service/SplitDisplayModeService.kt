package com.supermetroid.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.supermetroid.storage.FileStorageService
import com.supermetroid.model.SplitDisplayMode
import com.supermetroid.util.Logging
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * Service for managing split display mode configuration
 */
class SplitDisplayModeService(private val fileStorageService: FileStorageService) : Logging {

    private val _currentDisplayMode = MutableStateFlow(SplitDisplayMode.BOTH)
    val currentDisplayMode: StateFlow<SplitDisplayMode> = _currentDisplayMode.asStateFlow()

    private val _showSplitIcons = MutableStateFlow(true)
    val showSplitIcons: StateFlow<Boolean> = _showSplitIcons.asStateFlow()

    private val _showSplitNames = MutableStateFlow(true)
    val showSplitNames: StateFlow<Boolean> = _showSplitNames.asStateFlow()

    private val _showGameName = MutableStateFlow(true)
    val showGameName: StateFlow<Boolean> = _showGameName.asStateFlow()

    private val _showSegmentDeltas = MutableStateFlow(false)
    val showSegmentDeltas: StateFlow<Boolean> = _showSegmentDeltas.asStateFlow()

    private val _showBestPossibleColumn = MutableStateFlow(true)
    val showBestPossibleColumn: StateFlow<Boolean> = _showBestPossibleColumn.asStateFlow()

    private val _showBestPossibleDelta = MutableStateFlow(true)
    val showBestPossibleDelta: StateFlow<Boolean> = _showBestPossibleDelta.asStateFlow()

    private val _showBestColumn = MutableStateFlow(true)
    val showBestColumn: StateFlow<Boolean> = _showBestColumn.asStateFlow()

    private val _showAverageColumn = MutableStateFlow(false)
    val showAverageColumn: StateFlow<Boolean> = _showAverageColumn.asStateFlow()

    private val _showAverageDelta = MutableStateFlow(false)
    val showAverageDelta: StateFlow<Boolean> = _showAverageDelta.asStateFlow()

    private val _showBestPossibleDeltaTotal = MutableStateFlow(false)
    val showBestPossibleDeltaTotal: StateFlow<Boolean> = _showBestPossibleDeltaTotal.asStateFlow()

    private val _showBestDelta = MutableStateFlow(false)
    val showBestDelta: StateFlow<Boolean> = _showBestDelta.asStateFlow()

    private val _showBestDeltaTotal = MutableStateFlow(false)
    val showBestDeltaTotal: StateFlow<Boolean> = _showBestDeltaTotal.asStateFlow()

    private val _showBestPossibleSummary = MutableStateFlow(true)
    val showBestPossibleSummary: StateFlow<Boolean> = _showBestPossibleSummary.asStateFlow()

    /**
     * Initialize split display mode service and load saved settings from config
     */
    suspend fun initialize() {
        try {
            val config = fileStorageService.loadAppConfig()
            _showSplitIcons.value = config.showSplitIcons
            _showSplitNames.value = config.showSplitNames
            _showGameName.value = config.showGameName
            _showSegmentDeltas.value = config.showSegmentDeltas
            _showBestPossibleColumn.value = config.showBestPossibleColumn
            _showBestPossibleDelta.value = config.showBestPossibleDelta
            _showBestColumn.value = config.showBestColumn
            _showAverageColumn.value = config.showAverageColumn
            _showAverageDelta.value = config.showAverageDelta
            _showBestPossibleDeltaTotal.value = config.showBestPossibleDeltaTotal
            _showBestDelta.value = config.showBestDelta
            _showBestDeltaTotal.value = config.showBestDeltaTotal
            _showBestPossibleSummary.value = config.showBestPossibleSummary
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

    suspend fun setShowGameName(show: Boolean) {
        _showGameName.value = show
        try {
            val currentConfig = fileStorageService.loadAppConfig()
            fileStorageService.saveAppConfig(currentConfig.copy(showGameName = show))
        } catch (e: Exception) {
            logger.error(e) { "Failed to save show game name setting" }
        }
    }

    /**
     * Set whether to show segment deltas and save to config
     */
    suspend fun setShowSegmentDeltas(show: Boolean) {
        _showSegmentDeltas.value = show

        // Save to config
        try {
            val currentConfig = fileStorageService.loadAppConfig()
            val updatedConfig = currentConfig.copy(showSegmentDeltas = show)
            fileStorageService.saveAppConfig(updatedConfig)
            logger.info { "💾 Saved show segment deltas: $show" }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to save show segment deltas setting" }
        }
    }

    /**
     * Set whether to show Best Possible column and save to config
     */
    suspend fun setShowBestPossibleColumn(show: Boolean) {
        _showBestPossibleColumn.value = show

        // Save to config
        try {
            val currentConfig = fileStorageService.loadAppConfig()
            val updatedConfig = currentConfig.copy(showBestPossibleColumn = show)
            fileStorageService.saveAppConfig(updatedConfig)
            logger.info { "💾 Saved show Best Possible column: $show" }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to save show Best Possible column setting" }
        }
    }

    /**
     * Set whether to show Best Possible Delta column and save to config
     */
    suspend fun setShowBestPossibleDelta(show: Boolean) {
        _showBestPossibleDelta.value = show

        // Save to config
        try {
            val currentConfig = fileStorageService.loadAppConfig()
            val updatedConfig = currentConfig.copy(showBestPossibleDelta = show)
            fileStorageService.saveAppConfig(updatedConfig)
            logger.info { "💾 Saved show Best Possible Delta column: $show" }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to save show Best Possible Delta column setting" }
        }
    }

    /**
     * Set whether to show BEST (Personal Best run) column and save to config
     */
    suspend fun setShowBestColumn(show: Boolean) {
        _showBestColumn.value = show

        // Save to config
        try {
            val currentConfig = fileStorageService.loadAppConfig()
            val updatedConfig = currentConfig.copy(showBestColumn = show)
            fileStorageService.saveAppConfig(updatedConfig)
            logger.info { "💾 Saved show BEST column: $show" }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to save show BEST column setting" }
        }
    }

    /**
     * Set whether to show Average Time column and save to config
     */
    suspend fun setShowAverageColumn(show: Boolean) {
        _showAverageColumn.value = show

        // Save to config
        try {
            val currentConfig = fileStorageService.loadAppConfig()
            val updatedConfig = currentConfig.copy(showAverageColumn = show)
            fileStorageService.saveAppConfig(updatedConfig)
            logger.info { "💾 Saved show Average column: $show" }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to save show Average column setting" }
        }
    }

    /**
     * Set whether to show Average Delta column and save to config
     */
    suspend fun setShowAverageDelta(show: Boolean) {
        _showAverageDelta.value = show

        // Save to config
        try {
            val currentConfig = fileStorageService.loadAppConfig()
            val updatedConfig = currentConfig.copy(showAverageDelta = show)
            fileStorageService.saveAppConfig(updatedConfig)
            logger.info { "💾 Saved show Average Delta column: $show" }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to save show Average Delta column setting" }
        }
    }

    suspend fun setShowBestPossibleDeltaTotal(show: Boolean) {
        _showBestPossibleDeltaTotal.value = show
        try {
            val currentConfig = fileStorageService.loadAppConfig()
            fileStorageService.saveAppConfig(currentConfig.copy(showBestPossibleDeltaTotal = show))
        } catch (e: Exception) {
            logger.error(e) { "Failed to save show BP +/- Total setting" }
        }
    }

    suspend fun setShowBestDelta(show: Boolean) {
        _showBestDelta.value = show
        try {
            val currentConfig = fileStorageService.loadAppConfig()
            fileStorageService.saveAppConfig(currentConfig.copy(showBestDelta = show))
        } catch (e: Exception) {
            logger.error(e) { "Failed to save show Best +/- setting" }
        }
    }

    suspend fun setShowBestDeltaTotal(show: Boolean) {
        _showBestDeltaTotal.value = show
        try {
            val currentConfig = fileStorageService.loadAppConfig()
            fileStorageService.saveAppConfig(currentConfig.copy(showBestDeltaTotal = show))
        } catch (e: Exception) {
            logger.error(e) { "Failed to save show Best +/- Total setting" }
        }
    }

    suspend fun setShowBestPossibleSummary(show: Boolean) {
        _showBestPossibleSummary.value = show
        try {
            val currentConfig = fileStorageService.loadAppConfig()
            fileStorageService.saveAppConfig(currentConfig.copy(showBestPossibleSummary = show))
        } catch (e: Exception) {
            logger.error(e) { "Failed to save show Best Possible summary setting" }
        }
    }
}
