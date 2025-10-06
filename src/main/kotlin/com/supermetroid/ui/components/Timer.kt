package com.supermetroid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
fun Timer(
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

    // Add a unique ID for this timer instance to track in logs
    val timerId = remember { "timer-${System.currentTimeMillis()}" }

    // Log initial state
    LaunchedEffect(Unit) {
        println("[DEBUG_LOG] $timerId: Timer component initialized")
    }

    LaunchedEffect(currentRun, currentRun?.isPaused) {
        val runId = currentRun?.id ?: "null"
        val isPaused = currentRun?.isPaused ?: false
        println("[DEBUG_LOG] $timerId: LaunchedEffect triggered - runId: $runId, isPaused: $isPaused, wasPaused: $wasPaused")

        if (currentRun == null) {
            // No run, reset everything
            println("[DEBUG_LOG] $timerId: No run, resetting timer state")
            currentTime = 0L
            lastUpdateTime = System.currentTimeMillis()
            pausedDisplayTime = 0L
            wasPaused = false
        } else if (currentRun.isPaused) {
            // Just paused - save the current display time
            println("[DEBUG_LOG] $timerId: Run is paused")
            if (!wasPaused) {
                println("[DEBUG_LOG] $timerId: Newly paused, saving display time: $currentTime")
                pausedDisplayTime = currentTime
            }
            // When paused, just display the saved time
            currentTime = pausedDisplayTime
            println("[DEBUG_LOG] $timerId: Set current time to pausedDisplayTime: $pausedDisplayTime")
            wasPaused = true
        } else {
            // Run is active (not paused)
            val now = System.currentTimeMillis()
            println("[DEBUG_LOG] $timerId: Run is active (not paused)")

            // Initialize the timer state when a run starts or resumes
            if (currentRun.completedSplits.isEmpty() && !wasPaused) {
                // New run - initialize with the correct starting values
                val rawTime = now - currentRun.startTime.toEpochMilliseconds() - currentRun.pausedTime
                println("[DEBUG_LOG] $timerId: New run - initializing timer. startTime: ${currentRun.startTime}, pausedTime: ${currentRun.pausedTime}, rawTime: $rawTime")

                // Reset all timer state for a clean start
                pausedDisplayTime = 0L
                currentTime = rawTime
                lastUpdateTime = now

                println("[DEBUG_LOG] $timerId: Timer state reset for new run - currentTime: $currentTime, lastUpdateTime: $lastUpdateTime")
            } else if (wasPaused) {
                // Resuming from pause - keep the pausedDisplayTime as our base
                // but update the lastUpdateTime to now
                println("[DEBUG_LOG] $timerId: Resuming from pause - pausedDisplayTime: $pausedDisplayTime, updating lastUpdateTime to now")
                lastUpdateTime = now
                wasPaused = false
            } else {
                // This is a case where the run is already active and not paused
                // We need to recalculate the current time based on the run state
                val calculatedTime = now - currentRun.startTime.toEpochMilliseconds() - currentRun.pausedTime
                println("[DEBUG_LOG] $timerId: Continuing active run - recalculating time. startTime: ${currentRun.startTime}, pausedTime: ${currentRun.pausedTime}, calculatedTime: $calculatedTime")

                // Update the timer state to match the calculated time
                pausedDisplayTime = 0L
                currentTime = calculatedTime
                lastUpdateTime = now
            }

            // Continuously update the timer while the run is active
            println("[DEBUG_LOG] $timerId: Starting timer update loop")
            var updateCount = 0
            var lastLogTime = System.currentTimeMillis()

            // Safely check if the run is still active and not paused
            while (true) {
                // Get the current run state - exit loop if run has changed or been reset
                val currentRunState = splitsState.currentRun
                if (currentRunState != currentRun || currentRunState == null) {
                    println("[DEBUG_LOG] $timerId: Breaking timer loop - run changed or reset")
                    break
                }

                // Exit loop if run is paused
                if (currentRunState.isPaused) {
                    println("[DEBUG_LOG] $timerId: Breaking timer loop - run paused")
                    break
                }

                val now = System.currentTimeMillis()

                // Calculate the current time directly from the run state
                // This is more accurate than incremental updates
                val runTime = now - currentRun.startTime.toEpochMilliseconds() - currentRun.pausedTime
                currentTime = runTime

                // Log periodically to avoid flooding (every second)
                updateCount++
                if (now - lastLogTime > 1000) {
                    println("[DEBUG_LOG] $timerId: Timer update #$updateCount - runTime: $runTime")
                    lastLogTime = now
                }

                // No need to track lastUpdateTime or pausedDisplayTime for active runs
                // as we're calculating the time directly from the run state
                delay(16) // Update at approximately 60fps (16.67ms) for smooth display
            }
            println("[DEBUG_LOG] $timerId: Exited timer update loop")
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = TrackerColors.Surface
        ),
        shape = RoundedCornerShape(4.dp) // Reduced corner radius for more compact look
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp), // Minimal padding for compact design
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Main Timer Display with hover controls
            val interactionSource = remember { MutableInteractionSource() }
            val isHovered by interactionSource.collectIsHoveredAsState()
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp) // Minimal padding for compact design
                    .hoverable(interactionSource = interactionSource)
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

                // Control buttons (only show on hover, positioned absolutely)
                if (isHovered) {
                    // Play/Pause Button (top left)
                    IconButton(
                        onClick = onToggleRun,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = 2.dp, y = -2.dp)
                            .size(20.dp)
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
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Reset Button (bottom left)
                    IconButton(
                        onClick = onResetRun,
                        enabled = splitsState.currentRun != null,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = 2.dp, y = (2).dp)
                            .size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = if (splitsState.currentRun != null) TrackerColors.Error else TrackerColors.OnSurfaceVariant,
                            modifier = Modifier.size(20.dp)
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
