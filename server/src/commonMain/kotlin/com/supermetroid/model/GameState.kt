package com.supermetroid.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class GameState(
    val health: Int = 0,
    @SerialName("max_health") val maxHealth: Int = 99,
    val missiles: Int = 0,
    @SerialName("max_missiles") val maxMissiles: Int = 0,
    val supers: Int = 0,
    @SerialName("max_supers") val maxSupers: Int = 0,
    @SerialName("power_bombs") val powerBombs: Int = 0,
    @SerialName("max_power_bombs") val maxPowerBombs: Int = 0,
    @SerialName("reserve_energy") val reserveEnergy: Int = 0,
    @SerialName("max_reserve_energy") val maxReserveEnergy: Int = 0,
    @SerialName("room_id") val roomId: Int = 0,
    @SerialName("area_id") val areaId: Int = 0,
    @SerialName("area_name") val areaName: String = "Unknown",
    @SerialName("game_state") val gameState: Int = 0,
    @SerialName("player_x") val playerX: Int = 0,
    @SerialName("player_y") val playerY: Int = 0,
    val items: Map<String, Boolean> = emptyMap(),
    val beams: Map<String, Boolean> = emptyMap(),
    val bosses: Map<String, Boolean> = emptyMap()
)

@Serializable
data class Items(
    val morph: Boolean = false,
    val bombs: Boolean = false,
    val varia: Boolean = false,
    val gravity: Boolean = false,
    val hijump: Boolean = false,
    val speed: Boolean = false,
    val spacejump: Boolean = false,
    val screw: Boolean = false,
    val spring: Boolean = false,
    val grapple: Boolean = false,
    val xray: Boolean = false
)

@Serializable
data class Beams(
    val charge: Boolean = false,
    val ice: Boolean = false,
    val wave: Boolean = false,
    val spazer: Boolean = false,
    val plasma: Boolean = false,
    val hyper: Boolean = false
)

@Serializable
data class Bosses(
    val bombTorizo: Boolean = false,
    val kraid: Boolean = false,
    val sporeSpawn: Boolean = false,
    val crocomire: Boolean = false,
    val botwoon: Boolean = false,
    val phantoon: Boolean = false,
    val draygon: Boolean = false,
    val ridley: Boolean = false,
    val goldenTorizo: Boolean = false,
    val motherBrain1: Boolean = false,
    val motherBrain2: Boolean = false
)

@Serializable
data class ConnectionInfo(
    val connected: Boolean = false,
    @SerialName("game_loaded") val gameLoaded: Boolean = false,
    @SerialName("retroarch_version") val retroarchVersion: String? = null,
    @SerialName("game_info") val gameInfo: String? = null
)

@Serializable
data class ServerStatus(
    val connected: Boolean = false,
    @SerialName("game_loaded") val gameLoaded: Boolean = false,
    @SerialName("retroarch_version") val retroarchVersion: String? = null,
    @SerialName("game_info") val gameInfo: String? = null,
    val stats: GameState = GameState(),
    @SerialName("last_update") val lastUpdate: Long = 0,
    @SerialName("poll_count") val pollCount: Int = 0,
    @SerialName("error_count") val errorCount: Int = 0
) 