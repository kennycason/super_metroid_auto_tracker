package com.supermetroid.model

import kotlinx.serialization.Serializable

@Serializable
data class GameState(
    val health: Int = 0,
    val maxHealth: Int = 99,
    val missiles: Int = 0,
    val maxMissiles: Int = 0,
    val supers: Int = 0,
    val maxSupers: Int = 0,
    val powerBombs: Int = 0,
    val maxPowerBombs: Int = 0,
    val reserveEnergy: Int = 0,
    val maxReserveEnergy: Int = 0,
    val roomId: Int = 0,
    val areaId: Int = 0,
    val areaName: String = "Unknown",
    val gameState: Int = 0,
    val playerX: Int = 0,
    val playerY: Int = 0,
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
    val gameLoaded: Boolean = false,
    val retroarchVersion: String? = null,
    val gameInfo: String? = null
)

@Serializable
data class ServerStatus(
    val connected: Boolean = false,
    val gameLoaded: Boolean = false,
    val retroarchVersion: String? = null,
    val gameInfo: String? = null,
    val stats: GameState = GameState(),
    val lastUpdate: Long = 0,
    val pollCount: Int = 0,
    val errorCount: Int = 0
) 