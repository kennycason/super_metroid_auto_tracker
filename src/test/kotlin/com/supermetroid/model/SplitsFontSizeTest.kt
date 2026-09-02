package com.supermetroid.model

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo

class SplitsFontSizeTest {

    @Test
    fun `very large adds a larger tier without changing existing sizes`() {
        expectThat(SplitsFontSize.values().toList()).containsExactly(
            SplitsFontSize.SMALL,
            SplitsFontSize.MEDIUM,
            SplitsFontSize.LARGE,
            SplitsFontSize.VERY_LARGE
        )

        expectThat(SplitsFontSize.SMALL) {
            get { listOf(header, text, time, detail, summary) }
                .isEqualTo(listOf(9, 10, 9, 8, 13))
        }
        expectThat(SplitsFontSize.MEDIUM) {
            get { listOf(header, text, time, detail, summary) }
                .isEqualTo(listOf(10, 12, 10, 9, 15))
        }
        expectThat(SplitsFontSize.LARGE) {
            get { listOf(header, text, time, detail, summary) }
                .isEqualTo(listOf(11, 14, 12, 10, 18))
        }
        expectThat(SplitsFontSize.VERY_LARGE) {
            get { displayName }.isEqualTo("Very Large")
            get { listOf(header, text, time, detail, summary) }
                .isEqualTo(listOf(13, 18, 15, 12, 22))
        }
    }

    @Test
    fun `very large restores from its persisted display name`() {
        expectThat(SplitsFontSize.fromDisplayName("Very Large"))
            .isEqualTo(SplitsFontSize.VERY_LARGE)
    }

    @Test
    fun `time and delta columns grow with the larger font tiers`() {
        expectThat(SplitsFontSize.values().map { it.timeColumnWidth })
            .containsExactly(85, 85, 96, 116)
        expectThat(SplitsFontSize.values().map { it.deltaColumnWidth })
            .containsExactly(60, 65, 72, 86)
    }
}
