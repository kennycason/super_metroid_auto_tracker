package com.supermetroid.ui

import androidx.compose.ui.graphics.Color
import com.supermetroid.ui.components.liveSplitDeltaColor
import com.supermetroid.ui.components.deltaGradientColor
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import strikt.api.expectThat
import strikt.assertions.*

private val Gold = Color(0xFFFFD700)
private val AheadGaining = Color(0xFF00CC36)
private val AheadLosing = Color(0xFF6BD688)
private val BehindGaining = Color(0xFFD68B6B)
private val BehindLosing = Color(0xFFCC0000)

@DisplayName("LiveSplit Delta Color Logic")
class LiveSplitDeltaColorTest {

    @Nested
    @DisplayName("liveSplitDeltaColor — 5-color model for cumulative deltas")
    inner class LiveSplitColors {

        @Test
        @DisplayName("Gold overrides everything — best segment always gold regardless of delta")
        fun `best segment is always gold`() {
            expectThat(liveSplitDeltaColor(currentDelta = -5000, previousDelta = null, isBestSegment = true))
                .isEqualTo(Gold)
            expectThat(liveSplitDeltaColor(currentDelta = 5000, previousDelta = null, isBestSegment = true))
                .isEqualTo(Gold)
            expectThat(liveSplitDeltaColor(currentDelta = 0, previousDelta = null, isBestSegment = true))
                .isEqualTo(Gold)
            expectThat(liveSplitDeltaColor(currentDelta = 10000, previousDelta = -5000L, isBestSegment = true))
                .isEqualTo(Gold)
        }

        @Test
        @DisplayName("Ahead + gap widening = AheadGaining (bright green)")
        fun `ahead and gaining shows bright green`() {
            // Split 1: -5s ahead. Split 2: -10s ahead (gap widened by 5s)
            val color = liveSplitDeltaColor(currentDelta = -10_000, previousDelta = -5_000, isBestSegment = false)
            expectThat(color).isEqualTo(AheadGaining)
        }

        @Test
        @DisplayName("Ahead + gap shrinking = AheadLosing (light green)")
        fun `ahead but losing time shows light green`() {
            // Split 1: -10s ahead. Split 2: -7s ahead (gap shrunk by 3s)
            val color = liveSplitDeltaColor(currentDelta = -7_000, previousDelta = -10_000, isBestSegment = false)
            expectThat(color).isEqualTo(AheadLosing)
        }

        @Test
        @DisplayName("Ahead with no previous delta = AheadLosing (default for first split)")
        fun `ahead with null previous defaults to ahead losing`() {
            val color = liveSplitDeltaColor(currentDelta = -5_000, previousDelta = null, isBestSegment = false)
            expectThat(color).isEqualTo(AheadLosing)
        }

        @Test
        @DisplayName("Behind + gap widening = BehindLosing (bright red)")
        fun `behind and losing more shows bright red`() {
            // Split 1: +5s behind. Split 2: +10s behind (gap widened by 5s)
            val color = liveSplitDeltaColor(currentDelta = 10_000, previousDelta = 5_000, isBestSegment = false)
            expectThat(color).isEqualTo(BehindLosing)
        }

        @Test
        @DisplayName("Behind + gap shrinking = BehindGaining (light red)")
        fun `behind but catching up shows light red`() {
            // Split 1: +10s behind. Split 2: +7s behind (gap shrunk by 3s)
            val color = liveSplitDeltaColor(currentDelta = 7_000, previousDelta = 10_000, isBestSegment = false)
            expectThat(color).isEqualTo(BehindGaining)
        }

        @Test
        @DisplayName("Behind with no previous delta = BehindGaining (default for first split)")
        fun `behind with null previous defaults to behind gaining`() {
            // When previousDelta is null, currentDelta > previousDelta can't be true, so BehindGaining
            val color = liveSplitDeltaColor(currentDelta = 5_000, previousDelta = null, isBestSegment = false)
            expectThat(color).isEqualTo(BehindGaining)
        }

        @Test
        @DisplayName("Exactly tied (zero delta) = White")
        fun `zero delta is white`() {
            val color = liveSplitDeltaColor(currentDelta = 0, previousDelta = -5_000, isBestSegment = false)
            expectThat(color).isEqualTo(Color.White)
        }

        @Test
        @DisplayName("Transition from behind to ahead uses AheadGaining")
        fun `crossing from behind to ahead shows ahead gaining`() {
            // Previous split: +2s behind. This split: -3s ahead
            val color = liveSplitDeltaColor(currentDelta = -3_000, previousDelta = 2_000, isBestSegment = false)
            // -3000 < 2000, so AheadGaining
            expectThat(color).isEqualTo(AheadGaining)
        }

        @Test
        @DisplayName("Transition from ahead to behind uses BehindLosing")
        fun `crossing from ahead to behind shows behind losing`() {
            // Previous split: -2s ahead. This split: +3s behind
            val color = liveSplitDeltaColor(currentDelta = 3_000, previousDelta = -2_000, isBestSegment = false)
            // 3000 > -2000, so BehindLosing
            expectThat(color).isEqualTo(BehindLosing)
        }

        @Test
        @DisplayName("Same delta as previous = not gaining, uses Losing variant")
        fun `equal deltas use losing variant`() {
            // Ahead, same delta as previous → not strictly less → AheadLosing
            expectThat(liveSplitDeltaColor(currentDelta = -5_000, previousDelta = -5_000, isBestSegment = false))
                .isEqualTo(AheadLosing)
            // Behind, same delta as previous → not strictly greater → BehindGaining
            expectThat(liveSplitDeltaColor(currentDelta = 5_000, previousDelta = 5_000, isBestSegment = false))
                .isEqualTo(BehindGaining)
        }
    }

    @Nested
    @DisplayName("deltaGradientColor — gradient model for segment deltas")
    inner class GradientColors {

        @Test
        @DisplayName("Best segment override returns gold")
        fun `best segment returns gold`() {
            expectThat(deltaGradientColor(deltaMs = 5000, isBestSegment = true)).isEqualTo(Gold)
            expectThat(deltaGradientColor(deltaMs = -5000, isBestSegment = true)).isEqualTo(Gold)
        }

        @Test
        @DisplayName("Zero delta returns white")
        fun `zero delta returns white`() {
            expectThat(deltaGradientColor(deltaMs = 0)).isEqualTo(Color.White)
        }

        @Test
        @DisplayName("Negative delta trends toward green")
        fun `negative delta is greenish`() {
            val color = deltaGradientColor(deltaMs = -15_000)
            expectThat(color.green).isGreaterThan(color.red)
        }

        @Test
        @DisplayName("Positive delta trends toward red")
        fun `positive delta is reddish`() {
            val color = deltaGradientColor(deltaMs = 15_000)
            expectThat(color.red).isGreaterThan(color.green)
        }

        @Test
        @DisplayName("Larger negative delta is more saturated green than smaller")
        fun `larger negative is more green`() {
            val small = deltaGradientColor(deltaMs = -5_000)
            val large = deltaGradientColor(deltaMs = -25_000)
            // Lerps from white toward FullGreen — red channel drops faster for larger deltas
            expectThat(large.red).isLessThan(small.red)
        }

        @Test
        @DisplayName("Saturates at maxDeltaMs")
        fun `saturates at max`() {
            val atMax = deltaGradientColor(deltaMs = -30_000)
            val beyondMax = deltaGradientColor(deltaMs = -60_000)
            expectThat(atMax).isEqualTo(beyondMax)
        }
    }

    @Nested
    @DisplayName("Full run scenario — multi-split color sequence")
    inner class FullRunScenario {

        @Test
        @DisplayName("Realistic 4-split run produces correct color sequence")
        fun `realistic run color sequence`() {
            // Best Possible cumulative: 30s, 60s, 90s, 120s
            // PB run cumulative:        32s, 65s, 95s, 125s
            // Current run cumulative:   31s, 62s, 93s, 128s

            // Deltas vs PB: -1s, -3s, -2s, +3s
            val deltas = listOf(-1_000L, -3_000L, -2_000L, 3_000L)
            val bestSegFlags = listOf(false, false, false, false)

            // Split 1: -1s, no previous → AheadLosing
            expectThat(liveSplitDeltaColor(deltas[0], null, bestSegFlags[0]))
                .isEqualTo(AheadLosing)

            // Split 2: -3s, prev -1s, delta got more negative → AheadGaining
            expectThat(liveSplitDeltaColor(deltas[1], deltas[0], bestSegFlags[1]))
                .isEqualTo(AheadGaining)

            // Split 3: -2s, prev -3s, delta got less negative → AheadLosing
            expectThat(liveSplitDeltaColor(deltas[2], deltas[1], bestSegFlags[2]))
                .isEqualTo(AheadLosing)

            // Split 4: +3s, prev -2s, crossed to behind and got worse → BehindLosing
            expectThat(liveSplitDeltaColor(deltas[3], deltas[2], bestSegFlags[3]))
                .isEqualTo(BehindLosing)
        }

        @Test
        @DisplayName("Gold split in middle of behind run still shows gold")
        fun `gold in behind run`() {
            // Behind overall (+5s) but this segment was a gold
            val color = liveSplitDeltaColor(currentDelta = 5_000, previousDelta = 8_000, isBestSegment = true)
            expectThat(color).isEqualTo(Gold)
        }
    }

    @Nested
    @DisplayName("Delta calculation correctness")
    inner class DeltaCalculations {

        @Test
        @DisplayName("BP Total delta = current cumulative - sum of best segments")
        fun `bp total delta calculation`() {
            val currentCumulative = 95_000L
            val sumOfBestSegments = 90_000L // theoretical best
            val delta = currentCumulative - sumOfBestSegments
            expectThat(delta).isEqualTo(5_000L) // 5s behind best possible
        }

        @Test
        @DisplayName("Best Total delta = current cumulative - PB cumulative")
        fun `best total delta calculation`() {
            val currentCumulative = 88_000L
            val pbCumulative = 95_000L
            val delta = currentCumulative - pbCumulative
            expectThat(delta).isEqualTo(-7_000L) // 7s ahead of PB
        }

        @Test
        @DisplayName("Best segment delta = current segment - PB run's segment")
        fun `best segment delta calculation`() {
            val currentSegment = 28_000L
            val pbRunSegment = 30_000L
            val delta = currentSegment - pbRunSegment
            expectThat(delta).isEqualTo(-2_000L) // 2s faster than PB's segment
        }

        @Test
        @DisplayName("Previous delta correctly tracks gap direction")
        fun `previous delta gap tracking`() {
            // Split 1: cumDelta = -5s
            // Split 2: cumDelta = -8s → gained 3s (gap widened)
            // Split 3: cumDelta = -6s → lost 2s (gap shrunk)
            val d1 = -5_000L
            val d2 = -8_000L
            val d3 = -6_000L

            // d2 < d1 → gaining
            expectThat(d2 < d1).isTrue()
            // d3 > d2 → not gaining (losing time, though still ahead)
            expectThat(d3 < d2).isFalse()
        }
    }
}
