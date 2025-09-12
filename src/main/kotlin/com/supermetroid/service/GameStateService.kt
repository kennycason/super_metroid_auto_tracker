package com.supermetroid.service

import com.supermetroid.gamestate.GameStateParser
import com.supermetroid.model.ConnectionInfo
import com.supermetroid.model.GameState
import com.supermetroid.model.TrackerState
import com.supermetroid.network.DualMemoryAdapter
import com.supermetroid.network.MemoryAdapterDetectionService
import com.supermetroid.network.RetroArchUdpClient
import com.supermetroid.network.SuperMetroidAddresses
import com.supermetroid.network.SuperMetroidMemoryReader
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.time.Duration.Companion.milliseconds

private val logger = KotlinLogging.logger {}

/**
 * Core service for managing game state polling from SNI or RetroArch
 * Maintains connection state and provides real-time game data with automatic adapter detection
 */
class GameStateService(
    private val host: String = "localhost",
    private val port: Int = 55355,
    private val pollIntervalMs: Long = 500 // Much slower polling to prevent chaotic data jumping
) {
    private var lastStableGameState: GameState? = null
    
    // Dual adapter for SNI/RetroArch support
    private val dualAdapter = DualMemoryAdapter(
        MemoryAdapterDetectionService.DetectionPreferences(
            preferSNI = true,
            retroArchHost = host,
            retroArchPort = port
        )
    )
    
    // Legacy support - will be removed once all services are migrated
    private val legacyUdpClient = RetroArchUdpClient(host, port)
    private val legacyMemoryReader = SuperMetroidMemoryReader(legacyUdpClient)
    
    private val gameStateParser = GameStateParser()

    private var pollingJob: Job? = null
    private var pollCount = 0
    private var errorCount = 0

    // State flows for reactive UI updates
    private val _trackerState = MutableStateFlow(
        TrackerState(
            connection = ConnectionInfo(connected = false, gameLoaded = false),
            gameState = GameState(),
            lastUpdate = System.currentTimeMillis(),
            pollCount = 0,
            errorCount = 0
        )
    )
    val trackerState: StateFlow<TrackerState> = _trackerState.asStateFlow()

    /**
     * Start the game state polling service
     */
    suspend fun start() {
        if (pollingJob?.isActive == true) {
            logger.warn { "⚠️ Game state service already running" }
            return
        }

        try {
            logger.info { "🚀 Starting game state service..." }
            
            // Try to connect using the dual adapter first
            val connected = dualAdapter.connect()
            if (connected) {
                logger.info { "✅ Connected via ${dualAdapter.getAdapterName()}" }
            } else {
                logger.warn { "⚠️ No memory adapter available, service will retry connections automatically" }
            }

            updateConnectionState(connected = connected, gameLoaded = false)

            pollingJob = CoroutineScope(Dispatchers.IO).launch {
                pollGameState()
            }

            logger.info { "✅ Game state service started successfully" }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to start game state service" }
            updateConnectionState(connected = false, gameLoaded = false)
            throw e
        }
    }

    /**
     * Stop the game state polling service
     */
    fun stop() {
        logger.info { "🛑 Stopping game state service..." }

        pollingJob?.cancel()
        pollingJob = null

        dualAdapter.disconnect()
        updateConnectionState(connected = false, gameLoaded = false)

        logger.info { "✅ Game state service stopped" }
    }

    /**
     * Simple stability check to reduce erratic value bouncing
     */
        private fun isGameStateStable(gameState: GameState): Boolean {
        // Allow all updates if no previous state
        val lastState = lastStableGameState ?: return true

        // Much more aggressive stability filtering to prevent chaotic jumping
        val healthDiff = kotlin.math.abs(gameState.health - lastState.health)
        val missileDiff = kotlin.math.abs(gameState.missiles - lastState.missiles)
        val maxHealthDiff = kotlin.math.abs(gameState.maxHealth - lastState.maxHealth)

        // Only allow updates if:
        // 1. Changes are very small (normal gameplay)
        // 2. Room changed (significant event)
        // 3. Values are reasonable (no crazy numbers)
        return ((healthDiff < 100 &&
                missileDiff < 50 &&
                maxHealthDiff < 100 &&
                gameState.health >= 0 &&
                gameState.health <= 2000 &&
                gameState.maxHealth >= 0 &&
                gameState.maxHealth <= 2000) ||
                gameState.roomId != lastState.roomId) // Always allow room changes
    }

    /**
     * Main polling loop
     */
    private suspend fun pollGameState() {
        while (currentCoroutineContext().isActive) {
            try {
                // Try to read memory using the dual adapter
                val memoryData = readAllMemoryViaDualAdapter()
                val gameState = gameStateParser.parseGameState(memoryData)

                // Simple data stability: only update if values are reasonable
                val stableGameState = if (isGameStateStable(gameState)) {
                    lastStableGameState = gameState
                    gameState
                } else {
                    lastStableGameState ?: gameState
                }

                pollCount++

                // Determine if game is loaded (room ID > 0 usually indicates loaded game)
                // Use the stable game state to avoid inconsistencies
                val gameLoaded = stableGameState.roomId > 0

                val trackerState = TrackerState(
                    connection = ConnectionInfo(
                        connected = true,
                        gameLoaded = gameLoaded,
                        retroarchVersion = "Unknown", // Could be detected via separate command
                        gameInfo = if (gameLoaded) "Super Metroid (${stableGameState.areaName})" else "No game"
                    ),
                    gameState = stableGameState,
                    lastUpdate = System.currentTimeMillis(),
                    pollCount = pollCount,
                    errorCount = errorCount
                )

                _trackerState.value = trackerState

                // Reduce polling noise - only log significant changes
                if (pollCount % 100L == 0L || gameState.roomId != _trackerState.value.gameState.roomId) {
                    logger.debug { "🔄 Poll #$pollCount: ${gameState.areaName}, Room ${gameState.roomId}, Health ${gameState.health}/${gameState.maxHealth}" }
                }

            } catch (e: Exception) {
                errorCount++
                logger.debug(e) { "❌ Error during game state poll #$pollCount: ${e.message}" }

                // Update error count but keep trying
                val currentState = _trackerState.value
                _trackerState.value = currentState.copy(
                    errorCount = errorCount,
                    lastUpdate = System.currentTimeMillis()
                )

                // If too many consecutive errors, consider disconnected and slow down
                if (errorCount % 5 == 0) {
                    logger.warn { "⚠️ High error count ($errorCount), slowing down polling" }
                    updateConnectionState(connected = false, gameLoaded = false)
                    delay(1000) // Wait longer after errors
                }
            }

            delay(pollIntervalMs.milliseconds)
        }
    }

    /**
     * Update connection state
     */
    private fun updateConnectionState(connected: Boolean, gameLoaded: Boolean) {
        val currentState = _trackerState.value
        _trackerState.value = currentState.copy(
            connection = currentState.connection.copy(
                connected = connected,
                gameLoaded = gameLoaded
            ),
            lastUpdate = System.currentTimeMillis()
        )
    }

    /**
     * Get current connection status
     */
    fun isConnected(): Boolean = _trackerState.value.connection.connected

    /**
     * Get current game state
     */
    fun getCurrentGameState(): GameState = _trackerState.value.gameState

    /**
     * Reset error count
     */
    fun resetErrorCount() {
        errorCount = 0
        val currentState = _trackerState.value
        _trackerState.value = currentState.copy(errorCount = 0)
    }

    /**
     * Get polling statistics
     */
    fun getStats(): PollStats = PollStats(
        pollCount = pollCount,
        errorCount = errorCount,
        successRate = if (pollCount > 0) ((pollCount - errorCount).toDouble() / pollCount * 100) else 0.0,
        isActive = pollingJob?.isActive ?: false
    )

    /**
     * Read all Super Metroid memory addresses using the dual adapter
     */
    private suspend fun readAllMemoryViaDualAdapter(): Map<String, ByteArray> {
        // Define all Super Metroid memory addresses we need to read
        // Using only the addresses that are actually defined in SuperMetroidAddresses
        val addresses = mapOf(
            "health" to (SuperMetroidAddresses.HEALTH to 2),
            "maxHealth" to (SuperMetroidAddresses.MAX_HEALTH to 2),
            "missiles" to (SuperMetroidAddresses.MISSILES to 2),
            "maxMissiles" to (SuperMetroidAddresses.MAX_MISSILES to 2),
            "supers" to (SuperMetroidAddresses.SUPERS to 2),
            "maxSupers" to (SuperMetroidAddresses.MAX_SUPERS to 2),
            "powerBombs" to (SuperMetroidAddresses.POWER_BOMBS to 2),
            "maxPowerBombs" to (SuperMetroidAddresses.MAX_POWER_BOMBS to 2),
            "reserveEnergy" to (SuperMetroidAddresses.RESERVE_ENERGY to 2),
            "maxReserveEnergy" to (SuperMetroidAddresses.MAX_RESERVE_ENERGY to 2),
            "roomId" to (SuperMetroidAddresses.ROOM_ID to 2),
            "areaId" to (SuperMetroidAddresses.AREA_ID to 1),
            "gameState" to (SuperMetroidAddresses.GAME_STATE to 2),
            "playerX" to (SuperMetroidAddresses.PLAYER_X to 2),
            "playerY" to (SuperMetroidAddresses.PLAYER_Y to 2),
            "collectedItems" to (SuperMetroidAddresses.COLLECTED_ITEMS to 2),
            "collectedBeams" to (SuperMetroidAddresses.COLLECTED_BEAMS to 2),
            "bossFlags1" to (SuperMetroidAddresses.BOSS_FLAGS_1 to 2),
            "bossFlags2" to (SuperMetroidAddresses.BOSS_FLAGS_2 to 2),
            "bossFlags3" to (SuperMetroidAddresses.BOSS_FLAGS_3 to 2),
            "bossFlags4" to (SuperMetroidAddresses.BOSS_FLAGS_4 to 2),
            "bossFlags5" to (SuperMetroidAddresses.BOSS_FLAGS_5 to 2),
            "tourianBosses" to (SuperMetroidAddresses.TOURIAN_BOSSES to 2),
            "ceresBosses" to (SuperMetroidAddresses.CERES_BOSSES to 2),
            "eventFlags" to (SuperMetroidAddresses.EVENT_FLAGS to 2),
            // Special addresses
            "motherBrainHp" to (SuperMetroidAddresses.MOTHER_BRAIN_HP to 2),
            "shipAi" to (SuperMetroidAddresses.SHIP_AI to 2),
            // Escape timer addresses
            "escapeTimer1" to (SuperMetroidAddresses.ESCAPE_TIMER_1 to 2),
            "escapeTimer2" to (SuperMetroidAddresses.ESCAPE_TIMER_2 to 2),
            "escapeTimer3" to (SuperMetroidAddresses.ESCAPE_TIMER_3 to 2),
            "escapeTimer4" to (SuperMetroidAddresses.ESCAPE_TIMER_4 to 2)
        )

        return try {
            dualAdapter.readMemoryBatch(addresses)
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to read memory via dual adapter, falling back to legacy method" }
            
            // Fallback to legacy method if dual adapter fails
            try {
                // Ensure legacy client is connected
                if (!legacyUdpClient.isConnected()) {
                    legacyUdpClient.connect()
                }
                legacyMemoryReader.readAllMemory()
            } catch (legacyError: Exception) {
                logger.error(legacyError) { "❌ Legacy fallback also failed" }
                throw e // Throw original error
            }
        }
    }

    /**
     * Get the dual memory adapter
     */
    fun getDualAdapter(): DualMemoryAdapter = dualAdapter
    
    /**
     * Get the UDP client for direct memory access (legacy compatibility)
     * Used by other services that need to read/write memory
     * TODO: Migrate all services to use the dual adapter
     */
    fun getUdpClient(): RetroArchUdpClient {
        // Try to return the active adapter's UDP client if it's RetroArch
        return dualAdapter.getRetroArchUdpClient() ?: legacyUdpClient
    }
}

/**
 * Statistics about polling performance
 */
data class PollStats(
    val pollCount: Int,
    val errorCount: Int,
    val successRate: Double,
    val isActive: Boolean
)
