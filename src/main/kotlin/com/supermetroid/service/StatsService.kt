package com.supermetroid.service

import com.supermetroid.model.RunSession
import com.supermetroid.model.SplitProfile

/**
 * Pure data model for stats calculations. No state — just functions that take
 * run history + profile and return computed stats.
 */
object StatsService {

    data class RunStats(
        val totalRuns: Int,
        val completedRuns: Int,
        val failedRuns: Int,
        val totalPlayTime: Long,          // ms — sum of ALL segment times (complete + incomplete)
        val completedPlayTime: Long,      // ms — sum of completed run total times
        val fastestRun: Long?,            // ms — fastest completed run
        val averageCompletedTime: Long?,  // ms — average of completed runs
        val medianCompletedTime: Long?    // ms — median of completed runs
    )

    data class SplitCompletionEntry(
        val splitId: String,
        val splitName: String,
        val timesCompleted: Int
    )

    data class RunProgressEntry(
        val runIndex: Int,               // 1-based run number (chronological)
        val totalTime: Long,             // ms
        val isComplete: Boolean
    )

    data class SplitDeathEntry(
        val splitId: String,
        val splitName: String,
        val deaths: Int,
        val timesReached: Int,
        val timesCompleted: Int
    )

    /**
     * Compute aggregate stats from run history.
     */
    fun computeRunStats(runs: List<RunSession>): RunStats {
        val completed = runs.filter { it.endTime != null }
        val failed = runs.filter { it.endTime == null }

        val totalPlayTime = runs.sumOf { run ->
            run.completedSplits.sumOf { it.time.segmentTime }
        }

        val completedPlayTime = completed.sumOf { it.totalTime }
        val completedTimes = completed.map { it.totalTime }.sorted()

        return RunStats(
            totalRuns = runs.size,
            completedRuns = completed.size,
            failedRuns = failed.size,
            totalPlayTime = totalPlayTime,
            completedPlayTime = completedPlayTime,
            fastestRun = completedTimes.firstOrNull(),
            averageCompletedTime = if (completedTimes.isNotEmpty()) completedTimes.average().toLong() else null,
            medianCompletedTime = if (completedTimes.isNotEmpty()) {
                val mid = completedTimes.size / 2
                if (completedTimes.size % 2 == 0) {
                    (completedTimes[mid - 1] + completedTimes[mid]) / 2
                } else {
                    completedTimes[mid]
                }
            } else null
        )
    }

    /**
     * Compute how many times each split has been reached across all runs.
     * Ordered by profile split order.
     */
    fun computeSplitCompletions(runs: List<RunSession>, profile: SplitProfile): List<SplitCompletionEntry> {
        return profile.splits.map { split ->
            val count = runs.count { run ->
                run.completedSplits.any { it.splitId == split.id }
            }
            SplitCompletionEntry(
                splitId = split.id,
                splitName = split.name,
                timesCompleted = count
            )
        }
    }

    /**
     * Compute progress over time: completed run times in chronological order.
     * Shows how the runner's pace has improved over time.
     */
    fun computeRunProgress(runs: List<RunSession>): List<RunProgressEntry> {
        val sorted = runs.sortedBy { it.startTime }
        return sorted.mapIndexed { index, run ->
            RunProgressEntry(
                runIndex = index + 1,
                totalTime = run.totalTime,
                isComplete = run.endTime != null
            )
        }
    }

    /**
     * Compute deaths per split. Deaths at split[i] = runs that reached split[i] minus
     * runs that completed split[i]. ALL runs reach split[0] (every attempt starts at
     * the first split). A run reaches split[i] (i>0) if it completed split[i-1].
     * Results are sorted by death count (descending).
     */
    fun computeSplitDeaths(runs: List<RunSession>, profile: SplitProfile): List<SplitDeathEntry> {
        if (profile.splits.isEmpty()) return emptyList()

        val entries = profile.splits.mapIndexed { index, split ->
            val timesCompleted = runs.count { run ->
                run.completedSplits.any { it.splitId == split.id }
            }
            val timesReached = if (index == 0) {
                // First split: ALL runs reach it (every attempt starts here)
                runs.size
            } else {
                val prevSplit = profile.splits[index - 1]
                runs.count { run ->
                    run.completedSplits.any { it.splitId == prevSplit.id }
                }
            }
            SplitDeathEntry(
                splitId = split.id,
                splitName = split.name,
                deaths = (timesReached - timesCompleted).coerceAtLeast(0),
                timesReached = timesReached,
                timesCompleted = timesCompleted
            )
        }
        return entries.sortedByDescending { it.deaths }
    }

    /**
     * Compute a rolling average of completed run times (window of last N runs).
     * Returns pairs of (runIndex, rollingAverageMs).
     */
    fun computeRollingAverage(runs: List<RunSession>, windowSize: Int = 5): List<Pair<Int, Long>> {
        val completed = runs
            .sortedBy { it.startTime }
            .filter { it.endTime != null }

        if (completed.size < windowSize) return emptyList()

        return (windowSize..completed.size).map { end ->
            val window = completed.subList(end - windowSize, end)
            val avg = window.sumOf { it.totalTime } / windowSize
            Pair(end, avg)
        }
    }
}
