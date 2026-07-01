package com.supermetroid.gamestate

import com.supermetroid.model.GameState
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*

/**
 * Tests for GameStateParser focusing on boss detection logic
 */
class GameStateParserTest {

    private lateinit var parser: GameStateParser

    @BeforeEach
    fun setup() {
        parser = GameStateParser()
    }

    @Test
    fun `test Golden Torizo detection uses correct norfair boss flag`() {
        // Create memory data with different norfair boss flag values
        val correctFlagMemoryData = createMemoryDataWithNorfairBossFlag(0x04) // Bit 2 = Golden Torizo
        val wrongFlagMemoryData = createMemoryDataWithNorfairBossFlag(0x01) // Bit 0 = Ridley
        val bothFlagsMemoryData = createMemoryDataWithNorfairBossFlag(0x05) // Both Ridley and Golden Torizo

        // Parse game states
        val stateWithCorrectFlag = parser.parseGameState(correctFlagMemoryData)
        val stateWithWrongFlag = parser.parseGameState(wrongFlagMemoryData)
        val stateWithBothFlags = parser.parseGameState(bothFlagsMemoryData)

        // Verify Golden Torizo is only detected with the correct flag
        assertTrue(stateWithCorrectFlag.bosses.goldenTorizo, "Golden Torizo should be detected with norfair boss flag bit 2")
        assertFalse(stateWithWrongFlag.bosses.goldenTorizo, "Golden Torizo should NOT be detected with norfair boss flag bit 0")
        assertTrue(stateWithBothFlags.bosses.goldenTorizo, "Golden Torizo should be detected when both flags are set")
    }

    @Test
    fun `test Ship detection uses correct Mother Brain flag`() {
        // Create memory data with different combinations of flags
        val correctFlagsMemoryData = createMemoryDataForShipDetection(
            zebesAblaze = true,
            shipAi = 0xAA4F,
            motherBrainFlag = 0x02 // Bit 1 = Mother Brain defeated
        )

        val wrongMotherBrainFlagMemoryData = createMemoryDataForShipDetection(
            zebesAblaze = true,
            shipAi = 0xAA4F,
            motherBrainFlag = 0x100 // Wrong flag (bit 8)
        )

        val missingZebesAblazeMemoryData = createMemoryDataForShipDetection(
            zebesAblaze = false,
            shipAi = 0xAA4F,
            motherBrainFlag = 0x02
        )

        val wrongShipAiMemoryData = createMemoryDataForShipDetection(
            zebesAblaze = true,
            shipAi = 0x1234, // Wrong ship AI value
            motherBrainFlag = 0x02
        )

        // Parse game states
        val stateWithCorrectFlags = parser.parseGameState(correctFlagsMemoryData)
        val stateWithWrongMbFlag = parser.parseGameState(wrongMotherBrainFlagMemoryData)
        val stateWithoutZebesAblaze = parser.parseGameState(missingZebesAblazeMemoryData)
        val stateWithWrongShipAi = parser.parseGameState(wrongShipAiMemoryData)

        // Verify ship is only detected with all correct conditions
        assertTrue(stateWithCorrectFlags.bosses.samusShip, "Ship should be detected with all correct flags")
        assertFalse(stateWithWrongMbFlag.bosses.samusShip, "Ship should NOT be detected with wrong Mother Brain flag")
        assertFalse(stateWithoutZebesAblaze.bosses.samusShip, "Ship should NOT be detected without Zebes Ablaze flag")
        assertFalse(stateWithWrongShipAi.bosses.samusShip, "Ship should NOT be detected with wrong Ship AI value")
    }

    @Test
    fun `test G4 detection requires all four major bosses`() {
        // This test verifies that G4 detection requires all four major bosses to be defeated
        // Note: G4 detection is handled in AutoSplitsEngine, not GameStateParser
        // This test is just to verify the individual boss flags are parsed correctly

        // Create memory data with different combinations of boss flags
        val allBossesMemoryData = createMemoryDataWithAllBosses(true, true, true, true)
        val missingKraidMemoryData = createMemoryDataWithAllBosses(false, true, true, true)
        val missingPhantoonMemoryData = createMemoryDataWithAllBosses(true, false, true, true)
        val missingDraygonMemoryData = createMemoryDataWithAllBosses(true, true, false, true)
        val missingRidleyMemoryData = createMemoryDataWithAllBosses(true, true, true, false)

        // Parse game states
        val stateWithAllBosses = parser.parseGameState(allBossesMemoryData)
        val stateWithoutKraid = parser.parseGameState(missingKraidMemoryData)
        val stateWithoutPhantoon = parser.parseGameState(missingPhantoonMemoryData)
        val stateWithoutDraygon = parser.parseGameState(missingDraygonMemoryData)
        val stateWithoutRidley = parser.parseGameState(missingRidleyMemoryData)

        // Verify all bosses are detected correctly
        assertTrue(stateWithAllBosses.bosses.kraid, "Kraid should be detected")
        assertTrue(stateWithAllBosses.bosses.phantoon, "Phantoon should be detected")
        assertTrue(stateWithAllBosses.bosses.draygon, "Draygon should be detected")
        assertTrue(stateWithAllBosses.bosses.ridley, "Ridley should be detected")

        // Verify missing bosses are not detected
        assertFalse(stateWithoutKraid.bosses.kraid, "Kraid should NOT be detected when flag is missing")
        assertFalse(stateWithoutPhantoon.bosses.phantoon, "Phantoon should NOT be detected when flag is missing")
        assertFalse(stateWithoutDraygon.bosses.draygon, "Draygon should NOT be detected when flag is missing")
        assertFalse(stateWithoutRidley.bosses.ridley, "Ridley should NOT be detected when flag is missing")
    }

    @Test
    fun `map rando counters parse normal values`() {
        val state = parser.parseGameState(
            mapOf(
                "deathCount" to le16(12),
                "reloadCount" to le16(34),
                "resetCount" to le16(56)
            )
        )

        assertEquals(12, state.deathCount)
        assertEquals(34, state.reloadCount)
        assertEquals(56, state.resetCount)
    }

    @Test
    fun `map rando counters clamp extreme bad reads to zero`() {
        val state = parser.parseGameState(
            mapOf(
                "deathCount" to le16(65000),
                "reloadCount" to le16(65535),
                "resetCount" to le16(65001)
            )
        )

        assertEquals(0, state.deathCount)
        assertEquals(0, state.reloadCount)
        assertEquals(0, state.resetCount)
    }

    // Helper methods to create test memory data

    private fun le16(value: Int): ByteArray {
        return byteArrayOf(value.toByte(), (value shr 8).toByte())
    }

    private fun createMemoryDataWithNorfairBossFlag(flagValue: Int): Map<String, ByteArray> {
        val result = mutableMapOf<String, ByteArray>()

        // Add norfair boss flag (bossFlags3)
        result["bossFlags3"] = byteArrayOf(flagValue.toByte(), 0)

        // Add other required memory data with default values
        result["health"] = byteArrayOf(100, 0)
        result["maxHealth"] = byteArrayOf(100, 0)
        result["areaId"] = byteArrayOf(2) // Norfair

        return result
    }

    private fun createMemoryDataForShipDetection(
        zebesAblaze: Boolean,
        shipAi: Int,
        motherBrainFlag: Int
    ): Map<String, ByteArray> {
        val result = mutableMapOf<String, ByteArray>()

        // Add event flags (bit 6 = Zebes Ablaze)
        val eventFlagValue = if (zebesAblaze) 0x40 else 0
        result["eventFlags"] = byteArrayOf(eventFlagValue.toByte(), 0)

        // Add ship AI
        result["shipAi"] = byteArrayOf(shipAi.toByte(), (shipAi shr 8).toByte())

        // Add tourian bosses (bit 1 = Mother Brain defeated)
        result["tourianBosses"] = byteArrayOf(motherBrainFlag.toByte(), (motherBrainFlag shr 8).toByte())

        // Add other required memory data with default values
        result["health"] = byteArrayOf(100, 0)
        result["maxHealth"] = byteArrayOf(100, 0)
        result["areaId"] = byteArrayOf(5) // Tourian

        return result
    }

    private fun createMemoryDataWithAllBosses(
        kraid: Boolean,
        phantoon: Boolean,
        draygon: Boolean,
        ridley: Boolean
    ): Map<String, ByteArray> {
        val result = mutableMapOf<String, ByteArray>()

        // Add boss flags
        result["bossFlags1"] = byteArrayOf(if (kraid) 0x00 else 0, if (kraid) 0x01 else 0) // Brinstar bosses - bit 8 (0x0100) = Kraid
        result["bossFlags4"] = byteArrayOf(if (phantoon) 0x01 else 0, 0) // Wrecked Ship bosses - bit 0 = Phantoon
        result["bossFlags5"] = byteArrayOf(if (draygon) 0x01 else 0, 0) // Maridia bosses - bit 0 = Draygon
        result["bossFlags3"] = byteArrayOf(if (ridley) 0x01 else 0, 0) // Norfair bosses - bit 0 = Ridley

        // Add other required memory data with default values
        result["health"] = byteArrayOf(100, 0)
        result["maxHealth"] = byteArrayOf(100, 0)
        result["areaId"] = byteArrayOf(0) // Crateria

        return result
    }
}
