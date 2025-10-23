package com.supermetroid.network

import com.supermetroid.util.Logging
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

/**
 * Service for detecting available memory adapters and choosing the best one
 * Supports auto-detection of SNI and RetroArch services
 */
class MemoryAdapterDetectionService : Logging {

    /**
     * Detection result containing the available adapters and the recommended one
     */
    data class DetectionResult(
        val availableAdapters: List<MemoryAdapter>,
        val recommendedAdapter: MemoryAdapter?,
        val detectionTimeMs: Long
    )

    /**
     * Detection preferences for choosing between multiple available adapters
     */
    data class DetectionPreferences(
        val preferSNI: Boolean = true,  // Prefer SNI over RetroArch if both available
        val sniHost: String = "localhost",
        val sniPort: Int = 8191,
        val retroArchHost: String = "localhost",
        val retroArchPort: Int = 55355,
        val timeoutMs: Int = 2000
    )

    /**
     * Detect all available memory adapters and return the recommended one
     */
    suspend fun detectAvailableAdapters(preferences: DetectionPreferences = DetectionPreferences()): DetectionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        logger.info { "🔍 Detecting available memory adapters..." }

        // Create adapter instances to test
        val sniAdapter = SNIMemoryAdapter(preferences.sniHost, preferences.sniPort)
        val retroArchAdapter = RetroArchMemoryAdapter(preferences.retroArchHost, preferences.retroArchPort)

        // Test adapters concurrently for faster detection
        val sniAvailable = async {
            try {
                sniAdapter.isAvailable()
            } catch (e: Exception) {
                logger.debug(e) { "SNI availability check failed" }
                false
            }
        }

        val retroArchAvailable = async {
            try {
                retroArchAdapter.isAvailable()
            } catch (e: Exception) {
                logger.debug(e) { "RetroArch availability check failed" }
                false
            }
        }

        // Collect results
        val availableAdapters = mutableListOf<MemoryAdapter>()

        if (sniAvailable.await()) {
            availableAdapters.add(sniAdapter)
            logger.info { "✅ SNI adapter available at ${preferences.sniHost}:${preferences.sniPort}" }
        } else {
            logger.debug { "❌ SNI adapter not available at ${preferences.sniHost}:${preferences.sniPort}" }
        }

        if (retroArchAvailable.await()) {
            availableAdapters.add(retroArchAdapter)
            logger.info { "✅ RetroArch adapter available at ${preferences.retroArchHost}:${preferences.retroArchPort}" }
        } else {
            logger.debug { "❌ RetroArch adapter not available at ${preferences.retroArchHost}:${preferences.retroArchPort}" }
        }

        // Choose recommended adapter based on preferences
        val recommendedAdapter = chooseRecommendedAdapter(availableAdapters, preferences)

        val detectionTime = System.currentTimeMillis() - startTime
        logger.info { "🔍 Detection completed in ${detectionTime}ms. Found ${availableAdapters.size} adapter(s), recommended: ${recommendedAdapter?.getAdapterName() ?: "none"}" }

        return@withContext DetectionResult(
            availableAdapters = availableAdapters,
            recommendedAdapter = recommendedAdapter,
            detectionTimeMs = detectionTime
        )
    }

    /**
     * Quick check to see if any adapter is available
     */
    suspend fun isAnyAdapterAvailable(preferences: DetectionPreferences = DetectionPreferences()): Boolean = withContext(Dispatchers.IO) {
        val result = detectAvailableAdapters(preferences)
        return@withContext result.availableAdapters.isNotEmpty()
    }

    /**
     * Get the best available adapter, connecting it if possible
     */
    suspend fun getBestAvailableAdapter(preferences: DetectionPreferences = DetectionPreferences()): MemoryAdapter? = withContext(Dispatchers.IO) {
        val result = detectAvailableAdapters(preferences)
        val adapter = result.recommendedAdapter

        if (adapter != null) {
            val connected = adapter.connect()
            if (connected) {
                logger.info { "✅ Connected to ${adapter.getAdapterName()}" }
                return@withContext adapter
            } else {
                logger.error { "❌ Failed to connect to ${adapter.getAdapterName()}" }
                adapter.disconnect()
                return@withContext null
            }
        }

        logger.warn { "⚠️ No suitable memory adapter found" }
        return@withContext null
    }

    /**
     * Choose the recommended adapter from available ones based on preferences
     */
    private fun chooseRecommendedAdapter(
        availableAdapters: List<MemoryAdapter>,
        preferences: DetectionPreferences
    ): MemoryAdapter? {
        if (availableAdapters.isEmpty()) {
            return null
        }

        // If only one adapter is available, use it
        if (availableAdapters.size == 1) {
            return availableAdapters.first()
        }

        // If multiple adapters are available, choose based on preferences
        return if (preferences.preferSNI) {
            // Prefer SNI if available, fall back to RetroArch
            availableAdapters.firstOrNull { it.getAdapterType() == MemoryAdapter.AdapterType.SNI_GRPC }
                ?: availableAdapters.firstOrNull { it.getAdapterType() == MemoryAdapter.AdapterType.RETROARCH_NWA }
        } else {
            // Prefer RetroArch if available, fall back to SNI
            availableAdapters.firstOrNull { it.getAdapterType() == MemoryAdapter.AdapterType.RETROARCH_NWA }
                ?: availableAdapters.firstOrNull { it.getAdapterType() == MemoryAdapter.AdapterType.SNI_GRPC }
        }
    }
}
