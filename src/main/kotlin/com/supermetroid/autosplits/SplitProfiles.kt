package com.supermetroid.autosplits

import com.supermetroid.model.Split
import com.supermetroid.model.SplitProfile

/**
 * Registry of all available split profiles
 * Each profile defines a different speedrun category with its own split order
 */
object SplitProfiles {

    // Profile ID constants
    const val ID_KPDR_ANY = "kpdr-any"
    const val ID_KPDR_LATE_ICE = "kpdr-late-ice"
    const val ID_PRKD_ANY = "prkd-any"
    const val ID_LOW_14_ICE = "low-14-ice"
    const val ID_HUNDRED_PERCENT = "hundred-percent"
    const val ID_CONTAINMENT_CHAMBER = "containment-chamber"
    const val ID_CONTAINMENT_CHAMBER_PUZZLES = "containment-chamber-puzzles"

    // Simple boss-order profile ID constants (all permutations of KPDR)
    const val ID_SIMPLE_KPDR = "simple-kpdr"
    const val ID_SIMPLE_KPRD = "simple-kprd"
    const val ID_SIMPLE_PKDR = "simple-pkdr"
    const val ID_SIMPLE_PKRD = "simple-pkrd"
    const val ID_SIMPLE_PRKD = "simple-prkd"
    const val ID_SIMPLE_PRDK = "simple-prdk"
    const val ID_SIMPLE_KRPD = "simple-krpd"
    const val ID_SIMPLE_KRDP = "simple-krdp"
    const val ID_SIMPLE_PDKR = "simple-pdkr"
    const val ID_SIMPLE_PDRK = "simple-pdrk"
    const val ID_SIMPLE_KDPR = "simple-kdpr"
    const val ID_SIMPLE_KDRP = "simple-kdrp"
    const val ID_SIMPLE_DKPR = "simple-dkpr"
    const val ID_SIMPLE_DKRP = "simple-dkrp"
    const val ID_SIMPLE_DPKR = "simple-dpkr"
    const val ID_SIMPLE_DPRK = "simple-dprk"
    const val ID_SIMPLE_RPKD = "simple-rpkd"
    const val ID_SIMPLE_RPDK = "simple-rpdk"
    const val ID_SIMPLE_DRKP = "simple-drkp"
    const val ID_SIMPLE_DRPK = "simple-drpk"
    const val ID_SIMPLE_RKPD = "simple-rkpd"
    const val ID_SIMPLE_RKDP = "simple-rkdp"
    const val ID_SIMPLE_RDKP = "simple-rdkp"
    const val ID_SIMPLE_RDPK = "simple-rdpk"

    /**
     * KPDR Any% - Standard route with early Ice Beam
     * Kraid → Phantoon → Draygon → Ridley
     * Ice Beam obtained before Phantoon
     */
    val KPDR_ANY = SplitProfile(
        id = ID_KPDR_ANY,
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
     * KPDR Any% Late Ice - KPDR Any% route with Ice Beam obtained after Phantoon
     * Kraid → Phantoon → Draygon → Ridley
     * Ice Beam obtained after Plasma Beam; Spazer is omitted for late-ice routing.
     */
    val KPDR_LATE_ICE = SplitProfile(
        id = ID_KPDR_LATE_ICE,
        name = "KPDR Any% Late Ice",
        splits = listOf(
            Split("ceres_station", "Ceres Station", "boss", "Escape from Ceres Station"),
            Split("morph_ball", "Morph Ball", "item", "Morph Ball acquired"),
            Split("first_missile", "First Missiles", "item", "First missile pack collected"),
            Split("bomb", "Bomb", "item", "Bomb acquired"),
            Split("first_super", "First Super", "item", "First super missile pack collected"),
            Split("charge_beam", "Charge Beam", "beam", "Charge Beam acquired"),
            Split("kraid", "Kraid", "boss", "Kraid defeated"),
            Split("varia_suit", "Varia Suit", "item", "Varia Suit acquired"),
            Split("hi_jump", "Hi-Jump Boots", "item", "Hi-Jump Boots acquired"),
            Split("speed_booster", "Speed Booster", "item", "Speed Booster acquired"),
            Split("wave_beam", "Wave Beam", "beam", "Wave Beam acquired"),
            Split("first_power_bomb", "First Power Bomb", "item", "First power bomb pack collected"),
            Split("phantoon", "Phantoon", "boss", "Phantoon defeated"),
            Split("gravity_suit", "Gravity Suit", "item", "Gravity Suit acquired"),
            Split("draygon", "Draygon", "boss", "Draygon defeated"),
            Split("space_jump", "Space Jump", "item", "Space Jump acquired"),
            Split("plasma_beam", "Plasma Beam", "beam", "Plasma Beam acquired"),
            Split("ice_beam", "Ice Beam", "beam", "Ice Beam acquired"),
            Split("ridley", "Ridley", "boss", "Ridley defeated"),
            Split("golden_four", "G4", "event", "Entered Tourian (all 4 bosses defeated)"),
            Split("mother_brain_1", "Mother Brain 1", "boss", "Mother Brain phase 1 completed"),
            Split("mother_brain_2", "Mother Brain 2", "boss", "Mother Brain phase 2 completed"),
            Split("ship", "Ship", "event", "Escaped to ship (game complete)")
        )
    )

    /**
     * PRKD Any% - Advanced route
     * Phantoon -> Ridley -> Kraid -> Draygon
     * Common public LSS layouts center on Bombs, Power Bombs, Gravity, Ice,
     * Lower Norfair Elevator, Ridley, Varia, Draygon, G4, and escape.
     */
    val PRKD_ANY = SplitProfile(
        id = ID_PRKD_ANY,
        name = "PRKD Any%",
        splits = listOf(
            Split("bomb", "Bombs", "item", "Bombs acquired"),
            Split("first_power_bomb", "Power Bombs", "item", "First power bomb pack collected"),
            Split("phantoon", "Phantoon", "boss", "Phantoon defeated"),
            Split("gravity_suit", "Gravity Suit", "item", "Gravity Suit acquired"),
            Split("ice_beam", "Ice Beam", "beam", "Ice Beam acquired"),
            Split("lower_norfair_elevator", "LN Elevator", "room_entry", "Entered Lower Norfair Elevator", triggerRoomId = 0xAF3F),
            Split("ridley", "Ridley", "boss", "Ridley defeated"),
            Split("varia_suit", "Varia Suit", "item", "Varia Suit acquired"),
            Split("botwoon", "Botwoon", "boss", "Botwoon defeated"),
            Split("draygon", "Draygon", "boss", "Draygon defeated"),
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
        id = ID_LOW_14_ICE,
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
        id = ID_HUNDRED_PERCENT,
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
        id = ID_CONTAINMENT_CHAMBER,
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
            Split("ship", "Ship", "event", "Boarded ship (game complete)")
        )
    )

    /**
     * Containment Chamber (Puzzle) - Splits by puzzle room completion.
     * Each split triggers when entering the room that follows solving a puzzle.
     * Room IDs to be filled in from gameplay logs (look for room transitions in poll logs).
     * Puzzles: 1-9 then A-O (23 total puzzles + ship escape)
     */
    val CONTAINMENT_CHAMBER_PUZZLES = SplitProfile(
        id = ID_CONTAINMENT_CHAMBER_PUZZLES,
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
            Split("ship", "Ship", "event", "Boarded ship (game complete)")
        )
    )

    // Reusable boss split definitions
    private val SPLIT_CERES = Split("ceres_station", "Ceres Station", "boss", "Escape from Ceres Station")
    private val SPLIT_KRAID = Split("kraid", "Kraid", "boss", "Kraid defeated")
    private val SPLIT_PHANTOON = Split("phantoon", "Phantoon", "boss", "Phantoon defeated")
    private val SPLIT_DRAYGON = Split("draygon", "Draygon", "boss", "Draygon defeated")
    private val SPLIT_RIDLEY = Split("ridley", "Ridley", "boss", "Ridley defeated")
    private val SPLIT_G4 = Split("golden_four", "G4", "event", "Entered Tourian (all 4 bosses defeated)")
    private val SPLIT_MB1 = Split("mother_brain_1", "Mother Brain 1", "boss", "Mother Brain phase 1 completed")
    private val SPLIT_MB2 = Split("mother_brain_2", "Mother Brain 2", "boss", "Mother Brain phase 2 completed")
    private val SPLIT_SHIP = Split("ship", "Ship", "event", "Escaped to ship (game complete)")

    private val BOSS_SPLITS = mapOf(
        'K' to SPLIT_KRAID,
        'P' to SPLIT_PHANTOON,
        'D' to SPLIT_DRAYGON,
        'R' to SPLIT_RIDLEY
    )

    private fun simpleBossProfile(id: String, name: String, bossOrder: String): SplitProfile {
        val bossSplits = bossOrder.map { BOSS_SPLITS[it]!! }
        return SplitProfile(
            id = id,
            name = name,
            splits = listOf(SPLIT_CERES) + bossSplits + listOf(SPLIT_G4, SPLIT_MB1, SPLIT_MB2, SPLIT_SHIP)
        )
    }

    // === Simple Boss-Order Profiles ===
    // Group 1: Standard Suit Paths
    val SIMPLE_KPDR = simpleBossProfile(ID_SIMPLE_KPDR, "KPDR", "KPDR")
    val SIMPLE_KPRD = simpleBossProfile(ID_SIMPLE_KPRD, "KPRD", "KPRD")
    val SIMPLE_PKDR = simpleBossProfile(ID_SIMPLE_PKDR, "PKDR", "PKDR")
    val SIMPLE_PKRD = simpleBossProfile(ID_SIMPLE_PKRD, "PKRD", "PKRD")
    // Group 2: The Speedrun Paths
    val SIMPLE_PRKD = simpleBossProfile(ID_SIMPLE_PRKD, "PRKD", "PRKD")
    val SIMPLE_PRDK = simpleBossProfile(ID_SIMPLE_PRDK, "PRDK", "PRDK")
    val SIMPLE_KRPD = simpleBossProfile(ID_SIMPLE_KRPD, "KRPD", "KRPD")
    val SIMPLE_KRDP = simpleBossProfile(ID_SIMPLE_KRDP, "KRDP", "KRDP")
    // Group 3: Late Phantoon Hybrids
    val SIMPLE_PDKR = simpleBossProfile(ID_SIMPLE_PDKR, "PDKR", "PDKR")
    val SIMPLE_PDRK = simpleBossProfile(ID_SIMPLE_PDRK, "PDRK", "PDRK")
    // Group 4: Suitless Maridia Paths
    val SIMPLE_KDPR = simpleBossProfile(ID_SIMPLE_KDPR, "KDPR", "KDPR")
    val SIMPLE_KDRP = simpleBossProfile(ID_SIMPLE_KDRP, "KDRP", "KDRP")
    val SIMPLE_DKPR = simpleBossProfile(ID_SIMPLE_DKPR, "DKPR", "DKPR")
    val SIMPLE_DKRP = simpleBossProfile(ID_SIMPLE_DKRP, "DKRP", "DKRP")
    // Group 5: Draygon First Blind Runs
    val SIMPLE_DPKR = simpleBossProfile(ID_SIMPLE_DPKR, "DPKR", "DPKR")
    val SIMPLE_DPRK = simpleBossProfile(ID_SIMPLE_DPRK, "DPRK", "DPRK")
    // Group 6: Suitless Hell Paths
    val SIMPLE_RPKD = simpleBossProfile(ID_SIMPLE_RPKD, "RPKD", "RPKD")
    val SIMPLE_RPDK = simpleBossProfile(ID_SIMPLE_RPDK, "RPDK", "RPDK")
    val SIMPLE_DRKP = simpleBossProfile(ID_SIMPLE_DRKP, "DRKP", "DRKP")
    val SIMPLE_DRPK = simpleBossProfile(ID_SIMPLE_DRPK, "DRPK", "DRPK")
    // Group 7: True Hell Paths
    val SIMPLE_RKPD = simpleBossProfile(ID_SIMPLE_RKPD, "RKPD", "RKPD")
    val SIMPLE_RKDP = simpleBossProfile(ID_SIMPLE_RKDP, "RKDP", "RKDP")
    val SIMPLE_RDKP = simpleBossProfile(ID_SIMPLE_RDKP, "RDKP", "RDKP")
    val SIMPLE_RDPK = simpleBossProfile(ID_SIMPLE_RDPK, "RDPK (RBO)", "RDPK")

    /**
     * All available profiles
     */
    val ALL_PROFILES = listOf(
        // Detailed category profiles
        KPDR_ANY,
        KPDR_LATE_ICE,
        LOW_PERCENT_ICE,
        HUNDRED_PERCENT,
        CONTAINMENT_CHAMBER,
        CONTAINMENT_CHAMBER_PUZZLES,
        // Simple boss-order profiles (ordered by difficulty)
        SIMPLE_KPDR,
        SIMPLE_KPRD,
        SIMPLE_PKDR,
        SIMPLE_PKRD,
        PRKD_ANY,
        SIMPLE_PRKD,
        SIMPLE_PRDK,
        SIMPLE_KRPD,
        SIMPLE_KRDP,
        SIMPLE_PDKR,
        SIMPLE_PDRK,
        SIMPLE_KDPR,
        SIMPLE_KDRP,
        SIMPLE_DKPR,
        SIMPLE_DKRP,
        SIMPLE_DPKR,
        SIMPLE_DPRK,
        SIMPLE_RPKD,
        SIMPLE_RPDK,
        SIMPLE_DRKP,
        SIMPLE_DRPK,
        SIMPLE_RKPD,
        SIMPLE_RKDP,
        SIMPLE_RDKP,
        SIMPLE_RDPK
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
