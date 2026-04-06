package com.supermetroid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermetroid.model.SplitsState
import com.supermetroid.ui.theme.TrackerColors
import com.supermetroid.util.Logging
import kotlinx.coroutines.delay

private object TimerLog : Logging

@Composable
fun Timer(
    splitsState: SplitsState,
    onToggleRun: () -> Unit,
    onResetRun: () -> Unit,
    onManualSplit: () -> Unit = {},
    onUndoSplit: () -> Unit = {},
    onDiscardRun: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showDiscardConfirm by remember { mutableStateOf(false) }
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
        TimerLog.logger.debug { "$timerId: Timer component initialized" }
    }

    LaunchedEffect(currentRun, currentRun?.isPaused) {
        val runId = currentRun?.id ?: "null"
        val isPaused = currentRun?.isPaused ?: false
        TimerLog.logger.debug { "$timerId: LaunchedEffect triggered - runId: $runId, isPaused: $isPaused, wasPaused: $wasPaused" }

        if (currentRun == null) {
            // No run, reset everything
            TimerLog.logger.debug { "$timerId: No run, resetting timer state" }
            currentTime = 0L
            lastUpdateTime = System.currentTimeMillis()
            pausedDisplayTime = 0L
            wasPaused = false
        } else if (currentRun.isPaused) {
            // Just paused - use the run's totalTime as the authoritative source
            // This works for both manual pause and setTimer
            TimerLog.logger.debug { "$timerId: Run is paused" }
            if (!wasPaused) {
                TimerLog.logger.debug { "$timerId: Newly paused, totalTime: ${currentRun.totalTime}" }
                pausedDisplayTime = currentRun.totalTime
            }
            // When paused, always display the run's totalTime
            currentTime = currentRun.totalTime
            TimerLog.logger.debug { "$timerId: Set current time to totalTime: ${currentRun.totalTime}" }
            wasPaused = true
        } else {
            // Run is active (not paused)
            val now = System.currentTimeMillis()
            TimerLog.logger.debug { "$timerId: Run is active (not paused)" }

            // Initialize the timer state when a run starts or resumes
            if (currentRun.completedSplits.isEmpty() && !wasPaused) {
                // New run - initialize with the correct starting values
                val rawTime = now - currentRun.startTime.toEpochMilliseconds() - currentRun.pausedTime
                TimerLog.logger.debug { "$timerId: New run - initializing timer. startTime: ${currentRun.startTime}, pausedTime: ${currentRun.pausedTime}, rawTime: $rawTime" }

                // Reset all timer state for a clean start
                pausedDisplayTime = 0L
                currentTime = rawTime
                lastUpdateTime = now

                TimerLog.logger.debug { "$timerId: Timer state reset for new run - currentTime: $currentTime, lastUpdateTime: $lastUpdateTime" }
            } else if (wasPaused) {
                // Resuming from pause - keep the pausedDisplayTime as our base
                // but update the lastUpdateTime to now
                TimerLog.logger.debug { "$timerId: Resuming from pause - pausedDisplayTime: $pausedDisplayTime, updating lastUpdateTime to now" }
                lastUpdateTime = now
                wasPaused = false
            } else {
                // This is a case where the run is already active and not paused
                // We need to recalculate the current time based on the run state
                val calculatedTime = now - currentRun.startTime.toEpochMilliseconds() - currentRun.pausedTime
                TimerLog.logger.debug { "$timerId: Continuing active run - recalculating time. startTime: ${currentRun.startTime}, pausedTime: ${currentRun.pausedTime}, calculatedTime: $calculatedTime" }

                // Update the timer state to match the calculated time
                pausedDisplayTime = 0L
                currentTime = calculatedTime
                lastUpdateTime = now
            }

            // Continuously update the timer while the run is active
            TimerLog.logger.debug { "$timerId: Starting timer update loop" }
            var updateCount = 0
            var lastLogTime = System.currentTimeMillis()

            // Safely check if the run is still active and not paused
            while (true) {
                // Get the current run state - exit loop if run has changed or been reset
                val currentRunState = splitsState.currentRun
                if (currentRunState != currentRun || currentRunState == null) {
                    TimerLog.logger.debug { "$timerId: Breaking timer loop - run changed or reset" }
                    break
                }

                // Exit loop if run is paused
                if (currentRunState.isPaused) {
                    TimerLog.logger.debug { "$timerId: Breaking timer loop - run paused" }
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
                    TimerLog.logger.debug { "$timerId: Timer update #$updateCount - runTime: $runTime" }
                    lastLogTime = now
                }

                // No need to track lastUpdateTime or pausedDisplayTime for active runs
                // as we're calculating the time directly from the run state
                delay(16) // Update at approximately 60fps (16.67ms) for smooth display
            }
            TimerLog.logger.debug { "$timerId: Exited timer update loop" }
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
                    .hoverable(interactionSource = interactionSource)
                    .padding(8.dp) // Minimal padding for compact design
            ) {
                // Compute timer color: compare current time against PB run's cumulative time
                // at the last completed split point
                val timerColor = run {
                    val profileId = currentRun?.profileId
                    val completedSplits = currentRun?.completedSplits
                    val pb = if (profileId != null) splitsState.personalBests[profileId] else null

                    if (currentTime <= 0L || completedSplits.isNullOrEmpty() || pb == null) {
                        TrackerColors.Primary // Timer not started, no splits yet, or no PB
                    } else {
                        // Find the PB run to get its cumulative time at the last split
                        val pbRun = splitsState.runHistory.find { it.id == pb.runSessionId }
                            ?: splitsState.runHistory
                                .filter { it.profileId == profileId && it.endTime != null }
                                .minByOrNull { it.totalTime }

                        if (pbRun == null) {
                            TrackerColors.Primary
                        } else {
                            // Look up the PB run's cumulative time at the same split
                            val lastSplitId = completedSplits.last().splitId
                            val pbSplitTime = pbRun.completedSplits.find { it.splitId == lastSplitId }
                            if (pbSplitTime == null) {
                                TrackerColors.Primary // Split not found in PB run
                            } else {
                                // delta = our cumulative - PB cumulative (negative = ahead)
                                val delta = completedSplits.last().time.totalTime - pbSplitTime.time.totalTime
                                timerGradientColor(delta)
                            }
                        }
                    }
                }

                // Main timer text (centered)
                Text(
                    text = formatTime(currentTime),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = timerColor,
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
                    // Play/Pause Button (top left)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = 2.dp, y = -2.dp)
                            .size(20.dp)
                            .background(
                                TrackerColors.Surface.copy(alpha = 0.7f),
                                RoundedCornerShape(4.dp)
                            )
                            .clickable { onToggleRun() },
                        contentAlignment = Alignment.Center
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
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Manual Split Button (bottom left) - two squished play arrows ▶▶
                    val manualSplitEnabled = currentRun != null && currentRun.endTime == null
                    val manualSplitColor = if (manualSplitEnabled) TrackerColors.Primary else TrackerColors.OnSurfaceVariant
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = 2.dp, y = 2.dp)
                            .size(20.dp)
                            .background(
                                TrackerColors.Surface.copy(alpha = 0.7f),
                                RoundedCornerShape(4.dp)
                            )
                            .then(if (manualSplitEnabled) Modifier.clickable { onManualSplit() } else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = manualSplitColor,
                            modifier = Modifier.size(14.dp).offset(x = (-3).dp)
                        )
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Manual Split",
                            tint = manualSplitColor,
                            modifier = Modifier.size(14.dp).offset(x = 3.dp)
                        )
                    }

                    // Undo Split Button (bottom right) - ◀◀
                    val undoSplitEnabled = currentRun != null && currentRun.endTime == null &&
                        currentRun.completedSplits.isNotEmpty()
                    val undoSplitColor = if (undoSplitEnabled) TrackerColors.Warning else TrackerColors.OnSurfaceVariant
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-2).dp, y = 2.dp)
                            .size(20.dp)
                            .background(
                                TrackerColors.Surface.copy(alpha = 0.7f),
                                RoundedCornerShape(4.dp)
                            )
                            .then(if (undoSplitEnabled) Modifier.clickable { onUndoSplit() } else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = undoSplitColor,
                            modifier = Modifier.size(14.dp).offset(x = 3.dp)
                                .graphicsLayer(scaleX = -1f)
                        )
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Undo Split",
                            tint = undoSplitColor,
                            modifier = Modifier.size(14.dp).offset(x = (-3).dp)
                                .graphicsLayer(scaleX = -1f)
                        )
                    }

                    // Reset Button (top right) - saves run
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-2).dp, y = -2.dp)
                            .size(20.dp)
                            .background(
                                TrackerColors.Surface.copy(alpha = 0.7f),
                                RoundedCornerShape(4.dp)
                            )
                            .then(if (splitsState.currentRun != null) Modifier.clickable { onResetRun() } else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset (save run)",
                            tint = if (splitsState.currentRun != null) TrackerColors.Error else TrackerColors.OnSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }

    // Discard confirmation dialog
    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = {
                Text(
                    "Discard Run?",
                    style = MaterialTheme.typography.titleSmall.copy(color = TrackerColors.OnSurface)
                )
            },
            text = {
                Text(
                    "This will reset the timer without saving. The run data will be lost.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TrackerColors.OnSurfaceVariant)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardConfirm = false
                    onDiscardRun()
                }) {
                    Text("Discard", color = TrackerColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) {
                    Text("Cancel", color = TrackerColors.OnSurfaceVariant)
                }
            },
            containerColor = TrackerColors.Surface
        )
    }
}

private val TimerGold = Color(0xFFFFD700)
private val TimerGreen = Color(0xFF00DD00)
private val TimerRed = Color(0xFFFF4444)

/**
 * Timer color gradient vs PB run cumulative time.
 * Ahead (negative delta) → gold. Behind → white..red gradient (saturates at 5 min).
 * 5 minutes gives a gentle gradient for hour-long runs — 7s behind is barely pink.
 */
private fun timerGradientColor(deltaMs: Long, maxDeltaMs: Long = 300_000): Color {
    if (deltaMs <= 0L) return TimerGold // Ahead of or matching PB
    val fraction = kotlin.math.min(deltaMs.toFloat() / maxDeltaMs, 1f)
    return lerpColor(Color.White, TimerRed, fraction)
}

private fun formatTime(milliseconds: Long): String {
    if (milliseconds <= 0) return "--:--:--.--"

    val totalSeconds = milliseconds / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val centiseconds = (milliseconds % 1000) / 10

    return "%d:%02d:%02d.%02d".format(hours, minutes, seconds, centiseconds)
//    return if (hours > 0) {
//        "%d:%02d:%02d.%02d".format(hours, minutes, seconds, centiseconds)
//    } else {
//        "%02d:%02d.%02d".format(minutes, seconds, centiseconds)
//    }
}
