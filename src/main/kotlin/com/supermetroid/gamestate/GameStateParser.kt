package com.supermetroid.gamestate

import com.supermetroid.model.*
import com.supermetroid.model.RoomDatabase
import com.supermetroid.network.readInt16LE
import com.supermetroid.network.readInt8
import com.supermetroid.util.Logging
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * Parses raw memory data into structured GameState objects
 * Implements exact logic from the original TypeScript backend
 */
class GameStateParser : Logging {

    /**
     * Parse raw memory data into GameState
     */
    fun parseGameState(memoryData: Map<String, ByteArray>): GameState {
        logger.debug { "🔄 Parsing game state from ${memoryData.size} memory entries" }

        // Removed debug logging - too noisy

        return GameState(
            health = parseHealth(memoryData["health"]),
            maxHealth = parseMaxHealth(memoryData["maxHealth"]),
            missiles = parseMissiles(memoryData["missiles"]),
            maxMissiles = parseMaxMissiles(memoryData["maxMissiles"]),
            supers = parseSupers(memoryData["supers"]),
            maxSupers = parseMaxSupers(memoryData["maxSupers"]),
            powerBombs = parsePowerBombs(memoryData["powerBombs"]),
            maxPowerBombs = parseMaxPowerBombs(memoryData["maxPowerBombs"]),
            reserveEnergy = parseReserveEnergy(memoryData["reserveEnergy"]),
            maxReserveEnergy = parseMaxReserveEnergy(memoryData["maxReserveEnergy"]),
            roomId = parseRoomId(memoryData["roomId"]),
            areaId = parseAreaId(memoryData["areaId"]),
            areaName = parseAreaName(parseAreaId(memoryData["areaId"])),
            gameState = parseGameStateValue(memoryData["gameState"]),
            playerX = parsePlayerX(memoryData["playerX"]),
            playerY = parsePlayerY(memoryData["playerY"]),
            motherBrainHp = parseMotherBrainHp(memoryData["motherBrainHp"]),
            eventFlags = memoryData["eventFlags"]?.readInt16LE() ?: 0,
            tourianBosses = memoryData["tourianBosses"]?.readInt16LE() ?: 0,
            shipAi = memoryData["shipAi"]?.readInt16LE() ?: 0,
            deathCount = parseDeathCount(memoryData["deathCount"]),
            resetCount = parseResetCount(memoryData["resetCount"]),
            items = parseItems(memoryData["collectedItems"]),
            beams = parseBeams(memoryData["collectedBeams"]),
            bosses = parseBosses(memoryData, parseAreaId(memoryData["areaId"]))
        )
    }

    private fun parseHealth(data: ByteArray?): Int = data?.readInt16LE() ?: 0
    private fun parseMaxHealth(data: ByteArray?): Int = data?.readInt16LE() ?: 99
    private fun parseMissiles(data: ByteArray?): Int = data?.readInt16LE() ?: 0
    private fun parseMaxMissiles(data: ByteArray?): Int = data?.readInt16LE() ?: 0
    private fun parseSupers(data: ByteArray?): Int = data?.readInt16LE() ?: 0
    private fun parseMaxSupers(data: ByteArray?): Int = data?.readInt16LE() ?: 0
    private fun parsePowerBombs(data: ByteArray?): Int = data?.readInt16LE() ?: 0
    private fun parseMaxPowerBombs(data: ByteArray?): Int = data?.readInt16LE() ?: 0
    private fun parseReserveEnergy(data: ByteArray?): Int = data?.readInt16LE() ?: 0
    private fun parseMaxReserveEnergy(data: ByteArray?): Int = data?.readInt16LE() ?: 0
    private fun parseRoomId(data: ByteArray?): Int = data?.readInt16LE() ?: 0
    private fun parseAreaId(data: ByteArray?): Int = data?.readInt8() ?: 0
    private fun parseGameStateValue(data: ByteArray?): Int = data?.readInt16LE() ?: 0
    private fun parsePlayerX(data: ByteArray?): Int = data?.readInt16LE() ?: 0
    private fun parsePlayerY(data: ByteArray?): Int = data?.readInt16LE() ?: 0
    private fun parseMotherBrainHp(data: ByteArray?): Int {
        val hp = data?.readInt16LE() ?: 0
        logger.debug { "🧠💊 MB HP: $hp (0x${hp.toString(16)})" }
        return hp
    }
    
    private fun parseDeathCount(data: ByteArray?): Int {
        val count = data?.readInt16LE() ?: 0
        return count
    }
    
    private fun parseResetCount(data: ByteArray?): Int {
        val count = data?.readInt16LE() ?: 0
        return count
    }

    /**
     * Parse area name from area ID
     */
    private fun parseAreaName(areaId: Int): String = when (areaId) {
        0 -> "Crateria"
        1 -> "Brinstar"
        2 -> "Norfair"
        3 -> "Wrecked Ship"
        4 -> "Maridia"
        5 -> "Tourian"
        6 -> "Ceres Station"
        else -> "Unknown"
    }

    /**
     * Parse collected items from memory flags
     */
    private fun parseItems(data: ByteArray?): Items {
        val itemFlags = data?.readInt16LE() ?: 0

        return Items(
            morph = (itemFlags and 0x0004) != 0,
            bombs = (itemFlags and 0x1000) != 0,
            varia = (itemFlags and 0x0001) != 0,
            gravity = (itemFlags and 0x0020) != 0,
            hiJump = (itemFlags and 0x0100) != 0,
            speed = (itemFlags and 0x2000) != 0,
            spaceJump = (itemFlags and 0x0200) != 0,
            screw = (itemFlags and 0x0008) != 0,
            spring = (itemFlags and 0x0002) != 0,
            grapple = (itemFlags and 0x4000) != 0,
            xray = (itemFlags and 0x8000) != 0
        )
    }

    /**
     * Parse collected beams from memory flags
     */
    private fun parseBeams(data: ByteArray?): Beams {
        val beamFlags = data?.readInt16LE() ?: 0

        return Beams(
            charge = (beamFlags and 0x1000) != 0,
            ice = (beamFlags and 0x0002) != 0,
            wave = (beamFlags and 0x0001) != 0,
            spazer = (beamFlags and 0x0004) != 0,
            plasma = (beamFlags and 0x0008) != 0,
            hyper = (beamFlags and 0x0020) != 0
        )
    }

    /**
     * Parse boss defeat flags from multiple memory locations
     */
    private fun parseBosses(memoryData: Map<String, ByteArray>, areaId: Int): Bosses {
        val bossFlags1 = memoryData["bossFlags1"]?.readInt16LE() ?: 0
        val bossFlags2 = memoryData["bossFlags2"]?.readInt16LE() ?: 0
        val bossFlags3 = memoryData["bossFlags3"]?.readInt16LE() ?: 0
        val bossFlags4 = memoryData["bossFlags4"]?.readInt16LE() ?: 0
        val bossFlags5 = memoryData["bossFlags5"]?.readInt16LE() ?: 0
        val tourianBosses = memoryData["tourianBosses"]?.readInt16LE() ?: 0
        val ceresBosses = memoryData["ceresBosses"]?.readInt16LE() ?: 0
        val eventFlags = memoryData["eventFlags"]?.readInt16LE() ?: 0
        val shipAi = memoryData["shipAi"]?.readInt16LE() ?: 0

        // Reduce debug noise - only log every 50 polls or when data changes significantly
        val currentDataHash = (bossFlags1 + bossFlags2 + bossFlags3 + tourianBosses + eventFlags).toString(16)
        if (currentDataHash.hashCode() % 20 == 0) {
            logger.debug { "🏆 Boss flags - 1:0x${bossFlags1.toString(16)}, 2:0x${bossFlags2.toString(16)}, 3:0x${bossFlags3.toString(16)}, 4:0x${bossFlags4.toString(16)}, 5:0x${bossFlags5.toString(16)}" }
            logger.debug { "🧠 Tourian bosses: 0x${tourianBosses.toString(16)}, Ceres bosses: 0x${ceresBosses.toString(16)}, Event flags: 0x${eventFlags.toString(16)}" }
        }

        return Bosses(
            ceresStation = parseCeresStation(ceresBosses, areaId),
            bombTorizo = parseBombTorizo(bossFlags1),
            kraid = parseKraid(bossFlags1),
            sporeSpawn = parseSporeSpawn(bossFlags1),
            crocomire = parseCrocomire(bossFlags2),
            botwoon = parseBotwoon(bossFlags5),
            phantoon = parsePhantoon(bossFlags4),
            draygon = parseDraygon(bossFlags5),
            ridley = parseRidley(bossFlags3),
            goldenTorizo = parseGoldenTorizo(bossFlags1, bossFlags2, bossFlags3, bossFlags4),
            motherBrain1 = parseMotherBrain1(memoryData),
            motherBrain2 = parseMotherBrain2(memoryData),
            motherBrain = parseMotherBrainFinal(tourianBosses),
            samusShip = parseSamusShip(shipAi, eventFlags, tourianBosses)
        )
    }

    private fun parseCeresStation(ceresBosses: Int, areaId: Int): Boolean {
        // FIXED: For fresh runs, don't show Ceres as complete just because the memory flag is set
        // The flag can persist from previous playthroughs or save states
        val ceresRidleyDefeated = (ceresBosses and 0x0001) != 0
        val inCeresArea = areaId == 6

        // For status icon: Only show complete if we've legitimately completed Ceres
        // - If we're still in Ceres area, don't show complete even if boss is defeated (until we leave)
        // - If we're outside Ceres and boss is defeated, it's likely legitimate
        val result = ceresRidleyDefeated && !inCeresArea

        logger.debug { "🚨 Ceres Status: ridleyDefeated=$ceresRidleyDefeated, inCeres=$inCeresArea, result=$result (legitimate completion)" }
        return result
    }

    private fun parseBombTorizo(bossFlags1: Int): Boolean {
        val detected = (bossFlags1 and 0x0004) != 0
        logger.debug { "💣 Bomb Torizo: bossFlags1=0x${bossFlags1.toString(16)}, bit 2=$detected" }
        return detected
    }

    private fun parseKraid(bossFlags1: Int): Boolean {
        val detected = (bossFlags1 and 0x0100) != 0  // bit 8, matches working Node server
        logger.debug { "🦎 Kraid: bossFlags1=0x${bossFlags1.toString(16)}, bit 8=$detected" }
        return detected
    }

    private fun parseSporeSpawn(bossFlags1: Int): Boolean {
        val detected = (bossFlags1 and 0x0200) != 0  // bit 9, matches working Node server
        logger.debug { "🌸 Spore Spawn: bossFlags1=0x${bossFlags1.toString(16)}, bit 9=$detected" }
        return detected
    }
    private fun parseCrocomire(bossFlags2: Int): Boolean = (bossFlags2 and 0x0002) != 0
    private fun parseBotwoon(bossFlags5: Int): Boolean {
        // Matches ASL exactly: maridiaBosses & botwoon flag (bit 1)
        val detected = (bossFlags5 and 0x0002) != 0
        logger.debug { "🐍 Botwoon: bossFlags5=0x${bossFlags5.toString(16)}, bit1=$detected (ASL pattern)" }
        return detected
    }

    private fun parsePhantoon(bossFlags4: Int): Boolean {
        // Matches ASL exactly: wreckedShipBosses & phantoon flag (bit 0)
        val detected = (bossFlags4 and 0x0001) != 0
        logger.debug { "👻 Phantoon: bossFlags4=0x${bossFlags4.toString(16)}, bit0=$detected (ASL pattern)" }
        return detected
    }

    private fun parseRidley(bossFlags3: Int): Boolean {
        // Matches ASL exactly: norfairBosses & ridley flag (bit 0)
        val detected = (bossFlags3 and 0x0001) != 0
        logger.debug { "🐲 Ridley: bossFlags3=0x${bossFlags3.toString(16)}, bit0=$detected (ASL pattern)" }
        return detected
    }

    /**
     * Parse Draygon using event flags (bit 8)
     */
    private fun parseDraygon(bossFlags5: Int): Boolean {
        // Matches ASL exactly: maridiaBosses & draygon flag (bit 0)
        val detected = (bossFlags5 and 0x0001) != 0
        logger.debug { "🐉 Draygon: bossFlags5=0x${bossFlags5.toString(16)}, bit0=$detected (ASL pattern)" }
        return detected
    }

    /**
     * Parse Golden Torizo using norfair boss flag (bit 2)
     * Matches ASL exactly: norfairBosses & goldenTorizo flag (bit 2)
     */
    private fun parseGoldenTorizo(bossFlags1: Int, bossFlags2: Int, bossFlags3: Int, bossFlags4: Int): Boolean {
        // Use norfairBosses (bossFlags3) with goldenTorizo flag (bit 2 = 0x4)
        // This matches the ASL file exactly and prevents false positives
        val detected = (bossFlags3 and 0x0004) != 0

        logger.debug { "🏆 Golden Torizo: norfairBosses=0x${bossFlags3.toString(16)}, bit2=$detected (ASL pattern)" }
        return detected
    }

    /**
     * Parse Mother Brain final defeat (MB3) using Tourian boss flags
     */
    private fun parseMotherBrainFinal(tourianBosses: Int): Boolean {
        val defeated = (tourianBosses and 0x0002) != 0
        logger.debug { "🧠 Mother Brain final: tourianBosses=0x${tourianBosses.toString(16)}, defeated=$defeated" }
        return defeated
    }

    /**
     * Parse Mother Brain 1 - EXACT ASL LOGIC
     * MB1 = inMotherBrainRoom && gameState == normalGameplay && motherBrainHP >= 18000
     */
    private fun parseMotherBrain1(memoryData: Map<String, ByteArray>): Boolean {
        val roomId = parseRoomId(memoryData["roomId"])
        val gameState = parseGameStateValue(memoryData["gameState"])
        val motherBrainHp = parseMotherBrainHp(memoryData["motherBrainHp"])
        val eventFlags = memoryData["eventFlags"]?.readInt16LE() ?: 0
        val tourianBosses = memoryData["tourianBosses"]?.readInt16LE() ?: 0

        val inMbRoom = roomId == RoomIds.MOTHER_BRAIN_ROOM
        val normalGameplay = gameState == GameStateConstants.NORMAL_GAMEPLAY

        // RETROACTIVE LOGIC: If HP >= 18000 in MB room, MB1 already defeated
        val mb1AlreadyDefeated = inMbRoom && normalGameplay && motherBrainHp >= 18000

        // Also check escape scenarios
        val zebesEscaping = (eventFlags and 0x0040) != 0
        val mbFinalDefeated = (tourianBosses and 0x0002) != 0  // Bit 1 for final defeat - match parseMotherBrainFinal

        val result = mb1AlreadyDefeated || zebesEscaping || mbFinalDefeated

        if (result) {
            logger.debug { "🧠1 Parser MB1: HP=$motherBrainHp, room=$inMbRoom, gameplay=$normalGameplay, retroactive=$mb1AlreadyDefeated, escaping=$zebesEscaping, final=$mbFinalDefeated, result=$result" }
        }

        return result
    }

    /**
     * Parse Mother Brain 2 - EXACT ASL LOGIC
     * MB2 = inMotherBrainRoom && gameState == normalGameplay && motherBrainHP >= 36000
     */
    private fun parseMotherBrain2(memoryData: Map<String, ByteArray>): Boolean {
        val roomId = parseRoomId(memoryData["roomId"])
        val gameState = parseGameStateValue(memoryData["gameState"])
        val motherBrainHp = parseMotherBrainHp(memoryData["motherBrainHp"])
        val eventFlags = memoryData["eventFlags"]?.readInt16LE() ?: 0
        val tourianBosses = memoryData["tourianBosses"]?.readInt16LE() ?: 0

        val inMbRoom = roomId == RoomIds.MOTHER_BRAIN_ROOM
        val normalGameplay = gameState == GameStateConstants.NORMAL_GAMEPLAY

        // RETROACTIVE LOGIC: If HP >= 36000 in MB room, MB2 already defeated
        val mb2AlreadyDefeated = inMbRoom && normalGameplay && motherBrainHp >= 36000

        // Also check escape/final scenarios
        val zebesEscaping = (eventFlags and 0x0040) != 0
        val mbFinalDefeated = (tourianBosses and 0x0002) != 0  // Bit 1 for final defeat - match parseMotherBrainFinal

        val result = mb2AlreadyDefeated || zebesEscaping || mbFinalDefeated

        if (result) {
            logger.debug { "🧠2 Parser MB2: HP=$motherBrainHp, room=$inMbRoom, gameplay=$normalGameplay, retroactive=$mb2AlreadyDefeated, escaping=$zebesEscaping, final=$mbFinalDefeated, result=$result" }
        }

        return result
    }

    /**
     * Parse Samus Ship (end game detection)
     * Based on ASL logic: zebes ablaze + ship AI transition
     */
    private fun parseSamusShip(shipAi: Int, eventFlags: Int, tourianBosses: Int): Boolean {
        val zebesAblaze = (eventFlags and 0x0040) != 0  // bit 6
        val shipReady = shipAi == 0xAA4F
        val motherBrainDefeated = (tourianBosses and 0x0002) != 0

        val detected = zebesAblaze && shipReady && motherBrainDefeated

        logger.debug { "🚢 Ship: zebesAblaze=$zebesAblaze, shipAI=0x${shipAi.toString(16)}, mbDefeated=$motherBrainDefeated, result=$detected" }
        return detected
    }
}

/**
 * Game state constants
 */
object GameStateConstants {
    const val NORMAL_GAMEPLAY = 8
    const val DOOR_TRANSITION = 11  // 0xB
    const val ELEVATOR = 5
    const val PAUSED = 18
    const val LOADING = 0
    const val TITLE_SCREEN = 2              // Title screen state (before pressing start)
    const val GAME_START_TRANSITION = 31    // 0x1F - Transition after pressing start (ASL normalStart)
    const val START_OF_CERES_CUTSCENE = 32  // 0x20
    const val PRE_END_CUTSCENE = 38  // 0x26
    const val END_CUTSCENE = 39  // 0x27
    const val ZEBES_TRANSITION_END = 6      // For categories that start from Zebes (ASL zebesStart)
}

/**
 * Room ID constants for important locations (sourced from RoomDatabase)
 */
object RoomIds {
    val MOTHER_BRAIN_ROOM = RoomDatabase.getRoomByHandle("motherBrain")?.id ?: 0xDD58
    val CERES_STATION = RoomDatabase.getRoomByHandle("ceresRidley")?.id ?: 0xE0B5
    val CERES_ELEVATOR = RoomDatabase.getRoomByHandle("ceresElevator")?.id ?: 0xDF45
    val LANDING_SITE = RoomDatabase.getRoomByHandle("landingSite")?.id ?: 0x91F8
    val STATUES_HALLWAY = RoomDatabase.getRoomByHandle("statuesHallway")?.id ?: 0xA5ED
    val STATUES = RoomDatabase.getRoomByHandle("statues")?.id ?: 0xA66A
}
