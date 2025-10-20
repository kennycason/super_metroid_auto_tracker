package com.supermetroid.autosplits

import com.supermetroid.gamestate.GameStateConstants
import com.supermetroid.gamestate.RoomIds
import com.supermetroid.model.*
import com.supermetroid.storage.FileStorageService
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

private val logger = KotlinLogging.logger {}

/**
 * AutoSplits engine for detecting split conditions and managing run sessions
 */
class AutoSplitsEngine(private val fileStorageService: FileStorageService? = null) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var previousGameState: GameState? = null
    private var currentProfile: SplitProfile? = null
    private var currentSplitIndex = 0
    private var pauseStartTime: kotlinx.datetime.Instant? = null

    // For debouncing toggleRunState calls
    private var lastToggleTime: Long = 0
    private val debounceTimeMs: Long = 300 // 300ms debounce window

    // State flows for reactive UI
    private val _splitsState = MutableStateFlow(SplitsState())
    val splitsState: StateFlow<SplitsState> = _splitsState.asStateFlow()

    /**
     * Load a split profile
     */
    fun loadProfile(profile: SplitProfile) {
        currentProfile = profile
        logger.info { "📋 Loaded split profile: ${profile.name} with ${profile.splits.size} splits" }
    }

    /**
     * Load saved splits state and resume from current position
     */
    fun loadSavedState(savedState: SplitsState) {
        logger.info { "📄 Loading saved state - PersonalBests: ${savedState.personalBests.size}, RunHistory: ${savedState.runHistory.size}" }
        
        // First, update personal bests from run history
        val updatedState = updatePersonalBestsFromRunHistory(savedState)
        _splitsState.value = updatedState

        val currentRun = updatedState.currentRun
        if (currentRun != null) {
            val profile = currentProfile ?: KpdrAnyProfile.profile
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
        for (run in state.runHistory) {
            val profileId = run.profileId

            // Skip runs with no completed splits
            if (run.completedSplits.isEmpty()) {
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

            // Update personal best with new split times
            if (updatedSplitTimes != currentPB.splitTimes) {
                val updatedPB = currentPB.copy(splitTimes = updatedSplitTimes)
                updatedPersonalBests[profileId] = updatedPB
            }
        }

        // Also check current run if it exists
        val currentRun = state.currentRun
        if (currentRun != null && currentRun.completedSplits.isNotEmpty()) {
            val profileId = currentRun.profileId

            // Get or create personal best for this profile
            val currentPB = updatedPersonalBests[profileId] ?: PersonalBest(
                profileId = profileId,
                runSessionId = currentRun.id,
                totalTime = currentRun.totalTime,
                splitTimes = emptyMap()
            )

            // Create a map of updated split times
            val updatedSplitTimes = currentPB.splitTimes.toMutableMap()

            // Check each split in the current run
            for (split in currentRun.completedSplits) {
                val splitId = split.splitId
                val segmentTime = split.time.segmentTime

                // Get current best for this split
                val currentBestSplit = currentPB.splitTimes[splitId]

                // Update if this segment is faster or there's no existing best
                if (currentBestSplit == null || segmentTime < currentBestSplit.segmentTime) {
                    logger.info { "🎯 Found better segment in current run for $splitId: ${formatTime(segmentTime)} (was ${currentBestSplit?.let { formatTime(it.segmentTime) } ?: "N/A"})" }

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

            // Update personal best with new split times
            if (updatedSplitTimes != currentPB.splitTimes) {
                val updatedPB = currentPB.copy(splitTimes = updatedSplitTimes)
                updatedPersonalBests[profileId] = updatedPB
            }
        }

        // Return updated state
        return state.copy(personalBests = updatedPersonalBests)
    }

    /**
     * Start a new run or toggle pause/resume
     * Includes debounce logic to prevent rapid consecutive calls
     */
    fun toggleRunState(profileId: String = "kpdr-any") {
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

        // Add stack trace to see where this is being called from
        val stackTrace = Thread.currentThread().stackTrace
        val caller = stackTrace.getOrNull(3)?.toString() ?: "unknown"
        logger.info { "⏱️ Called from: $caller" }

        // Log full stack trace for debugging
        val fullStackTrace = stackTrace.joinToString("\n  at ")
        logger.info { "⏱️ FULL CALL STACK:\n  at $fullStackTrace" }

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
     * Start a completely new run
     */
    internal fun startNewRun(profileId: String = "kpdr-any") {
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
        _splitsState.value = currentState.copy(currentRun = newRun)

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

        // Check if pauseStartTime is null, which would indicate an inconsistent state
        val pauseStart = pauseStartTime
        if (pauseStart == null) {
            logger.error { "⚠️ Attempted to resume run ${currentRun.id} but pauseStartTime is null! Using current time instead." }
            // Create a new pauseStartTime to avoid errors, but log the issue
            pauseStartTime = Clock.System.now()
            return
        }

        // Calculate the pause duration and add it to the total paused time
        val now = Clock.System.now()
        val pauseDuration = (now - pauseStart).inWholeMilliseconds
        val newPausedTime = currentRun.pausedTime + pauseDuration

        logger.info { "▶️ Resuming run ${currentRun.id} - pause duration: ${formatTime(pauseDuration)}, total paused time: ${formatTime(newPausedTime)}" }

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

        // Add stack trace to see where this is being called from
        val stackTrace = Thread.currentThread().stackTrace
        val caller = stackTrace.getOrNull(3)?.toString() ?: "unknown"
        logger.info { "🔄 Called from: $caller" }

        // Log full stack trace for debugging
        val fullStackTrace = stackTrace.joinToString("\n  at ")
        logger.info { "🔄 FULL CALL STACK:\n  at $fullStackTrace" }

        // Save the run before resetting (if it has any completed splits)
        // This preserves segment PB data from partial runs
        if (currentRun != null && currentRun.completedSplits.isNotEmpty()) {
            logger.info { "💾 Saving partial run with ${currentRun.completedSplits.size} completed splits before reset" }
            
            // Calculate current time for the saved run
            val finalTime = if (currentRun.isPaused) {
                currentRun.totalTime
            } else {
                System.currentTimeMillis() - currentRun.startTime.toEpochMilliseconds() - currentRun.pausedTime
            }
            
            val runToSave = currentRun.copy(
                endTime = Clock.System.now(),
                totalTime = finalTime
            )
            
            // Save asynchronously to avoid blocking the reset
            fileStorageService?.let { storage ->
                scope.launch {
                    try {
                        storage.saveRun(runToSave)
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
        previousGameState = null
        pauseStartTime = null  // Ensure pauseStartTime is reset

        val currentState = _splitsState.value
        _splitsState.value = currentState.copy(currentRun = null)

        // Reset the debounce timer to prevent issues with immediate start after reset
        lastToggleTime = 0

        logger.info { "🔄 Run reset complete - timer state cleared" }

        // Add a small delay before allowing new toggleRunState calls
        // This helps prevent accidental immediate restart after reset
        Thread.sleep(100)
        logger.info { "🔄 Reset cooldown complete - ready for new run" }
    }

    /**
     * Set the timer to a specific time value
     * If no run exists, creates a paused run with the given time
     * If a run exists, adjusts the start time to reflect the desired time
     * 
     * @param timeMs The desired timer value in milliseconds
     * @param profileId The profile ID for the run (default: "kpdr-any")
     */
    fun setTimer(timeMs: Long, profileId: String = "kpdr-any") {
        val currentRun = _splitsState.value.currentRun
        val now = Clock.System.now()

        logger.info { "⏱️ setTimer called - setting timer to ${formatTime(timeMs)}" }

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
    }

    /**
     * Process game state and check for split conditions
     */
    fun processGameState(gameState: GameState) {
        val currentRun = _splitsState.value.currentRun

        // Always log current area for debugging
        logger.debug { "🎮 Current area: ${gameState.areaName} (ID: ${gameState.areaId}), gameState: ${gameState.gameState}" }

        // CRITICAL: Prevent false splits during intro/cutscenes
        if (!isValidGameplayState(gameState)) {
            logger.debug { "🚫 Ignoring game state - not in valid gameplay (state: ${gameState.gameState})" }
            previousGameState = gameState
            return
        }

        if (currentRun == null) {
            logger.debug { "🔍 No current run - checking auto-start conditions" }
            println("[DEBUG_LOG] No current run - checking auto-start conditions")
            // Check for auto-start conditions (like ASL zebesStart logic)
            val shouldAutoStart = checkAutoStartCondition(previousGameState, gameState)
            if (shouldAutoStart) {
                logger.info { "🚀 Auto-starting new game run in Ceres Station!" }
                println("[DEBUG_LOG] STARTING NEW RUN - Auto-start triggered!")
                startNewRun()
            }
            previousGameState = gameState
            return
        } else {
            logger.debug { "▶️ Run in progress: ${currentRun.id}, paused: ${currentRun.isPaused}" }
            println("[DEBUG_LOG] Run already exists: ${currentRun.id}, paused: ${currentRun.isPaused}")
            
            // Auto-reset paused runs when starting a new game in Ceres
            if (currentRun.isPaused) {
                val shouldAutoStart = checkAutoStartCondition(previousGameState, gameState)
                if (shouldAutoStart) {
                    logger.info { "🔄 Auto-resetting paused run to start new game in Ceres!" }
                    println("[DEBUG_LOG] AUTO-RESET: Clearing paused run to start new game!")
                    resetRun() // Reset the current run
                    startNewRun() // Start fresh run
                    previousGameState = gameState
                    return
                }
            }
        }

        // Don't process splits if paused
        if (currentRun.isPaused) {
            previousGameState = gameState
            return
        }

        val profile = currentProfile ?: KpdrAnyProfile.profile

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

                _splitsState.value = _splitsState.value.copy(currentRun = currentRun)

                currentSplitIndex++
                skipped = true
            } else {
                break
            }
        }

        if (skipped) {
            logger.info { "📍 Positioned at split ${currentSplitIndex}/${profile.splits.size}" }
        }
    }

    /**
     * Check if a split condition is already met in current game state
     */
    internal fun isConditionAlreadyMet(split: Split, gameState: GameState): Boolean {
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
            "kraid" -> gameState.bosses.kraid
            "phantoon" -> gameState.bosses.phantoon
            "draygon" -> gameState.bosses.draygon
            "ridley" -> gameState.bosses.ridley
            // For G4, we need to check if we're in the statues room, not just if all bosses are defeated
            // This prevents auto-triggering G4 immediately after defeating the last boss
            "golden_four" -> gameState.roomId == RoomIds.STATUES && gameState.bosses.kraid && gameState.bosses.phantoon && gameState.bosses.draygon && gameState.bosses.ridley
            "mother_brain_1" -> {
                // Use the same comprehensive detection logic as in checkMotherBrain1
                val inMbRoom = gameState.roomId == RoomIds.MOTHER_BRAIN_ROOM
                val normalGameplay = gameState.gameState == GameStateConstants.NORMAL_GAMEPLAY
                val mb1AlreadyDefeated = inMbRoom && normalGameplay && gameState.motherBrainHp >= 18000
                val zebesEscaping = (gameState.eventFlags and 0x0040) != 0
                val mbFinalDefeated = (gameState.tourianBosses and 0x0002) != 0

                gameState.bosses.motherBrain1 || mb1AlreadyDefeated || zebesEscaping || mbFinalDefeated
            }
            "mother_brain_2" -> {
                // Use the same comprehensive detection logic as in checkMotherBrain2
                val inMbRoom = gameState.roomId == RoomIds.MOTHER_BRAIN_ROOM
                val normalGameplay = gameState.gameState == GameStateConstants.NORMAL_GAMEPLAY
                val mb2AlreadyDefeated = inMbRoom && normalGameplay && gameState.motherBrainHp >= 36000
                val zebesEscaping = (gameState.eventFlags and 0x0040) != 0
                val mbFinalDefeated = (gameState.tourianBosses and 0x0002) != 0

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
     * Auto-start when gaining control in Ceres Station at the beginning of a new game
     */
    private fun checkAutoStartCondition(previousState: GameState?, currentState: GameState): Boolean {
        // For new game auto-start, look for:
        // 1. Being in Ceres Station (area ID 6)
        // 2. Normal gameplay state (gaining control)
        // 3. No significant progress (early in the game)

        val inCeres = currentState.areaId == 6
        val normalGameplay = currentState.gameState == 8 // Normal gameplay
        val earlyGame = currentState.maxHealth <= 99 && currentState.maxMissiles <= 5 // Starting stats

        val shouldAutoStart = inCeres && normalGameplay && earlyGame

        logger.debug { "🔎 Auto-start check: Ceres=$inCeres, gameplay=$normalGameplay, early=$earlyGame, condition=$shouldAutoStart" }
        logger.debug { "📊 Stats: health=${currentState.maxHealth}, missiles=${currentState.maxMissiles}, room=${currentState.roomId}" }
        println("[DEBUG_LOG] Auto-start check: area=${currentState.areaId}, gameState=${currentState.gameState}, health=${currentState.maxHealth}, missiles=${currentState.maxMissiles}")
        println("[DEBUG_LOG] Conditions: Ceres=$inCeres, gameplay=$normalGameplay, early=$earlyGame, shouldStart=$shouldAutoStart")

        if (shouldAutoStart) {
            logger.info { "🎯 Auto-start condition detected: New game in Ceres Station!" }
            println("[DEBUG_LOG] AUTO-START TRIGGERED! New game in Ceres Station!")
        }

        return shouldAutoStart
    }

    /**
     * Check if a split condition is met
     */
    private fun checkSplitCondition(split: Split, previousState: GameState?, currentState: GameState): Boolean {
        if (previousState == null) return false

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
        val finalPersonalBests = if ((isComplete || split.id == "ship") && finalRun.isPersonalBest) {
            val splitTimesMap = finalRun.completedSplits.associate { completedSplit ->
                completedSplit.splitId to completedSplit.time
            }

            val newPB = PersonalBest(
                profileId = finalRun.profileId,
                runSessionId = finalRun.id,
                totalTime = finalRun.totalTime,
                splitTimes = splitTimesMap
            )

            updatedPersonalBests + (finalRun.profileId to newPB)
        } else {
            updatedPersonalBests
        }

        _splitsState.value = currentState.copy(
            currentRun = if (isComplete || split.id == "ship") null else finalRun,
            runHistory = newRunHistory,
            personalBests = finalPersonalBests
        )

        logger.info { "⏰ Split triggered: ${split.name} at ${formatTime(totalTimeMs)} (segment: ${formatTime(segmentTimeMs)})" }

        // Save run incrementally after each split to prevent data loss
        // This saves partial runs with completed segments even if the app crashes or run is reset
        fileStorageService?.let { storage ->
            scope.launch {
                try {
                    storage.saveRun(finalRun)
                    logger.debug { "💾 Saved run progress (${finalRun.completedSplits.size} splits) to runs/ directory" }
                    
                    // Update run summaries to track segment PBs from this run so far
                    val summaries = storage.loadRunSummaries()
                    val updatedProfile = storage.deriveBestSplits(finalRun.profileId)
                    val updatedSummaries = summaries.copy(
                        lastUpdated = Clock.System.now(),
                        profiles = summaries.profiles + (finalRun.profileId to updatedProfile)
                    )
                    storage.saveRunSummaries(updatedSummaries)
                    logger.debug { "📊 Updated run summaries with current segments" }
                } catch (e: Exception) {
                    logger.error(e) { "❌ Failed to save run progress" }
                }
            }
        }

        if (isComplete || split.id == "ship") {
            logger.info { "🏁 Run completed in ${formatTime(totalTimeMs)}" }
            updatePersonalBest(finalRun)
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

    // Split condition checks - exact logic from TypeScript version

    private fun checkCeresStation(prev: GameState, curr: GameState): Boolean {
        // PRIMARY: ASL-accurate detection - room/gamestate transition
        // According to SuperMetroid.asl line 952: Room: ceresElevator (0xDF45), GameState: normalGameplay (0x8) -> startOfCeresCutscene (0x20)
        val inCeresElevator = curr.roomId == RoomIds.CERES_ELEVATOR
        val gameStateTransition = prev.gameState == GameStateConstants.NORMAL_GAMEPLAY && curr.gameState == GameStateConstants.START_OF_CERES_CUTSCENE
        val primaryDetection = inCeresElevator && gameStateTransition

        // FALLBACK: Memory flag detection (for cases where transition is missed)
        // If Ceres flag just became true AND we're not in Ceres area anymore
        val ceresCompleted = !prev.bosses.ceresStation && curr.bosses.ceresStation
        val leftCeresArea = prev.areaId == 6 && curr.areaId != 6
        val fallbackDetection = ceresCompleted && leftCeresArea

        // Always log for debugging when in or leaving Ceres Station area
        if (curr.areaName == "Ceres Station" || prev.areaName == "Ceres Station" || curr.areaId == 6 || prev.areaId == 6) {
            logger.info { "🚨 CERES DEBUG - room:0x${curr.roomId.toString(16)}, prevState:${prev.gameState}, currState:${curr.gameState}, area:${curr.areaName}" }
            logger.info { "🚨 CERES CONDITIONS - inElevator:$inCeresElevator, gameStateTransition:$gameStateTransition, primary:$primaryDetection" }
            logger.info { "🚨 CERES FALLBACK - ceresCompleted:$ceresCompleted, leftCeresArea:$leftCeresArea, fallback:$fallbackDetection" }
            println("[DEBUG_LOG] CERES DEBUG - room:0x${curr.roomId.toString(16)}, prevState:${prev.gameState}, currState:${curr.gameState}, area:${curr.areaName}")
            println("[DEBUG_LOG] CERES CONDITIONS - inElevator:$inCeresElevator, gameStateTransition:$gameStateTransition, primary:$primaryDetection")
            println("[DEBUG_LOG] CERES FALLBACK - ceresCompleted:$ceresCompleted, leftCeresArea:$leftCeresArea, fallback:$fallbackDetection")
        }

        val shouldSplit = primaryDetection || fallbackDetection

        if (shouldSplit) {
            val method = if (primaryDetection) "PRIMARY (ASL)" else "FALLBACK (Memory)"
            logger.info { "🎯 CERES SPLIT TRIGGERED via $method - Leaving Ceres Station!" }
            println("[DEBUG_LOG] 🎯 CERES SPLIT TRIGGERED via $method - Leaving Ceres Station!")
        }

        return shouldSplit
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

        // EXACT ASL LOGIC: HP transition 0 -> 18000
        val hpTransition = prev.motherBrainHp == 0 && curr.motherBrainHp == 18000

        // RETROACTIVE LOGIC: If HP >= 18000 in MB room, MB1 already defeated
        val mb1AlreadyDefeated = inMbRoom && normalGameplay && curr.motherBrainHp >= 18000

        // Also check escape scenarios
        val zebesEscaping = (curr.eventFlags and 0x0040) != 0
        val mbFinalDefeated = (curr.tourianBosses and 0x0002) != 0

        val result = hpTransition || mb1AlreadyDefeated || zebesEscaping || mbFinalDefeated

        // ALWAYS log for debugging
        logger.info { "🧠1 MB1: HP(${prev.motherBrainHp}->${curr.motherBrainHp}), room=$inMbRoom, gameplay=$normalGameplay, transition=$hpTransition, retroactive=$mb1AlreadyDefeated, escaping=$zebesEscaping, final=$mbFinalDefeated, result=$result" }

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
        val hpTransition = prev.motherBrainHp == 0 && curr.motherBrainHp == 36000

        // RETROACTIVE LOGIC: If HP >= 36000 in MB room, MB2 already defeated
        val mb2AlreadyDefeated = inMbRoom && normalGameplay && curr.motherBrainHp >= 36000

        // Also check escape/final scenarios
        val zebesEscaping = (curr.eventFlags and 0x0040) != 0
        val mbFinalDefeated = (curr.tourianBosses and 0x0002) != 0

        val result = hpTransition || mb2AlreadyDefeated || zebesEscaping || mbFinalDefeated

        // ALWAYS log for debugging
        logger.info { "🧠2 MB2: HP(${prev.motherBrainHp}->${curr.motherBrainHp}), room=$inMbRoom, gameplay=$normalGameplay, transition=$hpTransition, retroactive=$mb2AlreadyDefeated, escaping=$zebesEscaping, final=$mbFinalDefeated, result=$result" }

        return result
    }

    private fun checkShip(prev: GameState, curr: GameState): Boolean {
        // Implement ASL RTA finish logic exactly:
        // escape = zebesAblaze && shipAI.old != 0xaa4f && shipAI.current == 0xaa4f
        val zebesAblaze = (curr.eventFlags and 0x0040) != 0  // bit 6
        val motherBrainDefeated = (curr.tourianBosses and 0x0002) != 0  // Bit 1 (0x2) for Mother Brain, matching ASL and parseMotherBrainFinal
        val shipAiTransition = prev.shipAi != 0xAA4F && curr.shipAi == 0xAA4F

        val result = zebesAblaze && motherBrainDefeated && shipAiTransition

        // Always log for debugging ship detection issues
        logger.info { "🚢 Ship transition check: zebesAblaze=$zebesAblaze, mbDefeated=$motherBrainDefeated, shipAI(${prev.shipAi.toString(16)}->${curr.shipAi.toString(16)}), transition=$shipAiTransition, result=$result" }

        return result
    }

    /**
     * Generate unique run ID
     */
    private fun generateRunId(): String =
        "run_${Clock.System.now().toEpochMilliseconds()}"

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
 * KPDR Any% split profile - matches the TypeScript version exactly
 */
object KpdrAnyProfile {
    val profile = SplitProfile(
        id = "kpdr-any",
        name = "KPDR Any%",
        splits = listOf(
            Split("ceres_station", "Ceres Station", "boss", "Escape from Ceres Station"),
            Split("morph_ball", "Morph Ball", "item", "Morph Ball acquired"),
            Split("first_missile", "First Missiles", "item", "First missile pack collected"),
            Split("bomb", "Bomb", "item", "Bomb acquired"),
            Split("first_super", "First Super", "item", "First super missile pack collected"),
            Split("charge_beam", "Charge Beam", "beam", "Charge Beam acquired"),
            Split("spazer", "Spazer", "item", "Spazer acquired"),
            Split("kraid", "Kraid", "boss", "Kraid defeated"),
            Split("varia_suit", "Varia Suit", "item", "Varia Suit acquired"),
            Split("hi_jump", "Hi-Jump Boots", "item", "Hi-Jump Boots acquired"),
            Split("speed_booster", "Speed Booster", "item", "Speed Booster acquired"),
            Split("wave_beam", "Wave Beam", "beam", "Wave Beam acquired"),
            Split("ice_beam", "Ice Beam", "beam", "Ice Beam acquired"),
            Split("first_power_bomb", "First Power Bomb", "item", "First power bomb pack collected"),
            Split("phantoon", "Phantoon", "boss", "Phantoon defeated"),
            Split("gravity_suit", "Gravity Suit", "item", "Gravity Suit acquired"),
            Split("draygon", "Draygon", "boss", "Draygon defeated"),
            Split("space_jump", "Space Jump", "item", "Space Jump acquired"),
            Split("plasma_beam", "Plasma Beam", "beam", "Plasma Beam acquired"),
            Split("ridley", "Ridley", "boss", "Ridley defeated"),
            Split("golden_four", "G4", "event", "Entered Tourian (all 4 bosses defeated)"),
            Split("mother_brain_1", "Mother Brain 1", "boss", "Mother Brain phase 1 completed"),
            Split("mother_brain_2", "Mother Brain 2", "boss", "Mother Brain phase 2 completed"),
            Split("ship", "Ship", "event", "Escaped to ship (game complete)")
        )
    )
}
