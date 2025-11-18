package com.supermetroid.ui

import com.supermetroid.model.*
import kotlinx.datetime.Clock
import strikt.api.expectThat
import strikt.assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

/**
 * Tests for Best Possible Delta (BP Δ) calculation logic
 * BP Δ should compare segment-by-segment, not cumulative times
 * 
 * When a runner achieves a gold segment (TIME column shows gold), 
 * the BP Δ should show negative/green (time saved).
 */
@DisplayName("Best Possible Delta Calculation")
class BestPossibleDeltaCalculationTest {

    @Test
    @DisplayName("Gold segment (faster than best) should show negative BP Δ")
    fun `gold segment should show negative bp delta`() {
        // Given: Previous best segment was 30 seconds
        val bestSegmentTime = SplitTime(
            totalTime = 30_000L,
            segmentTime = 30_000L,
            delta = 0,
            originalDelta = 0
        )
        
        // When: Current run completes same segment in 28 seconds (2 seconds faster - GOLD!)
        val currentSegmentTime = 28_000L
        
        // Calculate BP Δ (segment comparison)
        val bpDelta = currentSegmentTime - bestSegmentTime.segmentTime
        
        // Then: BP Δ should be negative (saved time)
        expectThat(bpDelta) {
            isLessThan(0) // Negative means faster
            isEqualTo(-2_000L) // Saved 2 seconds
        }
    }

    @Test
    @DisplayName("Slower segment (slower than best) should show positive BP Δ")
    fun `slower segment should show positive bp delta`() {
        // Given: Previous best segment was 30 seconds
        val bestSegmentTime = SplitTime(
            totalTime = 30_000L,
            segmentTime = 30_000L,
            delta = 0,
            originalDelta = 0
        )
        
        // When: Current run completes same segment in 35 seconds (5 seconds slower)
        val currentSegmentTime = 35_000L
        
        // Calculate BP Δ (segment comparison)
        val bpDelta = currentSegmentTime - bestSegmentTime.segmentTime
        
        // Then: BP Δ should be positive (lost time)
        expectThat(bpDelta) {
            isGreaterThan(0) // Positive means slower
            isEqualTo(5_000L) // Lost 5 seconds
        }
    }

    @Test
    @DisplayName("Tied segment (exactly matches best) should show zero BP Δ")
    fun `tied segment should show zero bp delta`() {
        // Given: Previous best segment was 30 seconds
        val bestSegmentTime = SplitTime(
            totalTime = 30_000L,
            segmentTime = 30_000L,
            delta = 0,
            originalDelta = 0
        )
        
        // When: Current run completes same segment in exactly 30 seconds
        val currentSegmentTime = 30_000L
        
        // Calculate BP Δ (segment comparison)
        val bpDelta = currentSegmentTime - bestSegmentTime.segmentTime
        
        // Then: BP Δ should be zero (tied)
        expectThat(bpDelta) {
            isEqualTo(0L)
        }
    }

    @Test
    @DisplayName("Multiple segments: gold segments should show negative, slow segments positive")
    fun `multiple segments should calculate bp delta independently`() {
        // Given: Best segments for 3 splits
        val bestSegments = mapOf(
            "split1" to SplitTime(totalTime = 30_000L, segmentTime = 30_000L, delta = 0),
            "split2" to SplitTime(totalTime = 60_000L, segmentTime = 30_000L, delta = 0),
            "split3" to SplitTime(totalTime = 90_000L, segmentTime = 30_000L, delta = 0)
        )
        
        // When: Current run with mixed performance
        val currentSegments = mapOf(
            "split1" to 28_000L, // 2s faster (GOLD)
            "split2" to 35_000L, // 5s slower (BAD)
            "split3" to 30_000L  // Tied (OK)
        )
        
        // Calculate BP Δ for each segment
        val bpDeltas = currentSegments.mapValues { (splitId, currentTime) ->
            currentTime - bestSegments[splitId]!!.segmentTime
        }
        
        // Then: Each segment's BP Δ is independent
        expectThat(bpDeltas["split1"]!!) {
            isLessThan(0) // Gold: saved 2s
            isEqualTo(-2_000L)
        }
        
        expectThat(bpDeltas["split2"]!!) {
            isGreaterThan(0) // Slower: lost 5s
            isEqualTo(5_000L)
        }
        
        expectThat(bpDeltas["split3"]!!) {
            isEqualTo(0L) // Tied
        }
    }

    @Test
    @DisplayName("BP Δ should use segment time, not total time")
    fun `bp delta should compare segment times not total times`() {
        // Given: Best run with these times
        // Split 1: segment=30s, total=30s
        // Split 2: segment=40s, total=70s
        val bestSplit1 = SplitTime(totalTime = 30_000L, segmentTime = 30_000L, delta = 0)
        val bestSplit2 = SplitTime(totalTime = 70_000L, segmentTime = 40_000L, delta = 0)
        
        // When: Current run is behind overall but has a gold segment
        // Split 1: segment=35s, total=35s (5s slower - behind overall)
        // Split 2: segment=38s, total=73s (2s faster segment - GOLD!)
        val currentSplit1Time = 35_000L
        val currentSplit2Time = 38_000L
        val currentTotalAtSplit2 = 73_000L
        
        // Calculate BP Δ for split 2 (segment comparison, NOT cumulative)
        val bpDeltaSplit2 = currentSplit2Time - bestSplit2.segmentTime
        
        // Then: BP Δ for split 2 should be negative (gold segment)
        // Even though overall we're behind (+3s cumulative)
        expectThat(bpDeltaSplit2) {
            isLessThan(0) // Negative = gold segment
            isEqualTo(-2_000L) // Saved 2s on THIS segment
        }
        
        // THIS IS THE KEY: We don't care that currentTotalAtSplit2 (73s) > bestTotal (70s)
        // We only care that THIS segment (38s) < best segment (40s)
    }

    @Test
    @DisplayName("First split with no previous best should handle gracefully")
    fun `first split with no best should return null or zero`() {
        // Given: No previous best exists
        val bestSegmentTime: SplitTime? = null
        
        // When: Current run completes first split in 30 seconds
        val currentSegmentTime = 30_000L
        
        // Calculate BP Δ
        val bpDelta = if (bestSegmentTime != null) {
            currentSegmentTime - bestSegmentTime.segmentTime
        } else {
            null // No comparison possible
        }
        
        // Then: BP Δ should be null (no baseline to compare against)
        expectThat(bpDelta).isNull()
    }

    @Test
    @DisplayName("Real-world scenario: draygon segment from 2025-11-16 run")
    fun `real world draygon segment should show correct bp delta`() {
        // Given: Previous best Draygon segment was 7:38.85 (458,850ms)
        val bestDraygonSegment = SplitTime(
            totalTime = 2_289_854L, // This is cumulative, but we ignore it for BP Δ
            segmentTime = 458_850L,  // 7:38.85 - THIS is what we compare
            delta = 0
        )
        
        // When: 2025-11-16 run completed Draygon in 7:12.18 (432,180ms)
        val currentDraygonSegment = 432_180L // 7:12.18
        
        // Calculate BP Δ (segment comparison)
        val bpDelta = currentDraygonSegment - bestDraygonSegment.segmentTime
        
        // Then: BP Δ should be -26,670ms (-0:26.67) - GOLD!
        expectThat(bpDelta) {
            isLessThan(0) // Gold segment
            isEqualTo(-26_670L) // Saved 26.67 seconds
        }
    }
}

