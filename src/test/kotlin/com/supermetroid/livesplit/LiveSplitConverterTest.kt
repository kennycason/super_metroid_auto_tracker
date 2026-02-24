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
}
