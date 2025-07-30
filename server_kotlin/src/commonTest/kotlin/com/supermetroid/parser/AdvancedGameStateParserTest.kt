package com.supermetroid.parser

import com.supermetroid.model.GameState
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Advanced tests for GameStateParser
 * Focuses on complex scenarios and edge cases
 */
class AdvancedGameStateParserTest {

    private val parser = GameStateParser()

    /**
     * Helper function to create a ByteArray from an Int value
     * Handles the conversion to little-endian format
     */
    private fun createByteArray(value: Int): ByteArray {
        return byteArrayOf((value and 0xFF).toByte(), ((value shr 8) and 0xFF).toByte())
    }

    /**
     * Helper function to create a memory data map with common values
     * Makes test setup more concise
     */
    private fun createMemoryData(
        health: Int = 300,
        maxHealth: Int = 300,
        missiles: Int = 20,
        maxMissiles: Int = 20,
        supers: Int = 10,
        maxSupers: Int = 10,
        powerBombs: Int = 5,
        maxPowerBombs: Int = 5,
        reserveEnergy: Int = 0,
        maxReserveEnergy: Int = 0,
        roomId: Int = 0x1000,
        areaId: Int = 1,
        gameState: Int = 0,
        playerX: Int = 0,
        playerY: Int = 0,
        items: Int = 0,
        beams: Int = 0,
        mainBosses: Int = 0,
        crocomire: Int = 0,
        bossPlus1: Int = 0,
        bossPlus2: Int = 0,
        bossPlus3: Int = 0,
        bossPlus4: Int = 0,
        bossPlus5: Int = 0,
        escapeTimer1: Int = 0,
        escapeTimer2: Int = 0,
        escapeTimer3: Int = 0,
        escapeTimer4: Int = 0
    ): Map<String, ByteArray> {
        // Create basic stats ByteArray (22 bytes)
        val basicStats = ByteArray(22)

        // Manually set bytes for each value
        val healthBytes = createByteArray(health)
        basicStats[0] = healthBytes[0]
        basicStats[1] = healthBytes[1]

        val maxHealthBytes = createByteArray(maxHealth)
        basicStats[2] = maxHealthBytes[0]
        basicStats[3] = maxHealthBytes[1]

        val missilesBytes = createByteArray(missiles)
        basicStats[4] = missilesBytes[0]
        basicStats[5] = missilesBytes[1]

        val maxMissilesBytes = createByteArray(maxMissiles)
        basicStats[6] = maxMissilesBytes[0]
        basicStats[7] = maxMissilesBytes[1]

        val supersBytes = createByteArray(supers)
        basicStats[8] = supersBytes[0]
        basicStats[9] = supersBytes[1]

        val maxSupersBytes = createByteArray(maxSupers)
        basicStats[10] = maxSupersBytes[0]
        basicStats[11] = maxSupersBytes[1]

        val powerBombsBytes = createByteArray(powerBombs)
        basicStats[12] = powerBombsBytes[0]
        basicStats[13] = powerBombsBytes[1]

        val maxPowerBombsBytes = createByteArray(maxPowerBombs)
        basicStats[14] = maxPowerBombsBytes[0]
        basicStats[15] = maxPowerBombsBytes[1]

        val maxReserveEnergyBytes = createByteArray(maxReserveEnergy)
        basicStats[16] = maxReserveEnergyBytes[0]
        basicStats[17] = maxReserveEnergyBytes[1]

        val reserveEnergyBytes = createByteArray(reserveEnergy)
        basicStats[18] = reserveEnergyBytes[0]
        basicStats[19] = reserveEnergyBytes[1]

        return mapOf(
            "basic_stats" to basicStats,
            "room_id" to createByteArray(roomId),
            "area_id" to byteArrayOf(areaId.toByte()),
            "game_state" to createByteArray(gameState),
            "player_x" to createByteArray(playerX),
            "player_y" to createByteArray(playerY),
            "items" to createByteArray(items),
            "beams" to createByteArray(beams),
            "main_bosses" to createByteArray(mainBosses),
            "crocomire" to createByteArray(crocomire),
            "boss_plus_1" to createByteArray(bossPlus1),
            "boss_plus_2" to createByteArray(bossPlus2),
            "boss_plus_3" to createByteArray(bossPlus3),
            "boss_plus_4" to createByteArray(bossPlus4),
            "boss_plus_5" to createByteArray(bossPlus5),
            "escape_timer_1" to createByteArray(escapeTimer1),
            "escape_timer_2" to createByteArray(escapeTimer2),
            "escape_timer_3" to createByteArray(escapeTimer3),
            "escape_timer_4" to createByteArray(escapeTimer4)
        )
    }

    @Test
    fun testIntroSceneDetection() = runTest {
        // Test intro scene detection (Crateria, low health, early room)
        val introSceneData = createMemoryData(
            health = 99,
            maxHealth = 99,
            missiles = 0,
            maxMissiles = 0,
            roomId = 500,
            areaId = 0, // Crateria
            items = 0x0004 // Morph ball
        )

        val gameState = parser.parseCompleteGameState(introSceneData)

        // In intro scene, items should be reset to false regardless of memory values
        assertFalse(gameState.items["morph"] ?: true, "Morph ball should be reset to false in intro scene")
        assertFalse(gameState.items["bombs"] ?: true, "Bombs should be reset to false in intro scene")
        assertFalse(gameState.items["varia"] ?: true, "Varia suit should be reset to false in intro scene")

        // Health and area should be parsed correctly
        assertEquals(99, gameState.health, "Health should be parsed correctly")
        assertEquals(0, gameState.areaId, "Area ID should be parsed correctly")
        assertEquals("Crateria", gameState.areaName, "Area name should be parsed correctly")
    }

    @Test
    fun testNormalGameplayItemParsing() = runTest {
        // Test normal gameplay (not intro scene)
        val normalGameData = createMemoryData(
            health = 300,
            maxHealth = 300,
            missiles = 20,
            maxMissiles = 20,
            roomId = 5000,
            areaId = 2, // Norfair
            items = 0x0005 // Morph ball (0x0004) + Varia suit (0x0001)
        )

        val gameState = parser.parseCompleteGameState(normalGameData)

        // In normal gameplay, items should be parsed correctly
        assertTrue(gameState.items["morph"] ?: false, "Morph ball should be parsed correctly")
        assertTrue(gameState.items["varia"] ?: false, "Varia suit should be parsed correctly")
        assertFalse(gameState.items["bombs"] ?: true, "Bombs should be parsed correctly (not collected)")

        // Health and area should be parsed correctly
        assertEquals(300, gameState.health, "Health should be parsed correctly")
        assertEquals(2, gameState.areaId, "Area ID should be parsed correctly")
        assertEquals("Norfair", gameState.areaName, "Area name should be parsed correctly")
    }

    @Test
    fun testEdgeCaseBasicStatsParsing() = runTest {
        // Test edge cases for basic stats parsing

        // 1. Zero health (disconnected state)
        val zeroHealthData = createMemoryData(
            health = 0,
            maxHealth = 0,
            missiles = 0,
            maxMissiles = 0
        )

        val zeroHealthState = parser.parseCompleteGameState(zeroHealthData)
        assertEquals(0, zeroHealthState.health, "Zero health should be parsed correctly")
        assertEquals(0, zeroHealthState.maxHealth, "Zero max health should be parsed correctly")

        // 2. Very high values (potentially corrupted data)
        val highValuesData = createMemoryData(
            health = 9999,
            maxHealth = 9999,
            missiles = 9999,
            maxMissiles = 9999
        )

        val highValuesState = parser.parseCompleteGameState(highValuesData)
        assertEquals(9999, highValuesState.health, "High health values should be parsed correctly")
        assertEquals(9999, highValuesState.maxHealth, "High max health values should be parsed correctly")
        assertEquals(9999, highValuesState.missiles, "High missile values should be parsed correctly")

        // 3. Truncated basic stats data
        val truncatedData = mapOf(
            "basic_stats" to ByteArray(4) { 0x01 } // Only 4 bytes instead of 22
        )

        val truncatedState = parser.parseCompleteGameState(truncatedData)
        assertEquals(0, truncatedState.health, "Truncated data should result in default values")
        assertEquals(0, truncatedState.maxHealth, "Truncated data should result in default values")
    }

    @Test
    fun testComplexBossDetection() = runTest {
        // Test complex boss detection scenarios

        // 1. Multiple bosses defeated
        val multiBossData = createMemoryData(
            mainBosses = 0x0305, // Mother Brain (0x0001) + Bomb Torizo (0x0004) + Kraid (0x0100) + Spore Spawn (0x0200)
            crocomire = 0x0202,  // Crocomire defeated
            bossPlus3 = 0x0301   // Draygon and Phantoon defeated
        )

        val multiBossState = parser.parseCompleteGameState(multiBossData)
        assertTrue(multiBossState.bosses["mother_brain"] ?: false, "Mother Brain should be detected")
        assertTrue(multiBossState.bosses["bomb_torizo"] ?: false, "Bomb Torizo should be detected")
        assertTrue(multiBossState.bosses["kraid"] ?: false, "Kraid should be detected")
        assertTrue(multiBossState.bosses["spore_spawn"] ?: false, "Spore Spawn should be detected")
        assertTrue(multiBossState.bosses["crocomire"] ?: false, "Crocomire should be detected")
        assertTrue(multiBossState.bosses["draygon"] ?: false, "Draygon should be detected")
        assertTrue(multiBossState.bosses["phantoon"] ?: false, "Phantoon should be detected")

        // 2. Botwoon detection (two different patterns)
        val botwoonPattern1Data = createMemoryData(
            bossPlus2 = 0x0104  // Botwoon pattern 1
        )

        val botwoonPattern1State = parser.parseCompleteGameState(botwoonPattern1Data)
        assertTrue(botwoonPattern1State.bosses["botwoon"] ?: false, "Botwoon should be detected with pattern 1")

        val botwoonPattern2Data = createMemoryData(
            bossPlus4 = 0x0003  // Botwoon pattern 2
        )

        val botwoonPattern2State = parser.parseCompleteGameState(botwoonPattern2Data)
        assertTrue(botwoonPattern2State.bosses["botwoon"] ?: false, "Botwoon should be detected with pattern 2")
    }

    @Test
    fun testMotherBrainPhaseDetection() = runTest {
        // Reset MB cache before testing
        parser.resetMotherBrainCache()

        // 1. Mother Brain phase 1 detection
        val mb1Data = createMemoryData(
            mainBosses = 0x0001,  // Mother Brain bit set
            areaId = 5,           // Tourian
            roomId = 56664        // Mother Brain room
        )

        val mb1State = parser.parseCompleteGameState(mb1Data)
        assertTrue(mb1State.bosses["mother_brain"] ?: false, "Mother Brain should be detected")
        // Note: The current implementation doesn't actually detect MB1 vs MB2 phases
        // This would need to be implemented in the parser

        // 2. Mother Brain phase 2 detection (with escape timer)
        val mb2Data = createMemoryData(
            mainBosses = 0x0001,  // Mother Brain bit set
            areaId = 5,           // Tourian
            roomId = 56664,       // Mother Brain room
            escapeTimer1 = 0x0001 // Escape timer active
        )

        val mb2State = parser.parseCompleteGameState(mb2Data)
        assertTrue(mb2State.bosses["mother_brain"] ?: false, "Mother Brain should be detected")
        // Note: The current implementation doesn't actually detect MB1 vs MB2 phases
        // This would need to be implemented in the parser
    }

    @Test
    fun testLocationDataParsing() = runTest {
        // Test various location data scenarios

        // 1. Crateria
        val crateriaData = createMemoryData(
            roomId = 12345,
            areaId = 0,  // Crateria
            playerX = 100,
            playerY = 200
        )

        val crateriaState = parser.parseCompleteGameState(crateriaData)
        assertEquals(12345, crateriaState.roomId, "Room ID should be parsed correctly")
        assertEquals(0, crateriaState.areaId, "Area ID should be parsed correctly")
        assertEquals("Crateria", crateriaState.areaName, "Area name should be parsed correctly")
        assertEquals(100, crateriaState.playerX, "Player X should be parsed correctly")
        assertEquals(200, crateriaState.playerY, "Player Y should be parsed correctly")

        // 2. Tourian
        val tourianData = createMemoryData(
            roomId = 56664,
            areaId = 5,  // Tourian
            playerX = 300,
            playerY = 400
        )

        val tourianState = parser.parseCompleteGameState(tourianData)
        assertEquals(56664, tourianState.roomId, "Room ID should be parsed correctly")
        assertEquals(5, tourianState.areaId, "Area ID should be parsed correctly")
        assertEquals("Tourian", tourianState.areaName, "Area name should be parsed correctly")
        assertEquals(300, tourianState.playerX, "Player X should be parsed correctly")
        assertEquals(400, tourianState.playerY, "Player Y should be parsed correctly")

        // 3. Invalid area ID
        val invalidAreaData = createMemoryData(
            roomId = 12345,
            areaId = 10,  // Invalid area
            playerX = 100,
            playerY = 200
        )

        val invalidAreaState = parser.parseCompleteGameState(invalidAreaData)
        assertEquals(12345, invalidAreaState.roomId, "Room ID should be parsed correctly")
        assertEquals(10, invalidAreaState.areaId, "Area ID should be parsed correctly")
        assertEquals("Unknown", invalidAreaState.areaName, "Invalid area should be 'Unknown'")
    }

    @Test
    fun testErrorHandling() = runTest {
        // Test error handling for various scenarios

        // 1. Empty memory data
        val emptyData = mapOf<String, ByteArray>()
        val emptyState = parser.parseCompleteGameState(emptyData)
        assertEquals(GameState(), emptyState, "Empty data should result in default GameState")

        // 2. Null values in memory data
        // Use @Suppress to handle the unchecked cast warning
        @Suppress("UNCHECKED_CAST")
        val nullData = mapOf(
            "basic_stats" to null,
            "room_id" to null,
            "area_id" to null
        ) as Map<String, ByteArray>

        val nullState = parser.parseCompleteGameState(nullData)
        assertEquals(GameState(), nullState, "Null data should result in default GameState")

        // 3. Invalid ByteArray sizes
        val invalidSizeData = mapOf(
            "basic_stats" to ByteArray(1),
            "room_id" to ByteArray(1),
            "area_id" to ByteArray(0)
        )

        val invalidSizeState = parser.parseCompleteGameState(invalidSizeData)
        assertEquals(0, invalidSizeState.health, "Invalid size data should result in default values")
        assertEquals(0, invalidSizeState.roomId, "Invalid size data should result in default values")
    }
}
