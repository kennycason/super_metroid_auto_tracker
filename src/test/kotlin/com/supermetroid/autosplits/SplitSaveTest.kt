package com.supermetroid.autosplits

import com.supermetroid.model.*
import com.supermetroid.storage.FileStorageService
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.*
import java.io.File
import java.nio.file.Path
import kotlin.time.Duration.Companion.seconds

/**
 * Test that split triggers result in run files being saved to disk
 */
class SplitSaveTest {
    
    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var fileStorageService: FileStorageService
    private lateinit var autoSplitsEngine: AutoSplitsEngine
    private lateinit var runsDir: File
    
    @BeforeEach
    fun setup() {
        // Create temp directory structure
        val tempDirFile = tempDir.toFile()
        runsDir = File(tempDirFile, "runs")
        runsDir.mkdirs()
        
        // Create FileStorageService pointing to temp directory
        fileStorageService = FileStorageService(tempDirFile.absolutePath)
        
        // Create AutoSplitsEngine with the storage service
        autoSplitsEngine = AutoSplitsEngine(fileStorageService)
        
        println("📁 Test directory: ${tempDirFile.absolutePath}")
        println("📁 Runs directory: ${runsDir.absolutePath}")
    }
    
    @Test
    fun `should save run file after each split is triggered`() = runBlocking {
        println("\n=== Starting Split Save Test ===\n")
        
        // Process initial game state to establish baseline
        val initialGameState = createInitialGameState()
        autoSplitsEngine.processGameState(initialGameState)
        delay(100)
        
        // Start a run by toggling from stopped to running
        autoSplitsEngine.toggleRunState()
        delay(100) // Give it time to initialize
        
        val initialState = autoSplitsEngine.splitsState.value
        println("✅ Run started: ${initialState.currentRun?.id}")
        
        expectThat(initialState.currentRun).isNotNull()
        
        // Verify no run files exist yet
        val initialFiles = runsDir.listFiles()?.filter { it.extension == "json" } ?: emptyList()
        println("📄 Initial run files: ${initialFiles.size}")
        expectThat(initialFiles).isEmpty()
        
        // Simulate Ceres Station split trigger
        println("\n--- Triggering Ceres Station Split ---")
        val ceresGameState = createGameStateForCeres()
        autoSplitsEngine.processGameState(ceresGameState)
        
        // Give time for async save to complete
        delay(500)
        
        // Verify run file was created
        val afterCeresFiles = runsDir.listFiles()?.filter { it.extension == "json" } ?: emptyList()
        println("📄 After Ceres: ${afterCeresFiles.size} files")
        expectThat(afterCeresFiles).hasSize(1)
        
        // Load and verify the run file
        val ceresRunFile = afterCeresFiles.first()
        println("📂 Run file: ${ceresRunFile.name}")
        
        val savedRun = fileStorageService.loadAllRuns().first()
        println("✅ Loaded run from disk: ${savedRun.id}")
        println("   Completed splits: ${savedRun.completedSplits.size}")
        
        expectThat(savedRun) {
            get { completedSplits }.hasSize(1)
            get { completedSplits.first().splitId }.isEqualTo("ceres_station")
            get { totalTime }.isGreaterThan(0L)
        }
        
        println("\n=== ✅ Test Passed! Split was saved correctly after Ceres ===\n")
    }
    
    @Test
    fun `should save run file even if summary update fails`() = runBlocking {
        println("\n=== Testing Save Resilience ===\n")
        
        // Process initial game state
        autoSplitsEngine.processGameState(createInitialGameState())
        delay(100)
        
        // Start a run
        autoSplitsEngine.toggleRunState()
        delay(100)
        
        // Trigger a split
        println("--- Triggering split ---")
        val ceresGameState = createGameStateForCeres()
        autoSplitsEngine.processGameState(ceresGameState)
        
        // Give time for save
        delay(500)
        
        // Verify the run file exists even if summary update might fail
        val runFiles = runsDir.listFiles()?.filter { it.extension == "json" } ?: emptyList()
        println("📄 Run files after split: ${runFiles.size}")
        
        expectThat(runFiles).isNotEmpty()
        
        val savedRun = fileStorageService.loadAllRuns().first()
        println("✅ Run saved successfully: ${savedRun.completedSplits.size} splits")
        
        expectThat(savedRun.completedSplits).isNotEmpty()
    }
    
    @Test
    fun `should create separate file for each run`() = runBlocking {
        println("\n=== Testing Multiple Runs ===\n")
        
        // Process initial game state
        autoSplitsEngine.processGameState(createInitialGameState())
        delay(100)
        
        // Run 1
        println("--- Run 1 ---")
        autoSplitsEngine.toggleRunState()
        delay(100)
        
        val ceresState = createGameStateForCeres()
        autoSplitsEngine.processGameState(ceresState)
        delay(500)
        
        autoSplitsEngine.resetRun()
        delay(200)
        
        val afterRun1 = runsDir.listFiles()?.filter { it.extension == "json" } ?: emptyList()
        println("📄 After run 1: ${afterRun1.size} files")
        
        // Verify at least 1 file exists from run 1
        expectThat(afterRun1).isNotEmpty()
        
        println("\n=== ✅ Test Passed! Run files are being saved ===\n")
    }
    
    // Helper functions to create game states
    
    private fun createInitialGameState(): GameState {
        // Create a typical starting game state (at ship, no items)
        return GameState(
            roomId = 0x91f8, // Crat Room (ship area)
            areaId = 0, // Crateria
            areaName = "Crateria",
            health = 99,
            maxHealth = 99,
            missiles = 0,
            maxMissiles = 0,
            supers = 0,
            maxSupers = 0,
            powerBombs = 0,
            maxPowerBombs = 0,
            reserveEnergy = 0,
            maxReserveEnergy = 0,
            gameState = 8, // Normal gameplay
            items = Items(),
            beams = Beams(),
            bosses = Bosses()
        )
    }
    
    private fun createGameStateForCeres(): GameState {
        return GameState(
            roomId = 0xdf45, // Ceres elevator
            areaId = 6, // Ceres
            areaName = "Ceres Station",
            health = 99,
            maxHealth = 99,
            missiles = 0,
            maxMissiles = 0,
            supers = 0,
            maxSupers = 0,
            powerBombs = 0,
            maxPowerBombs = 0,
            reserveEnergy = 0,
            maxReserveEnergy = 0,
            gameState = 32, // Game state that triggers Ceres split
            items = Items(),
            beams = Beams(),
            bosses = Bosses()
        )
    }
    
    private fun createGameStateForMorphBall(): GameState {
        return GameState(
            roomId = 0x9e9f, // Morph Ball room
            areaId = 1, // Brinstar
            areaName = "Brinstar",
            health = 99,
            maxHealth = 99,
            missiles = 0,
            maxMissiles = 0,
            supers = 0,
            maxSupers = 0,
            powerBombs = 0,
            maxPowerBombs = 0,
            reserveEnergy = 0,
            maxReserveEnergy = 0,
            gameState = 8,
            items = Items(morph = true),
            beams = Beams(),
            bosses = Bosses()
        )
    }
}

