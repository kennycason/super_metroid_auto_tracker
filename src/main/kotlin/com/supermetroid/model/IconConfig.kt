package com.supermetroid.model

import kotlinx.serialization.Serializable

/**
 * Configuration for individual icons in the tracker
 */
@Serializable
data class IconItem(
    val id: String,
    val name: String,
    val category: String, // "collectible", "item", "beam", "boss"
    val enabled: Boolean = true,
    val order: Int = 0
)

/**
 * Complete icon configuration for the tracker
 */
@Serializable
data class IconConfig(
    val icons: List<IconItem> = emptyList(),
    val version: Int = 1
)

/**
 * Default icon configuration with all items in standard order
 */
object DefaultIconConfig {
    fun create(): IconConfig {
        val defaultIcons = listOf(
            // Collectibles (ammo/energy)
            IconItem("health", "Energy Tanks", "collectible", true, 0),
            IconItem("reserve_tank", "Reserve Tank", "collectible", true, 1),
            IconItem("missiles", "Missiles", "collectible", true, 2),
            IconItem("supers", "Super Missiles", "collectible", true, 3),
            IconItem("powerbombs", "Power Bombs", "collectible", true, 4),
            
            // Major Items
            IconItem("morph_ball", "Morph Ball", "item", true, 5),
            IconItem("bombs", "Bombs", "item", true, 6),
            IconItem("charge", "Charge Beam", "beam", true, 7),
            IconItem("spazer", "Spazer", "beam", true, 8),
            IconItem("varia", "Varia Suit", "item", true, 9),
            IconItem("hijump", "Hi-Jump", "item", true, 10),
            IconItem("speed_booster", "Speed Booster", "item", true, 11),
            IconItem("wave", "Wave Beam", "beam", true, 12),
            IconItem("ice", "Ice Beam", "beam", true, 13),
            IconItem("gravity", "Gravity Suit", "item", true, 14),
            IconItem("space", "Space Jump", "item", true, 15),
            IconItem("spring", "Spring Ball", "item", true, 16),
            IconItem("screw", "Screw Attack", "item", true, 17),
            IconItem("plasma", "Plasma Beam", "beam", true, 18),
            IconItem("grapple", "Grappling Beam", "item", true, 19),
            IconItem("xray", "X-Ray Scope", "item", true, 20),
            
            // Bosses
            IconItem("ceres_station", "Ceres Station", "boss", true, 21),
            IconItem("bomb_torizo", "Bomb Torizo", "boss", true, 22),
            IconItem("spore_spawn", "Spore Spawn", "boss", true, 23),
            IconItem("kraid", "Kraid", "boss", true, 24),
            IconItem("crocomire", "Crocomire", "boss", true, 25),
            IconItem("phantoon", "Phantoon", "boss", true, 26),
            IconItem("botwoon", "Botwoon", "boss", true, 27),
            IconItem("draygon", "Draygon", "boss", true, 28),
            IconItem("gold_torizo", "Gold Torizo", "boss", true, 29),
            IconItem("ridley", "Ridley", "boss", true, 30),
            IconItem("golden_four", "G4", "boss", true, 31),
            IconItem("mother_brain_1", "Mother Brain 1", "boss", true, 32),
            IconItem("mother_brain_2", "Mother Brain 2", "boss", true, 33),
            IconItem("ship", "Ship", "boss", true, 34)
        )
        
        return IconConfig(icons = defaultIcons)
    }
}
