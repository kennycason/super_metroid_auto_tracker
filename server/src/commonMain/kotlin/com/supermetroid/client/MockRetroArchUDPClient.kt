package com.supermetroid.client

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

/**
 * Mock RetroArch UDP client for testing and development
 * Provides realistic game state data matching the Python server format
 */
class MockRetroArchUDPClient(
    private val host: String = "localhost",
    private val port: Int = 55355
) {
    private val mutex = Mutex()
    private var connected = true
    private var pollCount = 0
    
    // Configuration for mock behavior
    private var mockMode = MockMode.CYCLING // Can be STATIC, SLOW_CYCLING, or CYCLING
    private var staticStateIndex = 1 // Which state to use for STATIC mode (0=early, 1=mid, 2=late)
    
    enum class MockMode {
        STATIC,       // Always return the same state
        SLOW_CYCLING, // Change state every 10 polls
        CYCLING       // Change state every poll (current behavior)
    }
    
    // EXACT Python server reference data - matches python_server_reference.json perfectly
    private val gameStates = listOf(
        // Early game state
        mapOf(
            "health" to 199, "max_health" to 299, "missiles" to 15, "max_missiles" to 20,
            "area_id" to 1, "area_name" to "Crateria", "room_id" to 12345,
            "morph" to true, "bombs" to true, "charge" to true, "ice" to false,
            "varia" to false, "hijump" to false, "speed" to false, "space" to false,
            "kraid" to false, "ridley" to false, "bomb_torizo" to true
        ),
        // Mid game state - EXACT match to python_server_reference.json
        mapOf(
            "health" to 516, "max_health" to 599, "missiles" to 32, "max_missiles" to 45,
            "area_id" to 2, "area_name" to "Norfair", "room_id" to 38662,
            "supers" to 5, "max_supers" to 5, "power_bombs" to 0, "max_power_bombs" to 0,
            "player_x" to 128, "player_y" to 152, "game_state" to 15,
            // Items - exact match
            "morph" to true, "bombs" to true, "varia" to true, "gravity" to false,
            "hijump" to true, "speed" to false, "space" to false, "screw" to false,
            "spring" to false, "xray" to false, "grapple" to false,
            // Beams - exact match
            "charge" to true, "ice" to true, "wave" to true, "spazer" to false,
            "plasma" to false, "hyper" to false,
            // Bosses - FIXED to match python_server_reference.json exactly
            "bomb_torizo" to true, "kraid" to false, "spore_spawn" to true,
            "draygon" to false, "mother_brain" to true, "crocomire" to true,
            "phantoon" to false, "botwoon" to false, "ridley" to false,
            "golden_torizo" to false, "mother_brain_1" to false, "mother_brain_2" to false,
            "samus_ship" to false
        ),
        // Late game state
        mapOf(
            "health" to 799, "max_health" to 1399, "missiles" to 85, "max_missiles" to 230,
            "area_id" to 4, "area_name" to "Lower Norfair", "room_id" to 55443,
            "morph" to true, "bombs" to true, "varia" to true, "gravity" to true, "space" to true,
            "charge" to true, "ice" to true, "wave" to true, "plasma" to true,
            "kraid" to true, "ridley" to true, "bomb_torizo" to true, "spore_spawn" to true, "crocomire" to true
        )
    )
    
    // Methods to control mock behavior
    fun setMockMode(mode: MockMode) {
        mockMode = mode
        println("🎭 Mock UDP: Mode changed to $mode")
    }
    
    fun setStaticState(index: Int) {
        if (index in 0..2) {
            staticStateIndex = index
            println("🎭 Mock UDP: Static state set to ${gameStates[index]["area_name"]}")
        }
    }
    
    private fun getCurrentStateIndex(): Int {
        return when (mockMode) {
            MockMode.STATIC -> staticStateIndex
            MockMode.SLOW_CYCLING -> (pollCount / 10) % gameStates.size
            MockMode.CYCLING -> pollCount % gameStates.size
        }
    }
    
    suspend fun connect(): Boolean = mutex.withLock {
        connected = true
        println("🎭 Mock UDP: Connected to simulated RetroArch (Mode: $mockMode)")
        return true
    }
    
    suspend fun sendCommand(command: String): String? = mutex.withLock {
        if (mockMode == MockMode.STATIC) {
            // Don't spam logs in static mode
        } else {
            println("🎭 Mock UDP: Sending command '$command'")
        }
        
        return when (command) {
            "VERSION" -> {
                "1.21.0"
            }
            "GET_STATUS" -> {
                "GET_STATUS PLAYING super_nes,Super Metroid Z-Factor,crc32=cea89814"
            }
            else -> {
                if (command.startsWith("READ_CORE_MEMORY")) {
                    val parts = command.split(" ")
                    if (parts.size >= 3) {
                        val address = parts[1].removePrefix("0x").toIntOrNull(16) ?: 0
                        val size = parts[2].toIntOrNull() ?: 0
                        generateMockMemoryData(address, size)
                    } else null
                } else null
            }
        }
    }

    private fun generateMockMemoryData(address: Int, size: Int): String {
        // Get current state based on mode
        val currentState = gameStates[getCurrentStateIndex()]
        pollCount++
        
        // Generate hex data based on address
        val hexData = when (address) {
            0x7E09C2 -> generateBasicStatsData(currentState) // Basic stats
            0x7E079B -> generateRoomIdData(currentState) // Room ID
            0x7E079F -> generateAreaIdData(currentState) // Area ID
            0x7E0998 -> generateGameStateData(currentState) // Game state
            0x7E0AF6 -> generatePlayerXData(currentState) // Player X
            0x7E0AFA -> generatePlayerYData(currentState) // Player Y
            0x7E09A4 -> generateItemsData(currentState) // Items
            0x7E09A8 -> generateBeamsData(currentState) // Beams
            0x7ED828 -> generateBossData(currentState) // Main bosses
            0x7ED82C -> generateCrocomireData(currentState) // Crocomire (separate address)
            else -> "00".repeat(size) // Default empty data
        }
        
        return "READ_CORE_MEMORY 0x${address.toString(16).uppercase()} $hexData"
    }
    
    private fun generateBasicStatsData(state: Map<String, Any>): String {
        // 22-byte basic stats - EXACT match to Python server reference  
        // Python: health=516, max_health=599, missiles=32, max_missiles=45, supers=5
        
        val health = (state["health"] as? Int) ?: 99
        val maxHealth = (state["max_health"] as? Int) ?: 99
        val missiles = (state["missiles"] as? Int) ?: 0
        val maxMissiles = (state["max_missiles"] as? Int) ?: 0
        val supers = (state["supers"] as? Int) ?: 5
        val maxSupers = (state["max_supers"] as? Int) ?: 5
        val powerBombs = (state["power_bombs"] as? Int) ?: 0
        val maxPowerBombs = (state["max_power_bombs"] as? Int) ?: 0
        
        // Convert to little-endian hex (low byte first)
        fun toLittleEndianHex(value: Int): String {
            val lowByte = value and 0xFF
            val highByte = (value shr 8) and 0xFF
            return "${lowByte.toString(16).padStart(2, '0')}${highByte.toString(16).padStart(2, '0')}"
        }
        
        return listOf(
            toLittleEndianHex(health), // Health as little-endian
            toLittleEndianHex(maxHealth), // Max health as little-endian
            toLittleEndianHex(missiles), // Missiles as little-endian
            toLittleEndianHex(maxMissiles), // Max missiles as little-endian
            toLittleEndianHex(supers), // Supers as little-endian
            toLittleEndianHex(maxSupers), // Max supers as little-endian
            toLittleEndianHex(powerBombs), // Power bombs as little-endian
            toLittleEndianHex(maxPowerBombs), // Max power bombs as little-endian
            "0000", // Reserve energy
            "0000", // Max reserve energy
            "0000"  // Padding
        ).joinToString("")
    }
    
    private fun generateItemsData(state: Map<String, Any>): String {
        // Create items bitfield to match Python reference exactly
        // Python target: morph=true, bombs=true, varia=true, hijump=true
        var itemBits = 0u
        
        if (state["morph"] == true) itemBits = itemBits or 0x04u
        if (state["bombs"] == true) itemBits = itemBits or 0x1000u
        if (state["varia"] == true) itemBits = itemBits or 0x01u
        if (state["gravity"] == true) itemBits = itemBits or 0x20u
        if (state["hijump"] == true) itemBits = itemBits or 0x100u
        if (state["speed"] == true) itemBits = itemBits or 0x200u
        if (state["space"] == true) itemBits = itemBits or 0x02u
        if (state["screw"] == true) itemBits = itemBits or 0x08u
        if (state["grapple"] == true) itemBits = itemBits or 0x4000u
        if (state["xray"] == true) itemBits = itemBits or 0x8000u
        
        // Convert to little-endian hex
        val lowByte = (itemBits and 0xFFu).toInt()
        val highByte = ((itemBits shr 8) and 0xFFu).toInt()
        return "${lowByte.toString(16).padStart(2, '0')}${highByte.toString(16).padStart(2, '0')}"
    }
    
    private fun generateBeamsData(state: Map<String, Any>): String {
        // Create beams bitfield to match Python reference exactly  
        // Python target: charge=true, ice=true, wave=true
        var beamBits = 0u
        
        if (state["charge"] == true) beamBits = beamBits or 0x1000u
        if (state["ice"] == true) beamBits = beamBits or 0x02u
        if (state["wave"] == true) beamBits = beamBits or 0x01u
        if (state["spazer"] == true) beamBits = beamBits or 0x04u
        if (state["plasma"] == true) beamBits = beamBits or 0x08u
        if (state["hyper"] == true) beamBits = beamBits or 0x10u
        
        // Convert to little-endian hex
        val lowByte = (beamBits and 0xFFu).toInt()
        val highByte = ((beamBits shr 8) and 0xFFu).toInt()
        return "${lowByte.toString(16).padStart(2, '0')}${highByte.toString(16).padStart(2, '0')}"
    }
    
    private fun generateBossData(state: Map<String, Any>): String {
        // Generate MAIN BOSSES data to match Python parser logic exactly
        // Python: 'bomb_torizo': bool(bosses_value & 0x04) = bit 2
        // Python: 'kraid': bool(bosses_value & 0x100) = bit 8  
        // Python: 'spore_spawn': bool(bosses_value & 0x200) = bit 9
        // Python: 'draygon': bool(bosses_value & 0x1000) = bit 12
        // Python: 'mother_brain': bool(bosses_value & 0x01) = bit 0
        
        var mainBossBits = 0u
        
        if (state["bomb_torizo"] == true) mainBossBits = mainBossBits or 0x04u     // bit 2
        if (state["kraid"] == true) mainBossBits = mainBossBits or 0x100u          // bit 8
        if (state["spore_spawn"] == true) mainBossBits = mainBossBits or 0x200u    // bit 9
        if (state["draygon"] == true) mainBossBits = mainBossBits or 0x1000u       // bit 12
        if (state["mother_brain"] == true) mainBossBits = mainBossBits or 0x01u    // bit 0
        
        // Convert to little-endian hex
        val lowByte = (mainBossBits and 0xFFu).toInt()
        val highByte = ((mainBossBits shr 8) and 0xFFu).toInt()
        return "${lowByte.toString(16).padStart(2, '0')}${highByte.toString(16).padStart(2, '0')}"
    }
    
    private fun generateCrocomireData(state: Map<String, Any>): String {
        // Crocomire has separate memory address and logic
        // Python: 'crocomire': bool(crocomire_value & 0x02) and (crocomire_value >= 0x0202)
        
        if (state["crocomire"] == true) {
            // Generate 0x0202 value: has bit 1 set AND >= 0x0202
            return "0202"  // Little-endian: 02 02
        } else {
            return "0000"  // Not defeated
        }
    }
    
    private fun generateRoomIdData(state: Map<String, Any>): String {
        val roomId = (state["room_id"] as? Int) ?: 12345
        val lowByte = roomId and 0xFF
        val highByte = (roomId shr 8) and 0xFF
        return "${lowByte.toString(16).padStart(2, '0')}${highByte.toString(16).padStart(2, '0')}"
    }
    
    private fun generateAreaIdData(state: Map<String, Any>): String {
        val areaId = (state["area_id"] as? Int) ?: 1
        return areaId.toString(16).padStart(2, '0')
    }
    
    private fun generateGameStateData(state: Map<String, Any>): String {
        val gameState = (state["game_state"] as? Int) ?: 15
        val lowByte = gameState and 0xFF
        val highByte = (gameState shr 8) and 0xFF
        return "${lowByte.toString(16).padStart(2, '0')}${highByte.toString(16).padStart(2, '0')}"
    }
    
    private fun generatePlayerXData(state: Map<String, Any>): String {
        val playerX = (state["player_x"] as? Int) ?: 128
        val lowByte = playerX and 0xFF
        val highByte = (playerX shr 8) and 0xFF
        return "${lowByte.toString(16).padStart(2, '0')}${highByte.toString(16).padStart(2, '0')}"
    }
    
    private fun generatePlayerYData(state: Map<String, Any>): String {
        val playerY = (state["player_y"] as? Int) ?: 152
        val lowByte = playerY and 0xFF
        val highByte = (playerY shr 8) and 0xFF
        return "${lowByte.toString(16).padStart(2, '0')}${highByte.toString(16).padStart(2, '0')}"
    }

    suspend fun readMemoryRange(startAddress: Int, size: Int): ByteArray? {
        val command = "READ_CORE_MEMORY 0x${startAddress.toString(16).uppercase()} $size"
        val response = sendCommand(command) ?: return null
        
        if (!response.startsWith("READ_CORE_MEMORY")) {
            return null
        }
        
        return try {
            val parts = response.split(' ', limit = 3)
            if (parts.size < 3) return null
            
            val hexData = parts[2].replace(" ", "")
            hexData.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        } catch (e: Exception) {
            println("Failed to parse mock memory data: ${e.message}")
            null
        }
    }
    
    suspend fun getRetroArchInfo(): Map<String, Any> {
        val versionResponse = sendCommand("VERSION")
        val gameInfoResponse = sendCommand("GET_STATUS")
        
        return buildMap {
            put("connected", connected)
            put("retroarch_version", versionResponse ?: "1.21.0 (Mock)")
            put("game_loaded", gameInfoResponse?.contains("PLAYING") == true)
            put("game_info", gameInfoResponse ?: "Mock Game Data")
        }
    }
    
    fun close() {
        connected = false
        println("🎭 Mock UDP: Disconnected")
    }
}

// Unit-testable bit parsing functions as requested
object GameStateUtils {
    
    /**
     * Test if Ridley is defeated based on boss state
     * @param bossState UInt value from boss memory
     * @return true if Ridley is defeated
     */
    fun isRidleyDefeated(bossState: UInt): Boolean = (bossState and 0x08u) != 0u
    
    /**
     * Test if Kraid is defeated based on boss state
     */
    fun isKraidDefeated(bossState: UInt): Boolean = (bossState and 0x01u) != 0u
    
    /**
     * Test if Phantoon is defeated based on boss state
     */
    fun isPhantoonDefeated(bossState: UInt): Boolean = (bossState and 0x02u) != 0u
    
    /**
     * Test if Draygon is defeated based on boss state
     */
    fun isDraygonDefeated(bossState: UInt): Boolean = (bossState and 0x04u) != 0u
    
    /**
     * Test if player has Morph Ball based on item state
     */
    fun hasMorphBall(itemState: UInt): Boolean = (itemState and 0x04u) != 0u
    
    /**
     * Test if player has Varia Suit based on item state
     */
    fun hasVariaSuit(itemState: UInt): Boolean = (itemState and 0x01u) != 0u
    
    /**
     * Test if player has Hi-Jump Boots based on item state
     */
    fun hasHiJumpBoots(itemState: UInt): Boolean = (itemState and 0x100u) != 0u
    
    /**
     * Test if player has Charge Beam based on beam state
     */
    fun hasChargeBeam(beamState: UInt): Boolean = (beamState and 0x1000u) != 0u
    
    /**
     * Test if player has Ice Beam based on beam state
     */
    fun hasIceBeam(beamState: UInt): Boolean = (beamState and 0x02u) != 0u
    
    /**
     * Test if player has Wave Beam based on beam state
     */
    fun hasWaveBeam(beamState: UInt): Boolean = (beamState and 0x01u) != 0u
} 