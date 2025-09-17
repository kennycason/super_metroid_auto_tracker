package com.supermetroid.jobs

import com.supermetroid.service.RunHistoryManager
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

/**
 * Standalone job for deriving personal bests from run history
 * This is the "one-off job" entry point that can rebuild PBs from all historical data
 * 
 * Usage:
 * - Can be run manually to rebuild PBs after data corruption
 * - Can be scheduled to run periodically for data integrity
 * - Safe to run multiple times (idempotent)
 */
class PersonalBestDerivationJob(
    private val smtrackerDir: File = File(System.getProperty("user.home"), ".smtracker")
) {
    
    private val runHistoryManager = RunHistoryManager(
        File(smtrackerDir, "run-history.json")
    )
    
    /**
     * Main entry point - derive PBs from complete run history
     */
    fun execute(): JobResult {
        return try {
            logger.info { "Starting Personal Best derivation job..." }
            
            // Ensure directory exists
            if (!smtrackerDir.exists()) {
                smtrackerDir.mkdirs()
                logger.info { "Created .smtracker directory at ${smtrackerDir.absolutePath}" }
            }
            
            // Load current run history
            val runHistory = runHistoryManager.loadRunHistory()
            logger.info { "Loaded run history: ${runHistory.completedRuns.size} completed, ${runHistory.incompleteRuns.size} incomplete runs" }
            
            // Derive personal bests
            val derivedPBs = runHistoryManager.derivePersonalBestsFromHistory()
            logger.info { "Derived personal bests for ${derivedPBs.size} profiles" }
            
            // Log details for each profile
            derivedPBs.forEach { (profileId, pb) ->
                val stats = runHistoryManager.getStatistics(profileId)
                logger.info { 
                    "Profile '$profileId': Best total time ${formatTime(pb.totalTime)}, " +
                    "${stats.completedRuns}/${stats.totalRuns} runs completed (${String.format("%.1f", stats.completionRate * 100)}%), " +
                    "${pb.splitTimes.size} best segments" 
                }
            }
            
            // Optional: Clean up old incomplete runs to prevent file bloat
            runHistoryManager.cleanupOldIncompleteRuns()
            
            JobResult.Success(
                message = "Successfully derived personal bests for ${derivedPBs.size} profiles",
                profilesProcessed = derivedPBs.size,
                personalBests = derivedPBs
            )
            
        } catch (e: Exception) {
            logger.error(e) { "Personal Best derivation job failed" }
            JobResult.Failure(
                message = "Failed to derive personal bests: ${e.message}",
                error = e
            )
        }
    }
    
    /**
     * Dry run - show what would be derived without saving
     */
    fun dryRun(): JobResult {
        return try {
            logger.info { "Starting Personal Best derivation job (DRY RUN)..." }
            
            val runHistory = runHistoryManager.loadRunHistory()
            val derivedPBs = com.supermetroid.model.RunHistoryService().derivePersonalBestsFromHistory(runHistory)
            
            logger.info { "DRY RUN: Would derive personal bests for ${derivedPBs.size} profiles" }
            derivedPBs.forEach { (profileId, pb) ->
                val stats = runHistoryManager.getStatistics(profileId)
                logger.info { 
                    "DRY RUN Profile '$profileId': Would set best total time to ${formatTime(pb.totalTime)}, " +
                    "${pb.splitTimes.size} best segments from ${stats.totalRuns} total runs" 
                }
            }
            
            JobResult.Success(
                message = "DRY RUN: Would derive personal bests for ${derivedPBs.size} profiles",
                profilesProcessed = derivedPBs.size,
                personalBests = derivedPBs
            )
            
        } catch (e: Exception) {
            logger.error(e) { "Personal Best derivation dry run failed" }
            JobResult.Failure(
                message = "DRY RUN failed: ${e.message}",
                error = e
            )
        }
    }
    
    private fun formatTime(timeMs: Long): String {
        val totalSeconds = timeMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val milliseconds = timeMs % 1000
        return String.format("%d:%02d.%03d", minutes, seconds, milliseconds)
    }
}

/**
 * Result of the PersonalBest derivation job
 */
sealed class JobResult {
    data class Success(
        val message: String,
        val profilesProcessed: Int,
        val personalBests: Map<String, com.supermetroid.model.PersonalBest>
    ) : JobResult()
    
    data class Failure(
        val message: String,
        val error: Throwable
    ) : JobResult()
}

/**
 * Command-line entry point for running the job standalone
 */
fun main(args: Array<String>) {
    val dryRun = args.contains("--dry-run") || args.contains("-d")
    val job = PersonalBestDerivationJob()
    
    val result = if (dryRun) {
        println("Running Personal Best derivation in DRY RUN mode...")
        job.dryRun()
    } else {
        println("Running Personal Best derivation job...")
        job.execute()
    }
    
    when (result) {
        is JobResult.Success -> {
            println("✅ SUCCESS: ${result.message}")
            println("Processed ${result.profilesProcessed} profiles")
            System.exit(0)
        }
        is JobResult.Failure -> {
            println("❌ FAILURE: ${result.message}")
            result.error.printStackTrace()
            System.exit(1)
        }
    }
}
