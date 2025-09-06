package com.supermetroid.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermetroid.service.ControllerState
import com.supermetroid.service.ButtonFrequency
import com.supermetroid.ui.theme.TrackerColors
import kotlin.math.min

/**
 * Controller tracker panel showing button states with colored rectangles
 * Layout: dpad-left, dpad-up, dpad-right, dpad-down, select, start, L, R, X, Y, B, A
 */
@Composable
fun ControllerTrackerPanel(
    controllerState: ControllerState,
    buttonFrequencies: Map<String, ButtonFrequency>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = TrackerColors.Surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title and connection status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CONTROLLER",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = TrackerColors.Primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )
                
                Text(
                    text = if (controllerState.isConnected) controllerState.controllerName else "DISCONNECTED",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (controllerState.isConnected) TrackerColors.Success else TrackerColors.Error,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Button display area - same height as timer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp) // Match timer height
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                TrackerColors.Surface,
                                TrackerColors.SurfaceOverlayLight
                            )
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .border(
                        2.dp,
                        TrackerColors.Border,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp)
            ) {
                // Button layout: 12 buttons in a row
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Define button order and their states
                    val buttons = listOf(
                        "dpadLeft" to controllerState.dpadLeft,
                        "dpadUp" to controllerState.dpadUp,
                        "dpadRight" to controllerState.dpadRight,
                        "dpadDown" to controllerState.dpadDown,
                        "select" to controllerState.select,
                        "start" to controllerState.start,
                        "l" to controllerState.l,
                        "r" to controllerState.r,
                        "x" to controllerState.x,
                        "y" to controllerState.y,
                        "b" to controllerState.b,
                        "a" to controllerState.a
                    )
                    
                    buttons.forEach { (buttonName, isPressed) ->
                        val frequency = buttonFrequencies[buttonName] ?: ButtonFrequency()
                        
                        ButtonIndicator(
                            buttonName = buttonName,
                            isPressed = isPressed,
                            frequency = frequency,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual button indicator with color coding and glow effects
 */
@Composable
private fun ButtonIndicator(
    buttonName: String,
    isPressed: Boolean,
    frequency: ButtonFrequency,
    modifier: Modifier = Modifier
) {
    // Color scheme for different button types
    val baseColor = when (buttonName) {
        "dpadLeft", "dpadUp", "dpadRight", "dpadDown" -> Color(0xFF4A90E2) // Blue for D-pad
        "select", "start" -> Color(0xFF7B68EE) // Purple for system buttons
        "l", "r" -> Color(0xFF50C878) // Green for shoulder buttons
        "x", "y" -> Color(0xFFFFD700) // Gold for face buttons top row
        "b", "a" -> Color(0xFFFF6B6B) // Red for face buttons bottom row
        else -> Color(0xFF888888) // Gray fallback
    }
    
    // Calculate glow intensity based on frequency
    val glowIntensity = min(frequency.pressesPerSecond / 10f, 1f) // Max glow at 10 presses/second
    
    // Pulsing animation for high frequency
    val infiniteTransition = rememberInfiniteTransition(label = "buttonGlow")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )
    
    // Determine final color and glow
    val finalColor = if (isPressed) {
        baseColor
    } else if (glowIntensity > 0.1f) {
        baseColor.copy(alpha = 0.3f + (glowIntensity * 0.4f * glowPulse))
    } else {
        baseColor.copy(alpha = 0.2f)
    }
    
    // Shadow/glow effect for high frequency buttons
    val shadowElevation = if (glowIntensity > 0.3f) {
        (4.dp * glowIntensity * glowPulse)
    } else {
        0.dp
    }
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Button rectangle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .shadow(
                    elevation = shadowElevation,
                    shape = RoundedCornerShape(4.dp),
                    ambientColor = baseColor,
                    spotColor = baseColor
                )
                .background(
                    color = finalColor,
                    shape = RoundedCornerShape(4.dp)
                )
                .border(
                    width = if (isPressed) 2.dp else 1.dp,
                    color = if (isPressed) baseColor.copy(alpha = 0.8f) else TrackerColors.Border.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(4.dp)
                )
        )
        
        // Button label
        Text(
            text = getButtonLabel(buttonName),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (isPressed) TrackerColors.Primary else TrackerColors.OnSurfaceVariant
            ),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        
        // Frequency indicator (small dot)
        if (frequency.pressesPerSecond > 1f) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(
                        color = baseColor.copy(alpha = min(frequency.pressesPerSecond / 5f, 1f)),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        } else {
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

/**
 * Get display label for button names
 */
private fun getButtonLabel(buttonName: String): String {
    return when (buttonName) {
        "dpadLeft" -> "◀"
        "dpadUp" -> "▲"
        "dpadRight" -> "▶"
        "dpadDown" -> "▼"
        "select" -> "SEL"
        "start" -> "STA"
        "l" -> "L"
        "r" -> "R"
        "x" -> "X"
        "y" -> "Y"
        "b" -> "B"
        "a" -> "A"
        else -> buttonName.take(3).uppercase()
    }
}
