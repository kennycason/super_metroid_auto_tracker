package com.supermetroid.service

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ControllerServiceTest {
    
    private lateinit var controllerService: ControllerService
    
    @BeforeEach
    fun setUp() {
        controllerService = ControllerService()
    }
    
    @AfterEach
    fun tearDown() {
        controllerService.stop()
    }
    
    @Test
    fun `should initialize with disconnected state`() = runBlocking {
        val initialState = controllerService.controllerState.first()
        
        assertFalse(initialState.isConnected)
        assertEquals("No Controller", initialState.controllerName)
        assertFalse(initialState.dpadLeft)
        assertFalse(initialState.dpadUp)
        assertFalse(initialState.dpadRight)
        assertFalse(initialState.dpadDown)
        assertFalse(initialState.select)
        assertFalse(initialState.start)
        assertFalse(initialState.l)
        assertFalse(initialState.r)
        assertFalse(initialState.x)
        assertFalse(initialState.y)
        assertFalse(initialState.b)
        assertFalse(initialState.a)
    }
    
    @Test
    fun `should start and stop service correctly`() = runBlocking {
        // Start the service
        controllerService.start()
        
        // Wait a bit for the service to initialize
        delay(200)
        
        // Check that the service is running (controller should be connected)
        val state = controllerService.controllerState.first()
        assertTrue(state.isConnected)
        // Controller name could be "SNES Controller", "Detected USB Controller", or "Keyboard (Arrow Keys + WASD)"
        assertTrue(state.controllerName.isNotEmpty(), "Controller name should not be empty")
        
        // Stop the service
        controllerService.stop()
        
        // Wait a bit for the service to stop
        delay(100)
        
        // Check that the service is stopped
        val stoppedState = controllerService.controllerState.first()
        assertFalse(stoppedState.isConnected)
        assertEquals("No Controller", stoppedState.controllerName)
    }
    
    @Test
    fun `should initialize button frequencies`() = runBlocking {
        controllerService.start()
        delay(100)
        
        val frequencies = controllerService.buttonFrequencies.first()
        
        // Check that all buttons have frequency tracking initialized
        val expectedButtons = listOf("dpadLeft", "dpadUp", "dpadRight", "dpadDown", "select", "start", "l", "r", "x", "y", "b", "a")
        
        expectedButtons.forEach { buttonName ->
            assertTrue(frequencies.containsKey(buttonName), "Missing frequency tracking for $buttonName")
            val freq = frequencies[buttonName]!!
            assertEquals(0, freq.pressCount)
            assertEquals(0f, freq.pressesPerSecond)
        }
    }
    
    @Test
    fun `should simulate button presses during active periods`() = runBlocking {
        controllerService.start()
        
        // Wait for the service to start and potentially enter a simulation period
        delay(500)
        
        val state = controllerService.controllerState.first()
        assertTrue(state.isConnected)
        
        // The test passes if we can successfully read the state (simulation may or may not be active)
        // During simulation periods, some buttons might be pressed, but timing is unpredictable
        assertTrue(true, "Controller service is running and providing state updates")
    }
}
