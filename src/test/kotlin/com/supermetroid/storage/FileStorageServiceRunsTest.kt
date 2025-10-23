package com.supermetroid.storage

import com.supermetroid.model.CompletedSplit
import com.supermetroid.model.RunSession
import com.supermetroid.model.SplitTime
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class FileStorageServiceRunsTest {

    @Test
    fun `saveRun and loadAllRuns round trip`(@TempDir tempDir: Path) = runBlocking {
        val storage = FileStorageService(tempDir.toAbsolutePath().toString())

        val start = Clock.System.now().minusMillis(120_000)
        val end = Clock.System.now()

        val split1 = CompletedSplit(
            splitId = "ceres_station",
            time = SplitTime(totalTime = 10_000, segmentTime = 10_000),
            timestamp = start.plusMillis(10_000)
        )
        val split2 = CompletedSplit(
            splitId = "morph_ball",
            time = SplitTime(totalTime = 30_000, segmentTime = 20_000),
            timestamp = start.plusMillis(30_000)
        )

        val run = RunSession(
            id = "run1",
            profileId = "kpdr-any",
            startTime = start,
            endTime = end,
            completedSplits = listOf(split1, split2),
            totalTime = 30_000,
            isPersonalBest = false,
            isPaused = false,
            pausedTime = 0
        )

        storage.saveRun(run)

        val runs = storage.loadAllRuns()
        assertTrue(runs.isNotEmpty(), "Expected at least one run to be loaded")
        val loaded = runs.find { it.id == "run1" }
        assertNotNull(loaded, "Saved run should be present")
        assertEquals(2, loaded!!.completedSplits.size, "Completed splits should round-trip")
        assertEquals(30_000, loaded.totalTime, "Total time should round-trip")
    }

    @Test
    fun `deriveBestSplits picks best segment times across runs`(@TempDir tempDir: Path) = runBlocking {
        val storage = FileStorageService(tempDir.toAbsolutePath().toString())

        val now = Clock.System.now()

        // Run A: slower ceres_station, faster morph_ball
        val runA = RunSession(
            id = "runA",
            profileId = "kpdr-any",
            startTime = now.minusMillis(90_000),
            endTime = now.minusMillis(60_000),
            completedSplits = listOf(
                CompletedSplit(
                    splitId = "ceres_station",
                    time = SplitTime(totalTime = 15_000, segmentTime = 15_000),
                    timestamp = now.minusMillis(75_000)
                ),
                CompletedSplit(
                    splitId = "morph_ball",
                    time = SplitTime(totalTime = 35_000, segmentTime = 20_000),
                    timestamp = now.minusMillis(55_000)
                )
            ),
            totalTime = 35_000
        )

        // Run B: faster ceres_station, slower morph_ball
        val runB = RunSession(
            id = "runB",
            profileId = "kpdr-any",
            startTime = now.minusMillis(50_000),
            endTime = now.minusMillis(20_000),
            completedSplits = listOf(
                CompletedSplit(
                    splitId = "ceres_station",
                    time = SplitTime(totalTime = 9_000, segmentTime = 9_000), // PB for ceres
                    timestamp = now.minusMillis(41_000)
                ),
                CompletedSplit(
                    splitId = "morph_ball",
                    time = SplitTime(totalTime = 34_000, segmentTime = 25_000), // slower than runA
                    timestamp = now.minusMillis(21_000)
                )
            ),
            totalTime = 34_000
        )

        storage.saveRun(runA)
        storage.saveRun(runB)

        val summary = storage.deriveBestSplits("kpdr-any")

        val ceresBest = summary.bestSplitTimes["ceres_station"]
        assertNotNull(ceresBest, "Best split times should include ceres_station")
        assertEquals(9_000, ceresBest!!.segmentTime, "Expected best ceres segment to be 9_000ms (from runB)")
        assertEquals("runB", ceresBest.runId, "Best ceres segment should be from runB")

        val morphBest = summary.bestSplitTimes["morph_ball"]
        assertNotNull(morphBest, "Best split times should include morph_ball")
        assertEquals(20_000, morphBest!!.segmentTime, "Expected best morph segment to be 20_000ms (from runA)")
        assertEquals("runA", morphBest.runId, "Best morph segment should be from runA")
    }
}

// Simple Instant helpers
private fun Instant.plusMillis(ms: Long): Instant = Instant.fromEpochMilliseconds(this.toEpochMilliseconds() + ms)
private fun Instant.minusMillis(ms: Long): Instant = Instant.fromEpochMilliseconds(this.toEpochMilliseconds() - ms)
