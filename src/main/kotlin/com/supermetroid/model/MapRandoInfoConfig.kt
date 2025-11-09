package com.supermetroid.model

import kotlinx.serialization.Serializable

/**
 * Configuration for an individual Map Rando info field
 */
@Serializable
data class MapRandoInfoItem(
    val id: String,           // e.g., "objectives", "difficulty", "deaths"
    val displayName: String,  // e.g., "Objectives", "Difficulty", "Deaths"
    val visible: Boolean = true,
    val order: Int
)

/**
 * Complete configuration for Map Rando info panel
 */
@Serializable
data class MapRandoInfoConfig(
    val items: List<MapRandoInfoItem>
)

/**
 * Default Map Rando info configuration
 */
object DefaultMapRandoInfoConfig {
    fun create(): MapRandoInfoConfig {
        val defaultItems = listOf(
            MapRandoInfoItem("objectives", "Objectives", true, 1),
            MapRandoInfoItem("difficulty", "Difficulty", true, 2),
            MapRandoInfoItem("itemProgression", "Item Progression", true, 3),
            MapRandoInfoItem("qualityOfLife", "Quality of Life", true, 4),
            MapRandoInfoItem("mapLayout", "Map Layout", true, 5),
            MapRandoInfoItem("deaths", "Deaths", true, 6),
            MapRandoInfoItem("resets", "Resets", true, 7)
        )
        
        return MapRandoInfoConfig(items = defaultItems)
    }
}

