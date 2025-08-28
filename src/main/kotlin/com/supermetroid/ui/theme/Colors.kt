package com.supermetroid.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Exact color scheme matching the original tracker
 */
object TrackerColors {
    // Main retro green colors
    val Primary = Color(0xFF00FF00)          // Bright green #00ff00
    val PrimaryVariant = Color(0xFF00CC00)   // Darker green #00cc00
    val PrimaryLight = Color(0xFF66FF66)     // Light green #66ff66
    
    // Background colors
    val Background = Color(0xFF0A0A0A)       // Main background #0a0a0a
    val BackgroundVariant = Color(0xFF1A1A1A) // Gradient variant #1a1a1a
    val Surface = Color(0xFF001100)          // Dark green surface #001100
    val SurfaceVariant = Color(0xFF002200)   // Focused surface #002200
    
    // Border and accent colors
    val Border = Color(0xFF003300)           // Dark green border #003300
    val BorderActive = Color(0xFF00CC00)     // Active border #00cc00
    
    // Status colors
    val Connected = Color(0xFF00FF00)        // Green for connected
    val Disconnected = Color(0xFFFF4444)    // Red for disconnected
    val Warning = Color(0xFFFFAA00)         // Orange for warnings
    
    // Text colors
    val OnPrimary = Color(0xFF000000)       // Black text on primary
    val OnBackground = Color(0xFF00FF00)    // Green text on background
    val OnSurface = Color(0xFF00FF00)       // Green text on surface
    val OnSurfaceVariant = Color(0xFF66FF66) // Light green variant
    
    // Translucent overlays
    val SurfaceOverlay = Color(0x4D003300)   // 30% opacity green overlay
    val SurfaceOverlayLight = Color(0x33003300) // 20% opacity green overlay
    val SurfaceOverlayHover = Color(0x66005500) // 40% opacity green hover
    
    // Special states
    val Success = Color(0xFF00FF00)
    val Error = Color(0xFFFF4444)
    val Inactive = Color(0xFF333333)
    val SplitCompleted = Color(0xFF66FF66)
    val SplitActive = Color(0xFF00FFFF)      // Cyan for active split
    val SplitPending = Color(0xFF666666)     // Gray for pending splits
}
