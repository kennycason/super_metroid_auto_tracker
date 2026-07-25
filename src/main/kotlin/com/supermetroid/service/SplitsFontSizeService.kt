package com.supermetroid.service

import com.supermetroid.model.SplitsFontSize
import com.supermetroid.storage.FileStorageService
import com.supermetroid.util.Logging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SplitsFontSizeService(
    private val fileStorageService: FileStorageService?,
    private val scope: CoroutineScope
) : Logging {
    private val _fontSize = MutableStateFlow(SplitsFontSize.MEDIUM)
    val fontSize: StateFlow<SplitsFontSize> = _fontSize.asStateFlow()

    suspend fun initialize() {
        try {
            fileStorageService?.let { storage ->
                val config = storage.loadAppConfig()
                val saved = config.splitsFontSize
                if (saved != null) {
                    _fontSize.value = SplitsFontSize.fromDisplayName(saved)
                    logger.info { "Loaded splits font size: ${_fontSize.value.displayName}" }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to load splits font size" }
        }
    }

    fun setFontSize(size: SplitsFontSize) {
        _fontSize.value = size
        saveToConfig()
    }

    private fun saveToConfig() {
        fileStorageService?.let { storage ->
            scope.launch(Dispatchers.IO) {
                try {
                    val config = storage.loadAppConfig()
                    storage.saveAppConfig(config.copy(splitsFontSize = _fontSize.value.displayName))
                } catch (e: Exception) {
                    logger.error(e) { "Failed to save splits font size" }
                }
            }
        }
    }
}
