package com.supermetroid.parser

import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Tests specifically for Mother Brain phase detection
 * This is a complex feature that deserves its own test file
 */
class MotherBrainPhaseDetectionTest {

    private val parser = GameStateParser()

    /**
     * Helper function to create a ByteArray from an Int value
     * Handles the conversion to little-endian format
     */
    private fun createByteArray(value: Int): ByteArray {
        return byteArrayOf((value and 0xFF).toByte(), ((value shr 8) and 0xFF).toByte())
    }

    /**
     * Helper function to create memory data for Mother Brain tests
     */
    private fun createMBTestData(
        mainBosses: Int = 0x0001, // Mother Brain bit set
        areaId: Int = 5,          // Tourian
        roomId: Int = 56664,      // Mother Brain room
        escapeTimer1: Int = 0,
        escapeTimer2: Int = 0,
        escapeTimer3: Int = 0,
        escapeTimer4: Int = 0,
        bossPlus1: Int = 0,
        bossPlus2: Int = 0,
        bossPlus3: Int = 0,
        bossPlus4: Int = 0
    ): Map<String, ByteArray> {
        return mapOf(
            "main_bosses" to createByteArray(mainBosses),
            "area_id" to byteArrayOf(areaId.toByte()),
            "room_id" to createByteArray(roomId),
            "escape_timer_1" to createByteArray(escapeTimer1),
            "escape_timer_2" to createByteArray(escapeTimer2),
            "escape_timer_3" to createByteArray(escapeTimer3),
            "escape_timer_4" to createByteArray(escapeTimer4),
            "boss_plus_1" to createByteArray(bossPlus1),
            "boss_plus_2" to createByteArray(bossPlus2),
            "boss_plus_3" to createByteArray(bossPlus3),
            "boss_plus_4" to createByteArray(bossPlus4)
        )
    }

    @Test
    fun testMotherBrainBasicDetection() = runTest {
        // Reset MB cache before testing
        parser.resetMotherBrainCache()

        // Test basic Mother Brain detection (bit 0 in main_bosses)
        val mbData = createMBTestData(mainBosses = 0x0001)
        val gameState = parser.parseCompleteGameState(mbData)

        assertTrue(gameState.bosses["mother_brain"] ?: false, "Mother Brain should be detected with bit 0")
    }

    @Test
    fun testMotherBrainPhase1Detection() = runTest {
        // Reset MB cache before testing
        parser.resetMotherBrainCache()

        // Test Mother Brain phase 1 detection
        // In Tourian, in MB room, MB bit set, no escape timer
        val mb1Data = createMBTestData(
            mainBosses = 0x0001,
            areaId = 5,
            roomId = 56664,
            escapeTimer1 = 0
        )

        val gameState = parser.parseCompleteGameState(mb1Data)

        assertTrue(gameState.bosses["mother_brain"] ?: false, "Mother Brain should be detected")
        assertTrue(gameState.bosses["mother_brain_1"] ?: false, "Mother Brain phase 1 should be detected")
        assertFalse(gameState.bosses["mother_brain_2"] ?: true, "Mother Brain phase 2 should not be detected yet")
    }

    @Test
    fun testMotherBrainPhase2Detection() = runTest {
        // Reset MB cache before testing
        parser.resetMotherBrainCache()

        // First detect phase 1
        val mb1Data = createMBTestData(
            mainBosses = 0x0001,
            areaId = 5,
            roomId = 56664
        )

        parser.parseCompleteGameState(mb1Data)

        // Then detect phase 2 with escape timer active
        val mb2Data = createMBTestData(
            mainBosses = 0x0001,
            areaId = 5,
            roomId = 56664,
            escapeTimer1 = 0x0001 // Escape timer active
        )

        val gameState = parser.parseCompleteGameState(mb2Data)

        assertTrue(gameState.bosses["mother_brain"] ?: false, "Mother Brain should be detected")
        assertTrue(gameState.bosses["mother_brain_1"] ?: false, "Mother Brain phase 1 should still be detected")
        assertTrue(gameState.bosses["mother_brain_2"] ?: false, "Mother Brain phase 2 should be detected with escape timer")
    }

    @Test
    fun testMotherBrainPhasePersistence() = runTest {
        // Reset MB cache before testing
        parser.resetMotherBrainCache()

        // First detect both phases
        val mbBothPhasesData = createMBTestData(
            mainBosses = 0x0001,
            areaId = 5,
            roomId = 56664,
            escapeTimer1 = 0x0001 // Escape timer active
        )

        parser.parseCompleteGameState(mbBothPhasesData)

        // Then check if phases persist even when leaving the room
        val outsideRoomData = createMBTestData(
            mainBosses = 0x0001,
            areaId = 5,
            roomId = 12345, // Different room
            escapeTimer1 = 0 // No escape timer
        )

        val gameState = parser.parseCompleteGameState(outsideRoomData)

        assertTrue(gameState.bosses["mother_brain"] ?: false, "Mother Brain should still be detected")
        assertTrue(gameState.bosses["mother_brain_1"] ?: false, "Mother Brain phase 1 should persist")
        assertTrue(gameState.bosses["mother_brain_2"] ?: false, "Mother Brain phase 2 should persist")
    }

    @Test
    fun testMotherBrainCacheReset() = runTest {
        // First detect both phases
        val mbBothPhasesData = createMBTestData(
            mainBosses = 0x0001,
            areaId = 5,
            roomId = 56664,
            escapeTimer1 = 0x0001 // Escape timer active
        )

        parser.parseCompleteGameState(mbBothPhasesData)

        // Then reset the cache
        parser.resetMotherBrainCache()

        // Check if phases are reset
        val afterResetData = createMBTestData(
            mainBosses = 0x0001,
            areaId = 5,
            roomId = 56664,
            escapeTimer1 = 0 // No escape timer
        )

        val gameState = parser.parseCompleteGameState(afterResetData)

        assertTrue(gameState.bosses["mother_brain"] ?: false, "Mother Brain should still be detected")
        assertTrue(gameState.bosses["mother_brain_1"] ?: false, "Mother Brain phase 1 should be detected again")
        assertFalse(gameState.bosses["mother_brain_2"] ?: true, "Mother Brain phase 2 should be reset")
    }

    @Test
    fun testMultipleEscapeTimers() = runTest {
        // Reset MB cache before testing
        parser.resetMotherBrainCache()

        // Test with different escape timer addresses
        val escapeTimer1Data = createMBTestData(
            mainBosses = 0x0001,
            areaId = 5,
            roomId = 56664,
            escapeTimer1 = 0x0001,
            escapeTimer2 = 0,
            escapeTimer3 = 0,
            escapeTimer4 = 0
        )

        val gameState1 = parser.parseCompleteGameState(escapeTimer1Data)
        assertTrue(gameState1.bosses["mother_brain_2"] ?: false, "MB2 should be detected with escape_timer_1")

        // Reset and try with escape_timer_2
        parser.resetMotherBrainCache()
        val escapeTimer2Data = createMBTestData(
            mainBosses = 0x0001,
            areaId = 5,
            roomId = 56664,
            escapeTimer1 = 0,
            escapeTimer2 = 0x0001,
            escapeTimer3 = 0,
            escapeTimer4 = 0
        )

        val gameState2 = parser.parseCompleteGameState(escapeTimer2Data)
        assertTrue(gameState2.bosses["mother_brain_2"] ?: false, "MB2 should be detected with escape_timer_2")

        // Reset and try with escape_timer_3
        parser.resetMotherBrainCache()
        val escapeTimer3Data = createMBTestData(
            mainBosses = 0x0001,
            areaId = 5,
            roomId = 56664,
            escapeTimer1 = 0,
            escapeTimer2 = 0,
            escapeTimer3 = 0x0001,
            escapeTimer4 = 0
        )

        val gameState3 = parser.parseCompleteGameState(escapeTimer3Data)
        assertTrue(gameState3.bosses["mother_brain_2"] ?: false, "MB2 should be detected with escape_timer_3")

        // Reset and try with escape_timer_4
        parser.resetMotherBrainCache()
        val escapeTimer4Data = createMBTestData(
            mainBosses = 0x0001,
            areaId = 5,
            roomId = 56664,
            escapeTimer1 = 0,
            escapeTimer2 = 0,
            escapeTimer3 = 0,
            escapeTimer4 = 0x0001
        )

        val gameState4 = parser.parseCompleteGameState(escapeTimer4Data)
        assertTrue(gameState4.bosses["mother_brain_2"] ?: false, "MB2 should be detected with escape_timer_4")
    }

    @Test
    fun testNuclearResetScenario() = runTest {
        // Test the "nuclear reset" scenario from the Python implementation
        // This is when we're in the MB room with reasonable missile count

        // First detect both phases
        val mbBothPhasesData = createMBTestData(
            mainBosses = 0x0001,
            areaId = 5,
            roomId = 56664,
            escapeTimer1 = 0x0001 // Escape timer active
        )

        parser.parseCompleteGameState(mbBothPhasesData)

        // Create a basic stats ByteArray with reasonable missile count
        val basicStats = ByteArray(22)
        val missilesBytes = createByteArray(20)
        val maxMissilesBytes = createByteArray(25)
        basicStats[4] = missilesBytes[0]
        basicStats[5] = missilesBytes[1]
        basicStats[6] = maxMissilesBytes[0]
        basicStats[7] = maxMissilesBytes[1]

        // Then simulate being back in MB room with reasonable missile count
        val nuclearResetData = createMBTestData(
            mainBosses = 0x0001,
            areaId = 5,
            roomId = 56664,
            escapeTimer1 = 0 // No escape timer
        ) + mapOf("basic_stats" to basicStats)

        val gameState = parser.parseCompleteGameState(nuclearResetData)

        assertTrue(gameState.bosses["mother_brain"] ?: false, "Mother Brain should still be detected")
        assertTrue(gameState.bosses["mother_brain_1"] ?: false, "Mother Brain phase 1 should still be detected")
        // In the Python implementation, MB2 would be reset in this scenario
        // But our current implementation doesn't have this feature yet
    }
}
