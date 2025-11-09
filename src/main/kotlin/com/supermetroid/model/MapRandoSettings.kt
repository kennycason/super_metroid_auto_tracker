package com.supermetroid.model

import kotlinx.serialization.Serializable

/**
 * Map Randomizer seed settings fetched from maprando.com API
 * Based on the seed metadata structure from the Map Rando API
 */
@Serializable
data class MapRandoSettings(
    val seedName: String = "",
    val objectives: String = "OFF", // "Bosses", "Minibosses", "Chozos", "Pirates", "Metroids", etc.
    val difficulty: String = "OFF", // Skill assumption: "Basic", "Casual", "Intermediate", etc.
    val itemProgression: String = "OFF", // "Normal", "Uniform", "Desolate", etc.
    val qualityOfLife: String = "OFF", // "OFF", "Low", "Default", "Max"
    val mapLayout: String = "OFF", // "Vanilla", "Standard", "Small", "Large", etc.
    val doorsMode: String = "OFF", // "Blue", "Ammo", "Beam", etc.
    val startLocation: String = "OFF", // "Ship", "Random", specific room name
    val saveAnimals: String = "OFF", // "Yes", "No", "Random"
    val deathCount: Int = 0, // Read from SRAM $701E16
    val version: String = "" // Map Rando version (e.g., "119")
)

/**
 * Response from maprando.com API
 * Example: GET https://maprando.com/api/seed/{seed_name}
 */
@Serializable
data class MapRandoApiResponse(
    val seed_name: String,
    val version: String? = null,
    val settings: ApiSettings? = null
)

@Serializable
data class ApiSettings(
    val skill_assumptions: String? = null,
    val item_progression: String? = null,
    val quality_of_life: String? = null,
    val objectives: String? = null,
    val map_layout: String? = null,
    val doors_mode: String? = null,
    val start_location: String? = null,
    val save_animals: String? = null
)

