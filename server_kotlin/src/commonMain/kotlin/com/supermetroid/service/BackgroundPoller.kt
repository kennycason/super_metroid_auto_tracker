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
    // Use real UDP client for production
    private val udpClient = RetroArchUDPClient()

    // For testing, uncomment this and comment out the real client above
    // private val udpClient = MockRetroArchUDPClient().apply {
    //     setMockMode(MockRetroArchUDPClient.MockMode.STATIC)
    //     setStaticState(1) // 0=Crateria, 1=Norfair, 2=Lower Norfair
    // }

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
                println("🔄 Poll: Getting RetroArch info...")
                val connectionInfo = udpClient.getRetroArchInfo()
                println("🔄 Poll: RetroArch info received: connected=${connectionInfo["connected"]}, game_loaded=${connectionInfo["game_loaded"]}")

                // Read game state if game is loaded
                var gameState = GameState()
                if (connectionInfo["game_loaded"] == true) {
                    println("🔄 Poll: Game is loaded, reading game state...")
                    gameState = readGameState()
                    println("🔄 Poll: Game state read: health=${gameState.health}, missiles=${gameState.missiles}, area=${gameState.areaName}")

                    // Bootstrap MB cache on first successful game read (if we haven't already)
                    if (gameState.health > 0 && !bootstrapAttempted) {
                        println("🔄 Poll: First successful game read with health > 0, bootstrapping MB cache...")
                        bootstrapMbCacheIfNeeded(gameState)
                        bootstrapAttempted = true
                    }
                } else {
                    println("🔄 Poll: Game is not loaded, skipping game state read")
                }

                // Update cache atomically
                println("🔄 Poll: Updating cache...")
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
                println("🔄 Poll: Cache updated: connected=${cache.connected}, game_loaded=${cache.gameLoaded}, poll_count=${cache.pollCount}")

                val pollDuration = Clock.System.now().toEpochMilliseconds() - startTime
                // Reduced logging frequency for cleaner output
                if (cache.pollCount <= 5 || cache.pollCount % 20 == 0) {
                    println("✅ Poll #${cache.pollCount} completed in ${pollDuration}ms")
                }

                println("🔄 Poll: Waiting ${updateInterval}ms for next poll...")
                delay(updateInterval)

            } catch (e: Exception) {
                println("❌ Polling error: ${e.message}")
                e.printStackTrace()
                mutex.withLock {
                    cache = cache.copy(errorCount = cache.errorCount + 1)
                }
                println("🔄 Poll: Error count increased to ${cache.errorCount}, pausing for 1000ms...")
                delay(1000) // Brief pause on error
            }
        }
    }

    private suspend fun readGameState(): GameState {
        try {
            println("🔄 ReadGameState: Reading game state from RetroArch...")

            // BULK READ: Get all basic stats in one 22-byte read
            println("🔄 ReadGameState: Reading basic stats (0x7E09C2, 22 bytes)...")
            val basicStats = udpClient.readMemoryRange(0x7E09C2, 22)
            if (basicStats == null) {
                println("❌ ReadGameState: Failed to read basic stats")
                return GameState()
            }
            println("🔄 ReadGameState: Basic stats read successfully (${basicStats.size} bytes)")

            // Individual reads for other data
            println("🔄 ReadGameState: Reading other memory addresses...")

            println("🔄 ReadGameState: Reading room_id (0x7E079B)...")
            val roomId = udpClient.readMemoryRange(0x7E079B, 2)

            println("🔄 ReadGameState: Reading area_id (0x7E079F)...")
            val areaId = udpClient.readMemoryRange(0x7E079F, 1)

            println("🔄 ReadGameState: Reading game_state (0x7E0998)...")
            val gameState = udpClient.readMemoryRange(0x7E0998, 2)

            println("🔄 ReadGameState: Reading player_x (0x7E0AF6)...")
            val playerX = udpClient.readMemoryRange(0x7E0AF6, 2)

            println("🔄 ReadGameState: Reading player_y (0x7E0AFA)...")
            val playerY = udpClient.readMemoryRange(0x7E0AFA, 2)

            println("🔄 ReadGameState: Reading items (0x7E09A4)...")
            val items = udpClient.readMemoryRange(0x7E09A4, 2)

            println("🔄 ReadGameState: Reading beams (0x7E09A8)...")
            val beams = udpClient.readMemoryRange(0x7E09A8, 2)

            println("🔄 ReadGameState: Reading boss data...")
            println("🔄 ReadGameState: Reading main_bosses (0x7ED828)...")
            val mainBosses = udpClient.readMemoryRange(0x7ED828, 2)

            println("🔄 ReadGameState: Reading crocomire (0x7ED829)...")
            val crocomire = udpClient.readMemoryRange(0x7ED829, 2)

            println("🔄 ReadGameState: Reading boss_plus_1 (0x7ED829)...")
            val bossPlus1 = udpClient.readMemoryRange(0x7ED829, 2)

            println("🔄 ReadGameState: Reading boss_plus_2 (0x7ED82A)...")
            val bossPlus2 = udpClient.readMemoryRange(0x7ED82A, 2)

            println("🔄 ReadGameState: Reading boss_plus_3 (0x7ED82B)...")
            val bossPlus3 = udpClient.readMemoryRange(0x7ED82B, 2)

            println("🔄 ReadGameState: Reading boss_plus_4 (0x7ED82C)...")
            val bossPlus4 = udpClient.readMemoryRange(0x7ED82C, 2)

            println("🔄 ReadGameState: Reading boss_plus_5 (0x7ED82D)...")
            val bossPlus5 = udpClient.readMemoryRange(0x7ED82D, 2)

            println("🔄 ReadGameState: Reading escape timers...")
            println("🔄 ReadGameState: Reading escape_timer_1 (0x7E0943)...")
            val escapeTimer1 = udpClient.readMemoryRange(0x7E0943, 2)

            println("🔄 ReadGameState: Reading escape_timer_2 (0x7E0945)...")
            val escapeTimer2 = udpClient.readMemoryRange(0x7E0945, 2)

            println("🔄 ReadGameState: Reading escape_timer_3 (0x7E09E2)...")
            val escapeTimer3 = udpClient.readMemoryRange(0x7E09E2, 2)

            println("🔄 ReadGameState: Reading escape_timer_4 (0x7E09E0)...")
            val escapeTimer4 = udpClient.readMemoryRange(0x7E09E0, 2)

            // Read end game detection memory addresses
            println("🔄 ReadGameState: Reading ship_ai (0x7E0FB2)...")
            val shipAi = udpClient.readMemoryRange(0x7E0FB2, 2)

            println("🔄 ReadGameState: Reading event_flags (0x7ED821)...")
            val eventFlags = udpClient.readMemoryRange(0x7ED821, 1)

            // Create memory data map
            println("🔄 ReadGameState: Creating memory data map...")
            val memoryData = mapOf(
                "basic_stats" to basicStats,
                "room_id" to roomId,
                "area_id" to areaId,
                "game_state" to gameState,
                "player_x" to playerX,
                "player_y" to playerY,
                "items" to items,
                "beams" to beams,
                "main_bosses" to mainBosses,
                "crocomire" to crocomire,
                "boss_plus_1" to bossPlus1,
                "boss_plus_2" to bossPlus2,
                "boss_plus_3" to bossPlus3,
                "boss_plus_4" to bossPlus4,
                "boss_plus_5" to bossPlus5,
                "escape_timer_1" to escapeTimer1,
                "escape_timer_2" to escapeTimer2,
                "escape_timer_3" to escapeTimer3,
                "escape_timer_4" to escapeTimer4,
                "ship_ai" to shipAi,
                "event_flags" to eventFlags
            ).filterValues { it != null }.mapValues { (_, value) -> value!! }

            println("🔄 ReadGameState: Memory data map created with ${memoryData.size} entries")

            // Parse game state
            println("🔄 ReadGameState: Parsing game state...")
            val parsedGameState = parser.parseCompleteGameState(memoryData)
            println("🔄 ReadGameState: Game state parsed successfully")

            return parsedGameState

        } catch (e: Exception) {
            println("❌ ReadGameState Error: ${e.message}")
            e.printStackTrace()
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
        bootstrapAttempted = false

        // Also reset the Mother Brain cache in the parser
        try {
            parser.resetMotherBrainCache()
            println("🧠 Mother Brain cache reset")
        } catch (e: Exception) {
            println("⚠️ Failed to reset Mother Brain cache: ${e.message}")
        }

        println("🔄 Background poller cache fully reset")
    }
}
