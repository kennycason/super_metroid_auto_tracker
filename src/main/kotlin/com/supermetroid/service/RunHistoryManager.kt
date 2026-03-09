package com.supermetroid.service

import com.supermetroid.model.*
import com.supermetroid.util.Logging
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.io.IOException
import kotlin.time.Duration.Companion.days

/**
 * Manages run history persistence separately from the main splits engine
 * This ensures we don't break existing auto-splits functionality
 */
class RunHistoryManager(
    private val runHistoryFile: File
) : Logging {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true // For backward compatibility
    }
    private val runHistoryService = RunHistoryService()

    /**
     * Load run history from disk
     */
    fun loadRunHistory(): RunHistoryData {
        return try {
            if (runHistoryFile.exists()) {
                val content = runHistoryFile.readText()
                json.decodeFromString<RunHistoryData>(content)
            } else {
                // Create empty run history
                val emptyHistory = RunHistoryData(
                    lastUpdated = Clock.System.now(),
                    completedRuns = emptyList(),
                    incompleteRuns = emptyList(),
                    derivedPersonalBests = emptyMap()
                )
                saveRunHistory(emptyHistory)
                emptyHistory
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to load run history, creating new one" }
            RunHistoryData(
                lastUpdated = Clock.System.now(),
                completedRuns = emptyList(),
                incompleteRuns = emptyList(),
                derivedPersonalBests = emptyMap()
            )
        }
    }

    /**
     * Save run history to disk
     */
    fun saveRunHistory(runHistory: RunHistoryData) {
        try {
            // Ensure parent directory exists
            runHistoryFile.parentFile?.mkdirs()

            val content = json.encodeToString(runHistory)
            runHistoryFile.writeText(content)

            logger.debug { "Saved run history with ${runHistory.completedRuns.size} completed and ${runHistory.incompleteRuns.size} incomplete runs" }
        } catch (e: IOException) {
            logger.error(e) { "Failed to save run history to ${runHistoryFile.absolutePath}" }
        }
    }

    /**
     * Convert RunSession to StoredRunSession
     */
    private fun RunSession.toStoredRunSession(
        isComplete: Boolean = false,
        completionReason: RunCompletionReason = RunCompletionReason.UNKNOWN
    ): StoredRunSession {
        return StoredRunSession(
            id = this.id,
            profileId = this.profileId,
            startTime = this.startTime,
            endTime = this.endTime,
            completedSplits = this.completedSplits,
            totalTime = this.totalTime,
            isComplete = isComplete,
            completionReason = completionReason,
            pausedTime = this.pausedTime
        )
    }

    /**
     * Store a completed run
     */
    fun storeCompletedRun(run: RunSession) {
        val currentHistory = loadRunHistory()
        val storedRun = run.toStoredRunSession(
            isComplete = true,
            completionReason = RunCompletionReason.FINISHED
        )

        val updatedHistory = runHistoryService.addRunToHistory(currentHistory, storedRun)
        saveRunHistory(updatedHistory)

        logger.info { "Stored completed run ${run.id} for profile ${run.profileId} with total time ${run.totalTime}ms" }
    }

    /**
     * Store an incomplete run (reset or abandoned)
     */
    fun storeIncompleteRun(run: RunSession, completionReason: RunCompletionReason = RunCompletionReason.RESET) {
        // Only store incomplete runs if they have at least one completed split
        if (run.completedSplits.isEmpty()) {
            logger.debug { "Skipping storage of incomplete run ${run.id} - no completed splits" }
            return
        }

        val currentHistory = loadRunHistory()
        val storedRun = run.toStoredRunSession(
            isComplete = false,
            completionReason = completionReason
        )

        val updatedHistory = runHistoryService.addRunToHistory(currentHistory, storedRun)
        saveRunHistory(updatedHistory)

        logger.info { "Stored incomplete run ${run.id} for profile ${run.profileId} with ${run.completedSplits.size} splits (reason: $completionReason)" }
    }

    /**
     * Derive personal bests from all run history
     * This is the "one-off job" entry point
     */
    fun derivePersonalBestsFromHistory(): Map<String, PersonalBest> {
        val runHistory = loadRunHistory()
        val derivedPBs = runHistoryService.derivePersonalBestsFromHistory(runHistory)

        // Update the stored derived PBs
        val updatedHistory = runHistory.copy(
            derivedPersonalBests = derivedPBs,
            lastUpdated = Clock.System.now()
        )
        saveRunHistory(updatedHistory)

        logger.info { "Derived personal bests for ${derivedPBs.size} profiles from run history" }
        return derivedPBs
    }

    /**
     * Get statistics for a profile
     */
    fun getStatistics(profileId: String): RunStatistics {
        val runHistory = loadRunHistory()
        return runHistoryService.generateStatistics(profileId, runHistory)
    }

    /**
     * Get all stored runs for a profile (complete and incomplete)
     */
    fun getRunsForProfile(profileId: String): List<StoredRunSession> {
        val runHistory = loadRunHistory()
        return (runHistory.completedRuns + runHistory.incompleteRuns)
            .filter { it.profileId == profileId }
            .sortedByDescending { it.startTime }
    }

    /**
     * Cleanup old incomplete runs (keep only recent ones to avoid file bloat)
     * Keeps incomplete runs from the last 30 days by default
     */
    fun cleanupOldIncompleteRuns(keepDays: Int = 30) {
        val cutoffTime = Clock.System.now().minus(keepDays.days)
        val runHistory = loadRunHistory()

        val keptIncompleteRuns = runHistory.incompleteRuns.filter { it.startTime >= cutoffTime }
        val removedCount = runHistory.incompleteRuns.size - keptIncompleteRuns.size

        if (removedCount > 0) {
            val updatedHistory = runHistory.copy(
                incompleteRuns = keptIncompleteRuns,
                lastUpdated = Clock.System.now()
            )
            saveRunHistory(updatedHistory)
            logger.info { "Cleaned up $removedCount old incomplete runs (older than $keepDays days)" }
        }
    }
}
