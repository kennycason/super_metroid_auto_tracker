package com.supermetroid.autosplits

import com.supermetroid.model.GameState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DeathCounterTest {
    private lateinit var engine: AutoSplitsEngine

    @BeforeEach
    fun setUp() {
        engine = AutoSplitsEngine()
        engine.setDeathCounterEnabled(true)
        engine.startNewRun()
    }

    @Test
    fun `positive HP to zero records one timestamped room event`() {
        engine.processGameState(gameState(health = 99, roomId = 0xA253))
        // Death animation states are intentionally accepted even though split
        // processing filters them out.
        engine.processGameState(gameState(health = 0, roomId = 0xA253, gameState = 0x13))

        val run = engine.splitsState.value.currentRun!!
        assertTrue(run.deathCounterEnabled)
        assertEquals(1, run.deaths.size)
        assertEquals(0xA253, run.deaths.single().roomId)
        assertTrue(run.deaths.single().runTimeMs >= 0)
    }

    @Test
    fun `repeated zero HP polls count once until health recovers`() {
        engine.processGameState(gameState(health = 99))
        engine.processGameState(gameState(health = 0, gameState = 0x13))
        engine.processGameState(gameState(health = 0, gameState = 0x14))
        engine.processGameState(gameState(health = 0, gameState = 0x14))
        assertEquals(1, engine.splitsState.value.currentRun!!.deaths.size)

        engine.processGameState(gameState(health = 99))
        engine.processGameState(gameState(health = 0, gameState = 0x13))
        assertEquals(2, engine.splitsState.value.currentRun!!.deaths.size)
    }

    @Test
    fun `disabled counter does not record deaths`() {
        engine.setDeathCounterEnabled(false)
        engine.processGameState(gameState(health = 99))
        engine.processGameState(gameState(health = 0, gameState = 0x13))

        val run = engine.splitsState.value.currentRun!!
        assertTrue(!run.deathCounterEnabled)
        assertTrue(run.deaths.isEmpty())
    }

    @Test
    fun `zero HP during automatic reserve refill is not a death`() {
        engine.processGameState(gameState(health = 99))
        engine.processGameState(
            gameState(health = 0).copy(reserveEnergy = 100)
        )
        engine.processGameState(gameState(health = 50))

        assertTrue(engine.splitsState.value.currentRun!!.deaths.isEmpty())
    }

    private fun gameState(
        health: Int,
        roomId: Int = 0x91F8,
        gameState: Int = 8
    ) = GameState(health = health, roomId = roomId, gameState = gameState)
}
