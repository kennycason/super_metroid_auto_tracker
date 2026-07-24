package com.supermetroid.ui.components

import com.supermetroid.livesplit.LiveSplitAttempt
import com.supermetroid.livesplit.LiveSplitDocument
import com.supermetroid.livesplit.LiveSplitSegment
import com.supermetroid.storage.FileStorageService
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LiveSplitRunDropdownMetadataTest {

    @Test
    fun `LiveSplit run metadata displays parsed attempt dates`() {
        val metadata = buildLiveSplitRunFileMetadata(
            doc = documentWithAttempts(
                LiveSplitAttempt(
                    id = 7,
                    started = "04/01/2026 10:00:00",
                    ended = "04/01/2026 10:17:23",
                    realTime = 1_043_210L,
                    gameTime = null
                )
            ),
            profileId = "map-rando",
            profileName = "Containment Chamber (Puzzles)"
        )

        assertEquals(1, metadata.size)
        assertEquals("lss-attempt-7", metadata.single().fileName)
        assertTrue(metadata.single().displayName.contains("2026-04-01 10:00:00"))
        assertTrue(metadata.single().displayName.contains("Containment Chamber (Puzzles)"))
    }

    @Test
    fun `LiveSplit run metadata uses attempt label for missing attempt dates`() {
        val metadata = buildLiveSplitRunFileMetadata(
            doc = documentWithAttempts(
                LiveSplitAttempt(
                    id = 42,
                    started = null,
                    ended = null,
                    realTime = 950_560L,
                    gameTime = null
                )
            ),
            profileId = "map-rando",
            profileName = "Containment Chamber (Puzzles)"
        )

        val run = metadata.single()
        assertTrue(run.displayName.contains("Attempt 42"))
        assertEquals(Instant.fromEpochMilliseconds(42), run.startTime)
    }

    @Test
    fun `run metadata uses LiveSplit source instead of JSON when active`() {
        val jsonRun = runMetadata(
            fileName = "map-rando_2026-04-01_10-00-00_run_1.json",
            displayName = "JSON",
            startTime = Instant.fromEpochMilliseconds(1_459L),
            totalTime = 1_043_210L
        )
        val lssRun = runMetadata(
            fileName = "lss-attempt-7",
            displayName = "LSS",
            startTime = Instant.fromEpochMilliseconds(1_000L),
            totalTime = 1_043_210L
        )
        val unrelatedJsonRun = runMetadata(
            fileName = "map-rando_2026-04-01_11-00-00_run_2.json",
            displayName = "Unrelated",
            startTime = Instant.fromEpochMilliseconds(10_000L),
            totalTime = 900_000L
        )

        val selected = selectRunFileMetadata(
            jsonRunFiles = listOf(jsonRun, unrelatedJsonRun),
            lssRunFiles = listOf(lssRun),
            useLiveSplitSource = true
        )

        assertEquals(listOf(lssRun.fileName), selected.map { it.fileName })
    }

    @Test
    fun `run metadata falls back to JSON when no LiveSplit source is active`() {
        val jsonRun = runMetadata(
            fileName = "map-rando_2026-04-01_10-00-00_run_1.json",
            displayName = "JSON",
            startTime = Instant.fromEpochMilliseconds(1_459L),
            totalTime = 1_043_210L
        )

        val selected = selectRunFileMetadata(
            jsonRunFiles = listOf(jsonRun),
            lssRunFiles = emptyList(),
            useLiveSplitSource = false
        )

        assertEquals(listOf(jsonRun.fileName), selected.map { it.fileName })
    }

    @Test
    fun `run metadata does not show JSON rows when active LiveSplit source has no runs`() {
        val jsonRun = runMetadata(
            fileName = "map-rando_2026-04-01_10-00-00_run_1.json",
            displayName = "JSON",
            startTime = Instant.fromEpochMilliseconds(1_459L),
            totalTime = 1_043_210L
        )

        val selected = selectRunFileMetadata(
            jsonRunFiles = listOf(jsonRun),
            lssRunFiles = emptyList(),
            useLiveSplitSource = true
        )

        assertTrue(selected.isEmpty())
    }

    private fun documentWithAttempts(vararg attempts: LiveSplitAttempt): LiveSplitDocument {
        return LiveSplitDocument(
            gameName = "Super Metroid",
            categoryName = "Map Rando",
            attemptCount = attempts.size,
            segments = listOf(
                LiveSplitSegment(
                    name = "Finish",
                    icon = null,
                    bestSegmentTime = null,
                    splitTimes = emptyList(),
                    segmentHistory = emptyList()
                )
            ),
            attemptHistory = attempts.toList()
        )
    }

    private fun runMetadata(
        fileName: String,
        displayName: String,
        startTime: Instant,
        totalTime: Long,
        profileId: String = "map-rando"
    ): FileStorageService.RunFileMetadata {
        return FileStorageService.RunFileMetadata(
            fileName = fileName,
            displayName = displayName,
            isComplete = true,
            startTime = startTime,
            totalTime = totalTime,
            profileId = profileId
        )
    }
}
