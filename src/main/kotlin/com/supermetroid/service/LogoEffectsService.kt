package com.supermetroid.service

import androidx.compose.runtime.*
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
 * Service for applying continuous effects to the Metroid logo
 */
class LogoEffectsService {
    private var effectsJob: Job? = null
    private var originalImage: BufferedImage? = null
    private var workingImage: BufferedImage? = null // For accumulating swaps
    
    // Effect state
    private val _logoState = MutableStateFlow(
        LogoEffectsState(
            logoImage = null,
            activeEffect = LogoEffectType.NONE,
            intensity = 0.5f,
            isRunning = false
        )
    )
    val logoState: StateFlow<LogoEffectsState> = _logoState.asStateFlow()
    
    // Dynamic coefficients for more varied noise
    private var noiseCoefficients = NoiseCoefficients()
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
    
    init {
        loadOriginalImage()
    }
    
    /**
     * Load the original logo image
     */
    private fun loadOriginalImage() {
        try {
            val resource = LogoEffectsService::class.java.getResourceAsStream("/metroid_390x390.jpeg")
            if (resource != null) {
                originalImage = ImageIO.read(resource)
                // Initialize working image as a copy of the original
                workingImage = BufferedImage(originalImage!!.width, originalImage!!.height, BufferedImage.TYPE_INT_RGB).apply {
                    graphics.drawImage(originalImage, 0, 0, null)
                    graphics.dispose()
                }
                _logoState.value = _logoState.value.copy(
                    logoImage = originalImage?.toComposeImageBitmap()
                )
                resource.close()
            }
        } catch (e: Exception) {
            println("[DEBUG_LOG] Failed to load logo image: ${e.message}")
        }
    }
    
    /**
     * Start the logo effects service
     */
    fun start() {
        if (effectsJob?.isActive == true) return
        
        effectsJob = CoroutineScope(Dispatchers.Default).launch {
            _logoState.value = _logoState.value.copy(isRunning = true)
            effectsLoop()
        }
    }
    
    /**
     * Stop the logo effects service
     */
    fun stop() {
        effectsJob?.cancel()
        effectsJob = null
        _logoState.value = _logoState.value.copy(isRunning = false)
        
        // Reset to original image and clear swap counter
        originalImage?.let { original ->
            // Reset working image to original
            workingImage = BufferedImage(original.width, original.height, BufferedImage.TYPE_INT_RGB).apply {
                graphics.drawImage(original, 0, 0, null)
                graphics.dispose()
            }
            totalSwapsPerformed = 0
            lastSwapX = -1
            lastSwapY = -1
            lastGridX = -1
            lastGridY = -1
            
            _logoState.value = _logoState.value.copy(
                logoImage = original.toComposeImageBitmap(),
                activeEffect = LogoEffectType.NONE
            )
        }
    }
    
    /**
     * Set the active effect type
     */
    fun setEffectType(effectType: LogoEffectType) {
        println("[DEBUG_LOG] Setting logo effect type to: $effectType")
        
        // Reset working image when changing effects
        originalImage?.let { original ->
            // Reset working image to original
            workingImage = BufferedImage(original.width, original.height, BufferedImage.TYPE_INT_RGB).apply {
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
                _logoState.value = _logoState.value.copy(
                    activeEffect = effectType,
                    logoImage = original.toComposeImageBitmap()
                )
            } else {
                _logoState.value = _logoState.value.copy(activeEffect = effectType)
            }
        }
    }
    
    /**
     * Set the effect intensity
     */
    fun setIntensity(intensity: Float) {
        _logoState.value = _logoState.value.copy(intensity = intensity.coerceIn(0f, 1f))
    }
    
    fun setTileSwapSize(size: Int) {
        tileSwapSize = size.coerceIn(1, 50) // Limit between 1x1 and 50x50 (increased for 30px support)
        _currentTileSize.value = tileSwapSize
        println("[DEBUG_LOG] Tile swap size set to: ${tileSwapSize}x${tileSwapSize}")
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
            
            // Apply current effect if enabled (get fresh state each time)
            val currentState = _logoState.value
            if (currentState.activeEffect != LogoEffectType.NONE && originalImage != null) {
                val newImage = when (currentState.activeEffect) {
                    LogoEffectType.NOISE -> applyContinuousNoiseEffect(originalImage!!, currentState.intensity, currentTime)
                    LogoEffectType.PIXEL_SWAP -> {
                        if (workingImage != null) {
                            applyAccumulativePixelSwapEffect(workingImage!!, currentState.intensity, frameCount)
                        } else {
                            originalImage!!.toComposeImageBitmap()
                        }
                    }
                    LogoEffectType.NONE -> originalImage!!.toComposeImageBitmap()
                }
                
                // Only update the image, preserve all other state
                _logoState.value = _logoState.value.copy(logoImage = newImage)
            }
            
            // Run at ~30 FPS for smooth animation
            delay(33.milliseconds)
        }
    }
    
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
                
                // Apply multiple noise layers with morphing coefficients
                val noiseR = calculateNoise(x, y, time, noiseCoefficients.redCoeffs) * intensity * 60
                val noiseG = calculateNoise(x, y, time, noiseCoefficients.greenCoeffs) * intensity * 60
                val noiseB = calculateNoise(x, y, time, noiseCoefficients.blueCoeffs) * intensity * 60
                
                // Apply noise to each color channel
                val newRed = (red + noiseR).coerceIn(0.0, 255.0).toInt()
                val newGreen = (green + noiseG).coerceIn(0.0, 255.0).toInt()
                val newBlue = (blue + noiseB).coerceIn(0.0, 255.0).toInt()
                
                val newColor = (newRed shl 16) or (newGreen shl 8) or newBlue
                result.setRGB(x, y, newColor)
            }
        }
        
        return result.toComposeImageBitmap()
    }
    
    /**
     * Apply accumulative grid-aligned tile swapping effect (like sliding puzzle games)
     */
    private fun applyAccumulativePixelSwapEffect(workingImg: BufferedImage, intensity: Float, frameCount: Int): ImageBitmap {
        // Calculate grid dimensions
        val tilesX = workingImg.width / tileSwapSize
        val tilesY = workingImg.height / tileSwapSize
        
        if (tilesX <= 1 || tilesY <= 1) {
            // Not enough tiles to make a meaningful grid
            return workingImg.toComposeImageBitmap()
        }
        
        // Fewer tile swaps but much more dramatic!
        val baseTileSwapCount = 6 // Fewer swaps since each swap is much bigger
        val swapCount = (baseTileSwapCount * intensity).toInt().coerceAtLeast(2)
        val random = Random(frameCount) // Use frame count for variety
        
        // Convert last position to grid coordinates
        var currentGridX = if (lastSwapX >= 0) (lastSwapX / tileSwapSize).coerceIn(0, tilesX - 1) else random.nextInt(tilesX)
        var currentGridY = if (lastSwapY >= 0) (lastSwapY / tileSwapSize).coerceIn(0, tilesY - 1) else random.nextInt(tilesY)
        
        repeat(swapCount) {
            // Get all valid neighbor directions (8-directional movement for perfect continuity)
            val validDirections = mutableListOf<Pair<Int, Int>>()
            
            for (dx in -1..1) {
                for (dy in -1..1) {
                    if (dx == 0 && dy == 0) continue // Skip self
                    
                    val nextX = currentGridX + dx
                    val nextY = currentGridY + dy
                    
                    // Check if the neighbor is within grid bounds
                    if (nextX in 0 until tilesX && nextY in 0 until tilesY) {
                        validDirections.add(Pair(dx, dy))
                    }
                }
            }
            
            if (validDirections.isNotEmpty()) {
                // Pick a random valid direction from current position for chaining
                val direction = validDirections[random.nextInt(validDirections.size)]
                val nextGridX = currentGridX + direction.first
                val nextGridY = currentGridY + direction.second
                
                // Always perform swap since we validated it's different and valid
                // Convert grid coordinates to pixel coordinates
                val tile1X = currentGridX * tileSwapSize
                val tile1Y = currentGridY * tileSwapSize
                val tile2X = nextGridX * tileSwapSize
                val tile2Y = nextGridY * tileSwapSize
                
                swapGridAlignedTiles(workingImg, tile1X, tile1Y, tile2X, tile2Y, tileSwapSize)
                
                // Move to the swapped tile position for next iteration (creates perfect chain)
                currentGridX = nextGridX
                currentGridY = nextGridY
            }
        }
        
        // Save last grid position converted back to pixel coordinates
        lastSwapX = currentGridX * tileSwapSize
        lastSwapY = currentGridY * tileSwapSize
        totalSwapsPerformed += swapCount
        
        // Debug log every 100 frames
        if (frameCount % 100 == 0) {
            println("[DEBUG_LOG] Chained tile swaps: ${swapCount} tiles (${tileSwapSize}x${tileSwapSize}), grid: ${tilesX}x${tilesY}, total: ${totalSwapsPerformed}, chain end: ($currentGridX, $currentGridY)")
        }
        
        // Return the modified working image
        return workingImg.toComposeImageBitmap()
    }
    
    /**
     * Swap two grid-aligned tiles (ensures perfect tile boundaries like sliding puzzle)
     */
    private fun swapGridAlignedTiles(img: BufferedImage, x1: Int, y1: Int, x2: Int, y2: Int, tileSize: Int) {
        // Create temporary storage for tile1
        val tile1Colors = Array(tileSize) { IntArray(tileSize) }
        
        // Ensure we don't go out of bounds
        val actualTileWidth = tileSize.coerceAtMost(img.width - x1).coerceAtMost(img.width - x2)
        val actualTileHeight = tileSize.coerceAtMost(img.height - y1).coerceAtMost(img.height - y2)
        
        // Copy tile1 to temporary storage and tile2 to tile1's position
        for (dy in 0 until actualTileHeight) {
            for (dx in 0 until actualTileWidth) {
                // Store tile1 pixel
                tile1Colors[dy][dx] = img.getRGB(x1 + dx, y1 + dy)
                
                // Copy tile2 pixel to tile1's position
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
    private fun calculateNoise(x: Int, y: Int, time: Double, coeffs: NoiseChannelCoeffs): Double {
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
        noiseCoefficients = NoiseCoefficients(
            redCoeffs = generateRandomCoeffs(random),
            greenCoeffs = generateRandomCoeffs(random),
            blueCoeffs = generateRandomCoeffs(random)
        )
    }
    
    /**
     * Generate random coefficients for a color channel
     */
    private fun generateRandomCoeffs(random: Random) = NoiseChannelCoeffs(
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
     * Convert BufferedImage to Compose ImageBitmap
     */
    private fun BufferedImage.toComposeImageBitmap(): ImageBitmap {
        return org.jetbrains.skia.Image.makeFromEncoded(
            java.io.ByteArrayOutputStream().use { baos ->
                ImageIO.write(this, "png", baos)
                baos.toByteArray()
            }
        ).asImageBitmap()
    }
}

/**
 * Logo effects state
 */
data class LogoEffectsState(
    val logoImage: ImageBitmap?,
    val activeEffect: LogoEffectType,
    val intensity: Float,
    val isRunning: Boolean
)

/**
 * Logo effect types
 */
enum class LogoEffectType {
    NONE,
    NOISE,
    PIXEL_SWAP
}

/**
 * Noise coefficients for complex, morphing effects
 */
private data class NoiseCoefficients(
    val redCoeffs: NoiseChannelCoeffs = NoiseChannelCoeffs(),
    val greenCoeffs: NoiseChannelCoeffs = NoiseChannelCoeffs(),
    val blueCoeffs: NoiseChannelCoeffs = NoiseChannelCoeffs()
)

/**
 * Noise coefficients for a single color channel
 */
private data class NoiseChannelCoeffs(
    val freq1: Double = 1.0,
    val freq2: Double = 0.5,
    val freq3: Double = 0.3,
    val freq4: Double = 0.2,
    val xScale1: Double = 0.03,
    val yScale1: Double = 0.03,
    val xScale2: Double = 0.05,
    val yScale2: Double = 0.05,
    val xScale3: Double = 0.02,
    val yScale3: Double = 0.02,
    val xyScale: Double = 0.025,
    val timeScale1: Double = 2.0,
    val timeScale2: Double = 2.5,
    val timeScale3: Double = 1.5,
    val timeScale4: Double = 1.8,
    val timeOffset1: Double = 0.0,
    val timeOffset2: Double = PI / 2
)
