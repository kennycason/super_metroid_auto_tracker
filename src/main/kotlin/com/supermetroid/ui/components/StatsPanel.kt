@file:OptIn(ExperimentalFoundationApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.supermetroid.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermetroid.model.SplitProfile
import com.supermetroid.model.SplitsState
import com.supermetroid.model.StatsFontSize
import com.supermetroid.service.StatsFontSizeService
import com.supermetroid.service.StatsService
import com.supermetroid.ui.theme.TrackerColors

private val GoldColor = Color(0xFFFFD700)

/** Interpolate from red (0.0) through yellow (0.5) to green (1.0) */
private fun redToGreenGradient(fraction: Float): Color {
    val clamped = fraction.coerceIn(0f, 1f)
    return when {
        clamped < 0.5f -> {
            val t = clamped / 0.5f
            Color(
                red = 0.9f,
                green = 0.2f + 0.6f * t,
                blue = 0.15f,
                alpha = 1f
            )
        }
        else -> {
            val t = (clamped - 0.5f) / 0.5f
            Color(
                red = 0.9f - 0.55f * t,
                green = 0.8f - 0.1f * t,
                blue = 0.15f + 0.05f * t,
                alpha = 1f
            )
        }
    }
}

private enum class StatsTab(val label: String) {
    SUMMARY("Summary"),
    SPLIT_COMPLETIONS("Splits"),
    DEATHS("Deaths"),
    PROGRESS("Progress"),
    TREND("Trend"),
    RECORDS("Records")
}

@Composable
fun StatsPanel(
    splitsState: SplitsState,
    profile: SplitProfile,
    statsFontSizeService: StatsFontSizeService,
    fileStorageService: com.supermetroid.storage.FileStorageService? = null,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(StatsTab.SUMMARY) }
    val fs by statsFontSizeService.fontSize.collectAsState()

    // Load all runs from disk for accurate stats, filtered to current profile
    var diskRuns by remember { mutableStateOf<List<com.supermetroid.model.RunSession>?>(null) }
    LaunchedEffect(profile.id) {
        diskRuns = fileStorageService?.loadAllRuns()
    }

    val runs = remember(diskRuns, splitsState.runHistory, profile.id) {
        val source = diskRuns ?: splitsState.runHistory
        source.filter {
            !it.id.startsWith("livesplit-pb") && it.profileId == profile.id
        }
    }

    Column(modifier = modifier.padding(horizontal = 8.dp)) {
        // Tab row with font size selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                StatsTab.values().forEach { tab ->
                    TextButton(
                        onClick = { selectedTab = tab },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (selectedTab == tab) TrackerColors.Success else TrackerColors.OnSurfaceVariant
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                }
            }

            // Font size toggle
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                StatsFontSize.values().forEach { size ->
                    val isSelected = fs == size
                    TextButton(
                        onClick = { statsFontSizeService.setFontSize(size) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (isSelected) TrackerColors.Success else TrackerColors.OnSurfaceVariant.copy(alpha = 0.4f),
                            containerColor = if (isSelected) TrackerColors.OnSurfaceVariant.copy(alpha = 0.15f) else Color.Transparent
                        ),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        modifier = Modifier.height(18.dp),
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Text(
                            text = size.displayName.first().toString(),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Load all runs (across all profiles) for Records tab
        var allDiskRuns by remember { mutableStateOf<List<com.supermetroid.model.RunSession>?>(null) }
        LaunchedEffect(Unit) {
            allDiskRuns = fileStorageService?.loadAllRuns()
        }

        // Tab content
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when (selectedTab) {
                StatsTab.SUMMARY -> SummaryTab(runs, fs)
                StatsTab.SPLIT_COMPLETIONS -> SplitCompletionsTab(runs, profile, fs)
                StatsTab.DEATHS -> DeathsTab(runs, profile, fs)
                StatsTab.PROGRESS -> ProgressTab(runs, fs)
                StatsTab.TREND -> TrendTab(runs, fs)
                StatsTab.RECORDS -> RecordsTab(allDiskRuns ?: splitsState.runHistory, profile, fs)
            }
        }
    }
}

@Composable
private fun SummaryTab(runs: List<com.supermetroid.model.RunSession>, fs: StatsFontSize) {
    val stats = remember(runs) { StatsService.computeRunStats(runs) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "RUN STATISTICS",
            style = MaterialTheme.typography.labelMedium.copy(
                color = TrackerColors.Success,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontSize = fs.heading.sp
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        StatRow("Total Runs", stats.totalRuns.toString(), fontSize = fs)
        StatRow("Completed", stats.completedRuns.toString(), TrackerColors.Success, fontSize = fs)
        StatRow("Failed", stats.failedRuns.toString(),
            if (stats.failedRuns > 0) TrackerColors.Error else TrackerColors.OnSurfaceVariant, fontSize = fs)
        if (stats.totalRuns > 0) {
            val completionRate = stats.completedRuns.toFloat() / stats.totalRuns * 100
            StatRow("Completion Rate", "%.2f%%".format(completionRate),
                redToGreenGradient(completionRate / 100f), fontSize = fs)
        }

        Divider(color = TrackerColors.OnSurfaceVariant.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))

        StatRow("Total Play Time", formatDuration(stats.totalPlayTime), fontSize = fs)
        StatRow("Completed Time", formatDuration(stats.completedPlayTime), fontSize = fs)

        if (stats.fastestRun != null) {
            Divider(color = TrackerColors.OnSurfaceVariant.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))
            StatRow("Fastest Run", formatDuration(stats.fastestRun), GoldColor, fontSize = fs)
            StatRow("Average", formatDuration(stats.averageCompletedTime ?: 0), fontSize = fs)
            StatRow("Median", formatDuration(stats.medianCompletedTime ?: 0), fontSize = fs)
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    valueColor: Color = TrackerColors.OnSurfaceVariant,
    fontSize: StatsFontSize = StatsFontSize.MEDIUM
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = TrackerColors.OnSurfaceVariant.copy(alpha = 0.7f),
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize.label.sp
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                color = valueColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize.value.sp
            )
        )
    }
}

@Composable
private fun SplitCompletionsTab(
    runs: List<com.supermetroid.model.RunSession>,
    profile: SplitProfile,
    fs: StatsFontSize
) {
    val completions = remember(runs, profile) {
        StatsService.computeSplitCompletions(runs, profile)
    }
    val maxCount = completions.maxOfOrNull { it.timesCompleted } ?: 1
    // Compute name column width based on longest split name (monospace, ~6dp per char at 9sp)
    val maxNameChars = completions.maxOfOrNull { it.splitName.length } ?: 10
    val charWidth = fs.label * 0.62f  // scale with font size
    val nameWidthDp = (maxNameChars * charWidth + 4).coerceIn(60f, 260f)

    var hoveredIndex by remember { mutableStateOf(-1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(4.dp)
    ) {
        Text(
            text = "SPLIT COMPLETIONS",
            style = MaterialTheme.typography.labelMedium.copy(
                color = TrackerColors.Success,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontSize = fs.heading.sp
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        completions.forEachIndexed { index, entry ->
            val isHovered = hoveredIndex == index
            val fraction = if (maxCount > 0) entry.timesCompleted.toFloat() / maxCount else 0f
            val barColor = redToGreenGradient(fraction)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .onPointerEvent(PointerEventType.Enter) { hoveredIndex = index }
                    .onPointerEvent(PointerEventType.Exit) { hoveredIndex = -1 },
                verticalAlignment = Alignment.CenterVertically
            ) {
                SpriteIcon(
                    itemId = statsSplitItemId(entry.splitId, entry.splitName),
                    isObtained = entry.timesCompleted > 0,
                    size = 16
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = entry.splitName,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TrackerColors.OnSurfaceVariant.copy(alpha = 0.8f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = fs.label.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.width(nameWidthDp.dp)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(14.dp)
                        .background(TrackerColors.OnSurfaceVariant.copy(alpha = 0.1f), RoundedCornerShape(2.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .background(barColor.copy(alpha = if (isHovered) 1f else 0.7f), RoundedCornerShape(2.dp))
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = entry.timesCompleted.toString(),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isHovered) TrackerColors.Success else TrackerColors.OnSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = fs.value.sp
                    ),
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(24.dp)
                )
            }
        }

        if (maxCount > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = (nameWidthDp + 24).dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0", style = axisLabelStyle(fs))
                Text("${maxCount / 2}", style = axisLabelStyle(fs))
                Text("$maxCount", style = axisLabelStyle(fs))
            }
        }
    }
}

@Composable
private fun DeathsTab(
    runs: List<com.supermetroid.model.RunSession>,
    profile: SplitProfile,
    fs: StatsFontSize
) {
    val deaths = remember(runs, profile) {
        StatsService.computeSplitDeaths(runs, profile)
    }
    val maxDeaths = deaths.maxOfOrNull { it.deaths } ?: 1
    val totalDeaths = deaths.sumOf { it.deaths }
    val maxNameChars = deaths.maxOfOrNull { it.splitName.length } ?: 10
    val charWidth = fs.label * 0.62f
    val nameWidthDp = (maxNameChars * charWidth + 4).coerceIn(60f, 260f)

    var hoveredIndex by remember { mutableStateOf(-1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(4.dp)
    ) {
        Text(
            text = "DEATHS BY SPLIT",
            style = MaterialTheme.typography.labelMedium.copy(
                color = TrackerColors.Error,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontSize = fs.heading.sp
            )
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "Total: $totalDeaths deaths across ${runs.size} runs",
            style = MaterialTheme.typography.bodySmall.copy(
                color = TrackerColors.OnSurfaceVariant.copy(alpha = 0.5f),
                fontFamily = FontFamily.Monospace,
                fontSize = fs.chart.sp
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        deaths.forEachIndexed { index, entry ->
            if (entry.deaths == 0) return@forEachIndexed

            val isHovered = hoveredIndex == index
            val fraction = if (maxDeaths > 0) entry.deaths.toFloat() / maxDeaths else 0f
            // Red gradient: more deaths = more intense red
            val barColor = Color(
                red = 0.5f + 0.4f * fraction,
                green = 0.15f + 0.15f * (1f - fraction),
                blue = 0.1f,
                alpha = 1f
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .onPointerEvent(PointerEventType.Enter) { hoveredIndex = index }
                    .onPointerEvent(PointerEventType.Exit) { hoveredIndex = -1 },
                verticalAlignment = Alignment.CenterVertically
            ) {
                SpriteIcon(
                    itemId = statsSplitItemId(entry.splitId, entry.splitName),
                    isObtained = true,
                    size = 16
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = entry.splitName,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TrackerColors.OnSurfaceVariant.copy(alpha = 0.8f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = fs.label.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.width(nameWidthDp.dp)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(14.dp)
                        .background(TrackerColors.OnSurfaceVariant.copy(alpha = 0.1f), RoundedCornerShape(2.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .background(barColor.copy(alpha = if (isHovered) 1f else 0.7f), RoundedCornerShape(2.dp))
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = entry.deaths.toString(),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isHovered) TrackerColors.Error else TrackerColors.OnSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = fs.value.sp
                    ),
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(30.dp)
                )
            }
        }

        // Show "no deaths" if all entries have 0
        if (deaths.all { it.deaths == 0 }) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = "No deaths recorded",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TrackerColors.OnSurfaceVariant.copy(alpha = 0.5f),
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
        }
    }
}

@Composable
private fun ProgressTab(runs: List<com.supermetroid.model.RunSession>, fs: StatsFontSize) {
    val completedProgress = remember(runs) {
        StatsService.computeRunProgress(runs).filter { it.isComplete }
    }

    if (completedProgress.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No completed runs yet",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TrackerColors.OnSurfaceVariant.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Monospace
                )
            )
        }
        return
    }

    var hoveredRunIndex by remember { mutableStateOf(-1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(4.dp)
    ) {
        Text(
            text = "COMPLETED RUN TIMES",
            style = MaterialTheme.typography.labelMedium.copy(
                color = TrackerColors.Success,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontSize = fs.heading.sp
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        val maxTime = completedProgress.maxOf { it.totalTime }
        val minTime = completedProgress.minOf { it.totalTime }
        val timeRange = (maxTime - minTime).coerceAtLeast(1)

        completedProgress.forEachIndexed { idx, entry ->
            val isHovered = hoveredRunIndex == idx
            val isBest = entry.totalTime == minTime
            val fraction = if (maxTime > 0) entry.totalTime.toFloat() / maxTime else 0f
            val normalizedQuality = 1f - ((entry.totalTime - minTime).toFloat() / timeRange)
            val barColor = if (isBest) GoldColor else redToGreenGradient(normalizedQuality)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .onPointerEvent(PointerEventType.Enter) { hoveredRunIndex = idx }
                    .onPointerEvent(PointerEventType.Exit) { hoveredRunIndex = -1 },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${entry.runIndex}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TrackerColors.OnSurfaceVariant.copy(alpha = 0.6f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = fs.label.sp
                    ),
                    modifier = Modifier.width(30.dp),
                    textAlign = TextAlign.End
                )

                Spacer(modifier = Modifier.width(4.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(12.dp)
                        .background(TrackerColors.OnSurfaceVariant.copy(alpha = 0.1f), RoundedCornerShape(2.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .background(barColor.copy(alpha = if (isHovered) 1f else 0.7f), RoundedCornerShape(2.dp))
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = formatDuration(entry.totalTime),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isBest) GoldColor
                        else if (isHovered) TrackerColors.Success
                        else TrackerColors.OnSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isBest) FontWeight.Bold else FontWeight.Normal,
                        fontSize = fs.value.sp
                    ),
                    modifier = Modifier.width(65.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

private data class TrendMode(val label: String, val windowSize: Int)

private val TREND_MODES = listOf(
    TrendMode("Raw", 1),
    TrendMode("3", 3),
    TrendMode("5", 5),
    TrendMode("10", 10)
)

@Composable
private fun TrendTab(runs: List<com.supermetroid.model.RunSession>, fs: StatsFontSize) {
    var selectedMode by remember { mutableStateOf(TREND_MODES[0]) } // Default to Raw

    val trendData = remember(runs, selectedMode) {
        if (selectedMode.windowSize == 1) {
            // Raw: just completed run times in order
            val completed = runs.sortedBy { it.startTime }.filter { it.endTime != null }
            completed.mapIndexed { idx, run -> Pair(idx + 1, run.totalTime) }
        } else {
            StatsService.computeRollingAverage(runs, windowSize = selectedMode.windowSize)
        }
    }

    val completedCount = remember(runs) { runs.count { it.endTime != null } }
    val minRequired = if (selectedMode.windowSize == 1) 1 else selectedMode.windowSize

    if (completedCount < minRequired) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = if (completedCount == 0) "No completed runs yet"
                       else "Need $minRequired+ completed runs for ${selectedMode.label} trend",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TrackerColors.OnSurfaceVariant.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Monospace
                )
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)
    ) {
        // Title + mode toggle row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (selectedMode.windowSize == 1) "COMPLETED RUN TIMES"
                       else "ROLLING AVG (${selectedMode.windowSize} RUNS)",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = TrackerColors.Success,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontSize = fs.heading.sp
                )
            )

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                TREND_MODES.forEach { mode ->
                    val isSelected = selectedMode == mode
                    TextButton(
                        onClick = { selectedMode = mode },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (isSelected) TrackerColors.Success else TrackerColors.OnSurfaceVariant.copy(alpha = 0.5f),
                            containerColor = if (isSelected) TrackerColors.OnSurfaceVariant.copy(alpha = 0.15f) else Color.Transparent
                        ),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier.height(20.dp),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val avgMax = trendData.maxOf { it.second }
        val avgMin = trendData.minOf { it.second }
        val range = (avgMax - avgMin).coerceAtLeast(1)

        val lineColor = TrackerColors.Success
        val gridColor = TrackerColors.OnSurfaceVariant.copy(alpha = 0.15f)
        val canvasBgColor = TrackerColors.OnSurfaceVariant.copy(alpha = 0.05f)
        val labelColor = TrackerColors.OnSurfaceVariant.copy(alpha = 0.5f)

        // Y-axis labels + chart
        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            // Y-axis labels
            Column(
                modifier = Modifier.fillMaxHeight().width(50.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatDuration(avgMax), style = MaterialTheme.typography.bodySmall.copy(
                    color = labelColor, fontFamily = FontFamily.Monospace, fontSize = fs.chart.sp
                ))
                Text(formatDuration((avgMax + avgMin) / 2), style = MaterialTheme.typography.bodySmall.copy(
                    color = labelColor, fontFamily = FontFamily.Monospace, fontSize = fs.chart.sp
                ))
                Text(formatDuration(avgMin), style = MaterialTheme.typography.bodySmall.copy(
                    color = GoldColor, fontFamily = FontFamily.Monospace, fontSize = fs.chart.sp
                ))
            }

            // Chart
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .background(canvasBgColor, RoundedCornerShape(4.dp))
                    .padding(8.dp)
            ) {
                val w = size.width
                val h = size.height
                val points = trendData.mapIndexed { idx, (_, avg) ->
                    val x = if (trendData.size > 1) idx.toFloat() / (trendData.size - 1) * w else w / 2
                    val y = h - ((avg - avgMin).toFloat() / range * h * 0.85f + h * 0.075f)
                    Offset(x, y)
                }

                // Grid lines
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                for (i in 0..4) {
                    val y = h * i / 4f
                    drawLine(gridColor, Offset(0f, y), Offset(w, y), pathEffect = dashEffect)
                }

                // Area fill (subtle)
                for (i in 0 until points.size - 1) {
                    val p1 = points[i]
                    val p2 = points[i + 1]
                    // Fill down to bottom
                    drawLine(
                        color = lineColor.copy(alpha = 0.08f),
                        start = Offset(p1.x, p1.y),
                        end = Offset(p1.x, h),
                        strokeWidth = (p2.x - p1.x).coerceAtLeast(1f)
                    )
                }

                // Line
                for (i in 0 until points.size - 1) {
                    drawLine(lineColor, points[i], points[i + 1], strokeWidth = 2.5f)
                }

                // Points
                points.forEachIndexed { idx, pt ->
                    val isLast = idx == points.size - 1
                    val isMin = trendData[idx].second == avgMin
                    val dotColor = when {
                        isMin -> GoldColor
                        isLast -> lineColor
                        else -> lineColor
                    }
                    val radius = if (isMin || isLast) 5f else 3f
                    drawCircle(dotColor, radius = radius, center = pt)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // X-axis: run range
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 50.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Run ${trendData.first().first}", style = MaterialTheme.typography.bodySmall.copy(
                color = labelColor, fontFamily = FontFamily.Monospace, fontSize = fs.chart.sp
            ))
            Text("Run ${trendData.last().first}", style = MaterialTheme.typography.bodySmall.copy(
                color = labelColor, fontFamily = FontFamily.Monospace, fontSize = fs.chart.sp
            ))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Current vs best stats
        if (selectedMode.windowSize == 1) {
            StatRow("Latest", formatDuration(trendData.last().second), fontSize = fs)
            StatRow("Best", formatDuration(avgMin), GoldColor, fontSize = fs)
        } else {
            StatRow("Current Avg", formatDuration(trendData.last().second), fontSize = fs)
            StatRow("Best Avg", formatDuration(avgMin), GoldColor, fontSize = fs)
        }
        val improvement = trendData.first().second - trendData.last().second
        if (improvement > 0) {
            StatRow("Total Improvement", "-${formatDuration(improvement)}", TrackerColors.Success, fontSize = fs)
        }
    }
}

@Composable
private fun axisLabelStyle(fs: StatsFontSize = StatsFontSize.MEDIUM) = MaterialTheme.typography.bodySmall.copy(
    color = TrackerColors.OnSurfaceVariant.copy(alpha = 0.5f),
    fontFamily = FontFamily.Monospace,
    fontSize = fs.chart.sp
)

/**
 * Map a split ID + name to a sprite item ID for the icon.
 * Delegates to the shared getSplitItemId in SplitsList.
 */
private fun statsSplitItemId(splitId: String, splitName: String): String {
    return getSplitItemId(com.supermetroid.model.Split(splitId, splitName, ""))
}

@Composable
private fun RecordsTab(
    allRuns: List<com.supermetroid.model.RunSession>,
    currentProfile: SplitProfile,
    fs: StatsFontSize
) {
    val records = remember(allRuns, currentProfile) {
        val profilesById = com.supermetroid.autosplits.SplitProfiles.BY_ID
        allRuns
            .filter { it.endTime != null && !it.id.startsWith("livesplit-pb") }
            .groupBy { it.profileId }
            .mapNotNull { (profileId, runs) ->
                val profile = profilesById[profileId]
                    ?: currentProfile.takeIf { it.id == profileId }
                    ?: runs.maxByOrNull { it.startTime }?.profileSnapshot
                    ?: return@mapNotNull null
                val best = runs.minByOrNull { it.totalTime } ?: return@mapNotNull null
                Triple(profile, best.totalTime, runs.size)
            }
            .sortedBy { (profile, _, _) ->
                // Sort by the order in ALL_PROFILES
                com.supermetroid.autosplits.SplitProfiles.ALL_PROFILES.indexOfFirst { it.id == profile.id }
                    .takeIf { it >= 0 } ?: Int.MAX_VALUE
            }
    }

    if (records.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No completed runs yet",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TrackerColors.OnSurfaceVariant.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Monospace
                )
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(4.dp)
    ) {
        Text(
            text = "BEST TIMES BY PROFILE",
            style = MaterialTheme.typography.labelMedium.copy(
                color = GoldColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontSize = fs.heading.sp
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        records.forEach { (profile, bestTime, runCount) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TrackerColors.OnSurfaceVariant.copy(alpha = 0.8f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = fs.label.sp
                    ),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${runCount}r",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TrackerColors.OnSurfaceVariant.copy(alpha = 0.4f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = fs.chart.sp
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Text(
                    text = formatDuration(bestTime),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TrackerColors.OnSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = fs.value.sp
                    ),
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(70.dp)
                )
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
