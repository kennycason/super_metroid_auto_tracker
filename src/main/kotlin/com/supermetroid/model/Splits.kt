package com.supermetroid.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Split(
    val id: String,
    val name: String,
    val type: String, // e.g., "item", "boss", "event", "room_entry"
    val description: String? = null,
    val triggerRoomId: Int? = null, // When set, split triggers on first entry to this room ID
    val requiredItems: List<String>? = null // Items that must be acquired before this split can trigger
)

@Serializable
data class SplitProfile(
    val id: String,
    val name: String,
    val splits: List<Split>,
    /**
     * Structural version of this profile. Built-in profiles remain at version 1.
     * Custom profiles receive a new version (and profile [id]) whenever their
     * ordered split list changes.
     */
    val version: Int = 1,
    /** Stable identity shared by all structural versions of a custom profile. */
    val familyId: String = id
)

@Serializable
data class SplitTime(
    val totalTime: Long, // Total time at split in milliseconds
    val segmentTime: Long, // Time taken for this segment in milliseconds
    val delta: Long? = null, // Delta vs personal best for this segment
    val originalDelta: Long? = null // Preserved delta when this becomes a new PB
)

@Serializable
data class CompletedSplit(
    val splitId: String,
    val time: SplitTime,
    val timestamp: Instant // When the split was recorded
)

@Serializable
data class RunSession(
    val id: String,
    val profileId: String,
    val startTime: Instant,
    val endTime: Instant? = null,
    val completedSplits: List<CompletedSplit>,
    val totalTime: Long,
    val isPersonalBest: Boolean = false,
    val isPaused: Boolean = false,
    val pausedTime: Long = 0, // Total time spent paused in milliseconds
    /**
     * Snapshot used to replay runs after a custom profile is revised or deleted.
     * Optional so all existing run JSON remains backwards compatible.
     */
    val profileSnapshot: SplitProfile? = null
)

@Serializable
data class PersonalBest(
    val profileId: String,
    val runSessionId: String,
    val totalTime: Long,
    val splitTimes: Map<String, SplitTime> // Map of splitId to best SplitTime for that segment
)

@Serializable
data class SplitsState(
    val currentRun: RunSession? = null,
    val personalBests: Map<String, PersonalBest> = emptyMap(), // Map of profileId to PersonalBest
    val runHistory: List<RunSession> = emptyList()
)
