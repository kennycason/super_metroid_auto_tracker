package com.supermetroid.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.awt.Component
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.Timer
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}

/**
 * Controller button states for SNES-style layout
 */
data class ControllerState(
    val dpadLeft: Boolean = false,
    val dpadUp: Boolean = false,
    val dpadRight: Boolean = false,
    val dpadDown: Boolean = false,
    val select: Boolean = false,
    val start: Boolean = false,
    val l: Boolean = false,
    val r: Boolean = false,
    val x: Boolean = false,
    val y: Boolean = false,
    val b: Boolean = false,
    val a: Boolean = false,
    val isConnected: Boolean = false,
    val controllerName: String = "No Controller"
)

/**
 * Button press frequency tracking for glow effects
 */
data class ButtonFrequency(
    val pressCount: Int = 0,
    val lastPressTime: Long = 0L,
    val pressesPerSecond: Float = 0f
)

/**
 * Controller configuration for different gamepad types
 */
data class ControllerConfig(
    val type: String,
    val name: String,
    val buttonMapping: Map<String, List<Int>>,
    val dpadMapping: DpadMapping
)

data class DpadMapping(
    val useAxes: Boolean = true,
    val axes: AxisMapping? = null,
    val buttons: ButtonMapping? = null
)

data class AxisMapping(
    val horizontal: Int,
    val vertical: Int,
    val leftThreshold: Float = -0.5f,
    val rightThreshold: Float = 0.5f,
    val upThreshold: Float = -0.5f,
    val downThreshold: Float = 0.5f
)

data class ButtonMapping(
    val up: Int,
    val down: Int,
    val left: Int,
    val right: Int
)

/**
 * Service for handling gamepad input and tracking button states
 */
class ControllerService {
    private val _controllerState = MutableStateFlow(ControllerState())
    val controllerState: StateFlow<ControllerState> = _controllerState.asStateFlow()
    
    private val _buttonFrequencies = MutableStateFlow(
        mapOf<String, ButtonFrequency>()
    )
    val buttonFrequencies: StateFlow<Map<String, ButtonFrequency>> = _buttonFrequencies.asStateFlow()
    
    private var pollingJob: Job? = null
    private var updateTimer: Timer? = null
    private val buttonNames = listOf("dpadLeft", "dpadUp", "dpadRight", "dpadDown", "select", "start", "l", "r", "x", "y", "b", "a")
    
    // Gamepad detection state
    private var controllerConfig: ControllerConfig? = null
    private var useSimulation = true
    private var keyboardFallback = false
    
    // Keyboard state tracking for fallback
    private val keyStates = mutableMapOf<Int, Boolean>()
    
    /**
     * Start the controller service
     */
    fun start() {
        if (pollingJob?.isActive == true) {
            logger.warn { "Controller service already running" }
            return
        }
        
        logger.info { "🎮 Starting controller service..." }
        
        // Initialize button frequencies
        val initialFrequencies = buttonNames.associateWith { ButtonFrequency() }
        _buttonFrequencies.value = initialFrequencies
        
        // Start polling for gamepad input
        pollingJob = CoroutineScope(Dispatchers.IO).launch {
            pollGamepadInput()
        }
        
        // Start frequency decay timer (runs on Swing thread)
        updateTimer = Timer(100) { // Update every 100ms
            updateButtonFrequencies()
        }
        updateTimer?.start()
        
        logger.info { "✅ Controller service started" }
    }
    
    /**
     * Stop the controller service
     */
    fun stop() {
        logger.info { "🛑 Stopping controller service..." }
        
        pollingJob?.cancel()
        pollingJob = null
        
        updateTimer?.stop()
        updateTimer = null
        
        _controllerState.value = ControllerState()
        _buttonFrequencies.value = buttonNames.associateWith { ButtonFrequency() }
        
        logger.info { "✅ Controller service stopped" }
    }
    
    /**
     * Main gamepad polling loop
     */
    private suspend fun pollGamepadInput() {
        while (currentCoroutineContext().isActive) {
            try {
                // Detect and configure controllers
                detectControllers()
                
                // Read current gamepad state
                val newState = readGamepadState()
                
                // Update button frequencies for pressed buttons
                updatePressFrequencies(newState)
                
                // Update the controller state
                _controllerState.value = newState
                
                delay(16) // ~60 FPS polling rate
                
            } catch (e: Exception) {
                logger.error(e) { "Error polling gamepad input" }
                delay(1000) // Wait longer on error
            }
        }
    }
    
    /**
     * Detect available controllers and configure them
     */
    private fun detectControllers() {
        try {
            if (controllerConfig == null) {
                // Try to detect real gamepad first
                val realGamepad = detectRealGamepad()
                
                if (realGamepad != null) {
                    controllerConfig = realGamepad
                    useSimulation = false
                    logger.info { "🎮 Detected real controller: ${controllerConfig?.name}" }
                } else {
                    // Check if we should enable keyboard fallback
                    if (shouldEnableKeyboardFallback()) {
                        controllerConfig = getKeyboardFallbackConfig()
                        useSimulation = false
                        keyboardFallback = true
                        logger.info { "⌨️ Using keyboard fallback for controller input" }
                    } else {
                        // Fall back to simulation
                        controllerConfig = getSNESControllerConfig()
                        useSimulation = true
                        logger.info { "🎮 Using simulated controller: ${controllerConfig?.name}" }
                    }
                }
            }
            
        } catch (e: Exception) {
            logger.error(e) { "Error detecting controllers" }
        }
    }
    
    /**
     * Try to detect real gamepad devices
     */
    private fun detectRealGamepad(): ControllerConfig? {
        try {
            // Check for actual gamepad device files (not just system directories)
            val gamepadPaths = when {
                System.getProperty("os.name").lowercase().contains("linux") -> {
                    // Check for actual joystick devices
                    listOf("/dev/input/js0", "/dev/input/js1", "/dev/input/js2", "/dev/input/js3")
                }
                System.getProperty("os.name").lowercase().contains("mac") -> {
                    // Check for HID gamepad devices (more specific than system directories)
                    listOf("/dev/input/js0", "/dev/input/js1") // These don't typically exist on macOS without special drivers
                }
                System.getProperty("os.name").lowercase().contains("windows") -> {
                    // Windows gamepad detection would need different approach (registry/DirectInput)
                    emptyList() // For now, don't detect on Windows
                }
                else -> emptyList()
            }
            
            // Check if any actual gamepad device files exist
            for (path in gamepadPaths) {
                if (Files.exists(Paths.get(path))) {
                    logger.info { "🎮 Found real gamepad device: $path" }
                    return getSNESControllerConfig().copy(name = "Detected USB Controller")
                }
            }
            
            logger.debug { "No real gamepad devices found" }
            
        } catch (e: Exception) {
            logger.debug { "Could not detect real gamepad: ${e.message}" }
        }
        
        return null
    }
    
    /**
     * Check if keyboard fallback should be enabled
     */
    private fun shouldEnableKeyboardFallback(): Boolean {
        // Enable keyboard fallback if no real gamepad detected and user wants it
        // For now, we'll enable it automatically for testing
        return true
    }
    
    /**
     * Get keyboard fallback configuration
     */
    private fun getKeyboardFallbackConfig(): ControllerConfig {
        return ControllerConfig(
            type = "keyboard",
            name = "Keyboard (Arrow Keys + WASD)",
            buttonMapping = mapOf(
                "select" to listOf(java.awt.event.KeyEvent.VK_BACK_SPACE),
                "start" to listOf(java.awt.event.KeyEvent.VK_ENTER),
                "l" to listOf(java.awt.event.KeyEvent.VK_Q),
                "r" to listOf(java.awt.event.KeyEvent.VK_E),
                "x" to listOf(java.awt.event.KeyEvent.VK_X),
                "y" to listOf(java.awt.event.KeyEvent.VK_Z),
                "b" to listOf(java.awt.event.KeyEvent.VK_C),
                "a" to listOf(java.awt.event.KeyEvent.VK_V)
            ),
            dpadMapping = DpadMapping(
                useAxes = false,
                buttons = ButtonMapping(
                    up = java.awt.event.KeyEvent.VK_UP,    // Arrow Up or W
                    down = java.awt.event.KeyEvent.VK_DOWN,  // Arrow Down or S
                    left = java.awt.event.KeyEvent.VK_LEFT,  // Arrow Left or A
                    right = java.awt.event.KeyEvent.VK_RIGHT // Arrow Right or D
                )
            )
        )
    }
    
    /**
     * Read the current state of the gamepad
     */
    private fun readGamepadState(): ControllerState {
        val config = controllerConfig
        
        if (config == null) {
            return ControllerState(isConnected = false, controllerName = "No Controller")
        }
        
        return when {
            keyboardFallback -> readKeyboardState(config)
            useSimulation -> readSimulatedState(config)
            else -> readRealGamepadState(config)
        }
    }
    
    /**
     * Read keyboard input as controller state
     */
    private fun readKeyboardState(config: ControllerConfig): ControllerState {
        // Check keyboard state and log any pressed keys for debugging
        val pressedKeys = keyStates.filter { it.value }.keys
        if (pressedKeys.isNotEmpty()) {
            logger.debug { "🎹 Keyboard keys pressed: ${pressedKeys.joinToString(", ")}" }
        }
        
        return ControllerState(
            dpadLeft = isKeyPressed(config.dpadMapping.buttons?.left ?: 0),
            dpadUp = isKeyPressed(config.dpadMapping.buttons?.up ?: 0),
            dpadRight = isKeyPressed(config.dpadMapping.buttons?.right ?: 0),
            dpadDown = isKeyPressed(config.dpadMapping.buttons?.down ?: 0),
            select = isKeyPressed(config.buttonMapping["select"]?.firstOrNull() ?: 0),
            start = isKeyPressed(config.buttonMapping["start"]?.firstOrNull() ?: 0),
            l = isKeyPressed(config.buttonMapping["l"]?.firstOrNull() ?: 0),
            r = isKeyPressed(config.buttonMapping["r"]?.firstOrNull() ?: 0),
            x = isKeyPressed(config.buttonMapping["x"]?.firstOrNull() ?: 0),
            y = isKeyPressed(config.buttonMapping["y"]?.firstOrNull() ?: 0),
            b = isKeyPressed(config.buttonMapping["b"]?.firstOrNull() ?: 0),
            a = isKeyPressed(config.buttonMapping["a"]?.firstOrNull() ?: 0),
            isConnected = true,
            controllerName = config.name
        )
    }
    
    /**
     * Read real gamepad state (placeholder for future implementation)
     */
    private fun readRealGamepadState(config: ControllerConfig): ControllerState {
        // TODO: Implement real gamepad reading using JInput or similar
        // For now, fall back to simulation
        return readSimulatedState(config)
    }
    
    /**
     * Read simulated gamepad state for testing
     */
    private fun readSimulatedState(config: ControllerConfig): ControllerState {
        val currentTime = System.currentTimeMillis()
        val simulateInput = (currentTime / 1000) % 10 < 2 // Simulate input for 2 seconds every 10 seconds
        
        return if (simulateInput) {
            ControllerState(
                dpadLeft = (currentTime / 200) % 4 == 0L,
                dpadUp = (currentTime / 300) % 4 == 1L,
                dpadRight = (currentTime / 250) % 4 == 2L,
                dpadDown = (currentTime / 350) % 4 == 3L,
                select = (currentTime / 500) % 6 == 0L,
                start = (currentTime / 600) % 6 == 1L,
                l = (currentTime / 400) % 5 == 0L,
                r = (currentTime / 450) % 5 == 1L,
                x = (currentTime / 180) % 3 == 0L,
                y = (currentTime / 220) % 3 == 1L,
                b = (currentTime / 160) % 3 == 2L,
                a = (currentTime / 140) % 4 == 0L,
                isConnected = true,
                controllerName = config.name
            )
        } else {
            ControllerState(
                isConnected = true,
                controllerName = config.name
            )
        }
    }
    
    /**
     * Check if a key is currently pressed
     */
    private fun isKeyPressed(keyCode: Int): Boolean {
        // This is a simplified check - in a real implementation,
        // we'd need proper keyboard event handling
        return keyStates[keyCode] ?: false
    }
    
    /**
     * Update key state (to be called from keyboard listeners)
     */
    fun updateKeyState(keyCode: Int, pressed: Boolean) {
        keyStates[keyCode] = pressed
    }
    
    /**
     * Update button press frequencies
     */
    private fun updatePressFrequencies(newState: ControllerState) {
        val currentTime = System.currentTimeMillis()
        val currentFrequencies = _buttonFrequencies.value.toMutableMap()
        val previousState = _controllerState.value
        
        // Check each button for new presses
        val buttonStates = mapOf(
            "dpadLeft" to (newState.dpadLeft to previousState.dpadLeft),
            "dpadUp" to (newState.dpadUp to previousState.dpadUp),
            "dpadRight" to (newState.dpadRight to previousState.dpadRight),
            "dpadDown" to (newState.dpadDown to previousState.dpadDown),
            "select" to (newState.select to previousState.select),
            "start" to (newState.start to previousState.start),
            "l" to (newState.l to previousState.l),
            "r" to (newState.r to previousState.r),
            "x" to (newState.x to previousState.x),
            "y" to (newState.y to previousState.y),
            "b" to (newState.b to previousState.b),
            "a" to (newState.a to previousState.a)
        )
        
        buttonStates.forEach { (buttonName, states) ->
            val (currentPressed, previousPressed) = states
            val currentFreq = currentFrequencies[buttonName] ?: ButtonFrequency()
            
            if (currentPressed && !previousPressed) {
                // New button press detected
                val newPressCount = currentFreq.pressCount + 1
                val timeSinceLastPress = currentTime - currentFreq.lastPressTime
                
                // Calculate presses per second (with smoothing)
                val newPressesPerSecond = if (timeSinceLastPress > 0) {
                    val instantRate = 1000f / timeSinceLastPress
                    // Smooth the rate with previous value
                    (currentFreq.pressesPerSecond * 0.7f + instantRate * 0.3f).coerceAtMost(20f)
                } else {
                    currentFreq.pressesPerSecond
                }
                
                currentFrequencies[buttonName] = ButtonFrequency(
                    pressCount = newPressCount,
                    lastPressTime = currentTime,
                    pressesPerSecond = newPressesPerSecond
                )
            }
        }
        
        _buttonFrequencies.value = currentFrequencies
    }
    
    /**
     * Update button frequencies (decay over time)
     */
    private fun updateButtonFrequencies() {
        val currentTime = System.currentTimeMillis()
        val currentFrequencies = _buttonFrequencies.value.toMutableMap()
        var updated = false
        
        currentFrequencies.forEach { (buttonName, freq) ->
            val timeSinceLastPress = currentTime - freq.lastPressTime
            
            if (timeSinceLastPress > 1000) { // 1 second decay
                val decayFactor = 0.95f // Gradual decay
                val newPressesPerSecond = freq.pressesPerSecond * decayFactor
                
                if (newPressesPerSecond > 0.1f) {
                    currentFrequencies[buttonName] = freq.copy(pressesPerSecond = newPressesPerSecond)
                    updated = true
                } else if (freq.pressesPerSecond > 0f) {
                    currentFrequencies[buttonName] = freq.copy(pressesPerSecond = 0f)
                    updated = true
                }
            }
        }
        
        if (updated) {
            _buttonFrequencies.value = currentFrequencies
        }
    }
    
    /**
     * Get SNES controller configuration
     */
    private fun getSNESControllerConfig(): ControllerConfig {
        return ControllerConfig(
            type = "snes",
            name = "SNES Controller",
            buttonMapping = mapOf(
                "select" to listOf(8),
                "start" to listOf(9),
                "l" to listOf(4),
                "r" to listOf(5),
                "x" to listOf(2),
                "y" to listOf(3),
                "b" to listOf(0),
                "a" to listOf(1)
            ),
            dpadMapping = DpadMapping(
                useAxes = true,
                axes = AxisMapping(
                    horizontal = 0,
                    vertical = 1
                ),
                buttons = ButtonMapping(
                    up = 12,
                    down = 13,
                    left = 14,
                    right = 15
                )
            )
        )
    }
}
