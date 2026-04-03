package com.supermetroid.livesplit

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LiveSplitCleanupTest {

    private fun makeSegment(
        name: String,
        pbCumulative: Long?,
        bestSegment: Long?,
        historyEntries: List<LiveSplitHistoryEntry> = emptyList()
    ) = LiveSplitSegment(
        name = name,
        icon = null,
        bestSegmentTime = bestSegment?.let { LiveSplitTimeSpan(it, null) },
        splitTimes = if (pbCumulative != null) {
            listOf(LiveSplitComparisonSplit("Personal Best", pbCumulative, null))
        } else emptyList(),
        segmentHistory = historyEntries
    )

    private fun makeDoc(
        segments: List<LiveSplitSegment>,
        attempts: List<LiveSplitAttempt> = emptyList()
    ) = LiveSplitDocument(
        gameName = "Test",
        categoryName = "Any%",
        attemptCount = attempts.size,
        segments = segments,
        attemptHistory = attempts
    )

    @Test
    fun `detectCorruptions - clean file returns empty`() {
        val doc = makeDoc(listOf(
            makeSegment("A", 100000, 80000),
            makeSegment("B", 250000, 120000),
            makeSegment("C", 400000, 110000)
        ))
        assertEquals(emptyList<Corruption>(), detectCorruptions(doc))
    }

    @Test
    fun `detectCorruptions - non-monotonic cumulative detected`() {
        val doc = makeDoc(listOf(
            makeSegment("A", 100000, 80000),
            makeSegment("B", 250000, 120000),
            makeSegment("C", 5000, 110000),  // corrupt: 5s < 250s
            makeSegment("D", 400000, 90000)
        ))
        val corruptions = detectCorruptions(doc)
        assertEquals(1, corruptions.size)
        assertEquals("C", corruptions[0].segmentName)
        assertEquals(5000L, corruptions[0].cumulativeMs)
        assertEquals(250000L, corruptions[0].previousCumulativeMs)
    }

    @Test
    fun `detectCorruptions - multiple corruptions detected`() {
        val doc = makeDoc(listOf(
            makeSegment("A", 100000, 80000),
            makeSegment("B", 250000, 120000),
            makeSegment("C", 3000, 110000),  // corrupt
            makeSegment("D", 3000, 90000),   // corrupt (same as C, still < B)
            makeSegment("E", 400000, 100000)
        ))
        val corruptions = detectCorruptions(doc)
        assertEquals(2, corruptions.size)
    }

    @Test
    fun `repairPbCumulativeTimes - reconstructs from segment history`() {
        // PB attempt is #2 with 400s total
        val attempts = listOf(
            LiveSplitAttempt(1, null, null, 500000, null),
            LiveSplitAttempt(2, null, null, 400000, null)
        )

        val doc = makeDoc(
            segments = listOf(
                makeSegment("A", 100000, 80000, listOf(
                    LiveSplitHistoryEntry(1, 110000, null),
                    LiveSplitHistoryEntry(2, 95000, null)
                )),
                makeSegment("B", 250000, 120000, listOf(
                    LiveSplitHistoryEntry(1, 130000, null),
                    LiveSplitHistoryEntry(2, 125000, null)
                )),
                makeSegment("C", 5000, 110000, listOf(  // corrupt cumulative
                    LiveSplitHistoryEntry(1, 120000, null),
                    LiveSplitHistoryEntry(2, 115000, null)
                )),
                makeSegment("D", 400000, 90000, listOf(
                    LiveSplitHistoryEntry(1, 100000, null),
                    LiveSplitHistoryEntry(2, 65000, null)
                ))
            ),
            attempts = attempts
        )

        // Before repair: has corruption
        assertEquals(1, detectCorruptions(doc).size)

        val repaired = repairPbCumulativeTimes(doc)
        assertNotNull(repaired)

        // After repair: no corruptions
        assertEquals(0, detectCorruptions(repaired!!).size)

        // Verify reconstructed cumulative times from attempt #2
        val repairedPbTimes = repaired.segments.map {
            it.splitTimes.find { st -> st.comparisonName == "Personal Best" }?.realTime
        }
        assertEquals(95000L, repairedPbTimes[0])   // A: 95s
        assertEquals(220000L, repairedPbTimes[1])   // B: 95 + 125 = 220s
        assertEquals(335000L, repairedPbTimes[2])   // C: 220 + 115 = 335s (FIXED)
        assertEquals(400000L, repairedPbTimes[3])   // D: 335 + 65 = 400s
    }

    @Test
    fun `repairPbCumulativeTimes - returns null with no valid attempts`() {
        val doc = makeDoc(
            segments = listOf(makeSegment("A", 100000, 80000)),
            attempts = listOf(
                LiveSplitAttempt(1, null, null, null, null) // no time
            )
        )
        assertNull(repairPbCumulativeTimes(doc))
    }

    // =============================================
    // PB mismatch detection
    // =============================================

    @Test
    fun `detectPbMismatch - no mismatch when comparison matches fastest`() {
        val doc = makeDoc(
            segments = listOf(
                makeSegment("A", 200000, 80000, listOf(LiveSplitHistoryEntry(1, 95000, null))),
                makeSegment("B", 400000, 120000, listOf(LiveSplitHistoryEntry(1, 110000, null)))
            ),
            attempts = listOf(LiveSplitAttempt(1, null, null, 400000, null))
        )
        assertNull(detectPbMismatch(doc))
    }

    @Test
    fun `detectPbMismatch - detects when comparison is slower than fastest complete attempt`() {
        // PB comparison shows 600s but attempt #1 completed in 360s with full segment history
        val doc = makeDoc(
            segments = listOf(
                makeSegment("A", 300000, 80000, listOf(
                    LiveSplitHistoryEntry(1, 95000, null),
                    LiveSplitHistoryEntry(2, 150000, null)
                )),
                makeSegment("B", 600000, 120000, listOf(
                    LiveSplitHistoryEntry(1, 110000, null),
                    LiveSplitHistoryEntry(2, 150000, null)
                ))
            ),
            attempts = listOf(
                LiveSplitAttempt(1, null, null, 360000, null),  // faster
                LiveSplitAttempt(2, null, null, 600000, null)   // matches comparison
            )
        )
        val mismatch = detectPbMismatch(doc)
        assertNotNull(mismatch)
        assertEquals(600000L, mismatch!!.comparisonTotal)
        assertEquals(360000L, mismatch.fastestAttemptTotal)
        assertEquals(1, mismatch.fastestAttemptId)
    }

    @Test
    fun `detectPbMismatch - ignores fast attempt without complete segment history`() {
        // Attempt #1 is faster (5s) but has NO segment history (quick reset)
        val doc = makeDoc(
            segments = listOf(
                makeSegment("A", 200000, 80000, listOf(LiveSplitHistoryEntry(2, 100000, null))),
                makeSegment("B", 400000, 120000, listOf(LiveSplitHistoryEntry(2, 110000, null)))
            ),
            attempts = listOf(
                LiveSplitAttempt(1, null, null, 5000, null),   // quick reset, no segments
                LiveSplitAttempt(2, null, null, 400000, null)
            )
        )
        assertNull(detectPbMismatch(doc))
    }
}
