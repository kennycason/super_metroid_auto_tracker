package com.supermetroid.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.awt.event.ActionListener
import javax.swing.Timer

private val logger = KotlinLogging.logger {}

/**
 * Simple controller service that works without LibGDX initialization
 * Uses keyboard input as primary method with excellent visual feedback
 */
class SimpleControllerService {
    private val _controllerState = MutableStateFlow(ControllerState())
    val controllerState: StateFlow<ControllerState> = _controllerState.asStateFlow()
    
    private val _buttonFrequencies = MutableStateFlow(
        mapOf<String, ButtonFrequency>()
    )
    val buttonFrequencies: StateFlow<Map<String, ButtonFrequency>> = _buttonFrequencies.asStateFlow()
    
    private var pollingJob: Job? = null
    private var updateTimer: Timer? = null
    private val buttonNames = listOf("dpadLeft", "dpadUp", "dpadRight", "dpadDown", "select", "start", "l", "r", "x", "y", "b", "a")
    
    // Keyboard state tracking
    private val keyStates = mutableMapOf<Int, Boolean>()
    private val keyPressTimestamps = mutableMapOf<Int, Long>()
    
    /**
     * Start the controller service
     */
    fun start() {
        if (pollingJob?.isActive == true) {
            logger.warn { "Controller service already running" }
            return
        }
        
        logger.info { "🎮 Starting Simple Controller Service..." }
        
        // Initialize button frequencies
        val initialFrequencies = buttonNames.associateWith { ButtonFrequency() }
        _buttonFrequencies.value = initialFrequencies
        
        // Set initial state to keyboard mode
        _controllerState.value = ControllerState(
            isConnected = true,
            controllerName = "Keyboard Controller (Arrow Keys + WASD + ZXCV + QE)"
        )
        
        // Start polling for input state
        pollingJob = CoroutineScope(Dispatchers.IO).launch {
            pollInputState()
        }
        
        // Start frequency decay timer
        updateTimer = Timer(100) { // Update every 100ms
            updateButtonFrequencies()
        }
        updateTimer?.start()
        
        logger.info { "✅ Simple Controller Service started - keyboard input ready" }
    }
    
    /**
     * Stop the controller service
     */
    fun stop() {
        logger.info { "🛑 Stopping Simple Controller Service..." }
        
        pollingJob?.cancel()
        pollingJob = null
        
        updateTimer?.stop()
        updateTimer = null
        
        _controllerState.value = ControllerState()
        _buttonFrequencies.value = buttonNames.associateWith { ButtonFrequency() }
        
        logger.info { "✅ Simple Controller Service stopped" }
    }
    
    /**
     * Main input polling loop
     */
    private suspend fun pollInputState() {
        while (currentCoroutineContext().isActive) {
            try {
                // Read current input state
                val newState = readInputState()
                
                // Update button frequencies for pressed buttons
                updatePressFrequencies(newState)
                
                // Update the controller state
                _controllerState.value = newState
                
                delay(16) // ~60 FPS polling rate
                
            } catch (e: Exception) {
                logger.error(e) { "❌ Error polling input state" }
                delay(1000) // Wait longer on error
            }
        }
    }
    
    /**
     * Read the current input state from keyboard
     */
    private fun readInputState(): ControllerState {
        return ControllerState(
            dpadLeft = isKeyPressed(java.awt.event.KeyEvent.VK_LEFT) || isKeyPressed(java.awt.event.KeyEvent.VK_A),
            dpadUp = isKeyPressed(java.awt.event.KeyEvent.VK_UP) || isKeyPressed(java.awt.event.KeyEvent.VK_W),
            dpadRight = isKeyPressed(java.awt.event.KeyEvent.VK_RIGHT) || isKeyPressed(java.awt.event.KeyEvent.VK_D),
            dpadDown = isKeyPressed(java.awt.event.KeyEvent.VK_DOWN) || isKeyPressed(java.awt.event.KeyEvent.VK_S),
            select = isKeyPressed(java.awt.event.KeyEvent.VK_BACK_SPACE),
            start = isKeyPressed(java.awt.event.KeyEvent.VK_ENTER),
            l = isKeyPressed(java.awt.event.KeyEvent.VK_Q),
            r = isKeyPressed(java.awt.event.KeyEvent.VK_E),
            x = isKeyPressed(java.awt.event.KeyEvent.VK_X),
            y = isKeyPressed(java.awt.event.KeyEvent.VK_Z),
            b = isKeyPressed(java.awt.event.KeyEvent.VK_C),
            a = isKeyPressed(java.awt.event.KeyEvent.VK_V),
            isConnected = true,
            controllerName = "Keyboard Controller"
        )
    }
    
    /**
     * Check if a key is currently pressed
     */
    private fun isKeyPressed(keyCode: Int): Boolean {
        return keyStates[keyCode] ?: false
    }
    
    /**
     * Update key state (called from keyboard event handlers)
     */
    fun updateKeyState(keyCode: Int, pressed: Boolean) {
        val wasPressed = keyStates[keyCode] ?: false
        keyStates[keyCode] = pressed
        
        if (pressed && !wasPressed) {
            // New key press
            keyPressTimestamps[keyCode] = System.currentTimeMillis()
            
            // Log the key press for debugging
            val buttonName = getButtonNameForKeyCode(keyCode)
            if (buttonName != null) {
                logger.debug { "🎹 Key pressed: $buttonName (keyCode: $keyCode)" }
            }
        } else if (!pressed && wasPressed) {
            // Key released
            val buttonName = getButtonNameForKeyCode(keyCode)
            if (buttonName != null) {
                logger.debug { "🎹 Key released: $buttonName (keyCode: $keyCode)" }
            }
        }
    }
    
    /**
     * Get button name for a key code
     */
    private fun getButtonNameForKeyCode(keyCode: Int): String? {
        return when (keyCode) {
            java.awt.event.KeyEvent.VK_LEFT, java.awt.event.KeyEvent.VK_A -> "dpadLeft"
            java.awt.event.KeyEvent.VK_UP, java.awt.event.KeyEvent.VK_W -> "dpadUp"
            java.awt.event.KeyEvent.VK_RIGHT, java.awt.event.KeyEvent.VK_D -> "dpadRight"
            java.awt.event.KeyEvent.VK_DOWN, java.awt.event.KeyEvent.VK_S -> "dpadDown"
            java.awt.event.KeyEvent.VK_BACK_SPACE -> "select"
            java.awt.event.KeyEvent.VK_ENTER -> "start"
            java.awt.event.KeyEvent.VK_Q -> "l"
            java.awt.event.KeyEvent.VK_E -> "r"
            java.awt.event.KeyEvent.VK_X -> "x"
            java.awt.event.KeyEvent.VK_Z -> "y"
            java.awt.event.KeyEvent.VK_C -> "b"
            java.awt.event.KeyEvent.VK_V -> "a"
            else -> null
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
                
                if (newPressesPerSecond > 1f) {
                    logger.debug { "🎮 High frequency detected: $buttonName (${String.format("%.1f", newPressesPerSecond)} presses/sec)" }
                }
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
}
