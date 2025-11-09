package com.supermetroid.service

import com.supermetroid.model.DefaultMapRandoInfoConfig
import com.supermetroid.model.MapRandoInfoConfig
import com.supermetroid.model.MapRandoInfoItem
import com.supermetroid.util.Logging
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Service to manage Map Rando info field visibility and ordering configuration
 */
class MapRandoInfoConfigService : Logging {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _config = MutableStateFlow(DefaultMapRandoInfoConfig.create())
    val config: StateFlow<MapRandoInfoConfig> = _config.asStateFlow()

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true  // Include default values (like visible = true)
    }

    suspend fun initialize() {
        try {
            val loadedConfig = loadConfig()
            _config.value = loadedConfig
            logger.info { "📋 Loaded Map Rando info config with ${_config.value.items.size} items" }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to load Map Rando info config, using defaults" }
            _config.value = DefaultMapRandoInfoConfig.create()
        }
    }

    fun setItemVisible(itemId: String, visible: Boolean) {
        val updatedItems = _config.value.items.map { item ->
            if (item.id == itemId) item.copy(visible = visible) else item
        }
        val updatedConfig = _config.value.copy(items = updatedItems)
        _config.value = updatedConfig
        
        // Save asynchronously
        scope.launch {
            saveConfig(updatedConfig)
        }
        
        logger.debug { "🔄 Map Rando info item $itemId ${if (visible) "shown" else "hidden"}" }
    }

    fun moveItem(fromIndex: Int, toIndex: Int) {
        val items = _config.value.items.toMutableList()
        
        if (fromIndex in items.indices && toIndex in items.indices) {
            val item = items.removeAt(fromIndex)
            items.add(toIndex, item)
            
            // Update order numbers
            val reorderedItems = items.mapIndexed { index, item ->
                item.copy(order = index + 1)
            }
            
            val updatedConfig = _config.value.copy(items = reorderedItems)
            _config.value = updatedConfig
            
            // Save asynchronously
            scope.launch {
                saveConfig(updatedConfig)
            }
            
            logger.debug { "🔄 Moved Map Rando info item from $fromIndex to $toIndex" }
        }
    }

    fun resetToDefaults() {
        val defaultConfig = DefaultMapRandoInfoConfig.create()
        _config.value = defaultConfig
        
        scope.launch {
            saveConfig(defaultConfig)
        }
        
        logger.info { "🔄 Reset Map Rando info config to defaults" }
    }

    /**
     * Get visible items sorted by order
     */
    fun getVisibleItems(): List<MapRandoInfoItem> {
        return _config.value.items
            .filter { it.visible }
            .sortedBy { it.order }
    }

    /**
     * Get all items sorted by order
     */
    fun getAllItems(): List<MapRandoInfoItem> {
        return _config.value.items.sortedBy { it.order }
    }

    private suspend fun loadConfig(): MapRandoInfoConfig {
        val configDir = File(System.getProperty("user.home"), ".smtracker")
        val configFile = File(configDir, "maprando-info-config.json")

        return if (configFile.exists()) {
            try {
                val jsonContent = configFile.readText()
                val loadedConfig = json.decodeFromString<MapRandoInfoConfig>(jsonContent)
                
                // Merge with default config to handle new items
                mergeWithDefault(loadedConfig)
            } catch (e: Exception) {
                logger.warn(e) { "⚠️ Failed to parse Map Rando info config, using default" }
                DefaultMapRandoInfoConfig.create()
            }
        } else {
            logger.info { "📋 No Map Rando info config found, creating default" }
            DefaultMapRandoInfoConfig.create()
        }
    }

    private suspend fun saveConfig(config: MapRandoInfoConfig) {
        try {
            val configDir = File(System.getProperty("user.home"), ".smtracker")
            if (!configDir.exists()) {
                configDir.mkdirs()
            }

            val configFile = File(configDir, "maprando-info-config.json")
            val jsonContent = json.encodeToString(config)
            configFile.writeText(jsonContent)

            logger.debug { "💾 Saved Map Rando info configuration" }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to save Map Rando info configuration" }
        }
    }

    private fun mergeWithDefault(loadedConfig: MapRandoInfoConfig): MapRandoInfoConfig {
        val defaultConfig = DefaultMapRandoInfoConfig.create()
        val loadedItemsMap = loadedConfig.items.associateBy { it.id }

        val mergedItems = defaultConfig.items.map { defaultItem ->
            loadedItemsMap[defaultItem.id] ?: defaultItem
        }

        return MapRandoInfoConfig(items = mergedItems)
    }
}

