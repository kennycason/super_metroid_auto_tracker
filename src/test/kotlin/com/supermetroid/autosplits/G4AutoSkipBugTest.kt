package com.supermetroid.autosplits

import com.supermetroid.model.*
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*

/**
 * SPECIFIC TEST FOR THE G4 AUTO-SKIP BUG
 * 
 * User reports: G4 shows complete in status icons, but auto-splits stuck on G4
 * Problem: AutoSplitsEngine.processGameState() not being called OR auto-skip logic broken
 */
class G4AutoSkipBugTest {
    
    private lateinit var engine: AutoSplitsEngine
    private lateinit var testProfile: SplitProfile
    
    @BeforeEach
    fun setup() {
        engine = AutoSplitsEngine()
        testProfile = SplitProfile(
            id = "kpdr-any",
            name = "KPDR Any%",
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
    fun `test G4 auto-skip with active run scenario`() {
        // Create the EXACT scenario from user's game:
        // - All 4 bosses defeated (Kraid, Phantoon, Draygon, Ridley)
        // - Active run exists 
        // - Current split is stuck on G4
        val gameStateWithAllBossesDefeated = GameState(
            gameState = 8, // NORMAL_GAMEPLAY - required for processing
            bosses = Bosses(
                kraid = true,           // ✓ Defeated
                phantoon = true,        // ✓ Defeated  
                draygon = true,         // ✓ Defeated
                ridley = true,          // ✓ Defeated
                motherBrain1 = false,  // Not yet
                motherBrain2 = false,  // Not yet
                goldenTorizo = true     // This triggers G4 complete
            ),
            areaId = 5 // Tourian
        )
        
        // Start a new run (this should trigger auto-skip)
        engine.startNewRun()
        
        // Verify we have an active run
        val stateAfterStart = engine.splitsState.value
        assertNotNull(stateAfterStart.currentRun, "Should have active run")
        
        // Process the game state - this should auto-skip past G4
        engine.processGameState(gameStateWithAllBossesDefeated)
        
        val currentState = engine.splitsState.value
        val currentRun = currentState.currentRun!!
        
        // Should have auto-skipped 5 splits: kraid, phantoon, draygon, ridley, golden_four
        assertEquals(5, currentRun.completedSplits.size, "Should auto-skip 5 splits (4 bosses + G4)")
        
        // Check that G4 was auto-skipped
        val completedSplitIds = currentRun.completedSplits.map { it.splitId }
        assertTrue(completedSplitIds.contains("golden_four"), "G4 should be auto-skipped")
        
        // Current split should be MB1 (index 5) 
        val currentSplit = engine.getCurrentSplit()
        assertEquals("mother_brain_1", currentSplit?.id, "Current split should be Mother Brain 1")
        
        println("✅ COMPLETED SPLITS: ${completedSplitIds}")
        println("✅ CURRENT SPLIT: ${currentSplit?.name}")
    }
    
    @Test 
    fun `test processGameState called with existing run`() {
        // Simulate the exact scenario: active run, but processGameState not triggering auto-skip
        val gameState = GameState(
            gameState = 8, // NORMAL_GAMEPLAY - required for processing
            bosses = Bosses(
                kraid = true, phantoon = true, draygon = true, ridley = true,
                goldenTorizo = true
            )
        )
        
        // Start run and manually call processGameState
        engine.startNewRun()
        
        // Verify initial state
        var currentState = engine.splitsState.value
        assertEquals(0, currentState.currentRun!!.completedSplits.size, "Should start with 0 completed splits")
        
        // Call processGameState - this is where the bug might be
        engine.processGameState(gameState)
        
        // Check if auto-skip worked
        currentState = engine.splitsState.value
        val completedCount = currentState.currentRun!!.completedSplits.size
        
        assertTrue(completedCount > 0, "Auto-skip should have completed some splits after processGameState")
        assertEquals(5, completedCount, "Should have completed 5 splits (4 bosses + G4)")
    }
    
    @Test
    fun `test auto-skip for resumed run stuck on completed split`() {
        // EXACT scenario: resumed run at split 20, but current split (G4) is already completed
        val gameStateWithAllBossesDefeated = GameState(
            gameState = 8, // NORMAL_GAMEPLAY - required for processing
            bosses = Bosses(
                kraid = true, phantoon = true, draygon = true, ridley = true,
                goldenTorizo = true // This makes G4 completed
            )
        )
        
        // Start run and manually advance to later split (simulating resumed state)
        engine.startNewRun()
        
        // Manually set the run to be at split 20 (like your saved state)
        val initialRun = engine.splitsState.value.currentRun!!
        val resumedRun = initialRun.copy(
            completedSplits = (0..19).map { 
                CompletedSplit("split_$it", SplitTime(1000L, 1000L), kotlinx.datetime.Clock.System.now())
            }
        )
        
        // Use reflection to set the currentSplitIndex to match G4 position (let's say split 4)
        val splitIndexField = AutoSplitsEngine::class.java.getDeclaredField("currentSplitIndex")
        splitIndexField.isAccessible = true
        splitIndexField.set(engine, 4) // G4 is at index 4
        
        // Update the state
        val currentState = engine.splitsState.value
        val stateField = AutoSplitsEngine::class.java.getDeclaredField("_splitsState")
        stateField.isAccessible = true
        val mutableStateFlow = stateField.get(engine) as kotlinx.coroutines.flow.MutableStateFlow<SplitsState>
        mutableStateFlow.value = currentState.copy(currentRun = resumedRun)
        
        // Process game state - should auto-skip G4 even though run has other completed splits
        engine.processGameState(gameStateWithAllBossesDefeated)
        
        val finalState = engine.splitsState.value
        val finalRun = finalState.currentRun!!
        
        // The run should have auto-skipped G4 and moved to next split
        val currentSplit = engine.getCurrentSplit()
        assertNotEquals("golden_four", currentSplit?.id, 
                       "Should have auto-skipped past G4 even in resumed run")
        
        println("✅ FINAL CURRENT SPLIT: ${currentSplit?.name}")
    }
    
    @Test
    fun `test isConditionAlreadyMet for G4 specifically`() {
        val gameStateG4Complete = GameState(
            gameState = 8, // NORMAL_GAMEPLAY - required for processing
            bosses = Bosses(
                kraid = true,
                phantoon = true, 
                draygon = true,
                ridley = true
            )
        )
        
        val gameStateG4Incomplete = GameState(
            gameState = 8, // NORMAL_GAMEPLAY - required for processing
            bosses = Bosses(
                kraid = true,
                phantoon = true,
                draygon = false, // Missing Draygon
                ridley = true
            )
        )
        
        val g4Split = Split("golden_four", "G4", "event", "G4")
        
        assertTrue(engine.isConditionAlreadyMet(g4Split, gameStateG4Complete), 
                  "G4 should be met when all 4 bosses defeated")
        
        assertFalse(engine.isConditionAlreadyMet(g4Split, gameStateG4Incomplete),
                   "G4 should NOT be met when missing bosses")
    }
}
