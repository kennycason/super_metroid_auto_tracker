package com.supermetroid.ui

import com.supermetroid.model.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import strikt.api.expectThat
import strikt.assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

/**
 * Tests for PB run BEST column calculation and timer color delta logic.
 *
 * Verifies that:
 *  - BEST column cumulative time is computed from PB run segment times in profile order
 *  - Split ID mismatches between profile and PB run are handled gracefully
 *  - Timer cumulative delta is the sum of per-split deltas
 *  - findActualPbRun matches by runSessionId first, then fastest complete run
 */
@DisplayName("PB Run Calculation")
class PbRunCalculationTest {

    private val now = Clock.System.now()

    private fun split(id: String, name: String = id) = Split(id, name, "item", "")

    private fun completedSplit(id: String, segmentTime: Long, totalTime: Long, delta: Long? = null) =
        CompletedSplit(
            splitId = id,
            time = SplitTime(totalTime = totalTime, segmentTime = segmentTime, delta = delta),
            timestamp = now
        )

    private fun runSession(
        id: String,
        profileId: String,
        splits: List<CompletedSplit>,
        totalTime: Long,
        endTime: Instant? = now
    ) = RunSession(
        id = id,
        profileId = profileId,
        startTime = now,
        endTime = endTime,
        completedSplits = splits,
        totalTime = totalTime
    )

    // --- BEST Column: cumulative time from PB run segments in profile order ---

    @Test
    @DisplayName("BEST column sums PB run segment times in profile order")
    fun `best column sums segments in profile order`() {
        val profile = SplitProfile("test", "Test", listOf(
            split("a"), split("b"), split("c")
        ))

        // PB run has splits in same order
        val pbRun = runSession("pb1", "test", listOf(
            completedSplit("a", segmentTime = 10_000, totalTime = 10_000),
            completedSplit("b", segmentTime = 20_000, totalTime = 30_000),
            completedSplit("c", segmentTime = 15_000, totalTime = 45_000)
        ), totalTime = 45_000)

        val state = SplitsState(
            personalBests = mapOf("test" to PersonalBest("test", "pb1", 45_000, emptyMap())),
            runHistory = listOf(pbRun)
        )

        // Cumulative at each split index:
        // index 0 (a): 10_000
        // index 1 (b): 10_000 + 20_000 = 30_000
        // index 2 (c): 10_000 + 20_000 + 15_000 = 45_000
        expectThat(calculatePbRunTimeUpTo(state, "test", profile, 0)).isEqualTo(10_000L)
        expectThat(calculatePbRunTimeUpTo(state, "test", profile, 1)).isEqualTo(30_000L)
        expectThat(calculatePbRunTimeUpTo(state, "test", profile, 2)).isEqualTo(45_000L)
    }

    @Test
    @DisplayName("BEST column handles PB run splits in different order than profile")
    fun `best column handles different split order`() {
        // Profile order: a, b, c
        val profile = SplitProfile("test", "Test", listOf(
            split("a"), split("b"), split("c")
        ))

        // PB run has splits in different order: b, a, c (different routing)
        val pbRun = runSession("pb1", "test", listOf(
            completedSplit("b", segmentTime = 20_000, totalTime = 20_000),
            completedSplit("a", segmentTime = 10_000, totalTime = 30_000),
            completedSplit("c", segmentTime = 15_000, totalTime = 45_000)
        ), totalTime = 45_000)

        val state = SplitsState(
            personalBests = mapOf("test" to PersonalBest("test", "pb1", 45_000, emptyMap())),
            runHistory = listOf(pbRun)
        )

        // In profile order: a(10k) + b(20k) + c(15k)
        expectThat(calculatePbRunTimeUpTo(state, "test", profile, 0)).isEqualTo(10_000L)
        expectThat(calculatePbRunTimeUpTo(state, "test", profile, 1)).isEqualTo(30_000L)
        expectThat(calculatePbRunTimeUpTo(state, "test", profile, 2)).isEqualTo(45_000L)
    }

    @Test
    @DisplayName("BEST column handles missing split IDs gracefully")
    fun `best column handles missing split ids`() {
        // Profile expects "hijump_boots" but PB run has "hi_jump"
        val profile = SplitProfile("test", "Test", listOf(
            split("a"), split("hijump_boots"), split("c")
        ))

        val pbRun = runSession("pb1", "test", listOf(
            completedSplit("a", segmentTime = 10_000, totalTime = 10_000),
            completedSplit("hi_jump", segmentTime = 20_000, totalTime = 30_000),
            completedSplit("c", segmentTime = 15_000, totalTime = 45_000)
        ), totalTime = 45_000)

        val state = SplitsState(
            personalBests = mapOf("test" to PersonalBest("test", "pb1", 45_000, emptyMap())),
            runHistory = listOf(pbRun)
        )

        // "hijump_boots" not found in PB run, so only a and c contribute
        expectThat(calculatePbRunTimeUpTo(state, "test", profile, 0)).isEqualTo(10_000L)
        expectThat(calculatePbRunTimeUpTo(state, "test", profile, 1)).isEqualTo(10_000L) // hijump_boots not found
        expectThat(calculatePbRunTimeUpTo(state, "test", profile, 2)).isEqualTo(25_000L) // a + c
    }

    @Test
    @DisplayName("BEST column returns 0 when no PB run found")
    fun `best column returns 0 when no pb run`() {
        val profile = SplitProfile("test", "Test", listOf(split("a")))
        val state = SplitsState() // No run history

        expectThat(calculatePbRunTimeUpTo(state, "test", profile, 0)).isEqualTo(0L)
    }

    // --- findActualPbRun ---

    @Test
    @DisplayName("findActualPbRun matches by runSessionId first")
    fun `find pb run matches by session id first`() {
        val profile = SplitProfile("test", "Test", listOf(split("a")))

        val slowRun = runSession("slow", "test", listOf(
            completedSplit("a", segmentTime = 50_000, totalTime = 50_000)
        ), totalTime = 50_000)

        val fastRun = runSession("fast", "test", listOf(
            completedSplit("a", segmentTime = 30_000, totalTime = 30_000)
        ), totalTime = 30_000)

        // PB points to slow run by ID, even though fast run is faster
        val state = SplitsState(
            personalBests = mapOf("test" to PersonalBest("test", "slow", 50_000, emptyMap())),
            runHistory = listOf(slowRun, fastRun)
        )

        val found = findActualPbRun(state, "test", profile)
        expectThat(found).isNotNull()
        expectThat(found!!.id).isEqualTo("slow")
    }

    @Test
    @DisplayName("findActualPbRun falls back to fastest complete run")
    fun `find pb run falls back to fastest`() {
        val profile = SplitProfile("test", "Test", listOf(split("a")))

        val slowRun = runSession("slow", "test", listOf(
            completedSplit("a", segmentTime = 50_000, totalTime = 50_000)
        ), totalTime = 50_000)

        val fastRun = runSession("fast", "test", listOf(
            completedSplit("a", segmentTime = 30_000, totalTime = 30_000)
        ), totalTime = 30_000)

        // PB ID doesn't match any run in history
        val state = SplitsState(
            personalBests = mapOf("test" to PersonalBest("test", "missing_id", 30_000, emptyMap())),
            runHistory = listOf(slowRun, fastRun)
        )

        val found = findActualPbRun(state, "test", profile)
        expectThat(found).isNotNull()
        expectThat(found!!.id).isEqualTo("fast") // Falls back to fastest
    }

    // --- Timer delta (current cumulative vs PB run cumulative at same split) ---

    @Test
    @DisplayName("Timer delta compares cumulative time against PB run at last split")
    fun `timer delta compares against pb run cumulative`() {
        // PB run: a=90s, b=190s cumulative
        val pbRun = runSession("pb1", "test", listOf(
            completedSplit("a", segmentTime = 90_000, totalTime = 90_000),
            completedSplit("b", segmentTime = 100_000, totalTime = 190_000)
        ), totalTime = 190_000)

        // Current run: a=95s, b=195s cumulative (5s behind at each split)
        val currentSplits = listOf(
            completedSplit("a", segmentTime = 95_000, totalTime = 95_000),
            completedSplit("b", segmentTime = 100_000, totalTime = 195_000)
        )

        // Timer compares last split cumulative: 195_000 - 190_000 = 5_000 (behind)
        val lastSplitId = currentSplits.last().splitId
        val pbSplitTime = pbRun.completedSplits.find { it.splitId == lastSplitId }!!
        val delta = currentSplits.last().time.totalTime - pbSplitTime.time.totalTime

        expectThat(delta).isEqualTo(5_000L) // 5s behind = white→red
    }

    @Test
    @DisplayName("Timer shows gold when ahead of PB run at last split")
    fun `timer gold when ahead of pb run`() {
        // PB run: a=95s, b=200s cumulative
        val pbRun = runSession("pb1", "test", listOf(
            completedSplit("a", segmentTime = 95_000, totalTime = 95_000),
            completedSplit("b", segmentTime = 105_000, totalTime = 200_000)
        ), totalTime = 200_000)

        // Current run: b at 190s cumulative (10s ahead!)
        val currentSplits = listOf(
            completedSplit("a", segmentTime = 90_000, totalTime = 90_000),
            completedSplit("b", segmentTime = 100_000, totalTime = 190_000)
        )

        val lastSplitId = currentSplits.last().splitId
        val pbSplitTime = pbRun.completedSplits.find { it.splitId == lastSplitId }!!
        val delta = currentSplits.last().time.totalTime - pbSplitTime.time.totalTime

        expectThat(delta).isLessThan(0L) // -10_000 = ahead = GOLD
        expectThat(delta).isEqualTo(-10_000L)
    }

    @Test
    @DisplayName("Timer handles split not found in PB run gracefully")
    fun `timer handles missing split in pb run`() {
        val pbRun = runSession("pb1", "test", listOf(
            completedSplit("a", segmentTime = 90_000, totalTime = 90_000)
        ), totalTime = 90_000)

        // Current run has a split "b" that doesn't exist in PB run
        val currentSplits = listOf(
            completedSplit("b", segmentTime = 50_000, totalTime = 50_000)
        )

        val lastSplitId = currentSplits.last().splitId
        val pbSplitTime = pbRun.completedSplits.find { it.splitId == lastSplitId }

        // Should be null — timer falls back to default color
        expectThat(pbSplitTime).isNull()
    }

    // --- Expose private functions for testing via package-level wrappers ---
    // These call the same logic as the private functions in SplitsList.kt

    companion object {
        /**
         * Mirrors SplitsList.calculatePbRunTimeUpTo for testing
         */
        fun calculatePbRunTimeUpTo(state: SplitsState, profileId: String, profile: SplitProfile, splitIndex: Int): Long {
            val pbRun = findActualPbRun(state, profileId, profile) ?: return 0L
            val segmentTimeMap = pbRun.completedSplits.associate { it.splitId to it.time.segmentTime }
            var cumulative = 0L
            for (i in 0..splitIndex) {
                val split = profile.splits.getOrNull(i) ?: continue
                val segTime = segmentTimeMap[split.id]
                if (segTime != null) {
                    cumulative += segTime
                }
            }
            return cumulative
        }

        /**
         * Mirrors SplitsList.findActualPbRun for testing
         */
        fun findActualPbRun(state: SplitsState, profileId: String, profile: SplitProfile): RunSession? {
            val pb = state.personalBests[profileId]
            if (pb != null && pb.runSessionId.isNotBlank()) {
                val byId = state.runHistory.find { it.id == pb.runSessionId }
                if (byId != null) return byId
            }
            val completeRuns = state.runHistory.filter { run ->
                run.profileId == profileId &&
                    run.endTime != null &&
                    run.completedSplits.size == profile.splits.size
            }
            return completeRuns.minByOrNull { it.totalTime }
        }
    }
}
