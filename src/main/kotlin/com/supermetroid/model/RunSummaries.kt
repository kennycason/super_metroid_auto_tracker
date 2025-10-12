package com.supermetroid.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Run summaries data structure for the new file-based format
 * This replaces the old monolithic splits-data.json with derived best splits
 */

@Serializable
data class BestSplitTime(
    val totalTime: Long,
    val segmentTime: Long,
    val runId: String
)

@Serializable
data class ProfileSummary(
    val profileId: String,
    val profileName: String,
    val bestTotalTime: Long?,
    val bestRunId: String?,
    val bestRunFile: String?,
    val totalRuns: Int,
    val completedRuns: Int,
    val bestSplitTimes: Map<String, BestSplitTime>
)

@Serializable
data class RunSummaries(
    val version: String = "2.0.0",
    val lastUpdated: Instant,
    val profiles: Map<String, ProfileSummary>
)

