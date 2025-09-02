package com.supermetroid.service

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Service for managing configurable app themes
 */
class ThemeService {
    private val _currentTheme = MutableStateFlow(AppTheme.RETRO_GREEN)
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()
    
    fun setTheme(theme: AppTheme) {
        _currentTheme.value = theme
    }
    
    fun getColors(): ThemeColors {
        return _currentTheme.value.colors
    }
}

/**
 * Available app themes
 */
enum class AppTheme(val displayName: String, val colors: ThemeColors) {
    RETRO_GREEN("Classic Green", 
        ThemeColors(
            primary = Color(0xFF00FF00),
            primaryVariant = Color(0xFF00CC00),
            primaryLight = Color(0xFF66FF66),
            background = Color(0xFF0A0A0A),
            backgroundVariant = Color(0xFF1A1A1A),
            surface = Color(0xFF001100),
            surfaceVariant = Color(0xFF002200),
            border = Color(0xFF003300),
            borderActive = Color(0xFF00CC00),
            connected = Color(0xFF00FF00),
            disconnected = Color(0xFFFF4444),
            warning = Color(0xFFFFAA00),
            onPrimary = Color(0xFF000000),
            onBackground = Color(0xFF00FF00),
            onSurface = Color(0xFF00FF00),
            onSurfaceVariant = Color(0xFF66FF66),
            surfaceOverlay = Color(0x4D003300),
            surfaceOverlayLight = Color(0x33003300),
            surfaceOverlayHover = Color(0x66005500),
            success = Color(0xFF00FF00),
            error = Color(0xFFFF4444),
            inactive = Color(0xFF333333),
            splitCompleted = Color(0xFF66FF66),
            splitActive = Color(0xFF00FFAA), // Bright green-cyan for active splits
            splitPending = Color(0xFF666666)
        )
    ),
    
    NEON_BLUE("Neon Blue",
        ThemeColors(
            primary = Color(0xFF00CCFF),
            primaryVariant = Color(0xFF0099CC),
            primaryLight = Color(0xFF66DDFF),
            background = Color(0xFF0A0A0A),
            backgroundVariant = Color(0xFF1A1A1A),
            surface = Color(0xFF001122),
            surfaceVariant = Color(0xFF002244),
            border = Color(0xFF003366),
            borderActive = Color(0xFF0099CC),
            connected = Color(0xFF00CCFF),
            disconnected = Color(0xFFFF4444),
            warning = Color(0xFFFFAA00),
            onPrimary = Color(0xFF000000),
            onBackground = Color(0xFF00CCFF),
            onSurface = Color(0xFF00CCFF),
            onSurfaceVariant = Color(0xFF66DDFF),
            surfaceOverlay = Color(0x4D003366),
            surfaceOverlayLight = Color(0x33003366),
            surfaceOverlayHover = Color(0x66005588),
            success = Color(0xFF00CCFF),
            error = Color(0xFFFF4444),
            inactive = Color(0xFF333333),
            splitCompleted = Color(0xFF66DDFF),
            splitActive = Color(0xFF00AAFF), // Bright cyan-blue for active splits
            splitPending = Color(0xFF666666)
        )
    ),
    
    ELECTRIC_PURPLE("Electric Purple",
        ThemeColors(
            primary = Color(0xFFCC00FF),
            primaryVariant = Color(0xFF9900CC),
            primaryLight = Color(0xFFDD66FF),
            background = Color(0xFF0A0A0A),
            backgroundVariant = Color(0xFF1A1A1A),
            surface = Color(0xFF220022),
            surfaceVariant = Color(0xFF440044),
            border = Color(0xFF660066),
            borderActive = Color(0xFF9900CC),
            connected = Color(0xFFCC00FF),
            disconnected = Color(0xFFFF4444),
            warning = Color(0xFFFFAA00),
            onPrimary = Color(0xFF000000),
            onBackground = Color(0xFFCC00FF),
            onSurface = Color(0xFFCC00FF),
            onSurfaceVariant = Color(0xFFDD66FF),
            surfaceOverlay = Color(0x4D660066),
            surfaceOverlayLight = Color(0x33660066),
            surfaceOverlayHover = Color(0x66880088),
            success = Color(0xFFCC00FF),
            error = Color(0xFFFF4444),
            inactive = Color(0xFF333333),
            splitCompleted = Color(0xFFDD66FF),
            splitActive = Color(0xFFAA00FF), // Bright purple for active splits
            splitPending = Color(0xFF666666)
        )
    ),
    
    CYBER_RED("Cyber Red",
        ThemeColors(
            primary = Color(0xFFFF0066),
            primaryVariant = Color(0xFFCC0044),
            primaryLight = Color(0xFFFF6699),
            background = Color(0xFF0A0A0A),
            backgroundVariant = Color(0xFF1A1A1A),
            surface = Color(0xFF220011),
            surfaceVariant = Color(0xFF440022),
            border = Color(0xFF660033),
            borderActive = Color(0xFFCC0044),
            connected = Color(0xFFFF0066),
            disconnected = Color(0xFFFF4444),
            warning = Color(0xFFFFAA00),
            onPrimary = Color(0xFF000000),
            onBackground = Color(0xFFFF0066),
            onSurface = Color(0xFFFF0066),
            onSurfaceVariant = Color(0xFFFF6699),
            surfaceOverlay = Color(0x4D660033),
            surfaceOverlayLight = Color(0x33660033),
            surfaceOverlayHover = Color(0x66880055),
            success = Color(0xFFFF0066),
            error = Color(0xFFFF4444),
            inactive = Color(0xFF333333),
            splitCompleted = Color(0xFFFF6699),
            splitActive = Color(0xFFFF0099),
            splitPending = Color(0xFF666666)
        )
    ),
    
    AMBER_GOLD("Amber Gold",
        ThemeColors(
            primary = Color(0xFFFFBB00),
            primaryVariant = Color(0xFFCC9900),
            primaryLight = Color(0xFFFFDD66),
            background = Color(0xFF0A0A0A),
            backgroundVariant = Color(0xFF1A1A1A),
            surface = Color(0xFF221100),
            surfaceVariant = Color(0xFF442200),
            border = Color(0xFF663300),
            borderActive = Color(0xFFCC9900),
            connected = Color(0xFFFFBB00),
            disconnected = Color(0xFFFF4444),
            warning = Color(0xFFFFAA00),
            onPrimary = Color(0xFF000000),
            onBackground = Color(0xFFFFBB00),
            onSurface = Color(0xFFFFBB00),
            onSurfaceVariant = Color(0xFFFFDD66),
            surfaceOverlay = Color(0x4D663300),
            surfaceOverlayLight = Color(0x33663300),
            surfaceOverlayHover = Color(0x66885500),
            success = Color(0xFFFFBB00),
            error = Color(0xFFFF4444),
            inactive = Color(0xFF333333),
            splitCompleted = Color(0xFFFFDD66),
            splitActive = Color(0xFFFFCC00),
            splitPending = Color(0xFF666666)
        )
    ),
    
    DARK_BLACK("Dark Black",
        ThemeColors(
            primary = Color(0xFFFFFFFF),
            primaryVariant = Color(0xFFE0E0E0),
            primaryLight = Color(0xFFFFFFFF),
            background = Color(0xFF000000),
            backgroundVariant = Color(0xFF111111),
            surface = Color(0xFF111111),
            surfaceVariant = Color(0xFF222222),
            border = Color(0xFF444444),
            borderActive = Color(0xFFFFFFFF),
            connected = Color(0xFFFFFFFF),
            disconnected = Color(0xFFFF4444),
            warning = Color(0xFFFFAA00),
            onPrimary = Color(0xFF000000),
            onBackground = Color(0xFFFFFFFF),
            onSurface = Color(0xFFFFFFFF),
            onSurfaceVariant = Color(0xFFCCCCCC),
            surfaceOverlay = Color(0x4D333333),
            surfaceOverlayLight = Color(0x33333333),
            surfaceOverlayHover = Color(0x66555555),
            success = Color(0xFF00FF00),
            error = Color(0xFFFF4444),
            inactive = Color(0xFF666666),
            splitCompleted = Color(0xFFCCCCCC),
            splitActive = Color(0xFFFFFFFF),
            splitPending = Color(0xFF666666)
        )
    ),
    
    WHITE_LIGHT("White Light",
        ThemeColors(
            primary = Color(0xFF000000),
            primaryVariant = Color(0xFF333333),
            primaryLight = Color(0xFF555555),
            background = Color(0xFFFFFFFF),
            backgroundVariant = Color(0xFFF8F8F8),
            surface = Color(0xFFF8F8F8),
            surfaceVariant = Color(0xFFEEEEEE),
            border = Color(0xFFCCCCCC),
            borderActive = Color(0xFF000000),
            connected = Color(0xFF000000),
            disconnected = Color(0xFFCC0000),
            warning = Color(0xFF996600),
            onPrimary = Color(0xFFFFFFFF),
            onBackground = Color(0xFF000000),
            onSurface = Color(0xFF000000),
            onSurfaceVariant = Color(0xFF333333),
            surfaceOverlay = Color(0x4DDDDDDD),
            surfaceOverlayLight = Color(0x33DDDDDD),
            surfaceOverlayHover = Color(0x66BBBBBB),
            success = Color(0xFF008800),
            error = Color(0xFFCC0000),
            inactive = Color(0xFF999999),
            splitCompleted = Color(0xFF333333),
            splitActive = Color(0xFF000000),
            splitPending = Color(0xFF999999)
        )
    )
}

/**
 * Theme color scheme data class
 */
data class ThemeColors(
    // Main colors
    val primary: Color,
    val primaryVariant: Color,
    val primaryLight: Color,
    
    // Background colors
    val background: Color,
    val backgroundVariant: Color,
    val surface: Color,
    val surfaceVariant: Color,
    
    // Border and accent colors
    val border: Color,
    val borderActive: Color,
    
    // Status colors
    val connected: Color,
    val disconnected: Color,
    val warning: Color,
    
    // Text colors
    val onPrimary: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    
    // Translucent overlays
    val surfaceOverlay: Color,
    val surfaceOverlayLight: Color,
    val surfaceOverlayHover: Color,
    
    // Special states
    val success: Color,
    val error: Color,
    val inactive: Color,
    val splitCompleted: Color,
    val splitActive: Color,
    val splitPending: Color
)
