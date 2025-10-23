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
import com.supermetroid.util.Logging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.time.Duration.Companion.milliseconds

/**
 * Core service for managing game state polling from SNI or RetroArch
 * Maintains connection state and provides real-time game data with automatic adapter detection
 */
class GameStateService(
    private val host: String = "localhost",
    private val port: Int = 55355,
    private val pollIntervalMs: Long = 500 // Much slower polling to prevent chaotic data jumping
) : Logging {
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
                logger.info { "🔧 Adapter Type: ${dualAdapter.getAdapterType()}" }
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

    suspend fun reconnectNow() {
        logger.info { "🔄 Manual reconnect requested" }
        try {
            resetErrorCount()
            val success = dualAdapter.forceRedetection()
            updateConnectionState(connected = success, gameLoaded = false)
            if (pollingJob?.isActive != true) {
                pollingJob = CoroutineScope(Dispatchers.IO).launch {
                    pollGameState()
                }
            }
            logger.info { if (success) "✅ Reconnected" else "⚠️ Reconnect attempt did not find any adapter" }
        } catch (e: Exception) {
            logger.error(e) { "❌ Manual reconnect failed" }
        }
    }

    /**
     * Improved stability check that allows item collection and save file changes
     */
    private fun isGameStateStable(gameState: GameState): Boolean {
        // Allow all updates if no previous state
        val lastState = lastStableGameState ?: return true

        // Basic sanity checks - reject obviously corrupted data
        if (gameState.health < 0 || gameState.health > 2000 ||
            gameState.maxHealth < 0 || gameState.maxHealth > 1999 ||
            gameState.missiles < 0 || gameState.missiles > 999 ||
            gameState.maxMissiles < 0 || gameState.maxMissiles > 999 ||
            gameState.supers < 0 || gameState.supers > 999 ||
            gameState.maxSupers < 0 || gameState.maxSupers > 999 ||
            gameState.powerBombs < 0 || gameState.powerBombs > 999 ||
            gameState.maxPowerBombs < 0 || gameState.maxPowerBombs > 999) {
            logger.debug { "🔄 Rejecting unstable data: Health=${gameState.health}/${gameState.maxHealth}, Missiles=${gameState.missiles}/${gameState.maxMissiles}" }
            return false
        }

        // Calculate differences
        val healthDiff = kotlin.math.abs(gameState.health - lastState.health)
        val missileDiff = kotlin.math.abs(gameState.missiles - lastState.missiles)
        val maxHealthDiff = kotlin.math.abs(gameState.maxHealth - lastState.maxHealth)
        val maxMissilesDiff = kotlin.math.abs(gameState.maxMissiles - lastState.maxMissiles)
        val maxSupersDiff = kotlin.math.abs(gameState.maxSupers - lastState.maxSupers)
        val maxPowerBombsDiff = kotlin.math.abs(gameState.maxPowerBombs - lastState.maxPowerBombs)

        // Allow updates in these cases:
        // 1. Room changed (always allow)
        // 2. Small changes (normal gameplay)
        // 3. Item collection patterns (specific large increases in max values)
        // 4. Save file loading (dramatic but consistent changes)

        val roomChanged = gameState.roomId != lastState.roomId
        val smallChanges = healthDiff < 100 && missileDiff < 50 && maxHealthDiff < 100
        val itemCollection = (
            maxHealthDiff in 1..200 ||      // Energy Tanks give +100, allow some tolerance
            maxMissilesDiff in 1..50 ||     // Missile expansions
            maxSupersDiff in 1..20 ||       // Super Missile expansions
            maxPowerBombsDiff in 1..20      // Power Bomb expansions
        )
        val saveFileLoad = (
            maxHealthDiff > 200 || maxMissilesDiff > 50 ||
            maxSupersDiff > 20 || maxPowerBombsDiff > 20
        ) && gameState.roomId == lastState.roomId  // Same room but big stat changes = save file change

        val isStable = roomChanged || smallChanges || itemCollection || saveFileLoad

        if (!isStable) {
            logger.debug { "🔄 Rejecting change - Health: ${lastState.health}->${gameState.health} (Δ$healthDiff), MaxHealth: ${lastState.maxHealth}->${gameState.maxHealth} (Δ$maxHealthDiff), Room: ${lastState.roomId}->${gameState.roomId}" }
        } else {
            // Only log changes that are actually significant
            val hasActualChanges = gameState.health != lastState.health ||
                gameState.maxHealth != lastState.maxHealth ||
                gameState.missiles != lastState.missiles ||
                gameState.maxMissiles != lastState.maxMissiles ||
                gameState.supers != lastState.supers ||
                gameState.maxSupers != lastState.maxSupers ||
                gameState.powerBombs != lastState.powerBombs ||
                gameState.maxPowerBombs != lastState.maxPowerBombs ||
                gameState.roomId != lastState.roomId ||
                gameState.items != lastState.items ||
                gameState.beams != lastState.beams ||
                gameState.bosses != lastState.bosses

            if (hasActualChanges) {
                logger.info { "✅ SIGNIFICANT CHANGE - Health: ${lastState.health}->${gameState.health}, MaxHealth: ${lastState.maxHealth}->${gameState.maxHealth}, Missiles: ${lastState.missiles}/${lastState.maxMissiles}->${gameState.missiles}/${gameState.maxMissiles}, Room: ${lastState.roomId}->${gameState.roomId}" }
            } else {
                logger.debug { "✅ Accepting change - Health: ${lastState.health}->${gameState.health}, MaxHealth: ${lastState.maxHealth}->${gameState.maxHealth}, Room: ${lastState.roomId}->${gameState.roomId}" }
            }
        }

        return isStable
    }

    /**
     * Main polling loop
     */
    private suspend fun pollGameState() {
        var currentDelayMs = pollIntervalMs
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

                // Determine if game is loaded using robust heuristics
                // - Valid gameplay states (normal/door/elevator/Ceres cutscene)
                // - Known area IDs (0..6) with any non-zero capacity
                // - Or a positive roomId
                val gameLoaded = isGameLoaded(stableGameState)

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

                // Log significant changes and periodic status
                val previousState = _trackerState.value.gameState
                val significantChange = gameState.roomId != previousState.roomId ||
                    gameState.maxHealth != previousState.maxHealth ||
                    gameState.maxMissiles != previousState.maxMissiles ||
                    gameState.maxSupers != previousState.maxSupers ||
                    gameState.maxPowerBombs != previousState.maxPowerBombs

                if (pollCount % 100L == 0L || significantChange) {
                    logger.info { "🔄 Poll #$pollCount: ${gameState.areaName}, Room ${gameState.roomId}, Health ${gameState.health}/${gameState.maxHealth}, Missiles ${gameState.missiles}/${gameState.maxMissiles}, Supers ${gameState.supers}/${gameState.maxSupers}, PBs ${gameState.powerBombs}/${gameState.maxPowerBombs}" }
                }
                // Reset backoff on successful poll
                currentDelayMs = pollIntervalMs

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
                // Exponential backoff on errors (max 5000ms)
                currentDelayMs = (currentDelayMs * 2).coerceAtMost(5000)
            }

            delay(currentDelayMs.milliseconds)
        }
    }

    /**
     * Determine if the game is actually loaded and active.
     * Heuristics:
     * - Valid gameplay states (normal gameplay, door or elevator transitions, Ceres cutscene)
     * - Known area IDs (0..6) with any non-zero capacity (health/missiles/supers/power bombs)
     * - Or a positive roomId
     */
    private fun isGameLoaded(state: GameState): Boolean {
        val validGameplay =
            state.gameState == com.supermetroid.gamestate.GameStateConstants.NORMAL_GAMEPLAY ||
            state.gameState == com.supermetroid.gamestate.GameStateConstants.DOOR_TRANSITION ||
            state.gameState == com.supermetroid.gamestate.GameStateConstants.ELEVATOR ||
            state.gameState == com.supermetroid.gamestate.GameStateConstants.START_OF_CERES_CUTSCENE ||
            state.gameState == 34 // Ceres continuation
        val validArea = state.areaId in 0..6

        // Any non-zero capacity or current ammo/health implies a loaded save/game
        val hasCapacities = state.maxHealth > 0 || state.maxMissiles > 0 || state.maxSupers > 0 || state.maxPowerBombs > 0
        val anyCombatValues = state.health > 0 || state.missiles > 0 || state.supers > 0 || state.powerBombs > 0

        // Event/boss flags also indicate a running game even if roomId is temporarily 0
        val anyFlags = state.eventFlags != 0 || state.tourianBosses != 0 || state.bosses.ceresStation

        // Consider loaded if:
        // - We have a positive roomId (normal case), or
        // - We're in a valid gameplay state, or
        // - We're in a known area and see any capacities/combat values/flags
        return state.roomId > 0 || validGameplay || (validArea && (hasCapacities || anyCombatValues || anyFlags))
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
