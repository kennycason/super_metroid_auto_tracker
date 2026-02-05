package com.supermetroid.autosplits

import com.supermetroid.model.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import strikt.api.expectThat
import strikt.assertions.*
import kotlin.time.Duration.Companion.minutes

/**
 * Tests for PersonalBest.totalTime calculation in AutoSplitsEngine
 * 
 * Bug fixed: PersonalBest.totalTime was set from the first run encountered and never updated
 * when faster runs were found. This caused the "Personal Best" row to show a different
 * (slower) time than the "BEST" column which correctly showed the fastest run.
 */
class PersonalBestTotalTimeTest {
    
    private lateinit var engine: AutoSplitsEngine
    
    @BeforeEach
    fun setup() {
        engine = AutoSplitsEngine(null)
        engine.loadProfile(SplitProfiles.KPDR_ANY)
    }
    
    @Test
    fun `PersonalBest totalTime should be the fastest run not the first run`() {
        val now = Clock.System.now()
        
        // Create runs in order: slow run first, then fast run
        // This reproduces the bug where first run's totalTime was used
        val slowRun = createCompleteRun(
            id = "slow_run",
            profileId = "kpdr-any",
            startTime = now,
            totalTime = 4155000L, // 1:09:15 (the slower time the user saw)
            splits = listOf(
                createSplit("ceres_station", 90000L, 90000L),
                createSplit("morph_ball", 120000L, 30000L)
            )
        )
        
        val fastRun = createCompleteRun(
            id = "fast_run",
            profileId = "kpdr-any",
            startTime = now.plus(1.minutes),
            totalTime = 3554480L, // 59:14.48 (the actual PB)
            splits = listOf(
                createSplit("ceres_station", 85000L, 85000L),
                createSplit("morph_ball", 110000L, 25000L)
            )
        )
        
        // Load state with both runs - slow run first in the list
        val state = SplitsState(
            currentRun = null,
            personalBests = emptyMap(),
            runHistory = listOf(slowRun, fastRun)
        )
        
        engine.loadSavedState(state)
        
        // Get the updated state
        val updatedState = engine.splitsState.value
        val personalBest = updatedState.personalBests["kpdr-any"]
        
        expectThat(personalBest).isNotNull().and {
            // The totalTime should be from the FASTEST run, not the first run
            get { totalTime }.isEqualTo(3554480L) // 59:14.48, not 1:09:15
            get { runSessionId }.isEqualTo("fast_run")
        }
    }
    
    @Test
    fun `PersonalBest totalTime should update when new faster run is found`() {
        val now = Clock.System.now()
        
        // Start with one run
        val firstRun = createCompleteRun(
            id = "first_run",
            profileId = "kpdr-any",
            startTime = now,
            totalTime = 4000000L, // 1:06:40
            splits = listOf(createSplit("ceres_station", 90000L, 90000L))
        )
        
        val initialState = SplitsState(
            currentRun = null,
            personalBests = emptyMap(),
            runHistory = listOf(firstRun)
        )
        
        engine.loadSavedState(initialState)
        
        // Verify initial PB
        var pb = engine.splitsState.value.personalBests["kpdr-any"]
        expectThat(pb).isNotNull().and {
            get { totalTime }.isEqualTo(4000000L)
        }
        
        // Add a faster run
        val fasterRun = createCompleteRun(
            id = "faster_run",
            profileId = "kpdr-any",
            startTime = now.plus(1.minutes),
            totalTime = 3500000L, // 58:20 (faster!)
            splits = listOf(createSplit("ceres_station", 85000L, 85000L))
        )
        
        val updatedState = SplitsState(
            currentRun = null,
            personalBests = emptyMap(),
            runHistory = listOf(firstRun, fasterRun)
        )
        
        engine.loadSavedState(updatedState)
        
        // Verify PB updated to faster time
        pb = engine.splitsState.value.personalBests["kpdr-any"]
        expectThat(pb).isNotNull().and {
            get { totalTime }.isEqualTo(3500000L) // Should be the faster time
            get { runSessionId }.isEqualTo("faster_run")
        }
    }
    
    @Test
    fun `PersonalBest should keep best segment times even when faster run has slower segments`() {
        val now = Clock.System.now()
        
        // Run 1: slower overall, but has the best ceres_station segment
        val run1 = createCompleteRun(
            id = "run1",
            profileId = "kpdr-any",
            startTime = now,
            totalTime = 4000000L,
            splits = listOf(
                createSplit("ceres_station", 80000L, 80000L), // Best ceres!
                createSplit("morph_ball", 130000L, 50000L)
            )
        )
        
        // Run 2: faster overall, but slower ceres_station
        val run2 = createCompleteRun(
            id = "run2",
            profileId = "kpdr-any",
            startTime = now.plus(1.minutes),
            totalTime = 3500000L, // Faster overall
            splits = listOf(
                createSplit("ceres_station", 90000L, 90000L), // Slower ceres
                createSplit("morph_ball", 110000L, 20000L)    // Best morph!
            )
        )
        
        val state = SplitsState(
            currentRun = null,
            personalBests = emptyMap(),
            runHistory = listOf(run1, run2)
        )
        
        engine.loadSavedState(state)
        
        val pb = engine.splitsState.value.personalBests["kpdr-any"]
        
        expectThat(pb).isNotNull().and {
            // totalTime should be from fastest run
            get { totalTime }.isEqualTo(3500000L)
            get { runSessionId }.isEqualTo("run2")
            
            // But segment times should be the best from any run
            get { splitTimes["ceres_station"]?.segmentTime }.isEqualTo(80000L) // From run1
            get { splitTimes["morph_ball"]?.segmentTime }.isEqualTo(20000L)    // From run2
        }
    }
    
    @Test
    fun `PersonalBests should be separate per profile`() {
        val now = Clock.System.now()
        
        val kpdrRun = createCompleteRun(
            id = "kpdr_run",
            profileId = "kpdr-any",
            startTime = now,
            totalTime = 3500000L,
            splits = listOf(createSplit("ceres_station", 85000L, 85000L))
        )
        
        val lowPercentRun = createCompleteRun(
            id = "low_run",
            profileId = "low-14-ice",
            startTime = now.plus(1.minutes),
            totalTime = 4500000L, // Different time for different profile
            splits = listOf(createSplit("ceres_station", 90000L, 90000L))
        )
        
        val state = SplitsState(
            currentRun = null,
            personalBests = emptyMap(),
            runHistory = listOf(kpdrRun, lowPercentRun)
        )
        
        engine.loadSavedState(state)
        
        val kpdrPB = engine.splitsState.value.personalBests["kpdr-any"]
        val lowPB = engine.splitsState.value.personalBests["low-14-ice"]
        
        expectThat(kpdrPB).isNotNull().and {
            get { totalTime }.isEqualTo(3500000L)
            get { profileId }.isEqualTo("kpdr-any")
        }
        
        expectThat(lowPB).isNotNull().and {
            get { totalTime }.isEqualTo(4500000L)
            get { profileId }.isEqualTo("low-14-ice")
        }
    }
    
    // Helper functions
    
    private fun createCompleteRun(
        id: String,
        profileId: String,
        startTime: Instant,
        totalTime: Long,
        splits: List<CompletedSplit>
    ): RunSession {
        return RunSession(
            id = id,
            profileId = profileId,
            startTime = startTime,
            endTime = startTime.plus(kotlin.time.Duration.parse("${totalTime}ms")),
            completedSplits = splits,
            totalTime = totalTime,
            isPaused = false,
            pausedTime = 0
        )
    }
    
    private fun createSplit(
        splitId: String,
        totalTime: Long,
        segmentTime: Long
    ): CompletedSplit {
        return CompletedSplit(
            splitId = splitId,
            time = SplitTime(
                totalTime = totalTime,
                segmentTime = segmentTime,
                delta = null,
                originalDelta = null
            ),
            timestamp = Clock.System.now()
        )
    }
}
