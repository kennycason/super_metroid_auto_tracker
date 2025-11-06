package com.supermetroid.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Extended run history model for comprehensive data storage and PB derivation
 */

@Serializable
data class StoredRunSession(
    val id: String,
    val profileId: String,
    val startTime: Instant,
    val endTime: Instant? = null,
    val completedSplits: List<CompletedSplit>,
    val totalTime: Long,
    val isComplete: Boolean = false, // Whether run was finished or reset/abandoned
    val completionReason: RunCompletionReason = RunCompletionReason.UNKNOWN,
    val pausedTime: Long = 0 // Total time spent paused in milliseconds
)

@Serializable
enum class RunCompletionReason {
    FINISHED,    // Run completed successfully
    RESET,       // User reset the run
    ABANDONED,   // User quit/closed app during run
    UNKNOWN      // Legacy runs or unknown state
}

@Serializable
data class RunHistoryData(
    val version: String = "1.0.0",
    val lastUpdated: Instant,
    val completedRuns: List<StoredRunSession>,
    val incompleteRuns: List<StoredRunSession>, // Runs that were reset/abandoned but had progress
    val derivedPersonalBests: Map<String, PersonalBest> // Derived from all run data
)

/**
 * Statistics derived from run history
 */
@Serializable
data class RunStatistics(
    val profileId: String,
    val totalRuns: Int,
    val completedRuns: Int,
    val incompleteRuns: Int,
    val bestTotalTime: Long?,
    val averageCompletionTime: Long?,
    val bestSegmentTimes: Map<String, SplitTime>, // Best time for each segment across all runs
    val completionRate: Double // completedRuns / totalRuns
)

/**
 * Service for managing run history and deriving personal bests
 */
class RunHistoryService {
    
    /**
     * Derive personal bests from complete run history
     * This is the "one-off job" that can rebuild PBs from all historical data
     */
    fun derivePersonalBestsFromHistory(runHistory: RunHistoryData): Map<String, PersonalBest> {
        val derivedPBs = mutableMapOf<String, PersonalBest>()
        
        // Group runs by profile
        val runsByProfile = runHistory.completedRuns.groupBy { it.profileId }
        
        for ((profileId, runs) in runsByProfile) {
            val bestSegmentTimes = mutableMapOf<String, SplitTime>()
            var bestTotalTime: Long? = null
            var bestRunId: String? = null
            
            // Find best total time
            for (run in runs.filter { it.isComplete }) {
                if (bestTotalTime == null || run.totalTime < bestTotalTime) {
                    bestTotalTime = run.totalTime
                    bestRunId = run.id
                }
            }
            
            // Find best segment times across all runs (ONLY complete runs)
            val allRuns = runs.filter { it.isComplete }
            for (run in allRuns) {
                for (split in run.completedSplits) {
                    val currentBest = bestSegmentTimes[split.splitId]
                    if (currentBest == null || split.time.segmentTime < currentBest.segmentTime) {
                        bestSegmentTimes[split.splitId] = split.time
                    }
                }
            }
            
            // Create PersonalBest if we have data
            if (bestTotalTime != null && bestRunId != null) {
                derivedPBs[profileId] = PersonalBest(
                    profileId = profileId,
                    runSessionId = bestRunId,
                    totalTime = bestTotalTime,
                    splitTimes = bestSegmentTimes
                )
            }
        }
        
        return derivedPBs
    }
    
    /**
     * Generate statistics for a profile from run history
     */
    fun generateStatistics(profileId: String, runHistory: RunHistoryData): RunStatistics {
        val allRuns = (runHistory.completedRuns + runHistory.incompleteRuns)
            .filter { it.profileId == profileId }
        val completedRuns = runHistory.completedRuns.filter { it.profileId == profileId && it.isComplete }
        
        val bestTotalTime = completedRuns.minByOrNull { it.totalTime }?.totalTime
        val averageCompletionTime = if (completedRuns.isNotEmpty()) {
            completedRuns.map { it.totalTime }.average().toLong()
        } else null
        
        // Best segment times across ONLY complete runs
        val bestSegmentTimes = mutableMapOf<String, SplitTime>()
        for (run in completedRuns) {
            for (split in run.completedSplits) {
                val currentBest = bestSegmentTimes[split.splitId]
                if (currentBest == null || split.time.segmentTime < currentBest.segmentTime) {
                    bestSegmentTimes[split.splitId] = split.time
                }
            }
        }
        
        return RunStatistics(
            profileId = profileId,
            totalRuns = allRuns.size,
            completedRuns = completedRuns.size,
            incompleteRuns = allRuns.size - completedRuns.size,
            bestTotalTime = bestTotalTime,
            averageCompletionTime = averageCompletionTime,
            bestSegmentTimes = bestSegmentTimes,
            completionRate = if (allRuns.isNotEmpty()) completedRuns.size.toDouble() / allRuns.size else 0.0
        )
    }
    
    /**
     * Add a run to history (complete or incomplete)
     */
    fun addRunToHistory(
        currentHistory: RunHistoryData,
        run: StoredRunSession
    ): RunHistoryData {
        return if (run.isComplete) {
            currentHistory.copy(
                completedRuns = currentHistory.completedRuns + run,
                lastUpdated = kotlinx.datetime.Clock.System.now()
            )
        } else {
            currentHistory.copy(
                incompleteRuns = currentHistory.incompleteRuns + run,
                lastUpdated = kotlinx.datetime.Clock.System.now()
            )
        }
    }
}
