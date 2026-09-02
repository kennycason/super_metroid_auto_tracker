package com.supermetroid.livesplit

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo

class LiveSplitStableSplitIdsTest {

    @Test
    fun `profile-owned ids survive arbitrary display name overrides`() {
        val attemptId = 1
        val document = LiveSplitDocument(
            gameName = "Custom Route",
            categoryName = "",
            attemptCount = 1,
            segments = listOf(
                segment("Rolled Up Into A Ball", attemptId, 1_000L, 1_000L),
                segment("Back At The Spaceship", attemptId, 2_000L, 3_000L)
            ),
            attemptHistory = listOf(
                LiveSplitAttempt(
                    id = attemptId,
                    started = null,
                    ended = null,
                    realTime = 3_000L,
                    gameTime = null
                )
            )
        )
        val stableIds = listOf("morph_ball", "ship")
        val converter = LiveSplitConverter()

        val pb = converter.toPersonalBest(document, "custom-route-v1", stableIds)
        val runs = converter.toRunHistory(document, "custom-route-v1", stableIds)

        expectThat(pb.splitTimes.keys.toList()).containsExactly("morph_ball", "ship")
        expectThat(runs.single().completedSplits.map { it.splitId })
            .containsExactly("morph_ball", "ship")
        expectThat(runs.single().totalTime).isEqualTo(3_000L)
    }

    private fun segment(
        name: String,
        attemptId: Int,
        segmentTime: Long,
        cumulativeTime: Long
    ) = LiveSplitSegment(
        name = name,
        icon = null,
        bestSegmentTime = LiveSplitTimeSpan(segmentTime, null),
        splitTimes = listOf(
            LiveSplitComparisonSplit("Personal Best", cumulativeTime, null)
        ),
        segmentHistory = listOf(
            LiveSplitHistoryEntry(attemptId, segmentTime, null)
        )
    )
}
