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
