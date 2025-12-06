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
    EXTRA_LARGE(64, "64x64"),
    HUGE(128, "128x128"),
    MASSIVE(256, "256x256");
    
    companion object {
        fun fromSize(size: Int): IconSize {
            return values().find { it.size == size } ?: MEDIUM
        }
    }
}

/**
 * Display mode options for splits
 */
enum class SplitDisplayMode(val displayName: String, val showIcons: Boolean, val showNames: Boolean) {
    ICON_ONLY("Icon Only", showIcons = true, showNames = false),
    NAME_ONLY("Split Name Only", showIcons = false, showNames = true),
    BOTH("Both Icon & Split Name", showIcons = true, showNames = true);
    
    companion object {
        fun fromBooleans(showIcons: Boolean, showNames: Boolean): SplitDisplayMode {
            return when {
                showIcons && showNames -> BOTH
                showIcons && !showNames -> ICON_ONLY
                !showIcons && showNames -> NAME_ONLY
                else -> BOTH // Default to both if neither is shown
            }
        }
    }
}

/**
 * Icon view modes for the status icons area
 */
enum class IconViewMode(val displayName: String) {
    DEFAULT("Default"),
    MAP_RANDO("Map Rando");
}

/**
 * How to render numbers on ammo/energy icons
 */
enum class AmmoNumberMode(val displayName: String) {
    AMOUNT("Amount (current/max)"),
    COUNT("Count (# found)");
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
    val showSplitIcons: Boolean = true, // Show icons in splits list
    val showSplitNames: Boolean = true, // Show split names in splits list
    val showRoomName: Boolean = true,   // Show room name in status display
    val retroarchPort: Int = 55355,        // Future: RetroArch port config  
    val pollIntervalMs: Long = 500,        // Future: Polling interval config
    val windowWidth: Int = 416,            // Window width persistence
    val windowHeight: Int = 1100,          // Window height persistence
    val autoSplitsEnabled: Boolean = false, // Future: Auto-splits default state
    // New icon layout + ammo number modes (string names to be forward/backward compatible)
    val iconViewMode: String = IconViewMode.DEFAULT.name,
    val ammoNumberMode: String = AmmoNumberMode.AMOUNT.name,
    // UI visibility state persistence
    val showSplits: Boolean = true,
    val showIcons: Boolean = true,
    val showTimer: Boolean = true,
    val showSettings: Boolean = false,
    val showGameGenie: Boolean = false,
    val showMapRandoInfo: Boolean = false,
    // Sound effects settings
    val soundEnabled: Boolean = false,
    val volume: Float = 1.0f,
    // Game Genie settings
    val gameGenieEnabled: Boolean = false,
    // Splits display settings
    val showSegmentDeltas: Boolean = false,
    val showBestPossibleColumn: Boolean = true,  // Show Best Possible (theoretical best from completed runs) column
    val showBestPossibleDelta: Boolean = true,   // Show Best Possible Delta (segment-by-segment comparison vs best) column
    val showBestColumn: Boolean = true,  // Show Personal Best run column
    val showAverageColumn: Boolean = false,      // Show Average Time (mean of all completed runs) column
    val showAverageDelta: Boolean = false,       // Show Average Delta (segment vs average) column
    // Timer persistence
    val savedTimerMs: Long? = null,  // Saved timer value in milliseconds (null = no saved timer)
    val savedTimerProfileId: String? = null,  // Profile ID for the saved timer
    // Map Rando info panel settings
    val mapRandoInfoFontSize: String? = null  // Font size for Map Rando info panel (null = default)
)
