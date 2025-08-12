package com.supermetroid.parser

import kotlin.test.*

class ShipDetectionTest {

    private fun createByteArray(value: Int): ByteArray {
        return byteArrayOf((value and 0xFF).toByte(), ((value shr 8) and 0xFF).toByte())
    }

    private fun parseTestBosses(
        mainBosses: Int = 0,
        shipAi: Int = 0,
        eventFlags: Int = 0,
        areaId: Int = 0,
        roomId: Int = 0,
        playerX: Int = 0,
        playerY: Int = 0,
        health: Int = 99,
        missiles: Int = 0,
        maxMissiles: Int = 0,
        mb1Detected: Boolean = false,
        mb2Detected: Boolean = false
    ): Map<String, Boolean> {
        val parser = GameStateParser()

        // Create location data with the specified values
        val locationData = mapOf(
            "area_id" to areaId,
            "room_id" to roomId,
            "player_x" to playerX,
            "player_y" to playerY,
            "health" to health,
            "missiles" to missiles,
            "max_missiles" to maxMissiles
        )

        // Create a byte array for event flags
        val eventFlagsData = if (eventFlags > 0) {
            byteArrayOf(eventFlags.toByte())
        } else {
            null
        }

        // Call the internal parseBosses function directly
        return parser.parseBosses(
            mainBossesData = createByteArray(mainBosses),
            crocomireData = createByteArray(0),
            bossPlus1Data = createByteArray(0),
            bossPlus2Data = createByteArray(0),
            bossPlus3Data = createByteArray(0),
            bossPlus4Data = createByteArray(0),
            bossPlus5Data = createByteArray(0),
            escapeTimer1Data = createByteArray(0),
            escapeTimer2Data = createByteArray(0),
            escapeTimer3Data = createByteArray(0),
            escapeTimer4Data = createByteArray(0),
            locationData = locationData,
            shipAiData = createByteArray(shipAi),
            eventFlagsData = eventFlagsData
        )
    }

    @Test
    fun testShipDetectionNewGame() {
        // Test ship detection in new game scenario
        // New game: health = 99, missiles = 0, maxMissiles = 0, roomId < 1000
        val newGameResult = parseTestBosses(
            health = 99,
            missiles = 0,
            maxMissiles = 0,
            areaId = 0, // Crateria
            roomId = 356, // Starting room
            shipAi = 0xaa4f, // Ship AI value that would normally trigger detection
            eventFlags = 0x40 // Zebes ablaze flag that would normally trigger detection
        )

        // Ship should NOT be detected in new game
        assertFalse(newGameResult["samus_ship"] ?: true, "Ship should NOT be detected in new game")
    }

    @Test
    fun testShipDetectionEarlyGame() {
        // Test ship detection in early game scenario
        // Early game: in Crateria, starting health, early rooms
        val earlyGameResult = parseTestBosses(
            health = 99,
            missiles = 5,
            maxMissiles = 5,
            areaId = 0, // Crateria
            roomId = 500, // Early room
            shipAi = 0xaa4f, // Ship AI value that would normally trigger detection
            eventFlags = 0x40 // Zebes ablaze flag that would normally trigger detection
        )

        // Ship should NOT be detected in early game
        assertFalse(earlyGameResult["samus_ship"] ?: true, "Ship should NOT be detected in early game")
    }

    @Test
    fun testShipDetectionMidGame() {
        // Test ship detection in mid-game scenario
        // Mid-game: higher health, more missiles, later rooms, but no Mother Brain progress
        val midGameResult = parseTestBosses(
            health = 300,
            missiles = 20,
            maxMissiles = 20,
            areaId = 1, // Brinstar
            roomId = 8192, // Later room
            shipAi = 0, // No ship AI value
            eventFlags = 0, // No Zebes ablaze flag
            mb1Detected = false,
            mb2Detected = false
        )

        // Ship should NOT be detected in mid-game without Mother Brain progress
        assertFalse(midGameResult["samus_ship"] ?: true, "Ship should NOT be detected in mid-game without Mother Brain progress")
    }

    @Test
    fun testShipDetectionEndGameOfficialMethod() {
        // Test ship detection in end-game scenario using official method
        // End-game: Mother Brain defeated, Zebes ablaze, ship AI reached
        val endGameResult = parseTestBosses(
            mainBosses = 0x0001, // Mother Brain bit set
            health = 300,
            missiles = 20,
            maxMissiles = 20,
            areaId = 0, // Crateria
            roomId = 31224, // Ship room
            shipAi = 0xaa4f, // Ship AI value
            eventFlags = 0x40, // Zebes ablaze flag
            mb1Detected = true,
            mb2Detected = true
        )

        // Ship should be detected in end-game with official method
        assertTrue(endGameResult["samus_ship"] ?: false, "Ship should be detected in end-game with official method")
    }

    @Test
    fun testShipDetectionEndGamePositionMethod() {
        // Test ship detection in end-game scenario using position method
        // End-game: Mother Brain defeated, correct position
        val endGameResult = parseTestBosses(
            mainBosses = 0x0001, // Mother Brain bit set
            health = 300,
            missiles = 20,
            maxMissiles = 20,
            areaId = 0, // Crateria
            roomId = 31224, // Ship room
            playerX = 1200, // Ship X position
            playerY = 1200, // Ship Y position
            shipAi = 0, // No ship AI value
            eventFlags = 0, // No Zebes ablaze flag
            mb1Detected = true,
            mb2Detected = true
        )

        // Ship should be detected in end-game with position method
        assertTrue(endGameResult["samus_ship"] ?: false, "Ship should be detected in end-game with position method")
    }

    @Test
    fun testShipDetectionEndGameEmergencyMethod() {
        // Test ship detection in end-game scenario using emergency method
        // End-game: MB2 detected, in escape area, correct position
        val endGameResult = parseTestBosses(
            health = 300,
            missiles = 20,
            maxMissiles = 20,
            areaId = 0, // Crateria
            roomId = 50, // Escape room
            playerX = 1250, // Ship X position
            playerY = 1350, // Ship Y position
            shipAi = 0, // No ship AI value
            eventFlags = 0, // No Zebes ablaze flag
            mb1Detected = true,
            mb2Detected = true
        )

        // Ship should be detected in end-game with emergency method
        assertTrue(endGameResult["samus_ship"] ?: false, "Ship should be detected in end-game with emergency method")
    }

    @Test
    fun testShipDetectionNoMotherBrainProgress() {
        // Test that ship is not detected without Mother Brain progress
        // Even with correct position and area
        val noMBResult = parseTestBosses(
            health = 300,
            missiles = 20,
            maxMissiles = 20,
            areaId = 0, // Crateria
            roomId = 31224, // Ship room
            playerX = 1200, // Ship X position
            playerY = 1200, // Ship Y position
            shipAi = 0xaa4f, // Ship AI value
            eventFlags = 0x40, // Zebes ablaze flag
            mb1Detected = false,
            mb2Detected = false
        )

        // Ship should NOT be detected without Mother Brain progress
        assertFalse(noMBResult["samus_ship"] ?: true, "Ship should NOT be detected without Mother Brain progress")
    }

    @Test
    fun testShipDetectionWrongPosition() {
        // Test that ship is not detected with wrong position
        // Even with Mother Brain progress
        val wrongPosResult = parseTestBosses(
            mainBosses = 0x0001, // Mother Brain bit set
            health = 300,
            missiles = 20,
            maxMissiles = 20,
            areaId = 0, // Crateria
            roomId = 31224, // Ship room
            playerX = 500, // Wrong X position
            playerY = 500, // Wrong Y position
            shipAi = 0, // No ship AI value
            eventFlags = 0, // No Zebes ablaze flag
            mb1Detected = true,
            mb2Detected = true
        )

        // Ship should NOT be detected with wrong position
        assertFalse(wrongPosResult["samus_ship"] ?: true, "Ship should NOT be detected with wrong position")
    }
}
