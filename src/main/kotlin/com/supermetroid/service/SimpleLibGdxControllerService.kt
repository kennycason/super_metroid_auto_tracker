package com.supermetroid.service

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.controllers.Controllers
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import io.github.oshai.kotlinlogging.KotlinLogging
import java.awt.event.KeyEvent

private val logger = KotlinLogging.logger {}

/**
 * Simple LibGDX Controller Service based on proven ninjaturdle approach
 * Creates a minimal LibGDX application context for proper controller detection
 */
class SimpleLibGdxControllerService {
    private val _controllerState = MutableStateFlow(ControllerState())
    val controllerState: StateFlow<ControllerState> = _controllerState
    
    private val _buttonFrequencies = MutableStateFlow(emptyMap<String, ButtonFrequency>())
    val buttonFrequencies: StateFlow<Map<String, ButtonFrequency>> = _buttonFrequencies
    
    private var pollingJob: Job? = null
    private var isRunning = false
    private var gdxController: com.badlogic.gdx.controllers.Controller? = null
    private var useKeyboardFallback = false
    private var gdxApp: Lwjgl3Application? = null
    
    // Keyboard state for fallback
    private val keyboardState = mutableMapOf<String, Boolean>()
    
    /**
     * Minimal LibGDX application for controller support
     */
    private class ControllerApp : ApplicationAdapter() {
        override fun create() {
            // Minimal setup - we just need the application context for controllers
        }
        
        override fun render() {
            // Do nothing - we're only using this for controller detection
        }
    }
    
    fun start() {
        if (isRunning) {
            logger.debug { "SimpleLibGdxControllerService already running" }
            return
        }
        
        logger.info { "🚀 Starting Simple LibGDX Controller Service..." }
        isRunning = true
        
        // Initialize LibGDX application context (like ninjaturdle does)
        initializeLibGdxApplication()
        
        // Give LibGDX time to initialize
        Thread.sleep(500)
        
        // Detect controllers
        detectControllers()
        
        pollingJob = CoroutineScope(Dispatchers.Default).launch {
            pollControllerInput()
        }
        
        logger.info { "✅ Simple LibGDX Controller Service started" }
    }
    
    private fun initializeLibGdxApplication() {
        try {
            logger.info { "🔧 Initializing LibGDX application context for controller support..." }
            
            val config = Lwjgl3ApplicationConfiguration().apply {
                setTitle("Controller Service")
                setWindowedMode(1, 1) // Minimal window
                setWindowPosition(-1000, -1000) // Move off-screen
                setDecorated(false) // No window decorations
                setResizable(false)
                setInitialVisible(false) // Start hidden
            }
            
            // Create LibGDX application in background thread
            Thread {
                gdxApp = Lwjgl3Application(ControllerApp(), config)
            }.start()
            
            logger.info { "✅ LibGDX application context initialized" }
            
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to initialize LibGDX application context" }
        }
    }
    
    fun stop() {
        logger.info { "🛑 Stopping Simple LibGDX Controller Service..." }
        isRunning = false
        
        pollingJob?.cancel()
        pollingJob = null
        
        // Clean up LibGDX application
        try {
            gdxApp?.exit()
            gdxApp = null
            logger.debug { "🧹 LibGDX application cleaned up" }
        } catch (e: Exception) {
            logger.debug(e) { "⚠️ Error cleaning up LibGDX application" }
        }
        
        logger.info { "✅ Simple LibGDX Controller Service stopped" }
    }
    
    fun updateKeyState(keyCode: Int, pressed: Boolean) {
        // Map keyboard keys to controller buttons
        val buttonName = when (keyCode) {
            KeyEvent.VK_LEFT, KeyEvent.VK_A -> "dpadLeft"
            KeyEvent.VK_UP, KeyEvent.VK_W -> "dpadUp"
            KeyEvent.VK_RIGHT, KeyEvent.VK_D -> "dpadRight"
            KeyEvent.VK_DOWN, KeyEvent.VK_S -> "dpadDown"
            KeyEvent.VK_SPACE -> "select"
            KeyEvent.VK_ENTER -> "start"
            KeyEvent.VK_Q -> "l"
            KeyEvent.VK_E -> "r"
            KeyEvent.VK_Z, KeyEvent.VK_J -> "a"
            KeyEvent.VK_X, KeyEvent.VK_K -> "b"
            KeyEvent.VK_C, KeyEvent.VK_L -> "x"
            KeyEvent.VK_V, KeyEvent.VK_I -> "y"
            else -> null
        }
        
        if (buttonName != null) {
            keyboardState[buttonName] = pressed
            logger.debug { "⌨️ Keyboard: $buttonName = $pressed" }
        }
    }
    
    private fun detectControllers() {
        try {
            logger.info { "🔍 Initializing LibGDX controller detection..." }
            
            // Try to initialize LibGDX controllers
            val controllers = Controllers.getControllers()
            logger.info { "🎮 LibGDX Controllers.getControllers() returned ${controllers.size} controllers" }
            
            // Log details about each controller found
            for (i in 0 until controllers.size) {
                val controller = controllers[i]
                logger.info { "🎮 Controller $i: name='${controller.name}', uniqueId='${controller.uniqueId}'" }
            }
            
            if (controllers.size == 0) {
                logger.info { "⌨️ No controllers detected by LibGDX, using keyboard fallback" }
                logger.info { "💡 This is normal on macOS - LibGDX often can't detect Bluetooth/USB controllers" }
                useKeyboardFallback = true
                _controllerState.value = ControllerState(
                    isConnected = true,
                    controllerName = "Keyboard (Arrow Keys + WASD)"
                )
            } else {
                gdxController = controllers.first()
                useKeyboardFallback = false
                val controllerName = gdxController?.name ?: "Unknown Controller"
                logger.info { "🎮 SUCCESS! Using controller: $controllerName" }
                _controllerState.value = ControllerState(
                    isConnected = true,
                    controllerName = controllerName
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "❌ Error detecting controllers - falling back to keyboard" }
            useKeyboardFallback = true
            _controllerState.value = ControllerState(
                isConnected = true,
                controllerName = "Keyboard (Error Fallback)"
            )
        }
    }
    
    private suspend fun pollControllerInput() {
        while (isRunning && currentCoroutineContext().isActive) {
            try {
                val currentState = if (useKeyboardFallback) {
                    readKeyboardState()
                } else {
                    readControllerState()
                }
                
                _controllerState.value = currentState
                
                delay(16) // ~60 FPS polling
                
            } catch (e: Exception) {
                logger.debug { "Error polling controller input: ${e.message}" }
                delay(100)
            }
        }
    }
    
    private fun readKeyboardState(): ControllerState {
        return ControllerState(
            dpadLeft = keyboardState["dpadLeft"] ?: false,
            dpadUp = keyboardState["dpadUp"] ?: false,
            dpadRight = keyboardState["dpadRight"] ?: false,
            dpadDown = keyboardState["dpadDown"] ?: false,
            select = keyboardState["select"] ?: false,
            start = keyboardState["start"] ?: false,
            l = keyboardState["l"] ?: false,
            r = keyboardState["r"] ?: false,
            a = keyboardState["a"] ?: false,
            b = keyboardState["b"] ?: false,
            x = keyboardState["x"] ?: false,
            y = keyboardState["y"] ?: false,
            isConnected = true,
            controllerName = "Keyboard (Arrow Keys + WASD)"
        )
    }
    
    private fun readControllerState(): ControllerState {
        val controller = gdxController ?: return ControllerState()
        
        try {
            // Xbox controller button mapping (based on common patterns)
            val dpadLeft = controller.getButton(14) || controller.getAxis(0) < -0.5f
            val dpadRight = controller.getButton(15) || controller.getAxis(0) > 0.5f
            val dpadUp = controller.getButton(12) || controller.getAxis(1) < -0.5f
            val dpadDown = controller.getButton(13) || controller.getAxis(1) > 0.5f
            
            val a = controller.getButton(0)      // A button
            val b = controller.getButton(1)      // B button  
            val x = controller.getButton(2)      // X button
            val y = controller.getButton(3)      // Y button
            
            val l = controller.getButton(4)      // Left bumper
            val r = controller.getButton(5)      // Right bumper
            
            val select = controller.getButton(8)  // Back/Select
            val start = controller.getButton(9)   // Start
            
            return ControllerState(
                dpadLeft = dpadLeft,
                dpadUp = dpadUp,
                dpadRight = dpadRight,
                dpadDown = dpadDown,
                select = select,
                start = start,
                l = l,
                r = r,
                a = a,
                b = b,
                x = x,
                y = y,
                isConnected = true,
                controllerName = controller.name
            )
            
        } catch (e: Exception) {
            logger.debug { "Error reading controller state: ${e.message}" }
            return ControllerState(
                isConnected = true,
                controllerName = controller.name
            )
        }
    }
}
