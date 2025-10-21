package com.supermetroid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.supermetroid.autosplits.AutoSplitsEngine
import com.supermetroid.autosplits.KpdrAnyProfile
import com.supermetroid.model.Split
import com.supermetroid.model.SplitsState
import com.supermetroid.ui.theme.TrackerColors

@Composable
fun SplitsList(
    splitsState: SplitsState,
    autoSplitsEngine: AutoSplitsEngine,
    splitIconSizeService: com.supermetroid.service.SplitIconSizeService,
    splitDisplayModeService: com.supermetroid.service.SplitDisplayModeService,
    modifier: Modifier = Modifier,
    maxHeight: Int = 400
) {
    val currentSplitIconSize by splitIconSizeService.currentSplitIconSize.collectAsState()
    val showSplitIcons by splitDisplayModeService.showSplitIcons.collectAsState()
    val showSplitNames by splitDisplayModeService.showSplitNames.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Get current split index for auto-scrolling
    val currentSplit = autoSplitsEngine.getCurrentSplit()
    val currentSplitIndex = KpdrAnyProfile.profile.splits.indexOfFirst { it.id == currentSplit?.id }

    // Auto-scroll to keep the current split near the top (around 3rd position)
    LaunchedEffect(currentSplitIndex) {
        if (currentSplitIndex >= 0) {
            coroutineScope.launch {
                // Calculate offset to position the split around the 3rd position
                val itemHeight = 48 + 1 // SplitRow height + spacing
                val targetPosition = 2 // 0-indexed, so 2 means 3rd position
                val offset = targetPosition * itemHeight

                listState.animateScrollToItem(
                    index = currentSplitIndex,
                    scrollOffset = -offset.coerceAtLeast(0)
                )
            }
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
            // Header
            SplitsHeader(splitsState, autoSplitsEngine)

            Spacer(modifier = Modifier.height(8.dp))

            // Splits list - full height now
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(maxHeight.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp) // Reduced spacing
            ) {
                itemsIndexed(KpdrAnyProfile.profile.splits) { index, split ->
                    SplitRow(
                        split = split,
                        splitIndex = index,
                        splitsState = splitsState,
                        autoSplitsEngine = autoSplitsEngine,
                        splitIconSize = currentSplitIconSize.size,
                        showIcon = showSplitIcons,
                        showName = showSplitNames
                    )
                }
            }
        }
    }

@Composable
private fun SplitsHeader(
    splitsState: SplitsState,
    autoSplitsEngine: AutoSplitsEngine
) {
    @Suppress("UNUSED_PARAMETER") // splitsState might be used in future
    val currentSplit = autoSplitsEngine.getCurrentSplit()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "KPDR ANY%",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TrackerColors.Primary,
                    letterSpacing = 1.sp
                )
            )
            // if (currentSplit != null) {
            //     Text(
            //         text = "Next: ${currentSplit.name}",
            //         style = MaterialTheme.typography.bodyMedium.copy(
            //             color = TrackerColors.SplitActive,
            //             fontWeight = FontWeight.Bold,
            //             fontSize = 12.sp
            //         )
            //     )
            // }
        }

        // Column headers (BEST | TIME with delta)
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "BEST",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TrackerColors.OnSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                ),
                modifier = Modifier.width(70.dp), // Increased width to prevent line wrapping
                textAlign = TextAlign.End
            )
            Spacer(modifier = Modifier.width(2.dp)) // Reduced to move BEST closer to TIME
            Text(
                text = "TIME",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TrackerColors.OnSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                ),
                modifier = Modifier.width(120.dp), // Match the TIME column width
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun SplitRow(
    split: Split,
    splitIndex: Int,
    splitsState: SplitsState,
    autoSplitsEngine: AutoSplitsEngine,
    splitIconSize: Int,
    showIcon: Boolean,
    showName: Boolean
) {
    val currentRun = splitsState.currentRun
    val completedSplit = currentRun?.completedSplits?.find { it.splitId == split.id }
    val isCompleted = completedSplit != null
    val isActive = autoSplitsEngine.getCurrentSplit()?.id == split.id

    // Use a default profileId ("kpdr-any") when currentRun is null to ensure BEST column is always shown
    val profileId = currentRun?.profileId ?: "kpdr-any"
    val personalBest = splitsState.personalBests[profileId]?.splitTimes?.get(split.id)

    // Calculate sum of best segments up to this point (including this split)
    val profileSplitTimes = splitsState.personalBests[profileId]?.splitTimes
    val sumOfBestUpToHere = if (profileSplitTimes != null) {
        KpdrAnyProfile.profile.splits.take(splitIndex + 1).sumOf { s ->
            profileSplitTimes[s.id]?.segmentTime ?: 0L
        }
    } else {
        0L
    }

    // Get best segment time for this split
    val bestSegmentTime = personalBest?.segmentTime ?: 0L

    // Determine row colors and styling
    val backgroundColor = when {
        isActive -> TrackerColors.SplitActive.copy(alpha = 0.2f)
        isCompleted -> TrackerColors.SplitCompleted.copy(alpha = 0.1f)
        else -> Color.Transparent
    }

    val borderColor = when {
        isActive -> TrackerColors.SplitActive
        isCompleted -> Color.Transparent // No border for completed splits
        else -> TrackerColors.Border.copy(alpha = 0.3f)
    }
    
    // Calculate row height: max of icon size or 24dp base height, plus padding
    val rowHeight = kotlin.math.max(splitIconSize, 24) + 2 // +2 for vertical padding

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight.dp)
                    .padding(horizontal = 4.dp, vertical = 1.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
            // Split name with icon
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Split index (slightly bigger: 10sp -> 12sp, width 20dp -> 24dp)
                // Text(
                //     text = "${splitIndex + 1}.",
                //     style = MaterialTheme.typography.labelSmall.copy(
                //         color = TrackerColors.OnSurfaceVariant,
                //         fontSize = 10.sp
                //     ),
                //     modifier = Modifier.width(20.dp)
                // )

                // Split icon (conditionally shown, uses configurable split icon size)
                if (showIcon) {
                    SpriteIcon(
                        itemId = getSplitItemId(split),
                        isObtained = isCompleted,
                        size = splitIconSize,
                        modifier = Modifier.padding(end = if (showName) 4.dp else 0.dp)
                    )
                }

                // Split name (conditionally shown)
                if (showName) {
                    Text(
                        text = split.name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = when {
                                isActive -> TrackerColors.SplitActive
                                isCompleted -> TrackerColors.SplitCompleted
                                else -> TrackerColors.OnBackground
                            },
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            // Times section (BEST | TIME with delta, right-aligned)
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp), // Match header spacing
                verticalAlignment = Alignment.CenterVertically
            ) {

                // Personal best (first column) - show sum of best segments + individual segment time
                Column(
                    modifier = Modifier.width(70.dp), // Match header width
                    horizontalAlignment = Alignment.End
                ) {
                    // Sum of best segments up to this point (main line)
                    Text(
                        text = if (sumOfBestUpToHere > 0) {
                            formatTimeNoMillis(sumOfBestUpToHere)
                        } else {
                            "--:--:--"
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TrackerColors.OnSurfaceVariant,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        textAlign = TextAlign.End
                    )

                    // Best segment time for this split (second line)
                    if (bestSegmentTime > 0) {
                        Text(
                            text = formatTime(bestSegmentTime),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TrackerColors.OnSurfaceVariant.copy(alpha = 0.7f),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            ),
                            textAlign = TextAlign.End
                        )
                    }
                }

                // Current time (second column) - no milliseconds
                val timeText = if (isCompleted) {
                    formatTimeNoMillis(completedSplit!!.time.totalTime)
                } else {
                    "--:--:--"
                }

                // Delta text with milliseconds for precision - compare SEGMENT times, not total times
                val deltaText = if (isCompleted && personalBest != null && completedSplit != null) {
                    try {
                        // Check if this is a personal best with an original delta preserved
                        // Use reflection to safely access the originalDelta field which might not exist in older data
                        val isPB = personalBest.segmentTime == completedSplit.time.segmentTime
                        val originalDeltaField = personalBest::class.java.getDeclaredField("originalDelta")
                        originalDeltaField.isAccessible = true
                        val originalDelta = originalDeltaField.get(personalBest) as? Long

                        if (isPB && originalDelta != null) {
                            // This is a PB, but we have the original delta preserved
                            if (originalDelta < 0) {
                                "(-${formatTime(-originalDelta)}) PB"
                            } else if (originalDelta > 0) {
                                "(+${formatTime(originalDelta)}) PB"
                            } else {
                                "PB" // First time or no improvement
                            }
                        } else {
                            // Not a PB or no original delta preserved, calculate normally
                            val delta = completedSplit.time.segmentTime - personalBest.segmentTime
                            if (delta < 0) {
                                "(-${formatTime(-delta)})"
                            } else if (delta > 0) {
                                "(+${formatTime(delta)})"
                            } else {
                                "PB" // This IS the personal best!
                            }
                        }
                    } catch (e: Exception) {
                        // Fallback to original calculation if reflection fails
                        val delta = completedSplit.time.segmentTime - personalBest.segmentTime
                        if (delta < 0) {
                            "(-${formatTime(-delta)})"
                        } else if (delta > 0) {
                            "(+${formatTime(delta)})"
                        } else {
                            "PB" // This IS the personal best!
                        }
                    }
                } else {
                    ""
                }

                // Two-column layout: time | delta
                Column(
                    modifier = Modifier.width(120.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    // Time part (main line)
                    val isPersonalBest = isCompleted && personalBest != null && completedSplit != null &&
                                        completedSplit.time.segmentTime == personalBest.segmentTime

                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = when {
                                isPersonalBest -> Color(0xFF2196F3) // Blue for personal best
                                isCompleted -> TrackerColors.SplitCompleted
                                else -> TrackerColors.SplitPending
                            },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        textAlign = TextAlign.End
                    )

                    // Delta part (second line with milliseconds)
                    if (deltaText.isNotEmpty()) {
                        val delta = if (isCompleted && personalBest != null && completedSplit != null) {
                            completedSplit.time.totalTime - personalBest.totalTime
                        } else 0L

                        val deltaColor = when {
                            deltaText.contains("PB") -> Color(0xFF2196F3) // Blue for PB
                            delta < 0 || deltaText.startsWith("(-") -> Color(0xFF2196F3) // Blue for improvements (faster than PB)
                            delta == 0L -> Color(0xFF2196F3) // Blue for equal to PB
                            delta < 5000 -> TrackerColors.Warning // Slightly slower
                            else -> TrackerColors.Error // Much slower
                        }

                        Text(
                            text = deltaText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = deltaColor,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium,
                                fontSize = 10.sp
                            ),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
}

@Composable
fun PersonalBestSummary(
    splitsState: SplitsState,
    modifier: Modifier = Modifier
) {
    val currentProfilePB = splitsState.personalBests.values.firstOrNull()

    if (currentProfilePB != null) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(20.dp) // Match split row height
                .padding(horizontal = 4.dp, vertical = 1.dp), // Match split row padding
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Personal Best name (left side)
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Personal Best",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TrackerColors.OnSurface,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            // Personal Best time (right side)
            Row(
                modifier = Modifier.width(192.dp), // Match split row time column width
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(72.dp))
                Text(
                    text = formatTimeNoMillis(currentProfilePB.totalTime),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TrackerColors.OnSurface,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    ),
                    modifier = Modifier.width(120.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

/**
 * Map split names to sprite item IDs
 */
private fun getSplitItemId(split: Split): String {
    return when (split.name.lowercase()) {
        "ceres station" -> "ceres_station"
        "first missiles" -> "missile"
        "first super" -> "super_missile"
        "first power bomb" -> "power_bomb"
        "morph ball" -> "morph_ball"
        "bomb" -> "bomb"
        "charge beam" -> "charge"
        "spazer" -> "spazer"
        "varia suit" -> "varia"
        "hi-jump boots" -> "hijump"
        "speed booster" -> "speed_booster"
        "wave beam" -> "wave"
        "ice beam" -> "ice"
        "gravity suit" -> "gravity"
        "space jump" -> "space"
        "plasma beam" -> "plasma"
        "kraid" -> "kraid"
        "phantoon" -> "phantoon"
        "draygon" -> "draygon"
        "ridley" -> "ridley"
        "g4" -> "golden_four"
        "mother brain 1", "mb1" -> "mother_brain_1"
        "mother brain 2", "mb2" -> "mother_brain_2"
        "ship" -> "samus_ship"
        "spore spawn" -> "spore_spawn"
        "botwoon" -> "botwoon"
        "crocomire" -> "crocomire"
        "bomb torizo" -> "bomb_torizo"
        "golden torizo" -> "golden_torizo"
        else -> "missile" // Default fallback
    }
}

/**
 * Format time for deltas in M:SS.ss format (with fractional seconds, no leading zero for minutes)
 */
private fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val centiseconds = (timeMs % 1000) / 10

    return "%d:%02d.%02d".format(minutes, seconds, centiseconds)
}

/**
 * Format time for main times in H:MM:SS format (no leading zero for hours)
 */
private fun formatTimeNoMillis(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)  // No leading zero for hours
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
