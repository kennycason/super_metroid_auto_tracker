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
import androidx.compose.ui.text.style.TextOverflow
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
import kotlin.math.absoluteValue
import kotlin.math.min

/** Gold color for PB / best segment / ahead of best run */
private val Gold = Color(0xFFFFD700)
private val FullGreen = Color(0xFF00DD00)
private val FullRed = Color(0xFFFF4444)

// LiveSplit semantic colors for cumulative delta columns
private val AheadGaining = Color(0xFF00CC36)   // Bright green — ahead and gap widening
private val AheadLosing = Color(0xFF6BD688)    // Light green — ahead but gap shrinking
private val BehindGaining = Color(0xFFD68B6B)  // Light red — behind but gap shrinking
private val BehindLosing = Color(0xFFCC0000)   // Bright red — behind and gap widening

/**
 * Compute a gradient delta color:
 *   - negative delta (ahead) → white..green, saturating at [maxDeltaMs]
 *   - positive delta (behind) → white..red, saturating at [maxDeltaMs]
 *   - exactly zero → gold
 *   - beating best segment → gold
 *
 * [maxDeltaMs] controls how quickly the color saturates (default 30s).
 */
fun deltaGradientColor(deltaMs: Long, isBestSegment: Boolean = false, maxDeltaMs: Long = 30_000): Color {
    if (isBestSegment) return Gold
    if (deltaMs == 0L) return Color.White
    val fraction = min(deltaMs.absoluteValue.toFloat() / maxDeltaMs, 1f)
    val target = if (deltaMs < 0) FullGreen else FullRed
    return lerpColor(Color.White, target, fraction)
}

/**
 * LiveSplit 5-color model for cumulative delta columns.
 * Gold overrides everything. Then ahead/behind × gaining/losing based on
 * whether the gap vs the previous split's delta is widening or shrinking.
 */
fun liveSplitDeltaColor(currentDelta: Long, previousDelta: Long?, isBestSegment: Boolean): Color {
    if (isBestSegment) return Gold
    return when {
        currentDelta < 0L -> {
            if (previousDelta != null && currentDelta < previousDelta) AheadGaining
            else AheadLosing
        }
        currentDelta > 0L -> {
            if (previousDelta != null && currentDelta > previousDelta) BehindLosing
            else BehindGaining
        }
        else -> Color.White
    }
}

/** Simple RGB lerp between two colors. */
fun lerpColor(a: Color, b: Color, t: Float): Color {
    return Color(
        red = a.red + (b.red - a.red) * t,
        green = a.green + (b.green - a.green) * t,
        blue = a.blue + (b.blue - a.blue) * t,
        alpha = 1f
    )
}

@Composable
fun SplitsList(
    splitsState: SplitsState,
    autoSplitsEngine: AutoSplitsEngine,
    splitIconSizeService: com.supermetroid.service.SplitIconSizeService,
    splitDisplayModeService: com.supermetroid.service.SplitDisplayModeService,
    splitProfileService: SplitProfileService,
    splitsFontSizeService: com.supermetroid.service.SplitsFontSizeService,
    modifier: Modifier = Modifier,
    maxHeight: Int = 400
) {
    // Get current profile from service
    val currentProfile by splitProfileService.currentProfile.collectAsState()
    val currentSplitIconSize by splitIconSizeService.currentSplitIconSize.collectAsState()
    val splitsFontSize by splitsFontSizeService.fontSize.collectAsState()
    val showSplitIcons by splitDisplayModeService.showSplitIcons.collectAsState()
    val showSplitNames by splitDisplayModeService.showSplitNames.collectAsState()
    val showSegmentDeltas by splitDisplayModeService.showSegmentDeltas.collectAsState()
    val showBestPossibleColumn by splitDisplayModeService.showBestPossibleColumn.collectAsState()
    val showBestPossibleDelta by splitDisplayModeService.showBestPossibleDelta.collectAsState()
    val showBestColumn by splitDisplayModeService.showBestColumn.collectAsState()
    val showAverageColumn by splitDisplayModeService.showAverageColumn.collectAsState()
    val showAverageDelta by splitDisplayModeService.showAverageDelta.collectAsState()
    val showBestPossibleDeltaTotal by splitDisplayModeService.showBestPossibleDeltaTotal.collectAsState()
    val showBestDelta by splitDisplayModeService.showBestDelta.collectAsState()
    val showBestDeltaTotal by splitDisplayModeService.showBestDeltaTotal.collectAsState()
    val showGameName by splitDisplayModeService.showGameName.collectAsState()
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
                showGameName = showGameName,
                showBestPossibleColumn = showBestPossibleColumn,
                showBestPossibleDelta = showBestPossibleDelta,
                showBestPossibleDeltaTotal = showBestPossibleDeltaTotal,
                showBestColumn = showBestColumn,
                showBestDelta = showBestDelta,
                showBestDeltaTotal = showBestDeltaTotal,
                showAverageColumn = showAverageColumn,
                showAverageDelta = showAverageDelta,
                fontSize = splitsFontSize
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Splits list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight.dp),
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
                        showBestPossibleDeltaTotal = showBestPossibleDeltaTotal,
                        showBestColumn = showBestColumn,
                        showBestDelta = showBestDelta,
                        showBestDeltaTotal = showBestDeltaTotal,
                        showAverageColumn = showAverageColumn,
                        showAverageDelta = showAverageDelta,
                        averageSegmentTimes = averageSegmentTimes,
                        fontSize = splitsFontSize
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
    showGameName: Boolean,
    showBestPossibleColumn: Boolean,
    showBestPossibleDelta: Boolean,
    showBestPossibleDeltaTotal: Boolean,
    showBestColumn: Boolean,
    showBestDelta: Boolean,
    showBestDeltaTotal: Boolean,
    showAverageColumn: Boolean,
    showAverageDelta: Boolean,
    fontSize: com.supermetroid.model.SplitsFontSize
) {
    @Suppress("UNUSED_PARAMETER") // splitsState might be used in future
    val currentSplit = autoSplitsEngine.getCurrentSplit()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showGameName) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name.uppercase(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TrackerColors.Primary,
                        letterSpacing = 1.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
        }

        // Column headers
        Row(
            horizontalArrangement = Arrangement.End
        ) {
            if (showBestPossibleColumn) {
                Text(
                    text = "Best Possible",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TrackerColors.OnSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = fontSize.time.sp
                    ),
                    modifier = Modifier.width(85.dp),
                    textAlign = TextAlign.End
                )
                Spacer(modifier = Modifier.width(2.dp))
            }
            if (showBestPossibleDelta) {
                Text(
                    text = "BP +/-",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TrackerColors.OnSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = fontSize.time.sp
                    ),
                    modifier = Modifier.width(65.dp),
                    textAlign = TextAlign.End
                )
                Spacer(modifier = Modifier.width(2.dp))
            }
            if (showBestPossibleDeltaTotal) {
                Text(
                    text = "BP +/-",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TrackerColors.OnSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = fontSize.time.sp
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
                        fontSize = fontSize.time.sp
                    ),
                    modifier = Modifier.width(85.dp),
                    textAlign = TextAlign.End
                )
                Spacer(modifier = Modifier.width(2.dp))
            }
            if (showAverageDelta) {
                Text(
                    text = "Avg +/-",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TrackerColors.OnSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = fontSize.time.sp
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
                        fontSize = fontSize.time.sp
                    ),
                    modifier = Modifier.width(85.dp),
                    textAlign = TextAlign.End
                )
                Spacer(modifier = Modifier.width(2.dp))
            }
            if (showBestDelta) {
                Text(
                    text = "Best +/-",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TrackerColors.OnSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = fontSize.time.sp
                    ),
                    modifier = Modifier.width(65.dp),
                    textAlign = TextAlign.End
                )
                Spacer(modifier = Modifier.width(2.dp))
            }
            if (showBestDeltaTotal) {
                Text(
                    text = "Best +/-",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TrackerColors.OnSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = fontSize.time.sp
                    ),
                    modifier = Modifier.width(65.dp),
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
    showBestPossibleDeltaTotal: Boolean,
    showBestColumn: Boolean,
    showBestDelta: Boolean,
    showBestDeltaTotal: Boolean,
    showAverageColumn: Boolean,
    showAverageDelta: Boolean,
    averageSegmentTimes: Map<String, Long>,
    fontSize: com.supermetroid.model.SplitsFontSize
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

    // BP +/- Total: cumulative delta vs sum of best segments
    val bpTotalDelta = if (isCompleted && completedSplit != null && sumOfBestPossibleUpToHere > 0) {
        completedSplit.time.totalTime - sumOfBestPossibleUpToHere
    } else null

    // BP +/- Total previous delta (for LiveSplit gaining/losing color)
    val prevBpTotalDelta = if (isCompleted && splitIndex > 0 && profileSplitTimes != null) {
        val prevSplit = profile.splits.getOrNull(splitIndex - 1)
        val prevCompleted = prevSplit?.let { currentRun?.completedSplits?.find { c -> c.splitId == it.id } }
        if (prevCompleted != null) {
            val prevSumBp = profile.splits.take(splitIndex).sumOf { s ->
                profileSplitTimes[s.id]?.segmentTime ?: 0L
            }
            if (prevSumBp > 0) prevCompleted.time.totalTime - prevSumBp else null
        } else null
    } else null

    // Best +/- (segment): current segment vs PB run's segment
    val bestDelta = if (isCompleted && completedSplit != null && pbRunSegmentTime != null) {
        completedSplit.time.segmentTime - pbRunSegmentTime.segmentTime
    } else null

    // Best +/- Total: cumulative delta vs PB run
    val bestTotalDelta = if (isCompleted && completedSplit != null && sumOfPbRunUpToHere > 0) {
        completedSplit.time.totalTime - sumOfPbRunUpToHere
    } else null

    // Best +/- Total previous delta (for LiveSplit gaining/losing color)
    val prevBestTotalDelta = if (isCompleted && splitIndex > 0) {
        val prevSplit = profile.splits.getOrNull(splitIndex - 1)
        val prevCompleted = prevSplit?.let { currentRun?.completedSplits?.find { c -> c.splitId == it.id } }
        if (prevCompleted != null) {
            val prevSumPb = calculatePbRunTimeUpTo(splitsState, profileId, profile, splitIndex - 1)
            if (prevSumPb > 0) prevCompleted.time.totalTime - prevSumPb else null
        } else null
    } else null

    val isNewBestSegment = isCompleted && bestSegmentTime != null && completedSplit != null &&
        completedSplit.time.segmentTime < bestSegmentTime.segmentTime

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

    // The existing tiers retain their current compact rows. Very Large needs
    // enough vertical room for its main time and segment-delta lines.
    val minimumRowHeight = if (
        fontSize == com.supermetroid.model.SplitsFontSize.VERY_LARGE
    ) 36 else 24
    val rowHeight = kotlin.math.max(splitIconSize, minimumRowHeight) + 2

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
                            fontSize = fontSize.text.sp
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
                                fontSize = if (showSegmentDeltas) fontSize.time.sp else (fontSize.time + 1).sp
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
                                    fontSize = fontSize.detail.sp
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
                            val deltaColor = deltaGradientColor(bestPossibleDelta)
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
                                    fontSize = fontSize.time.sp,
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
                                    fontSize = fontSize.time.sp
                                ),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }

                // BP +/- Total Column (cumulative delta vs sum of best segments, LiveSplit colors)
                if (showBestPossibleDeltaTotal) {
                    Column(
                        modifier = Modifier.width(65.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        if (bpTotalDelta != null) {
                            val deltaColor = liveSplitDeltaColor(bpTotalDelta, prevBpTotalDelta, isNewBestSegment)
                            val deltaText = when {
                                bpTotalDelta < 0 -> "-${formatTime(-bpTotalDelta)}"
                                bpTotalDelta > 0 -> "+${formatTime(bpTotalDelta)}"
                                else -> "±0:00"
                            }
                            Text(
                                text = deltaText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = deltaColor,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = fontSize.time.sp,
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
                                    fontSize = fontSize.time.sp
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
                                fontSize = if (showSegmentDeltas) fontSize.time.sp else (fontSize.time + 1).sp
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
                                    fontSize = fontSize.detail.sp
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
                            val deltaColor = deltaGradientColor(averageDelta)
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
                                    fontSize = fontSize.time.sp,
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
                                    fontSize = fontSize.time.sp
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
                                fontSize = if (showSegmentDeltas) fontSize.time.sp else (fontSize.time + 1).sp
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
                                    fontSize = fontSize.detail.sp
                                ),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }

                // Best +/- Column (segment delta vs PB run's segment)
                if (showBestDelta) {
                    Column(
                        modifier = Modifier.width(65.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        if (bestDelta != null) {
                            val isBestSeg = isNewBestSegment
                            val deltaColor = deltaGradientColor(bestDelta, isBestSegment = isBestSeg)
                            val deltaText = when {
                                bestDelta < 0 -> "-${formatTime(-bestDelta)}"
                                bestDelta > 0 -> "+${formatTime(bestDelta)}"
                                else -> "±0:00"
                            }
                            Text(
                                text = deltaText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = deltaColor,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = fontSize.time.sp,
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
                                    fontSize = fontSize.time.sp
                                ),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }

                // Best +/- Total Column (cumulative delta vs PB run, LiveSplit colors)
                if (showBestDeltaTotal) {
                    Column(
                        modifier = Modifier.width(65.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        if (bestTotalDelta != null) {
                            val deltaColor = liveSplitDeltaColor(bestTotalDelta, prevBestTotalDelta, isNewBestSegment)
                            val deltaText = when {
                                bestTotalDelta < 0 -> "-${formatTime(-bestTotalDelta)}"
                                bestTotalDelta > 0 -> "+${formatTime(bestTotalDelta)}"
                                else -> "±0:00"
                            }
                            Text(
                                text = deltaText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = deltaColor,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = fontSize.time.sp,
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
                                    fontSize = fontSize.time.sp
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

                    val pbCumulativeDelta = if (isCompleted && completedSplit != null && sumOfPbRunUpToHere > 0) {
                        completedSplit.time.totalTime - sumOfPbRunUpToHere
                    } else 0L

                    val timeColor = if (isCompleted && isNewBestSegment) {
                        Gold // New best segment
                    } else if (isCompleted && sumOfPbRunUpToHere > 0) {
                        deltaGradientColor(pbCumulativeDelta) // Ahead/behind PB
                    } else if (isCompleted) {
                        Gold // First run = every segment is a best
                    } else {
                        TrackerColors.SplitPending
                    }

                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = timeColor,
                            fontFamily = FontFamily.Monospace,
                            fontSize = if (showSegmentDeltas) fontSize.time.sp else (fontSize.time + 1).sp
                        ),
                        textAlign = TextAlign.End
                    )

                    // Segment time (second line) - conditionally shown
                    // Shows the actual segment time for this split (consistent with Best Possible and Average columns)
                    if (showSegmentDeltas && isCompleted && completedSplit != null) {
                        val segmentTime = completedSplit.time.segmentTime

                        // Color based on how this segment compares to best segment
                        val segmentColor = if (bestSegmentTime != null) {
                            val segDelta = segmentTime - bestSegmentTime.segmentTime
                            deltaGradientColor(segDelta, isBestSegment = segDelta < 0)
                        } else {
                            TrackerColors.OnSurfaceVariant.copy(alpha = 0.7f)
                        }

                        Text(
                            text = formatTime(segmentTime),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = segmentColor,
                                fontFamily = FontFamily.Monospace,
                                fontSize = fontSize.detail.sp
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
    profile: SplitProfile,
    splitsFontSizeService: com.supermetroid.service.SplitsFontSizeService,
    showBestPossible: Boolean = true,
    modifier: Modifier = Modifier
) {
    val fontSize by splitsFontSizeService.fontSize.collectAsState()
    val currentProfilePB = splitsState.personalBests[profileId]

    if (currentProfilePB != null) {
        // Personal Best row
        SummaryRow(
            label = "Personal Best",
            timeMs = currentProfilePB.totalTime,
            color = TrackerColors.OnSurface,
            fontSize = fontSize,
            modifier = modifier
        )

        // Best Possible row: completed segments actual time + best segments for remaining
        if (!showBestPossible) return
        val currentRun = splitsState.currentRun
        val bestSegmentTimes = currentProfilePB.splitTimes
        if (bestSegmentTimes.isNotEmpty()) {
            var bestPossibleTime = 0L
            var hasData = false
            for (split in profile.splits) {
                val completed = currentRun?.completedSplits?.find { it.splitId == split.id }
                if (completed != null) {
                    bestPossibleTime += completed.time.segmentTime
                    hasData = true
                } else {
                    val bestSeg = bestSegmentTimes[split.id]?.segmentTime
                    if (bestSeg != null && bestSeg > 0) {
                        bestPossibleTime += bestSeg
                        hasData = true
                    }
                }
            }
            if (hasData && bestPossibleTime > 0) {
                SummaryRow(
                    label = "Best Possible",
                    timeMs = bestPossibleTime,
                    color = TrackerColors.OnSurface,
                    fontSize = fontSize,
                    modifier = modifier
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    timeMs: Long,
    color: Color,
    fontSize: com.supermetroid.model.SplitsFontSize,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = color,
                fontWeight = FontWeight.Medium,
                fontSize = fontSize.summary.sp
            )
        )
        Text(
            text = formatTimeWithMillis(timeMs),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = color,
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize.summary.sp
            ),
            textAlign = TextAlign.End
        )
    }
}

/**
 * Maps a split's ID to the sprite item ID used by [SpriteIcon].
 * Split IDs are already normalized by [LiveSplitConverter.deriveSplitId] for imported
 * splits, and match our built-in profile IDs for native profiles.
 */
private val splitIdToSpriteId: Map<String, String> = mapOf(
    "ceres_station" to "ceres_station",
    "energy_tank" to "energy_tank",
    "morph_ball" to "morph_ball",
    "first_missile" to "missile",
    "first_super" to "super_missile",
    "first_power_bomb" to "power_bomb",
    "bomb" to "bomb",
    "charge_beam" to "charge",
    "spazer" to "spazer",
    "varia_suit" to "varia",
    "hi_jump" to "hijump",
    "speed_booster" to "speed_booster",
    "wave_beam" to "wave",
    "ice_beam" to "ice",
    "gravity_suit" to "gravity",
    "space_jump" to "space",
    "plasma_beam" to "plasma",
    "screw_attack" to "screw",
    "grapple_beam" to "grapple",
    "spring_ball" to "spring",
    "xray_scope" to "xray",
    "reserve_tank" to "reserve_tank",
    "kraid" to "kraid",
    "phantoon" to "phantoon",
    "draygon" to "draygon",
    "ridley" to "ridley",
    "golden_four" to "golden_four",
    "mother_brain_1" to "mother_brain_1",
    "mother_brain_2" to "mother_brain_2",
    "ship" to "samus_ship",
    "spore_spawn" to "spore_spawn",
    "botwoon" to "botwoon",
    "crocomire" to "crocomire",
    "bomb_torizo" to "bomb_torizo",
    "golden_torizo" to "golden_torizo",
    "metroid1" to "metroid1",
    "metroid2" to "metroid2",
    "metroid3" to "metroid3",
    "metroid4" to "metroid4"
)

/**
 * Contains-based name patterns, checked in priority order.
 * More specific patterns come before general ones (e.g. "super missile" before "missile").
 */
private val nameContainsPatterns: List<Pair<String, String>> = listOf(
    "ceres" to "ceres_station",
    "morph" to "morph_ball",
    "bomb torizo" to "bomb_torizo",
    "golden torizo" to "golden_torizo",
    "power bomb" to "power_bomb",
    "bomb" to "bomb",
    "super missile" to "super_missile",
    "super" to "super_missile",
    "charge" to "charge",
    "spazer" to "spazer",
    "varia" to "varia",
    "hi-jump" to "hijump",
    "hi jump" to "hijump",
    "speed" to "speed_booster",
    "wave" to "wave",
    "ice" to "ice",
    "gravity" to "gravity",
    "space jump" to "space",
    "plasma" to "plasma",
    "screw" to "screw",
    "grapple" to "grapple",
    "spring" to "spring",
    "x-ray" to "xray",
    "xray" to "xray",
    "reserve" to "reserve_tank",
    "missile" to "missile",
    "mother brain 2" to "mother_brain_2",
    "mother brain 1" to "mother_brain_1",
    "mother brain" to "mother_brain_1",
    "mb2" to "mother_brain_2",
    "mb1" to "mother_brain_1",
    "kraid" to "kraid",
    "phantoon" to "phantoon",
    "phaaan" to "phantoon",
    "draygon" to "draygon",
    "ridley" to "ridley",
    "spore spawn" to "spore_spawn",
    "botwoon" to "botwoon",
    "crocomire" to "crocomire",
    "metroid 4" to "metroid4",
    "metroid 3" to "metroid3",
    "metroid 2" to "metroid2",
    "metroid 1" to "metroid1",
    "golden four" to "golden_four",
    "tourian" to "golden_four",
    "g4" to "golden_four",
    "ship" to "samus_ship",
    "escape" to "samus_ship",
    "done" to "samus_ship"
)

/**
 * Resolve the sprite item ID for a split. Tries in order:
 * 1. Direct split.id lookup (works for built-in profiles and well-mapped LSS imports)
 * 2. Contains-based name matching (handles arbitrary LiveSplit segment names)
 * 3. Falls back to "missile" placeholder
 */
internal fun getSplitItemId(split: Split): String {
    splitIdToSpriteId[split.id]?.let { return it }

    val name = split.name.lowercase()
    for ((pattern, spriteId) in nameContainsPatterns) {
        if (name.contains(pattern)) return spriteId
    }

    return "missile"
}

/**
 * Find the actual Personal Best run from run history.
 * First tries matching by personalBest.runSessionId, then falls back to fastest complete run.
 */
private fun findActualPbRun(splitsState: SplitsState, profileId: String, profile: SplitProfile): com.supermetroid.model.RunSession? {
    val pb = splitsState.personalBests[profileId]

    // First: try to find by the stored PB run session ID
    if (pb != null && pb.runSessionId.isNotBlank()) {
        val byId = splitsState.runHistory.find { it.id == pb.runSessionId }
        if (byId != null) return byId
    }

    // Fallback: fastest complete run for this profile
    val completeRuns = splitsState.runHistory.filter { run ->
        run.profileId == profileId &&
        run.endTime != null &&
        run.completedSplits.size == profile.splits.size
    }
    return completeRuns.minByOrNull { it.totalTime }
}

/**
 * Get the Personal Best run's segment time for a specific split.
 * Looks up by splitId in the PB run's completed splits (order-independent).
 */
private fun getPbRunSegmentTime(splitsState: SplitsState, profileId: String, profile: SplitProfile, splitId: String): SplitTime? {
    val pbRun = findActualPbRun(splitsState, profileId, profile) ?: return null
    val completedSplit = pbRun.completedSplits.find { it.splitId == splitId } ?: return null
    return completedSplit.time
}

/**
 * Get the PB run's cumulative time at the given split index.
 * Uses the split's totalTime directly (the authoritative cumulative time from the PB run)
 * rather than summing segment times, because segment times in PersonalBest represent
 * best segments across ALL runs, not the PB run's actual segments.
 *
 * Returns 0 (displayed as "--") when the cumulative time is non-monotonic (i.e., less than
 * a previous split's cumulative), which indicates corrupted or reset PB data in the LSS file.
 */
private fun calculatePbRunTimeUpTo(splitsState: SplitsState, profileId: String, profile: SplitProfile, splitIndex: Int): Long {
    val pbRun = findActualPbRun(splitsState, profileId, profile) ?: return 0L
    val splitTimeMap = pbRun.completedSplits.associate { it.splitId to it.time.totalTime }

    val split = profile.splits.getOrNull(splitIndex) ?: return 0L
    val cumulative = splitTimeMap[split.id] ?: return 0L

    // Validate monotonicity: cumulative must be >= all previous splits' cumulative times
    // Non-monotonic data means corrupted/reset PB splits (e.g., LiveSplit segment reordering)
    for (i in 0 until splitIndex) {
        val prevSplit = profile.splits.getOrNull(i) ?: continue
        val prevCumulative = splitTimeMap[prevSplit.id] ?: continue
        if (prevCumulative > cumulative) {
            return 0L
        }
    }

    return cumulative
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
