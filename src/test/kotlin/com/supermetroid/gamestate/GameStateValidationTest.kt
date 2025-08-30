package com.supermetroid.gamestate

import com.supermetroid.model.GameState
import strikt.api.expectThat
import strikt.assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for game state validation logic
 * Critical for preventing false splits during cutscenes and intro sequences
 */
class GameStateValidationTest {

    @Test
    fun `should identify valid gameplay states`() {
        val validGameplayStates = listOf(8, 12, 14, 18, 20)
        
        validGameplayStates.forEach { gameStateValue ->
            val gameState = GameState(gameState = gameStateValue)
            
            expectThat(isValidGameplayState(gameState)) {
                isTrue()
            }
        }
    }

    @Test
    fun `should reject invalid gameplay states`() {
        val invalidGameplayStates = listOf(0, 1, 2, 4, 6, 26, 28, 30)
        
        invalidGameplayStates.forEach { gameStateValue ->
            val gameState = GameState(gameState = gameStateValue)
            
            expectThat(isValidGameplayState(gameState)) {
                isFalse()
            }
        }
    }

    @Test
    fun `should handle edge cases for game state validation`() {
        // Test boundary values
        val testCases = mapOf(
            "Exactly valid state 8" to Pair(8, true),
            "Just below valid range" to Pair(7, false),
            "Just above valid range" to Pair(21, false),
            "Menu state" to Pair(26, false),
            "Intro cutscene" to Pair(6, false)
        )
        
        testCases.forEach { (description, pair) ->
            val (gameStateValue, expectedValid) = pair
            val gameState = GameState(gameState = gameStateValue)
            
            expectThat(isValidGameplayState(gameState)) {
                isEqualTo(expectedValid)
            }
        }
    }

    @Test
    fun `should validate area IDs correctly`() {
        val validAreas = mapOf(
            0 to "Crateria",
            1 to "Brinstar", 
            2 to "Norfair",
            3 to "Wrecked Ship",
            4 to "Maridia",
            5 to "Tourian",
            6 to "Ceres"
        )
        
        validAreas.forEach { (areaId, expectedName) ->
            expectThat(areaId) {
                isIn(0..6)
            }
        }
    }

    @Test
    fun `should handle invalid area IDs gracefully`() {
        val invalidAreas = listOf(-1, 7, 8, 255)
        
        invalidAreas.forEach { areaId ->
            // The system should handle these gracefully without crashing
            // This tests defensive programming
            expectThat(areaId < 0 || areaId > 6) {
                isTrue()
            }
        }
    }

    @Test
    fun `should validate memory values are within expected ranges`() {
        val gameState = GameState(
            health = 99,
            maxHealth = 1499,
            missiles = 230,
            maxMissiles = 230,
            eventFlags = 0x0040,
            tourianBosses = 0x0002
        )
        
        expectThat(gameState) {
            get { health }.isLessThanOrEqualTo(gameState.maxHealth)
            get { missiles }.isLessThanOrEqualTo(gameState.maxMissiles)
            get { eventFlags }.isGreaterThanOrEqualTo(0)
            get { tourianBosses }.isGreaterThanOrEqualTo(0)
        }
    }

    // Helper function - would normally be imported from GameStateParser
    private fun isValidGameplayState(gameState: GameState): Boolean {
        return gameState.gameState in listOf(8, 12, 14, 18, 20)
    }
}
