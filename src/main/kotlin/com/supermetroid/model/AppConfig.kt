package com.supermetroid.model

import kotlinx.serialization.Serializable

/**
 * Application configuration that persists between app launches
 * Stored in ~/.smtracker/smtracker.json
 */
@Serializable
data class AppConfig(
    val theme: String = "RETRO_GREEN",  // Theme enum name
    val retroarchPort: Int = 55355,        // Future: RetroArch port config  
    val pollIntervalMs: Long = 500,        // Future: Polling interval config
    val windowWidth: Int = 800,            // Future: Window size persistence
    val windowHeight: Int = 600,           // Future: Window size persistence
    val autoSplitsEnabled: Boolean = false // Future: Auto-splits default state
)
