package com.supermetroid.autosplits

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SplitProfilesTest {

    @Test
    fun `PRKD Any profile is registered and findable`() {
        assertTrue(SplitProfiles.ALL_PROFILES.contains(SplitProfiles.PRKD_ANY))
        assertSame(SplitProfiles.PRKD_ANY, SplitProfiles.getProfileById("prkd-any"))
    }

    @Test
    fun `PRKD Any profile uses detailed common route split order`() {
        val splitIds = SplitProfiles.PRKD_ANY.splits.map { it.id }

        assertEquals(
            listOf(
                "bomb",
                "first_power_bomb",
                "phantoon",
                "gravity_suit",
                "ice_beam",
                "lower_norfair_elevator",
                "ridley",
                "varia_suit",
                "botwoon",
                "draygon",
                "golden_four",
                "mother_brain_1",
                "mother_brain_2",
                "ship"
            ),
            splitIds
        )
    }

    @Test
    fun `PRKD Any lower norfair elevator split is room entry based`() {
        val split = SplitProfiles.PRKD_ANY.splits.first { it.id == "lower_norfair_elevator" }

        assertEquals("room_entry", split.type)
        assertEquals(0xAF3F, split.triggerRoomId)
    }

    @Test
    fun `simple PRKD boss order profile remains available`() {
        assertSame(SplitProfiles.SIMPLE_PRKD, SplitProfiles.getProfileById("simple-prkd"))
    }

    @Test
    fun `detailed PRKD Any profile appears immediately before simple PRKD`() {
        val profiles = SplitProfiles.ALL_PROFILES

        assertEquals(profiles.indexOf(SplitProfiles.SIMPLE_PRKD) - 1, profiles.indexOf(SplitProfiles.PRKD_ANY))
    }
}
