package com.supermetroid.livesplit

import com.supermetroid.model.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for LiveSplit round-trip integrity, XML declaration, icon handling,
 * and the converter's fromRunHistory flow.
 */
class LiveSplitRoundTripTest {

    private lateinit var parser: LiveSplitParser
    private lateinit var writer: LiveSplitWriter
    private lateinit var converter: LiveSplitConverter

    @BeforeEach
    fun setup() {
        parser = LiveSplitParser()
        writer = LiveSplitWriter()
        converter = LiveSplitConverter()
    }

    // =============================================
    // XML Declaration
    // =============================================

    @Test
    fun `writer outputs XML declaration`() {
        val doc = minimalDocument()
        val xml = writer.writeToString(doc)
        assertTrue(
            xml.trimStart().startsWith("<?xml"),
            "Written LSS should start with XML declaration, got: ${xml.take(50)}"
        )
    }

    @Test
    fun `writer XML declaration specifies UTF-8 encoding`() {
        val doc = minimalDocument()
        val xml = writer.writeToString(doc)
        val firstLine = xml.lines().first().trim()
        assertTrue(
            firstLine.contains("UTF-8", ignoreCase = true),
            "XML declaration should specify UTF-8 encoding"
        )
    }

    // =============================================
    // Icon round-trip
    // =============================================

    @Test
    fun `round trip preserves segment icons`() {
        val icon = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAAC0lEQVQI12NgAAIABQAB" // tiny valid base64
        val doc = LiveSplitDocument(
            gameName = "Super Metroid",
            categoryName = "Test",
            attemptCount = 1,
            segments = listOf(
                LiveSplitSegment(
                    name = "Split With Icon",
                    icon = icon,
                    bestSegmentTime = LiveSplitTimeSpan(realTime = 60000, gameTime = null),
                    splitTimes = emptyList(),
                    segmentHistory = emptyList()
                )
            ),
            attemptHistory = emptyList()
        )

        val xml = writer.writeToString(doc)
        val reparsed = parser.parseString(xml)

        assertEquals(icon, reparsed.segments[0].icon, "Icon should survive round-trip")
    }

    @Test
    fun `round trip with null icon does not produce non-empty icon text`() {
        val doc = LiveSplitDocument(
            gameName = "Super Metroid",
            categoryName = "Test",
            attemptCount = 1,
            segments = listOf(
                LiveSplitSegment(
                    name = "No Icon",
                    icon = null,
                    bestSegmentTime = null,
                    splitTimes = emptyList(),
                    segmentHistory = emptyList()
                )
            ),
            attemptHistory = emptyList()
        )

        val xml = writer.writeToString(doc)
        val reparsed = parser.parseString(xml)

        assertNull(reparsed.segments[0].icon, "Null icon should remain null after round-trip")
    }

    // =============================================
    // 100% LSS full round-trip icon preservation
    // =============================================

    @Test
    fun `round trip 100 percent LSS preserves all segment icons`() {
        val stream = javaClass.getResourceAsStream("/livesplit/100_percent.lss")
            ?: error("Test resource not found")
        val original = parser.parse(stream)

        val xml = writer.writeToString(original)
        val reparsed = parser.parseString(xml)

        for (i in original.segments.indices) {
            assertEquals(
                original.segments[i].icon,
                reparsed.segments[i].icon,
                "Icon for segment '${original.segments[i].name}' should be preserved"
            )
        }
    }

    // =============================================
    // Converter: fromRunHistory
    // =============================================

    @Test
    fun `fromRunHistory creates valid document with attempt history`() {
        val profile = SplitProfile(
            id = "test",
            name = "Test Profile",
            splits = listOf(
                Split(id = "split_a", name = "Split A", type = "event"),
                Split(id = "split_b", name = "Split B", type = "event")
            )
        )

        val now = Clock.System.now()
        val runs = listOf(
            makeRun("run1", profile, 120_000L, now),
            makeRun("run2", profile, 100_000L, now + 10.minutes)
        )

        val doc = converter.fromRunHistory(runs, profile)

        assertEquals("Super Metroid", doc.gameName)
        assertEquals(2, doc.attemptCount)
        assertEquals(2, doc.attemptHistory.size)
        assertEquals(2, doc.segments.size)
        assertEquals("Split A", doc.segments[0].name)
        assertEquals("Split B", doc.segments[1].name)
    }

    @Test
    fun `fromRunHistory picks fastest run as PB`() {
        val profile = SplitProfile(
            id = "test",
            name = "Test",
            splits = listOf(
                Split(id = "split_a", name = "A", type = "event")
            )
        )

        val now = Clock.System.now()
        val slowRun = makeRun("slow", profile, 200_000L, now)
        val fastRun = makeRun("fast", profile, 100_000L, now + 10.minutes)

        val doc = converter.fromRunHistory(listOf(slowRun, fastRun), profile)

        // PB comparison should come from the faster run
        val pbSplit = doc.segments[0].splitTimes.find { it.comparisonName == "Personal Best" }
        assertNotNull(pbSplit, "Should have a Personal Best comparison")
        assertEquals(100_000L, pbSplit!!.realTime, "PB should be from the fastest run")
    }

    @Test
    fun `fromRunHistory tracks best segment times across runs`() {
        val profile = SplitProfile(
            id = "test",
            name = "Test",
            splits = listOf(
                Split(id = "a", name = "A", type = "event"),
                Split(id = "b", name = "B", type = "event")
            )
        )

        val now = Clock.System.now()
        // Run 1: A=40s, B=80s (segment B=40s)
        val run1 = RunSession(
            id = "r1", profileId = "test",
            startTime = now, endTime = now + 2.minutes,
            completedSplits = listOf(
                CompletedSplit("a", SplitTime(totalTime = 40_000, segmentTime = 40_000), now + 40.seconds),
                CompletedSplit("b", SplitTime(totalTime = 80_000, segmentTime = 40_000), now + 80.seconds)
            ),
            totalTime = 80_000
        )
        // Run 2: A=50s, B=70s (segment B=20s — faster segment B)
        val run2 = RunSession(
            id = "r2", profileId = "test",
            startTime = now + 10.minutes, endTime = now + 12.minutes,
            completedSplits = listOf(
                CompletedSplit("a", SplitTime(totalTime = 50_000, segmentTime = 50_000), now + 10.minutes + 50.seconds),
                CompletedSplit("b", SplitTime(totalTime = 70_000, segmentTime = 20_000), now + 10.minutes + 70.seconds)
            ),
            totalTime = 70_000
        )

        val doc = converter.fromRunHistory(listOf(run1, run2), profile)

        // Best segment for A should be 40s (from run1), B should be 20s (from run2)
        assertEquals(40_000L, doc.segments[0].bestSegmentTime?.realTime, "Best segment A")
        assertEquals(20_000L, doc.segments[1].bestSegmentTime?.realTime, "Best segment B")
    }

    @Test
    fun `fromRunHistory round-trips through writer and parser`() {
        val profile = SplitProfile(
            id = "test",
            name = "Round Trip Test",
            splits = listOf(
                Split(id = "a", name = "Alpha", type = "boss"),
                Split(id = "b", name = "Beta", type = "item")
            )
        )

        val now = Clock.System.now()
        val run = makeRun("r1", profile, 60_000L, now)

        val doc = converter.fromRunHistory(listOf(run), profile)
        val xml = writer.writeToString(doc)
        val reparsed = parser.parseString(xml)

        assertEquals(doc.gameName, reparsed.gameName)
        assertEquals(doc.segments.size, reparsed.segments.size)
        assertEquals(doc.attemptHistory.size, reparsed.attemptHistory.size)
        assertEquals(
            doc.segments[0].bestSegmentTime?.realTime,
            reparsed.segments[0].bestSegmentTime?.realTime
        )
    }

    // =============================================
    // Converter: formatInstantForLiveSplit
    // =============================================

    @Test
    fun `formatInstantForLiveSplit produces consistent format`() {
        val instant = Instant.parse("2025-06-15T14:30:00Z")
        val formatted = LiveSplitConverter.formatInstantForLiveSplit(instant)

        // Should be MM/dd/yyyy HH:mm:ss format (exact value depends on system timezone)
        assertTrue(
            formatted.matches(Regex("""\d{2}/\d{2}/\d{4} \d{2}:\d{2}:\d{2}""")),
            "Should match MM/dd/yyyy HH:mm:ss pattern, got: $formatted"
        )
    }

    // =============================================
    // Parser edge cases
    // =============================================

    @Test
    fun `parser handles segment with no split times`() {
        val xml = """
            <Run version="1.7.0">
                <GameName>Test</GameName>
                <CategoryName>Any%</CategoryName>
                <AttemptCount>0</AttemptCount>
                <Offset>00:00:00.0000000</Offset>
                <AttemptHistory/>
                <Segments>
                    <Segment>
                        <Name>NoTimes</Name>
                        <Icon/>
                        <BestSegmentTime/>
                        <SplitTimes/>
                        <SegmentHistory/>
                    </Segment>
                </Segments>
            </Run>
        """.trimIndent()

        val doc = parser.parseString(xml)
        assertEquals(1, doc.segments.size)
        assertTrue(doc.segments[0].splitTimes.isEmpty())
        assertTrue(doc.segments[0].segmentHistory.isEmpty())
        assertNull(doc.segments[0].bestSegmentTime)
    }

    @Test
    fun `parser handles negative segment history times`() {
        val xml = """
            <Run version="1.7.0">
                <GameName>Test</GameName>
                <CategoryName>Any%</CategoryName>
                <AttemptCount>1</AttemptCount>
                <Offset>00:00:00.0000000</Offset>
                <AttemptHistory/>
                <Segments>
                    <Segment>
                        <Name>Split</Name>
                        <Icon/>
                        <BestSegmentTime><RealTime>00:01:00.0000000</RealTime></BestSegmentTime>
                        <SplitTimes/>
                        <SegmentHistory>
                            <Time id="1"><RealTime>-00:00:05.0000000</RealTime></Time>
                        </SegmentHistory>
                    </Segment>
                </Segments>
            </Run>
        """.trimIndent()

        val doc = parser.parseString(xml)
        val historyEntry = doc.segments[0].segmentHistory[0]
        assertEquals(-5000L, historyEntry.realTime, "Negative segment time should parse correctly")
    }

    @Test
    fun `msToTimeString round trips for various values`() {
        val testValues = listOf(0L, 1L, 999L, 1000L, 60000L, 3600000L, 4977139L, 86400000L)
        for (ms in testValues) {
            val str = LiveSplitParser.msToTimeString(ms)
            val parsed = LiveSplitParser.parseTimeToMs(str)
            assertEquals(ms, parsed, "Round trip failed for $ms ms -> '$str'")
        }
    }

    // =============================================
    // Helpers
    // =============================================

    private fun minimalDocument() = LiveSplitDocument(
        gameName = "Super Metroid",
        categoryName = "Any%",
        attemptCount = 0,
        segments = listOf(
            LiveSplitSegment(
                name = "Done",
                icon = null,
                bestSegmentTime = null,
                splitTimes = emptyList(),
                segmentHistory = emptyList()
            )
        ),
        attemptHistory = emptyList()
    )

    private fun makeRun(
        id: String,
        profile: SplitProfile,
        totalTimeMs: Long,
        startTime: Instant
    ): RunSession {
        var cumulativeMs = 0L
        val segmentMs = totalTimeMs / profile.splits.size
        val splits = profile.splits.map { split ->
            cumulativeMs += segmentMs
            CompletedSplit(
                splitId = split.id,
                time = SplitTime(totalTime = cumulativeMs, segmentTime = segmentMs),
                timestamp = startTime + cumulativeMs.seconds
            )
        }
        return RunSession(
            id = id,
            profileId = profile.id,
            startTime = startTime,
            endTime = startTime + totalTimeMs.seconds,
            completedSplits = splits,
            totalTime = totalTimeMs
        )
    }
}
