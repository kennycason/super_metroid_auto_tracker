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
     * All available profiles
     */
    val ALL_PROFILES = listOf(
        KPDR_ANY,
        KPDR_LATE_ICE,
        LOW_PERCENT_ICE
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
