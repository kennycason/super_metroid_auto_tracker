package com.supermetroid.service

import com.supermetroid.livesplit.LiveSplitConverter
import com.supermetroid.livesplit.LiveSplitDocument
import com.supermetroid.livesplit.LiveSplitParser
import com.supermetroid.livesplit.LiveSplitWriter
import com.supermetroid.model.PersonalBest
import com.supermetroid.model.RunSession
import com.supermetroid.model.SplitProfile
import com.supermetroid.model.SplitsState
import com.supermetroid.storage.FileStorageService
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
    private val fileStorageService: FileStorageService
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
        _liveSplitFilePath.value = config.liveSplitFilePath

        if (_liveSplitFilePath.value != null) {
            loadLiveSplitFile(_liveSplitFilePath.value!!)
        }

        logger.info { "Split format initialized: read=${_readFormat.value}, writeJson=${_writeJson.value}, writeLSS=${_writeLiveSplit.value}" }
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
     */
    suspend fun setLiveSplitFilePath(path: String?) {
        _liveSplitFilePath.value = path
        persistSettings()

        if (path != null) {
            loadLiveSplitFile(path)
        } else {
            _liveSplitDocument.value = null
            _liveSplitProfile.value = null
            _liveSplitPersonalBest.value = null
        }
        onFormatChanged?.invoke()
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
    fun reloadLiveSplitFile(): Boolean {
        val path = _liveSplitFilePath.value ?: return false
        return loadLiveSplitFile(path)
    }

    /**
     * Save a completed run to LiveSplit format, updating the existing .lss file
     * with a new attempt and updated segment times.
     */
    fun saveRunToLiveSplit(run: RunSession, profile: SplitProfile): Boolean {
        val path = _liveSplitFilePath.value ?: return false
        return try {
            val existingDoc = _liveSplitDocument.value
            val updatedDoc = converter.fromRunSession(run, profile, existingDoc)

            val file = File(path)
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
     * Check if we're reading from LiveSplit and have a loaded document.
     */
    fun isLiveSplitActive(): Boolean {
        return _readFormat.value == ReadFormat.LIVESPLIT && _liveSplitDocument.value != null
    }

    /**
     * Build a [SplitsState] from the currently loaded LiveSplit file.
     * Used during initialization to feed PB data into AutoSplitsEngine.
     */
    fun toLiveSplitSplitsState(): SplitsState? {
        _liveSplitDocument.value ?: return null
        val profile = _liveSplitProfile.value ?: return null
        val pb = _liveSplitPersonalBest.value ?: return null

        return SplitsState(
            currentRun = null,
            personalBests = mapOf(profile.id to pb),
            runHistory = emptyList()
        )
    }

    private suspend fun persistSettings() {
        try {
            val config = fileStorageService.loadAppConfig()
            fileStorageService.saveAppConfig(
                config.copy(
                    splitReadFormat = _readFormat.value.name.lowercase(),
                    splitWriteJson = _writeJson.value,
                    splitWriteLiveSplit = _writeLiveSplit.value,
                    liveSplitFilePath = _liveSplitFilePath.value
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to persist split format settings" }
        }
    }
}
