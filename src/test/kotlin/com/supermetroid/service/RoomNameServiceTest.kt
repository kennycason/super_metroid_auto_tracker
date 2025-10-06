package com.supermetroid.service

import com.supermetroid.model.AppConfig
import com.supermetroid.storage.FileStorageService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.nio.file.Files
import java.nio.file.Path

@OptIn(ExperimentalCoroutinesApi::class)
class RoomNameServiceTest {

    private lateinit var tempDir: Path
    private lateinit var fileStorageService: FileStorageService
    private lateinit var roomNameService: RoomNameService

    @BeforeEach
    fun setUp() {
        tempDir = Files.createTempDirectory("smtracker_test")
        fileStorageService = FileStorageService(tempDir.toString())
        roomNameService = RoomNameService(fileStorageService)
    }

    @Test
    fun `initialize should load saved room name preference from config`() = runTest {
        // Given
        val savedConfig = AppConfig(showRoomName = false)
        fileStorageService.saveAppConfig(savedConfig)

        // When
        roomNameService.initialize()

        // Then
        assertEquals(false, roomNameService.showRoomName.value)
    }

    @Test
    fun `initialize should use default room name preference when no config exists`() = runTest {
        // Given - no config file exists

        // When
        roomNameService.initialize()

        // Then
        assertEquals(true, roomNameService.showRoomName.value)
    }

    @Test
    fun `setShowRoomName should update current preference`() = runTest {
        // Given
        val currentConfig = AppConfig(showRoomName = true)
        fileStorageService.saveAppConfig(currentConfig)
        roomNameService.initialize()

        // When
        roomNameService.setShowRoomName(false)

        // Then
        assertEquals(false, roomNameService.showRoomName.value)
    }

    @Test
    fun `setShowRoomName should update current preference immediately`() = runTest {
        // Given
        val currentConfig = AppConfig(showRoomName = true)
        fileStorageService.saveAppConfig(currentConfig)
        roomNameService.initialize()

        // When
        roomNameService.setShowRoomName(false)

        // Then
        assertEquals(false, roomNameService.showRoomName.value)
    }

    @Test
    fun `getShowRoomName should return current preference`() = runTest {
        // Given
        val savedConfig = AppConfig(showRoomName = false)
        fileStorageService.saveAppConfig(savedConfig)
        roomNameService.initialize()

        // When
        val showRoomName = roomNameService.getShowRoomName()

        // Then
        assertEquals(false, showRoomName)
    }

    @Test
    fun `service should handle multiple preference changes`() = runTest {
        // Given
        val currentConfig = AppConfig(showRoomName = true)
        fileStorageService.saveAppConfig(currentConfig)
        roomNameService.initialize()

        // When
        roomNameService.setShowRoomName(false)
        assertEquals(false, roomNameService.showRoomName.value)
        
        roomNameService.setShowRoomName(true)
        assertEquals(true, roomNameService.showRoomName.value)
        
        roomNameService.setShowRoomName(false)
        assertEquals(false, roomNameService.showRoomName.value)

        // Then
        assertEquals(false, roomNameService.showRoomName.value)
    }
}
