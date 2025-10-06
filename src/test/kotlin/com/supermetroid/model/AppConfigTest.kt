package com.supermetroid.model

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class AppConfigTest {

    private val json = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    @Test
    fun `AppConfig should have default icon size of 32`() {
        val config = AppConfig()
        assertEquals(32, config.iconSize)
    }

    @Test
    fun `AppConfig should serialize and deserialize icon size correctly`() {
        // Given
        val originalConfig = AppConfig(iconSize = 48)

        // When
        val jsonString = json.encodeToString(AppConfig.serializer(), originalConfig)
        val deserializedConfig = json.decodeFromString(AppConfig.serializer(), jsonString)

        // Then
        assertEquals(originalConfig.iconSize, deserializedConfig.iconSize)
        assertEquals(48, deserializedConfig.iconSize)
    }

    @Test
    fun `AppConfig should handle missing icon size field with default value`() {
        // Given - JSON without iconSize field (simulating old config files)
        val jsonString = """
        {
            "theme": "RETRO_GREEN",
            "retroarchPort": 55355,
            "pollIntervalMs": 500,
            "windowWidth": 800,
            "windowHeight": 600,
            "autoSplitsEnabled": false
        }
        """.trimIndent()

        // When
        val config = json.decodeFromString(AppConfig.serializer(), jsonString)

        // Then
        assertEquals(32, config.iconSize) // Should use default value
        assertEquals("RETRO_GREEN", config.theme)
    }

    @Test
    fun `AppConfig should preserve all fields when copying with new icon size`() {
        // Given
        val originalConfig = AppConfig(
            theme = "NEON_BLUE",
            iconSize = 32,
            retroarchPort = 12345,
            pollIntervalMs = 1000,
            windowWidth = 1200,
            windowHeight = 800,
            autoSplitsEnabled = true
        )

        // When
        val updatedConfig = originalConfig.copy(iconSize = 64)

        // Then
        assertEquals(64, updatedConfig.iconSize)
        assertEquals("NEON_BLUE", updatedConfig.theme)
        assertEquals(12345, updatedConfig.retroarchPort)
        assertEquals(1000, updatedConfig.pollIntervalMs)
        assertEquals(1200, updatedConfig.windowWidth)
        assertEquals(800, updatedConfig.windowHeight)
        assertEquals(true, updatedConfig.autoSplitsEnabled)
    }
}

