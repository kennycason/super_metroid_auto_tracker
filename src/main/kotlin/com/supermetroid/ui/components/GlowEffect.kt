package com.supermetroid.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.supermetroid.service.SuitTheme
import com.supermetroid.service.SuitThemeService
import androidx.compose.runtime.collectAsState

/**
 * Helper function to parse hex colors
 */
private fun parseColor(hex: String): Color {
    return when (hex) {
        "#4A90E2" -> Color(0xFF4A90E2)
        "#6BB6FF" -> Color(0xFF6BB6FF)
        "#FF6B35" -> Color(0xFFFF6B35)
        "#FF8C42" -> Color(0xFFFF8C42)
        "#8A2BE2" -> Color(0xFF8A2BE2)
        "#A855F7" -> Color(0xFFA855F7)
        else -> Color(0xFF4A90E2) // Default blue
    }
}

/**
 * Composable that applies dynamic glow effects based on suit theme
 */
@Composable
fun GlowEffect(
    suitThemeService: SuitThemeService,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val suitTheme by suitThemeService.currentSuitTheme.collectAsState()
    val visualEffectsEnabled by suitThemeService.visualEffectsEnabled.collectAsState()
    val glowIntensity by suitThemeService.glowIntensity.collectAsState()
    val animationSpeed by suitThemeService.animationSpeed.collectAsState()
    
    // Always render content, just skip the glow effect if disabled
    if (!visualEffectsEnabled) {
        // No wrapper, just render content directly to maintain layout
        content()
        return
    }
    
    // Animated glow effect
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (2000 / animationSpeed).toInt(),
                easing = EaseInOutSine
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    
    Box(
        modifier = modifier
            .drawWithContent {
                drawGlowEffect(
                    suitTheme = suitTheme,
                    glowAlpha = glowAlpha,
                    intensity = glowIntensity
                )
                drawContent()
            }
    ) {
        content()
    }
}

/**
 * Draw glow effect around the content
 */
private fun DrawScope.drawGlowEffect(
    suitTheme: SuitTheme,
    glowAlpha: Float,
    intensity: Float
) {
    val glowColor = parseColor(suitTheme.glowColor)
        .copy(alpha = glowAlpha * intensity)
    
    val primaryColor = parseColor(suitTheme.primaryColor)
        .copy(alpha = glowAlpha * intensity * 0.5f)
    
    // Outer glow
    drawRoundRect(
        color = glowColor,
        topLeft = androidx.compose.ui.geometry.Offset(-8f, -8f),
        size = androidx.compose.ui.geometry.Size(size.width + 16f, size.height + 16f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f),
        style = Stroke(width = 4f)
    )
    
    // Inner glow
    drawRoundRect(
        color = primaryColor,
        topLeft = androidx.compose.ui.geometry.Offset(-4f, -4f),
        size = androidx.compose.ui.geometry.Size(size.width + 8f, size.height + 8f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f),
        style = Stroke(width = 2f)
    )
}

/**
 * Composable for applying glow effects to individual icons
 */
@Composable
fun GlowIcon(
    suitThemeService: SuitThemeService,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val suitTheme by suitThemeService.currentSuitTheme.collectAsState()
    val visualEffectsEnabled by suitThemeService.visualEffectsEnabled.collectAsState()
    val glowIntensity by suitThemeService.glowIntensity.collectAsState()
    
    if (!visualEffectsEnabled) {
        content()
        return
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "iconGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconGlowAlpha"
    )
    
    Box(
        modifier = modifier
            .drawWithContent {
                if (visualEffectsEnabled) {
                    drawIconGlow(
                        suitTheme = suitTheme,
                        glowAlpha = glowAlpha,
                        intensity = glowIntensity
                    )
                }
                drawContent()
            }
    ) {
        content()
    }
}

/**
 * Draw glow effect for individual icons
 */
private fun DrawScope.drawIconGlow(
    suitTheme: SuitTheme,
    glowAlpha: Float,
    intensity: Float
) {
    val glowColor = parseColor(suitTheme.glowColor)
        .copy(alpha = glowAlpha * intensity * 0.7f)
    
    // Subtle glow around icon
    drawRoundRect(
        color = glowColor,
        topLeft = androidx.compose.ui.geometry.Offset(-2f, -2f),
        size = androidx.compose.ui.geometry.Size(size.width + 4f, size.height + 4f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f),
        style = Stroke(width = 1.5f)
    )
}
