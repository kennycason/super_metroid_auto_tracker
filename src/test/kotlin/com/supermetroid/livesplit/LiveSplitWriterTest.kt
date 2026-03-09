package com.supermetroid.livesplit

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class LiveSplitWriterTest {

    private lateinit var parser: LiveSplitParser
    private lateinit var writer: LiveSplitWriter

    @BeforeEach
    fun setup() {
        parser = LiveSplitParser()
        writer = LiveSplitWriter()
    }

    @Test
    fun `round trip - parse 100 percent then write then parse again preserves data`() {
        val stream = javaClass.getResourceAsStream("/livesplit/100_percent.lss")
            ?: error("Test resource not found")
        val original = parser.parse(stream)

        val xml = writer.writeToString(original)
        val reparsed = parser.parseString(xml)

        assertEquals(original.gameName, reparsed.gameName)
        assertEquals(original.categoryName, reparsed.categoryName)
        assertEquals(original.segments.size, reparsed.segments.size)
        assertEquals(original.attemptHistory.size, reparsed.attemptHistory.size)

        for (i in original.segments.indices) {
            val origSeg = original.segments[i]
            val repSeg = reparsed.segments[i]
            assertEquals(origSeg.name, repSeg.name, "Segment $i name")
            assertEquals(
                origSeg.bestSegmentTime?.realTime,
                repSeg.bestSegmentTime?.realTime,
                "Segment $i best RT (ms may lose sub-ms precision)"
            )
            assertEquals(origSeg.splitTimes.size, repSeg.splitTimes.size, "Segment $i split times count")
            assertEquals(origSeg.segmentHistory.size, repSeg.segmentHistory.size, "Segment $i history count")
        }
    }

    @Test
    fun `round trip - attempt history preserves completion status`() {
        val stream = javaClass.getResourceAsStream("/livesplit/100_percent.lss")
            ?: error("Test resource not found")
        val original = parser.parse(stream)

        val xml = writer.writeToString(original)
        val reparsed = parser.parseString(xml)

        val origCompleted = original.attemptHistory.count { it.realTime != null }
        val repCompleted = reparsed.attemptHistory.count { it.realTime != null }
        assertEquals(origCompleted, repCompleted, "Completed attempt count should be preserved")

        val origDnf = original.attemptHistory.count { it.realTime == null }
        val repDnf = reparsed.attemptHistory.count { it.realTime == null }
        assertEquals(origDnf, repDnf, "DNF attempt count should be preserved")
    }

    @Test
    fun `write to file creates valid lss file`(@TempDir tempDir: Path) {
        val doc = LiveSplitDocument(
            gameName = "Super Metroid",
            categoryName = "Test Category",
            attemptCount = 5,
            segments = listOf(
                LiveSplitSegment(
                    name = "First Split",
                    icon = null,
                    bestSegmentTime = LiveSplitTimeSpan(realTime = 60000, gameTime = null),
                    splitTimes = listOf(
                        LiveSplitComparisonSplit("Personal Best", realTime = 65000, gameTime = null)
                    ),
                    segmentHistory = listOf(
                        LiveSplitHistoryEntry(1, realTime = 70000, gameTime = null),
                        LiveSplitHistoryEntry(2, realTime = 62000, gameTime = null)
                    )
                ),
                LiveSplitSegment(
                    name = "Done",
                    icon = null,
                    bestSegmentTime = LiveSplitTimeSpan(realTime = 120000, gameTime = null),
                    splitTimes = listOf(
                        LiveSplitComparisonSplit("Personal Best", realTime = 195000, gameTime = null)
                    ),
                    segmentHistory = emptyList()
                )
            ),
            attemptHistory = listOf(
                LiveSplitAttempt(1, "01/01/2025 00:00:00", "01/01/2025 00:05:00", realTime = 200000, gameTime = null),
                LiveSplitAttempt(2, "01/02/2025 00:00:00", "01/02/2025 00:04:00", null, null)
            )
        )

        val file = File(tempDir.toFile(), "test_output.lss")
        writer.writeToFile(doc, file)

        assertTrue(file.exists())
        assertTrue(file.length() > 0)

        val reparsed = parser.parseFile(file)
        assertEquals("Super Metroid", reparsed.gameName)
        assertEquals("Test Category", reparsed.categoryName)
        assertEquals(2, reparsed.segments.size)
        assertEquals(2, reparsed.attemptHistory.size)
        assertEquals("First Split", reparsed.segments[0].name)
        assertEquals(60000L, reparsed.segments[0].bestSegmentTime!!.realTime)
        assertEquals(2, reparsed.segments[0].segmentHistory.size)
    }

    @Test
    fun `write preserves segment best times at millisecond resolution`() {
        val doc = LiveSplitDocument(
            gameName = "Test",
            categoryName = "Any%",
            attemptCount = 1,
            segments = listOf(
                LiveSplitSegment(
                    name = "Split",
                    icon = null,
                    bestSegmentTime = LiveSplitTimeSpan(realTime = 123456, gameTime = 100000),
                    splitTimes = emptyList(),
                    segmentHistory = emptyList()
                )
            ),
            attemptHistory = emptyList()
        )

        val xml = writer.writeToString(doc)
        val reparsed = parser.parseString(xml)

        assertEquals(123456L, reparsed.segments[0].bestSegmentTime!!.realTime)
        assertEquals(100000L, reparsed.segments[0].bestSegmentTime!!.gameTime)
    }
}
