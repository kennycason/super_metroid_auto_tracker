package com.supermetroid.model

import kotlinx.serialization.Serializable

/**
 * Icon size options for the tracker UI
 */
enum class IconSize(val size: Int, val displayName: String) {
    SMALL(16, "16x16"),
    MEDIUM_SMALL(24, "24x24"),
    MEDIUM(32, "32x32"),
    LARGE(48, "48x48"),
    EXTRA_LARGE(64, "64x64");
    
    companion object {
        fun fromSize(size: Int): IconSize {
            return values().find { it.size == size } ?: MEDIUM
        }
    }
}

/**
 * Application configuration that persists between app launches
 * Stored in ~/.smtracker/smtracker.json
 */
@Serializable
data class AppConfig(
    val theme: String = "DARK_BLACK",  // Theme enum name
    val iconSize: Int = 32,             // Icon size in pixels for icons view (default to 32x32)
    val splitIconSize: Int = 32,        // Icon size in pixels for splits view (default to 32x32)
    val showRoomName: Boolean = true,   // Show room name in status display
    val retroarchPort: Int = 55355,        // Future: RetroArch port config  
    val pollIntervalMs: Long = 500,        // Future: Polling interval config
    val windowWidth: Int = 800,            // Future: Window size persistence
    val windowHeight: Int = 600,           // Future: Window size persistence
    val autoSplitsEnabled: Boolean = false // Future: Auto-splits default state
)
