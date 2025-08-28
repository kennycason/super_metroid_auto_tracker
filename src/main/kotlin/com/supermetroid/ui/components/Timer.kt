package com.supermetroid.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermetroid.model.SplitsState
import com.supermetroid.ui.theme.TrackerColors
import kotlinx.coroutines.delay

@Composable
fun TimerSection(
    splitsState: SplitsState,
    onToggleRun: () -> Unit,
    onResetRun: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentRun = splitsState.currentRun
    val isRunning = currentRun != null
    
    // Real-time timer update
    var currentTime by remember { mutableLongStateOf(0L) }
    
    LaunchedEffect(currentRun) {
        if (currentRun != null) {
            while (true) {
                currentTime = System.currentTimeMillis() - currentRun.startTime.toEpochMilliseconds()
                delay(10) // Update every 10ms for smooth display
            }
        } else {
            currentTime = 0L
        }
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = TrackerColors.Surface
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            TrackerColors.Surface,
                            TrackerColors.Background
                        )
                    )
                )
                .border(
                    2.dp,
                    if (isRunning) TrackerColors.SplitActive else TrackerColors.Border,
                    RoundedCornerShape(4.dp)
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Timer display
                TimerDisplay(
                    timeMs = if (isRunning) currentTime else 0L,
                    isRunning = isRunning
                )
                
                // Control buttons
                TimerControls(
                    isRunning = isRunning,
                    onStart = onToggleRun,
                    onReset = onResetRun
                )
                
                // Current run info
                if (currentRun != null) {
                    CurrentRunInfo(splitsState)
                }
            }
        }
    }
}

@Composable
private fun TimerDisplay(
    timeMs: Long,
    isRunning: Boolean
) {
    // Pulsing animation for running timer
    val infiniteTransition = rememberInfiniteTransition(label = "timerPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRunning) 0.8f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    
    val displayTime = formatTime(timeMs)
    
    Box(
        modifier = Modifier
            .background(
                TrackerColors.Background.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp)
            )
            .border(
                2.dp,
                TrackerColors.Primary.copy(alpha = if (isRunning) pulseAlpha else 0.3f),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayTime,
            style = MaterialTheme.typography.displayLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 48.sp,
                letterSpacing = 2.sp,
                color = TrackerColors.Primary.copy(alpha = pulseAlpha),
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = TrackerColors.Primary.copy(alpha = 0.8f),
                    blurRadius = 12f
                )
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TimerControls(
    isRunning: Boolean,
    onStart: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Start/Pause button
        TrackerButton(
            onClick = onStart,
            enabled = !isRunning,
            colors = ButtonDefaults.buttonColors(
                containerColor = TrackerColors.Success.copy(alpha = 0.8f),
                contentColor = TrackerColors.Background,
                disabledContainerColor = TrackerColors.Inactive,
                disabledContentColor = TrackerColors.OnSurfaceVariant
            )
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "START",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )
        }
        
        // Reset button
        TrackerButton(
            onClick = onReset,
            colors = ButtonDefaults.buttonColors(
                containerColor = TrackerColors.Error.copy(alpha = 0.8f),
                contentColor = TrackerColors.Background
            )
        ) {
            Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "RESET",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )
        }
    }
}

@Composable
private fun CurrentRunInfo(splitsState: SplitsState) {
    val currentRun = splitsState.currentRun ?: return
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = TrackerColors.SurfaceOverlayLight
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "CURRENT RUN",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = TrackerColors.OnSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "Profile: ${currentRun.profileId.uppercase()}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TrackerColors.Primary
                )
            )
            Text(
                text = "Splits: ${currentRun.completedSplits.size}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TrackerColors.OnSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun TrackerButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = colors,
        modifier = Modifier
            .height(40.dp)
            .border(
                1.dp,
                if (enabled) TrackerColors.BorderActive else TrackerColors.Border,
                RoundedCornerShape(4.dp)
            ),
        shape = RoundedCornerShape(4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        content()
    }
}

/**
 * Format time in HH:MM:SS.ss format
 */
private fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val centiseconds = (timeMs % 1000) / 10
    
    return if (hours > 0) {
        "%02d:%02d:%02d.%02d".format(hours, minutes, seconds, centiseconds)
    } else {
        "%02d:%02d.%02d".format(minutes, seconds, centiseconds)
    }
}
