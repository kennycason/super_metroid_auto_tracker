package com.supermetroid.service

import com.supermetroid.network.RetroArchUdpClient
import com.supermetroid.network.SuperMetroidAddresses
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI
import kotlin.time.Duration.Companion.milliseconds
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * Service for applying visual effects to Super Metroid's palette data
 * Modifies the game's memory to create various visual effects:
 * - Psychedelic: Multiple wave frequencies for chaotic colors
 * - Neon: Bright, vibrant colors
 * - Rainbow: Smooth cycling through hues
 * - Grayscale: Convert colors to grayscale/black and white
 *
 * Effects can be toggled on/off by clicking the same effect button again.
 */
class PaletteEffectsService(
    private val udpClient: RetroArchUdpClient,
    private val updateIntervalMs: Long = 100, // Increased to 100ms to reduce UDP traffic
    private val maxUpdatesPerFrame: Int = 4,  // Reduced from 8 to 4 colors per frame
    private val maxRegionsPerFrame: Int = 2   // Limit to 2 palette regions per frame
) {
    // Rate limiting and health monitoring
    private var lastUpdateTime: Long = 0
    private var updateCount: Int = 0
    private var successCount: Int = 0
    private var errorCount: Int = 0
    private var consecutiveErrors: Int = 0
    private var connectionHealthy: Boolean = true
    private val minUpdateIntervalMs: Long = 20 // Minimum time between updates
    private val maxConsecutiveErrors: Int = 5 // Maximum consecutive errors before pausing effects
    private var lastHealthCheckTime: Long = 0
    private val healthCheckIntervalMs: Long = 5000 // Check connection health every 5 seconds

    // Performance tracking
    private var totalWriteTimeMs: Long = 0
    private var successfulWrites: Int = 0
    private var failedWrites: Int = 0
    private var effectsJob: Job? = null
    private var timeOffset: Double = 0.0
    private var originalPalettes = ConcurrentHashMap<Int, Byte>()
    private var currentGradients = ConcurrentHashMap<Int, GradientState>()

    // Effect state
    private val _effectsState = MutableStateFlow(
        PaletteEffectsState(
            enabled = false,
            activeEffect = EffectType.NONE,
            intensity = 0.5f
        )
    )
    val effectsState: StateFlow<PaletteEffectsState> = _effectsState.asStateFlow()

    /**
     * Start the palette effects service
     */
    suspend fun start() {
        if (effectsJob?.isActive == true) {
            logger.warn { "⚠️ Palette effects service already running" }
            return
        }

        try {
            logger.info { "🚀 Starting palette effects service..." }

            // Clear any previous error message
            _effectsState.value = _effectsState.value.copy(errorMessage = null)

            // Make sure we're connected
            if (!udpClient.isConnected()) {
                try {
                    udpClient.connect()
                } catch (e: Exception) {
                    val errorMsg = "Failed to connect to RetroArch. Make sure RetroArch is running with network commands enabled."
                    _effectsState.value = _effectsState.value.copy(errorMessage = errorMsg)
                    logger.error(e) { "❌ $errorMsg" }
                    throw e
                }
            }

            // Backup original palettes before modifying
            try {
                backupOriginalPalettes()
            } catch (e: Exception) {
                val errorMsg = "Failed to read palette data from RetroArch. Make sure Super Metroid is loaded."
                _effectsState.value = _effectsState.value.copy(errorMessage = errorMsg)
                logger.error(e) { "❌ $errorMsg" }
                throw e
            }

            // Start the effects job
            effectsJob = CoroutineScope(Dispatchers.IO).launch {
                effectsLoop()
            }

            // Update state - clear any error message and set enabled to true
            _effectsState.value = _effectsState.value.copy(
                enabled = true,
                errorMessage = null
            )

            logger.info { "✅ Palette effects service started successfully" }
        } catch (e: Exception) {
            // If we haven't set a specific error message yet, set a generic one
            if (_effectsState.value.errorMessage == null) {
                _effectsState.value = _effectsState.value.copy(
                    errorMessage = "Failed to start effects: ${e.message ?: "Unknown error"}"
                )
            }
            logger.error(e) { "❌ Failed to start palette effects service" }
            throw e
        }
    }

    /**
     * Stop the palette effects service and restore original palettes
     */
    suspend fun stop() {
        logger.info { "🛑 Stopping palette effects service..." }

        try {
            effectsJob?.cancel()
            effectsJob = null

            // Restore original palettes
            restoreOriginalPalettes()

            // Update state - clear error message when successfully stopped
            _effectsState.value = _effectsState.value.copy(
                enabled = false,
                activeEffect = EffectType.NONE,
                errorMessage = null
            )

            logger.info { "✅ Palette effects service stopped" }
        } catch (e: Exception) {
            // Set error message if stopping fails
            val errorMsg = "Failed to stop effects: ${e.message ?: "Unknown error"}"
            _effectsState.value = _effectsState.value.copy(
                enabled = false,
                errorMessage = errorMsg
            )
            logger.error(e) { "❌ $errorMsg" }
            throw e
        }
    }

    /**
     * Set the active effect type
     * If the same effect is selected again, it will be toggled off (set to NONE)
     */
    fun setEffectType(effectType: EffectType) {
        // If the same effect is selected again, toggle it off
        val newEffectType = if (_effectsState.value.activeEffect == effectType) {
            logger.info { "🎨 Toggling effect off (was $effectType)" }
            EffectType.NONE
        } else {
            logger.info { "🎨 Setting effect type to $effectType" }
            effectType
        }

        _effectsState.value = _effectsState.value.copy(activeEffect = newEffectType)
    }

    /**
     * Set the effect intensity (0.0 - 1.0)
     */
    fun setIntensity(intensity: Float) {
        val clampedIntensity = intensity.coerceIn(0.0f, 1.0f)
        logger.info { "🔆 Setting effect intensity to $clampedIntensity" }
        _effectsState.value = _effectsState.value.copy(intensity = clampedIntensity)
    }

    /**
     * Toggle the effects on/off
     */
    suspend fun toggleEffects() {
        if (_effectsState.value.enabled) {
            stop()
        } else {
            start()
        }
    }

    /**
     * Backup original palette data before modifying
     */
    private suspend fun backupOriginalPalettes() {
        logger.info { "💾 Backing up original palette data..." }

        // Include all palette regions for comprehensive effects
        val paletteAddresses = listOf(
            SuperMetroidAddresses.SAMUS_PALETTE_START,        // Samus palette
            SuperMetroidAddresses.ENEMY_PALETTE_1_START,      // Enemy palette 1
            SuperMetroidAddresses.ENEMY_PALETTE_2_START,      // Enemy palette 2
            SuperMetroidAddresses.ENVIRONMENT_PALETTE_START,  // Environment palette
            SuperMetroidAddresses.MAP_ICON_PALETTE_START,     // Map icon palette
            SuperMetroidAddresses.MAP_AREA_PALETTE_START,     // Map area palette
            SuperMetroidAddresses.MAP_BACKGROUND_PALETTE_START, // Map background palette
            SuperMetroidAddresses.UI_PALETTE_START,           // UI palette
            SuperMetroidAddresses.HUD_PALETTE_START,          // HUD palette
            SuperMetroidAddresses.BEAM_PALETTE_START,         // Beam palette
            SuperMetroidAddresses.MISC_PALETTE_START          // Misc palette
        )

        var successCount = 0
        var failureCount = 0
        var firstError: Exception? = null

        for (baseAddress in paletteAddresses) {
            logger.debug { "📊 Backing up palette at base address 0x${baseAddress.toString(16)}" }

            // Each palette is 32 bytes (16 colors, 2 bytes per color)
            // But we only need to modify one byte per color for our effects
            for (offset in 0 until 32 step 2) {
                val address = baseAddress + offset
                try {
                    val data = udpClient.readMemory(address, 1)
                    if (data.isNotEmpty()) {
                        originalPalettes[address] = data[0]
                        successCount++
                    } else {
                        logger.warn { "⚠️ Empty data returned for address 0x${address.toString(16)}" }
                        failureCount++
                    }
                } catch (e: Exception) {
                    logger.error(e) { "❌ Failed to backup palette at address 0x${address.toString(16)}" }
                    failureCount++
                    if (firstError == null) {
                        firstError = e
                    }
                }
            }
        }

        logger.info { "📊 Palette backup stats: ${originalPalettes.size} values backed up, $successCount successes, $failureCount failures" }

        // If we didn't back up any palette values, throw an exception
        if (originalPalettes.isEmpty()) {
            throw firstError ?: IllegalStateException("Failed to backup any palette values")
        }

        // If we backed up some values but had failures, log a warning
        if (failureCount > 0) {
            logger.warn { "⚠️ Some palette values could not be backed up ($failureCount failures), but will continue with ${originalPalettes.size} values" }
        } else {
            logger.info { "✅ Successfully backed up all palette values" }
        }
    }

    /**
     * Restore original palette data
     */
    private suspend fun restoreOriginalPalettes() {
        logger.info { "🔄 Restoring original palette data..." }

        var successCount = 0
        var failureCount = 0
        var firstError: Exception? = null

        // Group by base address for more organized logging
        val groupedByBase = originalPalettes.entries.groupBy {
            // Find the base address by rounding down to nearest 32
            it.key - (it.key % 32)
        }

        for ((baseAddress, entries) in groupedByBase) {
            logger.debug { "📊 Restoring palette at base address 0x${baseAddress.toString(16)} (${entries.size} values)" }

            for ((address, value) in entries) {
                try {
                    if (udpClient.writeByte(address, value)) {
                        successCount++
                    } else {
                        logger.warn { "⚠️ Failed to write value to address 0x${address.toString(16)}" }
                        failureCount++
                    }
                } catch (e: Exception) {
                    logger.error(e) { "❌ Failed to restore palette at address 0x${address.toString(16)}" }
                    failureCount++
                    if (firstError == null) {
                        firstError = e
                    }
                }
            }
        }

        logger.info { "📊 Palette restore stats: $successCount successes, $failureCount failures out of ${originalPalettes.size} total values" }

        // If we had failures but some successes, log a warning
        if (failureCount > 0 && successCount > 0) {
            logger.warn { "⚠️ Some palette values could not be restored ($failureCount failures), but $successCount values were restored successfully" }
        } else if (failureCount > 0) {
            logger.error { "❌ Failed to restore any palette values" }
        } else {
            logger.info { "✅ Successfully restored all palette values" }
        }

        originalPalettes.clear()
        currentGradients.clear()
    }

    /**
     * Main effects loop
     */
    private suspend fun effectsLoop() {
        logger.info { "🎨 Starting palette effects loop..." }

        while (currentCoroutineContext().isActive) {
            try {
                // Update time offset for smooth animations
                timeOffset += updateIntervalMs / 1000.0

                // Check connection health periodically
                val now = System.currentTimeMillis()
                if (now - lastHealthCheckTime > healthCheckIntervalMs) {
                    checkConnectionHealth()
                    lastHealthCheckTime = now
                }

                // Only apply effects if connection is healthy or we're within error threshold
                if (connectionHealthy || consecutiveErrors < maxConsecutiveErrors) {
                    // Apply the current effect
                    when (_effectsState.value.activeEffect) {
                        EffectType.PSYCHEDELIC -> applyPsychedelicEffect()
                        EffectType.NEON -> applyNeonEffect()
                        EffectType.RAINBOW -> applyRainbowEffect()
                        EffectType.GRAYSCALE -> applyGrayscaleEffect()
                        EffectType.NONE -> { /* No effect */ }
                    }
                } else {
                    // Connection is unhealthy and we've exceeded error threshold
                    // Pause effects temporarily to avoid spamming errors
                    logger.warn { "⚠️ Pausing effects due to connection issues (consecutive errors: $consecutiveErrors)" }

                    // Update state with error message
                    _effectsState.value = _effectsState.value.copy(
                        errorMessage = "Connection issues detected. Retrying..."
                    )

                    // Wait a bit longer before retrying
                    delay(1000)
                }

            } catch (e: Exception) {
                logger.error(e) { "❌ Error during palette effects update: ${e.message}" }
                errorCount++
                consecutiveErrors++

                // Update state with error message
                _effectsState.value = _effectsState.value.copy(
                    errorMessage = "Error: ${e.message ?: "Unknown error"}"
                )
            }

            delay(updateIntervalMs.milliseconds)
        }
    }

    /**
     * Check connection health by performing a simple memory read
     * This helps detect connection issues early before they cause more serious problems
     */
    private suspend fun checkConnectionHealth() {
        try {
            // Try to read a small amount of memory to check connection
            val testAddress = SuperMetroidAddresses.GAME_STATE
            val data = udpClient.readMemory(testAddress, 1)

            if (data.isNotEmpty()) {
                // Connection is healthy
                if (!connectionHealthy) {
                    logger.info { "✅ Connection restored after ${consecutiveErrors} consecutive errors" }
                }
                connectionHealthy = true
                consecutiveErrors = 0

                // Clear error message if present
                if (_effectsState.value.errorMessage != null) {
                    _effectsState.value = _effectsState.value.copy(errorMessage = null)
                }
            } else {
                // Empty data indicates a problem
                connectionHealthy = false
                consecutiveErrors++
                logger.warn { "⚠️ Connection check failed: empty data returned (consecutive errors: $consecutiveErrors)" }
            }
        } catch (e: Exception) {
            // Connection check failed
            connectionHealthy = false
            consecutiveErrors++
            errorCount++

            logger.error(e) { "❌ Connection check failed: ${e.message} (consecutive errors: $consecutiveErrors)" }

            // Update state with error message
            _effectsState.value = _effectsState.value.copy(
                errorMessage = "Connection issues: ${e.message ?: "Unknown error"}"
            )
        }
    }

    /**
     * Apply psychedelic effect - multiple wave frequencies for chaotic colors
     * Includes rate limiting and connection health monitoring
     */
    private suspend fun applyPsychedelicEffect() {
        val intensity = _effectsState.value.intensity
        val now = System.currentTimeMillis()

        // Rate limiting - ensure minimum time between updates
        val timeSinceLastUpdate = now - lastUpdateTime
        if (timeSinceLastUpdate < minUpdateIntervalMs) {
            return
        }

        // Update statistics
        lastUpdateTime = now
        updateCount++

        // Include all palette regions for comprehensive effects
        val paletteAddresses = listOf(
            SuperMetroidAddresses.SAMUS_PALETTE_START,        // Samus palette
            SuperMetroidAddresses.ENEMY_PALETTE_1_START,      // Enemy palette 1
            SuperMetroidAddresses.ENEMY_PALETTE_2_START,      // Enemy palette 2
            SuperMetroidAddresses.ENVIRONMENT_PALETTE_START,  // Environment palette
            SuperMetroidAddresses.MAP_ICON_PALETTE_START,     // Map icon palette
            SuperMetroidAddresses.MAP_AREA_PALETTE_START,     // Map area palette
            SuperMetroidAddresses.MAP_BACKGROUND_PALETTE_START, // Map background palette
            SuperMetroidAddresses.UI_PALETTE_START,           // UI palette
            SuperMetroidAddresses.HUD_PALETTE_START,          // HUD palette
            SuperMetroidAddresses.BEAM_PALETTE_START,         // Beam palette
            SuperMetroidAddresses.MISC_PALETTE_START          // Misc palette
        )

        // Process a limited number of palette regions each frame
        // This ensures we don't overload the UDP connection
        val regionsToUpdate = minOf(maxRegionsPerFrame, paletteAddresses.size)
        val startRegionIndex = (timeOffset * 3).toInt() % paletteAddresses.size

        var successfulWrites = 0
        var failedWrites = 0

        // Process multiple regions with limited updates per region
        for (r in 0 until regionsToUpdate) {
            val regionIndex = (startRegionIndex + r) % paletteAddresses.size
            val baseAddress = paletteAddresses[regionIndex]

            // For each selected palette region, update a limited subset of colors
            val startOffset = ((timeOffset * 7) + regionIndex * 3.7).toInt() % 16 * 2
            val updatesForThisRegion = maxUpdatesPerFrame / regionsToUpdate

            for (i in 0 until updatesForThisRegion) {
                val offset = (startOffset + i * 2) % 32
                val address = baseAddress + offset

                // Get or create gradient state for this address
                val gradient = currentGradients.getOrPut(address) {
                    generateGradientState(address)
                }

                // Update gradient progress
                gradient.progress += gradient.speed
                if (gradient.progress >= 1.0) {
                    // Generate new target
                    val originalColor = originalPalettes[address] ?: 0
                    val newTarget = generatePsychedelicTarget(originalColor.toInt(), address, intensity)
                    gradient.startColor = gradient.targetColor
                    gradient.targetColor = newTarget
                    gradient.progress = 0.0
                    gradient.speed = generateSpeed(0.05, 0.15) // Slower for psychedelic
                }

                // Calculate current color using sine curve for smooth transition
                val curvedProgress = (sin(gradient.progress * PI - PI/2) + 1) / 2
                val currentColor = interpolateColor(
                    gradient.startColor,
                    gradient.targetColor,
                    curvedProgress
                )

                // Write the color with connection health monitoring
                try {
                    val startWriteTime = System.currentTimeMillis()
                    val success = udpClient.writeByte(address, currentColor.toByte())
                    val writeTime = System.currentTimeMillis() - startWriteTime

                    // Track total write time for performance metrics
                    totalWriteTimeMs += writeTime

                    if (success) {
                        successfulWrites++
                        successCount++
                        // Reset consecutive errors on success
                        if (consecutiveErrors > 0) {
                            consecutiveErrors = 0
                        }

                        // Log timing occasionally
                        if (updateCount % 100 == 0 && i == 0) {
                            logger.debug { "⏱️ Write time: ${writeTime}ms for address 0x${address.toString(16)}" }
                        }
                    } else {
                        failedWrites++
                        errorCount++
                        consecutiveErrors++
                        logger.warn { "⚠️ Write operation returned unsuccessful response for address 0x${address.toString(16)}" }
                    }
                } catch (e: Exception) {
                    failedWrites++
                    errorCount++
                    consecutiveErrors++
                    connectionHealthy = false

                    // Log but don't crash the effect loop
                    logger.error(e) { "❌ Failed to write psychedelic effect to address 0x${address.toString(16)}: ${e.message}" }

                    // Update error message in state
                    _effectsState.value = _effectsState.value.copy(
                        errorMessage = "Write error: ${e.message ?: "Unknown error"}"
                    )

                    // Break early on error to prevent further failures
                    break
                }
            }

            // If connection is unhealthy, stop processing more regions
            if (!connectionHealthy) {
                break
            }
        }

        // Log statistics occasionally
        if (updateCount % 100 == 0) {
            val successRate = if (successCount + errorCount > 0) {
                (successCount.toDouble() / (successCount + errorCount) * 100).toInt()
            } else 0

            // Calculate average write time if we have successful writes
            val avgWriteTime = if (successfulWrites > 0 && totalWriteTimeMs > 0) {
                totalWriteTimeMs / successfulWrites
            } else 0

            logger.info { "📊 Palette effects stats: success rate ${successRate}%, total updates: $updateCount, errors: $errorCount" }
            logger.info { "⏱️ Performance: avg write time: ${avgWriteTime}ms, successful writes: $successfulWrites, failed writes: $failedWrites" }
            logger.info { "🔄 Connection: healthy: $connectionHealthy, consecutive errors: $consecutiveErrors" }

            // Reset performance counters after logging
            totalWriteTimeMs = 0

            // Don't reset connection health here - let the health check handle it
        }

        // Log current frame stats if there were failures
        if (failedWrites > 0) {
            logger.warn { "⚠️ Frame stats: ${successfulWrites} successful writes, ${failedWrites} failed writes" }
        }
    }

    /**
     * Apply neon effect - bright, vibrant colors
     * Includes rate limiting and connection health monitoring
     */
    private suspend fun applyNeonEffect() {
        val intensity = _effectsState.value.intensity
        val now = System.currentTimeMillis()

        // Rate limiting - ensure minimum time between updates
        val timeSinceLastUpdate = now - lastUpdateTime
        if (timeSinceLastUpdate < minUpdateIntervalMs) {
            return
        }

        // Update statistics
        lastUpdateTime = now
        updateCount++

        // Include all palette regions for comprehensive effects
        val paletteAddresses = listOf(
            SuperMetroidAddresses.SAMUS_PALETTE_START,        // Samus palette
            SuperMetroidAddresses.ENEMY_PALETTE_1_START,      // Enemy palette 1
            SuperMetroidAddresses.ENEMY_PALETTE_2_START,      // Enemy palette 2
            SuperMetroidAddresses.ENVIRONMENT_PALETTE_START,  // Environment palette
            SuperMetroidAddresses.MAP_ICON_PALETTE_START,     // Map icon palette
            SuperMetroidAddresses.MAP_AREA_PALETTE_START,     // Map area palette
            SuperMetroidAddresses.MAP_BACKGROUND_PALETTE_START, // Map background palette
            SuperMetroidAddresses.UI_PALETTE_START,           // UI palette
            SuperMetroidAddresses.HUD_PALETTE_START,          // HUD palette
            SuperMetroidAddresses.BEAM_PALETTE_START,         // Beam palette
            SuperMetroidAddresses.MISC_PALETTE_START          // Misc palette
        )

        // Process a limited number of palette regions each frame
        // This ensures we don't overload the UDP connection
        val regionsToUpdate = minOf(maxRegionsPerFrame, paletteAddresses.size)
        val startRegionIndex = (timeOffset * 3).toInt() % paletteAddresses.size

        var successfulWrites = 0
        var failedWrites = 0

        // Process multiple regions with limited updates per region
        for (r in 0 until regionsToUpdate) {
            val regionIndex = (startRegionIndex + r) % paletteAddresses.size
            val baseAddress = paletteAddresses[regionIndex]

            // For each selected palette region, update a limited subset of colors
            val startOffset = ((timeOffset * 7) + regionIndex * 3.7).toInt() % 16 * 2
            val updatesForThisRegion = maxUpdatesPerFrame / regionsToUpdate

            for (i in 0 until updatesForThisRegion) {
                val offset = (startOffset + i * 2) % 32
                val address = baseAddress + offset

                // Get or create gradient state for this address
                val gradient = currentGradients.getOrPut(address) {
                    generateGradientState(address)
                }

                // Update gradient progress
                gradient.progress += gradient.speed
                if (gradient.progress >= 1.0) {
                    // Generate new target
                    val originalColor = originalPalettes[address] ?: 0
                    val newTarget = generateNeonTarget(originalColor.toInt(), address, intensity)
                    gradient.startColor = gradient.targetColor
                    gradient.targetColor = newTarget
                    gradient.progress = 0.0
                    gradient.speed = generateSpeed(0.1, 0.2) // Faster for neon
                }

                // Calculate current color using sine curve for smooth transition
                val curvedProgress = (sin(gradient.progress * PI - PI/2) + 1) / 2
                val currentColor = interpolateColor(
                    gradient.startColor,
                    gradient.targetColor,
                    curvedProgress
                )

                // Write the color with connection health monitoring
                try {
                    val startWriteTime = System.currentTimeMillis()
                    val success = udpClient.writeByte(address, currentColor.toByte())
                    val writeTime = System.currentTimeMillis() - startWriteTime

                    // Track total write time for performance metrics
                    totalWriteTimeMs += writeTime

                    if (success) {
                        successfulWrites++
                        successCount++
                        // Reset consecutive errors on success
                        if (consecutiveErrors > 0) {
                            consecutiveErrors = 0
                        }

                        // Log timing occasionally
                        if (updateCount % 100 == 0 && i == 0) {
                            logger.debug { "⏱️ Write time: ${writeTime}ms for address 0x${address.toString(16)}" }
                        }
                    } else {
                        failedWrites++
                        errorCount++
                        consecutiveErrors++
                        logger.warn { "⚠️ Write operation returned unsuccessful response for address 0x${address.toString(16)}" }
                    }
                } catch (e: Exception) {
                    failedWrites++
                    errorCount++
                    consecutiveErrors++
                    connectionHealthy = false

                    // Log but don't crash the effect loop
                    logger.error(e) { "❌ Failed to write neon effect to address 0x${address.toString(16)}: ${e.message}" }

                    // Update error message in state
                    _effectsState.value = _effectsState.value.copy(
                        errorMessage = "Write error: ${e.message ?: "Unknown error"}"
                    )

                    // Break early on error to prevent further failures
                    break
                }
            }

            // If connection is unhealthy, stop processing more regions
            if (!connectionHealthy) {
                break
            }
        }

        // Log current frame stats if there were failures
        if (failedWrites > 0) {
            logger.warn { "⚠️ Frame stats: ${successfulWrites} successful writes, ${failedWrites} failed writes" }
        }
    }

    /**
     * Apply rainbow effect - smooth cycling through hues
     * Includes rate limiting and connection health monitoring
     */
    private suspend fun applyRainbowEffect() {
        val intensity = _effectsState.value.intensity
        val now = System.currentTimeMillis()

        // Rate limiting - ensure minimum time between updates
        val timeSinceLastUpdate = now - lastUpdateTime
        if (timeSinceLastUpdate < minUpdateIntervalMs) {
            return
        }

        // Update statistics
        lastUpdateTime = now
        updateCount++

        // Include all palette regions for comprehensive effects
        val paletteAddresses = listOf(
            SuperMetroidAddresses.SAMUS_PALETTE_START,        // Samus palette
            SuperMetroidAddresses.ENEMY_PALETTE_1_START,      // Enemy palette 1
            SuperMetroidAddresses.ENEMY_PALETTE_2_START,      // Enemy palette 2
            SuperMetroidAddresses.ENVIRONMENT_PALETTE_START,  // Environment palette
            SuperMetroidAddresses.MAP_ICON_PALETTE_START,     // Map icon palette
            SuperMetroidAddresses.MAP_AREA_PALETTE_START,     // Map area palette
            SuperMetroidAddresses.MAP_BACKGROUND_PALETTE_START, // Map background palette
            SuperMetroidAddresses.UI_PALETTE_START,           // UI palette
            SuperMetroidAddresses.HUD_PALETTE_START,          // HUD palette
            SuperMetroidAddresses.BEAM_PALETTE_START,         // Beam palette
            SuperMetroidAddresses.MISC_PALETTE_START          // Misc palette
        )

        // Process a limited number of palette regions each frame
        // This ensures we don't overload the UDP connection
        val regionsToUpdate = minOf(maxRegionsPerFrame, paletteAddresses.size)
        val startRegionIndex = (timeOffset * 3).toInt() % paletteAddresses.size

        var successfulWrites = 0
        var failedWrites = 0

        // Process multiple regions with limited updates per region
        for (r in 0 until regionsToUpdate) {
            val regionIndex = (startRegionIndex + r) % paletteAddresses.size
            val baseAddress = paletteAddresses[regionIndex]

            // For rainbow effect, we need to be more selective about which colors to update
            // since we're updating fewer colors per frame
            val updatesForThisRegion = maxUpdatesPerFrame / regionsToUpdate
            val startOffset = ((timeOffset * 7) + regionIndex * 3.7).toInt() % 16 * 2

            for (i in 0 until updatesForThisRegion) {
                val offset = (startOffset + i * 2) % 32
                val address = baseAddress + offset

                // Calculate rainbow color based on time and position
                // Add a phase shift based on the palette region and offset for more variety
                val phaseShift = regionIndex * 0.1 + (offset / 32.0) * 0.5
                val hueOffset = (timeOffset * 2 + offset * 0.1 + phaseShift) % 1.0
                val rainbowColor = generateRainbowColor(hueOffset, intensity)

                // Write the color with connection health monitoring
                try {
                    val startWriteTime = System.currentTimeMillis()
                    val success = udpClient.writeByte(address, rainbowColor.toByte())
                    val writeTime = System.currentTimeMillis() - startWriteTime

                    // Track total write time for performance metrics
                    totalWriteTimeMs += writeTime

                    if (success) {
                        successfulWrites++
                        successCount++
                        // Reset consecutive errors on success
                        if (consecutiveErrors > 0) {
                            consecutiveErrors = 0
                        }

                        // Log timing occasionally
                        if (updateCount % 100 == 0 && i == 0) {
                            logger.debug { "⏱️ Write time: ${writeTime}ms for address 0x${address.toString(16)}" }
                        }
                    } else {
                        failedWrites++
                        errorCount++
                        consecutiveErrors++
                        logger.warn { "⚠️ Write operation returned unsuccessful response for address 0x${address.toString(16)}" }
                    }
                } catch (e: Exception) {
                    failedWrites++
                    errorCount++
                    consecutiveErrors++
                    connectionHealthy = false

                    // Log but don't crash the effect loop
                    logger.error(e) { "❌ Failed to write rainbow effect to address 0x${address.toString(16)}: ${e.message}" }

                    // Update error message in state
                    _effectsState.value = _effectsState.value.copy(
                        errorMessage = "Write error: ${e.message ?: "Unknown error"}"
                    )

                    // Break early on error to prevent further failures
                    break
                }
            }

            // If connection is unhealthy, stop processing more regions
            if (!connectionHealthy) {
                break
            }
        }

        // Log current frame stats if there were failures
        if (failedWrites > 0) {
            logger.warn { "⚠️ Frame stats: ${successfulWrites} successful writes, ${failedWrites} failed writes" }
        }
    }

    /**
     * Apply grayscale effect - convert colors to grayscale/black and white
     * Includes rate limiting and connection health monitoring
     */
    private suspend fun applyGrayscaleEffect() {
        val intensity = _effectsState.value.intensity
        val now = System.currentTimeMillis()

        // Rate limiting - ensure minimum time between updates
        val timeSinceLastUpdate = now - lastUpdateTime
        if (timeSinceLastUpdate < minUpdateIntervalMs) {
            return
        }

        // Update statistics
        lastUpdateTime = now
        updateCount++

        // Include all palette regions for comprehensive effects
        val paletteAddresses = listOf(
            SuperMetroidAddresses.SAMUS_PALETTE_START,        // Samus palette
            SuperMetroidAddresses.ENEMY_PALETTE_1_START,      // Enemy palette 1
            SuperMetroidAddresses.ENEMY_PALETTE_2_START,      // Enemy palette 2
            SuperMetroidAddresses.ENVIRONMENT_PALETTE_START,  // Environment palette
            SuperMetroidAddresses.MAP_ICON_PALETTE_START,     // Map icon palette
            SuperMetroidAddresses.MAP_AREA_PALETTE_START,     // Map area palette
            SuperMetroidAddresses.MAP_BACKGROUND_PALETTE_START, // Map background palette
            SuperMetroidAddresses.UI_PALETTE_START,           // UI palette
            SuperMetroidAddresses.HUD_PALETTE_START,          // HUD palette
            SuperMetroidAddresses.BEAM_PALETTE_START,         // Beam palette
            SuperMetroidAddresses.MISC_PALETTE_START          // Misc palette
        )

        // Process a limited number of palette regions each frame
        // This ensures we don't overload the UDP connection
        val regionsToUpdate = minOf(maxRegionsPerFrame, paletteAddresses.size)
        val startRegionIndex = (timeOffset * 3).toInt() % paletteAddresses.size

        var successfulWrites = 0
        var failedWrites = 0

        // Process multiple regions with limited updates per region
        for (r in 0 until regionsToUpdate) {
            val regionIndex = (startRegionIndex + r) % paletteAddresses.size
            val baseAddress = paletteAddresses[regionIndex]

            // For grayscale effect, we can update more colors at once
            val updatesForThisRegion = maxUpdatesPerFrame / regionsToUpdate
            val startOffset = ((timeOffset * 7) + regionIndex * 3.7).toInt() % 16 * 2

            for (i in 0 until updatesForThisRegion) {
                val offset = (startOffset + i * 2) % 32
                val address = baseAddress + offset

                // Get the original color and convert to grayscale
                val originalColor = originalPalettes[address]?.toInt() ?: 0
                val grayscaleColor = generateGrayscaleColor(originalColor, intensity)

                // Write the color with connection health monitoring
                try {
                    val startWriteTime = System.currentTimeMillis()
                    val success = udpClient.writeByte(address, grayscaleColor.toByte())
                    val writeTime = System.currentTimeMillis() - startWriteTime

                    // Track total write time for performance metrics
                    totalWriteTimeMs += writeTime

                    if (success) {
                        successfulWrites++
                        successCount++
                        // Reset consecutive errors on success
                        if (consecutiveErrors > 0) {
                            consecutiveErrors = 0
                        }

                        // Log timing occasionally
                        if (updateCount % 100 == 0 && i == 0) {
                            logger.debug { "⏱️ Write time: ${writeTime}ms for address 0x${address.toString(16)}" }
                        }
                    } else {
                        failedWrites++
                        errorCount++
                        consecutiveErrors++
                        logger.warn { "⚠️ Write operation returned unsuccessful response for address 0x${address.toString(16)}" }
                    }
                } catch (e: Exception) {
                    failedWrites++
                    errorCount++
                    consecutiveErrors++
                    connectionHealthy = false

                    // Log but don't crash the effect loop
                    logger.error(e) { "❌ Failed to write grayscale effect to address 0x${address.toString(16)}: ${e.message}" }

                    // Update error message in state
                    _effectsState.value = _effectsState.value.copy(
                        errorMessage = "Write error: ${e.message ?: "Unknown error"}"
                    )

                    // Break early on error to prevent further failures
                    break
                }
            }

            // If connection is unhealthy, stop processing more regions
            if (!connectionHealthy) {
                break
            }
        }

        // Log current frame stats if there were failures
        if (failedWrites > 0) {
            logger.warn { "⚠️ Frame stats: ${successfulWrites} successful writes, ${failedWrites} failed writes" }
        }
    }

    /**
     * Generate a psychedelic target color
     */
    private fun generatePsychedelicTarget(originalColor: Int, address: Int, intensity: Float): Int {
        // Multiple wave frequencies for chaotic effect
        val wave1 = sin(timeOffset * 2 + address * 0.01) * 80 * intensity
        val wave2 = cos(timeOffset * 3 + address * 0.02) * 60 * intensity
        val wave3 = sin(timeOffset * 1.5 + address * 0.015) * 40 * intensity

        val targetColor = (originalColor + wave1 + wave2 + wave3).toInt()
        return targetColor.coerceIn(0, 255)
    }

    /**
     * Generate a neon target color
     *
     * @param originalColor The original color value to enhance
     * @param intensity The intensity factor (0.0-1.0) controlling the brightness enhancement
     * @return The enhanced neon color value
     */
    private fun generateNeonTarget(originalColor: Int, @Suppress("UNUSED_PARAMETER") address: Int, intensity: Float): Int {
        // Bright, vibrant colors
        val brightnessFactor = 1.5f + intensity * 0.5f
        val targetColor = (originalColor * brightnessFactor).toInt()
        return targetColor.coerceIn(0, 255)
    }

    /**
     * Generate a rainbow color based on hue
     */
    private fun generateRainbowColor(hue: Double, intensity: Float): Int {
        // Convert HSV-like to RGB for smooth rainbow
        val rainbowIntensity = 127 * intensity
        val color = (128 + rainbowIntensity * sin(hue * 2 * PI)).toInt()
        return color.coerceIn(0, 255)
    }

    /**
     * Generate a grayscale color from an original color
     *
     * For SNES palette data, we're working with a single byte per color component.
     * This method applies a true grayscale conversion by:
     * 1. Mapping the original color to a grayscale value (0-255)
     * 2. Optionally enhancing contrast based on intensity
     */
    private fun generateGrayscaleColor(originalColor: Int, intensity: Float): Int {
        // First, ensure we have a valid color value
        val validColor = originalColor.coerceIn(0, 255)

        // For a better grayscale effect, we'll use a non-linear mapping
        // that emphasizes mid-tones and provides better contrast
        val grayscaleValue = when {
            validColor < 64 -> (validColor * 0.6).toInt()  // Darken shadows
            validColor < 192 -> (validColor * 1.1).toInt() // Enhance mid-tones
            else -> (validColor * 0.9).toInt()             // Slightly darken highlights
        }

        // Apply contrast enhancement based on intensity
        // At intensity=0, it's standard grayscale; at intensity=1, it's high contrast
        val contrastEnhanced = if (intensity > 0.1f) {
            // Apply contrast enhancement
            val midpoint = 128
            val contrast = 1.0f + (intensity * 1.5f) // 1.0 to 2.5
            midpoint + ((grayscaleValue - midpoint) * contrast).toInt()
        } else {
            grayscaleValue
        }

        // Ensure the result is within valid range
        return contrastEnhanced.coerceIn(0, 255)
    }

    /**
     * Generate a random gradient speed
     */
    private fun generateSpeed(min: Double, max: Double): Double {
        return min + Math.random() * (max - min)
    }

    /**
     * Generate initial gradient state for an address
     */
    private fun generateGradientState(address: Int): GradientState {
        val originalColor = originalPalettes[address]?.toInt() ?: 0
        val targetColor = originalColor
        return GradientState(
            startColor = originalColor,
            targetColor = targetColor,
            progress = 0.0,
            speed = generateSpeed(0.05, 0.15)
        )
    }

    /**
     * Interpolate between two colors
     */
    private fun interpolateColor(startColor: Int, endColor: Int, progress: Double): Int {
        val result = startColor + ((endColor - startColor) * progress).toInt()
        return result.coerceIn(0, 255)
    }
}

/**
 * Gradient state for smooth transitions
 */
data class GradientState(
    var startColor: Int,
    var targetColor: Int,
    var progress: Double,
    var speed: Double
)

/**
 * Palette effects state
 */
data class PaletteEffectsState(
    val enabled: Boolean,
    val activeEffect: EffectType,
    val intensity: Float,
    val errorMessage: String? = null
)

/**
 * Effect types
 */
enum class EffectType {
    NONE,
    PSYCHEDELIC,
    NEON,
    RAINBOW,
    GRAYSCALE
}
