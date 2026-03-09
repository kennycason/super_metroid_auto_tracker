package com.supermetroid.livesplit

/**
 * Data model representing a parsed LiveSplit (.lss) file.
 *
 * LSS files are XML-based and contain:
 * - Game/category metadata
 * - Segment definitions with best times
 * - Full attempt history
 * - Per-segment history across all attempts
 */
data class LiveSplitDocument(
    val gameName: String,
    val categoryName: String,
    val attemptCount: Int,
    val offset: String = "00:00:00.0000000",
    val segments: List<LiveSplitSegment>,
    val attemptHistory: List<LiveSplitAttempt>,
    val autoSplitterSettings: String? = null
)

data class LiveSplitSegment(
    val name: String,
    val icon: String? = null,
    val bestSegmentTime: LiveSplitTimeSpan?,
    val splitTimes: List<LiveSplitComparisonSplit>,
    val segmentHistory: List<LiveSplitHistoryEntry>
)

data class LiveSplitTimeSpan(
    val realTime: Long?,
    val gameTime: Long?
)

data class LiveSplitComparisonSplit(
    val comparisonName: String,
    val realTime: Long?,
    val gameTime: Long?
)

data class LiveSplitAttempt(
    val id: Int,
    val started: String?,
    val ended: String?,
    val realTime: Long?,
    val gameTime: Long?
)

data class LiveSplitHistoryEntry(
    val id: Int,
    val realTime: Long?,
    val gameTime: Long?
)
