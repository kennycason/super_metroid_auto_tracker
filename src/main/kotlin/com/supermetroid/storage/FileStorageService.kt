package com.supermetroid.storage

import com.supermetroid.model.*
import com.supermetroid.util.Logging
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

/**
 * File-based storage service for splits data and personal bests
 * Stores data in ~/.smtracker/ directory by default
 * 
 * @param dataDir Optional custom data directory path. If provided, uses that directory directly.
 *                If null, uses ~/.smtracker/ as the default.
 */
class FileStorageService(private val dataDir: String? = null) : Logging {
    private val trackerDir = if (dataDir != null) {
        File(dataDir)
    } else {
        File(System.getProperty("user.home"), ".smtracker")
    }
    private val splitsFile = File(trackerDir, "splits-data.json")
    private val configFile = File(trackerDir, "smtracker.json")
    private val runsDir = File(trackerDir, "runs")
    private val backupsDir = File(trackerDir, "backups")
    private val runSummariesFile = File(trackerDir, "run-summaries.json")

    /** Get the storage directory path (e.g., ~/.smtracker/) */
    fun getStorageDir(): File = trackerDir

    // Serialize all run file writes to prevent concurrent saves from corrupting files
    private val saveMutex = Mutex()

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true  // Always encode default values in JSON
    }

    init {
        // Ensure directories exist
        if (!trackerDir.exists()) {
            trackerDir.mkdirs()
            logger.info { "📁 Created tracker directory: ${trackerDir.absolutePath}" }
        }
        if (!runsDir.exists()) {
            runsDir.mkdirs()
            logger.info { "📁 Created runs directory: ${runsDir.absolutePath}" }
        }
        if (!backupsDir.exists()) {
            backupsDir.mkdirs()
            logger.info { "📁 Created backups directory: ${backupsDir.absolutePath}" }
        }
    }

    /**
     * Save splits state to file (deprecated - no longer used)
     * @deprecated Legacy format no longer supported. Runs are saved individually in runs/ directory.
     */
    @Deprecated("Legacy format no longer supported. Use saveRun() instead.")
    suspend fun saveSplitsState(splitsState: SplitsState) = withContext(Dispatchers.IO) {
        // No-op: Legacy format is no longer saved
        // Individual runs are saved to runs/ directory instead
        logger.debug { "⚠️ saveSplitsState called but is deprecated - runs are saved individually" }
    }

    /**
     * Load splits state from runs directory (new format only)
     * Legacy splits-data.json format is no longer supported
     */
    suspend fun loadSplitsState(): SplitsState = withContext(Dispatchers.IO) {
        try {
            // Always use new format - load from runs/ directory
            logger.info { "📊 Loading splits state from runs directory: ${runsDir.absolutePath}" }
                return@withContext loadSplitsStateFromNewFormat()
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to load splits state, returning empty state" }
            SplitsState()
        }
    }

    /**
     * Check if legacy splits file exists (deprecated)
     * @deprecated Legacy format no longer supported, kept for migration checking only
     */
    @Deprecated("Legacy format no longer supported")
    fun splitsFileExists(): Boolean = splitsFile.exists()

    /**
     * Get legacy splits file path (deprecated)
     * @deprecated Legacy format no longer supported, kept for migration checking only
     */
    @Deprecated("Legacy format no longer supported")
    fun getSplitsFilePath(): String = splitsFile.absolutePath

    /**
     * Create backup of legacy splits file (deprecated)
     * @deprecated Legacy format no longer supported, runs/ directory is the source of truth
     */
    @Deprecated("Legacy format no longer supported")
    suspend fun backupSplitsFile() = withContext(Dispatchers.IO) {
        try {
            if (splitsFile.exists()) {
                val timestamp = System.currentTimeMillis()
                val backupFile = File(trackerDir, "splits-data-backup-$timestamp.json")
                splitsFile.copyTo(backupFile)
                logger.info { "📦 Created backup: ${backupFile.absolutePath}" }
            }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to create backup" }
        }
    }

    /**
     * Save app config to file
     */
    suspend fun saveAppConfig(config: AppConfig) = withContext(Dispatchers.IO) {
        try {
            val jsonString = json.encodeToString(config)
            configFile.writeText(jsonString)
            logger.debug { "💾 Saved app config to ${configFile.absolutePath}" }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to save app config" }
            throw e
        }
    }

    /**
     * Load app config from file
     */
    suspend fun loadAppConfig(): AppConfig = withContext(Dispatchers.IO) {
        try {
            if (!configFile.exists()) {
                logger.info { "📄 No existing config file found, returning default config" }
                return@withContext AppConfig()
            }

            val jsonString = configFile.readText()
            val config = json.decodeFromString<AppConfig>(jsonString)
            logger.info { "📄 Loaded app config from ${configFile.absolutePath}" }
            config
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to load app config, returning default config" }
            AppConfig()
        }
    }

    /**
     * Get config file path for debugging
     */
    fun getConfigFilePath(): String = configFile.absolutePath

    // ========================================
    // NEW FILE-BASED RUN STORAGE (v2.0.0)
    // ========================================

    /**
     * Save a completed run as an individual file
     * Uses run ID to ensure uniqueness when multiple runs have the same start time
     */
    suspend fun saveRun(run: RunSession) = withContext(Dispatchers.IO) {
        saveMutex.withLock {
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
                val dateStr = dateFormat.format(Date(run.startTime.toEpochMilliseconds()))
                // Extract timestamp from run ID (format: run_<timestamp>) to ensure uniqueness
                val runTimestamp = run.id.substringAfter("run_", "")
                // Profile ID prefix for natural sorting/grouping by category
                val filename = "${run.profileId}_${dateStr}_${runTimestamp}.json"
                val runFile = File(runsDir, filename)

                val jsonString = json.encodeToString(run)
                // Atomic write: write to uniquely-named temp file then rename.
                // Unique suffix prevents concurrent saves from clobbering each other's temp file.
                val tmpFile = File.createTempFile("run_", ".tmp", runsDir)
                tmpFile.writeText(jsonString)
                if (!tmpFile.renameTo(runFile)) {
                    // renameTo can fail on some platforms; fall back to copy + delete
                    tmpFile.copyTo(runFile, overwrite = true)
                    tmpFile.delete()
                }
                logger.info { "💾 Saved run to ${runFile.name}" }
            } catch (e: Exception) {
                logger.error(e) { "❌ Failed to save run ${run.id}" }
                throw e
            }
        }
    }

    /**
     * Backup a file to the backups directory with a timestamp suffix (suspend version).
     * Used before deleting runs.
     */
    suspend fun backupFile(sourceFile: File): Boolean = withContext(Dispatchers.IO) {
        backupFileSync(sourceFile)
    }

    /**
     * Backup a file to the backups directory with a timestamp suffix (blocking version).
     * Used by non-suspend callers like SplitFormatService.saveRunToLiveSplit.
     */
    fun backupFileSync(sourceFile: File): Boolean {
        return try {
            if (!sourceFile.exists()) return false
            val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss").format(Date())
            val backupName = "${sourceFile.nameWithoutExtension}_$timestamp.${sourceFile.extension}"
            val backupFile = File(backupsDir, backupName)
            sourceFile.copyTo(backupFile, overwrite = true)
            logger.info { "📦 Backed up ${sourceFile.name} → backups/${backupFile.name}" }
            true
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to backup ${sourceFile.name}" }
            false
        }
    }

    /**
     * Delete a run file by filename, backing it up first.
     * Returns true if deletion succeeded.
     */
    suspend fun deleteRun(fileName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val runFile = File(runsDir, fileName)
            if (!runFile.exists()) {
                logger.warn { "⚠️ Run file not found for deletion: $fileName" }
                return@withContext false
            }
            // Always backup before deleting
            backupFile(runFile)
            val deleted = runFile.delete()
            if (deleted) {
                logger.info { "🗑️ Deleted run: $fileName" }
            } else {
                logger.error { "❌ Failed to delete run file: $fileName" }
            }
            deleted
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to delete run: $fileName" }
            false
        }
    }

    /**
     * Get the runs directory path for external backup (e.g., LSS files).
     */
    fun getBackupsDir(): File = backupsDir

    /**
     * Load all runs from the runs directory
     */
    suspend fun loadAllRuns(): List<RunSession> = withContext(Dispatchers.IO) {
        try {
            if (!runsDir.exists()) {
                logger.info { "📁 Runs directory doesn't exist yet" }
                return@withContext emptyList()
            }

            val runFiles = runsDir.listFiles { file ->
                file.isFile && file.extension == "json" && !file.name.contains("corrupted")
            } ?: emptyArray()

            val runs = runFiles.mapNotNull { file ->
                try {
                    val jsonString = file.readText()
                    json.decodeFromString<RunSession>(jsonString)
                } catch (e: Exception) {
                    logger.error(e) { "❌ Failed to load run from ${file.name}" }
                    null
                }
            }

            logger.info { "📄 Loaded ${runs.size} runs from ${runsDir.absolutePath}" }
            runs
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to load runs" }
            emptyList()
        }
    }

    /**
     * Load a specific run by its ID
     */
    suspend fun loadRunById(runId: String): RunSession? = withContext(Dispatchers.IO) {
        try {
            if (!runsDir.exists()) {
                logger.info { "📁 Runs directory doesn't exist" }
                return@withContext null
            }

            val runFiles = runsDir.listFiles { file ->
                file.isFile && file.extension == "json" && !file.name.contains("corrupted")
            } ?: emptyArray()

            for (file in runFiles) {
                try {
                    val jsonString = file.readText()
                    val run = json.decodeFromString<RunSession>(jsonString)
                    if (run.id == runId) {
                        logger.info { "📄 Loaded run ${runId} from ${file.name}" }
                        return@withContext run
                    }
                } catch (e: Exception) {
                    logger.error(e) { "❌ Failed to parse run from ${file.name}" }
                }
            }

            logger.warn { "⚠️  Run with ID ${runId} not found" }
            null
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to load run by ID" }
            null
        }
    }

    /**
     * Metadata for a run file (for listing in UI)
     */
    data class RunFileMetadata(
        val fileName: String,
        val displayName: String,
        val isComplete: Boolean,
        val startTime: kotlinx.datetime.Instant,
        val totalTime: Long,
        val profileId: String
    )

    /**
     * List all run files with metadata, sorted by start time (most recent first)
     */
    suspend fun listRunFiles(): List<RunFileMetadata> = withContext(Dispatchers.IO) {
        try {
            if (!runsDir.exists()) {
                return@withContext emptyList()
            }

            val runFiles = runsDir.listFiles { file ->
                file.isFile && file.extension == "json" && !file.name.contains("corrupted")
            } ?: emptyArray()

            val metadata = runFiles.mapNotNull { file ->
                try {
                    val jsonString = file.readText()
                    val run = json.decodeFromString<RunSession>(jsonString)
                    
                    // Format display name
                    val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    val dateStr = dateFormatter.format(Date(run.startTime.toEpochMilliseconds()))
                    val profileName = run.profileId.uppercase().replace("-", " ")
                    val timeStr = formatTime(run.totalTime)
                    val completeIcon = if (run.endTime != null) "✅" else "❌"
                    
                    val displayName = "$completeIcon $dateStr - $profileName ($timeStr)"
                    
                    RunFileMetadata(
                        fileName = file.name,
                        displayName = displayName,
                        isComplete = run.endTime != null,
                        startTime = run.startTime,
                        totalTime = run.totalTime,
                        profileId = run.profileId
                    )
                } catch (e: Exception) {
                    logger.error(e) { "❌ Failed to load metadata from ${file.name}" }
                    null
                }
            }
            
            // Sort by start time descending (most recent first)
            metadata.sortedByDescending { it.startTime }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to list run files" }
            emptyList()
        }
    }

    /**
     * Load a specific run by its filename
     */
    suspend fun loadRunByFileName(fileName: String): RunSession? = withContext(Dispatchers.IO) {
        try {
            if (!runsDir.exists()) {
                logger.info { "📁 Runs directory doesn't exist" }
                return@withContext null
            }

            val file = File(runsDir, fileName)
            if (!file.exists() || !file.isFile) {
                logger.warn { "⚠️ Run file not found: $fileName" }
                return@withContext null
            }

            val jsonString = file.readText()
            val run = json.decodeFromString<RunSession>(jsonString)
            logger.info { "📄 Loaded run from file: $fileName" }
            run
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to load run from file: $fileName" }
            null
        }
    }

    /**
     * Derive best splits from all completed runs
     */
    suspend fun deriveBestSplits(profileId: String): ProfileSummary = withContext(Dispatchers.IO) {
        val allRuns = loadAllRuns().filter { it.profileId == profileId }
        val completedRuns = allRuns.filter { it.endTime != null }

        // Find best total time
        val bestRun = completedRuns.minByOrNull { it.totalTime }

        // Find best time for each split across all runs
        val bestSplitTimes = mutableMapOf<String, BestSplitTime>()

        for (run in completedRuns) {
            for (completedSplit in run.completedSplits) {
                val existing = bestSplitTimes[completedSplit.splitId]
                if (existing == null || completedSplit.time.segmentTime < existing.segmentTime) {
                    bestSplitTimes[completedSplit.splitId] = BestSplitTime(
                        totalTime = completedSplit.time.totalTime,
                        segmentTime = completedSplit.time.segmentTime,
                        runId = run.id
                    )
                }
            }
        }

        ProfileSummary(
            profileId = profileId,
            profileName = getProfileName(profileId),
            bestTotalTime = bestRun?.totalTime,
            bestRunId = bestRun?.id,
            bestRunFile = bestRun?.let { findRunFileName(it.id) },
            totalRuns = allRuns.size,
            completedRuns = completedRuns.size,
            bestSplitTimes = bestSplitTimes
        )
    }

    /**
     * Save run summaries to file
     */
    suspend fun saveRunSummaries(summaries: RunSummaries) = withContext(Dispatchers.IO) {
        try {
            val jsonString = json.encodeToString(summaries)
            // Atomic write with unique temp file to prevent corruption
            val tmpFile = File.createTempFile("summaries_", ".tmp", runSummariesFile.parentFile)
            tmpFile.writeText(jsonString)
            if (!tmpFile.renameTo(runSummariesFile)) {
                tmpFile.copyTo(runSummariesFile, overwrite = true)
                tmpFile.delete()
            }
            logger.debug { "💾 Saved run summaries to ${runSummariesFile.absolutePath}" }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to save run summaries" }
            throw e
        }
    }

    /**
     * Load run summaries by deriving from individual run files (source of truth)
     * Saves a summary file for human browsing convenience (but never reads from it)
     */
    suspend fun loadRunSummaries(): RunSummaries = withContext(Dispatchers.IO) {
        try {
            // Always derive summaries from individual run files (source of truth)
            logger.info { "📊 Deriving run summaries from individual runs..." }
            val profiles = mutableMapOf<String, ProfileSummary>()

            // Find all unique profile IDs
            val allRuns = loadAllRuns()
            val profileIds = allRuns.map { it.profileId }.distinct()

            for (profileId in profileIds) {
                profiles[profileId] = deriveBestSplits(profileId)
            }

            val summaries = RunSummaries(
                version = "1.0.0",
                lastUpdated = Clock.System.now(),
                profiles = profiles
            )

            // Save for human browsing convenience (but never read from it)
            try {
                saveRunSummaries(summaries)
                logger.debug { "📄 Saved run summaries cache for browsing" }
            } catch (e: Exception) {
                logger.warn(e) { "⚠️ Failed to save run summaries cache (non-critical)" }
            }

            summaries
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to derive run summaries, returning empty" }
            RunSummaries(
                version = "1.0.0",
                lastUpdated = Clock.System.now(),
                profiles = emptyMap()
            )
        }
    }

    /**
     * Update splits state to use new format data
     * Calculates "Best Possible" times from best segments across all runs
     */
    suspend fun loadSplitsStateFromNewFormat(): SplitsState = withContext(Dispatchers.IO) {
        try {
            val summaries = loadRunSummaries()

            if (summaries.profiles.isEmpty()) {
                logger.info { "📄 No run summaries found, returning empty state" }
                return@withContext SplitsState()
            }

            val personalBests = mutableMapOf<String, PersonalBest>()
            val allPbRuns = mutableListOf<RunSession>()

            for ((profileId, profileSummary) in summaries.profiles) {
                val splitTimes = profileSummary.bestSplitTimes.mapValues { (_, bestSplit) ->
                    SplitTime(
                        totalTime = bestSplit.totalTime,
                        segmentTime = bestSplit.segmentTime,
                        delta = 0,
                        originalDelta = 0
                    )
                }

                val bestPossibleTime = splitTimes.values.sumOf { it.segmentTime }
                logger.info { "📊 Loaded best segments for $profileId - PB: ${formatTime(profileSummary.bestTotalTime ?: 0)}, Best Possible: ${formatTime(bestPossibleTime)}" }

                val pbRunId = profileSummary.bestRunId ?: ""
                if (pbRunId.isNotEmpty()) {
                    loadRunById(pbRunId)?.let { allPbRuns.add(it) }
                }

                personalBests[profileId] = PersonalBest(
                    profileId = profileId,
                    runSessionId = pbRunId,
                    totalTime = profileSummary.bestTotalTime ?: 0,
                    splitTimes = splitTimes
                )
            }

            SplitsState(
                currentRun = null,
                personalBests = personalBests,
                runHistory = allPbRuns
            )
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to load splits state from new format" }
            SplitsState()
        }
    }

    private fun formatTime(timeMs: Long): String {
        val totalSeconds = timeMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        val millis = (timeMs % 1000) / 10
        return if (hours > 0) {
            "%d:%02d:%02d.%02d".format(hours, minutes, seconds, millis)
        } else {
            "%d:%02d.%02d".format(minutes, seconds, millis)
        }
    }

    // Helper functions

    private fun getProfileName(profileId: String): String {
        return when (profileId) {
            "kpdr-any" -> "KPDR Any%"
            else -> profileId
        }
    }

    private fun findRunFileName(runId: String): String? {
        val runFiles = runsDir.listFiles { file -> file.isFile && file.extension == "json" } ?: return null
        for (file in runFiles) {
            try {
                val jsonString = file.readText()
                val run = json.decodeFromString<RunSession>(jsonString)
                if (run.id == runId) {
                    return file.name
                }
            } catch (e: Exception) {
                // Skip files that can't be parsed
            }
        }
        return null
    }
}
