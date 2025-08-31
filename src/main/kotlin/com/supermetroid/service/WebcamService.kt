package com.supermetroid.service

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.awt.Graphics2D
import java.awt.RenderingHints
import kotlin.time.Duration.Companion.milliseconds
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.UUID

/**
 * Service for managing webcam capture and device switching
 * Uses ffmpeg/system commands for cross-platform webcam access
 */
class WebcamService {
    private var captureJob: Job? = null
    private var ffmpegProcess: Process? = null
    private val tempDir = File(System.getProperty("java.io.tmpdir"), "smtracker_webcam")
    
    // Webcam state
    private val _webcamState = MutableStateFlow(
        WebcamState(
            currentFrame = null,
            availableCameras = emptyList(),
            selectedCameraIndex = 0,
            isCapturing = false,
            errorMessage = null,
            frameCount = 0,
            lastUpdateTime = System.currentTimeMillis(),
            frameId = UUID.randomUUID().toString()
        )
    )
    val webcamState: StateFlow<WebcamState> = _webcamState.asStateFlow()
    
    init {
        // Create temp directory and detect cameras
        tempDir.mkdirs()
        detectAvailableCameras()
    }
    
    /**
     * Detect available webcam devices using system-specific commands
     */
    private fun detectAvailableCameras() {
        try {
            println("[WEBCAM] Starting camera detection on ${System.getProperty("os.name")}")
            val cameras = when {
                System.getProperty("os.name").lowercase().contains("mac") -> detectMacCameras()
                System.getProperty("os.name").lowercase().contains("windows") -> detectWindowsCameras()
                else -> detectLinuxCameras()
            }
            
            _webcamState.value = _webcamState.value.copy(
                availableCameras = cameras,
                selectedCameraIndex = if (cameras.isNotEmpty()) 0 else -1
            )
            
            println("[WEBCAM] Detected ${cameras.size} cameras: ${cameras.map { it.name }}")
        } catch (e: Exception) {
            println("[WEBCAM] Error detecting cameras: ${e.message}")
            e.printStackTrace()
            _webcamState.value = _webcamState.value.copy(
                errorMessage = "Failed to detect cameras: ${e.message}"
            )
        }
    }
    
    /**
     * Detect cameras on macOS using system_profiler
     */
    private fun detectMacCameras(): List<WebcamDevice> {
        return try {
            // Use FFmpeg to list avfoundation devices directly
            val ffmpegProcess = ProcessBuilder("ffmpeg", "-f", "avfoundation", "-list_devices", "true", "-i", "").start()
            val ffmpegOutput = ffmpegProcess.errorStream.bufferedReader().readText()
            ffmpegProcess.waitFor()
            
            println("[WEBCAM] FFmpeg device list output: $ffmpegOutput")
            
            val cameras = mutableListOf<WebcamDevice>()
            val lines = ffmpegOutput.lines()
            
            for (line in lines) {
                // Look for lines like "[0] FaceTime HD Camera"
                if (line.contains("] ") && !line.contains("audio")) {
                    val match = Regex("\\[(\\d+)\\]\\s+(.+)").find(line.trim())
                    if (match != null) {
                        val deviceIndex = match.groupValues[1]
                        val deviceName = match.groupValues[2].trim()
                        println("[WEBCAM] Found camera: $deviceName (index: $deviceIndex)")
                        cameras.add(WebcamDevice(deviceName, deviceIndex))
                    }
                }
            }
            
            if (cameras.isEmpty()) {
                // Fallback to common camera names
                println("[WEBCAM] No cameras found via FFmpeg, using fallback")
                listOf(
                    WebcamDevice("FaceTime HD Camera", "0"),
                    WebcamDevice("Built-in Camera", "0")
                )
            } else {
                cameras
            }
        } catch (e: Exception) {
            println("[WEBCAM] Error detecting Mac cameras: ${e.message}")
            e.printStackTrace()
            listOf(
                WebcamDevice("FaceTime HD Camera", "0"),
                WebcamDevice("Built-in Camera", "0")
            )
        }
    }
    
    /**
     * Detect cameras on Windows using DirectShow
     */
    private fun detectWindowsCameras(): List<WebcamDevice> {
        return try {
            // Try to use ffmpeg to list DirectShow devices
            val process = ProcessBuilder("ffmpeg", "-list_devices", "true", "-f", "dshow", "-i", "dummy").start()
            val error = process.errorStream.bufferedReader().readText()
            process.waitFor()
            
            val cameraNames = error.lines()
                .filter { it.contains("DirectShow video devices") }
                .mapIndexed { index, line ->
                    WebcamDevice(
                        name = "Camera $index",
                        deviceId = index.toString()
                    )
                }
            
            if (cameraNames.isEmpty()) {
                listOf(WebcamDevice("Default Camera", "0"))
            } else {
                cameraNames
            }
        } catch (e: Exception) {
            println("[WEBCAM] Error detecting Windows cameras: ${e.message}")
            listOf(WebcamDevice("Default Camera", "0"))
        }
    }
    
    /**
     * Detect cameras on Linux using /dev/video*
     */
    private fun detectLinuxCameras(): List<WebcamDevice> {
        return try {
            val videoDevices = File("/dev").listFiles { file ->
                file.name.startsWith("video")
            }
            
            if (videoDevices != null) {
                videoDevices.sortedBy { it.name }.mapIndexed { _, device ->
                    WebcamDevice(
                        name = "Camera ${device.name}",
                        deviceId = device.absolutePath
                    )
                }.ifEmpty {
                    listOf(WebcamDevice("Default Camera", "/dev/video0"))
                }
            } else {
                listOf(WebcamDevice("Default Camera", "/dev/video0"))
            }
        } catch (e: Exception) {
            println("[WEBCAM] Error detecting Linux cameras: ${e.message}")
            listOf(WebcamDevice("Default Camera", "/dev/video0"))
        }
    }
    
    /**
     * Start webcam capture using ffmpeg
     */
    fun startCapture() {
        if (captureJob?.isActive == true) {
            println("[WEBCAM] Capture already running")
            return
        }
        
        val cameras = _webcamState.value.availableCameras
        val selectedIndex = _webcamState.value.selectedCameraIndex
        
        println("[WEBCAM] Starting capture - found ${cameras.size} cameras, selected index: $selectedIndex")
        
        if (cameras.isEmpty() || selectedIndex < 0 || selectedIndex >= cameras.size) {
            val errorMsg = "No camera available (cameras: ${cameras.size}, selectedIndex: $selectedIndex)"
            println("[WEBCAM] $errorMsg")
            _webcamState.value = _webcamState.value.copy(
                errorMessage = errorMsg
            )
            return
        }
        
        captureJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val selectedCamera = cameras[selectedIndex]
                startFfmpegCapture(selectedCamera)
                
                _webcamState.value = _webcamState.value.copy(
                    isCapturing = true,
                    errorMessage = null
                )
                
                // Start capture loop
                captureLoop()
            } catch (e: Exception) {
                println("[WEBCAM] Error starting capture: ${e.message}")
                _webcamState.value = _webcamState.value.copy(
                    isCapturing = false,
                    errorMessage = "Failed to start camera: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Stop webcam capture
     */
    fun stopCapture() {
        captureJob?.cancel()
        captureJob = null
        
        ffmpegProcess?.destroyForcibly()
        ffmpegProcess = null
        
        // Clean up temp files
        tempDir.listFiles()?.forEach { it.delete() }
        
        _webcamState.value = _webcamState.value.copy(
            isCapturing = false,
            currentFrame = null
        )
        
        println("[WEBCAM] Capture stopped")
    }
    
    /**
     * Switch to next available camera
     */
    fun nextCamera() {
        val cameras = _webcamState.value.availableCameras
        if (cameras.isEmpty()) return
        
        val currentIndex = _webcamState.value.selectedCameraIndex
        val nextIndex = (currentIndex + 1) % cameras.size
        
        _webcamState.value = _webcamState.value.copy(selectedCameraIndex = nextIndex)
        
        println("[WEBCAM] Switching to camera: ${cameras[nextIndex].name}")
        
        // Restart capture with new camera if currently capturing
        if (_webcamState.value.isCapturing) {
            stopCapture()
            startCapture()
        }
    }
    
    /**
     * Start ffmpeg capture process
     */
    private suspend fun startFfmpegCapture(camera: WebcamDevice) = withContext(Dispatchers.IO) {
        // Clean up previous process
        ffmpegProcess?.destroyForcibly()
        
        // Build ffmpeg command based on OS
        val command = when {
            System.getProperty("os.name").lowercase().contains("mac") -> {
                listOf("ffmpeg", "-f", "avfoundation", "-framerate", "30.000030", "-i", camera.deviceId, 
                       "-vf", "scale=640:480", "-f", "image2", 
                       "-update", "1", File(tempDir, "frame.jpg").absolutePath)
            }
            System.getProperty("os.name").lowercase().contains("windows") -> {
                listOf("ffmpeg", "-f", "dshow", "-i", "video=${camera.name}",
                       "-vf", "scale=640:480", "-r", "30", "-f", "image2",
                       "-update", "1", File(tempDir, "frame.jpg").absolutePath)
            }
            else -> { // Linux
                listOf("ffmpeg", "-f", "v4l2", "-i", camera.deviceId,
                       "-vf", "scale=640:480", "-r", "30", "-f", "image2",
                       "-update", "1", File(tempDir, "frame.jpg").absolutePath)
            }
        }
        
        try {
            println("[WEBCAM] Starting FFmpeg with command: ${command.joinToString(" ")}")
            
            ffmpegProcess = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            
            println("[WEBCAM] FFmpeg process started for camera: ${camera.name}")
            
            // Monitor FFmpeg output in a separate coroutine
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    ffmpegProcess?.inputStream?.bufferedReader()?.use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            println("[WEBCAM] FFmpeg: $line")
                        }
                    }
                    println("[WEBCAM] FFmpeg output stream ended")
                } catch (e: Exception) {
                    println("[WEBCAM] Error reading FFmpeg output: ${e.message}")
                }
            }
            
        } catch (e: Exception) {
            println("[WEBCAM] Error starting FFmpeg: ${e.message}")
            e.printStackTrace()
            throw RuntimeException("Failed to start ffmpeg: ${e.message}", e)
        }
    }
    
    /**
     * Main capture loop - read frames from ffmpeg output
     */
    private suspend fun captureLoop() {
        val frameFile = File(tempDir, "frame.jpg")
        var lastModified = 0L
        
        println("[WEBCAM] Starting capture loop")
        
        while (currentCoroutineContext().isActive && ffmpegProcess?.isAlive == true) {
            try {
                // Check if frame file has been updated
                if (frameFile.exists() && frameFile.lastModified() > lastModified) {
                    lastModified = frameFile.lastModified()
                    println("[WEBCAM] Frame file updated: ${frameFile.absolutePath}")
                    
                    // Wait a bit to ensure file write is complete
                    delay(20.milliseconds)
                    
                    // Validate JPEG file before reading - check for minimum size and that file exists
                    if (frameFile.exists() && frameFile.length() > 200) { // Ensure file has reasonable content
                        try {
                            val bufferedImage = ImageIO.read(frameFile)
                            if (bufferedImage != null) {
                                _webcamState.value = _webcamState.value.copy(
                                    currentFrame = bufferedImage.asImageBitmap(),
                                    frameCount = _webcamState.value.frameCount + 1,
                                    lastUpdateTime = System.currentTimeMillis(),
                                    frameId = UUID.randomUUID().toString()
                                )
                                println("[WEBCAM] Frame captured: ${bufferedImage.width}x${bufferedImage.height} (frame #${_webcamState.value.frameCount})")
                            }
                        } catch (e: javax.imageio.IIOException) {
                            // Skip corrupted/incomplete frames but continue processing
                            println("[WEBCAM] Skipping incomplete frame: ${e.message}")
                        }
                    }
                }
                
                // Check at ~30 FPS
                delay(33.milliseconds)
            } catch (e: Exception) {
                println("[WEBCAM] Error in capture loop: ${e.message}")
                e.printStackTrace()
                _webcamState.value = _webcamState.value.copy(
                    errorMessage = "Capture error: ${e.message}"
                )
                break
            }
        }
        
        println("[WEBCAM] Capture loop ended. Process alive: ${ffmpegProcess?.isAlive}")
        
        // If ffmpeg process died, update state
        if (ffmpegProcess?.isAlive != true) {
            val exitValue = try { ffmpegProcess?.exitValue() } catch (e: Exception) { "unknown" }
            println("[WEBCAM] FFmpeg process exit value: $exitValue")
            _webcamState.value = _webcamState.value.copy(
                isCapturing = false,
                errorMessage = "Camera process stopped unexpectedly (exit: $exitValue)"
            )
        }
    }
    
    /**
     * Convert BufferedImage to Compose ImageBitmap
     */
    private fun BufferedImage.asImageBitmap(): ImageBitmap {
        return org.jetbrains.skia.Image.makeFromEncoded(
            java.io.ByteArrayOutputStream().use { baos ->
                ImageIO.write(this, "png", baos)
                baos.toByteArray()
            }
        ).asImageBitmap()
    }
}

/**
 * Webcam state data class
 */
data class WebcamState(
    val currentFrame: ImageBitmap?,
    val availableCameras: List<WebcamDevice>,
    val selectedCameraIndex: Int,
    val isCapturing: Boolean,
    val errorMessage: String?,
    val frameCount: Long = 0, // Add frame counter to force UI updates
    val lastUpdateTime: Long = System.currentTimeMillis(), // Add timestamp to force UI updates
    val frameId: String = UUID.randomUUID().toString() // Unique ID to force UI recomposition
)

/**
 * Webcam device information
 */
data class WebcamDevice(
    val name: String,
    val deviceId: String
)
