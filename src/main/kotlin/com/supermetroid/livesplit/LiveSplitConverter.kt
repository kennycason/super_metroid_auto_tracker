package com.supermetroid.livesplit

import com.supermetroid.model.*
import com.supermetroid.util.Logging
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Converts between LiveSplit (.lss) data and our internal model.
 *
 * Handles:
 * - Mapping LSS segment names to our split IDs
 * - Converting LSS time data to our SplitTime/PersonalBest model
 * - Creating SplitProfiles from LSS segment lists
 * - Converting our RunSession data back to LSS format
 */
class LiveSplitConverter : Logging {

    /**
     * Known mappings from common LiveSplit segment names to our internal split IDs.
     * Case-insensitive lookup. Multiple LSS names can map to the same split ID.
     */
    private val segmentNameToSplitId: Map<String, String> = mapOf(
        // Bosses
        "kraid" to "kraid",
        "phantoon" to "phantoon",
        "phaaan" to "phantoon",
        "draygon" to "draygon",
        "water" to "draygon",
        "ridley" to "ridley",
        "mother brain 1" to "mother_brain_1",
        "mb1" to "mother_brain_1",
        "mother brain 2" to "mother_brain_2",
        "mb2" to "mother_brain_2",
        "mother brain" to "mother_brain_2",

        // Items
        "morph" to "morph_ball",
        "morph ball" to "morph_ball",
        "bomb" to "bomb",
        "bombs" to "bomb",
        "missile" to "first_missile",
        "first missile" to "first_missile",
        "first missiles" to "first_missile",
        "super" to "first_super",
        "first super" to "first_super",
        "first supers" to "first_super",
        "super missile" to "first_super",
        "charge" to "charge_beam",
        "charge beam" to "charge_beam",
        "power bomb" to "first_power_bomb",
        "first power bomb" to "first_power_bomb",
        "power bombs" to "first_power_bomb",
        "pb" to "first_power_bomb",

        // Suits
        "varia" to "varia_suit",
        "varia suit" to "varia_suit",
        "gravity" to "gravity_suit",
        "gravity suit" to "gravity_suit",

        // Beams
        "ice" to "ice_beam",
        "ice beam" to "ice_beam",
        "wave" to "wave_beam",
        "wave beam" to "wave_beam",
        "spazer" to "spazer",
        "plasma" to "plasma_beam",
        "plasma beam" to "plasma_beam",

        // Movement items
        "hi-jump" to "hi_jump",
        "hi-jump boots" to "hi_jump",
        "hi jump" to "hi_jump",
        "speed" to "speed_booster",
        "speed booster" to "speed_booster",
        "space" to "space_jump",
        "space jump" to "space_jump",
        "screw" to "screw_attack",
        "screw attack" to "screw_attack",
        "grapple" to "grapple_beam",
        "grapple beam" to "grapple_beam",
        "spring ball" to "spring_ball",
        "x-ray" to "xray_scope",
        "x-ray scope" to "xray_scope",

        // Other items
        "reserve" to "reserve_tank",
        "reserve tank" to "reserve_tank",

        // Events
        "ceres" to "ceres_station",
        "ceres station" to "ceres_station",
        "g4" to "golden_four",
        "golden four" to "golden_four",
        "tourian" to "golden_four",
        "ship" to "ship",
        "done" to "ship",
        "escape" to "ship"
    )

    /**
     * Maps a split ID to its display type for our model
     */
    private val splitIdToType: Map<String, String> = mapOf(
        "ceres_station" to "boss",
        "morph_ball" to "item",
        "bomb" to "item",
        "first_missile" to "item",
        "first_super" to "item",
        "charge_beam" to "beam",
        "spazer" to "item",
        "kraid" to "boss",
        "varia_suit" to "item",
        "hi_jump" to "item",
        "speed_booster" to "item",
        "wave_beam" to "beam",
        "ice_beam" to "beam",
        "first_power_bomb" to "item",
        "phantoon" to "boss",
        "gravity_suit" to "item",
        "draygon" to "boss",
        "space_jump" to "item",
        "plasma_beam" to "beam",
        "screw_attack" to "item",
        "grapple_beam" to "item",
        "reserve_tank" to "item",
        "spring_ball" to "item",
        "xray_scope" to "item",
        "ridley" to "boss",
        "golden_four" to "event",
        "mother_brain_1" to "boss",
        "mother_brain_2" to "boss",
        "ship" to "event"
    )

    /**
     * Derive a split ID from a LiveSplit segment name.
     * Falls back to a sanitized version of the name if no known mapping exists.
     */
    fun deriveSplitId(segmentName: String): String {
        val normalized = segmentName.trim().lowercase()
        return segmentNameToSplitId[normalized]
            ?: normalized.replace(Regex("[^a-z0-9]+"), "_").trim('_')
    }

    /**
     * Convert a [LiveSplitDocument] into a [SplitProfile].
     * Uses the game/category name and segment list to create splits.
     */
    fun toSplitProfile(doc: LiveSplitDocument, profileId: String? = null): SplitProfile {
        val id = profileId ?: doc.categoryName.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')

        val name = "${doc.gameName} - ${doc.categoryName}".takeIf { doc.gameName.isNotBlank() }
            ?: doc.categoryName

        val splits = doc.segments.map { segment ->
            val splitId = deriveSplitId(segment.name)
            val type = splitIdToType[splitId] ?: "event"
            Split(
                id = splitId,
                name = segment.name,
                type = type,
                description = "Imported from LiveSplit"
            )
        }

        return SplitProfile(id = id, name = name, splits = splits)
    }

    /**
     * Extract [PersonalBest] data from a [LiveSplitDocument].
     * Uses the "Personal Best" comparison split times and best segment times.
     */
    fun toPersonalBest(doc: LiveSplitDocument, profileId: String): PersonalBest {
        val splitTimes = mutableMapOf<String, SplitTime>()
        var prevCumulativeTime = 0L

        for (segment in doc.segments) {
            val splitId = deriveSplitId(segment.name)

            val pbSplit = segment.splitTimes.find { it.comparisonName == "Personal Best" }
            val cumulativeTime = pbSplit?.realTime ?: 0L
            val bestSegment = segment.bestSegmentTime?.realTime ?: 0L

            val segmentTime = if (cumulativeTime > 0 && prevCumulativeTime >= 0) {
                cumulativeTime - prevCumulativeTime
            } else {
                bestSegment
            }

            splitTimes[splitId] = SplitTime(
                totalTime = cumulativeTime,
                segmentTime = bestSegment.takeIf { it > 0 } ?: segmentTime
            )

            if (cumulativeTime > 0) {
                prevCumulativeTime = cumulativeTime
            }
        }

        // The PB total is the last segment's cumulative "Personal Best" time,
        // not the fastest attempt (which could be a quick reset with a timer value)
        val totalTime = doc.segments.lastOrNull()?.splitTimes
            ?.find { it.comparisonName == "Personal Best" }?.realTime ?: 0L

        return PersonalBest(
            profileId = profileId,
            runSessionId = "livesplit-import",
            totalTime = totalTime,
            splitTimes = splitTimes
        )
    }

    /**
     * Convert a full run history into a [LiveSplitDocument].
     * Builds proper attempt history, best segments, PB comparison, and segment history
     * from all runs (both complete and incomplete).
     */
    fun fromRunHistory(
        runs: List<RunSession>,
        profile: SplitProfile
    ): LiveSplitDocument {
        val sortedRuns = runs.sortedBy { it.startTime }

        val completedRuns = sortedRuns.filter { it.endTime != null }
        val pbRun = completedRuns.minByOrNull { it.totalTime }

        val bestSegmentTimes = mutableMapOf<String, Long>()
        for (run in completedRuns) {
            for (cs in run.completedSplits) {
                val current = bestSegmentTimes[cs.splitId]
                if (current == null || cs.time.segmentTime < current) {
                    bestSegmentTimes[cs.splitId] = cs.time.segmentTime
                }
            }
        }

        val segments = profile.splits.map { split ->
            val bestSegMs = bestSegmentTimes[split.id]
            val bestSegTime = bestSegMs?.let { LiveSplitTimeSpan(realTime = it, gameTime = null) }

            val pbSplitTime = pbRun?.completedSplits?.find { it.splitId == split.id }
            val pbComparison = pbSplitTime?.let {
                LiveSplitComparisonSplit(
                    comparisonName = "Personal Best",
                    realTime = it.time.totalTime,
                    gameTime = null
                )
            }
            val splitTimes = listOfNotNull(pbComparison)

            val history = mutableListOf<LiveSplitHistoryEntry>()
            for ((runIdx, run) in sortedRuns.withIndex()) {
                val cs = run.completedSplits.find { it.splitId == split.id }
                if (cs != null) {
                    history.add(LiveSplitHistoryEntry(
                        id = runIdx + 1,
                        realTime = cs.time.segmentTime,
                        gameTime = null
                    ))
                }
            }

            LiveSplitSegment(
                name = split.name,
                icon = null,
                bestSegmentTime = bestSegTime,
                splitTimes = splitTimes,
                segmentHistory = history
            )
        }

        val attemptHistory = sortedRuns.mapIndexed { index, run ->
            LiveSplitAttempt(
                id = index + 1,
                started = formatInstantForLiveSplit(run.startTime),
                ended = run.endTime?.let { formatInstantForLiveSplit(it) },
                realTime = if (run.endTime != null) run.totalTime else null,
                gameTime = null
            )
        }

        return LiveSplitDocument(
            gameName = "Super Metroid",
            categoryName = profile.name,
            attemptCount = sortedRuns.size,
            segments = segments,
            attemptHistory = attemptHistory
        )
    }

    /**
     * Convert our [RunSession] data into a [LiveSplitDocument], merging with
     * an existing document if provided (to preserve attempt history, icons, etc.)
     */
    fun fromRunSession(
        run: RunSession,
        profile: SplitProfile,
        existingDoc: LiveSplitDocument? = null
    ): LiveSplitDocument {
        val segments = profile.splits.mapIndexed { segIdx, split ->
            val completedSplit = run.completedSplits.find { it.splitId == split.id }
            val existingSegment = existingDoc?.segments?.getOrNull(segIdx)

            val bestSegment = existingSegment?.bestSegmentTime
                ?: completedSplit?.let {
                    LiveSplitTimeSpan(realTime = it.time.segmentTime, gameTime = null)
                }

            val splitTimes = if (completedSplit != null) {
                listOf(
                    LiveSplitComparisonSplit(
                        comparisonName = "Personal Best",
                        realTime = completedSplit.time.totalTime,
                        gameTime = null
                    )
                )
            } else {
                existingSegment?.splitTimes ?: emptyList()
            }

            LiveSplitSegment(
                name = split.name,
                icon = existingSegment?.icon,
                bestSegmentTime = bestSegment,
                splitTimes = splitTimes,
                segmentHistory = existingSegment?.segmentHistory ?: emptyList()
            )
        }

        val newAttempt = LiveSplitAttempt(
            id = (existingDoc?.attemptHistory?.maxOfOrNull { it.id } ?: 0) + 1,
            started = formatInstantForLiveSplit(run.startTime),
            ended = run.endTime?.let { formatInstantForLiveSplit(it) },
            realTime = if (run.endTime != null) run.totalTime else null,
            gameTime = null
        )

        val attemptHistory = (existingDoc?.attemptHistory ?: emptyList()) + newAttempt

        return LiveSplitDocument(
            gameName = existingDoc?.gameName ?: "Super Metroid",
            categoryName = existingDoc?.categoryName ?: profile.name,
            attemptCount = attemptHistory.size,
            offset = existingDoc?.offset ?: "00:00:00.0000000",
            segments = segments,
            attemptHistory = attemptHistory,
            autoSplitterSettings = existingDoc?.autoSplitterSettings
        )
    }

    companion object {
        /** Thread-safe date formatter for LiveSplit attempt timestamps */
        private val liveSplitDateFormat: DateTimeFormatter =
            DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss")
                .withZone(ZoneId.systemDefault())

        /** Format a kotlinx Instant consistently for LiveSplit attempt history */
        fun formatInstantForLiveSplit(instant: Instant): String {
            val javaInstant = java.time.Instant.ofEpochMilli(instant.toEpochMilliseconds())
            return liveSplitDateFormat.format(javaInstant)
        }
    }
}
