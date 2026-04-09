package com.supermetroid.service

import com.supermetroid.autosplits.SplitProfiles
import com.supermetroid.livesplit.LiveSplitConverter
import com.supermetroid.livesplit.LiveSplitComparisonSplit
import com.supermetroid.livesplit.LiveSplitDocument
import com.supermetroid.livesplit.LiveSplitParser
import com.supermetroid.livesplit.LiveSplitSegment
import com.supermetroid.livesplit.LiveSplitWriter
import com.supermetroid.model.AppConfig
import com.supermetroid.model.CompletedSplit
import com.supermetroid.model.PersonalBest
import com.supermetroid.model.RunSession
import com.supermetroid.model.SplitProfile
import com.supermetroid.model.SplitTime
import com.supermetroid.model.SplitsState
import com.supermetroid.storage.FileStorageService
import kotlinx.datetime.Instant
import com.supermetroid.util.Logging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Manages split data format preferences (JSON vs LiveSplit .lss).
 *
 * Supports:
 * - Reading split/PB data from either JSON (our format) or LiveSplit (.lss)
 * - Writing run data to JSON, LiveSplit, or both
 * - Selecting and loading a specific .lss file
 * - Re-loading data when the read format is toggled
 */
class SplitFormatService(
    private val fileStorageService: FileStorageService,
    private val splitProfileService: SplitProfileService
) : Logging {

    enum class ReadFormat(val displayName: String) {
        JSON("JSON (Tracker)"),
        LIVESPLIT("LiveSplit (.lss)")
    }

    private val parser = LiveSplitParser()
    private val writer = LiveSplitWriter()
    private val converter = LiveSplitConverter()

    private val _readFormat = MutableStateFlow(ReadFormat.JSON)
    val readFormat: StateFlow<ReadFormat> = _readFormat.asStateFlow()

    private val _writeJson = MutableStateFlow(true)
    val writeJson: StateFlow<Boolean> = _writeJson.asStateFlow()

    private val _writeLiveSplit = MutableStateFlow(false)
    val writeLiveSplit: StateFlow<Boolean> = _writeLiveSplit.asStateFlow()

    private val _liveSplitFilePath = MutableStateFlow<String?>(null)
    val liveSplitFilePath: StateFlow<String?> = _liveSplitFilePath.asStateFlow()

    // Track which run we last backed up LSS for, to avoid redundant backups per auto-skip
    private var lastBackedUpRunId: String? = null

    private val _liveSplitDocument = MutableStateFlow<LiveSplitDocument?>(null)
    val liveSplitDocument: StateFlow<LiveSplitDocument?> = _liveSplitDocument.asStateFlow()

    private val _liveSplitProfile = MutableStateFlow<SplitProfile?>(null)
    val liveSplitProfile: StateFlow<SplitProfile?> = _liveSplitProfile.asStateFlow()

    private val _liveSplitPersonalBest = MutableStateFlow<PersonalBest?>(null)
    val liveSplitPersonalBest: StateFlow<PersonalBest?> = _liveSplitPersonalBest.asStateFlow()

    suspend fun initialize() {
        val config = fileStorageService.loadAppConfig()
        _readFormat.value = if (config.splitReadFormat == "livesplit") ReadFormat.LIVESPLIT else ReadFormat.JSON
        _writeJson.value = config.splitWriteJson
        _writeLiveSplit.value = config.splitWriteLiveSplit

        // Migrate legacy single path to per-profile map
        val profileId = splitProfileService.getCurrentProfileId()
        val migratedConfig = migrateLegacyLssPath(config, profileId)

        // Load LSS for current profile
        val pathForProfile = migratedConfig.liveSplitFilePaths[profileId]
        _liveSplitFilePath.value = pathForProfile

        if (pathForProfile != null) {
            loadLiveSplitFile(pathForProfile)
        }

        logger.info { "Split format initialized: read=${_readFormat.value}, writeJson=${_writeJson.value}, writeLSS=${_writeLiveSplit.value}, lssPath=$pathForProfile (profile=$profileId)" }
    }

    /**
     * Migrate legacy single liveSplitFilePath to per-profile map.
     * If the old field has a value and the map is empty, move it to the current profile.
     */
    private suspend fun migrateLegacyLssPath(config: AppConfig, profileId: String): AppConfig {
        if (config.liveSplitFilePath != null && config.liveSplitFilePaths.isEmpty()) {
            logger.info { "Migrating legacy LSS path to per-profile: ${config.liveSplitFilePath} -> $profileId" }
            val updated = config.copy(
                liveSplitFilePath = null,
                liveSplitFilePaths = mapOf(profileId to config.liveSplitFilePath)
            )
            fileStorageService.saveAppConfig(updated)
            return updated
        }
        return config
    }

    /**
     * Switch the active LSS file when the split profile changes.
     * Loads the LSS for the new profile (or clears if none configured).
     */
    suspend fun onProfileChanged(profileId: String) {
        val config = fileStorageService.loadAppConfig()
        val pathForProfile = config.liveSplitFilePaths[profileId]
        _liveSplitFilePath.value = pathForProfile

        if (pathForProfile != null) {
            loadLiveSplitFile(pathForProfile)
        } else {
            _liveSplitDocument.value = null
            _liveSplitProfile.value = null
            _liveSplitPersonalBest.value = null
        }

        logger.info { "Profile switched to $profileId, LSS path: $pathForProfile" }
        onFormatChanged?.invoke()
    }

    /**
     * Check if the current profile has an LSS file configured.
     */
    fun hasLssFileForCurrentProfile(): Boolean {
        return _liveSplitFilePath.value != null
    }

    private var onFormatChanged: (suspend () -> Unit)? = null

    /**
     * Register a callback that fires when the read format changes or new LSS data loads.
     * Used by the main composable to reload data into AutoSplitsEngine.
     */
    fun setOnFormatChangedCallback(callback: suspend () -> Unit) {
        onFormatChanged = callback
    }

    suspend fun setReadFormat(format: ReadFormat) {
        _readFormat.value = format
        persistSettings()
        logger.info { "Read format changed to: $format" }
        onFormatChanged?.invoke()
    }

    suspend fun setWriteJson(enabled: Boolean) {
        _writeJson.value = enabled
        persistSettings()
    }

    suspend fun setWriteLiveSplit(enabled: Boolean) {
        _writeLiveSplit.value = enabled
        persistSettings()
    }

    /**
     * Set the LiveSplit file path and load it immediately.
     * The path is stored under the resolved profile (if the LSS matches a built-in profile)
     * rather than always under the current profile, so the config mapping stays correct
     * when the LSS triggers a profile switch.
     */
    suspend fun setLiveSplitFilePath(path: String?) {
        _liveSplitFilePath.value = path

        if (path != null) {
            loadLiveSplitFile(path)
        } else {
            _liveSplitDocument.value = null
            _liveSplitProfile.value = null
            _liveSplitPersonalBest.value = null
        }

        // Store under resolved profile if possible, otherwise current profile
        val resolvedProfile = getResolvedLiveSplitProfile()
        val profileId = resolvedProfile?.id ?: splitProfileService.getCurrentProfileId()
        val config = fileStorageService.loadAppConfig()
        val updatedPaths = if (path != null) {
            config.liveSplitFilePaths + (profileId to path)
        } else {
            config.liveSplitFilePaths - splitProfileService.getCurrentProfileId()
        }
        fileStorageService.saveAppConfig(config.copy(
            liveSplitFilePaths = updatedPaths,
            splitReadFormat = _readFormat.value.name.lowercase(),
            splitWriteJson = _writeJson.value,
            splitWriteLiveSplit = _writeLiveSplit.value
        ))

        onFormatChanged?.invoke()
    }

    /**
     * Create a new empty LSS file for the given profile in the default storage directory.
     * Returns the path to the created file, or null on failure.
     */
    suspend fun createNewLssFile(profile: SplitProfile): String? {
        return try {
            val segments = profile.splits.map { split ->
                LiveSplitSegment(
                    name = split.name,
                    icon = null,
                    bestSegmentTime = null,
                    splitTimes = listOf(LiveSplitComparisonSplit("Personal Best", null, null)),
                    segmentHistory = emptyList()
                )
            }
            val doc = LiveSplitDocument(
                gameName = profile.name,
                categoryName = "",
                attemptCount = 0,
                segments = segments,
                attemptHistory = emptyList()
            )
            val dir = fileStorageService.getStorageDir()
            val fileName = "${profile.id}.lss"
            val file = File(dir, fileName)
            writer.writeToFile(doc, file)

            val path = file.absolutePath
            setLiveSplitFilePath(path)
            logger.info { "Created new LSS file: $path" }
            path
        } catch (e: Exception) {
            logger.error(e) { "Failed to create new LSS file for profile: ${profile.id}" }
            null
        }
    }

    /**
     * Load and parse a LiveSplit file. Extracts the profile and PB data.
     */
    fun loadLiveSplitFile(path: String): Boolean {
        return try {
            val file = File(path)
            if (!file.exists()) {
                logger.warn { "LiveSplit file not found: $path" }
                return false
            }

            val doc = parser.parseFile(file)
            _liveSplitDocument.value = doc

            val profile = converter.toSplitProfile(doc)
            _liveSplitProfile.value = profile

            val pb = converter.toPersonalBest(doc, profile.id)
            _liveSplitPersonalBest.value = pb

            logger.info { "Loaded LiveSplit file: ${doc.gameName} - ${doc.categoryName} (${doc.segments.size} segments, ${doc.attemptHistory.size} attempts)" }
            true
        } catch (e: Exception) {
            logger.error(e) { "Failed to load LiveSplit file: $path" }
            _liveSplitDocument.value = null
            _liveSplitProfile.value = null
            _liveSplitPersonalBest.value = null
            false
        }
    }

    /**
     * Re-load the current LiveSplit file (e.g., after external changes).
     */
    suspend fun reloadLiveSplitFile(): Boolean {
        val path = _liveSplitFilePath.value ?: return false
        val result = loadLiveSplitFile(path)
        if (result) {
            onFormatChanged?.invoke()
        }
        return result
    }

    /**
     * Save a completed run to LiveSplit format, updating the existing .lss file
     * with a new attempt and updated segment times.
     */
    fun saveRunToLiveSplit(run: RunSession, profile: SplitProfile): Boolean {
        val path = _liveSplitFilePath.value ?: return false
        return try {
            // Backup LSS file once per run (first write), not per auto-skip save
            val file = File(path)
            if (file.exists() && lastBackedUpRunId != run.id) {
                try {
                    fileStorageService.backupFileSync(file)
                    lastBackedUpRunId = run.id
                } catch (e: Exception) {
                    logger.warn(e) { "Failed to backup LSS before write, proceeding anyway" }
                }
            }

            val existingDoc = _liveSplitDocument.value
            val updatedDoc = converter.fromRunSession(run, profile, existingDoc)

            writer.writeToFile(updatedDoc, file)
            _liveSplitDocument.value = updatedDoc

            logger.info { "Saved run to LiveSplit file: $path (attempt #${updatedDoc.attemptCount})" }
            true
        } catch (e: Exception) {
            logger.error(e) { "Failed to save run to LiveSplit: $path" }
            false
        }
    }

    /**
     * Write a run using the configured write formats.
     */
    suspend fun writeRun(run: RunSession, profile: SplitProfile) {
        if (_writeJson.value) {
            fileStorageService.saveRun(run)
        }
        if (_writeLiveSplit.value) {
            saveRunToLiveSplit(run, profile)
        }
    }

    /**
     * Handle a run saved by AutoSplitsEngine. If LiveSplit writing is enabled
     * and a file path is configured, appends the run to the LSS file.
     * Called from the engine's onRunSaved callback.
     */
    fun handleRunSaved(run: RunSession) {
        if (!_writeLiveSplit.value) return
        if (_liveSplitFilePath.value == null) return
        
        val profile = splitProfileService.currentProfile.value
        if (profile.id != run.profileId) {
            logger.debug { "Skipping LSS write: run profile '${run.profileId}' doesn't match current profile '${profile.id}'" }
            return
        }
        
        try {
            saveRunToLiveSplit(run, profile)
        } catch (e: Exception) {
            logger.error(e) { "Failed to write run to LiveSplit after save" }
        }
    }

    /**
     * Delete an attempt from the loaded LSS document and rewrite the file.
     * Also removes matching segment history entries for that attempt ID.
     */
    fun deleteLssAttempt(attemptId: Int): Boolean {
        val doc = _liveSplitDocument.value ?: return false
        val path = _liveSplitFilePath.value ?: return false
        val file = File(path)

        val updatedAttempts = doc.attemptHistory.filter { it.id != attemptId }
        if (updatedAttempts.size == doc.attemptHistory.size) {
            logger.warn { "LSS attempt $attemptId not found" }
            return false
        }

        // Also remove segment history entries for this attempt
        val updatedSegments = doc.segments.map { segment ->
            segment.copy(segmentHistory = segment.segmentHistory.filter { it.id != attemptId })
        }

        val updatedDoc = doc.copy(
            attemptCount = updatedAttempts.size,
            attemptHistory = updatedAttempts,
            segments = updatedSegments
        )

        return try {
            fileStorageService.backupFileSync(file)
            writer.writeToFile(updatedDoc, file)
            _liveSplitDocument.value = updatedDoc
            logger.info { "🗑️ Deleted LSS attempt #$attemptId, ${updatedAttempts.size} attempts remaining" }
            true
        } catch (e: Exception) {
            logger.error(e) { "Failed to delete LSS attempt #$attemptId" }
            false
        }
    }

    /**
     * Check if we're reading from LiveSplit and have a loaded document.
     */
    fun isLiveSplitActive(): Boolean {
        return _readFormat.value == ReadFormat.LIVESPLIT && _liveSplitDocument.value != null
    }

    /**
     * If the loaded LSS matches a built-in profile (same split ids in order),
     * return the canonical profile. Use this when switching to LiveSplit so the UI
     * uses the same profile id as JSON mode (e.g. "kpdr-any").
     */
    fun getResolvedLiveSplitProfile(): SplitProfile? {
        val lssProfile = _liveSplitProfile.value ?: return null
        return resolveCanonicalProfile(lssProfile)
    }

    /**
     * If the LSS-derived profile matches a built-in profile (same split ids in order),
     * return the canonical profile so the UI and state use a consistent id (e.g. "kpdr-any").
     */
    private fun resolveCanonicalProfile(lssProfile: SplitProfile): SplitProfile {
        val lssIds = lssProfile.splits.map { it.id }
        for (builtIn in SplitProfiles.ALL_PROFILES) {
            if (builtIn.splits.map { it.id } == lssIds) {
                return builtIn
            }
        }
        return lssProfile
    }

    /**
     * Build a synthetic RunSession from PB data so runHistory can feed the BEST column
     * (findActualPbRun) and any other logic that expects the PB run in history.
     *
     * NOTE: pb.splitTimes contains *best segment* times in segmentTime (best across all
     * attempts) and *PB cumulative* times in totalTime. The BEST column uses totalTime
     * directly (not segment sums) so that it shows the actual PB run's cumulative pace.
     */
    private fun syntheticPbRun(profile: SplitProfile, pb: PersonalBest): RunSession {
        val baseTime = Instant.fromEpochMilliseconds(0L)
        val completedSplits = profile.splits.map { split ->
            val st = pb.splitTimes[split.id] ?: return@map null
            CompletedSplit(
                splitId = split.id,
                time = st,
                timestamp = baseTime
            )
        }.filterNotNull()
        return RunSession(
            id = "livesplit-pb",
            profileId = profile.id,
            startTime = baseTime,
            endTime = Instant.fromEpochMilliseconds(pb.totalTime),
            completedSplits = completedSplits,
            totalTime = pb.totalTime,
            isPersonalBest = true
        )
    }

    /**
     * Build a [SplitsState] from the currently loaded LiveSplit file.
     * Uses canonical profile id when the LSS matches a built-in profile so the UI
     * finds personalBests[profileId]. Includes a synthetic PB run in runHistory so
     * the BEST column and BP delta have the data they need (same as JSON load).
     */
    fun toLiveSplitSplitsState(): SplitsState? {
        val doc = _liveSplitDocument.value ?: return null
        val lssProfile = _liveSplitProfile.value ?: return null
        val pb = _liveSplitPersonalBest.value ?: return null

        val profile = resolveCanonicalProfile(lssProfile)
        val pbWithCanonicalId = pb.copy(profileId = profile.id)

        // Build full run history from LSS attempt/segment data for stats
        val lssRuns = converter.toRunHistory(doc, profile.id)

        // Also include synthetic PB run so findActualPbRun() works for BEST column
        val syntheticRun = syntheticPbRun(profile, pbWithCanonicalId)
        val allRuns = lssRuns + syntheticRun

        return SplitsState(
            currentRun = null,
            personalBests = mapOf(profile.id to pbWithCanonicalId),
            runHistory = allRuns
        )
    }

    private suspend fun persistSettings() {
        try {
            val config = fileStorageService.loadAppConfig()
            fileStorageService.saveAppConfig(
                config.copy(
                    splitReadFormat = _readFormat.value.name.lowercase(),
                    splitWriteJson = _writeJson.value,
                    splitWriteLiveSplit = _writeLiveSplit.value
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to persist split format settings" }
        }
    }
}
