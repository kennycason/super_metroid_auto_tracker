package com.supermetroid.autosplits

import com.supermetroid.model.Bosses
import com.supermetroid.model.GameState
import com.supermetroid.model.Items
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AdditionalAutomaticSplitsTest {

    private val engine = AutoSplitsEngine()

    @Test
    fun `catalog-only automatic splits have current-state and transition detection`() {
        val detectedStates = mapOf(
            "energy_tank" to GameState(maxHealth = 199),
            "xray_scope" to GameState(items = Items(xray = true)),
            "bomb_torizo" to GameState(bosses = Bosses(bombTorizo = true)),
            "spore_spawn" to GameState(bosses = Bosses(sporeSpawn = true)),
            "crocomire" to GameState(bosses = Bosses(crocomire = true)),
            "golden_torizo" to GameState(bosses = Bosses(goldenTorizo = true)),
            "metroid1" to GameState(bosses = Bosses(metroid1 = true)),
            "metroid2" to GameState(bosses = Bosses(metroid2 = true)),
            "metroid3" to GameState(bosses = Bosses(metroid3 = true)),
            "metroid4" to GameState(bosses = Bosses(metroid4 = true))
        )

        for ((splitId, detectedState) in detectedStates) {
            val split = SplitProfiles.SPLIT_CATALOG_BY_ID.getValue(splitId)
            assertTrue(
                engine.isConditionAlreadyMet(split, detectedState),
                "$splitId should be recognized when loading or starting after the trigger"
            )
            assertTrue(
                engine.checkSplitCondition(split, GameState(), detectedState),
                "$splitId should be recognized on its game-state transition"
            )
        }
    }
}
