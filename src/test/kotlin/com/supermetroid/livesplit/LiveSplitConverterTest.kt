package com.supermetroid.livesplit

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LiveSplitConverterTest {

    private lateinit var parser: LiveSplitParser
    private lateinit var converter: LiveSplitConverter

    @BeforeEach
    fun setup() {
        parser = LiveSplitParser()
        converter = LiveSplitConverter()
    }

    private fun load100PercentDoc(): LiveSplitDocument {
        val stream = javaClass.getResourceAsStream("/livesplit/100_percent.lss")
            ?: error("Test resource not found")
        return parser.parse(stream)
    }

    // =============================================
    // Segment name → split ID mapping
    // =============================================

    @Test
    fun `deriveSplitId - common boss names`() {
        assertEquals("phantoon", converter.deriveSplitId("Phantoon"))
        assertEquals("phantoon", converter.deriveSplitId("Phaaan"))
        assertEquals("kraid", converter.deriveSplitId("Kraid"))
        assertEquals("draygon", converter.deriveSplitId("Draygon"))
        assertEquals("draygon", converter.deriveSplitId("Water"))
        assertEquals("ridley", converter.deriveSplitId("Ridley"))
    }

    @Test
    fun `deriveSplitId - common item names`() {
        assertEquals("bomb", converter.deriveSplitId("Bomb"))
        assertEquals("varia_suit", converter.deriveSplitId("Varia"))
        assertEquals("gravity_suit", converter.deriveSplitId("Gravity"))
        assertEquals("space_jump", converter.deriveSplitId("Space"))
        assertEquals("plasma_beam", converter.deriveSplitId("Plasma"))
        assertEquals("screw_attack", converter.deriveSplitId("Screw"))
        assertEquals("grapple_beam", converter.deriveSplitId("Grapple"))
        assertEquals("reserve_tank", converter.deriveSplitId("Reserve"))
    }

    @Test
    fun `deriveSplitId - event names`() {
        assertEquals("golden_four", converter.deriveSplitId("G4"))
        assertEquals("ship", converter.deriveSplitId("Done"))
        assertEquals("ship", converter.deriveSplitId("Ship"))
        assertEquals("ceres_station", converter.deriveSplitId("Ceres"))
    }

    @Test
    fun `deriveSplitId - case insensitive`() {
        assertEquals("bomb", converter.deriveSplitId("BOMB"))
        assertEquals("bomb", converter.deriveSplitId("bomb"))
        assertEquals("bomb", converter.deriveSplitId("Bomb"))
    }

    @Test
    fun `deriveSplitId - unknown name falls back to sanitized version`() {
        assertEquals("my_custom_split", converter.deriveSplitId("My Custom Split"))
        assertEquals("lower_norfair", converter.deriveSplitId("Lower Norfair"))
    }

    // =============================================
    // LSS → SplitProfile conversion
    // =============================================

    @Test
    fun `toSplitProfile - 100 percent creates profile with 12 splits`() {
        val doc = load100PercentDoc()
        val profile = converter.toSplitProfile(doc, "hundred-percent")

        assertEquals("hundred-percent", profile.id)
        assertEquals(12, profile.splits.size)
    }

    @Test
    fun `toSplitProfile - 100 percent split IDs are correctly mapped`() {
        val doc = load100PercentDoc()
        val profile = converter.toSplitProfile(doc, "hundred-percent")

        val splitIds = profile.splits.map { it.id }
        assertEquals(
            listOf("bomb", "varia_suit", "grapple_beam", "phantoon", "gravity_suit", "draygon",
                    "space_jump", "plasma_beam", "screw_attack", "reserve_tank", "golden_four", "ship"),
            splitIds
        )
    }

    @Test
    fun `toSplitProfile - preserves original segment names as display names`() {
        val doc = load100PercentDoc()
        val profile = converter.toSplitProfile(doc, "hundred-percent")

        assertEquals("Bomb", profile.splits[0].name)
        assertEquals("Phaaan", profile.splits[3].name)
        assertEquals("Done", profile.splits[11].name)
    }

    @Test
    fun `toSplitProfile - assigns correct types`() {
        val doc = load100PercentDoc()
        val profile = converter.toSplitProfile(doc, "hundred-percent")

        val types = profile.splits.associate { it.id to it.type }
        assertEquals("item", types["bomb"])
        assertEquals("item", types["varia_suit"])
        assertEquals("item", types["grapple_beam"])
        assertEquals("boss", types["phantoon"])
        assertEquals("item", types["gravity_suit"])
        assertEquals("boss", types["draygon"])
        assertEquals("item", types["space_jump"])
        assertEquals("beam", types["plasma_beam"])
        assertEquals("item", types["screw_attack"])
        assertEquals("item", types["reserve_tank"])
        assertEquals("event", types["golden_four"])
        assertEquals("event", types["ship"])
    }

    @Test
    fun `toSplitProfile - auto-generates ID from category when not provided`() {
        val doc = load100PercentDoc()
        val profile = converter.toSplitProfile(doc)
        assertEquals("100", profile.id)
        assertTrue(profile.name.contains("100%"))
    }

    // =============================================
    // LSS → PersonalBest conversion
    // =============================================

    @Test
    fun `toPersonalBest - extracts best segment times`() {
        val doc = load100PercentDoc()
        val pb = converter.toPersonalBest(doc, "hundred-percent")

        assertEquals("hundred-percent", pb.profileId)
        assertTrue(pb.splitTimes.isNotEmpty())
        assertEquals(12, pb.splitTimes.size)

        // Bomb best segment: 00:04:51.8787600 = 291878ms
        val bombBest = pb.splitTimes["bomb"]
        assertNotNull(bombBest)
        assertEquals(291878L, bombBest!!.segmentTime)
    }

    @Test
    fun `toPersonalBest - total time is last segment PB cumulative`() {
        val doc = load100PercentDoc()
        val pb = converter.toPersonalBest(doc, "hundred-percent")

        // PB total should be the last segment's cumulative PB time (01:22:57.1393650 = 4977139ms)
        assertEquals(4977139L, pb.totalTime)
    }

    @Test
    fun `toPersonalBest - cumulative times are set from PB comparison`() {
        val doc = load100PercentDoc()
        val pb = converter.toPersonalBest(doc, "hundred-percent")

        // Bomb PB cumulative: 00:04:57.2103800
        val bombTime = pb.splitTimes["bomb"]!!
        assertEquals(297210L, bombTime.totalTime)

        // Final split (ship/done) cumulative should equal the PB time from comparison
        val shipTime = pb.splitTimes["ship"]!!
        assertEquals(4977139L, shipTime.totalTime)
    }

    @Test
    fun `toPersonalBest - sum of best segments less than total PB time`() {
        val doc = load100PercentDoc()
        val pb = converter.toPersonalBest(doc, "hundred-percent")

        val sumOfBest = pb.splitTimes.values.sumOf { it.segmentTime }
        assertTrue(
            sumOfBest < pb.totalTime,
            "Sum of best segments ($sumOfBest ms) should be < PB total (${ pb.totalTime} ms)"
        )
    }

    @Test
    fun `toPersonalBest - totalTime stores PB cumulative and segmentTime stores best segment`() {
        val doc = load100PercentDoc()
        val profile = converter.toSplitProfile(doc, "hundred-percent")
        val pb = converter.toPersonalBest(doc, "hundred-percent")

        // totalTime values should be monotonically increasing (PB cumulative)
        var prevTotalTime = 0L
        for (split in profile.splits) {
            val st = pb.splitTimes[split.id] ?: continue
            if (st.totalTime > 0) {
                assertTrue(
                    st.totalTime >= prevTotalTime,
                    "Split ${split.id}: totalTime ${st.totalTime} must be >= previous $prevTotalTime"
                )
                prevTotalTime = st.totalTime
            }
        }
        // Last split's totalTime should equal PB total
        assertEquals(pb.totalTime, prevTotalTime, "Last cumulative totalTime must equal PB total")

        // segmentTime (best segment across all attempts) should sum to LESS than PB total
        val sumOfBest = pb.splitTimes.values.sumOf { it.segmentTime }
        assertTrue(
            sumOfBest < pb.totalTime,
            "Sum of best segments ($sumOfBest ms) should be < PB total (${pb.totalTime} ms) " +
            "— they come from different runs"
        )
    }

    // =============================================
    // PB comparison vs fastest attempt cross-check
    // =============================================

    @Test
    fun `toPersonalBest - uses fastest attempt when PB comparison is slower`() {
        // Simulate: PB comparison shows 1:00:38 but fastest attempt is 59:14
        val segments = listOf(
            makeLssSegment("Split A", pbCumulative = 200000, bestSegment = 80000,
                history = listOf(
                    LiveSplitHistoryEntry(1, 95000, null),   // attempt 1: 95s
                    LiveSplitHistoryEntry(2, 100000, null)   // attempt 2: 100s
                )),
            makeLssSegment("Split B", pbCumulative = 400000, bestSegment = 120000,
                history = listOf(
                    LiveSplitHistoryEntry(1, 110000, null),  // attempt 1: 110s
                    LiveSplitHistoryEntry(2, 130000, null)   // attempt 2: 130s
                )),
            makeLssSegment("Split C", pbCumulative = 600000, bestSegment = 150000,
                history = listOf(
                    LiveSplitHistoryEntry(1, 155000, null),  // attempt 1: 155s
                    LiveSplitHistoryEntry(2, 170000, null)   // attempt 2: 170s
                ))
        )
        // PB comparison total = 600000 (from last segment)
        // Attempt 1 total = 360000 (faster!)
        // Attempt 2 total = 400000
        val doc = LiveSplitDocument(
            gameName = "Test", categoryName = "Any%", attemptCount = 2,
            segments = segments,
            attemptHistory = listOf(
                LiveSplitAttempt(1, null, null, 360000, null),
                LiveSplitAttempt(2, null, null, 400000, null)
            )
        )

        val pb = converter.toPersonalBest(doc, "test")

        // Should use attempt 1 (360s) not PB comparison (600s)
        assertEquals(360000L, pb.totalTime,
            "totalTime should come from fastest attempt (360s), not PB comparison (600s)")

        // Cumulative times should be reconstructed from attempt 1's segment history
        assertEquals(95000L, pb.splitTimes["split_a"]?.totalTime,
            "Split A cumulative should be 95s from attempt 1")
        assertEquals(205000L, pb.splitTimes["split_b"]?.totalTime,
            "Split B cumulative should be 95+110=205s from attempt 1")
        assertEquals(360000L, pb.splitTimes["split_c"]?.totalTime,
            "Split C cumulative should be 95+110+155=360s from attempt 1")

        // Best segments should still be from BestSegmentTime, not the attempt
        assertEquals(80000L, pb.splitTimes["split_a"]?.segmentTime)
        assertEquals(120000L, pb.splitTimes["split_b"]?.segmentTime)
        assertEquals(150000L, pb.splitTimes["split_c"]?.segmentTime)
    }

    @Test
    fun `toPersonalBest - uses PB comparison when it matches fastest attempt`() {
        val doc = load100PercentDoc()
        val pb = converter.toPersonalBest(doc, "hundred-percent")

        // 100% LSS: PB comparison matches fastest complete attempt (4977139ms)
        assertEquals(4977139L, pb.totalTime)
        assertEquals("livesplit-import", pb.runSessionId,
            "Should use comparison path when PB comparison matches fastest attempt")
    }

    @Test
    fun `toPersonalBest - ignores incomplete fast attempts`() {
        // Fast attempt exists but has no segment history (quick reset)
        val segments = listOf(
            makeLssSegment("Split A", pbCumulative = 300000, bestSegment = 80000,
                history = listOf(LiveSplitHistoryEntry(2, 150000, null))),
            makeLssSegment("Split B", pbCumulative = 600000, bestSegment = 120000,
                history = listOf(LiveSplitHistoryEntry(2, 160000, null)))
        )
        val doc = LiveSplitDocument(
            gameName = "Test", categoryName = "Any%", attemptCount = 2,
            segments = segments,
            attemptHistory = listOf(
                LiveSplitAttempt(1, null, null, 5000, null),   // 5s quick reset, no segments
                LiveSplitAttempt(2, null, null, 600000, null)  // matches PB comparison
            )
        )

        val pb = converter.toPersonalBest(doc, "test")

        // Should use PB comparison (600s), not the 5s quick reset
        assertEquals(600000L, pb.totalTime)
        assertEquals("livesplit-import", pb.runSessionId)
    }

    private fun makeLssSegment(
        name: String,
        pbCumulative: Long,
        bestSegment: Long,
        history: List<LiveSplitHistoryEntry> = emptyList()
    ) = LiveSplitSegment(
        name = name,
        icon = null,
        bestSegmentTime = LiveSplitTimeSpan(bestSegment, null),
        splitTimes = listOf(LiveSplitComparisonSplit("Personal Best", pbCumulative, null)),
        segmentHistory = history
    )
}
