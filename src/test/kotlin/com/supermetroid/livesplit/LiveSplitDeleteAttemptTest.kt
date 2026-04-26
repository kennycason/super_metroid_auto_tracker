package com.supermetroid.livesplit

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File

class LiveSplitDeleteAttemptTest {

    private val parser = LiveSplitParser()
    private val writer = LiveSplitWriter()

    private fun makeSegment(
        name: String,
        historyEntries: List<LiveSplitHistoryEntry> = emptyList()
    ) = LiveSplitSegment(
        name = name,
        icon = null,
        bestSegmentTime = LiveSplitTimeSpan(80000, null),
        splitTimes = listOf(LiveSplitComparisonSplit("Personal Best", 100000, null)),
        segmentHistory = historyEntries
    )

    private fun makeDoc(
        segments: List<LiveSplitSegment>,
        attempts: List<LiveSplitAttempt>
    ) = LiveSplitDocument(
        gameName = "Test Game",
        categoryName = "Any%",
        attemptCount = attempts.size,
        segments = segments,
        attemptHistory = attempts
    )

    @Test
    fun `removing attempt filters it from attemptHistory`() {
        val doc = makeDoc(
            segments = listOf(makeSegment("A")),
            attempts = listOf(
                LiveSplitAttempt(1, null, null, 100000, null),
                LiveSplitAttempt(2, null, null, 200000, null),
                LiveSplitAttempt(3, null, null, 300000, null)
            )
        )

        val updated = doc.copy(
            attemptCount = doc.attemptHistory.size - 1,
            attemptHistory = doc.attemptHistory.filter { it.id != 2 }
        )

        assertEquals(2, updated.attemptHistory.size)
        assertEquals(2, updated.attemptCount)
        assertNull(updated.attemptHistory.find { it.id == 2 })
        assertNotNull(updated.attemptHistory.find { it.id == 1 })
        assertNotNull(updated.attemptHistory.find { it.id == 3 })
    }

    @Test
    fun `removing attempt also removes matching segment history entries`() {
        val doc = makeDoc(
            segments = listOf(
                makeSegment("A", listOf(
                    LiveSplitHistoryEntry(1, 50000, null),
                    LiveSplitHistoryEntry(2, 60000, null),
                    LiveSplitHistoryEntry(3, 70000, null)
                )),
                makeSegment("B", listOf(
                    LiveSplitHistoryEntry(1, 80000, null),
                    LiveSplitHistoryEntry(2, 90000, null)
                ))
            ),
            attempts = listOf(
                LiveSplitAttempt(1, null, null, 100000, null),
                LiveSplitAttempt(2, null, null, 200000, null),
                LiveSplitAttempt(3, null, null, 300000, null)
            )
        )

        val attemptIdToDelete = 2
        val updated = doc.copy(
            attemptCount = doc.attemptHistory.size - 1,
            attemptHistory = doc.attemptHistory.filter { it.id != attemptIdToDelete },
            segments = doc.segments.map { segment ->
                segment.copy(segmentHistory = segment.segmentHistory.filter { it.id != attemptIdToDelete })
            }
        )

        assertEquals(2, updated.attemptHistory.size)
        assertEquals(2, updated.segments[0].segmentHistory.size)
        assertEquals(1, updated.segments[1].segmentHistory.size)
        // Verify attempt 2's entries are gone
        assertTrue(updated.segments[0].segmentHistory.none { it.id == 2 })
        assertTrue(updated.segments[1].segmentHistory.none { it.id == 2 })
    }

    @Test
    fun `removing nonexistent attempt changes nothing`() {
        val doc = makeDoc(
            segments = listOf(makeSegment("A")),
            attempts = listOf(
                LiveSplitAttempt(1, null, null, 100000, null)
            )
        )

        val updated = doc.copy(
            attemptHistory = doc.attemptHistory.filter { it.id != 999 }
        )

        assertEquals(doc.attemptHistory.size, updated.attemptHistory.size)
    }

    @Test
    fun `removing all attempts results in empty history`() {
        val attempts = listOf(
            LiveSplitAttempt(1, null, null, 100000, null),
            LiveSplitAttempt(2, null, null, 200000, null)
        )
        val doc = makeDoc(
            segments = listOf(
                makeSegment("A", listOf(
                    LiveSplitHistoryEntry(1, 50000, null),
                    LiveSplitHistoryEntry(2, 60000, null)
                ))
            ),
            attempts = attempts
        )

        var updated = doc
        for (attempt in attempts) {
            updated = updated.copy(
                attemptCount = updated.attemptHistory.size - 1,
                attemptHistory = updated.attemptHistory.filter { it.id != attempt.id },
                segments = updated.segments.map { segment ->
                    segment.copy(segmentHistory = segment.segmentHistory.filter { it.id != attempt.id })
                }
            )
        }

        assertEquals(0, updated.attemptHistory.size)
        assertEquals(0, updated.attemptCount)
        assertEquals(0, updated.segments[0].segmentHistory.size)
    }

    @Test
    fun `deleted attempt survives write and re-parse round trip`() {
        val doc = makeDoc(
            segments = listOf(
                makeSegment("Morph Ball", listOf(
                    LiveSplitHistoryEntry(1, 50000, null),
                    LiveSplitHistoryEntry(2, 60000, null),
                    LiveSplitHistoryEntry(3, 70000, null)
                ))
            ),
            attempts = listOf(
                LiveSplitAttempt(1, "04/01/2026 10:00:00", "04/01/2026 10:05:00", 100000, null),
                LiveSplitAttempt(2, "04/02/2026 10:00:00", "04/02/2026 10:10:00", 200000, null),
                LiveSplitAttempt(3, "04/03/2026 10:00:00", null, null, null)
            )
        )

        // Delete attempt 2
        val updated = doc.copy(
            attemptCount = 2,
            attemptHistory = doc.attemptHistory.filter { it.id != 2 },
            segments = doc.segments.map { segment ->
                segment.copy(segmentHistory = segment.segmentHistory.filter { it.id != 2 })
            }
        )

        // Write to temp file and re-parse
        val tempFile = File.createTempFile("lss-delete-test", ".lss")
        try {
            writer.writeToFile(updated, tempFile)
            val reparsed = parser.parseFile(tempFile)

            assertEquals(2, reparsed.attemptCount)
            assertEquals(2, reparsed.attemptHistory.size)
            assertNull(reparsed.attemptHistory.find { it.id == 2 })
            assertEquals(2, reparsed.segments[0].segmentHistory.size)
            assertTrue(reparsed.segments[0].segmentHistory.none { it.id == 2 })
            // Remaining attempts preserved
            assertNotNull(reparsed.attemptHistory.find { it.id == 1 })
            assertNotNull(reparsed.attemptHistory.find { it.id == 3 })
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `deleting PB attempt recalculates best segment times from remaining history`() {
        // Attempt 1: segment A=50s, B=80s (PB, total 130s)
        // Attempt 2: segment A=60s, B=70s (total 130s, but B is faster)
        // Delete attempt 1 → best segments should be A=60s, B=70s
        val doc = makeDoc(
            segments = listOf(
                LiveSplitSegment("A", null, LiveSplitTimeSpan(50000, null),
                    listOf(LiveSplitComparisonSplit("Personal Best", 50000, null)),
                    listOf(LiveSplitHistoryEntry(1, 50000, null), LiveSplitHistoryEntry(2, 60000, null))),
                LiveSplitSegment("B", null, LiveSplitTimeSpan(70000, null),
                    listOf(LiveSplitComparisonSplit("Personal Best", 130000, null)),
                    listOf(LiveSplitHistoryEntry(1, 80000, null), LiveSplitHistoryEntry(2, 70000, null)))
            ),
            attempts = listOf(
                LiveSplitAttempt(1, null, null, 130000, null),
                LiveSplitAttempt(2, null, null, 130000, null)
            )
        )

        // Simulate deleteLssAttempt logic: remove attempt 1, recalculate
        val deletedId = 1
        val remainingAttempts = doc.attemptHistory.filter { it.id != deletedId }
        val segmentsWithoutAttempt = doc.segments.map { segment ->
            segment.copy(segmentHistory = segment.segmentHistory.filter { it.id != deletedId })
        }

        // Recalculate best segment times
        val recalculated = segmentsWithoutAttempt.map { segment ->
            val bestRealTime = segment.segmentHistory.mapNotNull { it.realTime }.filter { it > 0 }.minOrNull()
            segment.copy(bestSegmentTime = bestRealTime?.let { LiveSplitTimeSpan(it, null) })
        }

        assertEquals(60000L, recalculated[0].bestSegmentTime?.realTime, "Best segment A should be 60s from attempt 2")
        assertEquals(70000L, recalculated[1].bestSegmentTime?.realTime, "Best segment B should be 70s from attempt 2")
    }

    @Test
    fun `deleting PB attempt recalculates personal best from next fastest attempt`() {
        // Attempt 1: 100s total (PB), segments A=40s, B=60s
        // Attempt 2: 150s total, segments A=50s, B=100s
        // Delete attempt 1 → PB should now be attempt 2
        val doc = makeDoc(
            segments = listOf(
                LiveSplitSegment("A", null, LiveSplitTimeSpan(40000, null),
                    listOf(LiveSplitComparisonSplit("Personal Best", 40000, null)),
                    listOf(LiveSplitHistoryEntry(1, 40000, null), LiveSplitHistoryEntry(2, 50000, null))),
                LiveSplitSegment("B", null, LiveSplitTimeSpan(60000, null),
                    listOf(LiveSplitComparisonSplit("Personal Best", 100000, null)),
                    listOf(LiveSplitHistoryEntry(1, 60000, null), LiveSplitHistoryEntry(2, 100000, null)))
            ),
            attempts = listOf(
                LiveSplitAttempt(1, null, null, 100000, null),
                LiveSplitAttempt(2, null, null, 150000, null)
            )
        )

        val deletedId = 1
        val remainingAttempts = doc.attemptHistory.filter { it.id != deletedId }
        val segmentsWithoutAttempt = doc.segments.map { segment ->
            segment.copy(segmentHistory = segment.segmentHistory.filter { it.id != deletedId })
        }

        // Find new PB attempt
        val pbAttempt = remainingAttempts.filter { (it.realTime ?: 0L) > 0L }.minByOrNull { it.realTime!! }
        assertNotNull(pbAttempt)
        assertEquals(2, pbAttempt!!.id, "New PB should be attempt 2")

        // Rebuild cumulative PB times
        var cumulative = 0L
        val recalculated = segmentsWithoutAttempt.map { segment ->
            val segTime = segment.segmentHistory.find { it.id == pbAttempt.id }?.realTime
            if (segTime != null) cumulative += segTime
            val pbEntry = segTime?.let { LiveSplitComparisonSplit("Personal Best", cumulative, null) }
            val updatedSplitTimes = segment.splitTimes.filter { it.comparisonName != "Personal Best" } +
                listOfNotNull(pbEntry)
            segment.copy(splitTimes = updatedSplitTimes)
        }

        val pbA = recalculated[0].splitTimes.find { it.comparisonName == "Personal Best" }
        val pbB = recalculated[1].splitTimes.find { it.comparisonName == "Personal Best" }
        assertEquals(50000L, pbA?.realTime, "PB cumulative A should be 50s from attempt 2")
        assertEquals(150000L, pbB?.realTime, "PB cumulative B should be 150s from attempt 2")
    }

    @Test
    fun `deleting all completed attempts clears personal best`() {
        val doc = makeDoc(
            segments = listOf(
                LiveSplitSegment("A", null, LiveSplitTimeSpan(50000, null),
                    listOf(LiveSplitComparisonSplit("Personal Best", 50000, null)),
                    listOf(LiveSplitHistoryEntry(1, 50000, null)))
            ),
            attempts = listOf(
                LiveSplitAttempt(1, null, null, 50000, null)
            )
        )

        val remainingAttempts = doc.attemptHistory.filter { it.id != 1 }
        val segmentsWithoutAttempt = doc.segments.map { segment ->
            segment.copy(segmentHistory = segment.segmentHistory.filter { it.id != 1 })
        }

        // No completed attempts → clear PB
        val pbAttempt = remainingAttempts.filter { (it.realTime ?: 0L) > 0L }.minByOrNull { it.realTime!! }
        assertNull(pbAttempt, "No PB attempt should remain")

        val recalculated = segmentsWithoutAttempt.map { segment ->
            segment.copy(
                splitTimes = segment.splitTimes.filter { it.comparisonName != "Personal Best" },
                bestSegmentTime = null
            )
        }

        val pbA = recalculated[0].splitTimes.find { it.comparisonName == "Personal Best" }
        assertNull(pbA, "PB should be cleared")
        assertNull(recalculated[0].bestSegmentTime, "Best segment should be cleared")
    }

    @Test
    fun `filtering zero-time LSS attempts excludes resets`() {
        val attempts = listOf(
            LiveSplitAttempt(1, "04/01/2026 10:00:00", "04/01/2026 10:05:00", 300000, null),
            LiveSplitAttempt(2, "04/02/2026 10:00:00", null, null, null),  // reset, no time
            LiveSplitAttempt(3, "04/03/2026 10:00:00", null, null, null),  // reset, no time
            LiveSplitAttempt(4, "04/04/2026 10:00:00", null, 0, null),    // zero time
            LiveSplitAttempt(5, "04/05/2026 10:00:00", "04/05/2026 10:10:00", 600000, null)
        )

        val displayable = attempts.filter { (it.realTime ?: 0L) > 0L }

        assertEquals(2, displayable.size)
        assertEquals(1, displayable[0].id)
        assertEquals(5, displayable[1].id)
    }
}
