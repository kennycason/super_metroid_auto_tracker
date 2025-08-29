package com.supermetroid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun SimpleEnhancedTimer(
    splitsState: SplitsState,
    onToggleRun: () -> Unit,
    onResetRun: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentRun = splitsState.currentRun

    // Real-time timer update
    var currentTime by remember { mutableLongStateOf(0L) }

    // Track the last time we updated the timer to calculate elapsed time
    var lastUpdateTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Remember the displayed time when paused to prevent jumps on resume
    var pausedDisplayTime by remember { mutableLongStateOf(0L) }

    // Track the previous pause state to detect changes
    var wasPaused by remember { mutableStateOf(false) }

    LaunchedEffect(currentRun, currentRun?.isPaused) {
        if (currentRun == null) {
            // No run, reset everything
            currentTime = 0L
            lastUpdateTime = System.currentTimeMillis()
            pausedDisplayTime = 0L
            wasPaused = false
        } else if (currentRun.isPaused) {
            // Just paused - save the current display time
            if (!wasPaused) {
                pausedDisplayTime = currentTime
            }
            // When paused, just display the saved time
            currentTime = pausedDisplayTime
            wasPaused = true
        } else {
            // Run is active (not paused)
            val now = System.currentTimeMillis()

            // Initialize the timer state when a run starts or resumes
            if (currentRun.completedSplits.isEmpty() && !wasPaused) {
                // New run - initialize with the correct starting values
                val rawTime = now - currentRun.startTime.toEpochMilliseconds() - currentRun.pausedTime
                pausedDisplayTime = rawTime
                currentTime = rawTime
            } else if (wasPaused) {
                // Resuming from pause - keep the pausedDisplayTime as our base
                // but update the lastUpdateTime to now
                lastUpdateTime = now
                wasPaused = false
            }

            // Continuously update the timer while the run is active
            while (currentRun == splitsState.currentRun && !splitsState.currentRun?.isPaused!!) {
                val now = System.currentTimeMillis()
                val elapsed = now - lastUpdateTime

                // Update the current time based on the elapsed time since last update
                // This prevents jumps when resuming after a pause
                currentTime = pausedDisplayTime + elapsed

                lastUpdateTime = now
                delay(10) // Update every 10ms for smooth display
            }
        }
    }

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
                .padding(8.dp), // Reduced padding from 16dp to 8dp
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main Timer Display with small control buttons inside
            Box(
                modifier = Modifier
                    .fillMaxWidth()
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
                    .padding(12.dp) // Reduced padding from 16dp to 12dp
            ) {
                // Main timer text (centered)
                Text(
                    text = formatTime(currentTime),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = TrackerColors.Primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp, // Slightly smaller font for more compact layout
                        fontFamily = FontFamily.Monospace
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Small control buttons (top-left corner)
                Row(
                    modifier = Modifier.align(Alignment.TopStart),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Start/Pause Button (small icon only)
                    IconButton(
                        onClick = onToggleRun,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                currentRun == null -> Icons.Default.PlayArrow
                                currentRun.isPaused -> Icons.Default.PlayArrow
                                else -> Icons.Default.Pause
                            },
                            contentDescription = when {
                                currentRun == null -> "Start"
                                currentRun.isPaused -> "Resume"
                                else -> "Pause"
                            },
                            tint = when {
                                currentRun == null -> TrackerColors.Success
                                currentRun.isPaused -> TrackerColors.Success
                                else -> TrackerColors.Warning
                            },
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Reset Button (small icon only)
                    IconButton(
                        onClick = onResetRun,
                        enabled = splitsState.currentRun != null,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = if (splitsState.currentRun != null) TrackerColors.Error else TrackerColors.OnSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(milliseconds: Long): String {
    if (milliseconds <= 0) return "--:--:--.--"

    val totalSeconds = milliseconds / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val centiseconds = (milliseconds % 1000) / 10

    return if (hours > 0) {
        "%d:%02d:%02d.%02d".format(hours, minutes, seconds, centiseconds)
    } else {
        "%02d:%02d.%02d".format(minutes, seconds, centiseconds)
    }
}
