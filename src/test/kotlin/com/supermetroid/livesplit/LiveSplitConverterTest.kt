package com.supermetroid.livesplit

import com.supermetroid.model.*
import kotlinx.datetime.Instant
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

    private fun loadKaizoDoc(): LiveSplitDocument {
        val stream = javaClass.getResourceAsStream("/livesplit/Super Metroid Kaizo Possible Hacks 2019.lss")
            ?: error("Kaizo test resource not found")
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
    fun `deriveSplitId - common PRKD LiveSplit names`() {
        assertEquals("first_power_bomb", converter.deriveSplitId("Power Bombs"))
        assertEquals("lower_norfair_elevator", converter.deriveSplitId("LN Elevator"))
        assertEquals("ridley", converter.deriveSplitId("Ridley Dead"))
        assertEquals("botwoon", converter.deriveSplitId("Botwoon"))
        assertEquals("draygon", converter.deriveSplitId("Draygon Dead"))
        assertEquals("ship", converter.deriveSplitId("Escape"))
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
    fun `toSplitProfile - assigns room trigger for imported lower norfair elevator split`() {
        val doc = LiveSplitDocument(
            gameName = "Super Metroid",
            categoryName = "Any% PRKD",
            attemptCount = 0,
            segments = listOf(
                LiveSplitSegment(
                    name = "LN Elevator",
                    icon = null,
                    bestSegmentTime = null,
                    splitTimes = emptyList(),
                    segmentHistory = emptyList()
                )
            ),
            attemptHistory = emptyList()
        )

        val profile = converter.toSplitProfile(doc, "prkd-import")
        val split = profile.splits.single()

        assertEquals("lower_norfair_elevator", split.id)
        assertEquals("room_entry", split.type)
        assertEquals(0xAF3F, split.triggerRoomId)
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

    // =============================================
    // Kaizo Possible LSS parsing
    // =============================================

    @Test
    fun `kaizo - parses all 55 segments`() {
        val doc = loadKaizoDoc()
        assertEquals(55, doc.segments.size)
        assertEquals("Super Metroid ROM Hacks", doc.gameName)
        assertEquals("(2019) Kaizo Possible", doc.categoryName)
    }

    @Test
    fun `kaizo - 2025 attempts with 2065 attempt entries`() {
        val doc = loadKaizoDoc()
        assertEquals(2025, doc.attemptCount)
        assertEquals(2065, doc.attemptHistory.size)
    }

    @Test
    fun `kaizo - toSplitProfile creates profile with 55 splits`() {
        val doc = loadKaizoDoc()
        val profile = converter.toSplitProfile(doc)
        assertEquals(55, profile.splits.size)
        // First split name preserved
        assertEquals("Morph save", profile.splits[0].name)
        // Last split
        assertEquals("End", profile.splits[54].name)
    }

    @Test
    fun `kaizo - toPersonalBest extracts PB at 3h36m`() {
        val doc = loadKaizoDoc()
        val pb = converter.toPersonalBest(doc, "kaizo")

        // PB total: 03:36:14.128 = 12974128ms
        assertTrue(pb.totalTime in 12970000L..12980000L,
            "PB total should be ~3:36:14 (${pb.totalTime}ms)")

        // Should have all 55 split times
        assertEquals(55, pb.splitTimes.size)

        // Sum of best segments should be less than PB (gold splits from different runs)
        val sumOfBest = pb.splitTimes.values.sumOf { it.segmentTime }
        assertTrue(sumOfBest < pb.totalTime,
            "Sum of best ($sumOfBest) should be < PB total (${pb.totalTime})")
    }

    @Test
    fun `kaizo - segment history has rich data for early splits`() {
        val doc = loadKaizoDoc()
        // First segment has 796 history entries (lots of resets)
        assertTrue(doc.segments[0].segmentHistory.size >= 700,
            "Morph save should have 700+ history entries, got ${doc.segments[0].segmentHistory.size}")

        // Last segment has 18 (only 18 completions)
        assertEquals(18, doc.segments[54].segmentHistory.size)
    }

    @Test
    fun `kaizo - toRunHistory converts all attempts to RunSessions`() {
        val doc = loadKaizoDoc()
        val runs = converter.toRunHistory(doc, "kaizo")

        // Should have an entry for every attempt
        assertEquals(doc.attemptHistory.size, runs.size)

        // Only truly complete runs (RealTime AND all segments) should have endTime
        val completed = runs.filter { it.endTime != null }
        assertEquals(7, completed.size, "Should have 7 fully completed runs (all 55 segments)")

        // Failed/incomplete runs are the rest
        val incomplete = runs.filter { it.endTime == null }
        assertTrue(incomplete.size > 1000, "Should have 1000+ incomplete/reset runs")

        // First segment should be reached by many runs (796 history entries)
        val runsReachingFirstSplit = runs.count { run ->
            run.completedSplits.any { it.splitId == "morph_save" }
        }
        assertTrue(runsReachingFirstSplit >= 700,
            "700+ runs should reach first split, got $runsReachingFirstSplit")
    }

    @Test
    fun `kaizo - handles duplicate segment names with unique IDs`() {
        val doc = loadKaizoDoc()
        val profile = converter.toSplitProfile(doc)

        // File has multiple "Ship" and "Upper Norfair first save" segments
        // Profile should have unique split IDs for all of them (55 unique IDs)
        val ids = profile.splits.map { it.id }
        assertEquals(55, ids.toSet().size,
            "All split IDs should be unique, found duplicates: ${ids.groupBy { it }.filter { it.value.size > 1 }.keys}")

        // First "ship" gets "ship", second gets "ship_2", etc.
        val shipIds = ids.filter { it.startsWith("ship") }
        assertTrue(shipIds.contains("ship"), "First Ship should be 'ship'")
        assertTrue(shipIds.contains("ship_2"), "Second Ship should be 'ship_2'")
    }

    @Test
    fun `parseLiveSplitTimestamp parses attempt dates`() {
        val parsed = LiveSplitConverter.parseLiveSplitTimestamp("04/01/2026 10:00:00")

        assertNotNull(parsed)
        assertEquals("04/01/2026 10:00:00", LiveSplitConverter.formatInstantForLiveSplit(parsed!!))
    }

    @Test
    fun `toRunHistory uses stable fallback for missing attempt dates`() {
        val doc = LiveSplitDocument(
            gameName = "Test",
            categoryName = "Any%",
            attemptCount = 2,
            segments = listOf(
                makeLssSegment(
                    name = "Split A",
                    pbCumulative = 1_000L,
                    bestSegment = 1_000L,
                    history = listOf(
                        LiveSplitHistoryEntry(2, 1_000L, null),
                        LiveSplitHistoryEntry(5, 1_500L, null)
                    )
                )
            ),
            attemptHistory = listOf(
                LiveSplitAttempt(2, null, null, 1_000L, null),
                LiveSplitAttempt(5, "", "", 1_500L, null)
            )
        )

        val runs = converter.toRunHistory(doc, "test")

        assertEquals(Instant.fromEpochMilliseconds(2), runs[0].startTime)
        assertEquals(Instant.fromEpochMilliseconds(1_002), runs[0].endTime)
        assertEquals(Instant.fromEpochMilliseconds(5), runs[1].startTime)
        assertEquals(Instant.fromEpochMilliseconds(1_505), runs[1].endTime)
    }

    // =============================================
    // fromRunSession — PB comparison protection
    // =============================================

    private val testProfile = SplitProfile(
        id = "test",
        name = "Test",
        splits = listOf(
            Split("split_a", "Split A", "item"),
            Split("split_b", "Split B", "boss")
        )
    )

    private fun makeExistingDoc(pbTotalA: Long, pbTotalB: Long, bestA: Long, bestB: Long): LiveSplitDocument {
        return LiveSplitDocument(
            gameName = "Test", categoryName = "Any%", attemptCount = 1,
            segments = listOf(
                LiveSplitSegment("Split A", null,
                    LiveSplitTimeSpan(bestA, null),
                    listOf(LiveSplitComparisonSplit("Personal Best", pbTotalA, null)),
                    listOf(LiveSplitHistoryEntry(1, bestA, null))),
                LiveSplitSegment("Split B", null,
                    LiveSplitTimeSpan(bestB, null),
                    listOf(LiveSplitComparisonSplit("Personal Best", pbTotalB, null)),
                    listOf(LiveSplitHistoryEntry(1, bestB, null)))
            ),
            attemptHistory = listOf(
                LiveSplitAttempt(1, "01/01/2024 00:00:00", "01/01/2024 01:00:00", pbTotalB, null)
            )
        )
    }

    private fun makeRun(id: String, totalTime: Long, splitATotalTime: Long, splitASegment: Long,
                        splitBTotalTime: Long, splitBSegment: Long, complete: Boolean = true): RunSession {
        val baseTime = Instant.fromEpochMilliseconds(1000000000L)
        return RunSession(
            id = id, profileId = "test", startTime = baseTime,
            endTime = if (complete) Instant.fromEpochMilliseconds(1000000000L + totalTime) else null,
            completedSplits = listOf(
                CompletedSplit("split_a", SplitTime(splitATotalTime, splitASegment), baseTime),
                CompletedSplit("split_b", SplitTime(splitBTotalTime, splitBSegment), baseTime)
            ),
            totalTime = totalTime
        )
    }

    @Test
    fun `fromRunSession - slower run does not overwrite PB comparison`() {
        val existingDoc = makeExistingDoc(pbTotalA = 100000, pbTotalB = 200000, bestA = 90000, bestB = 95000)
        val slowerRun = makeRun("slow", 300000, 150000, 150000, 300000, 150000)

        val result = converter.fromRunSession(slowerRun, testProfile, existingDoc)

        // PB comparison should still be from the original PB, not the slower run
        val splitAPb = result.segments[0].splitTimes.find { it.comparisonName == "Personal Best" }
        val splitBPb = result.segments[1].splitTimes.find { it.comparisonName == "Personal Best" }
        assertEquals(100000L, splitAPb?.realTime, "Split A PB should remain 100s, not be overwritten to 150s")
        assertEquals(200000L, splitBPb?.realTime, "Split B PB should remain 200s, not be overwritten to 300s")
    }

    @Test
    fun `fromRunSession - faster run DOES overwrite PB comparison`() {
        val existingDoc = makeExistingDoc(pbTotalA = 100000, pbTotalB = 200000, bestA = 90000, bestB = 95000)
        val fasterRun = makeRun("fast", 150000, 70000, 70000, 150000, 80000)

        val result = converter.fromRunSession(fasterRun, testProfile, existingDoc)

        val splitAPb = result.segments[0].splitTimes.find { it.comparisonName == "Personal Best" }
        val splitBPb = result.segments[1].splitTimes.find { it.comparisonName == "Personal Best" }
        assertEquals(70000L, splitAPb?.realTime, "Split A PB should be updated to faster run's time")
        assertEquals(150000L, splitBPb?.realTime, "Split B PB should be updated to faster run's time")
    }

    @Test
    fun `fromRunSession - adds segment history for new run`() {
        val existingDoc = makeExistingDoc(pbTotalA = 100000, pbTotalB = 200000, bestA = 90000, bestB = 95000)
        val newRun = makeRun("new", 300000, 150000, 150000, 300000, 150000)

        val result = converter.fromRunSession(newRun, testProfile, existingDoc)

        // Should have 2 history entries: original + new run
        assertEquals(2, result.segments[0].segmentHistory.size, "Split A should have 2 history entries")
        assertEquals(2, result.segments[1].segmentHistory.size, "Split B should have 2 history entries")

        // New entry should have the new attempt ID
        val newEntry = result.segments[0].segmentHistory.last()
        assertEquals(2, newEntry.id)  // existing max is 1, so new is 2
        assertEquals(150000L, newEntry.realTime)
    }

    @Test
    fun `fromRunSession - updates best segment when current run has faster segment`() {
        val existingDoc = makeExistingDoc(pbTotalA = 100000, pbTotalB = 200000, bestA = 90000, bestB = 95000)
        // Slower overall run but with one faster segment
        val run = makeRun("mixed", 300000, 80000, 80000, 300000, 220000)

        val result = converter.fromRunSession(run, testProfile, existingDoc)

        // Split A best should be updated (80s < 90s existing)
        assertEquals(80000L, result.segments[0].bestSegmentTime?.realTime,
            "Split A best segment should be updated to 80s")
        // Split B best should NOT be updated (220s > 95s existing)
        assertEquals(95000L, result.segments[1].bestSegmentTime?.realTime,
            "Split B best segment should remain 95s")
    }

    @Test
    fun `fromRunSession - incomplete run does not overwrite PB comparison`() {
        val existingDoc = makeExistingDoc(pbTotalA = 100000, pbTotalB = 200000, bestA = 90000, bestB = 95000)
        // Incomplete run (even if splits look fast) should never overwrite PB
        val incompleteRun = makeRun("incomplete", 50000, 25000, 25000, 50000, 25000, complete = false)

        val result = converter.fromRunSession(incompleteRun, testProfile, existingDoc)

        val splitBPb = result.segments[1].splitTimes.find { it.comparisonName == "Personal Best" }
        assertEquals(200000L, splitBPb?.realTime, "PB should not be overwritten by incomplete run")
    }

    @Test
    fun `fromRunSession - incomplete run does not update best segment`() {
        val existingDoc = makeExistingDoc(pbTotalA = 100000, pbTotalB = 200000, bestA = 90000, bestB = 95000)
        val incompleteRun = makeRun("incomplete", 50000, 25000, 25000, 50000, 25000, complete = false)

        val result = converter.fromRunSession(incompleteRun, testProfile, existingDoc)

        assertEquals(90000L, result.segments[0].bestSegmentTime?.realTime)
        assertEquals(95000L, result.segments[1].bestSegmentTime?.realTime)
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
