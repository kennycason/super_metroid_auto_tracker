package com.supermetroid.autosplits

import com.supermetroid.model.*
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*

class AutoSplitsEngineTest {
    
    private lateinit var engine: AutoSplitsEngine
    private lateinit var testProfile: SplitProfile
    
    @BeforeEach
    fun setup() {
        engine = AutoSplitsEngine()
        testProfile = SplitProfile(
            id = "test-profile",
            name = "Test Profile",
            splits = listOf(
                Split("kraid", "Kraid", "boss", "Kraid defeated"),
                Split("phantoon", "Phantoon", "boss", "Phantoon defeated"),
                Split("draygon", "Draygon", "boss", "Draygon defeated"),
                Split("ridley", "Ridley", "boss", "Ridley defeated"),
                Split("golden_four", "G4", "event", "Entered Tourian"),
                Split("mother_brain_1", "Mother Brain 1", "boss", "MB1 defeated"),
                Split("mother_brain_2", "Mother Brain 2", "boss", "MB2 defeated"),
                Split("ship", "Ship", "event", "Escape complete")
            )
        )
        engine.loadProfile(testProfile)
    }
    
    @Test
    fun `test auto-skip logic for completed bosses`() {
        // Create game state with all 4 bosses defeated
        val gameState = createGameStateWithAllBossesDefeated()
        
        // Start a new run
        engine.startNewRun()
        
        // Process the game state - should auto-skip past G4
        engine.processGameState(gameState)
        
        val currentState = engine.splitsState.value
        val currentRun = currentState.currentRun!!
        
        // Should have auto-skipped kraid, phantoon, draygon, ridley, golden_four
        assertEquals(5, currentRun.completedSplits.size, "Should auto-skip 5 splits (4 bosses + G4)")
        
        // Check that the correct splits were auto-skipped
        val completedSplitIds = currentRun.completedSplits.map { it.splitId }
        assertTrue(completedSplitIds.contains("kraid"), "Should auto-skip Kraid")
        assertTrue(completedSplitIds.contains("phantoon"), "Should auto-skip Phantoon") 
        assertTrue(completedSplitIds.contains("draygon"), "Should auto-skip Draygon")
        assertTrue(completedSplitIds.contains("ridley"), "Should auto-skip Ridley")
        assertTrue(completedSplitIds.contains("golden_four"), "Should auto-skip Golden Four")
        
        // Current split should be MB1
        // Note: currentSplitIndex would be 5 (0-indexed), which is "mother_brain_1"
        val expectedCurrentSplitIndex = 5
        assertEquals("mother_brain_1", testProfile.splits[expectedCurrentSplitIndex].id, 
                    "Current split should be Mother Brain 1")
    }
    
    @Test
    fun `test MB1 auto-skip when already defeated`() {
        // Create game state with all bosses + MB1 defeated
        val gameState = createGameStateWithMB1Defeated()
        
        // Start a new run
        engine.startNewRun()
        
        // Process the game state - should auto-skip past MB1
        engine.processGameState(gameState)
        
        val currentState = engine.splitsState.value
        val currentRun = currentState.currentRun!!
        
        // Should have auto-skipped 6 splits (4 bosses + G4 + MB1)
        assertEquals(6, currentRun.completedSplits.size, "Should auto-skip 6 splits including MB1")
        
        val completedSplitIds = currentRun.completedSplits.map { it.splitId }
        assertTrue(completedSplitIds.contains("mother_brain_1"), "Should auto-skip Mother Brain 1")
        
        // Current split should be MB2
        val expectedCurrentSplitIndex = 6
        assertEquals("mother_brain_2", testProfile.splits[expectedCurrentSplitIndex].id,
                    "Current split should be Mother Brain 2")
    }
    
    @Test
    fun `test MB2 auto-skip when already defeated`() {
        // Create game state with all bosses + MB1 + MB2 defeated
        val gameState = createGameStateWithMB2Defeated()
        
        // Start a new run
        engine.startNewRun()
        
        // Process the game state - should auto-skip past MB2
        engine.processGameState(gameState)
        
        val currentState = engine.splitsState.value
        val currentRun = currentState.currentRun!!
        
        // Should have auto-skipped 7 splits (4 bosses + G4 + MB1 + MB2)
        assertEquals(7, currentRun.completedSplits.size, "Should auto-skip 7 splits including MB2")
        
        val completedSplitIds = currentRun.completedSplits.map { it.splitId }
        assertTrue(completedSplitIds.contains("mother_brain_2"), "Should auto-skip Mother Brain 2")
        
        // Current split should be Ship
        val expectedCurrentSplitIndex = 7
        assertEquals("ship", testProfile.splits[expectedCurrentSplitIndex].id,
                    "Current split should be Ship")
    }
    
    @Test
    fun `test isConditionAlreadyMet for golden_four`() {
        val gameStateAllBosses = createGameStateWithAllBossesDefeated()
        val gameStateSomeBosses = GameState(
            gameState = 8, // NORMAL_GAMEPLAY - required for processing
            bosses = Bosses(
                kraid = true,
                phantoon = true,
                draygon = false, // Missing Draygon
                ridley = true
            )
        )
        
        val g4Split = Split("golden_four", "G4", "event", "G4")
        
        assertTrue(engine.isConditionAlreadyMet(g4Split, gameStateAllBosses),
                  "G4 should be met when all 4 bosses defeated")
        
        assertFalse(engine.isConditionAlreadyMet(g4Split, gameStateSomeBosses),
                   "G4 should NOT be met when missing bosses")
    }
    
    @Test
    fun `test isConditionAlreadyMet for mother_brain phases`() {
        val gameStateWithMB1 = GameState(gameState = 8, bosses = Bosses(motherBrain1 = true))
        val gameStateWithMB2 = GameState(gameState = 8, bosses = Bosses(motherBrain1 = true, motherBrain2 = true))
        val gameStateNoMB = GameState(gameState = 8, bosses = Bosses())
        
        val mb1Split = Split("mother_brain_1", "MB1", "boss", "MB1")
        val mb2Split = Split("mother_brain_2", "MB2", "boss", "MB2")
        
        assertTrue(engine.isConditionAlreadyMet(mb1Split, gameStateWithMB1),
                  "MB1 should be met when motherBrain1 is true")
        
        assertFalse(engine.isConditionAlreadyMet(mb1Split, gameStateNoMB),
                   "MB1 should NOT be met when motherBrain1 is false")
        
        assertTrue(engine.isConditionAlreadyMet(mb2Split, gameStateWithMB2),
                  "MB2 should be met when motherBrain2 is true")
        
        assertFalse(engine.isConditionAlreadyMet(mb2Split, gameStateWithMB1),
                   "MB2 should NOT be met when motherBrain2 is false")
    }
    
    // Helper methods to create test game states
    private fun createGameStateWithAllBossesDefeated(): GameState {
        return GameState(
            gameState = 8, // NORMAL_GAMEPLAY - required for processing
            bosses = Bosses(
                kraid = true,
                phantoon = true, 
                draygon = true,
                ridley = true,
                motherBrain1 = false,
                motherBrain2 = false
            )
        )
    }
    
    private fun createGameStateWithMB1Defeated(): GameState {
        return GameState(
            gameState = 8, // NORMAL_GAMEPLAY - required for processing
            bosses = Bosses(
                kraid = true,
                phantoon = true,
                draygon = true, 
                ridley = true,
                motherBrain1 = true,
                motherBrain2 = false
            )
        )
    }
    
        private fun createGameStateWithMB2Defeated(): GameState {
        return GameState(
            gameState = 8, // NORMAL_GAMEPLAY - required for processing
            bosses = Bosses(
                kraid = true,
                phantoon = true,
                draygon = true,
                ridley = true,
                motherBrain1 = true,
                motherBrain2 = true
            )
        )
    }
}
