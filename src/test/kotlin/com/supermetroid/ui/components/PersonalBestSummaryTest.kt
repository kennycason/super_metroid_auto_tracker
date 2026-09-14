package com.supermetroid.ui.components

import com.supermetroid.model.CompletedSplit
import com.supermetroid.model.PersonalBest
import com.supermetroid.model.RunSession
import com.supermetroid.model.Split
import com.supermetroid.model.SplitProfile
import com.supermetroid.model.SplitTime
import com.supermetroid.model.SplitsState
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PersonalBestSummaryTest {
    private val profile = SplitProfile(
        id = "rbo",
        name = "RBO",
        splits = listOf(
            Split("ceres", "Ceres", "event"),
            Split("ridley", "Ridley", "boss")
        )
    )

    @Test
    fun `current run segment does not enable footer without completed history`() {
        val ceres = completedSplit("ceres", 92_200L)
        val state = SplitsState(
            currentRun = run(
                id = "current",
                endTime = null,
                completedSplits = listOf(ceres),
                totalTime = 92_200L
            ),
            // Reproduce the live engine state after the first split: it has a
            // PB-shaped value even though no run has ever been completed.
            personalBests = mapOf(
                profile.id to PersonalBest(
                    profileId = profile.id,
                    runSessionId = "current",
                    totalTime = 0L,
                    splitTimes = mapOf("ceres" to ceres.time)
                )
            )
        )

        assertFalse(hasCompletedRunForSummary(state, profile.id, profile))
    }

    @Test
    fun `incomplete and partial completed runs do not enable footer`() {
        val fullButIncomplete = run(
            id = "dnf",
            endTime = null,
            completedSplits = listOf(
                completedSplit("ceres", 92_200L),
                completedSplit("ridley", 3_000_000L)
            ),
            totalTime = 3_092_200L
        )
        val endedButMissingSplit = run(
            id = "corrupt",
            endTime = Instant.fromEpochMilliseconds(4_000_000L),
            completedSplits = listOf(completedSplit("ceres", 92_200L)),
            totalTime = 92_200L
        )

        assertFalse(
            hasCompletedRunForSummary(
                SplitsState(runHistory = listOf(fullButIncomplete, endedButMissingSplit)),
                profile.id,
                profile
            )
        )
    }

    @Test
    fun `full completed historical run enables footer`() {
        val completedRun = run(
            id = "complete",
            endTime = Instant.fromEpochMilliseconds(4_000_000L),
            completedSplits = listOf(
                completedSplit("ceres", 92_200L),
                completedSplit("ridley", 3_000_000L)
            ),
            totalTime = 3_092_200L
        )

        assertTrue(
            hasCompletedRunForSummary(
                SplitsState(runHistory = listOf(completedRun)),
                profile.id,
                profile
            )
        )
    }

    @Test
    fun `completed run from another profile does not enable footer`() {
        val otherProfileRun = run(
            id = "other",
            profileId = "other-profile",
            endTime = Instant.fromEpochMilliseconds(4_000_000L),
            completedSplits = listOf(
                completedSplit("ceres", 92_200L),
                completedSplit("ridley", 3_000_000L)
            ),
            totalTime = 3_092_200L
        )

        assertFalse(
            hasCompletedRunForSummary(
                SplitsState(runHistory = listOf(otherProfileRun)),
                profile.id,
                profile
            )
        )
    }

    private fun completedSplit(id: String, segmentTime: Long): CompletedSplit {
        return CompletedSplit(
            splitId = id,
            time = SplitTime(totalTime = segmentTime, segmentTime = segmentTime),
            timestamp = Instant.fromEpochMilliseconds(segmentTime)
        )
    }

    private fun run(
        id: String,
        profileId: String = profile.id,
        endTime: Instant?,
        completedSplits: List<CompletedSplit>,
        totalTime: Long
    ): RunSession {
        return RunSession(
            id = id,
            profileId = profileId,
            startTime = Instant.fromEpochMilliseconds(0L),
            endTime = endTime,
            completedSplits = completedSplits,
            totalTime = totalTime
        )
    }
}
