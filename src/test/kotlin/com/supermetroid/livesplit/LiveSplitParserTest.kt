package com.supermetroid.livesplit

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LiveSplitParserTest {

    private lateinit var parser: LiveSplitParser

    @BeforeEach
    fun setup() {
        parser = LiveSplitParser()
    }

    // =============================================
    // Time parsing tests
    // =============================================

    @Test
    fun `parseTimeToMs - standard HH-MM-SS format`() {
        val ms = LiveSplitParser.parseTimeToMs("01:22:57.1393650")
        assertNotNull(ms)
        // 1h 22m 57s = 4977000ms + 139ms (1393650 ticks / 10000)
        assertEquals(4977139L, ms)
    }

    @Test
    fun `parseTimeToMs - short time under a minute`() {
        val ms = LiveSplitParser.parseTimeToMs("00:00:51.8787600")
        assertNotNull(ms)
        // 51s + 878ms
        assertEquals(51878L, ms)
    }

    @Test
    fun `parseTimeToMs - MM-SS format with minutes`() {
        val ms = LiveSplitParser.parseTimeToMs("04:51.8787600")
        assertNotNull(ms)
        // 4m 51s + 878ms
        assertEquals(291878L, ms)
    }

    @Test
    fun `parseTimeToMs - zero fractional`() {
        val ms = LiveSplitParser.parseTimeToMs("00:05:07.0000000")
        assertNotNull(ms)
        assertEquals(307000L, ms)
    }

    @Test
    fun `parseTimeToMs - blank returns null`() {
        assertNull(LiveSplitParser.parseTimeToMs(""))
        assertNull(LiveSplitParser.parseTimeToMs("   "))
    }

    @Test
    fun `parseTimeToMs - negative time`() {
        val ms = LiveSplitParser.parseTimeToMs("-00:00:05.0000000")
        assertNotNull(ms)
        assertEquals(-5000L, ms)
    }

    @Test
    fun `msToTimeString - round trip preserves value`() {
        val original = "01:22:57.1390000"
        val ms = LiveSplitParser.parseTimeToMs(original)!!
        val backToString = LiveSplitParser.msToTimeString(ms)
        val roundTripped = LiveSplitParser.parseTimeToMs(backToString)!!
        assertEquals(ms, roundTripped, "Round trip should preserve millisecond value")
    }

    @Test
    fun `msToTimeString - zero`() {
        assertEquals("00:00:00.0000000", LiveSplitParser.msToTimeString(0))
    }

    @Test
    fun `msToTimeString - negative`() {
        val result = LiveSplitParser.msToTimeString(-5000)
        assertTrue(result.startsWith("-"))
        assertEquals(-5000L, LiveSplitParser.parseTimeToMs(result))
    }

    // =============================================
    // 100% LSS file parsing tests
    // =============================================

    private fun load100PercentDoc(): LiveSplitDocument {
        val stream = javaClass.getResourceAsStream("/livesplit/100_percent.lss")
            ?: error("Test resource /livesplit/100_percent.lss not found")
        return parser.parse(stream)
    }

    @Test
    fun `parse 100 percent - game and category metadata`() {
        val doc = load100PercentDoc()
        assertEquals("Super Metroid", doc.gameName)
        assertEquals("100%", doc.categoryName)
        assertTrue(doc.attemptCount > 200, "Should have 200+ attempts")
    }

    @Test
    fun `parse 100 percent - correct number of segments`() {
        val doc = load100PercentDoc()
        assertEquals(12, doc.segments.size, "100% LSS has 12 segments")
    }

    @Test
    fun `parse 100 percent - segment names`() {
        val doc = load100PercentDoc()
        val names = doc.segments.map { it.name }
        assertEquals(
            listOf("Bomb", "Varia", "Grapple", "Phaaan", "Gravity", "Water",
                    "Space", "Plasma", "Screw", "Reserve", "G4", "Done"),
            names
        )
    }

    @Test
    fun `parse 100 percent - first segment best time`() {
        val doc = load100PercentDoc()
        val bombSegment = doc.segments[0]
        assertEquals("Bomb", bombSegment.name)
        assertNotNull(bombSegment.bestSegmentTime)
        // Best segment time for Bomb is 00:04:51.8787600 = 291878ms
        assertEquals(291878L, bombSegment.bestSegmentTime!!.realTime)
    }

    @Test
    fun `parse 100 percent - PB split times are cumulative`() {
        val doc = load100PercentDoc()
        var prevTime = 0L
        for (segment in doc.segments) {
            val pbSplit = segment.splitTimes.find { it.comparisonName == "Personal Best" }
            assertNotNull(pbSplit, "Segment '${segment.name}' should have a Personal Best split time")
            val cumulative = pbSplit!!.realTime!!
            assertTrue(
                cumulative > prevTime,
                "Cumulative PB time should increase: ${segment.name} ($cumulative) should be > $prevTime"
            )
            prevTime = cumulative
        }
    }

    @Test
    fun `parse 100 percent - final PB time matches expected`() {
        val doc = load100PercentDoc()
        val lastSegment = doc.segments.last()
        val finalPb = lastSegment.splitTimes.find { it.comparisonName == "Personal Best" }
        assertNotNull(finalPb)
        // 01:22:57.1393650 = 4977139ms
        assertEquals(4977139L, finalPb!!.realTime)
    }

    @Test
    fun `parse 100 percent - segment history present`() {
        val doc = load100PercentDoc()
        val bombSegment = doc.segments[0]
        assertTrue(bombSegment.segmentHistory.isNotEmpty(), "Bomb segment should have history entries")
        assertTrue(bombSegment.segmentHistory.size > 50, "Bomb should have many history entries")
    }

    @Test
    fun `parse 100 percent - attempt history`() {
        val doc = load100PercentDoc()
        assertTrue(doc.attemptHistory.isNotEmpty())
        // Should have both completed (with realTime) and DNF (without) attempts
        val completed = doc.attemptHistory.filter { it.realTime != null }
        val dnf = doc.attemptHistory.filter { it.realTime == null }
        assertTrue(completed.isNotEmpty(), "Should have completed attempts")
        assertTrue(dnf.isNotEmpty(), "Should have DNF attempts")
    }

    @Test
    fun `parse 100 percent - best segment times form sum-of-best less than PB`() {
        val doc = load100PercentDoc()
        val sumOfBest = doc.segments.mapNotNull { it.bestSegmentTime?.realTime }.sum()
        val finalPb = doc.segments.last().splitTimes
            .find { it.comparisonName == "Personal Best" }!!.realTime!!

        assertTrue(
            sumOfBest < finalPb,
            "Sum of best segments ($sumOfBest ms) should be less than PB ($finalPb ms)"
        )
    }

    @Test
    fun `parse 100 percent - segment icons are present`() {
        val doc = load100PercentDoc()
        for (segment in doc.segments) {
            assertNotNull(segment.icon, "Segment '${segment.name}' should have an icon")
            assertTrue(segment.icon!!.length > 10, "Icon should be a non-trivial base64 string")
        }
    }

    @Test
    fun `parse 100 percent - both real and game time tracked`() {
        val doc = load100PercentDoc()
        val bombSegment = doc.segments[0]
        assertNotNull(bombSegment.bestSegmentTime?.realTime, "Should have real time")
        assertNotNull(bombSegment.bestSegmentTime?.gameTime, "Should have game time")
    }

    // =============================================
    // KPDR LSS file parsing tests
    // =============================================

    private fun loadKpdrDoc(): LiveSplitDocument {
        val stream = javaClass.getResourceAsStream("/livesplit/kpdr_rid.lss")
            ?: error("Test resource /livesplit/kpdr_rid.lss not found")
        return parser.parse(stream)
    }

    @Test
    fun `parse KPDR - category is Any percent`() {
        val doc = loadKpdrDoc()
        assertEquals("Super Metroid", doc.gameName)
        assertEquals("Any% KPDR", doc.categoryName)
    }

    @Test
    fun `parse KPDR - has segments`() {
        val doc = loadKpdrDoc()
        assertTrue(doc.segments.isNotEmpty(), "KPDR file should have segments")
    }

    // =============================================
    // Edge case / error handling
    // =============================================

    @Test
    fun `parse minimal valid LSS`() {
        val xml = """
            <Run version="1.7.0">
                <GameName>Test</GameName>
                <CategoryName>Any%</CategoryName>
                <AttemptCount>0</AttemptCount>
                <Offset>00:00:00.0000000</Offset>
                <AttemptHistory/>
                <Segments>
                    <Segment>
                        <Name>Start</Name>
                        <Icon/>
                        <BestSegmentTime/>
                        <SplitTimes>
                            <SplitTime name="Personal Best"/>
                        </SplitTimes>
                        <SegmentHistory/>
                    </Segment>
                </Segments>
            </Run>
        """.trimIndent()

        val doc = parser.parseString(xml)
        assertEquals("Test", doc.gameName)
        assertEquals(1, doc.segments.size)
        assertEquals("Start", doc.segments[0].name)
        assertNull(doc.segments[0].bestSegmentTime)
    }

    @Test
    fun `parse rejects non-Run root element`() {
        val xml = "<NotARun><GameName>Test</GameName></NotARun>"
        assertThrows(IllegalArgumentException::class.java) {
            parser.parseString(xml)
        }
    }
}
