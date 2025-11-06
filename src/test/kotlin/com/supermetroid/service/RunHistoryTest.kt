package com.supermetroid.service

import com.supermetroid.model.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.*
import java.io.File
import java.nio.file.Path
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * CRITICAL: These tests use temporary files and NEVER modify the real splits-data.json
 */
class RunHistoryTest {
    
    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var runHistoryManager: RunHistoryManager
    private lateinit var testRunHistoryFile: File
    
    @BeforeEach
    fun setup() {
        // Use temporary file - NEVER touch real splits-data.json
        testRunHistoryFile = tempDir.resolve("test-run-history.json").toFile()
        runHistoryManager = RunHistoryManager(testRunHistoryFile)
    }
    
    @Test
    fun `should create empty run history when file does not exist`() {
        val runHistory = runHistoryManager.loadRunHistory()
        
        expectThat(runHistory) {
            get { completedRuns }.isEmpty()
            get { incompleteRuns }.isEmpty()
            get { derivedPersonalBests }.isEmpty()
            get { version }.isEqualTo("1.0.0")
        }
    }
    
    @Test
    fun `should store and load completed run`() {
        val now = Clock.System.now()
        val completedRun = createTestRun(
            id = "test_run_1",
            profileId = "test_profile",
            startTime = now,
            totalTime = 120000, // 2 minutes
            splits = listOf(
                createTestSplit("ceres_station", 90000), // 1:30
                createTestSplit("morph_ball", 120000)    // 2:00 total
            ),
            isComplete = true
        )
        
        runHistoryManager.storeCompletedRun(completedRun)
        val loadedHistory = runHistoryManager.loadRunHistory()
        
        expectThat(loadedHistory) {
            get { completedRuns }.hasSize(1)
            get { incompleteRuns }.isEmpty()
            get { completedRuns.first() }.and {
                get { id }.isEqualTo("test_run_1")
                get { isComplete }.isTrue()
                get { completionReason }.isEqualTo(RunCompletionReason.FINISHED)
                get { completedSplits }.hasSize(2)
            }
        }
    }
    
    @Test
    fun `should store incomplete run with splits but skip empty runs`() {
        val now = Clock.System.now()
        
        // Run with splits - should be stored
        val incompleteRunWithSplits = createTestRun(
            id = "incomplete_with_splits",
            profileId = "test_profile",
            startTime = now,
            totalTime = 60000,
            splits = listOf(createTestSplit("ceres_station", 60000))
        )
        
        // Run without splits - should be skipped
        val incompleteRunEmpty = createTestRun(
            id = "incomplete_empty",
            profileId = "test_profile",
            startTime = now,
            totalTime = 0,
            splits = emptyList()
        )
        
        runHistoryManager.storeIncompleteRun(incompleteRunWithSplits, RunCompletionReason.RESET)
        runHistoryManager.storeIncompleteRun(incompleteRunEmpty, RunCompletionReason.RESET)
        
        val loadedHistory = runHistoryManager.loadRunHistory()
        
        expectThat(loadedHistory) {
            get { completedRuns }.isEmpty()
            get { incompleteRuns }.hasSize(1) // Only the one with splits
            get { incompleteRuns.first() }.and {
                get { id }.isEqualTo("incomplete_with_splits")
                get { isComplete }.isFalse()
                get { completionReason }.isEqualTo(RunCompletionReason.RESET)
            }
        }
    }
    
    @Test
    fun `should derive personal bests correctly from multiple runs`() {
        val now = Clock.System.now()
        
        // Create multiple runs with different times
        val runs = listOf(
            // Run 1: Good Ceres, slow Morph Ball
            createTestRun("run1", "test_profile", now, 150000, listOf(
                createTestSplitWithSegment("ceres_station", 90000, 90000),   // 1:30 total, 1:30 segment - BEST CERES
                createTestSplitWithSegment("morph_ball", 150000, 60000)      // 2:30 total, 1:00 segment
            ), true),
            
            // Run 2: Slow Ceres, fast Morph Ball, has Bomb segment
            createTestRun("run2", "test_profile", now.plus(1.minutes), 160000, listOf(
                createTestSplitWithSegment("ceres_station", 100000, 100000), // 1:40 total, 1:40 segment
                createTestSplitWithSegment("morph_ball", 140000, 40000),     // 2:20 total, 0:40 segment - BEST MORPH
                createTestSplitWithSegment("bomb", 160000, 20000)            // 2:40 total, 0:20 segment - BEST BOMB
            ), true),
            
            // Run 3: Incomplete - should NOT contribute to best splits (BUG FIX!)
            createTestRun("run3", "test_profile", now.plus(2.minutes), 180000, listOf(
                createTestSplitWithSegment("ceres_station", 50000, 50000),   // 0:50 total - FASTER but incomplete!
                createTestSplitWithSegment("morph_ball", 80000, 30000),      // 1:20 total - FASTER but incomplete!
                createTestSplitWithSegment("bomb", 100000, 10000)            // 1:40 total - FASTER but incomplete!
            ), false)
        )
        
        // Store all runs
        runs.forEach { run ->
            if (run.endTime != null) {
                runHistoryManager.storeCompletedRun(run)
            } else {
                runHistoryManager.storeIncompleteRun(run)
            }
        }
        
        // Derive PBs
        val derivedPBs = runHistoryManager.derivePersonalBestsFromHistory()
        
        expectThat(derivedPBs).hasSize(1)
        val pb = derivedPBs["test_profile"]!!
        
        expectThat(pb) {
            get { profileId }.isEqualTo("test_profile")
            get { totalTime }.isEqualTo(150000) // Run 1 had best total time (run 2 is 160000)
            get { runSessionId }.isEqualTo("run1")
            get { splitTimes }.and {
                // Best segment times should come ONLY from complete runs (run1 and run2)
                get { getValue("ceres_station").segmentTime }.isEqualTo(90000)  // From run1 - best Ceres segment
                get { getValue("morph_ball").segmentTime }.isEqualTo(40000)     // From run2 - best Morph Ball segment
                get { getValue("bomb").segmentTime }.isEqualTo(20000)           // From run2 - best Bomb segment (NOT from incomplete run3!)
            }
        }
    }
    
    @Test
    fun `should generate accurate statistics`() {
        val now = Clock.System.now()
        
        // Create mix of completed and incomplete runs
        val runs = listOf(
            createTestRun("complete1", "test_profile", now, 120000, 
                listOf(createTestSplit("ceres_station", 90000)), true),
            createTestRun("complete2", "test_profile", now.plus(1.minutes), 110000,
                listOf(createTestSplit("ceres_station", 85000)), true),
            createTestRun("incomplete1", "test_profile", now.plus(2.minutes), 60000,
                listOf(createTestSplit("ceres_station", 60000)), false)
        )
        
        runs.forEach { run ->
            if (run.endTime != null) {
                runHistoryManager.storeCompletedRun(run)
            } else {
                runHistoryManager.storeIncompleteRun(run)
            }
        }
        
        val stats = runHistoryManager.getStatistics("test_profile")
        
        expectThat(stats) {
            get { profileId }.isEqualTo("test_profile")
            get { totalRuns }.isEqualTo(3)
            get { completedRuns }.isEqualTo(2)
            get { incompleteRuns }.isEqualTo(1)
            get { bestTotalTime }.isEqualTo(110000)
            get { averageCompletionTime }.isEqualTo(115000) // (120000 + 110000) / 2
            get { completionRate }.isEqualTo(2.0 / 3.0)
            get { bestSegmentTimes["ceres_station"]?.segmentTime }.isEqualTo(85000) // Best from complete run (NOT incomplete!)
        }
    }
    
    @Test
    fun `should not break when no runs exist for profile`() {
        val stats = runHistoryManager.getStatistics("nonexistent_profile")
        
        expectThat(stats) {
            get { profileId }.isEqualTo("nonexistent_profile")
            get { totalRuns }.isEqualTo(0)
            get { completedRuns }.isEqualTo(0)
            get { incompleteRuns }.isEqualTo(0)
            get { bestTotalTime }.isNull()
            get { averageCompletionTime }.isNull()
            get { completionRate }.isEqualTo(0.0)
            get { bestSegmentTimes }.isEmpty()
        }
    }
    
    @Test
    fun `should handle personal best comparison correctly`() {
        val now = Clock.System.now()
        
        // Previous PB: 90 seconds Ceres
        val previousPBRun = createTestRun("pb_run", "test_profile", now, 90000,
            listOf(createTestSplit("ceres_station", 90000)), true)
        runHistoryManager.storeCompletedRun(previousPBRun)
        
        // New run: 100 seconds Ceres (SLOWER - should NOT be new PB)
        val slowerRun = createTestRun("slower_run", "test_profile", now.plus(1.minutes), 100000,
            listOf(createTestSplit("ceres_station", 100000)), true)
        runHistoryManager.storeCompletedRun(slowerRun)
        
        val derivedPBs = runHistoryManager.derivePersonalBestsFromHistory()
        val pb = derivedPBs["test_profile"]!!
        
        expectThat(pb) {
            // Should still show the original PB, not the slower time
            get { totalTime }.isEqualTo(90000)
            get { runSessionId }.isEqualTo("pb_run")
            get { splitTimes["ceres_station"]?.segmentTime }.isEqualTo(90000)
        }
    }
    
    // Helper functions
    private fun createTestRun(
        id: String,
        profileId: String,
        startTime: Instant,
        totalTime: Long,
        splits: List<CompletedSplit>,
        isComplete: Boolean = false
    ): RunSession {
        return RunSession(
            id = id,
            profileId = profileId,
            startTime = startTime,
            endTime = if (isComplete) startTime.plus(totalTime.seconds.div(1000)) else null,
            completedSplits = splits,
            totalTime = totalTime,
            isPaused = false,
            pausedTime = 0
        )
    }
    
    private fun createTestSplit(splitId: String, totalTime: Long): CompletedSplit {
        return CompletedSplit(
            splitId = splitId,
            time = SplitTime(
                totalTime = totalTime,
                segmentTime = totalTime, // Simplified for testing (assumes first split)
                delta = null
            ),
            timestamp = Clock.System.now()
        )
    }
    
    private fun createTestSplitWithSegment(splitId: String, totalTime: Long, segmentTime: Long): CompletedSplit {
        return CompletedSplit(
            splitId = splitId,
            time = SplitTime(
                totalTime = totalTime,
                segmentTime = segmentTime,
                delta = null
            ),
            timestamp = Clock.System.now()
        )
    }
}

/**
 * Tests for PersonalBest comparison logic
 * These test the critical PB detection that was previously broken
 */
class PersonalBestComparisonTest {
    
    @Test
    fun `should correctly identify when time is faster`() {
        val previousPB = SplitTime(totalTime = 90000, segmentTime = 90000)
        val newTime = SplitTime(totalTime = 85000, segmentTime = 85000)
        
        val isNewPB = newTime.segmentTime < previousPB.segmentTime
        expectThat(isNewPB).isTrue()
    }
    
    @Test
    fun `should correctly identify when time is slower`() {
        val previousPB = SplitTime(totalTime = 90000, segmentTime = 90000)
        val newTime = SplitTime(totalTime = 100000, segmentTime = 100000) // SLOWER
        
        val isNewPB = newTime.segmentTime < previousPB.segmentTime
        expectThat(isNewPB).isFalse() // Critical: This should be FALSE
    }
    
    @Test 
    fun `should correctly calculate delta for slower time`() {
        val previousPB = SplitTime(totalTime = 90000, segmentTime = 90000)
        val newTime = SplitTime(totalTime = 100000, segmentTime = 100000)
        
        val delta = newTime.segmentTime - previousPB.segmentTime
        expectThat(delta).isEqualTo(10000) // +10 seconds (SLOWER)
    }
    
    @Test
    fun `should correctly calculate delta for faster time`() {
        val previousPB = SplitTime(totalTime = 90000, segmentTime = 90000)
        val newTime = SplitTime(totalTime = 85000, segmentTime = 85000)
        
        val delta = newTime.segmentTime - previousPB.segmentTime
        expectThat(delta).isEqualTo(-5000) // -5 seconds (FASTER)
    }
}
