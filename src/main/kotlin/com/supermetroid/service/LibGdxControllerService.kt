package com.supermetroid.service

import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.controllers.ControllerListener
import com.badlogic.gdx.controllers.Controllers
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.awt.event.ActionListener
import javax.swing.Timer

private val logger = KotlinLogging.logger {}

/**
 * LibGDX-based controller service for real gamepad support
 */
class LibGdxControllerService {
    private val _controllerState = MutableStateFlow(ControllerState())
    val controllerState: StateFlow<ControllerState> = _controllerState.asStateFlow()
    
    private val _buttonFrequencies = MutableStateFlow(
        mapOf<String, ButtonFrequency>()
    )
    val buttonFrequencies: StateFlow<Map<String, ButtonFrequency>> = _buttonFrequencies.asStateFlow()
    
    private var pollingJob: Job? = null
    private var updateTimer: Timer? = null
    private val buttonNames = listOf("dpadLeft", "dpadUp", "dpadRight", "dpadDown", "select", "start", "l", "r", "x", "y", "b", "a")
    
    // LibGDX controller state
    private var currentController: Controller? = null
    private var controllerConfig: LibGdxControllerConfig? = null
    private var isLibGdxInitialized = false
    
    // Button mapping for different controller types (based on your existing code)
    data class LibGdxControllerConfig(
        val name: String,
        val buttonMapping: Map<String, Int>,
        val dpadMapping: DpadAxisMapping
    )
    
    data class DpadAxisMapping(
        val horizontalAxis: Int = 0,
        val verticalAxis: Int = 1,
        val threshold: Float = 0.3f,
        val useButtons: Boolean = false,
        val buttonUp: Int = 12,
        val buttonDown: Int = 13,
        val buttonLeft: Int = 14,
        val buttonRight: Int = 15
    )
    
    /**
     * Start the LibGDX controller service
     */
    fun start() {
        if (pollingJob?.isActive == true) {
            logger.warn { "LibGDX controller service already running" }
            return
        }
        
        logger.info { "🎮 Starting LibGDX controller service..." }
        
        try {
            // Initialize LibGDX controllers (minimal setup)
            if (!isLibGdxInitialized) {
                initializeLibGdx()
            }
            
            // Initialize button frequencies
            val initialFrequencies = buttonNames.associateWith { ButtonFrequency() }
            _buttonFrequencies.value = initialFrequencies
            
            // Detect controllers
            detectControllers()
            
            // Start polling for gamepad input
            pollingJob = CoroutineScope(Dispatchers.IO).launch {
                pollGamepadInput()
            }
            
            // Start frequency decay timer
            updateTimer = Timer(100) { // Update every 100ms
                updateButtonFrequencies()
            }
            updateTimer?.start()
            
            logger.info { "✅ LibGDX controller service started" }
            
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to start LibGDX controller service" }
            // Fall back to keyboard-only mode
            _controllerState.value = ControllerState(
                isConnected = true,
                controllerName = "Keyboard Only (LibGDX Failed)"
            )
        }
    }
    
    /**
     * Stop the controller service
     */
    fun stop() {
        logger.info { "🛑 Stopping LibGDX controller service..." }
        
        pollingJob?.cancel()
        pollingJob = null
        
        updateTimer?.stop()
        updateTimer = null
        
        _controllerState.value = ControllerState()
        _buttonFrequencies.value = buttonNames.associateWith { ButtonFrequency() }
        
        logger.info { "✅ LibGDX controller service stopped" }
    }
    
    /**
     * Initialize LibGDX controllers (minimal setup without full LibGDX app)
     */
    private fun initializeLibGdx() {
        try {
            // Initialize LibGDX controllers without full application
            // This is a lightweight initialization just for controller support
            logger.debug { "🔧 Initializing LibGDX controllers..." }
            
            // The Controllers class should initialize automatically when first accessed
            val controllerCount = Controllers.getControllers().size
            logger.info { "🎮 LibGDX initialized - found $controllerCount controllers" }
            
            isLibGdxInitialized = true
            
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to initialize LibGDX controllers" }
            throw e
        }
    }
    
    /**
     * Detect and configure controllers
     */
    private fun detectControllers() {
        try {
            val controllers = Controllers.getControllers()
            
            if (controllers.size == 0) {
                logger.info { "⌨️ No gamepads detected, using keyboard fallback" }
                _controllerState.value = ControllerState(
                    isConnected = true,
                    controllerName = "Keyboard (Arrow Keys + WASD)"
                )
                return
            }
            
            // Use the first controller
            currentController = controllers.first()
            val controllerName = currentController?.name ?: "Unknown Controller"
            
            logger.info { "🎮 Detected controller: $controllerName" }
            
            // Configure controller based on name/type
            controllerConfig = detectControllerType(controllerName)
            
            _controllerState.value = ControllerState(
                isConnected = true,
                controllerName = controllerName
            )
            
        } catch (e: Exception) {
            logger.error(e) { "❌ Error detecting controllers" }
        }
    }
    
    /**
     * Detect controller type and return appropriate configuration
     */
    private fun detectControllerType(controllerName: String): LibGdxControllerConfig {
        val name = controllerName.lowercase()
        
        return when {
            name.contains("xbox") || name.contains("microsoft") -> {
                logger.info { "🎮 Configured as Xbox controller" }
                LibGdxControllerConfig(
                    name = "Xbox Controller",
                    buttonMapping = mapOf(
                        "a" to 0,      // A
                        "b" to 1,      // B  
                        "x" to 2,      // X
                        "y" to 3,      // Y
                        "l" to 4,      // LB
                        "r" to 5,      // RB
                        "select" to 6, // Back/View
                        "start" to 7   // Start/Menu
                    ),
                    dpadMapping = DpadAxisMapping(
                        horizontalAxis = 0,
                        verticalAxis = 1,
                        threshold = 0.3f
                    )
                )
            }
            
            name.contains("playstation") || name.contains("ps4") || name.contains("ps5") || name.contains("dualshock") || name.contains("dualsense") -> {
                logger.info { "🎮 Configured as PlayStation controller" }
                LibGdxControllerConfig(
                    name = "PlayStation Controller",
                    buttonMapping = mapOf(
                        "a" to 1,      // Circle (A equivalent)
                        "b" to 0,      // Cross (B equivalent)
                        "x" to 2,      // Square (X equivalent)  
                        "y" to 3,      // Triangle (Y equivalent)
                        "l" to 4,      // L1
                        "r" to 5,      // R1
                        "select" to 8, // Share/Create
                        "start" to 9   // Options
                    ),
                    dpadMapping = DpadAxisMapping(
                        horizontalAxis = 0,
                        verticalAxis = 1,
                        threshold = 0.3f
                    )
                )
            }
            
            name.contains("nintendo") || name.contains("switch") || name.contains("pro controller") -> {
                logger.info { "🎮 Configured as Nintendo Switch Pro controller" }
                LibGdxControllerConfig(
                    name = "Nintendo Switch Pro Controller",
                    buttonMapping = mapOf(
                        "a" to 1,      // A
                        "b" to 0,      // B
                        "x" to 3,      // X
                        "y" to 2,      // Y
                        "l" to 4,      // L
                        "r" to 5,      // R
                        "select" to 8, // Minus
                        "start" to 9   // Plus
                    ),
                    dpadMapping = DpadAxisMapping(
                        horizontalAxis = 0,
                        verticalAxis = 1,
                        threshold = 0.3f
                    )
                )
            }
            
            else -> {
                logger.info { "🎮 Using generic USB gamepad configuration" }
                LibGdxControllerConfig(
                    name = "Generic USB Gamepad",
                    buttonMapping = mapOf(
                        "a" to 0,      // Button 0
                        "b" to 1,      // Button 1
                        "x" to 2,      // Button 2
                        "y" to 3,      // Button 3
                        "l" to 4,      // Button 4
                        "r" to 5,      // Button 5
                        "select" to 8, // Button 8
                        "start" to 9   // Button 9
                    ),
                    dpadMapping = DpadAxisMapping(
                        horizontalAxis = 0,
                        verticalAxis = 1,
                        threshold = 0.3f
                    )
                )
            }
        }
    }
    
    /**
     * Main gamepad polling loop
     */
    private suspend fun pollGamepadInput() {
        while (currentCoroutineContext().isActive) {
            try {
                // Re-detect controllers if needed
                if (currentController == null) {
                    detectControllers()
                }
                
                // Read current gamepad state
                val newState = readGamepadState()
                
                // Update button frequencies for pressed buttons
                updatePressFrequencies(newState)
                
                // Update the controller state
                _controllerState.value = newState
                
                delay(16) // ~60 FPS polling rate
                
            } catch (e: Exception) {
                logger.error(e) { "❌ Error polling gamepad input" }
                delay(1000) // Wait longer on error
            }
        }
    }
    
    /**
     * Read the current state of the gamepad
     */
    private fun readGamepadState(): ControllerState {
        val controller = currentController
        val config = controllerConfig
        
        if (controller == null || config == null) {
            return ControllerState(
                isConnected = false,
                controllerName = "No Controller"
            )
        }
        
        try {
            // Read D-pad from axes
            val dpadLeft = controller.getAxis(config.dpadMapping.horizontalAxis) < -config.dpadMapping.threshold
            val dpadRight = controller.getAxis(config.dpadMapping.horizontalAxis) > config.dpadMapping.threshold
            val dpadUp = controller.getAxis(config.dpadMapping.verticalAxis) < -config.dpadMapping.threshold
            val dpadDown = controller.getAxis(config.dpadMapping.verticalAxis) > config.dpadMapping.threshold
            
            // Read face buttons
            val aPressed = controller.getButton(config.buttonMapping["a"] ?: 0)
            val bPressed = controller.getButton(config.buttonMapping["b"] ?: 1)
            val xPressed = controller.getButton(config.buttonMapping["x"] ?: 2)
            val yPressed = controller.getButton(config.buttonMapping["y"] ?: 3)
            
            // Read shoulder buttons
            val lPressed = controller.getButton(config.buttonMapping["l"] ?: 4)
            val rPressed = controller.getButton(config.buttonMapping["r"] ?: 5)
            
            // Read system buttons
            val selectPressed = controller.getButton(config.buttonMapping["select"] ?: 8)
            val startPressed = controller.getButton(config.buttonMapping["start"] ?: 9)
            
            return ControllerState(
                dpadLeft = dpadLeft,
                dpadUp = dpadUp,
                dpadRight = dpadRight,
                dpadDown = dpadDown,
                select = selectPressed,
                start = startPressed,
                l = lPressed,
                r = rPressed,
                x = xPressed,
                y = yPressed,
                b = bPressed,
                a = aPressed,
                isConnected = true,
                controllerName = config.name
            )
            
        } catch (e: Exception) {
            logger.error(e) { "❌ Error reading gamepad state" }
            return ControllerState(
                isConnected = false,
                controllerName = "Controller Error"
            )
        }
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
                
                logger.debug { "🎮 Button pressed: $buttonName (${newPressesPerSecond} presses/sec)" }
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
     * Update key state for keyboard fallback
     */
    fun updateKeyState(keyCode: Int, pressed: Boolean) {
        // For keyboard fallback when no gamepad is detected
        // This integrates with the existing keyboard handling
        if (currentController == null) {
            // Handle keyboard input as controller buttons
            val buttonName = when (keyCode) {
                java.awt.event.KeyEvent.VK_UP -> "dpadUp"
                java.awt.event.KeyEvent.VK_DOWN -> "dpadDown"
                java.awt.event.KeyEvent.VK_LEFT -> "dpadLeft"
                java.awt.event.KeyEvent.VK_RIGHT -> "dpadRight"
                java.awt.event.KeyEvent.VK_W -> "dpadUp"
                java.awt.event.KeyEvent.VK_S -> "dpadDown"
                java.awt.event.KeyEvent.VK_A -> "dpadLeft"
                java.awt.event.KeyEvent.VK_D -> "dpadRight"
                java.awt.event.KeyEvent.VK_Z -> "y"
                java.awt.event.KeyEvent.VK_X -> "x"
                java.awt.event.KeyEvent.VK_C -> "b"
                java.awt.event.KeyEvent.VK_V -> "a"
                java.awt.event.KeyEvent.VK_Q -> "l"
                java.awt.event.KeyEvent.VK_E -> "r"
                java.awt.event.KeyEvent.VK_ENTER -> "start"
                java.awt.event.KeyEvent.VK_BACK_SPACE -> "select"
                else -> null
            }
            
            if (buttonName != null && pressed) {
                logger.debug { "⌨️ Keyboard button pressed: $buttonName" }
                // Update frequencies for keyboard input
                val currentTime = System.currentTimeMillis()
                val currentFrequencies = _buttonFrequencies.value.toMutableMap()
                val currentFreq = currentFrequencies[buttonName] ?: ButtonFrequency()
                
                currentFrequencies[buttonName] = ButtonFrequency(
                    pressCount = currentFreq.pressCount + 1,
                    lastPressTime = currentTime,
                    pressesPerSecond = 1f
                )
                
                _buttonFrequencies.value = currentFrequencies
            }
        }
    }
}
