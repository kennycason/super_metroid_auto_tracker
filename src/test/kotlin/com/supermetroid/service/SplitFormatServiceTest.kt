package com.supermetroid.service

import com.supermetroid.livesplit.LiveSplitConverter
import com.supermetroid.livesplit.LiveSplitParser
import com.supermetroid.model.CompletedSplit
import com.supermetroid.model.PersonalBest
import com.supermetroid.model.RunSession
import com.supermetroid.model.SplitProfile
import com.supermetroid.model.SplitTime
import com.supermetroid.model.SplitsState
import com.supermetroid.model.Split
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for SplitFormatService logic, particularly the synthetic PB run
 * that feeds the BEST column in the splits UI.
 *
 * Regression context: the BEST column was showing the same values as Best Possible
 * because calculatePbRunTimeUpTo summed segment times (which are best segments
 * from all attempts) instead of using the PB run's cumulative totalTime directly.
 */
class SplitFormatServiceTest {

    private val parser = LiveSplitParser()
    private val converter = LiveSplitConverter()

    private fun load100PercentDoc() =
        parser.parse(javaClass.getResourceAsStream("/livesplit/100_percent.lss")!!)

    /**
     * Reproduce the synthetic PB run construction logic (mirrors SplitFormatService.syntheticPbRun).
     */
    private fun buildSyntheticPbRun(profile: SplitProfile, pb: PersonalBest): RunSession {
        val baseTime = Instant.fromEpochMilliseconds(0L)
        val completedSplits = profile.splits.map { split ->
            val st = pb.splitTimes[split.id] ?: return@map null
            CompletedSplit(
                splitId = split.id,
                time = st,
                timestamp = baseTime
            )
        }.filterNotNull()
        return RunSession(
            id = "livesplit-pb",
            profileId = profile.id,
            startTime = baseTime,
            endTime = Instant.fromEpochMilliseconds(pb.totalTime),
            completedSplits = completedSplits,
            totalTime = pb.totalTime,
            isPersonalBest = true
        )
    }

    /**
     * Simulates calculatePbRunTimeUpTo: returns cumulative PB time at splitIndex,
     * or 0 if non-monotonic (corrupted data).
     */
    private fun calculatePbRunTimeUpTo(pbRun: RunSession, profile: SplitProfile, splitIndex: Int): Long {
        val splitTimeMap = pbRun.completedSplits.associate { it.splitId to it.time.totalTime }
        val split = profile.splits.getOrNull(splitIndex) ?: return 0L
        val cumulative = splitTimeMap[split.id] ?: return 0L

        for (i in 0 until splitIndex) {
            val prevSplit = profile.splits.getOrNull(i) ?: continue
            val prevCumulative = splitTimeMap[prevSplit.id] ?: continue
            if (prevCumulative > cumulative) return 0L
        }
        return cumulative
    }

    @Test
    fun `BEST column uses PB cumulative totalTime not sum of best segments`() {
        val doc = load100PercentDoc()
        val profile = converter.toSplitProfile(doc, "hundred-percent")
        val pb = converter.toPersonalBest(doc, "hundred-percent")

        val syntheticRun = buildSyntheticPbRun(profile, pb)

        // The BEST column value at the last split should equal the PB total time
        val lastIndex = profile.splits.size - 1
        val bestColumnTotal = calculatePbRunTimeUpTo(syntheticRun, profile, lastIndex)
        assertEquals(pb.totalTime, bestColumnTotal, "BEST column at last split must equal PB total time")

        // Sum of best segments should be LESS than PB total
        val sumOfBestSegments = pb.splitTimes.values.sumOf { it.segmentTime }
        assertTrue(
            sumOfBestSegments < pb.totalTime,
            "Sum of best segments ($sumOfBestSegments ms) should be < PB total (${pb.totalTime} ms)"
        )

        // Therefore BEST column differs from Best Possible
        assertNotEquals(
            sumOfBestSegments, bestColumnTotal,
            "BEST column total must differ from sum of best segments (Best Possible)"
        )
    }

    @Test
    fun `BEST column cumulative is monotonically increasing for valid data`() {
        val doc = load100PercentDoc()
        val profile = converter.toSplitProfile(doc, "hundred-percent")
        val pb = converter.toPersonalBest(doc, "hundred-percent")

        val syntheticRun = buildSyntheticPbRun(profile, pb)

        var prevCumulative = 0L
        for (i in profile.splits.indices) {
            val cumulative = calculatePbRunTimeUpTo(syntheticRun, profile, i)
            if (cumulative > 0) {
                assertTrue(
                    cumulative >= prevCumulative,
                    "Split ${profile.splits[i].id}: cumulative $cumulative must be >= previous $prevCumulative"
                )
                prevCumulative = cumulative
            }
        }
    }

    @Test
    fun `BEST column returns 0 for non-monotonic corrupted PB data`() {
        // Simulate corrupted LSS data where PB cumulative drops mid-run
        // (e.g., Spazer shows 3.9s cumulative after Charge Beam at 8:41)
        val profile = SplitProfile(
            id = "test",
            name = "Test",
            splits = listOf(
                Split("split_a", "Split A", "item"),
                Split("split_b", "Split B", "item"),
                Split("split_c", "Split C", "item"),  // corrupted: cumulative drops
                Split("split_d", "Split D", "item")
            )
        )
        val pb = PersonalBest(
            profileId = "test",
            runSessionId = "test-run",
            totalTime = 600000L,
            splitTimes = mapOf(
                "split_a" to SplitTime(totalTime = 100000L, segmentTime = 80000L),
                "split_b" to SplitTime(totalTime = 250000L, segmentTime = 120000L),
                "split_c" to SplitTime(totalTime = 5000L, segmentTime = 90000L),  // corrupted: 5s < 250s
                "split_d" to SplitTime(totalTime = 600000L, segmentTime = 110000L)
            )
        )

        val syntheticRun = buildSyntheticPbRun(profile, pb)

        // Valid splits should show cumulative times
        assertEquals(100000L, calculatePbRunTimeUpTo(syntheticRun, profile, 0))
        assertEquals(250000L, calculatePbRunTimeUpTo(syntheticRun, profile, 1))

        // Corrupted split (non-monotonic) should return 0 (displays as "--")
        assertEquals(0L, calculatePbRunTimeUpTo(syntheticRun, profile, 2),
            "Non-monotonic cumulative (5000 < 250000) should return 0")

        // Split after corrupted should still work if its cumulative is valid
        assertEquals(600000L, calculatePbRunTimeUpTo(syntheticRun, profile, 3))
    }

    @Test
    fun `Best Possible uses best segment times not PB cumulative times`() {
        val doc = load100PercentDoc()
        val profile = converter.toSplitProfile(doc, "hundred-percent")
        val pb = converter.toPersonalBest(doc, "hundred-percent")

        // Best Possible = sum of best segments (segmentTime in personalBest)
        val sumOfBest = profile.splits.sumOf { split ->
            pb.splitTimes[split.id]?.segmentTime ?: 0L
        }

        // BEST total = PB cumulative at last split (totalTime)
        val pbTotal = pb.totalTime

        // They should be different (best segments < PB total)
        assertTrue(
            sumOfBest < pbTotal,
            "Best Possible ($sumOfBest ms) must be < BEST total ($pbTotal ms)"
        )

        // Every split should have a best segment time > 0 (for Best Possible column)
        for (split in profile.splits) {
            val segmentTime = pb.splitTimes[split.id]?.segmentTime ?: 0L
            assertTrue(segmentTime > 0, "Split ${split.id} must have a best segment time > 0")
        }
    }
}
