package com.supermetroid.livesplit

import com.supermetroid.autosplits.SplitProfiles
import com.supermetroid.model.RunSession
import com.supermetroid.storage.FileStorageService
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * CLI tool: exports JSON run files to a LiveSplit (.lss) file.
 *
 * Usage:
 *   ./gradlew run -PmainClass=com.supermetroid.livesplit.ExportRunsToLiveSplitKt
 *   -- or --
 *   Run this main() directly from your IDE
 *
 * Reads from ~/.smtracker/runs/, writes to ~/.smtracker/exports/
 * Does NOT modify or delete the original JSON files.
 */
fun main() {
    val storage = FileStorageService()
    val converter = LiveSplitConverter()
    val writer = LiveSplitWriter()

    val allRuns: List<RunSession> = runBlocking { storage.loadAllRuns() }

    val profileIds = allRuns.map { it.profileId }.distinct().sorted()
    println("Found ${allRuns.size} total runs across profiles: $profileIds")

    val exportDir = File(System.getProperty("user.home"), ".smtracker/exports")
    if (!exportDir.exists()) exportDir.mkdirs()

    for (profileId in profileIds) {
        val profileRuns = allRuns.filter { it.profileId == profileId }.sortedBy { it.startTime }
        val profile = SplitProfiles.BY_ID[profileId]

        if (profile == null) {
            println("  SKIP $profileId - no matching SplitProfile found in registry")
            continue
        }

        val completedCount = profileRuns.count { it.endTime != null }
        val pbRun = profileRuns.filter { it.endTime != null }.minByOrNull { it.totalTime }
        val pbTimeStr = pbRun?.let { formatTime(it.totalTime) } ?: "N/A"

        println("\n=== $profileId (${profile.name}) ===")
        println("  Runs: ${profileRuns.size} total, $completedCount complete")
        println("  PB: $pbTimeStr")
        println("  Splits: ${profile.splits.size} (${profile.splits.joinToString(", ") { it.name }})")

        val doc = converter.fromRunHistory(profileRuns, profile)

        val outputFile = File(exportDir, "${profileId}.lss")
        writer.writeToFile(doc, outputFile)

        // Verify the written file by parsing it back
        val parser = LiveSplitParser()
        val verified = parser.parseFile(outputFile)

        println("  Written: ${outputFile.absolutePath}")
        println("  Verified: ${verified.segments.size} segments, ${verified.attemptHistory.size} attempts")

        val sumOfBest = verified.segments.mapNotNull { it.bestSegmentTime?.realTime }.sum()
        println("  Sum of Best: ${formatTime(sumOfBest)}")

        val pbFromLss = verified.segments.lastOrNull()?.splitTimes
            ?.find { it.comparisonName == "Personal Best" }?.realTime
        if (pbFromLss != null) {
            println("  PB in LSS: ${formatTime(pbFromLss)}")
        }
    }

    println("\nDone! Exported LSS files to: ${exportDir.absolutePath}")
    println("Original JSON files are UNTOUCHED in ~/.smtracker/runs/")
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val centis = (ms % 1000) / 10
    return if (hours > 0) "%d:%02d:%02d.%02d".format(hours, minutes, seconds, centis)
    else "%d:%02d.%02d".format(minutes, seconds, centis)
}
