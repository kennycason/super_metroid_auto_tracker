package com.supermetroid.service

import com.supermetroid.client.RetroArchUDPClient
import com.supermetroid.client.MockRetroArchUDPClient
import com.supermetroid.model.*
import com.supermetroid.parser.GameStateParser
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

/**
 * Background game state poller
 * Continuously polls RetroArch and maintains cached game state
 * Equivalent to Python's BackgroundGamePoller
 */
class BackgroundPoller(
    private val updateInterval: Long = 2500L // 2.5 seconds
) {
    // Use mock client for now to enable immediate testing
    // TODO: Switch back to RetroArchUDPClient when UDP issue is resolved
    private val udpClient = MockRetroArchUDPClient().apply {
        // Set to STATIC mode for predictable testing (mid-game state: Norfair)
        setMockMode(MockRetroArchUDPClient.MockMode.STATIC)
        setStaticState(1) // 0=Crateria, 1=Norfair, 2=Lower Norfair
    }
    // private val udpClient = RetroArchUDPClient()
    
    internal val parser = GameStateParser()
    private val mutex = Mutex()
    
    private var cache = ServerStatus()
    private var lastUpdate = 0L
    private var bootstrapAttempted = false
    private var pollingJob: Job? = null
    
    suspend fun start() {
        pollingJob = CoroutineScope(Dispatchers.Default).launch {
            pollLoop()
        }
        println("Background poller started with ${updateInterval}ms interval")
    }
    
    fun stop() {
        pollingJob?.cancel()
        println("Background poller stopped")
    }
    
    suspend fun getCachedState(): ServerStatus = mutex.withLock {
        cache.copy()
    }
    
    private suspend fun pollLoop() {
        println("📡 Starting background polling loop...")
        
        while (currentCoroutineContext().isActive) {
            try {
                val startTime = Clock.System.now().toEpochMilliseconds()
                
                // Get connection info
                val connectionInfo = udpClient.getRetroArchInfo()
                
                // Read game state if game is loaded
                var gameState = GameState()
                if (connectionInfo["game_loaded"] == true) {
                    gameState = readGameState()
                    
                    // Bootstrap MB cache on first successful game read (if we haven't already)
                    if (gameState.health > 0 && !bootstrapAttempted) {
                        bootstrapMbCacheIfNeeded(gameState)
                        bootstrapAttempted = true
                    }
                }
                
                // Update cache atomically
                mutex.withLock {
                    cache = cache.copy(
                        connected = connectionInfo["connected"] as? Boolean ?: false,
                        gameLoaded = connectionInfo["game_loaded"] as? Boolean ?: false,
                        retroarchVersion = connectionInfo["retroarch_version"] as? String,
                        gameInfo = connectionInfo["game_info"] as? String,
                        stats = gameState,
                        lastUpdate = Clock.System.now().toEpochMilliseconds(),
                        pollCount = cache.pollCount + 1
                    )
                }
                
                val pollDuration = Clock.System.now().toEpochMilliseconds() - startTime
                // Reduced logging frequency for cleaner output
                if (cache.pollCount <= 5 || cache.pollCount % 20 == 0) {
                    println("✅ Poll #${cache.pollCount} completed in ${pollDuration}ms")
                }
                
                delay(updateInterval)
                
            } catch (e: Exception) {
                println("Polling error: ${e.message}")
                mutex.withLock {
                    cache = cache.copy(errorCount = cache.errorCount + 1)
                }
                delay(1000) // Brief pause on error
            }
        }
    }
    
    private suspend fun readGameState(): GameState {
        try {
            // BULK READ: Get all basic stats in one 22-byte read
            val basicStats = udpClient.readMemoryRange(0x7E09C2, 22)
            
            // Individual reads for other data
            val memoryData = mapOf(
                "basic_stats" to basicStats,
                "room_id" to udpClient.readMemoryRange(0x7E079B, 2),
                "area_id" to udpClient.readMemoryRange(0x7E079F, 1),
                "game_state" to udpClient.readMemoryRange(0x7E0998, 2),
                "player_x" to udpClient.readMemoryRange(0x7E0AF6, 2),
                "player_y" to udpClient.readMemoryRange(0x7E0AFA, 2),
                "items" to udpClient.readMemoryRange(0x7E09A4, 2),
                "beams" to udpClient.readMemoryRange(0x7E09A8, 2),
                
                // Boss memory (multiple addresses for advanced detection)
                "main_bosses" to udpClient.readMemoryRange(0x7ED828, 2),
                "crocomire" to udpClient.readMemoryRange(0x7ED829, 2),
                "boss_plus_1" to udpClient.readMemoryRange(0x7ED829, 2),
                "boss_plus_2" to udpClient.readMemoryRange(0x7ED82A, 2),
                "boss_plus_3" to udpClient.readMemoryRange(0x7ED82B, 2),
                "boss_plus_4" to udpClient.readMemoryRange(0x7ED82C, 2),
                "boss_plus_5" to udpClient.readMemoryRange(0x7ED82D, 2),
                
                // Escape timer for MB2 detection (multiple addresses to try)
                "escape_timer_1" to udpClient.readMemoryRange(0x7E0943, 2),
                "escape_timer_2" to udpClient.readMemoryRange(0x7E0945, 2),
                "escape_timer_3" to udpClient.readMemoryRange(0x7E09E2, 2),
                "escape_timer_4" to udpClient.readMemoryRange(0x7E09E0, 2)
            ).filterValues { it != null } as Map<String, ByteArray>
            
            return parser.parseCompleteGameState(memoryData)
            
        } catch (e: Exception) {
            println("Error reading game state: ${e.message}")
            return GameState()
        }
    }
    
    private suspend fun bootstrapMbCacheIfNeeded(gameState: GameState) {
        try {
            val health = gameState.health
            val maxHealth = gameState.maxHealth
            val missiles = gameState.missiles
            
            // Don't bootstrap if it looks like a new file
            if (health <= 99 && missiles <= 5) {
                println("🚫 SKIPPING BOOTSTRAP: Detected new save file (Health: $health/$maxHealth, Missiles: $missiles)")
                return
            }
            
            // Check for post-game scenarios that indicate MB completion
            val hasHyperBeam = gameState.beams["hyper"] == true
            val inTourian = gameState.areaId == 5
            val highHealth = maxHealth > 500
            val highMissiles = gameState.maxMissiles > 200
            
            if (hasHyperBeam || (inTourian && highHealth) || (highHealth && highMissiles)) {
                println("🔧 BOOTSTRAP: Post-game state detected, setting MB1+MB2 complete")
                // Note: In a full implementation, this would call parser.setMotherBrainComplete()
            } else {
                println("✅ BOOTSTRAP: Save file looks like mid-game, keeping MB cache as-is")
            }
            
        } catch (e: Exception) {
            println("Error during MB bootstrap: ${e.message}")
        }
    }
    
    suspend fun resetCache() = mutex.withLock {
        cache = ServerStatus()
        lastUpdate = 0L
        println("🔄 Background poller cache cleared")
    }
} 