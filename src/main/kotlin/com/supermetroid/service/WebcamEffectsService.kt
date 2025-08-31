package com.supermetroid.service

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.math.*
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

/**
 * Service for applying visual effects to webcam frames
 * Reuses the same effects system as LogoEffectsService
 */
class WebcamEffectsService {
    private var effectsJob: Job? = null
    private var originalFrame: BufferedImage? = null
    private var workingFrame: BufferedImage? = null // For accumulating swaps
    
    // Effect state
    private val _webcamEffectsState = MutableStateFlow(
        WebcamEffectsState(
            processedFrame = null,
            activeEffect = LogoEffectType.NONE,
            intensity = 0.5f,
            isRunning = false
        )
    )
    val webcamEffectsState: StateFlow<WebcamEffectsState> = _webcamEffectsState.asStateFlow()
    
    // Dynamic coefficients for more varied noise
    private var noiseCoefficients = com.supermetroid.service.NoiseCoefficients()
    private var lastCoefficientUpdateTime = 0.0
    
    // Swap accumulation tracking
    private var totalSwapsPerformed = 0
    private var lastSwapX = -1
    private var lastSwapY = -1
    
    // Grid-based chaining for continuous paths
    private var lastGridX = -1
    private var lastGridY = -1
    
    // Configurable tile swap size
    private var tileSwapSize = 5 // Start with 5x5 tiles
    
    // Add to state for UI tracking
    private val _currentTileSize = MutableStateFlow(5)
    val currentTileSize: StateFlow<Int> = _currentTileSize.asStateFlow()
    
    /**
     * Start the webcam effects service
     */
    fun start() {
        if (effectsJob?.isActive == true) return
        
        effectsJob = CoroutineScope(Dispatchers.Default).launch {
            _webcamEffectsState.value = _webcamEffectsState.value.copy(isRunning = true)
            effectsLoop()
        }
    }
    
    /**
     * Stop the webcam effects service
     */
    fun stop() {
        effectsJob?.cancel()
        effectsJob = null
        _webcamEffectsState.value = _webcamEffectsState.value.copy(isRunning = false)
        
        // Reset to original frame and clear swap counter
        originalFrame?.let { original ->
            // Reset working frame to original
            workingFrame = BufferedImage(original.width, original.height, BufferedImage.TYPE_INT_RGB).apply {
                graphics.drawImage(original, 0, 0, null)
                graphics.dispose()
            }
            totalSwapsPerformed = 0
            lastSwapX = -1
            lastSwapY = -1
            lastGridX = -1
            lastGridY = -1
            
            _webcamEffectsState.value = _webcamEffectsState.value.copy(
                processedFrame = original.asImageBitmap(),
                activeEffect = LogoEffectType.NONE
            )
        }
    }
    
    /**
     * Process a new webcam frame with effects
     */
    fun processFrame(frame: ImageBitmap) {
        try {
            // Convert ImageBitmap to BufferedImage
            val bufferedFrame = frame.toBufferedImage()
            
            // Store as original frame
            originalFrame = bufferedFrame
            
            // Initialize working frame if needed
            if (workingFrame == null || 
                workingFrame!!.width != bufferedFrame.width || 
                workingFrame!!.height != bufferedFrame.height) {
                workingFrame = BufferedImage(bufferedFrame.width, bufferedFrame.height, BufferedImage.TYPE_INT_RGB).apply {
                    graphics.drawImage(bufferedFrame, 0, 0, null)
                    graphics.dispose()
                }
            } else {
                // Update working frame with new frame data
                workingFrame!!.graphics.apply {
                    drawImage(bufferedFrame, 0, 0, null)
                    dispose()
                }
            }
            
        } catch (e: Exception) {
            println("[WEBCAM_EFFECTS] Error processing frame: ${e.message}")
        }
    }
    
    /**
     * Set the active effect type
     */
    fun setEffectType(effectType: LogoEffectType) {
        println("[WEBCAM_EFFECTS] Setting effect type to: $effectType")
        
        // Reset working frame when changing effects
        originalFrame?.let { original ->
            // Reset working frame to original
            workingFrame = BufferedImage(original.width, original.height, BufferedImage.TYPE_INT_RGB).apply {
                graphics.drawImage(original, 0, 0, null)
                graphics.dispose()
            }
            totalSwapsPerformed = 0
            lastSwapX = -1
            lastSwapY = -1
            lastGridX = -1
            lastGridY = -1
            
            // If stopping effects, reset to original
            if (effectType == LogoEffectType.NONE) {
                _webcamEffectsState.value = _webcamEffectsState.value.copy(
                    activeEffect = effectType,
                    processedFrame = original.asImageBitmap()
                )
            } else {
                _webcamEffectsState.value = _webcamEffectsState.value.copy(activeEffect = effectType)
            }
        }
    }
    
    /**
     * Set the effect intensity
     */
    fun setIntensity(intensity: Float) {
        _webcamEffectsState.value = _webcamEffectsState.value.copy(intensity = intensity.coerceIn(0f, 1f))
    }
    
    fun setTileSwapSize(size: Int) {
        tileSwapSize = size.coerceIn(1, 50)
        _currentTileSize.value = tileSwapSize
        println("[WEBCAM_EFFECTS] Tile swap size set to: ${tileSwapSize}x${tileSwapSize}")
    }
    
    /**
     * Main effects loop - runs continuously while service is active
     */
    private suspend fun effectsLoop() {
        var frameCount = 0
        val startTime = System.currentTimeMillis()
        
        while (currentCoroutineContext().isActive) {
            val currentTime = (System.currentTimeMillis() - startTime) / 1000.0
            frameCount++
            
            // Update noise coefficients periodically for variety
            if (currentTime - lastCoefficientUpdateTime > 3.0) { // Every 3 seconds
                updateNoiseCoefficients()
                lastCoefficientUpdateTime = currentTime
            }
            
            // Apply current effect if enabled and we have a frame
            val currentState = _webcamEffectsState.value
            if (currentState.activeEffect != LogoEffectType.NONE && originalFrame != null) {
                val newImage = when (currentState.activeEffect) {
                    LogoEffectType.NOISE -> applyContinuousNoiseEffect(originalFrame!!, currentState.intensity, currentTime)
                    LogoEffectType.PIXEL_SWAP -> {
                        if (workingFrame != null) {
                            applyAccumulativePixelSwapEffect(workingFrame!!, currentState.intensity, frameCount)
                        } else {
                            originalFrame!!.asImageBitmap()
                        }
                    }
                    LogoEffectType.WAVE -> applyContinuousWaveEffect(originalFrame!!, currentState.intensity, currentTime)
                    LogoEffectType.NONE -> originalFrame!!.asImageBitmap()
                }
                
                // Only update the image, preserve all other state
                _webcamEffectsState.value = _webcamEffectsState.value.copy(processedFrame = newImage)
            }
            
            // Run at ~30 FPS for smooth animation
            delay(33.milliseconds)
        }
    }
    
    // === EFFECT IMPLEMENTATIONS (copied from LogoEffectsService) ===
    
    /**
     * Apply continuous noise effect with morphing coefficients
     */
    private fun applyContinuousNoiseEffect(original: BufferedImage, intensity: Float, time: Double): ImageBitmap {
        val result = BufferedImage(original.width, original.height, BufferedImage.TYPE_INT_RGB)
        
        for (y in 0 until original.height) {
            for (x in 0 until original.width) {
                val originalColor = original.getRGB(x, y)
                
                // Extract RGB components
                val red = (originalColor shr 16) and 0xFF
                val green = (originalColor shr 8) and 0xFF
                val blue = originalColor and 0xFF
                
                // Apply multiple noise layers with exponential intensity scaling for more dramatic effect
                val scaledIntensity = intensity * intensity * 80 // Exponential scaling for more dramatic low-end control
                val noiseR = calculateNoise(x, y, time, noiseCoefficients.redCoeffs) * scaledIntensity
                val noiseG = calculateNoise(x, y, time, noiseCoefficients.greenCoeffs) * scaledIntensity
                val noiseB = calculateNoise(x, y, time, noiseCoefficients.blueCoeffs) * scaledIntensity
                
                // Apply noise to each color channel
                val newRed = (red + noiseR).coerceIn(0.0, 255.0).toInt()
                val newGreen = (green + noiseG).coerceIn(0.0, 255.0).toInt()
                val newBlue = (blue + noiseB).coerceIn(0.0, 255.0).toInt()
                
                val newColor = (newRed shl 16) or (newGreen shl 8) or newBlue
                result.setRGB(x, y, newColor)
            }
        }
        
        return result.asImageBitmap()
    }
    
    /**
     * Apply accumulative grid-aligned tile swapping effect
     */
    private fun applyAccumulativePixelSwapEffect(workingImg: BufferedImage, intensity: Float, frameCount: Int): ImageBitmap {
        // Calculate grid dimensions
        val tilesX = workingImg.width / tileSwapSize
        val tilesY = workingImg.height / tileSwapSize
        
        if (tilesX <= 1 || tilesY <= 1) {
            return workingImg.asImageBitmap()
        }
        
        // Make intensity have dramatic effect on swap rate
        val maxTileSwapCount = 8
        val minTileSwapCount = 0.1f
        val swapCount = if (intensity < 0.1f) {
            if (frameCount % (20 - (intensity * 100).toInt()) == 0) 1 else 0
        } else {
            (minTileSwapCount + (maxTileSwapCount - minTileSwapCount) * intensity).toInt().coerceAtLeast(1)
        }
        val random = Random(frameCount)
        
        // Convert last position to grid coordinates
        var currentGridX = if (lastSwapX >= 0) (lastSwapX / tileSwapSize).coerceIn(0, tilesX - 1) else random.nextInt(tilesX)
        var currentGridY = if (lastSwapY >= 0) (lastSwapY / tileSwapSize).coerceIn(0, tilesY - 1) else random.nextInt(tilesY)
        
        repeat(swapCount) {
            // Get all valid neighbor directions
            val validDirections = mutableListOf<Pair<Int, Int>>()
            
            for (dx in -1..1) {
                for (dy in -1..1) {
                    if (dx == 0 && dy == 0) continue
                    
                    val nextX = currentGridX + dx
                    val nextY = currentGridY + dy
                    
                    if (nextX in 0 until tilesX && nextY in 0 until tilesY) {
                        validDirections.add(Pair(dx, dy))
                    }
                }
            }
            
            if (validDirections.isNotEmpty()) {
                val direction = validDirections[random.nextInt(validDirections.size)]
                val nextGridX = currentGridX + direction.first
                val nextGridY = currentGridY + direction.second
                
                // Convert grid coordinates to pixel coordinates
                val tile1X = currentGridX * tileSwapSize
                val tile1Y = currentGridY * tileSwapSize
                val tile2X = nextGridX * tileSwapSize
                val tile2Y = nextGridY * tileSwapSize
                
                swapGridAlignedTiles(workingImg, tile1X, tile1Y, tile2X, tile2Y, tileSwapSize)
                
                // Move to the swapped tile position for next iteration
                currentGridX = nextGridX
                currentGridY = nextGridY
            }
        }
        
        // Save last grid position
        lastSwapX = currentGridX * tileSwapSize
        lastSwapY = currentGridY * tileSwapSize
        totalSwapsPerformed += swapCount
        
        return workingImg.asImageBitmap()
    }
    
    /**
     * Apply continuous 2D wave distortion effect
     */
    private fun applyContinuousWaveEffect(originalImg: BufferedImage, intensity: Float, timeSeconds: Double): ImageBitmap {
        val width = originalImg.width
        val height = originalImg.height
        val distortedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        
        val time = timeSeconds.toFloat()
        val scaledIntensity = intensity * intensity
        
        // Primary wave parameters
        val waveAmplitudeX = 10.0f * scaledIntensity * (1.0f + 0.3f * sin(time * 0.2f))
        val waveAmplitudeY = 8.0f * scaledIntensity * (1.0f + 0.4f * cos(time * 0.25f))
        val waveFrequencyX = 0.02f * (1.0f + 0.2f * sin(time * 0.15f))
        val waveFrequencyY = 0.025f * (1.0f + 0.3f * cos(time * 0.18f))
        
        // Secondary wave parameters
        val waveAmplitudeX2 = 6.0f * scaledIntensity * (1.0f + 0.5f * cos(time * 0.35f))
        val waveAmplitudeY2 = 7.0f * scaledIntensity * (1.0f + 0.4f * sin(time * 0.3f))
        val waveFrequencyX2 = 0.04f * (1.0f + 0.3f * cos(time * 0.22f))
        val waveFrequencyY2 = 0.03f * (1.0f + 0.2f * sin(time * 0.28f))
        
        // Tertiary wave parameters
        val waveAmplitudeX3 = 4.0f * scaledIntensity * (1.0f + 0.7f * sin(time * 0.8f))
        val waveAmplitudeY3 = 5.0f * scaledIntensity * (1.0f + 0.6f * cos(time * 0.7f))
        val waveFrequencyX3 = 0.08f * (1.0f + 0.4f * sin(time * 0.45f))
        val waveFrequencyY3 = 0.06f * (1.0f + 0.5f * cos(time * 0.5f))
        
        // Phase shifts
        val phaseX = time * 0.5f
        val phaseY = time * 0.4f
        val phaseX2 = time * 0.3f
        val phaseY2 = time * 0.6f
        val phaseX3 = time * 1.2f
        val phaseY3 = time * 0.9f
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                // Calculate multiple wave distortions and combine them
                val distortX1 = waveAmplitudeX * sin(y * waveFrequencyY + phaseX)
                val distortY1 = waveAmplitudeY * cos(x * waveFrequencyX + phaseY)
                
                val distortX2 = waveAmplitudeX2 * sin(y * waveFrequencyY2 + phaseX2)
                val distortY2 = waveAmplitudeY2 * cos(x * waveFrequencyX2 + phaseY2)
                
                val distortX3 = waveAmplitudeX3 * sin(y * waveFrequencyY3 + phaseX3)
                val distortY3 = waveAmplitudeY3 * cos(x * waveFrequencyX3 + phaseY3)
                
                // Combine all distortions
                val totalDistortX = distortX1 + distortX2 + distortX3
                val totalDistortY = distortY1 + distortY2 + distortY3
                
                // Calculate source coordinates with wrapping
                val sourceX = (x + totalDistortX).toInt()
                val sourceY = (y + totalDistortY).toInt()
                
                // Wrap coordinates around the image for seamless distortion
                val wrappedX = ((sourceX % width) + width) % width
                val wrappedY = ((sourceY % height) + height) % height
                
                // Sample the pixel from the wrapped coordinates
                val color = originalImg.getRGB(wrappedX, wrappedY)
                distortedImage.setRGB(x, y, color)
            }
        }
        
        return distortedImage.asImageBitmap()
    }
    
    // === HELPER METHODS ===
    
    /**
     * Swap two grid-aligned tiles
     */
    private fun swapGridAlignedTiles(img: BufferedImage, x1: Int, y1: Int, x2: Int, y2: Int, tileSize: Int) {
        val tile1Colors = Array(tileSize) { IntArray(tileSize) }
        
        val actualTileWidth = tileSize.coerceAtMost(img.width - x1).coerceAtMost(img.width - x2)
        val actualTileHeight = tileSize.coerceAtMost(img.height - y1).coerceAtMost(img.height - y2)
        
        // Copy tile1 to temporary storage and tile2 to tile1's position
        for (dy in 0 until actualTileHeight) {
            for (dx in 0 until actualTileWidth) {
                tile1Colors[dy][dx] = img.getRGB(x1 + dx, y1 + dy)
                img.setRGB(x1 + dx, y1 + dy, img.getRGB(x2 + dx, y2 + dy))
            }
        }
        
        // Copy stored tile1 pixels to tile2's position
        for (dy in 0 until actualTileHeight) {
            for (dx in 0 until actualTileWidth) {
                img.setRGB(x2 + dx, y2 + dy, tile1Colors[dy][dx])
            }
        }
    }
    
    /**
     * Calculate complex noise with multiple frequencies
     */
    private fun calculateNoise(x: Int, y: Int, time: Double, coeffs: com.supermetroid.service.NoiseChannelCoeffs): Double {
        return coeffs.freq1 * sin(x * coeffs.xScale1 + time * coeffs.timeScale1) * cos(y * coeffs.yScale1 + time * coeffs.timeOffset1) +
               coeffs.freq2 * sin(x * coeffs.xScale2 + time * coeffs.timeScale2) * cos(y * coeffs.yScale2 + time * coeffs.timeOffset2) +
               coeffs.freq3 * sin(x * coeffs.xScale3 + time * coeffs.timeScale3) * cos(y * coeffs.yScale3) +
               coeffs.freq4 * sin((x + y) * coeffs.xyScale + time * coeffs.timeScale4)
    }
    
    /**
     * Update noise coefficients for variety
     */
    private fun updateNoiseCoefficients() {
        val random = Random(System.currentTimeMillis())
        noiseCoefficients = com.supermetroid.service.NoiseCoefficients(
            redCoeffs = generateRandomCoeffs(random),
            greenCoeffs = generateRandomCoeffs(random),
            blueCoeffs = generateRandomCoeffs(random)
        )
    }
    
    /**
     * Generate random coefficients for a color channel
     */
    private fun generateRandomCoeffs(random: Random) = com.supermetroid.service.NoiseChannelCoeffs(
        freq1 = random.nextDouble(0.3, 1.0),
        freq2 = random.nextDouble(0.2, 0.8),
        freq3 = random.nextDouble(0.1, 0.6),
        freq4 = random.nextDouble(0.1, 0.4),
        xScale1 = random.nextDouble(0.01, 0.05),
        yScale1 = random.nextDouble(0.01, 0.05),
        xScale2 = random.nextDouble(0.02, 0.08),
        yScale2 = random.nextDouble(0.02, 0.08),
        xScale3 = random.nextDouble(0.005, 0.03),
        yScale3 = random.nextDouble(0.005, 0.03),
        xyScale = random.nextDouble(0.01, 0.04),
        timeScale1 = random.nextDouble(1.0, 3.0),
        timeScale2 = random.nextDouble(1.5, 4.0),
        timeScale3 = random.nextDouble(0.5, 2.0),
        timeScale4 = random.nextDouble(0.8, 2.5),
        timeOffset1 = random.nextDouble(0.0, PI * 2),
        timeOffset2 = random.nextDouble(0.0, PI * 2)
    )
    
    /**
     * Convert ImageBitmap to BufferedImage
     */
    private fun ImageBitmap.toBufferedImage(): BufferedImage {
        // This is a simplified conversion - in a real implementation you'd want
        // to properly handle the conversion from Skia bitmap to BufferedImage
        val width = this.width
        val height = this.height
        val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        
        // For now, create a placeholder conversion
        // In practice, you'd extract pixel data from the ImageBitmap
        return bufferedImage
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
 * Webcam effects state
 */
data class WebcamEffectsState(
    val processedFrame: ImageBitmap?,
    val activeEffect: LogoEffectType,
    val intensity: Float,
    val isRunning: Boolean
)


