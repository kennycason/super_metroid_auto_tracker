package com.supermetroid.storage

import com.supermetroid.model.SplitsState
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

private val logger = KotlinLogging.logger {}

/**
 * File-based storage service for splits data and personal bests
 * Stores data in ~/.smtracker/ directory to match the old system
 */
class FileStorageService {
    private val homeDir = System.getProperty("user.home")
    private val trackerDir = File(homeDir, ".smtracker")
    private val splitsFile = File(trackerDir, "splits-data.json")
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    init {
        // Ensure directory exists
        if (!trackerDir.exists()) {
            trackerDir.mkdirs()
            logger.info { "📁 Created tracker directory: ${trackerDir.absolutePath}" }
        }
    }
    
    /**
     * Save splits state to file
     */
    suspend fun saveSplitsState(splitsState: SplitsState) = withContext(Dispatchers.IO) {
        try {
            val jsonString = json.encodeToString(splitsState)
            splitsFile.writeText(jsonString)
            logger.debug { "💾 Saved splits state to ${splitsFile.absolutePath}" }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to save splits state" }
            throw e
        }
    }
    
    /**
     * Load splits state from file
     */
    suspend fun loadSplitsState(): SplitsState = withContext(Dispatchers.IO) {
        try {
            if (!splitsFile.exists()) {
                logger.info { "📄 No existing splits file found, returning empty state" }
                return@withContext SplitsState()
            }
            
            val jsonString = splitsFile.readText()
            val splitsState = json.decodeFromString<SplitsState>(jsonString)
            logger.info { "📄 Loaded splits state from ${splitsFile.absolutePath}" }
            splitsState
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to load splits state, returning empty state" }
            SplitsState()
        }
    }
    
    /**
     * Check if splits file exists
     */
    fun splitsFileExists(): Boolean = splitsFile.exists()
    
    /**
     * Get splits file path for debugging
     */
    fun getSplitsFilePath(): String = splitsFile.absolutePath
    
    /**
     * Create backup of current splits file
     */
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
}
