package com.supermetroid.livesplit

import com.supermetroid.storage.FileStorageService
import java.io.File

/**
 * CLI tool: detects and repairs corrupted PB cumulative times in LiveSplit (.lss) files.
 *
 * Corruption pattern: non-monotonic "Personal Best" cumulative times across segments
 * (e.g., Spazer showing 3.9s cumulative after Charge Beam at 8:41). This can happen
 * when segments are reordered/removed/re-added in LiveSplit.
 *
 * Repair strategy: find the actual PB attempt from attempt history, then reconstruct
 * cumulative PB times by summing that attempt's segment times from segment history.
 *
 * Usage:
 *   ./gradlew cleanupLiveSplit
 *   ./gradlew cleanupLiveSplit --args="/path/to/file.lss"
 *
 * If no path is provided, uses the configured LSS path from smtracker.json.
 */
fun main(args: Array<String>) {
    val fileStorage = FileStorageService()
    val parser = LiveSplitParser()
    val writer = LiveSplitWriter()

    // Determine LSS file path
    val path = if (args.isNotEmpty()) {
        args[0]
    } else {
        val config = kotlinx.coroutines.runBlocking { fileStorage.loadAppConfig() }
        config.liveSplitFilePath
    }

    if (path.isNullOrBlank()) {
        println("ERROR: No LSS file path provided and none configured in smtracker.json")
        println("Usage: ./gradlew cleanupLiveSplit --args=\"/path/to/file.lss\"")
        return
    }

    val file = File(path)
    if (!file.exists()) {
        println("ERROR: File not found: $path")
        return
    }

    println("=== LiveSplit Cleanup Utility ===")
    println("File: ${file.absolutePath}")
    println()

    // Parse the LSS
    val doc = parser.parseFile(file)
    println("Game: ${doc.gameName} - ${doc.categoryName}")
    println("Segments: ${doc.segments.size}")
    println("Attempts: ${doc.attemptHistory.size}")
    println()

    // Detect corruptions
    val corruptions = detectCorruptions(doc)

    if (corruptions.isEmpty()) {
        println("No corruptions detected. File is clean.")
        return
    }

    println("CORRUPTIONS DETECTED: ${corruptions.size} non-monotonic PB cumulative times")
    for (c in corruptions) {
        println("  ${c.segmentName}: cumulative=${formatMs(c.cumulativeMs)} but previous=${formatMs(c.previousCumulativeMs)}")
    }
    println()

    // Attempt repair
    val repaired = repairPbCumulativeTimes(doc)
    if (repaired == null) {
        println("UNABLE TO REPAIR: Could not reconstruct PB times from attempt/segment history.")
        println("Manual intervention required.")
        return
    }

    // Verify repair
    val postRepairCorruptions = detectCorruptions(repaired)
    if (postRepairCorruptions.isNotEmpty()) {
        println("WARNING: Repair incomplete — ${postRepairCorruptions.size} corruptions remain.")
        for (c in postRepairCorruptions) {
            println("  ${c.segmentName}: cumulative=${formatMs(c.cumulativeMs)} but previous=${formatMs(c.previousCumulativeMs)}")
        }
        println("Skipping save.")
        return
    }

    // Show what changed
    println("REPAIR SUCCESSFUL — reconstructed PB cumulative times:")
    var prevCumulative = 0L
    for (seg in repaired.segments) {
        val pbTime = seg.splitTimes.find { it.comparisonName == "Personal Best" }?.realTime
        val segTime = if (pbTime != null) pbTime - prevCumulative else 0L
        val marker = if (doc.segments.find { it.name == seg.name }
                ?.splitTimes?.find { it.comparisonName == "Personal Best" }?.realTime != pbTime) " (FIXED)" else ""
        println("  ${seg.name}: ${formatMs(pbTime ?: 0)} (segment: ${formatMs(segTime)})$marker")
        if (pbTime != null && pbTime > 0) prevCumulative = pbTime
    }
    println()

    // Backup original
    println("Backing up original file...")
    val backed = fileStorage.backupFileSync(file)
    if (!backed) {
        println("ERROR: Failed to create backup. Aborting — no changes written.")
        return
    }
    println("Backup saved to: ${fileStorage.getBackupsDir().absolutePath}")

    // Write repaired file
    writer.writeToFile(repaired, file)
    println("Repaired file written to: ${file.absolutePath}")
    println()
    println("Done.")
}

data class Corruption(
    val segmentName: String,
    val segmentIndex: Int,
    val cumulativeMs: Long,
    val previousCumulativeMs: Long
)

/**
 * Detect non-monotonic PB cumulative times across segments.
 */
fun detectCorruptions(doc: LiveSplitDocument): List<Corruption> {
    val corruptions = mutableListOf<Corruption>()
    var maxCumulative = 0L

    for ((index, segment) in doc.segments.withIndex()) {
        val pbTime = segment.splitTimes.find { it.comparisonName == "Personal Best" }?.realTime ?: continue
        if (pbTime > 0 && pbTime < maxCumulative) {
            corruptions.add(Corruption(segment.name, index, pbTime, maxCumulative))
        }
        if (pbTime > maxCumulative) {
            maxCumulative = pbTime
        }
    }

    return corruptions
}

/**
 * Repair PB cumulative times by finding the actual PB attempt and
 * reconstructing cumulative times from segment history.
 */
fun repairPbCumulativeTimes(doc: LiveSplitDocument): LiveSplitDocument? {
    // Find the PB attempt: the completed attempt with the fastest RealTime
    val pbAttempt = doc.attemptHistory
        .filter { it.realTime != null && it.realTime > 0 }
        .minByOrNull { it.realTime!! }
        ?: return null

    val pbAttemptId = pbAttempt.id
    println("PB attempt: #$pbAttemptId (${formatMs(pbAttempt.realTime!!)})")

    // Reconstruct cumulative times from segment history for this attempt
    var cumulative = 0L
    val repairedSegments = doc.segments.map { segment ->
        // Find this attempt's segment time in segment history
        val historyEntry = segment.segmentHistory.find { it.id == pbAttemptId }
        val segmentRealTime = historyEntry?.realTime

        val newPbCumulative = if (segmentRealTime != null && segmentRealTime > 0) {
            cumulative += segmentRealTime
            cumulative
        } else {
            // No data for this segment in the PB attempt — keep existing or null
            val existing = segment.splitTimes.find { it.comparisonName == "Personal Best" }?.realTime
            if (existing != null && existing > cumulative) {
                cumulative = existing
            }
            existing
        }

        // Update the "Personal Best" comparison split time
        val updatedSplitTimes = segment.splitTimes.map { st ->
            if (st.comparisonName == "Personal Best") {
                st.copy(realTime = newPbCumulative)
            } else {
                st
            }
        }

        // If there's no PB comparison yet, add one
        val hasPb = updatedSplitTimes.any { it.comparisonName == "Personal Best" }
        val finalSplitTimes = if (!hasPb && newPbCumulative != null) {
            updatedSplitTimes + LiveSplitComparisonSplit("Personal Best", newPbCumulative, null)
        } else {
            updatedSplitTimes
        }

        segment.copy(splitTimes = finalSplitTimes)
    }

    return doc.copy(segments = repairedSegments)
}

private fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val centiseconds = (ms % 1000) / 10
    return if (hours > 0) {
        "%d:%02d:%02d.%02d".format(hours, minutes, seconds, centiseconds)
    } else {
        "%02d:%02d.%02d".format(minutes, seconds, centiseconds)
    }
}
