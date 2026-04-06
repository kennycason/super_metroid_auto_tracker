package com.supermetroid.autosplits

import com.supermetroid.model.Split
import com.supermetroid.model.SplitProfile

/**
 * Registry of all available split profiles
 * Each profile defines a different speedrun category with its own split order
 */
object SplitProfiles {
    
    /**
     * KPDR Any% - Standard route with early Ice Beam
     * Kraid → Phantoon → Draygon → Ridley
     * Ice Beam obtained before Phantoon
     */
    val KPDR_ANY = SplitProfile(
        id = "kpdr-any",
        name = "KPDR Any%",
        splits = listOf(
            Split("ceres_station", "Ceres Station", "boss", "Escape from Ceres Station"),
            Split("morph_ball", "Morph Ball", "item", "Morph Ball acquired"),
            Split("first_missile", "First Missiles", "item", "First missile pack collected"),
            Split("bomb", "Bomb", "item", "Bomb acquired"),
            Split("first_super", "First Super", "item", "First super missile pack collected"),
            Split("charge_beam", "Charge Beam", "beam", "Charge Beam acquired"),
            Split("spazer", "Spazer", "item", "Spazer acquired"),
            Split("kraid", "Kraid", "boss", "Kraid defeated"),
            Split("varia_suit", "Varia Suit", "item", "Varia Suit acquired"),
            Split("hi_jump", "Hi-Jump Boots", "item", "Hi-Jump Boots acquired"),
            Split("speed_booster", "Speed Booster", "item", "Speed Booster acquired"),
            Split("wave_beam", "Wave Beam", "beam", "Wave Beam acquired"),
            Split("ice_beam", "Ice Beam", "beam", "Ice Beam acquired"),
            Split("first_power_bomb", "First Power Bomb", "item", "First power bomb pack collected"),
            Split("phantoon", "Phantoon", "boss", "Phantoon defeated"),
            Split("gravity_suit", "Gravity Suit", "item", "Gravity Suit acquired"),
            Split("draygon", "Draygon", "boss", "Draygon defeated"),
            Split("space_jump", "Space Jump", "item", "Space Jump acquired"),
            Split("plasma_beam", "Plasma Beam", "beam", "Plasma Beam acquired"),
            Split("ridley", "Ridley", "boss", "Ridley defeated"),
            Split("golden_four", "G4", "event", "Entered Tourian (all 4 bosses defeated)"),
            Split("mother_brain_1", "Mother Brain 1", "boss", "Mother Brain phase 1 completed"),
            Split("mother_brain_2", "Mother Brain 2", "boss", "Mother Brain phase 2 completed"),
            Split("ship", "Ship", "event", "Escaped to ship (game complete)")
        )
    )
    
    /**
     * KPDR Late Ice - Route with Ice Beam obtained after Phantoon
     * Kraid → Phantoon → Draygon → Ridley
     * Ice Beam obtained after Gravity Suit (common for certain strats)
     */
    val KPDR_LATE_ICE = SplitProfile(
        id = "kpdr-late-ice",
        name = "KPDR Late Ice",
        splits = listOf(
            Split("ceres_station", "Ceres Station", "boss", "Escape from Ceres Station"),
            Split("morph_ball", "Morph Ball", "item", "Morph Ball acquired"),
            Split("first_missile", "First Missiles", "item", "First missile pack collected"),
            Split("bomb", "Bomb", "item", "Bomb acquired"),
            Split("first_super", "First Super", "item", "First super missile pack collected"),
            Split("charge_beam", "Charge Beam", "beam", "Charge Beam acquired"),
            Split("spazer", "Spazer", "item", "Spazer acquired"),
            Split("kraid", "Kraid", "boss", "Kraid defeated"),
            Split("varia_suit", "Varia Suit", "item", "Varia Suit acquired"),
            Split("hi_jump", "Hi-Jump Boots", "item", "Hi-Jump Boots acquired"),
            Split("speed_booster", "Speed Booster", "item", "Speed Booster acquired"),
            Split("wave_beam", "Wave Beam", "beam", "Wave Beam acquired"),
            Split("first_power_bomb", "First Power Bomb", "item", "First power bomb pack collected"),
            Split("phantoon", "Phantoon", "boss", "Phantoon defeated"),
            Split("gravity_suit", "Gravity Suit", "item", "Gravity Suit acquired"),
            Split("ice_beam", "Ice Beam", "beam", "Ice Beam acquired"),  // Moved after Gravity Suit
            Split("draygon", "Draygon", "boss", "Draygon defeated"),
            Split("space_jump", "Space Jump", "item", "Space Jump acquired"),
            Split("plasma_beam", "Plasma Beam", "beam", "Plasma Beam acquired"),
            Split("ridley", "Ridley", "boss", "Ridley defeated"),
            Split("golden_four", "G4", "event", "Entered Tourian (all 4 bosses defeated)"),
            Split("mother_brain_1", "Mother Brain 1", "boss", "Mother Brain phase 1 completed"),
            Split("mother_brain_2", "Mother Brain 2", "boss", "Mother Brain phase 2 completed"),
            Split("ship", "Ship", "event", "Escaped to ship (game complete)")
        )
    )
    
    /**
     * Low% 14% Ice - Minimal item collection route
     * Only 14% items: Morph, Bombs, Missiles, Supers, Charge, Power Bombs, Gravity, Ice Beam
     * No Varia, Hi-Jump, Speed Booster, Space Jump, Spazer, Wave, Plasma
     */
    val LOW_PERCENT_ICE = SplitProfile(
        id = "low-14-ice",
        name = "Low% Ice (14%)",
        splits = listOf(
            Split("ceres_station", "Ceres Station", "boss", "Escape from Ceres Station"),
            Split("morph_ball", "Morph Ball", "item", "Morph Ball acquired"),
            Split("first_missile", "First Missiles", "item", "First missile pack collected"),
            Split("bomb", "Bomb", "item", "Bomb acquired"),
            Split("first_super", "First Super", "item", "First super missile pack collected"),
            Split("charge_beam", "Charge Beam", "beam", "Charge Beam acquired"),
            Split("kraid", "Kraid", "boss", "Kraid defeated"),
            Split("varia_suit", "Varia Suit", "item", "Varia Suit acquired"),
            Split("first_power_bomb", "First Power Bomb", "item", "First power bomb pack collected"),
            Split("phantoon", "Phantoon", "boss", "Phantoon defeated"),
            Split("gravity_suit", "Gravity Suit", "item", "Gravity Suit acquired"),
            Split("ice_beam", "Ice Beam", "beam", "Ice Beam acquired"),
            Split("ridley", "Ridley", "boss", "Ridley defeated"),
            Split("draygon", "Draygon", "boss", "Draygon defeated"),
            Split("golden_four", "G4", "event", "Entered Tourian (all 4 bosses defeated)"),
            Split("mother_brain_1", "Mother Brain 1", "boss", "Mother Brain phase 1 completed"),
            Split("mother_brain_2", "Mother Brain 2", "boss", "Mother Brain phase 2 completed"),
            Split("ship", "Ship", "event", "Escaped to ship (game complete)")
        )
    )
    
    /**
     * 100% - Full item collection route
     * All major items acquired, typically ~1:20-1:30
     * Splits derived from standard LiveSplit 100% layout (12 milestones)
     */
    val HUNDRED_PERCENT = SplitProfile(
        id = "hundred-percent",
        name = "100%",
        splits = listOf(
            Split("bomb", "Bomb", "item", "Bombs acquired"),
            Split("varia_suit", "Varia", "item", "Varia Suit acquired"),
            Split("grapple_beam", "Grapple", "item", "Grapple Beam acquired"),
            Split("phantoon", "Phantoon", "boss", "Phantoon defeated"),
            Split("gravity_suit", "Gravity", "item", "Gravity Suit acquired"),
            Split("draygon", "Draygon", "boss", "Draygon defeated (Water section)"),
            Split("space_jump", "Space Jump", "item", "Space Jump acquired"),
            Split("plasma_beam", "Plasma", "beam", "Plasma Beam acquired"),
            Split("screw_attack", "Screw Attack", "item", "Screw Attack acquired"),
            Split("reserve_tank", "Reserve", "item", "All Reserve Tanks collected"),
            Split("golden_four", "G4", "event", "Entered Tourian (all 4 bosses defeated)"),
            Split("ship", "Done", "event", "Escaped to ship (game complete)")
        )
    )

    /**
     * Containment Chamber - Popular Super Metroid puzzle ROM hack
     * Custom split points based on item acquisition and room entries.
     * NOTE: Room-entry splits (missile, super_missiles) use triggerRoomId for
     * ROM hack-specific rooms that don't correspond to vanilla item pickups.
     */
    val CONTAINMENT_CHAMBER = SplitProfile(
        id = "containment-chamber",
        name = "Containment Chamber",
        splits = listOf(
            Split("morph_ball", "Morph Ball", "item", "Morph Ball acquired"),
            Split("charge_beam", "Charge Beam", "beam", "Charge Beam acquired"),
            Split("bomb", "Bombs", "item", "Bombs acquired"),
            Split("missile", "Missiles", "room_entry", "Entered Terminator Room (missiles)", triggerRoomId = 0x990D),
            Split("super_missiles", "Super Missiles", "room_entry", "Entered Milly Mays' Room (supers)", triggerRoomId = 0xA1D8),
            Split("speed_booster", "Speed Booster", "item", "Speed Booster acquired"),
            Split("hi_jump", "Hi-Jump Boots", "item", "Hi-Jump Boots acquired"),
            Split("grapple_beam", "Grapple Beam", "item", "Grapple Beam acquired"),
            Split("ice_beam", "Ice Beam", "beam", "Ice Beam acquired"),
            Split("gravity_suit", "Gravity Suit", "item", "Gravity Suit acquired"),
            Split("spring_ball", "Spring Ball", "item", "Spring Ball acquired"),
            Split("ship", "Ship", "room_entry", "Reached ship (game complete)", triggerRoomId = 0x91F8)
        )
    )

    /**
     * Containment Chamber (Puzzle) - Splits by puzzle room completion.
     * Each split triggers when entering the room that follows solving a puzzle.
     * Room IDs to be filled in from gameplay logs (look for room transitions in poll logs).
     * Puzzles: 1-9 then A-O (23 total puzzles + ship escape)
     */
    val CONTAINMENT_CHAMBER_PUZZLES = SplitProfile(
        id = "containment-chamber-puzzles",
        name = "Containment Chamber (Puzzles)",
        splits = listOf(
            // Puzzles 1-9
            Split("puzzle_1", "Puzzle 1", "room_entry", "Completed Puzzle 1", triggerRoomId = 0x92FD),
            Split("puzzle_2", "Puzzle 2", "room_entry", "Completed Puzzle 2", triggerRoomId = 0x9A90),
            Split("puzzle_3", "Puzzle 3", "room_entry", "Completed Puzzle 3 (Terminator Room)", triggerRoomId = 0x990D),
            Split("puzzle_4", "Puzzle 4", "room_entry", "Completed Puzzle 4", triggerRoomId = 0x92FD, requiredItems = listOf("bombs")),
            Split("puzzle_5", "Puzzle 5", "room_entry", "Completed Puzzle 5", triggerRoomId = 0x99BD),
            Split("puzzle_6", "Puzzle 6", "room_entry", "Completed Puzzle 6", triggerRoomId = 0x9E52),
            Split("puzzle_7", "Puzzle 7", "room_entry", "Completed Puzzle 7", triggerRoomId = 0x9AD9, requiredItems = listOf("speed_booster")),
            Split("puzzle_8", "Puzzle 8", "room_entry", "Completed Puzzle 8 (Early Supers)", triggerRoomId = 0x9BC8),
            Split("puzzle_9", "Puzzle 9", "room_entry", "Completed Puzzle 9", triggerRoomId = 0xCA08),
            // Puzzles A-O
            Split("puzzle_a", "Puzzle A", "room_entry", "Completed Puzzle A", triggerRoomId = 0xD08A),
            Split("puzzle_b", "Puzzle B", "room_entry", "Completed Puzzle B", triggerRoomId = 0xD21C),
            Split("puzzle_c", "Puzzle C", "room_entry", "Completed Puzzle C", triggerRoomId = 0xD646),
            Split("puzzle_d", "Puzzle D", "room_entry", "Completed Puzzle D", triggerRoomId = 0xD1A3),
            Split("puzzle_e", "Puzzle E", "room_entry", "Completed Puzzle E", triggerRoomId = 0xD95E),
            Split("puzzle_f", "Puzzle F", "room_entry", "Completed Puzzle F", triggerRoomId = 0xD617),
            Split("puzzle_g", "Puzzle G", "room_entry", "Completed Puzzle G", triggerRoomId = 0xD408),
            Split("puzzle_h", "Puzzle H", "room_entry", "Completed Puzzle H", triggerRoomId = 0xB139),
            Split("puzzle_i", "Puzzle I", "room_entry", "Completed Puzzle I", triggerRoomId = 0xAF3F),
            Split("puzzle_j", "Puzzle J", "room_entry", "Completed Puzzle J", triggerRoomId = 0xDB31),
            Split("puzzle_k", "Puzzle K", "room_entry", "Completed Puzzle K", triggerRoomId = 0xDB7D),
            Split("puzzle_l", "Puzzle L", "room_entry", "Completed Puzzle L", triggerRoomId = 0xDBCD),
            Split("puzzle_m", "Puzzle M", "room_entry", "Completed Puzzle M", triggerRoomId = 0xDDC4),
            Split("puzzle_n", "Puzzle N", "room_entry", "Completed Puzzle N", triggerRoomId = 0xDEDE),
            Split("puzzle_o", "Puzzle O", "room_entry", "Completed Puzzle O", triggerRoomId = 0x96BA),
            Split("ship", "Ship", "room_entry", "Reached ship (game complete)", triggerRoomId = 0x91F8)
        )
    )

    /**
     * All available profiles
     */
    val ALL_PROFILES = listOf(
        KPDR_ANY,
        KPDR_LATE_ICE,
        LOW_PERCENT_ICE,
        HUNDRED_PERCENT,
        CONTAINMENT_CHAMBER,
        CONTAINMENT_CHAMBER_PUZZLES
    )
    
    /**
     * Map of profile ID to profile for quick lookup
     */
    val BY_ID: Map<String, SplitProfile> = ALL_PROFILES.associateBy { it.id }
    
    /**
     * Get a profile by ID, returns KPDR_ANY as default if not found
     */
    fun getProfileById(id: String): SplitProfile = BY_ID[id] ?: KPDR_ANY
    
    /**
     * Default profile for new runs
     */
    val DEFAULT = KPDR_ANY
}
