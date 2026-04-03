package com.supermetroid.service

import com.supermetroid.model.StatsFontSize
import com.supermetroid.storage.FileStorageService
import com.supermetroid.util.Logging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StatsFontSizeService(
    private val fileStorageService: FileStorageService?,
    private val scope: CoroutineScope
) : Logging {
    private val _fontSize = MutableStateFlow(StatsFontSize.MEDIUM)
    val fontSize: StateFlow<StatsFontSize> = _fontSize.asStateFlow()

    suspend fun initialize() {
        try {
            fileStorageService?.let { storage ->
                val config = storage.loadAppConfig()
                val saved = config.statsFontSize
                if (saved != null) {
                    _fontSize.value = StatsFontSize.fromDisplayName(saved)
                    logger.info { "Loaded stats font size: ${_fontSize.value.displayName}" }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to load stats font size" }
        }
    }

    fun setFontSize(size: StatsFontSize) {
        _fontSize.value = size
        saveToConfig()
    }

    private fun saveToConfig() {
        fileStorageService?.let { storage ->
            scope.launch(Dispatchers.IO) {
                try {
                    val config = storage.loadAppConfig()
                    storage.saveAppConfig(config.copy(statsFontSize = _fontSize.value.displayName))
                } catch (e: Exception) {
                    logger.error(e) { "Failed to save stats font size" }
                }
            }
        }
    }
}
