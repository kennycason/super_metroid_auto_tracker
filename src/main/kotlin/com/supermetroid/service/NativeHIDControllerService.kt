package com.supermetroid.service

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Native HID Controller Service for macOS
 * Detects controllers using system_profiler and provides basic functionality
 */
class NativeHIDControllerService {
    private val _controllerState = MutableStateFlow(ControllerState())
    val controllerState: StateFlow<ControllerState> = _controllerState
    
    private val _buttonFrequencies = MutableStateFlow(emptyMap<String, ButtonFrequency>())
    val buttonFrequencies: StateFlow<Map<String, ButtonFrequency>> = _buttonFrequencies
    
    private var pollingJob: Job? = null
    private var isRunning = false
    
    data class ControllerInfo(
        val name: String,
        val vendorId: String,
        val productId: String,
        val connectionType: String
    )
    
    suspend fun start() {
        if (isRunning) {
            logger.debug { "Native HID Controller Service already running" }
            return
        }
        
        logger.info { "🚀 Starting Native HID Controller Service..." }
        isRunning = true
        
        pollingJob = CoroutineScope(Dispatchers.Default).launch {
            pollControllerState()
        }
        
        logger.info { "✅ Native HID Controller Service started" }
    }
    
    fun stop() {
        logger.info { "🛑 Stopping Native HID Controller Service..." }
        isRunning = false
        
        pollingJob?.cancel()
        pollingJob = null
        
        logger.info { "✅ Native HID Controller Service stopped" }
    }
    
    fun updateKeyState(keyCode: Int, pressed: Boolean) {
        logger.debug { "📱 Native service ignoring keyboard input (keyCode=$keyCode, pressed=$pressed)" }
        // Native service ignores keyboard input - it's for real controllers only
    }
    
    private suspend fun pollControllerState() {
        while (isRunning && currentCoroutineContext().isActive) {
            try {
                val controller = detectController()
                
                if (controller != null) {
                    logger.debug { "🎮 Controller detected: ${controller.name}" }
                    _controllerState.value = ControllerState(
                        isConnected = true,
                        controllerName = controller.name
                    )
                } else {
                    logger.debug { "❌ No controller detected" }
                    _controllerState.value = ControllerState(
                        isConnected = false,
                        controllerName = "DISCONNECTED"
                    )
                }
                
                delay(1000) // Check every second
                
            } catch (e: Exception) {
                logger.debug { "Error polling controller: ${e.message}" }
                delay(2000)
            }
        }
    }
    
    private fun detectController(): ControllerInfo? {
        return try {
            // Check USB controllers first (more reliable)
            val usbController = detectUSBController()
            if (usbController != null) {
                return usbController
            }
            
            // Then check Bluetooth controllers
            val bluetoothController = detectBluetoothController()
            return bluetoothController
            
        } catch (e: Exception) {
            logger.debug { "Error detecting controller: ${e.message}" }
            null
        }
    }
    
    private fun detectUSBController(): ControllerInfo? {
        return try {
            logger.debug { "🔍 Checking for USB controllers..." }
            
            val process = ProcessBuilder("system_profiler", "SPUSBDataType").start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            
            // Look for known controller patterns
            val lines = output.lines()
            var currentDevice: String? = null
            var vendorId: String? = null
            var productId: String? = null
            
            for (line in lines) {
                val trimmed = line.trim()
                
                when {
                    trimmed.endsWith(":") && !trimmed.contains("ID:") && !trimmed.contains("Bus") -> {
                        val deviceName = trimmed.removeSuffix(":").trim()
                        if (isLikelyController(deviceName)) {
                            currentDevice = deviceName
                        }
                    }
                    trimmed.contains("Vendor ID:") -> {
                        vendorId = trimmed.substringAfter("Vendor ID:").trim()
                    }
                    trimmed.contains("Product ID:") -> {
                        productId = trimmed.substringAfter("Product ID:").trim()
                    }
                    trimmed.isEmpty() && currentDevice != null && vendorId != null && productId != null -> {
                        // Complete controller found
                        val controllerName = when {
                            vendorId.contains("0x045e") -> "Xbox Controller"
                            vendorId.contains("0x054c") -> "PlayStation Controller"
                            vendorId.contains("0x057e") -> "Nintendo Controller"
                            else -> currentDevice
                        }
                        
                        logger.info { "🎮 Found USB controller: $controllerName (${vendorId}:${productId})" }
                        return ControllerInfo(
                            name = controllerName,
                            vendorId = vendorId,
                            productId = productId,
                            connectionType = "USB"
                        )
                    }
                }
            }
            
            null
        } catch (e: Exception) {
            logger.debug { "Error detecting USB controller: ${e.message}" }
            null
        }
    }
    
    private fun detectBluetoothController(): ControllerInfo? {
        return try {
            logger.debug { "🔍 Checking for Bluetooth controllers..." }
            
            val process = ProcessBuilder("system_profiler", "SPBluetoothDataType").start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            
            // Look for SNES Controller or other known controllers
            if (output.contains("SNES Controller") && output.contains("Connected:")) {
                logger.info { "🎮 Found Bluetooth SNES Controller" }
                return ControllerInfo(
                    name = "SNES Controller",
                    vendorId = "0x057e",
                    productId = "0x2017",
                    connectionType = "Bluetooth"
                )
            }
            
            null
        } catch (e: Exception) {
            logger.debug { "Error detecting Bluetooth controller: ${e.message}" }
            null
        }
    }
    
    private fun isLikelyController(deviceName: String): Boolean {
        val lowerName = deviceName.lowercase()
        return lowerName.contains("controller") ||
               lowerName.contains("gamepad") ||
               lowerName.contains("joystick") ||
               lowerName.contains("xbox") ||
               lowerName.contains("playstation") ||
               lowerName.contains("nintendo") ||
               lowerName.contains("snes")
    }
}
