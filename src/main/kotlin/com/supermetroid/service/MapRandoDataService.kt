package com.supermetroid.service

import com.supermetroid.model.MapRandoSettings
import com.supermetroid.model.MapRandoApiResponse
import com.supermetroid.model.GameState
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Service for reading Map Randomizer seed data from memory and API
 * 
 * Memory addresses:
 * - $dffef0: Seed name (16 bytes, null-terminated ASCII)
 * - $701E16: Death count (2 bytes, little-endian)
 * 
 * API endpoint: GET https://maprando.com/api/seed/{seed_name}
 */
class MapRandoDataService {
    private val _settings = MutableStateFlow(MapRandoSettings())
    val settings: StateFlow<MapRandoSettings> = _settings.asStateFlow()

    private var lastSeedName: String? = null
    private var cachedSettings: MapRandoSettings? = null
    
    // Use a dedicated scope that won't be cancelled by Compose
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Track active fetch jobs to avoid duplicate requests
    private var activeFetchJob: Job? = null

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    /**
     * Update Map Rando settings from game state
     * Reads seed name and death count from memory, then fetches full settings from API
     * 
     * This runs in a dedicated service scope to avoid cancellation by Compose recomposition
     */
    fun updateFromGameState(gameState: GameState) {
        try {
            // Read seed name from memory ($dffef0)
            val seedName = readSeedNameFromMemory(gameState)
            
            // Read death count from memory ($701E16)
            val deathCount = readDeathCountFromMemory(gameState)

            // If seed name changed, fetch new settings from API (in service scope)
            if (seedName != lastSeedName) {
                lastSeedName = seedName
                
                if (seedName.isNotEmpty()) {
                    logger.info { "🎲 Detected Map Rando seed: $seedName" }
                    
                    // Cancel any existing fetch and start a new one
                    activeFetchJob?.cancel()
                    activeFetchJob = serviceScope.launch {
                        try {
                            val apiSettings = fetchSeedDataFromApi(seedName)
                            cachedSettings = apiSettings.copy(deathCount = deathCount)
                            _settings.value = cachedSettings ?: MapRandoSettings()
                        } catch (e: CancellationException) {
                            logger.debug { "🚫 Seed fetch cancelled (expected during shutdown)" }
                        } catch (e: Exception) {
                            logger.error(e) { "❌ Failed to fetch seed data" }
                            cachedSettings = MapRandoSettings(seedName = seedName, deathCount = deathCount)
                            _settings.value = cachedSettings ?: MapRandoSettings()
                        }
                    }
                } else {
                    logger.debug { "📭 No Map Rando seed detected" }
                    cachedSettings = MapRandoSettings(deathCount = deathCount)
                    _settings.value = cachedSettings ?: MapRandoSettings()
                }
            } else {
                // Update only death count if seed hasn't changed
                cachedSettings?.let {
                    _settings.value = it.copy(deathCount = deathCount)
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to update Map Rando settings" }
        }
    }

    /**
     * Read seed name from memory at $dffef0 (16 bytes, null-terminated)
     */
    private fun readSeedNameFromMemory(gameState: GameState): String {
        // TODO: Implement actual memory read via SNI
        // TEMPORARY TEST MODE: Hardcode a seed for testing
        // Remove this and uncomment the return "" when SNI integration is ready
        return "pFSJBxm7R" // TEST SEED - User's example seed
        
        // For now, return empty string (will be implemented when SNI integration is added)
        // return ""
    }

    /**
     * Read death count from SRAM at $701E16 (2 bytes, little-endian)
     */
    private fun readDeathCountFromMemory(gameState: GameState): Int {
        // TODO: Implement actual memory read via SNI
        // TEMPORARY TEST MODE: Hardcode a death count for testing
        return 42 // TEST VALUE
        
        // For now, return 0 (will be implemented when SNI integration is added)
        // return 0
    }

    /**
     * Fetch seed metadata from maprando.com HTML page
     * Example: GET https://maprando.com/seed/pFSJBxm7R (follows 302 redirect)
     * 
     * Note: There's no JSON API, we parse the HTML directly
     */
    private suspend fun fetchSeedDataFromApi(seedName: String): MapRandoSettings {
        return try {
            logger.info { "🌐 Fetching Map Rando seed data from HTML: $seedName" }
            
            val response: HttpResponse = httpClient.get("https://maprando.com/seed/$seedName")
            
            if (response.status == HttpStatusCode.OK) {
                val html = response.bodyAsText()
                logger.info { "✅ Successfully fetched seed page for $seedName" }
                
                // Parse HTML to extract settings
                // HTML format: <div class="col-5 col-sm-4 col-md-3">Skill assumption:</div>
                //              <div class="col-7 col-sm-8 col-md-9">Basic</div>
                val objectives = extractSetting(html, "Objectives:")
                val difficulty = extractSetting(html, "Skill assumption:")
                val itemProgression = extractSetting(html, "Item progression:")
                val qualityOfLife = extractSetting(html, "Quality-of-life options:")
                val mapLayout = extractSetting(html, "Map layout:")
                val doorsMode = extractSetting(html, "Doors:")
                val startLocation = extractSetting(html, "Start location:")
                val saveAnimals = extractSetting(html, "Save the animals:")
                
                MapRandoSettings(
                    seedName = seedName,
                    objectives = objectives,
                    difficulty = difficulty,
                    itemProgression = itemProgression,
                    qualityOfLife = qualityOfLife,
                    mapLayout = mapLayout,
                    doorsMode = doorsMode,
                    startLocation = startLocation,
                    saveAnimals = saveAnimals,
                    version = "",
                    deathCount = 0 // Will be set from memory
                )
            } else {
                logger.warn { "⚠️ Seed page returned status ${response.status} for seed $seedName" }
                MapRandoSettings(seedName = seedName)
            }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to fetch seed data for $seedName" }
            // Return placeholder settings on error (no internet, etc.)
            MapRandoSettings(seedName = seedName)
        }
    }
    
    /**
     * Extract a setting value from the HTML
     * Looks for pattern: <div class="col-5...">Label:</div><div class="col-7...">Value</div>
     */
    private fun extractSetting(html: String, label: String): String {
        return try {
            // Regex to find the settings section (col-5/col-7 pattern)
            // This avoids matching the wrong section (col-8/col-4)
            val pattern = """<div class="col-5[^"]*">$label</div>\s*<div class="col-7[^"]*">([^<]+)</div>""".toRegex()
            val match = pattern.find(html)
            match?.groupValues?.get(1)?.trim() ?: "OFF"
        } catch (e: Exception) {
            logger.warn(e) { "⚠️ Failed to extract setting: $label" }
            "OFF"
        }
    }

    /**
     * Clean up resources
     */
    fun close() {
        activeFetchJob?.cancel()
        serviceScope.cancel()
        httpClient.close()
    }
}

