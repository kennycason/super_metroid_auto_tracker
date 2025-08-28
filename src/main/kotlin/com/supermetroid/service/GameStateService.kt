package com.supermetroid.service

import com.supermetroid.gamestate.GameStateParser
import com.supermetroid.model.ConnectionInfo
import com.supermetroid.model.GameState
import com.supermetroid.model.TrackerState
import com.supermetroid.network.RetroArchUdpClient
import com.supermetroid.network.SuperMetroidMemoryReader
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.time.Duration.Companion.milliseconds

private val logger = KotlinLogging.logger {}

/**
 * Core service for managing game state polling from RetroArch
 * Maintains connection state and provides real-time game data
 */
class GameStateService(
    private val host: String = "localhost",
    private val port: Int = 55355,
    private val pollIntervalMs: Long = 500 // Much slower polling to prevent chaotic data jumping
) {
    private var lastStableGameState: GameState? = null
    private val udpClient = RetroArchUdpClient(host, port)
    private val memoryReader = SuperMetroidMemoryReader(udpClient)
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
            udpClient.connect()
            
            updateConnectionState(connected = true, gameLoaded = false)
            
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
        
        udpClient.disconnect()
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
                val memoryData = memoryReader.readAllMemory()
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
                val gameLoaded = gameState.roomId > 0
                
                val trackerState = TrackerState(
                    connection = ConnectionInfo(
                        connected = true,
                        gameLoaded = gameLoaded,
                        retroarchVersion = "Unknown", // Could be detected via separate command
                        gameInfo = if (gameLoaded) "Super Metroid (${gameState.areaName})" else "No game loaded"
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


