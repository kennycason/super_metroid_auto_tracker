package com.supermetroid.storage

import com.supermetroid.model.RunSession
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.readText

/**
 * Unit tests for the bug where incomplete runs contaminate best split times
 * 
 * BUG: When a user starts a run but doesn't finish it (endTime == null),
 * the incomplete run's splits are being added to the "Best" column.
 * This should NOT happen - only completed runs should contribute to best times.
 * 
 * Test cases based on real user data:
 * - Kenny's runs: Multiple complete runs, one incomplete run with 1 split
 * - MrFoxDemon's runs: 1 complete run (all 24 splits), 1 incomplete run (only 1 split)
 * 
 * The bug manifests as: "im noticing that as i finish the splits i havent finished yet 
 * they get placed into the best column"
 */
class IncompleteRunBestSplitBugTest {

    private val json = Json { 
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    /**
     * Test Kenny's scenario: Has both complete and incomplete runs
     * 
     * Files:
     * - 2025-09-22_14-52-10_kpdr-any_1-06-50.json (COMPLETE)
     * - 2025-10-12_06-25-07_kpdr-any_1-07-14.json (COMPLETE)
     * - 2025-10-19_00-32-06_kpdr-any_0-07-36.json (INCOMPLETE - endTime: null, 1 split)
     * - 2025-10-19_00-32-06_kpdr-any_3-51-59.json (INCOMPLETE - endTime: null, 4 splits)
     */
    @Test
    fun `Kenny scenario - incomplete runs should NOT contribute to best splits`(@TempDir tempDir: Path) = runBlocking {
        val storage = FileStorageService(tempDir.toAbsolutePath().toString())
        
        // Load Kenny's runs from test resources
        val kennyRuns = loadTestRuns("kenny")
        
        // Verify we have the expected runs
        assertEquals(4, kennyRuns.size, "Kenny should have 4 test runs")
        
        // Identify complete vs incomplete runs
        val completeRuns = kennyRuns.filter { it.endTime != null }
        val incompleteRuns = kennyRuns.filter { it.endTime == null }
        
        assertEquals(2, completeRuns.size, "Kenny should have 2 complete runs")
        assertEquals(2, incompleteRuns.size, "Kenny should have 2 incomplete runs")
        
        // Verify incomplete runs have correct characteristics
        val shortIncompleteRun = incompleteRuns.find { it.completedSplits.size == 1 }
        assertNotNull(shortIncompleteRun, "Should have incomplete run with 1 split")
        assertNull(shortIncompleteRun!!.endTime, "Incomplete run should have null endTime")
        assertEquals("ceres_station", shortIncompleteRun.completedSplits[0].splitId)
        
        // Save all runs
        kennyRuns.forEach { storage.saveRun(it) }
        
        // Derive best splits
        val summary = storage.deriveBestSplits("kpdr-any")
        
        // **BUG VERIFICATION**: Best splits should ONLY come from complete runs
        // The incomplete run has ceres_station with segment time: 456308ms (7:36)
        // Complete runs have ceres_station with times: 98117ms (1:38) and others
        
        val ceresBest = summary.bestSplitTimes["ceres_station"]
        assertNotNull(ceresBest, "Best times should include ceres_station")
        
        // ✅ EXPECTED: Best ceres time should come from a COMPLETE run (98117ms or similar)
        // ❌ BUG: If bug exists, it might include the incomplete run's time (456308ms)
        
        // Find which run the best came from
        val bestRunId = ceresBest!!.runId
        val bestRun = kennyRuns.find { it.id == bestRunId }
        assertNotNull(bestRun, "Best run should exist in our test data")
        
        // **CRITICAL TEST**: Best split must come from a COMPLETE run
        assertNotNull(
            bestRun!!.endTime,
            "BUG DETECTED: Best split time came from an INCOMPLETE run (endTime is null)! " +
            "Run ID: ${bestRun.id}, Segment time: ${ceresBest.segmentTime}ms. " +
            "Incomplete runs should NEVER contribute to best splits."
        )
        
        // Verify the best time is reasonable (not from the incomplete run)
        assertTrue(
            ceresBest.segmentTime < 200_000,
            "Best ceres time should be < 200 seconds, got ${ceresBest.segmentTime}ms. " +
            "If this is 456308ms, the incomplete run contaminated the best splits!"
        )
    }

    /**
     * Test MrFoxDemon2 scenario: One FULL complete run (all 24 splits) + one incomplete run (1 split)
     * 
     * This is the IDEAL test case for the bug:
     * - Complete run: 2025-10-28_23-33-33_kpdr-any_1-03-55.json (ALL 24 splits, endTime set)
     * - Incomplete run: 2025-10-28_23-33-33_kpdr-any_0-01-27.json (1 split: ceres_station, endTime: null)
     * 
     * Expected behavior:
     * - All 24 splits should have best times (from the complete run)
     * - The incomplete run's ceres_station should NOT override the complete run's ceres_station
     * - Best splits should all reference the complete run ID
     */
    @Test
    fun `MrFoxDemon2 - full complete run vs incomplete run - all splits should have best times from complete run only`(@TempDir tempDir: Path) = runBlocking {
        val storage = FileStorageService(tempDir.toAbsolutePath().toString())
        
        // Load MrFoxDemon2's runs
        val mrfox2Runs = loadTestRuns("mrfoxdemon2")
        
        println("MrFoxDemon2: Loaded ${mrfox2Runs.size} runs")
        
        // Verify we have exactly 2 runs
        assertEquals(2, mrfox2Runs.size, "MrFoxDemon2 should have exactly 2 runs")
        
        // Identify complete vs incomplete
        val completeRun = mrfox2Runs.find { it.endTime != null }
        val incompleteRun = mrfox2Runs.find { it.endTime == null }
        
        assertNotNull(completeRun, "Should have 1 complete run")
        assertNotNull(incompleteRun, "Should have 1 incomplete run")
        
        // Verify complete run has ALL 24 splits
        assertEquals(24, completeRun!!.completedSplits.size, "Complete run should have all 24 splits")
        assertEquals("ship", completeRun.completedSplits.last().splitId, "Last split should be 'ship'")
        
        // Verify incomplete run has only 1 split
        assertEquals(1, incompleteRun!!.completedSplits.size, "Incomplete run should have only 1 split")
        assertEquals("ceres_station", incompleteRun.completedSplits[0].splitId)
        
        // Both runs have same ceres_station time (87813ms)
        val completeRunCeres = completeRun.completedSplits.find { it.splitId == "ceres_station" }!!
        val incompleteRunCeres = incompleteRun.completedSplits[0]
        assertEquals(87813, completeRunCeres.time.segmentTime)
        assertEquals(87813, incompleteRunCeres.time.segmentTime)
        
        // Save both runs
        mrfox2Runs.forEach { storage.saveRun(it) }
        
        // Derive best splits
        val summary = storage.deriveBestSplits("kpdr-any")
        
        println("Best split times count: ${summary.bestSplitTimes.size}")
        
        // **CRITICAL TEST**: All 24 splits should have best times
        val expectedSplits = listOf(
            "ceres_station", "morph_ball", "first_missile", "bomb", "first_super",
            "charge_beam", "spazer", "kraid", "varia_suit", "hi_jump",
            "speed_booster", "wave_beam", "ice_beam", "first_power_bomb", "phantoon",
            "gravity_suit", "draygon", "space_jump", "plasma_beam", "ridley",
            "golden_four", "mother_brain_1", "mother_brain_2", "ship"
        )
        
        assertEquals(24, summary.bestSplitTimes.size, "Should have best times for all 24 splits")
        
        // Verify each split has a best time
        for (splitId in expectedSplits) {
            val bestTime = summary.bestSplitTimes[splitId]
            assertNotNull(bestTime, "Split '$splitId' should have a best time")
            
            // **CRITICAL**: All best times should come from the COMPLETE run, not the incomplete run
            assertEquals(
                completeRun.id,
                bestTime!!.runId,
                "Best time for '$splitId' should come from complete run ${completeRun.id}, got ${bestTime.runId}"
            )
        }
        
        // Verify ceres_station specifically - both runs have it, but best should be from complete run
        val ceresBest = summary.bestSplitTimes["ceres_station"]!!
        assertEquals(
            completeRun.id,
            ceresBest.runId,
            "Ceres best time MUST come from complete run, not incomplete run!"
        )
        assertEquals(87813, ceresBest.segmentTime)
        
        // Verify splits that ONLY the complete run has
        val completeOnlySplits = listOf("ridley", "golden_four", "mother_brain_1", "mother_brain_2", "ship")
        for (splitId in completeOnlySplits) {
            val bestTime = summary.bestSplitTimes[splitId]
            assertNotNull(
                bestTime,
                "Split '$splitId' exists only in complete run, should have best time"
            )
            assertEquals(
                completeRun.id,
                bestTime!!.runId,
                "Split '$splitId' should reference complete run"
            )
        }
        
        println("✅ All 24 splits have best times from the complete run only!")
    }

    /**
     * Test that incomplete runs are properly filtered out when deriving best splits
     * 
     * This test explicitly verifies the fix: only runs with endTime != null
     * should contribute to best segment calculations.
     */
    @Test
    fun `MrFoxDemon full dataset - best times should only exist for splits that were completed`(@TempDir tempDir: Path) = runBlocking {
        val storage = FileStorageService(tempDir.toAbsolutePath().toString())
        
        // Load ALL of MrFoxDemon's runs
        val mrfoxRuns = loadTestRuns("mrfoxdemon")
        
        println("MrFoxDemon: Loaded ${mrfoxRuns.size} runs")
        val completeRuns = mrfoxRuns.filter { it.endTime != null }
        println("Complete runs: ${completeRuns.size}")
        
        // Save all runs
        mrfoxRuns.forEach { storage.saveRun(it) }
        
        // Derive best splits
        val summary = storage.deriveBestSplits("kpdr-any")
        
        // Find the highest split number that exists in ANY complete run
        val allSplitsFromCompleteRuns = completeRuns.flatMap { run ->
            run.completedSplits.map { it.splitId }
        }.toSet()
        
        println("Splits that exist in complete runs: ${allSplitsFromCompleteRuns.sorted()}")
        println("Best split times in summary: ${summary.bestSplitTimes.keys.sorted()}")
        
        // The expected splits based on the data
        val expectedSplits = listOf(
            "ceres_station", "morph_ball", "first_missile", "bomb", "first_super",
            "charge_beam", "spazer", "kraid", "varia_suit", "hi_jump",
            "speed_booster", "wave_beam", "ice_beam", "first_power_bomb", "phantoon",
            "gravity_suit", "draygon", "space_jump", "plasma_beam"
        )
        
        // These splits should NOT have best times because NO complete run has them
        val shouldNotExist = listOf("ridley", "g4", "mother_brain_1", "mother_brain_2", "ship")
        
        // Verify that splits that don't exist in complete runs don't have best times
        for (splitId in shouldNotExist) {
            val bestTime = summary.bestSplitTimes[splitId]
            assertNull(
                bestTime,
                "BUG: Split '$splitId' has a best time (${bestTime?.segmentTime}ms) but NO complete run contains it!"
            )
        }
        
        // Verify that splits that DO exist have best times
        for (splitId in expectedSplits) {
            val bestTime = summary.bestSplitTimes[splitId]
            assertNotNull(
                bestTime,
                "Split '$splitId' should have a best time since it exists in complete runs"
            )
        }
    }
    
    @Test
    fun `only complete runs should be considered for best segment times`(@TempDir tempDir: Path) = runBlocking {
        val storage = FileStorageService(tempDir.toAbsolutePath().toString())
        
        val now = Instant.parse("2025-11-05T15:00:00Z")
        
        // Create an incomplete run with a VERY FAST segment time
        val incompleteRun = createTestRun(
            id = "incomplete_fast",
            profileId = "test-profile",
            startTime = now.minusSeconds(300),
            endTime = null, // INCOMPLETE!
            splits = listOf(
                TestSplit("ceres_station", totalTime = 1000, segmentTime = 1000), // 1 second - impossibly fast!
                TestSplit("morph_ball", totalTime = 2000, segmentTime = 1000)
            )
        )
        
        // Create a complete run with normal times
        val completeRun = createTestRun(
            id = "complete_normal",
            profileId = "test-profile",
            startTime = now.minusSeconds(200),
            endTime = now.minusSeconds(100), // COMPLETE!
            splits = listOf(
                TestSplit("ceres_station", totalTime = 100_000, segmentTime = 100_000), // 100 seconds - normal
                TestSplit("morph_ball", totalTime = 200_000, segmentTime = 100_000)
            )
        )
        
        // Save both runs
        storage.saveRun(incompleteRun)
        storage.saveRun(completeRun)
        
        // Derive best splits
        val summary = storage.deriveBestSplits("test-profile")
        
        // **CRITICAL TEST**: Best splits should come from the COMPLETE run, not the incomplete one
        val ceresBest = summary.bestSplitTimes["ceres_station"]
        assertNotNull(ceresBest, "Should have best time for ceres_station")
        
        assertEquals(
            completeRun.id,
            ceresBest!!.runId,
            "BUG DETECTED: Best time came from INCOMPLETE run instead of complete run!"
        )
        
        assertEquals(
            100_000,
            ceresBest.segmentTime,
            "Best segment should be from complete run (100s), not incomplete run (1s). " +
            "If this is 1000, incomplete runs are contaminating best times!"
        )
        
        // Verify the same for morph_ball
        val morphBest = summary.bestSplitTimes["morph_ball"]
        assertNotNull(morphBest, "Should have best time for morph_ball")
        assertEquals(
            completeRun.id,
            morphBest!!.runId,
            "Best morph_ball time should come from complete run"
        )
    }

    // =====================================================================
    // Helper Functions
    // =====================================================================

    /**
     * Load test run files from test resources
     */
    private fun loadTestRuns(username: String): List<RunSession> {
        val resourcePath = "src/test/resources/runs/$username"
        val resourceDir = java.io.File(resourcePath)
        
        require(resourceDir.exists()) { "Test resource directory not found: $resourcePath" }
        
        val jsonFiles = resourceDir.listFiles { file -> file.extension == "json" }
            ?: error("No JSON files found in $resourcePath")
        
        return jsonFiles.map { file ->
            json.decodeFromString<RunSession>(file.readText())
        }
    }

    /**
     * Helper to create test runs programmatically
     */
    private data class TestSplit(val id: String, val totalTime: Long, val segmentTime: Long)
    
    private fun createTestRun(
        id: String,
        profileId: String,
        startTime: Instant,
        endTime: Instant?,
        splits: List<TestSplit>
    ): RunSession {
        return RunSession(
            id = id,
            profileId = profileId,
            startTime = startTime,
            endTime = endTime,
            completedSplits = splits.map { split ->
                com.supermetroid.model.CompletedSplit(
                    splitId = split.id,
                    time = com.supermetroid.model.SplitTime(
                        totalTime = split.totalTime,
                        segmentTime = split.segmentTime
                    ),
                    timestamp = startTime.plusSeconds(split.totalTime / 1000)
                )
            },
            totalTime = splits.lastOrNull()?.totalTime ?: 0,
            isPersonalBest = false,
            isPaused = false,
            pausedTime = 0
        )
    }
}

// Extension helper for Instant
private fun Instant.plusSeconds(seconds: Long): Instant = 
    Instant.fromEpochMilliseconds(this.toEpochMilliseconds() + (seconds * 1000))

private fun Instant.minusSeconds(seconds: Long): Instant = 
    Instant.fromEpochMilliseconds(this.toEpochMilliseconds() - (seconds * 1000))

