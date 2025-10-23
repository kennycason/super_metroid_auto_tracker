package com.supermetroid.service

import com.supermetroid.model.IconConfig
import com.supermetroid.model.IconItem
import com.supermetroid.model.DefaultIconConfig
import com.supermetroid.storage.FileStorageService
import com.supermetroid.util.Logging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File

/**
 * Service for managing icon configuration (visibility and order)
 */
class IconConfigService(private val fileStorageService: FileStorageService) : Logging {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _iconConfig = MutableStateFlow(DefaultIconConfig.create())
    val iconConfig: StateFlow<IconConfig> = _iconConfig.asStateFlow()

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * Initialize service and load saved configuration
     */
    suspend fun initialize() {
        try {
            val config = loadIconConfig()
            _iconConfig.value = config
            logger.info { "📋 Loaded icon configuration with ${config.icons.size} icons" }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to load icon config, using default" }
            _iconConfig.value = DefaultIconConfig.create()
        }
    }

    /**
     * Update icon enabled state
     */
    fun setIconEnabled(iconId: String, enabled: Boolean) {
        val currentConfig = _iconConfig.value
        val updatedIcons = currentConfig.icons.map { icon ->
            if (icon.id == iconId) {
                icon.copy(enabled = enabled)
            } else {
                icon
            }
        }

        val updatedConfig = currentConfig.copy(icons = updatedIcons)
        _iconConfig.value = updatedConfig

        // Save asynchronously
        scope.launch {
            saveIconConfig(updatedConfig)
        }

        logger.debug { "🔄 Icon $iconId ${if (enabled) "enabled" else "disabled"}" }
    }

    /**
     * Reorder icons by moving an icon to a new position
     */
    fun moveIcon(fromIndex: Int, toIndex: Int) {
        val currentConfig = _iconConfig.value
        val icons = currentConfig.icons.toMutableList()

        if (fromIndex in icons.indices && toIndex in icons.indices) {
            val item = icons.removeAt(fromIndex)
            icons.add(toIndex, item)

            // Update order values
            val reorderedIcons = icons.mapIndexed { index, icon ->
                icon.copy(order = index)
            }

            val updatedConfig = currentConfig.copy(icons = reorderedIcons)
            _iconConfig.value = updatedConfig

            // Save asynchronously
            scope.launch {
                saveIconConfig(updatedConfig)
            }

            logger.debug { "🔄 Moved icon from position $fromIndex to $toIndex" }
        }
    }

    /**
     * Get enabled icons in order for display
     */
    fun getEnabledIcons(): List<IconItem> {
        return _iconConfig.value.icons
            .filter { it.enabled }
            .sortedBy { it.order }
    }

    /**
     * Get all icons in order for settings management
     */
    fun getAllIcons(): List<IconItem> {
        return _iconConfig.value.icons.sortedBy { it.order }
    }

    /**
     * Reset to default configuration
     */
    fun resetToDefault() {
        val defaultConfig = DefaultIconConfig.create()
        _iconConfig.value = defaultConfig

        scope.launch {
            saveIconConfig(defaultConfig)
        }

        logger.info { "🔄 Reset icon configuration to default" }
    }

    /**
     * Load icon configuration from file
     */
    private suspend fun loadIconConfig(): IconConfig {
        val configDir = File(System.getProperty("user.home"), ".smtracker")
        val configFile = File(configDir, "icon-config.json")

        return if (configFile.exists()) {
            try {
                val jsonContent = configFile.readText()
                val loadedConfig = json.decodeFromString<IconConfig>(jsonContent)

                // Merge with default config to handle new icons
                mergeWithDefault(loadedConfig)
            } catch (e: Exception) {
                logger.warn(e) { "⚠️ Failed to parse icon config, using default" }
                DefaultIconConfig.create()
            }
        } else {
            logger.info { "📋 No icon config found, creating default" }
            DefaultIconConfig.create()
        }
    }

    /**
     * Save icon configuration to file
     */
    private suspend fun saveIconConfig(config: IconConfig) {
        try {
            val configDir = File(System.getProperty("user.home"), ".smtracker")
            if (!configDir.exists()) {
                configDir.mkdirs()
            }

            val configFile = File(configDir, "icon-config.json")
            val jsonContent = json.encodeToString(config)
            configFile.writeText(jsonContent)

            logger.debug { "💾 Saved icon configuration" }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to save icon configuration" }
        }
    }

    /**
     * Merge loaded config with default to handle new icons
     */
    private fun mergeWithDefault(loadedConfig: IconConfig): IconConfig {
        val defaultConfig = DefaultIconConfig.create()
        val loadedIconsMap = loadedConfig.icons.associateBy { it.id }

        val mergedIcons = defaultConfig.icons.map { defaultIcon ->
            loadedIconsMap[defaultIcon.id] ?: defaultIcon
        }

        return IconConfig(icons = mergedIcons, version = loadedConfig.version)
    }
}
