package com.supermetroid.parser

import com.supermetroid.model.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Super Metroid Game State Parser
 * Converts raw memory data into structured game state
 * Equivalent to Python's SuperMetroidGameStateParser
 */
class GameStateParser {
    private val mutex = Mutex()
    
    // Persistent state for Mother Brain phases - once detected, stays detected
    private var motherBrainPhaseState = mutableMapOf(
        "mb1_detected" to false,
        "mb2_detected" to false
    )
    
    // Super Metroid memory layout
    private val memoryMap = mapOf(
        "health" to 0x7E09C2,
        "max_health" to 0x7E09C4,
        "missiles" to 0x7E09C6,
        "max_missiles" to 0x7E09C8,
        "supers" to 0x7E09CA,
        "max_supers" to 0x7E09CC,
        "power_bombs" to 0x7E09CE,
        "max_power_bombs" to 0x7E09D0,
        "reserve_energy" to 0x7E09D6,
        "max_reserve_energy" to 0x7E09D4,
        "room_id" to 0x7E079B,
        "area_id" to 0x7E079F,
        "game_state" to 0x7E0998,
        "player_x" to 0x7E0AF6,
        "player_y" to 0x7E0AFA,
        "items" to 0x7E09A4,
        "beams" to 0x7E09A8,
        "bosses" to 0x7ED828,
    )
    
    // Area names mapping
    private val areas = mapOf(
        0 to "Crateria",
        1 to "Brinstar", 
        2 to "Norfair",
        3 to "Wrecked Ship",
        4 to "Maridia",
        5 to "Tourian"
    )
    
    suspend fun parseCompleteGameState(memoryData: Map<String, ByteArray>): GameState = mutex.withLock {
        try {
            // Parse basic stats from bulk read
            val basicStats = memoryData["basic_stats"]
            val stats = if (basicStats != null && basicStats.size >= 22) {
                parseBasicStats(basicStats)
            } else {
                mapOf<String, Int>()
            }
            
            // Parse location data
            val locationData = parseLocationData(
                memoryData["room_id"],
                memoryData["area_id"],
                memoryData["game_state"],
                memoryData["player_x"],
                memoryData["player_y"]
            )
            
            // Parse items with reset detection
            val items = parseItems(
                memoryData["items"],
                locationData,
                stats["health"] ?: 0,
                stats["missiles"] ?: 0,
                stats["max_missiles"] ?: 0
            )
            
            // Parse beams with reset detection  
            val beams = parseBeams(
                memoryData["beams"],
                locationData,
                stats["health"] ?: 0,
                stats["missiles"] ?: 0,
                stats["max_missiles"] ?: 0
            )
            
            // Parse bosses
            val bosses = parseBosses(
                memoryData["main_bosses"],
                memoryData["crocomire"],
                memoryData["boss_plus_1"],
                memoryData["boss_plus_2"],
                memoryData["boss_plus_3"],
                memoryData["boss_plus_4"],
                memoryData["boss_plus_5"],
                memoryData["escape_timer_1"],
                memoryData["escape_timer_2"],
                memoryData["escape_timer_3"],
                memoryData["escape_timer_4"],
                locationData
            )
            
            return GameState(
                health = stats["health"] ?: 0,
                maxHealth = stats["max_health"] ?: 99,
                missiles = stats["missiles"] ?: 0,
                maxMissiles = stats["max_missiles"] ?: 0,
                supers = stats["supers"] ?: 0,
                maxSupers = stats["max_supers"] ?: 0,
                powerBombs = stats["power_bombs"] ?: 0,
                maxPowerBombs = stats["max_power_bombs"] ?: 0,
                reserveEnergy = stats["reserve_energy"] ?: 0,
                maxReserveEnergy = stats["max_reserve_energy"] ?: 0,
                roomId = locationData["room_id"] as? Int ?: 0,
                areaId = locationData["area_id"] as? Int ?: 0,
                areaName = locationData["area_name"] as? String ?: "Unknown",
                gameState = locationData["game_state"] as? Int ?: 0,
                playerX = locationData["player_x"] as? Int ?: 0,
                playerY = locationData["player_y"] as? Int ?: 0,
                items = items,
                beams = beams,
                bosses = bosses
            )
            
        } catch (e: Exception) {
            println("Error parsing game state: ${e.message}")
            GameState()
        }
    }
    
    private fun parseBasicStats(data: ByteArray): Map<String, Int> {
        if (data.size < 22) return emptyMap()
        
        return try {
            mapOf(
                "health" to data.readInt16LE(0),
                "max_health" to data.readInt16LE(2),
                "missiles" to data.readInt16LE(4),
                "max_missiles" to data.readInt16LE(6),
                "supers" to data.readInt16LE(8),
                "max_supers" to data.readInt16LE(10),
                "power_bombs" to data.readInt16LE(12),
                "max_power_bombs" to data.readInt16LE(14),
                "reserve_energy" to data.readInt16LE(18),
                "max_reserve_energy" to data.readInt16LE(16)
            )
        } catch (e: Exception) {
            println("Error parsing basic stats: ${e.message}")
            emptyMap()
        }
    }
    
    private fun parseLocationData(
        roomIdData: ByteArray?,
        areaIdData: ByteArray?,
        gameStateData: ByteArray?,
        playerXData: ByteArray?,
        playerYData: ByteArray?
    ): Map<String, Any> {
        return try {
            val roomId = roomIdData?.readInt16LE(0) ?: 0
            val areaId = areaIdData?.let { if (it.isNotEmpty()) it[0].toInt() and 0xFF else 0 } ?: 0
            val gameState = gameStateData?.readInt16LE(0) ?: 0
            val playerX = playerXData?.readInt16LE(0) ?: 0
            val playerY = playerYData?.readInt16LE(0) ?: 0
            
            mapOf(
                "room_id" to roomId,
                "area_id" to areaId,
                "area_name" to (areas[areaId] ?: "Unknown"),
                "game_state" to gameState,
                "player_x" to playerX,
                "player_y" to playerY
            )
        } catch (e: Exception) {
            println("Error parsing location data: ${e.message}")
            emptyMap()
        }
    }
    
    private fun shouldResetItemState(
        locationData: Map<String, Any>,
        health: Int,
        missiles: Int,
        maxMissiles: Int
    ): Boolean {
        val areaId = locationData["area_id"] as? Int ?: 0
        val roomId = locationData["room_id"] as? Int ?: 0
        
        // Reset scenarios - CONSERVATIVE:
        
        // 1. Intro scene (very specific early game indicators)
        val inStartingArea = areaId == 0 // Crateria
        val hasStartingHealth = health <= 99
        val inStartingRooms = roomId < 1000
        if (inStartingArea && hasStartingHealth && inStartingRooms) {
            println("🔄 ITEM STATE RESET: intro scene detected - Area:$areaId, Room:$roomId, Health:$health")
            return true
        }
        
        // 2. Very specific new game indicators only
        val definiteNewGame = (health == 99 && missiles == 0 && maxMissiles == 0 && roomId < 1000)
        if (definiteNewGame) {
            println("🔄 ITEM STATE RESET: new game detected - Area:$areaId, Room:$roomId, Health:$health, Missiles:$missiles/$maxMissiles")
            return true
        }
        
        return false
    }
    
    private fun parseItems(
        itemsData: ByteArray?,
        locationData: Map<String, Any>,
        health: Int,
        missiles: Int,
        maxMissiles: Int
    ): Map<String, Boolean> {
        if (itemsData == null || itemsData.size < 2) return emptyMap()
        
        // Check if we should reset item state
        if (shouldResetItemState(locationData, health, missiles, maxMissiles)) {
            return mapOf(
                "morph" to false,
                "bombs" to false,
                "varia" to false,
                "gravity" to false,
                "hijump" to false,
                "speed" to false,
                "spacejump" to false,
                "screw" to false,
                "spring" to false,
                "grapple" to false,
                "xray" to false
            )
        }
        
        val itemValue = itemsData.readInt16LE(0)
        
        return mapOf(
            "morph" to ((itemValue and 0x0004) != 0),
            "bombs" to ((itemValue and 0x1000) != 0),
            "varia" to ((itemValue and 0x0001) != 0),
            "gravity" to ((itemValue and 0x0020) != 0),
            "hijump" to ((itemValue and 0x0100) != 0),
            "speed" to ((itemValue and 0x2000) != 0),
            "spacejump" to ((itemValue and 0x0200) != 0),
            "screw" to ((itemValue and 0x0008) != 0),
            "spring" to ((itemValue and 0x0002) != 0),
            "grapple" to ((itemValue and 0x4000) != 0),
            "xray" to ((itemValue and 0x8000) != 0)
        )
    }
    
    private fun parseBeams(
        beamsData: ByteArray?,
        locationData: Map<String, Any>,
        health: Int,
        missiles: Int,
        maxMissiles: Int
    ): Map<String, Boolean> {
        if (beamsData == null || beamsData.size < 2) return emptyMap()
        
        // Check if we should reset beam state (same logic as items)
        if (shouldResetItemState(locationData, health, missiles, maxMissiles)) {
            println("🔄 BEAM STATE RESET: Resetting to starting state (no beams)")
            return mapOf(
                "charge" to false,  // Charge beam must be collected
                "ice" to false,
                "wave" to false,
                "spazer" to false,
                "plasma" to false,
                "hyper" to false
            )
        }
        
        val beamValue = beamsData.readInt16LE(0)
        
        return mapOf(
            "charge" to ((beamValue and 0x1000) != 0),
            "ice" to ((beamValue and 0x0002) != 0),
            "wave" to ((beamValue and 0x0001) != 0),
            "spazer" to ((beamValue and 0x0004) != 0),
            "plasma" to ((beamValue and 0x0008) != 0),
            "hyper" to ((beamValue and 0x0010) != 0)
        )
    }
    
    private fun parseBosses(
        mainBossesData: ByteArray?,
        crocomireData: ByteArray?,
        bossPlus1Data: ByteArray?,
        bossPlus2Data: ByteArray?,
        bossPlus3Data: ByteArray?,
        bossPlus4Data: ByteArray?,
        bossPlus5Data: ByteArray?,
        escapeTimer1Data: ByteArray?,
        escapeTimer2Data: ByteArray?,
        escapeTimer3Data: ByteArray?,
        escapeTimer4Data: ByteArray?,
        locationData: Map<String, Any>
    ): Map<String, Boolean> {
        // Simplified boss parsing - full implementation would match Python logic
        val mainBosses = mainBossesData?.readInt16LE(0) ?: 0
        
        return mapOf(
            "bomb_torizo" to ((mainBosses and 0x0004) != 0),
            "kraid" to ((mainBosses and 0x0001) != 0),
            "spore_spawn" to ((mainBosses and 0x0002) != 0),
            "crocomire" to ((mainBosses and 0x0008) != 0),
            "botwoon" to ((mainBosses and 0x0010) != 0),
            "phantoon" to ((mainBosses and 0x0100) != 0),
            "draygon" to ((mainBosses and 0x0200) != 0),
            "ridley" to ((mainBosses and 0x0400) != 0),
            "golden_torizo" to ((mainBosses and 0x0800) != 0),
            "mother_brain_1" to motherBrainPhaseState["mb1_detected"]!!,
            "mother_brain_2" to motherBrainPhaseState["mb2_detected"]!!
        )
    }
    
    suspend fun resetMotherBrainCache() = mutex.withLock {
        motherBrainPhaseState["mb1_detected"] = false
        motherBrainPhaseState["mb2_detected"] = false
        println("🔄 MB cache reset to default (not detected)")
    }
    
    // Extension function to read little-endian 16-bit integers from ByteArray
    private fun ByteArray.readInt16LE(offset: Int): Int {
        if (offset + 1 >= size) return 0
        return (this[offset].toInt() and 0xFF) or ((this[offset + 1].toInt() and 0xFF) shl 8)
    }
} 