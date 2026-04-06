package com.supermetroid.autosplits

import com.supermetroid.gamestate.GameStateConstants
import com.supermetroid.gamestate.RoomIds
import com.supermetroid.model.*
import com.supermetroid.storage.FileStorageService
import com.supermetroid.util.Logging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.math.abs

/**
 * AutoSplits engine for detecting split conditions and managing run sessions
 */
class AutoSplitsEngine(private val fileStorageService: FileStorageService? = null) : Logging {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var previousGameState: GameState? = null
    private var currentProfile: SplitProfile? = null
    private var currentSplitIndex = 0
    private var pauseStartTime: kotlinx.datetime.Instant? = null

    // For debouncing toggleRunState calls
    private var lastToggleTime: Long = 0
    private val debounceTimeMs: Long = 300 // 300ms debounce window

    // Auto-start control - can be disabled if user is manually managing timer
    var autoStartEnabled: Boolean = true
        private set
    
    // Stores the personalBests at the START of the current run
    // Used to freeze the display after run completion so BP Δ doesn't immediately go to ±0:00
    private var preRunPersonalBests: Map<String, PersonalBest> = emptyMap()
    
    // Callback for notifying SplitProfileService when profile changes from within engine
    // This avoids circular dependency while enabling bidirectional sync
    private var onProfileChangedFromEngine: (suspend (SplitProfile) -> Unit)? = null
    
    // Callback fired after a run is saved to disk (used by SplitFormatService to also write LSS)
    private var onRunSaved: ((RunSession) -> Unit)? = null
    
    /**
     * Set callback for profile changes originating from the engine (e.g., loading a replay run)
     * Used by SplitProfileService to sync state without circular constructor dependencies
     */
    fun setOnProfileChangedCallback(callback: suspend (SplitProfile) -> Unit) {
        onProfileChangedFromEngine = callback
    }
    
    /**
     * Set callback fired after a run is saved to JSON.
     * Used by SplitFormatService to also write to LiveSplit (.lss) when enabled.
     */
    fun setOnRunSavedCallback(callback: (RunSession) -> Unit) {
        onRunSaved = callback
    }

    // State flows for reactive UI
    private val _splitsState = MutableStateFlow(SplitsState())
    val splitsState: StateFlow<SplitsState> = _splitsState.asStateFlow()

    /**
     * Initialize engine and restore saved timer state if present
     */
    suspend fun initialize() {
        try {
            fileStorageService?.let { storage ->
                val config = storage.loadAppConfig()
                val savedTimerMs = config.savedTimerMs
                val savedProfileId = config.savedTimerProfileId
                
                if (savedTimerMs != null && savedTimerMs > 0 && savedProfileId != null) {
                    logger.info { "⏱️ Restoring saved timer: ${formatTime(savedTimerMs)} for profile $savedProfileId" }
                    setTimer(savedTimerMs, savedProfileId)
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to restore saved timer state" }
        }
    }

    /**
     * Load a specific run in replay mode for reviewing stats
     * This loads all runs that came before the target run to calculate accurate Best Possible times,
     * then displays the target run as if it just finished
     */
    suspend fun loadReplayRun(runFileName: String) {
        logger.info { "🎬 Loading replay run: $runFileName" }
        
        try {
            fileStorageService?.let { storage ->
                // Load all runs
                val allRuns = storage.loadAllRuns()
                
                // Find the target run by filename
                val targetRun = storage.loadRunByFileName(runFileName)
                if (targetRun == null) {
                    logger.error { "❌ Could not find run file: $runFileName" }
                    return
                }
                
                logger.info { "✓ Found target run: ${targetRun.id}" }
                logger.info { "  Profile: ${targetRun.profileId}" }
                logger.info { "  Start time: ${targetRun.startTime}" }
                logger.info { "  Total time: ${formatTime(targetRun.totalTime)}" }
                logger.info { "  Splits: ${targetRun.completedSplits.size}" }
                
                // Get target run's start time to filter previous runs
                val targetStartTime = targetRun.startTime
                
                // Filter runs that came before this run (by start time)
                val previousRuns = allRuns.filter { it.startTime < targetStartTime && it.endTime != null }
                logger.info { "📊 Found ${previousRuns.size} completed runs before target run" }
                
                // Calculate best segments from previous runs only
                // IMPORTANT: Always mark replay runs as paused to prevent timer from advancing
                val replayRun = targetRun.copy(isPaused = true)
                
                val updatedState = SplitsState(
                    currentRun = replayRun, // Set as current run (paused)
                    personalBests = emptyMap(),
                    runHistory = previousRuns // Include previous runs for BP calculation
                )
                
                // Update personal bests from previous runs
                val finalState = updatePersonalBestsFromRunHistory(updatedState)
                
                // Set the state
                _splitsState.value = finalState
                
                // Load the profile and sync to SplitProfileService
                val newProfile = SplitProfiles.getProfileById(targetRun.profileId)
                currentProfile = newProfile
                currentSplitIndex = targetRun.completedSplits.size
                
                // Notify SplitProfileService of the profile change
                onProfileChangedFromEngine?.invoke(newProfile)
                
                logger.info { "✅ Replay mode loaded successfully" }
                logger.info { "  Displaying run with ${targetRun.completedSplits.size} splits" }
                logger.info { "  Best Possible calculated from ${previousRuns.size} previous runs" }
            }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to load replay run" }
        }
    }

    /**
     * Reset to current state - reload all runs and show true personal bests
     * Use this to exit replay mode and return to normal tracking
     */
    suspend fun resetToCurrentState() {
        logger.info { "🔄 Resetting to current state (exiting replay mode)" }
        
        try {
            fileStorageService?.let { storage ->
                // Reload the full splits state from disk (all runs, true PB)
                val savedState = storage.loadSplitsState()
                
                // Clear current run (start fresh, not mid-run)
                val resetState = savedState.copy(currentRun = null)
                
                // Load the saved state
                loadSavedState(resetState)
                
                // Reset the split index
                currentSplitIndex = 0
                
                val pbTime = savedState.personalBests.values.firstOrNull()?.totalTime ?: 0
                logger.info { "✅ Reset to current state - PB: ${formatTime(pbTime)} from ${savedState.runHistory.size} total runs" }
            }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to reset to current state" }
        }
    }

    /**
     * Load a split profile
     * If there's a current run from a different profile, clear it to avoid showing mismatched data
     */
    fun loadProfile(profile: SplitProfile) {
        val currentRun = _splitsState.value.currentRun
        
        // If there's a current run from a different profile, clear it
        if (currentRun != null && currentRun.profileId != profile.id) {
            logger.info { "📋 Profile changed from ${currentRun.profileId} to ${profile.id}, clearing mismatched run" }
            _splitsState.value = _splitsState.value.copy(currentRun = null)
            currentSplitIndex = 0
        }
        
        currentProfile = profile
        logger.info { "📋 Loaded split profile: ${profile.name} with ${profile.splits.size} splits" }
    }

    /**
     * Load saved splits state and resume from current position
     */
    fun loadSavedState(savedState: SplitsState) {
        logger.info { "📄 Loading saved state - PersonalBests: ${savedState.personalBests.size}, RunHistory: ${savedState.runHistory.size}" }

        // Preserve manually-set timer if there's no saved run to restore
        val existingTimerRun = _splitsState.value.currentRun
        val hasTimerOnlyRun = existingTimerRun != null && 
                              existingTimerRun.completedSplits.isEmpty() && 
                              existingTimerRun.isPaused

        // First, update personal bests from run history
        val updatedState = updatePersonalBestsFromRunHistory(savedState)
        
        // If there's no saved run but we have a timer-only run, preserve it
        val finalState = if (updatedState.currentRun == null && hasTimerOnlyRun && existingTimerRun != null) {
            logger.info { "⏱️ Preserving manually-set timer: ${formatTime(existingTimerRun.totalTime)}" }
            // Also preserve the pause start time for the timer run
            if (pauseStartTime == null && existingTimerRun.isPaused) {
                pauseStartTime = Clock.System.now()
            }
            updatedState.copy(currentRun = existingTimerRun)
        } else {
            updatedState
        }
        
        _splitsState.value = finalState

        val currentRun = finalState.currentRun
        if (currentRun != null) {
            val profile = currentProfile ?: SplitProfiles.getProfileById(currentRun.profileId)
            currentSplitIndex = currentRun.completedSplits.size

            logger.info { "🔄 Resumed run ${currentRun.id} at split ${currentSplitIndex}/${profile.splits.size}" }

            // If paused, set pause start time to now for accurate calculation
            if (currentRun.isPaused) {
                pauseStartTime = Clock.System.now()
                logger.info { "⏸️ Run is paused" }
            }
        }
    }

    /**
     * Update personal bests from run history by comparing individual segments
     * This ensures that the best segments are always used, even if the overall run wasn't a PB
     */
    private fun updatePersonalBestsFromRunHistory(state: SplitsState): SplitsState {
        if (state.runHistory.isEmpty()) {
            return state
        }

        logger.info { "🔄 Updating personal bests from run history (${state.runHistory.size} runs)" }

        // Start with existing personal bests
        val updatedPersonalBests = state.personalBests.toMutableMap()

        // Process each run in history
        // ONLY include completed runs (endTime != null) for Best Possible calculation
        for (run in state.runHistory) {
            val profileId = run.profileId

            // Skip incomplete runs - only count segments from finished runs
            if (run.endTime == null || run.completedSplits.isEmpty()) {
                continue
            }

            // Get or create personal best for this profile
            val currentPB = updatedPersonalBests[profileId] ?: PersonalBest(
                profileId = profileId,
                runSessionId = run.id,
                totalTime = run.totalTime,
                splitTimes = emptyMap()
            )

            // Create a map of updated split times
            val updatedSplitTimes = currentPB.splitTimes.toMutableMap()

            // Check each split in the run
            for (split in run.completedSplits) {
                val splitId = split.splitId
                val segmentTime = split.time.segmentTime

                // Get current best for this split
                val currentBestSplit = currentPB.splitTimes[splitId]

                // Update if this segment is faster or there's no existing best
                if (currentBestSplit == null || segmentTime < currentBestSplit.segmentTime) {
                    logger.info { "🎯 Found better segment for $splitId: ${formatTime(segmentTime)} (was ${currentBestSplit?.let { formatTime(it.segmentTime) } ?: "N/A"})" }

                    // Calculate delta if there was a previous best
                    val delta = currentBestSplit?.let { segmentTime - it.segmentTime }

                    // Create new split time with this segment as best
                    val newSplitTime = SplitTime(
                        totalTime = split.time.totalTime,
                        segmentTime = segmentTime,
                        delta = 0, // Delta is 0 for a PB
                        originalDelta = delta // Preserve original delta
                    )

                    updatedSplitTimes[splitId] = newSplitTime
                }
            }

            // Update personal best with new split times AND check for faster total time
            val isFasterRun = run.totalTime < currentPB.totalTime
            if (updatedSplitTimes != currentPB.splitTimes || isFasterRun) {
                val updatedPB = currentPB.copy(
                    splitTimes = updatedSplitTimes,
                    totalTime = if (isFasterRun) run.totalTime else currentPB.totalTime,
                    runSessionId = if (isFasterRun) run.id else currentPB.runSessionId
                )
                updatedPersonalBests[profileId] = updatedPB
                
                if (isFasterRun) {
                    logger.info { "🏆 Found faster PB for $profileId: ${formatTime(run.totalTime)} (was ${formatTime(currentPB.totalTime)})" }
                }
            }
        }

        // DO NOT include current active run in Best Possible calculation
        // Best Possible should only be calculated from COMPLETED runs that are saved to disk
        // When a run completes, it's saved to disk and will be included in the next BP calculation

        // Return updated state
        return state.copy(personalBests = updatedPersonalBests)
    }

    /**
     * Calculate average segment times from all completed runs
     * Returns a map of splitId to average segment time in milliseconds
     */
    fun getAverageSegmentTimes(profileId: String = SplitProfiles.DEFAULT.id): Map<String, Long> {
        val state = _splitsState.value
        val completedRuns = state.runHistory.filter { 
            it.profileId == profileId && it.endTime != null && it.completedSplits.isNotEmpty()
        }
        
        if (completedRuns.isEmpty()) {
            return emptyMap()
        }
        
        // Collect all segment times for each split
        val segmentTimesBySplit = mutableMapOf<String, MutableList<Long>>()
        
        for (run in completedRuns) {
            for (split in run.completedSplits) {
                val times = segmentTimesBySplit.getOrPut(split.splitId) { mutableListOf() }
                times.add(split.time.segmentTime)
            }
        }
        
        // Calculate average for each split
        return segmentTimesBySplit.mapValues { (_, times) ->
            times.sum() / times.size
        }
    }

    /**
     * Start a new run or toggle pause/resume
     * Includes debounce logic to prevent rapid consecutive calls
     */
    fun toggleRunState(profileId: String = currentProfile?.id ?: SplitProfiles.DEFAULT.id) {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastToggle = currentTime - lastToggleTime

        // Check if this call is within the debounce window
        if (timeSinceLastToggle < debounceTimeMs) {
            logger.info { "⏱️ toggleRunState called too quickly (${timeSinceLastToggle}ms since last call) - DEBOUNCED" }
            // Log full stack trace for debugging
            val stackTrace = Thread.currentThread().stackTrace.joinToString("\n  at ")
            logger.info { "⏱️ DEBOUNCED CALL STACK:\n  at $stackTrace" }
            return
        }

        // Update last toggle time
        lastToggleTime = currentTime

        val currentRun = _splitsState.value.currentRun

        // Log detailed timer state for debugging
        val runState = if (currentRun == null) {
            "no run"
        } else {
            val pauseStatus = if (currentRun.isPaused) "paused" else "running"
            val totalTime = formatTime(currentRun.totalTime)
            val pausedTime = formatTime(currentRun.pausedTime)
            val runningTime = if (currentRun.isPaused) {
                totalTime
            } else {
                val rawTime = System.currentTimeMillis() - currentRun.startTime.toEpochMilliseconds() - currentRun.pausedTime
                formatTime(rawTime)
            }
            "$pauseStatus (total: $totalTime, paused: $pausedTime, current: $runningTime)"
        }

        logger.info { "⏱️ toggleRunState called - current state: $runState" }

        when {
            currentRun == null -> {
                logger.info { "⏱️ Starting new run" }
                startNewRun(profileId)
            }
            currentRun.isPaused -> {
                logger.info { "⏱️ Resuming paused run" }
                resumeRun()
            }
            else -> {
                logger.info { "⏱️ Pausing running run" }
                pauseRun()
            }
        }

        // Log the state after the operation
        val updatedRun = _splitsState.value.currentRun
        val updatedState = if (updatedRun == null) {
            "no run (reset)"
        } else {
            val pauseStatus = if (updatedRun.isPaused) "paused" else "running"
            val totalTime = formatTime(updatedRun.totalTime)
            val pausedTime = formatTime(updatedRun.pausedTime)
            "$pauseStatus (total: $totalTime, paused: $pausedTime)"
        }
        logger.info { "⏱️ After toggleRunState - new state: $updatedState" }
    }

    /**
     * Enable or disable auto-start
     * This can be called when the timer visibility is toggled
     */
    fun setAutoStartEnabled(enabled: Boolean) {
        autoStartEnabled = enabled
        logger.info { if (enabled) "✅ Auto-start enabled" else "🚫 Auto-start disabled" }
    }

    /**
     * Start a completely new run
     */
    internal fun startNewRun(profileId: String = currentProfile?.id ?: SplitProfiles.DEFAULT.id) {
        // Re-enable auto-start when starting a new run (unless user manually set time)
        autoStartEnabled = true
        
        // Log the current state before starting a new run
        val currentRun = _splitsState.value.currentRun
        if (currentRun != null) {
            logger.warn { "🚨 Starting new run while another run exists! Current run state: ${if (currentRun.isPaused) "paused" else "running"}" }
        }

        // Generate a unique run ID
        val runId = generateRunId()
        logger.info { "🏁 Generating new run with ID: $runId" }

        // Get the exact start time
        val startTime = Clock.System.now()
        logger.info { "🏁 Run start time: $startTime" }

        // Create the new run session
        val newRun = RunSession(
            id = runId,
            profileId = profileId,
            startTime = startTime,
            endTime = null,
            completedSplits = emptyList(),
            totalTime = 0,
            isPaused = false,
            pausedTime = 0
        )

        // Reset the split index and pause state
        currentSplitIndex = 0
        pauseStartTime = null

        // Update the state with the new run
        val currentState = _splitsState.value
        
        // Recalculate Best Possible from all completed runs before starting the new run
        // This ensures BP Δ compares against the latest Best Possible
        val updatedState = updatePersonalBestsFromRunHistory(currentState)
        
        // IMPORTANT: Capture the personalBests at run start
        // This is used to freeze the display after run completion
        // so BP Δ doesn't immediately show ±0:00 for all segments
        preRunPersonalBests = updatedState.personalBests
        
        _splitsState.value = updatedState.copy(currentRun = newRun)

        // Add stack trace to see where this is being called from
        val stackTrace = Thread.currentThread().stackTrace
        val caller = stackTrace.getOrNull(3)?.toString() ?: "unknown"
        logger.info { "🏁 New run started from: $caller" }

        logger.info { "🏁 Started new run: ${newRun.id} with profile: $profileId" }
    }

    /**
     * Pause the current run
     */
    private fun pauseRun() {
        val currentRun = _splitsState.value.currentRun ?: return

        // Calculate the current running time
        val runningTime = System.currentTimeMillis() - currentRun.startTime.toEpochMilliseconds() - currentRun.pausedTime
        logger.info { "⏸️ Pausing run ${currentRun.id} - current time: ${formatTime(runningTime)}, paused time: ${formatTime(currentRun.pausedTime)}" }

        // Record the exact pause start time
        pauseStartTime = Clock.System.now()
        logger.info { "⏸️ Pause start time set to: ${pauseStartTime}" }

        // Update the state to mark the run as paused AND update totalTime to the current running time
        val currentState = _splitsState.value
        _splitsState.value = currentState.copy(
            currentRun = currentRun.copy(
                isPaused = true,
                totalTime = runningTime  // Update totalTime to current running time
            )
        )

        logger.info { "⏸️ Run ${currentRun.id} paused successfully at ${formatTime(runningTime)}" }
    }

    /**
     * Resume the current run
     */
    private fun resumeRun() {
        val currentRun = _splitsState.value.currentRun ?: return

        val now = Clock.System.now()
        val newPausedTime: Long

        if (pauseStartTime != null) {
            // Normal resume: calculate pause duration from when we paused
            val pauseDuration = (now - pauseStartTime!!).inWholeMilliseconds
            newPausedTime = currentRun.pausedTime + pauseDuration
            logger.info { "▶️ Resuming run ${currentRun.id} - pause duration: ${formatTime(pauseDuration)}, total paused time: ${formatTime(newPausedTime)}" }
        } else {
            // Loaded from disk: calculate the gap between run state and now
            // Timer formula is: now - startTime - pausedTime
            // We want the display to resume from totalTime, so:
            // totalTime = now - startTime - newPausedTime
            // newPausedTime = now - startTime - totalTime
            val gapMs = now.toEpochMilliseconds() - currentRun.startTime.toEpochMilliseconds() -
                currentRun.totalTime - currentRun.pausedTime
            newPausedTime = currentRun.pausedTime + gapMs
            logger.info { "▶️ Resuming run ${currentRun.id} from disk - gap: ${formatTime(gapMs)}, total paused time: ${formatTime(newPausedTime)}" }
        }

        // Reset the pause start time
        pauseStartTime = null

        // Update the state to mark the run as resumed and update the paused time
        val currentState = _splitsState.value
        _splitsState.value = currentState.copy(
            currentRun = currentRun.copy(
                isPaused = false,
                pausedTime = newPausedTime
            )
        )

        logger.info { "▶️ Run ${currentRun.id} resumed successfully" }
        
        // Clear saved timer from config since run is now active
        clearSavedTimer()
    }

    /**
     * Reset current run
     * Saves the run (if it has any completed splits) before resetting
     */
    fun resetRun() {
        // Log the current state before reset
        val currentRun = _splitsState.value.currentRun
        val runState = if (currentRun == null) {
            "no run (already reset)"
        } else {
            val pauseStatus = if (currentRun.isPaused) "paused" else "running"
            val totalTime = formatTime(currentRun.totalTime)
            val pausedTime = formatTime(currentRun.pausedTime)
            val runningTime = if (currentRun.isPaused) {
                totalTime
            } else {
                val rawTime = System.currentTimeMillis() - currentRun.startTime.toEpochMilliseconds() - currentRun.pausedTime
                formatTime(rawTime)
            }
            "$pauseStatus (total: $totalTime, paused: $pausedTime, current: $runningTime)"
        }

        logger.info { "🔄 resetRun called - current state: $runState" }

        // Save the run before resetting (if it has any completed splits)
        // This preserves segment PB data from partial runs
        // IMPORTANT: Don't save if this is a completed run being viewed in replay mode!
        // A completed run has endTime set, meaning it was already saved properly.
        val isCompletedRunInReplay = currentRun?.endTime != null
        
        if (isCompletedRunInReplay) {
            logger.info { "🎬 Viewing completed run in replay mode - not saving (run already exists on disk)" }
        } else if (currentRun != null && currentRun.completedSplits.isNotEmpty()) {
            logger.info { "💾 Saving partial run with ${currentRun.completedSplits.size} completed splits before reset" }

            // Calculate current time for the saved run
            val finalTime = if (currentRun.isPaused) {
                currentRun.totalTime
            } else {
                System.currentTimeMillis() - currentRun.startTime.toEpochMilliseconds() - currentRun.pausedTime
            }

            // Don't set endTime for reset runs - only completed runs should have endTime
            // This prevents incomplete runs from being counted as PBs
            val runToSave = currentRun.copy(
                endTime = null,  // Reset runs should NOT have endTime
                totalTime = finalTime
            )

            // Save asynchronously to avoid blocking the reset
            fileStorageService?.let { storage ->
                scope.launch {
                    try {
                        storage.saveRun(runToSave)
                        onRunSaved?.invoke(runToSave)
                        logger.info { "💾 Saved partial run to runs/ directory (${currentRun.completedSplits.size} splits, ${formatTime(finalTime)})" }

                        // Update run summaries to include segment PBs from this partial run
                        val summaries = storage.loadRunSummaries()
                        val updatedProfile = storage.deriveBestSplits(runToSave.profileId)
                        val updatedSummaries = summaries.copy(
                            lastUpdated = Clock.System.now(),
                            profiles = summaries.profiles + (runToSave.profileId to updatedProfile)
                        )
                        storage.saveRunSummaries(updatedSummaries)
                        logger.info { "📊 Updated run summaries with partial run segments" }
                    } catch (e: Exception) {
                        logger.error(e) { "❌ Failed to save partial run" }
                    }
                }
            }
        } else if (currentRun != null) {
            logger.info { "⏭️ Not saving run - no completed splits" }
        }

        // Reset the state
        currentSplitIndex = 0
        // Keep previousGameState so the next processGameState poll can detect
        // a valid state transition (e.g., title screen 2 → 31) for auto-start.
        // Clearing it would cause the first poll after reset to miss the transition.
        pauseStartTime = null  // Ensure pauseStartTime is reset

        val currentState = _splitsState.value

        // Recalculate Best Possible from all completed runs (including the one we just finished)
        // This ensures BP Δ on the next run compares against the updated Best Possible
        val updatedState = updatePersonalBestsFromRunHistory(currentState)

        _splitsState.value = updatedState.copy(currentRun = null)

        // Reset the debounce timer to prevent issues with immediate start after reset
        lastToggleTime = 0

        // Re-enable auto-start so the next game start is detected automatically.
        // The transition checks (gameState 2→31, 5→6, Ceres fallback) are specific enough
        // to avoid phantom starts from save file loads.
        autoStartEnabled = true

        logger.info { "🔄 Run reset complete - timer state cleared, auto-start enabled" }
        
        // Clear saved timer from config
        clearSavedTimer()

        // Add a small delay before allowing new toggleRunState calls
        // This helps prevent accidental immediate restart after reset
        // TODO(safety): Convert to a suspend-friendly cooldown using delay(100) by making resetRun() suspend.
        // Avoid blocking UI threads (e.g., Swing dispatcher). Keep Thread.sleep for now to avoid behavior changes.
        Thread.sleep(100)
        logger.info { "🔄 Reset cooldown complete - ready for new run" }
    }

    /**
     * Set the timer to a specific time value
     * If no run exists, creates a paused run with the given time
     * If a run exists, adjusts the start time to reflect the desired time
     *
     * @param timeMs The desired timer value in milliseconds
     * @param profileId The profile ID for the run (default: SplitProfiles.DEFAULT.id)
     */
    fun setTimer(timeMs: Long, profileId: String = SplitProfiles.DEFAULT.id) {
        val currentRun = _splitsState.value.currentRun
        val now = Clock.System.now()

        logger.info { "⏱️ setTimer called - setting timer to ${formatTime(timeMs)}" }
        
        // Disable auto-start when user manually sets a non-zero time
        // Re-enable auto-start if timer is set to 0 (reset)
        if (timeMs > 0) {
            autoStartEnabled = false
            logger.info { "🚫 Auto-start disabled - user manually set timer to ${formatTime(timeMs)}" }
        } else {
            autoStartEnabled = true
            logger.info { "✅ Auto-start re-enabled - timer reset to 0" }
        }

        if (currentRun == null) {
            // No run exists - create a paused run with the given time
            val runId = generateRunId()

            // Calculate the start time that would result in the desired time
            // startTime = now - timeMs
            val adjustedStartTime = kotlinx.datetime.Instant.fromEpochMilliseconds(
                now.toEpochMilliseconds() - timeMs
            )

            val newRun = RunSession(
                id = runId,
                profileId = profileId,
                startTime = adjustedStartTime,
                endTime = null,
                completedSplits = emptyList(),
                totalTime = timeMs,
                isPaused = true,  // Start paused so time doesn't advance
                pausedTime = 0
            )

            // Set pause start time to now
            pauseStartTime = now

            val currentState = _splitsState.value
            _splitsState.value = currentState.copy(currentRun = newRun)

            logger.info { "⏱️ Created new paused run with timer set to ${formatTime(timeMs)}" }
        } else {
            // Run exists - adjust the start time to reflect the desired time
            val wasPaused = currentRun.isPaused

            // Calculate the new start time that would result in the desired time
            // For a running timer: startTime = now - timeMs - pausedTime
            // For a paused timer: startTime = now - timeMs - pausedTime
            val adjustedStartTime = kotlinx.datetime.Instant.fromEpochMilliseconds(
                now.toEpochMilliseconds() - timeMs - currentRun.pausedTime
            )

            val updatedRun = currentRun.copy(
                startTime = adjustedStartTime,
                totalTime = timeMs
            )

            // If the run was paused, update pause start time to now
            if (wasPaused) {
                pauseStartTime = now
            }

            val currentState = _splitsState.value
            _splitsState.value = currentState.copy(currentRun = updatedRun)

            logger.info { "⏱️ Updated existing run timer to ${formatTime(timeMs)} (${if (wasPaused) "paused" else "running"})" }
        }
        
        // Save timer value to config for persistence
        fileStorageService?.let { storage ->
            scope.launch(Dispatchers.IO) {
                try {
                    val config = storage.loadAppConfig()
                    storage.saveAppConfig(config.copy(
                        savedTimerMs = timeMs,
                        savedTimerProfileId = profileId
                    ))
                    logger.debug { "💾 Saved timer value to config: ${formatTime(timeMs)}" }
                } catch (e: Exception) {
                    logger.error(e) { "❌ Failed to save timer value to config" }
                }
            }
        }
    }

    /**
     * Process game state and check for split conditions
     */
    fun processGameState(gameState: GameState) {
        val currentRun = _splitsState.value.currentRun

        // Always log current area for debugging
        logger.debug { "🎮 Current area: ${gameState.areaName} (ID: ${gameState.areaId}), gameState: ${gameState.gameState}" }

        // Check for auto-start conditions BEFORE filtering invalid states
        // This allows detection of title screen transitions (gameState 2 → 31) for ASL-accurate timing
        if (currentRun == null) {
            logger.debug { "🔍 No current run - checking auto-start conditions" }
            val shouldAutoStart = checkAutoStartCondition(previousGameState, gameState)
            if (shouldAutoStart) {
                logger.info { "🚀 Auto-starting new game run!" }
                logger.info { "STARTING NEW RUN - Auto-start triggered!" }
                startNewRun(currentProfile?.id ?: SplitProfiles.DEFAULT.id)
            }
            previousGameState = gameState
            return
        }

        // Auto-reset paused runs when starting a new game (check BEFORE gameplay state validation)
        if (currentRun.isPaused) {
            val shouldAutoStart = checkAutoStartCondition(previousGameState, gameState)
            if (shouldAutoStart) {
                logger.info { "🔄 Auto-resetting paused run to start new game!" }
                logger.info { "AUTO-RESET: Clearing paused run to start new game!" }
                resetRun() // Reset the current run
                startNewRun(currentProfile?.id ?: SplitProfiles.DEFAULT.id) // Start fresh run with current profile
                previousGameState = gameState
                return
            }
        }

        // CRITICAL: Prevent false splits during intro/cutscenes
        // Now that auto-start has been checked, filter out invalid states for split processing
        // NOTE: We do NOT update previousGameState here - we want to preserve the last VALID state
        // for proper transition detection (e.g., state 8 → 6 (invalid) → 32 should still detect 8→32)
        if (!isValidGameplayState(gameState)) {
            logger.debug { "🚫 Ignoring game state - not in valid gameplay (state: ${gameState.gameState})" }
            return
        }

        logger.debug { "▶️ Run in progress: ${currentRun.id}, paused: ${currentRun.isPaused}" }
        logger.debug { "Run already exists: ${currentRun.id}, paused: ${currentRun.isPaused}" }

        // Don't process splits if paused
        if (currentRun.isPaused) {
            previousGameState = gameState
            return
        }

        val profile = currentProfile ?: SplitProfiles.DEFAULT

        // Auto-skip completed splits for mid-run starts OR if current split is already completed
        if ((currentRun.completedSplits.isEmpty() && currentSplitIndex == 0) ||
            (currentSplitIndex < profile.splits.size && isConditionAlreadyMet(profile.splits[currentSplitIndex], gameState))) {
            autoSkipCompletedSplits(gameState, profile)
        }

        // Check if we should trigger a split
        if (currentSplitIndex < profile.splits.size) {
            val split = profile.splits[currentSplitIndex]
            val shouldSplit = checkSplitCondition(split, previousGameState, gameState)

            if (shouldSplit) {
                triggerSplit(split, gameState)
            }
        }

        previousGameState = gameState
    }

    /**
     * Auto-skip splits that are already completed based on current game state
     */
    private fun autoSkipCompletedSplits(gameState: GameState, profile: SplitProfile) {
        var currentRun = _splitsState.value.currentRun ?: return
        var skipped = false
        val currentTime = Clock.System.now()
        val estimatedTime = (currentTime - currentRun.startTime).inWholeMilliseconds - currentRun.pausedTime

        while (currentSplitIndex < profile.splits.size) {
            val split = profile.splits[currentSplitIndex]
            val isAlreadyCompleted = isConditionAlreadyMet(split, gameState)

            if (isAlreadyCompleted) {
                logger.info { "⏭️ Auto-skipping completed split: ${split.name}" }

                // Add the skipped split to completedSplits with estimated time
                // Calculate proper segment time for auto-skipped splits
                val segmentTimeMs = if (currentRun.completedSplits.isEmpty()) {
                    estimatedTime
                } else {
                    estimatedTime - currentRun.completedSplits.last().time.totalTime
                }

                val completedSplit = CompletedSplit(
                    splitId = split.id,
                    time = SplitTime(
                        totalTime = estimatedTime,
                        segmentTime = segmentTimeMs
                    ),
                    timestamp = currentTime
                )

                currentRun = currentRun.copy(
                    completedSplits = currentRun.completedSplits + completedSplit,
                    totalTime = estimatedTime
                )

                currentSplitIndex++
                skipped = true

                // Check if this is the final split (run complete)
                val isComplete = currentSplitIndex >= profile.splits.size || split.id == "ship"
                
                val finalRun = if (isComplete) {
                    logger.info { "🏁 Run complete via auto-skip! Final time: ${formatTime(estimatedTime)}" }
                    
                    // Check if this is a personal best
                    val currentBest = _splitsState.value.personalBests[currentRun.profileId]
                    val isNewPersonalBest = currentBest == null || estimatedTime < currentBest.totalTime
                    
                    if (isNewPersonalBest) {
                        logger.info { "🎉 NEW PERSONAL BEST (via auto-skip)! ${formatTime(estimatedTime)}" }
                    }
                    
                    currentRun.copy(
                        endTime = currentTime,
                        isPaused = true,
                        isPersonalBest = isNewPersonalBest
                    )
                } else {
                    currentRun
                }

                // Update state
                val currentState = _splitsState.value
                val newRunHistory = if (isComplete) {
                    currentState.runHistory + finalRun
                } else {
                    currentState.runHistory
                }

                // When run completes via auto-skip, freeze the display using pre-run personalBests
                // This prevents BP Δ from immediately showing ±0:00 for all segments
                // The run is saved to disk and will be included in BP calculation on next run start
                val finalPersonalBests = if (isComplete) {
                    preRunPersonalBests  // Use the personalBests from when the run STARTED
                } else {
                    currentState.personalBests
                }

                _splitsState.value = currentState.copy(
                    currentRun = finalRun,  // Keep the run visible so user can see final times (timer still stops due to isPaused = true)
                    runHistory = newRunHistory,
                    personalBests = finalPersonalBests
                )

                // Save the auto-skipped split to disk
                fileStorageService?.let { storage ->
                    scope.launch(Dispatchers.IO) {
                        try {
                            logger.info { "💾 Saving run progress after auto-skip: ${split.name} (${finalRun.completedSplits.size} splits)" }
                            storage.saveRun(finalRun)
                            onRunSaved?.invoke(finalRun)
                            logger.info { "✅ Successfully saved auto-skipped run progress for split: ${split.name}" }

                            // Update run summaries
                            try {
                                val summaries = storage.loadRunSummaries()
                                val updatedProfile = storage.deriveBestSplits(finalRun.profileId)
                                val updatedSummaries = summaries.copy(
                                    lastUpdated = Clock.System.now(),
                                    profiles = summaries.profiles + (finalRun.profileId to updatedProfile)
                                )
                                storage.saveRunSummaries(updatedSummaries)
                                logger.info { "✅ Updated run summaries after auto-skip" }
                            } catch (e: Exception) {
                                logger.error(e) { "⚠️  Failed to update run summaries after auto-skip, but run data was saved" }
                            }
                        } catch (e: Exception) {
                            logger.error(e) { "❌ CRITICAL: Failed to save run progress after auto-skip ${split.name}" }
                        }
                    }
                }

                // Run complete via auto-skip
                if (isComplete) {
                    logger.info { "🏁 Run completed via auto-skip in ${formatTime(estimatedTime)}" }
                    // DON'T update personalBests here - keep the display frozen
                    // The run is already saved to disk and will be included in BP calculation on next run start
                }
            } else {
                break
            }
        }

        if (skipped) {
            logger.info { "📍 Positioned at split ${currentSplitIndex}/${profile.splits.size}" }
        }
    }

    /**
     * Check if all required items for a split are acquired
     */
    private fun hasRequiredItems(split: Split, gameState: GameState): Boolean {
        val required = split.requiredItems ?: return true
        return required.all { itemId ->
            when (itemId) {
                "morph_ball" -> gameState.items.morph
                "bombs" -> gameState.items.bombs
                "varia_suit" -> gameState.items.varia
                "gravity_suit" -> gameState.items.gravity
                "hi_jump" -> gameState.items.hiJump
                "speed_booster" -> gameState.items.speed
                "space_jump" -> gameState.items.spaceJump
                "screw_attack" -> gameState.items.screw
                "spring_ball" -> gameState.items.spring
                "grapple_beam" -> gameState.items.grapple
                "xray" -> gameState.items.xray
                "charge_beam" -> gameState.beams.charge
                "ice_beam" -> gameState.beams.ice
                "wave_beam" -> gameState.beams.wave
                "spazer" -> gameState.beams.spazer
                "plasma_beam" -> gameState.beams.plasma
                "missiles" -> gameState.maxMissiles > 0
                "super_missiles" -> gameState.maxSupers > 0
                "power_bombs" -> gameState.maxPowerBombs > 0
                else -> {
                    logger.warn { "Unknown required item: $itemId" }
                    false
                }
            }
        }
    }

    /**
     * Check if a split condition is already met in current game state
     */
    internal fun isConditionAlreadyMet(split: Split, gameState: GameState): Boolean {
        // Room-entry splits: if the player is already in the trigger room, consider it met
        // EXCEPT: splits with requiredItems must use transition-based detection (leave + re-enter)
        // to avoid false triggers when picking up the required item while already in the room
        if (split.triggerRoomId != null) {
            if (split.requiredItems != null) return false // Force transition-based check
            return gameState.roomId == split.triggerRoomId
        }

        return when (split.id) {
            "ceres_station" -> gameState.areaId != 6 && gameState.bosses.ceresStation // Not in Ceres AND defeated Ceres Ridley
            "first_missile" -> gameState.maxMissiles > 0
            "first_super" -> gameState.maxSupers > 0
            "first_power_bomb" -> gameState.maxPowerBombs > 0
            "morph_ball" -> gameState.items.morph
            "bomb" -> gameState.items.bombs
            "charge_beam" -> gameState.beams.charge
            "spazer" -> gameState.beams.spazer
            "varia_suit" -> gameState.items.varia
            "hi_jump" -> gameState.items.hiJump
            "speed_booster" -> gameState.items.speed
            "wave_beam" -> gameState.beams.wave
            "ice_beam" -> gameState.beams.ice
            "gravity_suit" -> gameState.items.gravity
            "space_jump" -> gameState.items.spaceJump
            "plasma_beam" -> gameState.beams.plasma
            "screw_attack" -> gameState.items.screw
            "grapple_beam" -> gameState.items.grapple
            "spring_ball" -> gameState.items.spring
            "reserve_tank" -> gameState.maxReserveEnergy > 0
            "kraid" -> gameState.bosses.kraid
            "phantoon" -> gameState.bosses.phantoon
            "draygon" -> gameState.bosses.draygon
            "ridley" -> gameState.bosses.ridley
            // For G4, we need to check if we're in the statues room, not just if all bosses are defeated
            // This prevents auto-triggering G4 immediately after defeating the last boss
            "golden_four" -> gameState.roomId == RoomIds.STATUES && gameState.bosses.kraid && gameState.bosses.phantoon && gameState.bosses.draygon && gameState.bosses.ridley
            "mother_brain_1" -> {
                // Uses shared constants from GameStateConstants to stay in sync with GameStateParser
                val inMbRoom = gameState.roomId == RoomIds.MOTHER_BRAIN_ROOM
                val normalGameplay = gameState.gameState == GameStateConstants.NORMAL_GAMEPLAY
                val mb1AlreadyDefeated = inMbRoom && normalGameplay && gameState.motherBrainHp >= GameStateConstants.MB1_HP_THRESHOLD
                val zebesEscaping = (gameState.eventFlags and GameStateConstants.ZEBES_ABLAZE_FLAG) != 0
                val mbFinalDefeated = (gameState.tourianBosses and GameStateConstants.MB_FINAL_DEFEATED_FLAG) != 0

                gameState.bosses.motherBrain1 || mb1AlreadyDefeated || zebesEscaping || mbFinalDefeated
            }
            "mother_brain_2" -> {
                // Uses shared constants from GameStateConstants to stay in sync with GameStateParser
                val inMbRoom = gameState.roomId == RoomIds.MOTHER_BRAIN_ROOM
                val normalGameplay = gameState.gameState == GameStateConstants.NORMAL_GAMEPLAY
                val mb2AlreadyDefeated = inMbRoom && normalGameplay && gameState.motherBrainHp >= GameStateConstants.MB2_HP_THRESHOLD
                val zebesEscaping = (gameState.eventFlags and GameStateConstants.ZEBES_ABLAZE_FLAG) != 0
                val mbFinalDefeated = (gameState.tourianBosses and GameStateConstants.MB_FINAL_DEFEATED_FLAG) != 0

                gameState.bosses.motherBrain2 || mb2AlreadyDefeated || zebesEscaping || mbFinalDefeated
            }
            "ship" -> gameState.bosses.samusShip
            else -> false
        }
    }

    /**
     * Check if the game state is valid for processing splits (not intro/cutscenes)
     * Based on SuperMetroid.asl gameStateEnum - only allow normal gameplay and door transitions
     */
    private fun isValidGameplayState(gameState: GameState): Boolean {
        return when (gameState.gameState) {
            GameStateConstants.NORMAL_GAMEPLAY -> true         // 8 - Normal gameplay
            GameStateConstants.DOOR_TRANSITION -> true        // 11 - Door transitions
            GameStateConstants.ELEVATOR -> true               // 5 - Elevator transitions
            GameStateConstants.START_OF_CERES_CUTSCENE -> true // 32 - Ceres escape cutscene start
            34 -> true                                         // 34 - Ceres escape cutscene continuation
            else -> {
                logger.debug { "🚫 Invalid game state: ${gameState.gameState} (not normal gameplay/door transition)" }
                false
            }
        }
    }

    /**
     * Check for auto-start conditions for new game
     * Matches ASL auto-start behavior: starts when pressing Start on title screen
     * ASL logic: gameState 2 → 31 (title screen → game start transition)
     */
    private fun checkAutoStartCondition(previousState: GameState?, currentState: GameState): Boolean {
        // Don't auto-start if:
        // 1. Auto-start is explicitly disabled
        // 2. There's a paused run with non-zero time (user manually set the timer)
        if (!autoStartEnabled) {
            logger.debug { "⏸️ Auto-start disabled" }
            return false
        }
        
        val currentRun = _splitsState.value.currentRun
        if (currentRun != null && currentRun.isPaused && currentRun.totalTime > 0) {
            logger.debug { "⏸️ Auto-start skipped - manual timer set (time: ${formatTime(currentRun.totalTime)})" }
            return false
        }

        // PRIMARY: ASL-style start - Pressing start on title screen (gameState 2 → 31)
        // This matches SuperMetroid.asl line 778: vars.watchers["gameState"].Old == 2 && vars.watchers["gameState"].Current == 0x1F
        val titleScreenStart = previousState?.gameState == GameStateConstants.TITLE_SCREEN && 
                              currentState.gameState == GameStateConstants.GAME_START_TRANSITION
        
        // SECONDARY: Zebes start for categories that skip Ceres (e.g., Spore Spawn RTA)
        // This matches SuperMetroid.asl line 782: vars.watchers["gameState"].Old == 5 && vars.watchers["gameState"].Current == 6
        // Guard: only trigger if game looks like early-game (no items collected beyond starting gear).
        // Loading a mid-game save also transitions 5→6 but would have items/health above starting values.
        val zebesTransition = previousState?.gameState == GameStateConstants.ELEVATOR &&
                        currentState.gameState == GameStateConstants.ZEBES_TRANSITION_END
        val isEarlyGame = currentState.maxHealth <= 99 && currentState.maxMissiles <= 5
        val zebesStart = zebesTransition && isEarlyGame
        
        // FALLBACK: Original Ceres start (backup for mid-run detection)
        // Keep this as a backup in case title screen transition is missed
        val inCeres = currentState.areaId == 6
        val normalGameplay = currentState.gameState == GameStateConstants.NORMAL_GAMEPLAY
        val earlyGame = currentState.maxHealth <= 99 && currentState.maxMissiles <= 5
        val ceresStart = inCeres && normalGameplay && earlyGame

        val shouldAutoStart = titleScreenStart || zebesStart || ceresStart

        // Log for debugging
        if (previousState != null) {
            logger.debug { "🔎 Auto-start check: titleScreen=$titleScreenStart (${previousState.gameState}→${currentState.gameState}), zebes=$zebesStart, ceres=$ceresStart" }
        }

        if (shouldAutoStart) {
            val method = when {
                titleScreenStart -> "TITLE_SCREEN_TRANSITION (ASL normalStart)"
                zebesStart -> "ZEBES_START (ASL zebesStart)"
                else -> "CERES_CONTROL (Fallback)"
            }
            logger.info { "🎯 Auto-start triggered via $method" }
            logger.info { "AUTO-START: gameState ${previousState?.gameState}→${currentState.gameState}" }
        }

        return shouldAutoStart
    }

    /**
     * Check if a split condition is met
     */
    private fun checkSplitCondition(split: Split, previousState: GameState?, currentState: GameState): Boolean {
        if (previousState == null) return false

        // Room-entry trigger: split fires when player transitions into the specified room
        if (split.triggerRoomId != null) {
            val entered = previousState.roomId != split.triggerRoomId && currentState.roomId == split.triggerRoomId

            // Log every check for requiredItems splits to help debug
            if (split.requiredItems != null && currentState.roomId == split.triggerRoomId) {
                val hasItems = hasRequiredItems(split, currentState)
                logger.debug { "🔍 ${split.name}: inRoom=true, entered=$entered, hasItems=$hasItems, prevRoom=0x${previousState.roomId.toString(16)}, curRoom=0x${currentState.roomId.toString(16)}" }
            }

            if (entered && !hasRequiredItems(split, currentState)) {
                logger.info { "🚪 Room entry for ${split.name} but missing required items: ${split.requiredItems}" }
                return false
            }
            if (entered) {
                logger.info { "🚪 Room entry split: ${split.name} (room 0x${split.triggerRoomId.toString(16)})" }
            }
            return entered
        }

        return when (split.id) {
            "ceres_station" -> checkCeresStation(previousState, currentState)
            "first_missile" -> checkFirstMissile(previousState, currentState)
            "first_super" -> checkFirstSuper(previousState, currentState)
            "first_power_bomb" -> checkFirstPowerBomb(previousState, currentState)
            "morph_ball" -> checkMorphBall(previousState, currentState)
            "bomb" -> checkBomb(previousState, currentState)
            "charge_beam" -> checkChargeBeam(previousState, currentState)
            "spazer" -> checkSpazer(previousState, currentState)
            "varia_suit" -> checkVariaSuit(previousState, currentState)
            "hi_jump" -> checkHiJump(previousState, currentState)
            "speed_booster" -> checkSpeedBooster(previousState, currentState)
            "wave_beam" -> checkWaveBeam(previousState, currentState)
            "ice_beam" -> checkIceBeam(previousState, currentState)
            "gravity_suit" -> checkGravitySuit(previousState, currentState)
            "space_jump" -> checkSpaceJump(previousState, currentState)
            "plasma_beam" -> checkPlasmaBeam(previousState, currentState)
            "screw_attack" -> !previousState.items.screw && currentState.items.screw
            "grapple_beam" -> !previousState.items.grapple && currentState.items.grapple
            "spring_ball" -> !previousState.items.spring && currentState.items.spring
            "reserve_tank" -> checkReserveTank(previousState, currentState)
            "kraid" -> checkKraid(previousState, currentState)
            "phantoon" -> checkPhantoon(previousState, currentState)
            "draygon" -> checkDraygon(previousState, currentState)
            "ridley" -> checkRidley(previousState, currentState)
            "golden_four" -> checkGoldenFour(previousState, currentState)
            "mother_brain_1" -> checkMotherBrain1(previousState, currentState)
            "mother_brain_2" -> checkMotherBrain2(previousState, currentState)
            "ship" -> checkShip(previousState, currentState)
            else -> false
        }
    }

    /**
     * Trigger a split
     */
    private fun triggerSplit(split: Split, gameState: GameState) {
        val currentRun = _splitsState.value.currentRun ?: return
        val currentTime = Clock.System.now()
        val rawTimeMs = (currentTime - currentRun.startTime).inWholeMilliseconds
        val totalTimeMs = rawTimeMs - currentRun.pausedTime

        // Calculate segment time
        val segmentTimeMs = if (currentRun.completedSplits.isEmpty()) {
            totalTimeMs
        } else {
            totalTimeMs - currentRun.completedSplits.last().time.totalTime
        }

        // Check for personal best delta
        val currentState = _splitsState.value
        val personalBest = currentState.personalBests[currentRun.profileId]
        val delta = personalBest?.splitTimes?.get(split.id)?.let { pbTime ->
            segmentTimeMs - pbTime.segmentTime
        }

        val splitTime = SplitTime(
            totalTime = totalTimeMs,
            segmentTime = segmentTimeMs,
            delta = delta,
            originalDelta = delta // Preserve the original delta
        )

        val completedSplit = CompletedSplit(
            splitId = split.id,
            time = splitTime,
            timestamp = currentTime
        )

        // Always update personal best for this individual split, preserving delta information
        val updatedPersonalBests = if (personalBest != null) {
            val currentPBSplit = personalBest.splitTimes[split.id]

            // Check if this is a new personal best for this segment
            val isNewPB = currentPBSplit == null || segmentTimeMs < currentPBSplit.segmentTime

            // Create a new SplitTime with the appropriate delta
            val updatedSplitTime = if (isNewPB) {
                // If this is a new PB, create a new SplitTime with delta=0 but preserve the original delta
                logger.info { "🎉 New split PB for ${split.name}! ${formatTime(segmentTimeMs)} (was ${currentPBSplit?.let { formatTime(it.segmentTime) } ?: "N/A"})" }

                SplitTime(
                    totalTime = splitTime.totalTime,
                    segmentTime = splitTime.segmentTime,
                    delta = 0, // Delta is 0 for a PB
                    originalDelta = splitTime.delta // Preserve the original delta
                )
            } else {
                // Not a new PB, just use the current split time
                splitTime
            }

            // Only update the split time in the personal best if it's actually a new PB
            val updatedSplitTimes = if (isNewPB) {
                personalBest.splitTimes + (split.id to updatedSplitTime)
            } else {
                personalBest.splitTimes // Don't overwrite existing PB with slower time
            }
            val updatedPB = personalBest.copy(splitTimes = updatedSplitTimes)

            currentState.personalBests + (currentRun.profileId to updatedPB)
        } else {
            // First time - create new personal best with just this split
            val newSplitTimes = mapOf(split.id to splitTime)
            val newPB = PersonalBest(
                profileId = currentRun.profileId,
                runSessionId = currentRun.id,
                totalTime = totalTimeMs,
                splitTimes = newSplitTimes
            )

            logger.info { "🎉 First PB for ${split.name}! ${formatTime(segmentTimeMs)}" }
            currentState.personalBests + (currentRun.profileId to newPB)
        }

        val updatedRun = currentRun.copy(
            completedSplits = currentRun.completedSplits + completedSplit,
            totalTime = totalTimeMs,
            isPaused = currentRun.isPaused,
            pausedTime = currentRun.pausedTime
        )

        currentSplitIndex++

        // Check if run is complete
        val isComplete = currentSplitIndex >= (currentProfile?.splits?.size ?: 0)

        // For ship split, pause the timer and mark run as complete
        val finalRun = if (isComplete || split.id == "ship") {
            logger.info { "🏁 Run complete! Final time: ${formatTime(totalTimeMs)}" }

            // Check if this is a personal best
            val currentBest = _splitsState.value.personalBests[currentRun.profileId]
            val isNewPersonalBest = currentBest == null || totalTimeMs < currentBest.totalTime

            if (isNewPersonalBest) {
                logger.info { "🎉 NEW PERSONAL BEST! ${formatTime(totalTimeMs)}" }
            }

            updatedRun.copy(
                endTime = currentTime,
                isPaused = true,  // Pause the timer when run ends
                isPersonalBest = isNewPersonalBest
            )
        } else {
            updatedRun
        }

        // Update state
        val newRunHistory = if (isComplete || split.id == "ship") {
            currentState.runHistory + finalRun
        } else {
            currentState.runHistory
        }

        // Update overall personal bests if this run set a new record
        // NOTE: We update personalBests ONLY if not complete yet (during the run)
        // When the run completes, we DON'T update personalBests so the display stays frozen
        // showing BP Δ against the pre-run Best Possible. This lets users review their
        // performance before starting a new run. Best Possible will be recalculated on next run start.
        val finalPersonalBests = if (isComplete || split.id == "ship") {
            // Run complete - freeze the display, use the personalBests from RUN START
            // This prevents BP Δ from immediately showing ±0:00 after completing the run
            preRunPersonalBests
        } else {
            // Run still in progress - update personalBests for real-time BP Δ
            updatedPersonalBests
        }

        _splitsState.value = currentState.copy(
            currentRun = finalRun,  // Keep the run visible so user can see final times (timer still stops due to isPaused = true)
            runHistory = newRunHistory,
            personalBests = finalPersonalBests
        )

        logger.info { "⏰ Split triggered: ${split.name} at ${formatTime(totalTimeMs)} (segment: ${formatTime(segmentTimeMs)})" }

        // Save run incrementally after each split to prevent data loss
        // Launch async save but don't block the split detection thread
        fileStorageService?.let { storage ->
            scope.launch(Dispatchers.IO) {
                try {
                    logger.info { "💾 Saving run progress for split: ${split.name} (${finalRun.completedSplits.size} splits)" }
                    storage.saveRun(finalRun)
                    onRunSaved?.invoke(finalRun)
                    logger.info { "✅ Successfully saved run progress to disk for split: ${split.name}" }

                    // Update run summaries to track segment PBs from this run so far
                    try {
                    val summaries = storage.loadRunSummaries()
                    val updatedProfile = storage.deriveBestSplits(finalRun.profileId)
                    val updatedSummaries = summaries.copy(
                        lastUpdated = Clock.System.now(),
                        profiles = summaries.profiles + (finalRun.profileId to updatedProfile)
                    )
                    storage.saveRunSummaries(updatedSummaries)
                        logger.info { "✅ Updated run summaries with current segments" }
                } catch (e: Exception) {
                        logger.error(e) { "⚠️  Failed to update run summaries, but run data was saved" }
                    }
                } catch (e: Exception) {
                    logger.error(e) { "❌ CRITICAL: Failed to save run progress after split ${split.name}" }
                    // Try to save with error recovery
                    try {
                        storage.saveRun(finalRun)
                        logger.info { "✅ Retry succeeded - run saved for split: ${split.name}" }
                    } catch (e2: Exception) {
                        logger.error(e2) { "❌ FATAL: Could not save run even after retry for split: ${split.name}" }
                    }
                }
            }
        }

        if (isComplete || split.id == "ship") {
            logger.info { "🏁 Run completed in ${formatTime(totalTimeMs)}" }
            // DON'T update personalBests here - keep the display frozen showing BP Δ against pre-run Best Possible
            // The run is already saved to disk (line ~1189) and will be included in BP calculation on next run start
            // This lets users review their performance before starting a new run
        }
    }

    /**
     * Update personal best if this run was better
     */
    private fun updatePersonalBest(completedRun: RunSession) {
        val currentPB = _splitsState.value.personalBests[completedRun.profileId]

        if (currentPB == null || completedRun.totalTime < currentPB.totalTime) {
            val splitTimes = completedRun.completedSplits.associate { split ->
                split.splitId to split.time
            }

            val newPB = PersonalBest(
                profileId = completedRun.profileId,
                runSessionId = completedRun.id,
                totalTime = completedRun.totalTime,
                splitTimes = splitTimes
            )

            val currentState = _splitsState.value
            _splitsState.value = currentState.copy(
                personalBests = currentState.personalBests + (completedRun.profileId to newPB)
            )

            logger.info { "🏆 New personal best! ${formatTime(completedRun.totalTime)}" }
        }
    }

    /**
     * Get current active split
     */
    fun getCurrentSplit(): Split? {
        val profile = currentProfile ?: return null
        return if (currentSplitIndex < profile.splits.size) {
            profile.splits[currentSplitIndex]
        } else null
    }

    /**
     * Discard the current run without saving. Resets timer and state.
     */
    fun discardRun() {
        val currentRun = _splitsState.value.currentRun
        if (currentRun == null) {
            logger.info { "🗑️ discardRun called but no active run" }
            return
        }

        logger.info { "🗑️ Discarding run ${currentRun.id} (${currentRun.completedSplits.size} splits, ${formatTime(currentRun.totalTime)})" }

        currentSplitIndex = 0
        pauseStartTime = null

        val currentState = _splitsState.value
        val updatedState = updatePersonalBestsFromRunHistory(currentState)
        _splitsState.value = updatedState.copy(currentRun = null)

        lastToggleTime = 0
        autoStartEnabled = true

        clearSavedTimer()

        logger.info { "🗑️ Run discarded - no data saved" }
    }

    /**
     * Resume an incomplete run. Loads the run as current, adjusts paused time
     * to account for the gap, and positions at the next uncompleted split.
     */
    suspend fun resumeRun(runFileName: String) {
        logger.info { "▶️ Resuming run: $runFileName" }

        try {
            fileStorageService?.let { storage ->
                val targetRun = storage.loadRunByFileName(runFileName)
                if (targetRun == null) {
                    logger.error { "❌ Could not find run file: $runFileName" }
                    return
                }

                if (targetRun.endTime != null) {
                    logger.warn { "⚠️ Cannot resume a completed run" }
                    return
                }

                // Calculate the gap between when the run was paused and now
                val now = kotlinx.datetime.Clock.System.now()
                val gapMs = now.toEpochMilliseconds() - targetRun.startTime.toEpochMilliseconds() -
                    targetRun.totalTime - targetRun.pausedTime

                // Resume the run: add the gap to pausedTime so the timer picks up from totalTime
                val resumedRun = targetRun.copy(
                    isPaused = false,
                    pausedTime = targetRun.pausedTime + gapMs
                )

                // Set split index past completed splits
                val profile = SplitProfiles.getProfileById(targetRun.profileId)
                currentProfile = profile
                currentSplitIndex = targetRun.completedSplits.size

                // Load all runs for PB calculation
                val allRuns = storage.loadAllRuns()
                val previousRuns = allRuns.filter { it.startTime < targetRun.startTime && it.endTime != null }

                val updatedState = SplitsState(
                    currentRun = resumedRun,
                    personalBests = emptyMap(),
                    runHistory = previousRuns
                )

                val finalState = updatePersonalBestsFromRunHistory(updatedState)
                _splitsState.value = finalState

                onProfileChangedFromEngine?.invoke(profile)
                autoStartEnabled = false

                logger.info { "▶️ Resumed run ${targetRun.id}: ${targetRun.completedSplits.size} splits done, timer at ${formatTime(targetRun.totalTime)}" }

                // Delete the old run file since we're continuing it
                storage.deleteRun(runFileName)
                logger.info { "🗑️ Removed old run file (run is now active): $runFileName" }
            }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to resume run" }
        }
    }

    /**
     * Manually trigger the current split at the current timer time.
     * Used when auto-detection misses a split (e.g., ROM hacks, custom conditions).
     * Returns true if a split was triggered, false if no active split exists.
     */
    fun manualSplit(): Boolean {
        val currentRun = _splitsState.value.currentRun ?: return false
        if (currentRun.isPaused && currentRun.endTime != null) return false // Run already finished

        val profile = currentProfile ?: return false
        if (currentSplitIndex >= profile.splits.size) return false

        val split = profile.splits[currentSplitIndex]
        logger.info { "✋ Manual split triggered: ${split.name}" }

        // Use a minimal dummy GameState - triggerSplit only needs the current run state
        triggerSplit(split, GameState())
        return true
    }

    /**
     * Undo the last completed split - removes the most recent split and moves back.
     * This is destructive: the split time data is lost.
     * Returns true if a split was undone.
     */
    fun undoSplit(): Boolean {
        val currentRun = _splitsState.value.currentRun ?: return false
        if (currentRun.completedSplits.isEmpty()) return false

        val lastSplit = currentRun.completedSplits.last()
        logger.info { "⏪ Undo split: removing ${lastSplit.splitId} (was split ${currentSplitIndex})" }

        val updatedSplits = currentRun.completedSplits.dropLast(1)
        currentSplitIndex = (currentSplitIndex - 1).coerceAtLeast(0)

        val updatedRun = currentRun.copy(completedSplits = updatedSplits)
        _splitsState.value = _splitsState.value.copy(currentRun = updatedRun)

        logger.info { "⏪ Split undone. Now at split ${currentSplitIndex}/${currentProfile?.splits?.size ?: 0}" }
        return true
    }

    // Split condition checks - exact logic from TypeScript version

    private fun checkCeresStation(prev: GameState, curr: GameState): Boolean {
        // ASL LOGIC from supermetroid.asl line 952:
        // ceresEscape = roomID.Current == ceresElevator && gameState.Old == normalGameplay && gameState.Current == startOfCeresCutscene
        // 
        // Our polling (~1.6s) may miss the exact frame, so we're slightly more lenient on prevState.
        // The ASL runs frame-by-frame, we poll. The cutscene state (32) is reliable, the prev state may vary.
        val inCeresElevator = curr.roomId == RoomIds.CERES_ELEVATOR
        val cutsceneStarted = curr.gameState == GameStateConstants.START_OF_CERES_CUTSCENE
        val prevWasGameplay = prev.gameState == GameStateConstants.NORMAL_GAMEPLAY ||
                              prev.gameState == GameStateConstants.DOOR_TRANSITION ||
                              prev.gameState == GameStateConstants.ELEVATOR
        
        val result = inCeresElevator && cutsceneStarted && prevWasGameplay
        if (result) {
            logger.info { "🚀 CERES ESCAPE DETECTED! gameState=${prev.gameState}->${curr.gameState}" }
        }
        return result
    }

    private fun checkFirstMissile(prev: GameState, curr: GameState): Boolean =
        prev.maxMissiles == 0 && curr.maxMissiles > 0

    private fun checkFirstSuper(prev: GameState, curr: GameState): Boolean =
        prev.maxSupers == 0 && curr.maxSupers > 0

    private fun checkFirstPowerBomb(prev: GameState, curr: GameState): Boolean =
        prev.maxPowerBombs == 0 && curr.maxPowerBombs > 0

    private fun checkMorphBall(prev: GameState, curr: GameState): Boolean =
        !prev.items.morph && curr.items.morph

    private fun checkBomb(prev: GameState, curr: GameState): Boolean =
        !prev.items.bombs && curr.items.bombs

    private fun checkChargeBeam(prev: GameState, curr: GameState): Boolean =
        !prev.beams.charge && curr.beams.charge

    private fun checkSpazer(prev: GameState, curr: GameState): Boolean =
        !prev.beams.spazer && curr.beams.spazer

    private fun checkVariaSuit(prev: GameState, curr: GameState): Boolean =
        !prev.items.varia && curr.items.varia

    private fun checkHiJump(prev: GameState, curr: GameState): Boolean =
        !prev.items.hiJump && curr.items.hiJump

    private fun checkSpeedBooster(prev: GameState, curr: GameState): Boolean =
        !prev.items.speed && curr.items.speed

    private fun checkWaveBeam(prev: GameState, curr: GameState): Boolean =
        !prev.beams.wave && curr.beams.wave

    private fun checkIceBeam(prev: GameState, curr: GameState): Boolean =
        !prev.beams.ice && curr.beams.ice

    private fun checkGravitySuit(prev: GameState, curr: GameState): Boolean =
        !prev.items.gravity && curr.items.gravity

    private fun checkSpaceJump(prev: GameState, curr: GameState): Boolean =
        !prev.items.spaceJump && curr.items.spaceJump

    private fun checkPlasmaBeam(prev: GameState, curr: GameState): Boolean =
        !prev.beams.plasma && curr.beams.plasma

    private fun checkReserveTank(prev: GameState, curr: GameState): Boolean =
        prev.maxReserveEnergy == 0 && curr.maxReserveEnergy > 0

    private fun checkKraid(prev: GameState, curr: GameState): Boolean =
        !prev.bosses.kraid && curr.bosses.kraid

    private fun checkPhantoon(prev: GameState, curr: GameState): Boolean =
        !prev.bosses.phantoon && curr.bosses.phantoon

    private fun checkDraygon(prev: GameState, curr: GameState): Boolean =
        !prev.bosses.draygon && curr.bosses.draygon

    private fun checkRidley(prev: GameState, curr: GameState): Boolean =
        !prev.bosses.ridley && curr.bosses.ridley

    /**
     * Golden Four - EXACT ASL LOGIC
     * Triggers when entering statues room with all 4 bosses defeated
     */
    private fun checkGoldenFour(prev: GameState, curr: GameState): Boolean {
        // Room transition: statues hallway -> statues room
        val roomTransition = prev.roomId == RoomIds.STATUES_HALLWAY && curr.roomId == RoomIds.STATUES

        // All four major bosses defeated
        val allBossesDefeated = curr.bosses.kraid && curr.bosses.phantoon && curr.bosses.draygon && curr.bosses.ridley

        val result = roomTransition && allBossesDefeated

        logger.info { "🏆 Golden Four CHECK: prevRoom=0x${prev.roomId.toString(16)}, currRoom=0x${curr.roomId.toString(16)}, transition=$roomTransition, bosses=(K:${curr.bosses.kraid}, P:${curr.bosses.phantoon}, D:${curr.bosses.draygon}, R:${curr.bosses.ridley}), allDefeated=$allBossesDefeated, result=$result" }

        return result
    }

    /**
     * Mother Brain 1 - EXACT ASL LOGIC
     * MB1 = inMotherBrainRoom && gameState == normalGameplay && motherBrainHP.Old == 0 && motherBrainHP.Current == 18000
     */
    private fun checkMotherBrain1(prev: GameState, curr: GameState): Boolean {
        val inMbRoom = curr.roomId == RoomIds.MOTHER_BRAIN_ROOM
        val normalGameplay = curr.gameState == GameStateConstants.NORMAL_GAMEPLAY

        // EXACT ASL LOGIC: HP transition 0 -> threshold
        val hpTransition = prev.motherBrainHp == 0 && curr.motherBrainHp == GameStateConstants.MB1_HP_THRESHOLD

        // RETROACTIVE LOGIC: If HP >= threshold in MB room, MB1 already defeated
        val mb1AlreadyDefeated = inMbRoom && normalGameplay && curr.motherBrainHp >= GameStateConstants.MB1_HP_THRESHOLD

        // Also check escape scenarios
        val zebesEscaping = (curr.eventFlags and GameStateConstants.ZEBES_ABLAZE_FLAG) != 0
        val mbFinalDefeated = (curr.tourianBosses and GameStateConstants.MB_FINAL_DEFEATED_FLAG) != 0

        val result = hpTransition || mb1AlreadyDefeated || zebesEscaping || mbFinalDefeated

        // Log only on successful detection to reduce noise
        if (result) {
            logger.info { "🧠 MB1 detected: HP(${prev.motherBrainHp}->${curr.motherBrainHp})" }
        }

        return result
    }

    /**
     * Mother Brain 2 - EXACT ASL LOGIC
     * MB2 = inMotherBrainRoom && gameState == normalGameplay && motherBrainHP.Old == 0 && motherBrainHP.Current == 36000
     */
    private fun checkMotherBrain2(prev: GameState, curr: GameState): Boolean {
        val inMbRoom = curr.roomId == RoomIds.MOTHER_BRAIN_ROOM
        val normalGameplay = curr.gameState == GameStateConstants.NORMAL_GAMEPLAY

        // EXACT ASL LOGIC: HP transition 0 -> 36000 (0x8CA0)
        val hpTransition = prev.motherBrainHp == 0 && curr.motherBrainHp == GameStateConstants.MB2_HP_THRESHOLD

        // RETROACTIVE LOGIC: If HP >= 36000 in MB room, MB2 already defeated
        val mb2AlreadyDefeated = inMbRoom && normalGameplay && curr.motherBrainHp >= GameStateConstants.MB2_HP_THRESHOLD

        // Also check escape/final scenarios
        val zebesEscaping = (curr.eventFlags and GameStateConstants.ZEBES_ABLAZE_FLAG) != 0
        val mbFinalDefeated = (curr.tourianBosses and GameStateConstants.MB_FINAL_DEFEATED_FLAG) != 0

        val result = hpTransition || mb2AlreadyDefeated || zebesEscaping || mbFinalDefeated

        // Log only on successful detection to reduce noise
        if (result) {
            logger.info { "🧠 MB2 detected: HP(${prev.motherBrainHp}->${curr.motherBrainHp})" }
        }

        return result
    }

    private fun checkShip(prev: GameState, curr: GameState): Boolean {
        // Implement ASL RTA finish logic exactly:
        // escape = zebesAblaze && shipAI.old != 0xaa4f && shipAI.current == 0xaa4f
        val zebesAblaze = (curr.eventFlags and GameStateConstants.ZEBES_ABLAZE_FLAG) != 0  // bit 6
        val motherBrainDefeated = (curr.tourianBosses and GameStateConstants.MB_FINAL_DEFEATED_FLAG) != 0  // Bit 1 (0x2) for Mother Brain, matching ASL and parseMotherBrainFinal
        val shipAiTransition = prev.shipAi != 0xAA4F && curr.shipAi == 0xAA4F

        val result = zebesAblaze && motherBrainDefeated && shipAiTransition

        // Log only on successful detection to reduce noise
        if (result) {
            logger.info { "🚢 Ship escape detected" }
        }

        return result
    }

    /**
     * Generate unique run ID
     */
    private fun generateRunId(): String =
        "run_${Clock.System.now().toEpochMilliseconds()}"

    /**
     * Clear saved timer from config
     */
    private fun clearSavedTimer() {
        fileStorageService?.let { storage ->
            scope.launch(Dispatchers.IO) {
                try {
                    val config = storage.loadAppConfig()
                    storage.saveAppConfig(config.copy(
                        savedTimerMs = null,
                        savedTimerProfileId = null
                    ))
                    logger.debug { "🗑️ Cleared saved timer from config" }
                } catch (e: Exception) {
                    logger.error(e) { "❌ Failed to clear saved timer from config" }
                }
            }
        }
    }

    /**
     * Format time in HH:MM:SS.ss format
     */
    private fun formatTime(timeMs: Long): String {
        val totalSeconds = timeMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        val centiseconds = (timeMs % 1000) / 10

        return if (hours > 0) {
            "%02d:%02d:%02d.%02d".format(hours, minutes, seconds, centiseconds)
        } else {
            "%02d:%02d.%02d".format(minutes, seconds, centiseconds)
        }
    }
}

/**
 * KPDR Any% split profile - legacy reference for backwards compatibility
 * @deprecated Use SplitProfiles.KPDR_ANY instead
 */
@Deprecated("Use SplitProfiles.KPDR_ANY instead", ReplaceWith("SplitProfiles.KPDR_ANY"))
object KpdrAnyProfile {
    val profile = SplitProfiles.KPDR_ANY
}
