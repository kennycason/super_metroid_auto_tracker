package com.supermetroid.service

import com.supermetroid.model.AppConfig
import com.supermetroid.model.IconSize
import com.supermetroid.storage.FileStorageService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

@OptIn(ExperimentalCoroutinesApi::class)
class IconSizeServiceTest {

    private lateinit var tempDir: Path
    private lateinit var fileStorageService: FileStorageService
    private lateinit var iconSizeService: IconSizeService

    @BeforeEach
    fun setUp() {
        tempDir = Files.createTempDirectory("smtracker_test")
        fileStorageService = FileStorageService()
        iconSizeService = IconSizeService(fileStorageService)
        
        // Override the config directory for testing
        System.setProperty("user.home", tempDir.toString())
    }

    @Test
    fun `initialize should load saved icon size from config`() = runTest {
        // Given
        val savedConfig = AppConfig(iconSize = 48)
        fileStorageService.saveAppConfig(savedConfig)

        // When
        iconSizeService.initialize()

        // Then
        assertEquals(IconSize.LARGE, iconSizeService.currentIconSize.value)
    }

    @Test
    fun `initialize should use default icon size when no config exists`() = runTest {
        // Given - no config file exists

        // When
        iconSizeService.initialize()

        // Then
        assertEquals(IconSize.MEDIUM, iconSizeService.currentIconSize.value)
    }

    @Test
    fun `initialize should use default icon size when saved size is invalid`() = runTest {
        // Given
        val savedConfig = AppConfig(iconSize = 99) // Invalid size
        fileStorageService.saveAppConfig(savedConfig)

        // When
        iconSizeService.initialize()

        // Then
        assertEquals(IconSize.MEDIUM, iconSizeService.currentIconSize.value)
    }

    @Test
    fun `setIconSize should update current size`() = runTest {
        // Given
        val currentConfig = AppConfig(iconSize = 32)
        fileStorageService.saveAppConfig(currentConfig)
        iconSizeService.initialize()

        // When
        iconSizeService.setIconSize(IconSize.EXTRA_LARGE)

        // Then
        assertEquals(IconSize.EXTRA_LARGE, iconSizeService.currentIconSize.value)
    }

    @Test
    fun `getCurrentIconSizePixels should return correct pixel value`() = runTest {
        // Given
        val savedConfig = AppConfig(iconSize = 24)
        fileStorageService.saveAppConfig(savedConfig)
        iconSizeService.initialize()

        // When
        val pixelSize = iconSizeService.getCurrentIconSizePixels()

        // Then
        assertEquals(24, pixelSize)
    }
}

class IconSizeTest {

    @Test
    fun `IconSize enum should have correct values`() {
        assertEquals(16, IconSize.SMALL.size)
        assertEquals("16x16", IconSize.SMALL.displayName)
        
        assertEquals(24, IconSize.MEDIUM_SMALL.size)
        assertEquals("24x24", IconSize.MEDIUM_SMALL.displayName)
        
        assertEquals(32, IconSize.MEDIUM.size)
        assertEquals("32x32", IconSize.MEDIUM.displayName)
        
        assertEquals(48, IconSize.LARGE.size)
        assertEquals("48x48", IconSize.LARGE.displayName)
        
        assertEquals(64, IconSize.EXTRA_LARGE.size)
        assertEquals("64x64", IconSize.EXTRA_LARGE.displayName)
    }

    @Test
    fun `fromSize should return correct IconSize for valid sizes`() {
        assertEquals(IconSize.SMALL, IconSize.fromSize(16))
        assertEquals(IconSize.MEDIUM_SMALL, IconSize.fromSize(24))
        assertEquals(IconSize.MEDIUM, IconSize.fromSize(32))
        assertEquals(IconSize.LARGE, IconSize.fromSize(48))
        assertEquals(IconSize.EXTRA_LARGE, IconSize.fromSize(64))
    }

    @Test
    fun `fromSize should return MEDIUM for invalid sizes`() {
        assertEquals(IconSize.MEDIUM, IconSize.fromSize(0))
        assertEquals(IconSize.MEDIUM, IconSize.fromSize(99))
        assertEquals(IconSize.MEDIUM, IconSize.fromSize(-1))
    }
}
