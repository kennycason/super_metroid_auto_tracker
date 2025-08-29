package com.supermetroid.gamestate

import com.supermetroid.model.GameState
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*

/**
 * Tests specifically for ship detection logic in GameStateParser
 */
class ShipDetectionTest {

    private lateinit var parser: GameStateParser

    @BeforeEach
    fun setup() {
        parser = GameStateParser()
    }

    @Test
    fun `test ship detection uses correct Mother Brain flag`() {
        // Create memory data with the correct conditions for ship detection
        val correctMemoryData = createMemoryDataForShipDetection(
            zebesAblaze = true,
            shipAi = 0xAA4F,
            motherBrainFlag = 0x02 // Bit 1 = Mother Brain defeated
        )

        // Create memory data with the wrong Mother Brain flag
        val wrongMotherBrainFlagMemoryData = createMemoryDataForShipDetection(
            zebesAblaze = true,
            shipAi = 0xAA4F,
            motherBrainFlag = 0x100 // Wrong flag (bit 8)
        )

        // Parse game states
        val stateWithCorrectFlag = parser.parseGameState(correctMemoryData)
        val stateWithWrongFlag = parser.parseGameState(wrongMotherBrainFlagMemoryData)

        // Verify ship is only detected with the correct Mother Brain flag
        assertTrue(stateWithCorrectFlag.bosses.samusShip, "Ship should be detected with correct Mother Brain flag (bit 1)")
        assertFalse(stateWithWrongFlag.bosses.samusShip, "Ship should NOT be detected with wrong Mother Brain flag (bit 8)")

        // Print debug info
        println("Ship detection with correct flag: ${stateWithCorrectFlag.bosses.samusShip}")
        println("Ship detection with wrong flag: ${stateWithWrongFlag.bosses.samusShip}")
        println("Memory data with correct flag: $correctMemoryData")
        println("Memory data with wrong flag: $wrongMotherBrainFlagMemoryData")
    }

    @Test
    fun `test ship detection requires all three conditions`() {
        // Create memory data with all three conditions met
        val allConditionsMetMemoryData = createMemoryDataForShipDetection(
            zebesAblaze = true,
            shipAi = 0xAA4F,
            motherBrainFlag = 0x02
        )

        // Create memory data with missing Zebes Ablaze flag
        val missingZebesAblazeMemoryData = createMemoryDataForShipDetection(
            zebesAblaze = false,
            shipAi = 0xAA4F,
            motherBrainFlag = 0x02
        )

        // Create memory data with wrong Ship AI value
        val wrongShipAiMemoryData = createMemoryDataForShipDetection(
            zebesAblaze = true,
            shipAi = 0x1234, // Wrong Ship AI value
            motherBrainFlag = 0x02
        )

        // Create memory data with missing Mother Brain flag
        val missingMotherBrainFlagMemoryData = createMemoryDataForShipDetection(
            zebesAblaze = true,
            shipAi = 0xAA4F,
            motherBrainFlag = 0x00 // No Mother Brain flag
        )

        // Parse game states
        val stateWithAllConditions = parser.parseGameState(allConditionsMetMemoryData)
        val stateWithoutZebesAblaze = parser.parseGameState(missingZebesAblazeMemoryData)
        val stateWithWrongShipAi = parser.parseGameState(wrongShipAiMemoryData)
        val stateWithoutMotherBrainFlag = parser.parseGameState(missingMotherBrainFlagMemoryData)

        // Verify ship is only detected when all conditions are met
        assertTrue(stateWithAllConditions.bosses.samusShip, "Ship should be detected when all conditions are met")
        assertFalse(stateWithoutZebesAblaze.bosses.samusShip, "Ship should NOT be detected without Zebes Ablaze flag")
        assertFalse(stateWithWrongShipAi.bosses.samusShip, "Ship should NOT be detected with wrong Ship AI value")
        assertFalse(stateWithoutMotherBrainFlag.bosses.samusShip, "Ship should NOT be detected without Mother Brain flag")
    }

    // Helper method to create test memory data for ship detection
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
}
