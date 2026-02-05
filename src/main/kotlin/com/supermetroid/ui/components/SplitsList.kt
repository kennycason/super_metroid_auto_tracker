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
import com.supermetroid.model.Split
import com.supermetroid.model.SplitProfile
import com.supermetroid.model.SplitTime
import com.supermetroid.model.SplitsState
import com.supermetroid.service.SplitProfileService
import com.supermetroid.ui.theme.TrackerColors

@Composable
fun SplitsList(
    splitsState: SplitsState,
    autoSplitsEngine: AutoSplitsEngine,
    splitIconSizeService: com.supermetroid.service.SplitIconSizeService,
    splitDisplayModeService: com.supermetroid.service.SplitDisplayModeService,
    splitProfileService: SplitProfileService,
    modifier: Modifier = Modifier,
    maxHeight: Int = 400
) {
    // Get current profile from service
    val currentProfile by splitProfileService.currentProfile.collectAsState()
    val currentSplitIconSize by splitIconSizeService.currentSplitIconSize.collectAsState()
    val showSplitIcons by splitDisplayModeService.showSplitIcons.collectAsState()
    val showSplitNames by splitDisplayModeService.showSplitNames.collectAsState()
    val showSegmentDeltas by splitDisplayModeService.showSegmentDeltas.collectAsState()
    val showBestPossibleColumn by splitDisplayModeService.showBestPossibleColumn.collectAsState()
    val showBestPossibleDelta by splitDisplayModeService.showBestPossibleDelta.collectAsState()
    val showBestColumn by splitDisplayModeService.showBestColumn.collectAsState()
    val showAverageColumn by splitDisplayModeService.showAverageColumn.collectAsState()
    val showAverageDelta by splitDisplayModeService.showAverageDelta.collectAsState()
    val listState = rememberLazyListState()
    
    // Calculate average segment times for the CURRENT PROFILE (memoized to avoid recalculating on every recomposition)
    val averageSegmentTimes = remember(splitsState.runHistory, currentProfile.id) {
        autoSplitsEngine.getAverageSegmentTimes(currentProfile.id)
    }
    val coroutineScope = rememberCoroutineScope()

    // Get current split index for auto-scrolling
    val currentSplit = autoSplitsEngine.getCurrentSplit()
    val currentSplitIndex = currentProfile.splits.indexOfFirst { it.id == currentSplit?.id }

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
            SplitsHeader(
                splitsState = splitsState,
                autoSplitsEngine = autoSplitsEngine,
                profile = currentProfile,
                showBestPossibleColumn = showBestPossibleColumn,
                showBestPossibleDelta = showBestPossibleDelta,
                showBestColumn = showBestColumn,
                showAverageColumn = showAverageColumn,
                showAverageDelta = showAverageDelta
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Splits list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(maxHeight.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp) // 1.dp spacing between rows
            ) {
                itemsIndexed(currentProfile.splits) { index, split ->
                    SplitRow(
                        split = split,
                        splitIndex = index,
                        splitsState = splitsState,
                        autoSplitsEngine = autoSplitsEngine,
                        profile = currentProfile,
                        splitIconSize = currentSplitIconSize.size,
                        showIcon = showSplitIcons,
                        showName = showSplitNames,
                        showSegmentDeltas = showSegmentDeltas,
                        showBestPossibleColumn = showBestPossibleColumn,
                        showBestPossibleDelta = showBestPossibleDelta,
                        showBestColumn = showBestColumn,
                        showAverageColumn = showAverageColumn,
                        showAverageDelta = showAverageDelta,
                        averageSegmentTimes = averageSegmentTimes
                    )
                }
            }
        }
    }

@Composable
private fun SplitsHeader(
    splitsState: SplitsState,
    autoSplitsEngine: AutoSplitsEngine,
    profile: SplitProfile,
    showBestPossibleColumn: Boolean,
    showBestPossibleDelta: Boolean,
    showBestColumn: Boolean,
    showAverageColumn: Boolean,
    showAverageDelta: Boolean
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
                text = profile.name.uppercase(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TrackerColors.Primary,
                    letterSpacing = 1.sp
                )
            )
        }

        // Column headers (Best Possible | BP Δ | Average | Avg Δ | BEST | TIME)
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (showBestPossibleColumn) {
                Text(
                    text = "Best Possible",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TrackerColors.OnSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    modifier = Modifier.width(85.dp),
                    textAlign = TextAlign.End
                )
                Spacer(modifier = Modifier.width(2.dp))
            }
            if (showBestPossibleDelta) {
                Text(
                    text = "BP Δ",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TrackerColors.OnSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    modifier = Modifier.width(65.dp),
                    textAlign = TextAlign.End
                )
                Spacer(modifier = Modifier.width(2.dp))
            }
            if (showAverageColumn) {
                Text(
                    text = "Average",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TrackerColors.OnSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    modifier = Modifier.width(85.dp),
                    textAlign = TextAlign.End
                )
                Spacer(modifier = Modifier.width(2.dp))
            }
            if (showAverageDelta) {
                Text(
                    text = "Avg Δ",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TrackerColors.OnSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    modifier = Modifier.width(65.dp),
                    textAlign = TextAlign.End
                )
                Spacer(modifier = Modifier.width(2.dp))
            }
            if (showBestColumn) {
                Text(
                    text = "BEST",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TrackerColors.OnSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    modifier = Modifier.width(85.dp),
                    textAlign = TextAlign.End
                )
                Spacer(modifier = Modifier.width(2.dp))
            }
            Text(
                text = "TIME",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TrackerColors.OnSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                ),
                modifier = Modifier.width(85.dp),
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
    profile: SplitProfile,
    splitIconSize: Int,
    showIcon: Boolean,
    showName: Boolean,
    showSegmentDeltas: Boolean,
    showBestPossibleColumn: Boolean,
    showBestPossibleDelta: Boolean,
    showBestColumn: Boolean,
    showAverageColumn: Boolean,
    showAverageDelta: Boolean,
    averageSegmentTimes: Map<String, Long>
) {
    val currentRun = splitsState.currentRun
    val completedSplit = currentRun?.completedSplits?.find { it.splitId == split.id }
    val isCompleted = completedSplit != null
    val isActive = autoSplitsEngine.getCurrentSplit()?.id == split.id

    // Use the current profile's ID when currentRun is null to ensure columns are always shown for the selected profile
    val profileId = currentRun?.profileId ?: profile.id
    
    // Get average segment time for this split
    val averageSegmentTime = averageSegmentTimes[split.id]
    
    // Calculate sum of average segments up to this point
    val sumOfAveragesUpToHere = profile.splits.take(splitIndex + 1).sumOf { s ->
        averageSegmentTimes[s.id] ?: 0L
    }
    
    // Calculate Average Delta (segment vs average segment)
    val averageDelta = if (isCompleted && averageSegmentTime != null && averageSegmentTime > 0) {
        completedSplit!!.time.segmentTime - averageSegmentTime
    } else null
    val personalBest = splitsState.personalBests[profileId]
    
    // Get the best segment time for this split (Best Possible - from completed runs only)
    val bestSegmentTime = personalBest?.splitTimes?.get(split.id)
    
    // Get the PB run's segment time for this split (BEST - from Personal Best run)
    val pbRunSegmentTime = getPbRunSegmentTime(splitsState, profileId, profile, split.id)

    // Calculate sum of best segments up to this point for Best Possible column
    val profileSplitTimes = personalBest?.splitTimes
    val sumOfBestPossibleUpToHere = if (profileSplitTimes != null) {
        profile.splits.take(splitIndex + 1).sumOf { s ->
            profileSplitTimes[s.id]?.segmentTime ?: 0L
        }
    } else {
        0L
    }
    
    // Calculate sum of PB run's segment times up to this point for BEST column
    val sumOfPbRunUpToHere = calculatePbRunTimeUpTo(splitsState, profileId, profile, splitIndex)

    // Only show Best Possible if THIS split has a best segment time
    val showBestPossible = (bestSegmentTime?.segmentTime ?: 0L) > 0
    
    // Calculate Best Possible Delta (segment-by-segment comparison)
    // Shows how much time was gained/lost on THIS segment vs the best segment time
    // Negative (green) = faster than best, Positive (red) = slower than best
    val bestPossibleDelta = if (isCompleted && bestSegmentTime != null) {
        completedSplit!!.time.segmentTime - bestSegmentTime.segmentTime
    } else null

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

            // Times section (Best Possible | BP Delta | BEST | TIME, right-aligned)
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp), // Match header spacing
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Best Possible Column (theoretical best from COMPLETED runs only)
                if (showBestPossibleColumn) {
                    Column(
                        modifier = Modifier.width(85.dp), // Match header width
                        horizontalAlignment = Alignment.End
                    ) {
                        // Sum of best segments up to this point (main line)
                        Text(
                            text = if (showBestPossible && sumOfBestPossibleUpToHere > 0) {
                                formatTimeWithMillis(sumOfBestPossibleUpToHere)
                            } else {
                                "--:--:--.--"
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TrackerColors.OnSurfaceVariant,
                                fontFamily = FontFamily.Monospace,
                                fontSize = if (showSegmentDeltas) 10.sp else 11.sp
                            ),
                            textAlign = TextAlign.End
                        )

                        // Best segment time for this split (second line) - conditionally shown
                        if (showSegmentDeltas && (bestSegmentTime?.segmentTime ?: 0L) > 0) {
                            Text(
                                text = formatTime(bestSegmentTime!!.segmentTime),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TrackerColors.OnSurfaceVariant.copy(alpha = 0.7f),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp
                                ),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }

                // Best Possible Delta Column (segment vs best segment)
                if (showBestPossibleDelta) {
                    Column(
                        modifier = Modifier.width(65.dp), // Match header width
                        horizontalAlignment = Alignment.End
                    ) {
                        if (bestPossibleDelta != null) {
                            val deltaColor = when {
                                bestPossibleDelta < 0 -> Color(0xFF00FF00) // Green for ahead
                                bestPossibleDelta > 0 -> Color(0xFFFF4444) // Red for behind
                                else -> Color(0xFFFFD700) // Gold for exactly on pace
                            }
                            val deltaText = when {
                                bestPossibleDelta < 0 -> "-${formatTime(-bestPossibleDelta)}"
                                bestPossibleDelta > 0 -> "+${formatTime(bestPossibleDelta)}"
                                else -> "±0:00"
                            }
                            Text(
                                text = deltaText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = deltaColor,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                textAlign = TextAlign.End
                            )
                        } else {
                            Text(
                                text = "--:--",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TrackerColors.OnSurfaceVariant.copy(alpha = 0.5f),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                ),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }

                // Average Column (mean time from all completed runs)
                if (showAverageColumn) {
                    Column(
                        modifier = Modifier.width(85.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        // Sum of average segments up to this point (main line)
                        Text(
                            text = if (sumOfAveragesUpToHere > 0) {
                                formatTimeWithMillis(sumOfAveragesUpToHere)
                            } else {
                                "--:--:--.--"
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TrackerColors.OnSurfaceVariant,
                                fontFamily = FontFamily.Monospace,
                                fontSize = if (showSegmentDeltas) 10.sp else 11.sp
                            ),
                            textAlign = TextAlign.End
                        )

                        // Average segment time for this split (second line) - conditionally shown
                        if (showSegmentDeltas && (averageSegmentTime ?: 0L) > 0) {
                            Text(
                                text = formatTime(averageSegmentTime!!),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TrackerColors.OnSurfaceVariant.copy(alpha = 0.7f),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp
                                ),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }

                // Average Delta Column (segment vs average segment)
                if (showAverageDelta) {
                    Column(
                        modifier = Modifier.width(65.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        if (averageDelta != null) {
                            val deltaColor = when {
                                averageDelta < 0 -> Color(0xFF00FF00) // Green for ahead of average
                                averageDelta > 0 -> Color(0xFFFF4444) // Red for behind average
                                else -> Color(0xFFFFD700) // Gold for exactly on average
                            }
                            val deltaText = when {
                                averageDelta < 0 -> "-${formatTime(-averageDelta)}"
                                averageDelta > 0 -> "+${formatTime(averageDelta)}"
                                else -> "±0:00"
                            }
                            Text(
                                text = deltaText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = deltaColor,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                textAlign = TextAlign.End
                            )
                        } else {
                            Text(
                                text = "--:--",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TrackerColors.OnSurfaceVariant.copy(alpha = 0.5f),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                ),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }

                // BEST Column (Personal Best run's segment time)
                if (showBestColumn) {
                    Column(
                        modifier = Modifier.width(85.dp), // Match header width
                        horizontalAlignment = Alignment.End
                    ) {
                        // PB run's total time up to this point (main line)
                        Text(
                            text = if (sumOfPbRunUpToHere > 0) {
                                formatTimeWithMillis(sumOfPbRunUpToHere)
                            } else {
                                "--:--:--.--"
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TrackerColors.OnSurfaceVariant.copy(alpha = 0.9f),
                                fontFamily = FontFamily.Monospace,
                                fontSize = if (showSegmentDeltas) 10.sp else 11.sp
                            ),
                            textAlign = TextAlign.End
                        )

                        // PB run's segment time for this split (second line) - conditionally shown
                        if (showSegmentDeltas && (pbRunSegmentTime?.segmentTime ?: 0L) > 0) {
                            Text(
                                text = formatTime(pbRunSegmentTime!!.segmentTime),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TrackerColors.OnSurfaceVariant.copy(alpha = 0.7f),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp
                                ),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }

                // TIME Column (Current run time)
                Column(
                    modifier = Modifier.width(85.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    // Current time (main line) with milliseconds
                    val timeText = if (isCompleted) {
                        formatTimeWithMillis(completedSplit!!.time.totalTime)
                    } else {
                        "--:--:--.--"
                    }

                    // Calculate delta against best segment time for coloring
                    val delta = if (isCompleted && bestSegmentTime != null && completedSplit != null) {
                        completedSplit.time.segmentTime - bestSegmentTime.segmentTime
                    } else 0L

                    // Determine color: gold for good times (PB or ahead), red for bad times (behind)
                    val timeColor = if (isCompleted && bestSegmentTime != null) {
                        when {
                            delta <= 0 -> Color(0xFFFFD700) // Gold for PB or ahead
                            else -> Color(0xFFFF4444) // Red for behind
                        }
                    } else if (isCompleted) {
                        TrackerColors.SplitCompleted
                    } else {
                        TrackerColors.SplitPending
                    }

                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = timeColor,
                            fontFamily = FontFamily.Monospace,
                            fontSize = if (showSegmentDeltas) 10.sp else 11.sp
                        ),
                        textAlign = TextAlign.End
                    )

                    // Segment time (second line) - conditionally shown
                    // Shows the actual segment time for this split (consistent with Best Possible and Average columns)
                    if (showSegmentDeltas && isCompleted && completedSplit != null) {
                        val segmentTime = completedSplit.time.segmentTime

                        // Color based on whether this segment beat the best segment
                        val segmentColor = when {
                            bestSegmentTime != null && segmentTime <= bestSegmentTime.segmentTime -> 
                                Color(0xFFFFD700) // Gold for PB or improvements
                            bestSegmentTime != null -> Color(0xFFFF4444) // Red for slower
                            else -> TrackerColors.OnSurfaceVariant.copy(alpha = 0.7f)
                        }

                        Text(
                            text = formatTime(segmentTime),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = segmentColor,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp
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
    profileId: String,
    modifier: Modifier = Modifier
) {
    // Get the PB for the CURRENT profile only, not just any profile
    val currentProfilePB = splitsState.personalBests[profileId]

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

            // Personal Best time (right side) with milliseconds
            Row(
                modifier = Modifier.width(192.dp), // Match split row time column width
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(72.dp))
                Text(
                    text = formatTimeWithMillis(currentProfilePB.totalTime),
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
        "metroid 1", "metroid1" -> "metroid1"
        "metroid 2", "metroid2" -> "metroid2"
        "metroid 3", "metroid3" -> "metroid3"
        "metroid 4", "metroid4" -> "metroid4"
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
 * Find the actual Personal Best run - the fastest complete run in history
 * This is more reliable than using personalBest.runSessionId which may be stale
 */
private fun findActualPbRun(splitsState: SplitsState, profileId: String, profile: SplitProfile): com.supermetroid.model.RunSession? {
    // Find all complete runs for this profile
    val completeRuns = splitsState.runHistory.filter { run ->
        run.profileId == profileId && 
        run.endTime != null && 
        run.completedSplits.size == profile.splits.size
    }
    
    // Return the fastest one
    return completeRuns.minByOrNull { it.totalTime }
}

/**
 * Get the Personal Best run's segment time for a specific split
 */
private fun getPbRunSegmentTime(splitsState: SplitsState, profileId: String, profile: SplitProfile, splitId: String): SplitTime? {
    // Find the actual PB run (fastest complete run)
    val pbRun = findActualPbRun(splitsState, profileId, profile) ?: return null
    
    // Find the completed split for this splitId
    val completedSplit = pbRun.completedSplits.find { it.splitId == splitId } ?: return null
    
    return completedSplit.time
}

/**
 * Calculate the sum of PB run's segment times up to (and including) the given split index
 */
private fun calculatePbRunTimeUpTo(splitsState: SplitsState, profileId: String, profile: SplitProfile, splitIndex: Int): Long {
    // Find the actual PB run (fastest complete run)
    val pbRun = findActualPbRun(splitsState, profileId, profile) ?: return 0L
    
    // Sum up the segment times for all splits up to splitIndex
    var totalTime = 0L
    for (i in 0..splitIndex) {
        val split = profile.splits.getOrNull(i) ?: continue
        val completedSplit = pbRun.completedSplits.find { it.splitId == split.id }
        if (completedSplit != null) {
            // Use the total time from the completed split (cumulative time up to this split)
            totalTime = completedSplit.time.totalTime
        }
    }
    
    return totalTime
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
 * Format time with milliseconds in H:MM:SS.ss or MM:SS.ss format
 */
private fun formatTimeWithMillis(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val centiseconds = (timeMs % 1000) / 10

    return if (hours > 0) {
        "%d:%02d:%02d.%02d".format(hours, minutes, seconds, centiseconds)  // No leading zero for hours
    } else {
        "%02d:%02d.%02d".format(minutes, seconds, centiseconds)
    }
}

/**
 * Format time for main times in H:MM:SS format (no leading zero for hours)
 * @deprecated Use formatTimeWithMillis instead
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
