package com.supermetroid.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.supermetroid.service.ThemeService
import com.supermetroid.service.AppTheme

// CompositionLocal to provide theme service throughout the composition tree
val LocalThemeService = compositionLocalOf<ThemeService> { 
    error("ThemeService not provided") 
}

/**
 * Provides theme service to the entire composition tree
 */
@Composable
fun ProvideThemeService(
    themeService: ThemeService,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalThemeService provides themeService) {
        content()
    }
}

/**
 * Dynamic color scheme that changes based on selected theme
 */
object TrackerColors {
    @Composable
    private fun getCurrentColors() = LocalThemeService.current.currentTheme.collectAsState().value.colors
    
    // Main colors
    val Primary: Color @Composable get() = getCurrentColors().primary
    val PrimaryVariant: Color @Composable get() = getCurrentColors().primaryVariant
    val PrimaryLight: Color @Composable get() = getCurrentColors().primaryLight
    
    // Background colors
    val Background: Color @Composable get() = getCurrentColors().background
    val BackgroundVariant: Color @Composable get() = getCurrentColors().backgroundVariant
    val Surface: Color @Composable get() = getCurrentColors().surface
    val SurfaceVariant: Color @Composable get() = getCurrentColors().surfaceVariant
    
    // Border and accent colors
    val Border: Color @Composable get() = getCurrentColors().border
    val BorderActive: Color @Composable get() = getCurrentColors().borderActive
    
    // Status colors
    val Connected: Color @Composable get() = getCurrentColors().connected
    val Disconnected: Color @Composable get() = getCurrentColors().disconnected
    val Warning: Color @Composable get() = getCurrentColors().warning
    
    // Text colors
    val OnPrimary: Color @Composable get() = getCurrentColors().onPrimary
    val OnBackground: Color @Composable get() = getCurrentColors().onBackground
    val OnSurface: Color @Composable get() = getCurrentColors().onSurface
    val OnSurfaceVariant: Color @Composable get() = getCurrentColors().onSurfaceVariant
    
    // Translucent overlays
    val SurfaceOverlay: Color @Composable get() = getCurrentColors().surfaceOverlay
    val SurfaceOverlayLight: Color @Composable get() = getCurrentColors().surfaceOverlayLight
    val SurfaceOverlayHover: Color @Composable get() = getCurrentColors().surfaceOverlayHover
    
    // Special states
    val Success: Color @Composable get() = getCurrentColors().success
    val Error: Color @Composable get() = getCurrentColors().error
    val Inactive: Color @Composable get() = getCurrentColors().inactive
    val SplitCompleted: Color @Composable get() = getCurrentColors().splitCompleted
    val SplitActive: Color @Composable get() = getCurrentColors().splitActive
    val SplitPending: Color @Composable get() = getCurrentColors().splitPending
}
