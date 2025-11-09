package com.supermetroid.model

import kotlinx.serialization.Serializable

/**
 * Complete game state - modern Kotlin with camelCase
 */
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
    val motherBrainHp: Int = 0,  // Added for ASL HP transitions
    val eventFlags: Int = 0,     // Added for ship detection
    val tourianBosses: Int = 0,  // Added for ship detection  
    val shipAi: Int = 0,         // Added for ship detection
    val deathCount: Int = 0,     // Map Rando death counter from SRAM ($701E16)
    val reloadCount: Int = 0,    // Map Rando reload counter from SRAM ($701E18)
    val resetCount: Int = 0,     // Map Rando reset counter from SRAM ($701E1C)
    val items: Items = Items(),
    val beams: Beams = Beams(),
    val bosses: Bosses = Bosses()
)

@Serializable
data class Items(
    val morph: Boolean = false,
    val bombs: Boolean = false,
    val varia: Boolean = false,
    val gravity: Boolean = false,
    val hiJump: Boolean = false,
    val speed: Boolean = false,
    val spaceJump: Boolean = false,
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
    val ceresStation: Boolean = false,
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
    val motherBrain2: Boolean = false,
    val motherBrain: Boolean = false,  // Final defeat flag
    val samusShip: Boolean = false     // End-game completion
)

@Serializable
data class ConnectionInfo(
    val connected: Boolean = false,
    val gameLoaded: Boolean = false,
    val retroarchVersion: String? = null,
    val gameInfo: String? = null
)

/**
 * Complete application state for the tracker
 */
@Serializable  
data class TrackerState(
    val connection: ConnectionInfo = ConnectionInfo(),
    val gameState: GameState = GameState(),
    val lastUpdate: Long = 0,
    val pollCount: Int = 0,
    val errorCount: Int = 0
) 