package com.supermetroid.service

import com.supermetroid.model.MapRandoInfoFontSize
import com.supermetroid.storage.FileStorageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class MapRandoInfoFontSizeService(
    private val fileStorageService: FileStorageService?,
    private val scope: CoroutineScope
) {
    private val _fontSize = MutableStateFlow(MapRandoInfoFontSize.MEDIUM)
    val fontSize: StateFlow<MapRandoInfoFontSize> = _fontSize.asStateFlow()

    suspend fun initialize() {
        try {
            fileStorageService?.let { storage ->
                val config = storage.loadAppConfig()
                val savedSize = config.mapRandoInfoFontSize
                if (savedSize != null) {
                    _fontSize.value = MapRandoInfoFontSize.fromDisplayName(savedSize)
                    logger.info { "📏 Loaded Map Rando info font size: ${_fontSize.value.displayName}" }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to load Map Rando info font size" }
        }
    }

    fun setFontSize(size: MapRandoInfoFontSize) {
        _fontSize.value = size
        saveToConfig()
        logger.info { "📏 Map Rando info font size changed to: ${size.displayName}" }
    }

    private fun saveToConfig() {
        fileStorageService?.let { storage ->
            scope.launch(Dispatchers.IO) {
                try {
                    val config = storage.loadAppConfig()
                    storage.saveAppConfig(
                        config.copy(
                            mapRandoInfoFontSize = _fontSize.value.displayName
                        )
                    )
                    logger.debug { "💾 Saved Map Rando info font size to config" }
                } catch (e: Exception) {
                    logger.error(e) { "❌ Failed to save Map Rando info font size to config" }
                }
            }
        }
    }
}

