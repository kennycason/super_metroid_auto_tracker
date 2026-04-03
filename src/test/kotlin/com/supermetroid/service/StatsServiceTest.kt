package com.supermetroid.service

import com.supermetroid.model.*
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StatsServiceTest {

    private val profile = SplitProfile(
        id = "test", name = "Test",
        splits = listOf(
            Split("ceres", "Ceres Station", "event"),
            Split("morph", "Morph Ball", "item"),
            Split("bomb", "Bomb", "item"),
            Split("kraid", "Kraid", "boss"),
            Split("ship", "Ship", "event")
        )
    )

    private fun makeRun(
        id: String,
        totalTime: Long,
        complete: Boolean,
        splitIds: List<String>,
        segmentTimes: List<Long>,
        startMs: Long = 0L
    ): RunSession {
        val start = Instant.fromEpochMilliseconds(startMs)
        var cumulative = 0L
        val splits = splitIds.zip(segmentTimes).map { (splitId, seg) ->
            cumulative += seg
            CompletedSplit(splitId, SplitTime(cumulative, seg), start)
        }
        return RunSession(
            id = id, profileId = "test", startTime = start,
            endTime = if (complete) Instant.fromEpochMilliseconds(startMs + totalTime) else null,
            completedSplits = splits,
            totalTime = totalTime
        )
    }

    // =============================================
    // computeRunStats
    // =============================================

    @Test
    fun `computeRunStats - empty history`() {
        val stats = StatsService.computeRunStats(emptyList())
        assertEquals(0, stats.totalRuns)
        assertEquals(0, stats.completedRuns)
        assertEquals(0, stats.failedRuns)
        assertEquals(0L, stats.totalPlayTime)
        assertNull(stats.fastestRun)
        assertNull(stats.averageCompletedTime)
        assertNull(stats.medianCompletedTime)
    }

    @Test
    fun `computeRunStats - single completed run`() {
        val run = makeRun("r1", 300000, true,
            listOf("ceres", "morph", "bomb", "kraid", "ship"),
            listOf(60000, 50000, 40000, 80000, 70000))
        val stats = StatsService.computeRunStats(listOf(run))

        assertEquals(1, stats.totalRuns)
        assertEquals(1, stats.completedRuns)
        assertEquals(0, stats.failedRuns)
        assertEquals(300000L, stats.totalPlayTime)
        assertEquals(300000L, stats.fastestRun)
        assertEquals(300000L, stats.averageCompletedTime)
        assertEquals(300000L, stats.medianCompletedTime)
    }

    @Test
    fun `computeRunStats - mix of completed and failed runs`() {
        val runs = listOf(
            makeRun("r1", 300000, true,
                listOf("ceres", "morph", "bomb", "kraid", "ship"),
                listOf(60000, 50000, 40000, 80000, 70000), startMs = 1000),
            makeRun("r2", 250000, true,
                listOf("ceres", "morph", "bomb", "kraid", "ship"),
                listOf(50000, 45000, 35000, 70000, 50000), startMs = 2000),
            makeRun("r3", 80000, false,
                listOf("ceres", "morph"),
                listOf(60000, 20000), startMs = 3000)
        )
        val stats = StatsService.computeRunStats(runs)

        assertEquals(3, stats.totalRuns)
        assertEquals(2, stats.completedRuns)
        assertEquals(1, stats.failedRuns)
        // totalPlayTime = sum of all segment times (300000 + 250000 + 80000)
        assertEquals(630000L, stats.totalPlayTime)
        assertEquals(250000L, stats.fastestRun)
        assertEquals(275000L, stats.averageCompletedTime)
        assertEquals(275000L, stats.medianCompletedTime)
    }

    @Test
    fun `computeRunStats - median with odd number of completed runs`() {
        val runs = listOf(
            makeRun("r1", 400000, true, listOf("ceres"), listOf(400000)),
            makeRun("r2", 300000, true, listOf("ceres"), listOf(300000)),
            makeRun("r3", 350000, true, listOf("ceres"), listOf(350000))
        )
        val stats = StatsService.computeRunStats(runs)
        assertEquals(350000L, stats.medianCompletedTime) // sorted: 300, 350, 400 → median = 350
    }

    // =============================================
    // computeSplitCompletions
    // =============================================

    @Test
    fun `computeSplitCompletions - counts per split across all runs`() {
        val runs = listOf(
            makeRun("r1", 300000, true,
                listOf("ceres", "morph", "bomb", "kraid", "ship"),
                listOf(60000, 50000, 40000, 80000, 70000)),
            makeRun("r2", 250000, true,
                listOf("ceres", "morph", "bomb", "kraid", "ship"),
                listOf(50000, 45000, 35000, 70000, 50000)),
            makeRun("r3", 80000, false,
                listOf("ceres", "morph"),
                listOf(60000, 20000))
        )
        val completions = StatsService.computeSplitCompletions(runs, profile)

        assertEquals(5, completions.size)
        assertEquals(3, completions[0].timesCompleted) // ceres: all 3 runs
        assertEquals(3, completions[1].timesCompleted) // morph: all 3 runs
        assertEquals(2, completions[2].timesCompleted) // bomb: 2 complete runs
        assertEquals(2, completions[3].timesCompleted) // kraid: 2 complete runs
        assertEquals(2, completions[4].timesCompleted) // ship: 2 complete runs
    }

    @Test
    fun `computeSplitCompletions - preserves profile order`() {
        val completions = StatsService.computeSplitCompletions(emptyList(), profile)
        assertEquals(listOf("ceres", "morph", "bomb", "kraid", "ship"),
            completions.map { it.splitId })
    }

    @Test
    fun `computeSplitCompletions - decreasing pattern for failed runs`() {
        // 3 runs where each gets progressively further
        val runs = listOf(
            makeRun("r1", 60000, false, listOf("ceres"), listOf(60000)),
            makeRun("r2", 110000, false, listOf("ceres", "morph"), listOf(60000, 50000)),
            makeRun("r3", 300000, true,
                listOf("ceres", "morph", "bomb", "kraid", "ship"),
                listOf(60000, 50000, 40000, 80000, 70000))
        )
        val completions = StatsService.computeSplitCompletions(runs, profile)
        assertEquals(listOf(3, 2, 1, 1, 1), completions.map { it.timesCompleted })
    }

    // =============================================
    // computeRunProgress
    // =============================================

    @Test
    fun `computeRunProgress - chronological order with 1-based index`() {
        val runs = listOf(
            makeRun("r1", 400000, true, listOf("ceres"), listOf(400000), startMs = 3000),
            makeRun("r2", 300000, true, listOf("ceres"), listOf(300000), startMs = 1000),
            makeRun("r3", 350000, false, listOf("ceres"), listOf(350000), startMs = 2000)
        )
        val progress = StatsService.computeRunProgress(runs)

        assertEquals(3, progress.size)
        // Should be sorted by startTime: r2(1000), r3(2000), r1(3000)
        assertEquals(1, progress[0].runIndex)
        assertEquals(300000L, progress[0].totalTime)
        assertTrue(progress[0].isComplete)
        assertEquals(2, progress[1].runIndex)
        assertEquals(350000L, progress[1].totalTime)
        assertFalse(progress[1].isComplete)
        assertEquals(3, progress[2].runIndex)
        assertEquals(400000L, progress[2].totalTime)
    }

    // =============================================
    // computeRollingAverage
    // =============================================

    @Test
    fun `computeRollingAverage - not enough runs returns empty`() {
        val runs = listOf(
            makeRun("r1", 300000, true, listOf("ceres"), listOf(300000)),
            makeRun("r2", 350000, true, listOf("ceres"), listOf(350000))
        )
        assertEquals(emptyList<Pair<Int, Long>>(), StatsService.computeRollingAverage(runs, windowSize = 5))
    }

    @Test
    fun `computeRollingAverage - window of 3`() {
        val runs = (1..5).map { i ->
            makeRun("r$i", (300000 - i * 10000).toLong(), true,
                listOf("ceres"), listOf((300000 - i * 10000).toLong()),
                startMs = i * 1000L)
        }
        val avgs = StatsService.computeRollingAverage(runs, windowSize = 3)

        assertEquals(3, avgs.size) // 5 runs, window 3 → 3 entries
        // Window [r1=290000, r2=280000, r3=270000] → avg = 280000
        assertEquals(280000L, avgs[0].second)
    }

    // =============================================
    // computeSplitDeaths
    // =============================================

    @Test
    fun `computeSplitDeaths - deaths equal reached minus completed`() {
        // 3 runs: all reach ceres, 2 reach morph, 1 reaches bomb onward
        val runs = listOf(
            makeRun("r1", 60000, false, listOf("ceres"), listOf(60000)),
            makeRun("r2", 110000, false, listOf("ceres", "morph"), listOf(60000, 50000)),
            makeRun("r3", 300000, true,
                listOf("ceres", "morph", "bomb", "kraid", "ship"),
                listOf(60000, 50000, 40000, 80000, 70000))
        )
        val deaths = StatsService.computeSplitDeaths(runs, profile)

        // Deaths sorted by count descending
        // ceres: reached=3 (all have splits), completed=3 → deaths=0
        // morph: reached=3 (completed ceres), completed=2 → deaths=1
        // bomb: reached=2 (completed morph), completed=1 → deaths=1
        // kraid: reached=1 (completed bomb), completed=1 → deaths=0
        // ship: reached=1 (completed kraid), completed=1 → deaths=0
        val deathMap = deaths.associate { it.splitId to it.deaths }
        assertEquals(0, deathMap["ceres"])
        assertEquals(1, deathMap["morph"])
        assertEquals(1, deathMap["bomb"])
        assertEquals(0, deathMap["kraid"])
        assertEquals(0, deathMap["ship"])
    }

    @Test
    fun `computeSplitDeaths - sorted by death count descending`() {
        // Many runs dying at morph, a few at bomb
        val runs = listOf(
            makeRun("r1", 60000, false, listOf("ceres"), listOf(60000), startMs = 1000),
            makeRun("r2", 60000, false, listOf("ceres"), listOf(60000), startMs = 2000),
            makeRun("r3", 60000, false, listOf("ceres"), listOf(60000), startMs = 3000),
            makeRun("r4", 110000, false, listOf("ceres", "morph"), listOf(60000, 50000), startMs = 4000),
            makeRun("r5", 300000, true,
                listOf("ceres", "morph", "bomb", "kraid", "ship"),
                listOf(60000, 50000, 40000, 80000, 70000), startMs = 5000)
        )
        val deaths = StatsService.computeSplitDeaths(runs, profile)

        // ceres: reached=5, completed=5 → deaths=0
        // morph: reached=5 (completed ceres), completed=2 → deaths=3 (most deaths!)
        // bomb: reached=2, completed=1 → deaths=1
        // First entry should be the one with most deaths
        assertEquals("morph", deaths[0].splitId)
        assertEquals(3, deaths[0].deaths)
        assertEquals("bomb", deaths[1].splitId)
        assertEquals(1, deaths[1].deaths)
    }

    @Test
    fun `computeSplitDeaths - empty runs returns zeros`() {
        val deaths = StatsService.computeSplitDeaths(emptyList(), profile)
        assertEquals(5, deaths.size)
        assertTrue(deaths.all { it.deaths == 0 })
    }

    @Test
    fun `computeSplitDeaths - all completed runs no deaths`() {
        val runs = listOf(
            makeRun("r1", 300000, true,
                listOf("ceres", "morph", "bomb", "kraid", "ship"),
                listOf(60000, 50000, 40000, 80000, 70000))
        )
        val deaths = StatsService.computeSplitDeaths(runs, profile)
        assertTrue(deaths.all { it.deaths == 0 })
    }

    @Test
    fun `computeSplitDeaths - tracks reached and completed counts`() {
        val runs = listOf(
            makeRun("r1", 60000, false, listOf("ceres"), listOf(60000)),
            makeRun("r2", 110000, false, listOf("ceres", "morph"), listOf(60000, 50000)),
            makeRun("r3", 300000, true,
                listOf("ceres", "morph", "bomb", "kraid", "ship"),
                listOf(60000, 50000, 40000, 80000, 70000))
        )
        val deaths = StatsService.computeSplitDeaths(runs, profile)
        val morphEntry = deaths.find { it.splitId == "morph" }!!

        assertEquals(3, morphEntry.timesReached)    // 3 runs completed ceres
        assertEquals(2, morphEntry.timesCompleted)  // 2 runs completed morph
        assertEquals(1, morphEntry.deaths)           // 3 - 2 = 1
    }

    // =============================================
    // computeRollingAverage
    // =============================================

    @Test
    fun `computeRollingAverage - ignores incomplete runs`() {
        val runs = listOf(
            makeRun("r1", 300000, true, listOf("ceres"), listOf(300000), startMs = 1000),
            makeRun("r2", 50000, false, listOf("ceres"), listOf(50000), startMs = 2000),
            makeRun("r3", 280000, true, listOf("ceres"), listOf(280000), startMs = 3000),
            makeRun("r4", 260000, true, listOf("ceres"), listOf(260000), startMs = 4000)
        )
        val avgs = StatsService.computeRollingAverage(runs, windowSize = 3)

        // Only 3 completed runs → 1 entry
        assertEquals(1, avgs.size)
        assertEquals(280000L, avgs[0].second) // (300000 + 280000 + 260000) / 3
    }
}
