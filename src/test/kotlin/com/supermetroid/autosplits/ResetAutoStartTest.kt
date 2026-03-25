package com.supermetroid.autosplits

import com.supermetroid.gamestate.GameStateConstants
import com.supermetroid.model.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests that auto-start works correctly after resetting a run.
 *
 * Regression: after clicking reset, autoStartEnabled was set to false and
 * previousGameState was nulled, preventing auto-start from detecting the
 * title screen transition (gameState 2 → 31) for the next game.
 */
class ResetAutoStartTest {

    private lateinit var engine: AutoSplitsEngine

    @BeforeEach
    fun setup() {
        engine = AutoSplitsEngine()
        engine.loadProfile(SplitProfile(
            id = "kpdr-any",
            name = "KPDR Any%",
            splits = listOf(
                Split("ceres_station", "Ceres Station", "event"),
                Split("morph_ball", "Morph Ball", "item")
            )
        ))
    }

    @Test
    fun `auto-start is enabled after reset`() {
        // Start and complete a basic run
        engine.setAutoStartEnabled(true)
        engine.toggleRunState("kpdr-any")  // start run
        assertNotNull(engine.splitsState.value.currentRun)

        // Reset the run
        engine.resetRun()
        assertNull(engine.splitsState.value.currentRun)

        // Auto-start should be re-enabled after reset
        assertTrue(engine.autoStartEnabled,
            "Auto-start must be enabled after reset so the next game auto-starts")
    }

    @Test
    fun `title screen transition auto-starts after reset`() {
        // Start a run, then reset
        engine.setAutoStartEnabled(true)
        engine.toggleRunState("kpdr-any")
        engine.resetRun()

        // Simulate title screen → game start transition (gameState 2 → 31)
        val titleScreen = GameState(gameState = GameStateConstants.TITLE_SCREEN)
        val gameStart = GameState(gameState = GameStateConstants.GAME_START_TRANSITION)

        // First poll: establishes previous state
        engine.processGameState(titleScreen)
        assertNull(engine.splitsState.value.currentRun, "Should not start on title screen alone")

        // Second poll: detects transition 2 → 31
        engine.processGameState(gameStart)
        assertNotNull(engine.splitsState.value.currentRun,
            "Auto-start should trigger on title screen transition after reset")
    }

    @Test
    fun `Ceres fallback auto-starts after reset`() {
        // Start a run, then reset
        engine.setAutoStartEnabled(true)
        engine.toggleRunState("kpdr-any")
        engine.resetRun()

        // Simulate being in Ceres with early-game state (fallback auto-start)
        val ceresState = GameState(
            gameState = GameStateConstants.NORMAL_GAMEPLAY,
            areaId = 6,  // Ceres
            maxHealth = 99,
            maxMissiles = 0
        )

        // Two polls to establish state + detect
        engine.processGameState(ceresState)
        engine.processGameState(ceresState)

        assertNotNull(engine.splitsState.value.currentRun,
            "Ceres fallback auto-start should work after reset")
    }
}
