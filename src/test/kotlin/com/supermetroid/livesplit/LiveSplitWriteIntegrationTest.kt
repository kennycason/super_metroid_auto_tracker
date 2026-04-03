package com.supermetroid.livesplit

import com.supermetroid.autosplits.AutoSplitsEngine
import com.supermetroid.autosplits.SplitProfiles
import com.supermetroid.model.*
import com.supermetroid.service.SplitFormatService
import com.supermetroid.service.SplitProfileService
import com.supermetroid.storage.FileStorageService
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.time.Duration.Companion.minutes

class LiveSplitWriteIntegrationTest {

    private lateinit var fileStorage: FileStorageService
    private lateinit var autoSplitsEngine: AutoSplitsEngine
    private lateinit var splitProfileService: SplitProfileService
    private lateinit var splitFormatService: SplitFormatService
    private lateinit var lssFile: File

    @BeforeEach
    fun setup(@TempDir tempDir: Path) {
        fileStorage = FileStorageService(tempDir.toAbsolutePath().toString())
        autoSplitsEngine = AutoSplitsEngine(fileStorage)
        splitProfileService = SplitProfileService(fileStorage, autoSplitsEngine)
        splitFormatService = SplitFormatService(fileStorage, splitProfileService)

        // Wire up the callback (same as AppDependencies.create())
        autoSplitsEngine.setOnRunSavedCallback { run ->
            splitFormatService.handleRunSaved(run)
        }

        lssFile = File(tempDir.toFile(), "test-output.lss")
    }

    private fun makeCompletedRun(
        profileId: String = "kpdr-any",
        totalTimeMs: Long = 3600_000L,
        startTime: Instant = Clock.System.now()
    ): RunSession {
        val profile = SplitProfiles.getProfileById(profileId)
        var cumulativeMs = 0L
        val splits = profile.splits.map { split ->
            val segmentMs = totalTimeMs / profile.splits.size
            cumulativeMs += segmentMs
            CompletedSplit(
                splitId = split.id,
                time = SplitTime(
                    totalTime = cumulativeMs,
                    segmentTime = segmentMs
                ),
                timestamp = startTime + cumulativeMs.minutes
            )
        }
        return RunSession(
            id = "run_${startTime.toEpochMilliseconds()}",
            profileId = profileId,
            startTime = startTime,
            endTime = startTime + totalTimeMs.minutes,
            completedSplits = splits,
            totalTime = totalTimeMs
        )
    }

    @Test
    fun `handleRunSaved writes to LSS file when LiveSplit write is enabled`() = runBlocking {
        // Enable LiveSplit writing and set file path
        splitFormatService.setWriteLiveSplit(true)
        splitFormatService.setLiveSplitFilePath(lssFile.absolutePath)
        splitProfileService.initialize()

        val run = makeCompletedRun(totalTimeMs = 3_600_000L)

        // Simulate what AutoSplitsEngine does
        fileStorage.saveRun(run)
        splitFormatService.handleRunSaved(run)

        assertTrue(lssFile.exists(), "LSS file should be created")

        val parser = LiveSplitParser()
        val doc = parser.parseFile(lssFile)
        assertEquals("Super Metroid", doc.gameName)
        assertEquals(24, doc.segments.size)
        assertEquals(1, doc.attemptHistory.size)
        assertNotNull(doc.attemptHistory[0].realTime, "Completed run should have a real time")
    }

    @Test
    fun `handleRunSaved appends multiple runs to same LSS file`() = runBlocking {
        splitFormatService.setWriteLiveSplit(true)
        splitFormatService.setLiveSplitFilePath(lssFile.absolutePath)
        splitProfileService.initialize()

        val run1 = makeCompletedRun(totalTimeMs = 3_800_000L)
        fileStorage.saveRun(run1)
        splitFormatService.handleRunSaved(run1)

        val run2 = makeCompletedRun(totalTimeMs = 3_600_000L)
        fileStorage.saveRun(run2)
        splitFormatService.handleRunSaved(run2)

        val parser = LiveSplitParser()
        val doc = parser.parseFile(lssFile)
        assertEquals(2, doc.attemptHistory.size, "Should have 2 attempts")
        assertEquals(2, doc.attemptCount)
    }

    @Test
    fun `handleRunSaved does nothing when LiveSplit write is disabled`() = runBlocking {
        splitFormatService.setWriteLiveSplit(false)
        splitFormatService.setLiveSplitFilePath(lssFile.absolutePath)
        splitProfileService.initialize()

        val run = makeCompletedRun()
        fileStorage.saveRun(run)
        splitFormatService.handleRunSaved(run)

        assertFalse(lssFile.exists(), "LSS file should NOT be created when write is disabled")
    }

    @Test
    fun `handleRunSaved does nothing when no LSS file path is set`() = runBlocking {
        splitFormatService.setWriteLiveSplit(true)
        // Don't set a file path
        splitProfileService.initialize()

        val run = makeCompletedRun()
        fileStorage.saveRun(run)
        splitFormatService.handleRunSaved(run)

        assertFalse(lssFile.exists(), "LSS file should NOT be created when no path is set")
    }

    @Test
    fun `handleRunSaved skips run with mismatched profile`() = runBlocking {
        splitFormatService.setWriteLiveSplit(true)
        splitFormatService.setLiveSplitFilePath(lssFile.absolutePath)
        splitProfileService.initialize()

        // Current profile defaults to kpdr-any, but this run is for a different profile
        val run = makeCompletedRun(profileId = "hundred-percent")
        fileStorage.saveRun(run)
        splitFormatService.handleRunSaved(run)

        assertFalse(lssFile.exists(), "LSS file should NOT be created for mismatched profile")
    }

    @Test
    fun `handleRunSaved updates best segment times across runs`() = runBlocking {
        splitFormatService.setWriteLiveSplit(true)
        splitFormatService.setLiveSplitFilePath(lssFile.absolutePath)
        splitProfileService.initialize()

        // First run: slower
        val run1 = makeCompletedRun(totalTimeMs = 4_000_000L)
        fileStorage.saveRun(run1)
        splitFormatService.handleRunSaved(run1)

        val parser = LiveSplitParser()
        val doc1 = parser.parseFile(lssFile)
        val firstBest = doc1.segments[0].bestSegmentTime?.realTime

        // Second run: faster (so segments should be faster)
        val run2 = makeCompletedRun(totalTimeMs = 3_600_000L)
        fileStorage.saveRun(run2)
        splitFormatService.handleRunSaved(run2)

        val doc2 = parser.parseFile(lssFile)
        val secondBest = doc2.segments[0].bestSegmentTime?.realTime

        assertNotNull(firstBest)
        assertNotNull(secondBest)
        // The fromRunSession merge should keep the faster segment
        assertTrue(
            secondBest!! <= firstBest!!,
            "Best segment should not get slower: first=$firstBest, after=$secondBest"
        )
    }

    @Test
    fun `callback wiring - onRunSaved fires handleRunSaved`() = runBlocking {
        splitFormatService.setWriteLiveSplit(true)
        splitFormatService.setLiveSplitFilePath(lssFile.absolutePath)
        splitProfileService.initialize()

        var callbackFired = false
        autoSplitsEngine.setOnRunSavedCallback { run ->
            callbackFired = true
            splitFormatService.handleRunSaved(run)
        }

        val run = makeCompletedRun()

        // Directly invoke the callback (simulating what the engine does after storage.saveRun)
        autoSplitsEngine.setOnRunSavedCallback { r ->
            callbackFired = true
            splitFormatService.handleRunSaved(r)
        }

        // Simulate the callback
        val callback = autoSplitsEngine::class.java.getDeclaredField("onRunSaved")
        callback.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val fn = callback.get(autoSplitsEngine) as? ((RunSession) -> Unit)
        fn?.invoke(run)

        assertTrue(callbackFired, "Callback should have fired")
        assertTrue(lssFile.exists(), "LSS file should be created via callback")
    }

    @Test
    fun `toLiveSplitSplitsState includes runHistory with synthetic PB run so BEST and BP columns work`() = runBlocking {
        // Simulate: load an LSS (e.g. from export), then build state. State must have runHistory
        // so findActualPbRun finds the PB and BEST column and BP delta show (same as JSON load).
        val stream = javaClass.getResourceAsStream("/livesplit/100_percent.lss")
            ?: error("Test resource not found")
        val parser = LiveSplitParser()
        val doc = parser.parse(stream)
        val converter = LiveSplitConverter()
        val profile = converter.toSplitProfile(doc, "hundred-percent")
        converter.toPersonalBest(doc, profile.id)

        fileStorage.saveAppConfig(
            com.supermetroid.model.AppConfig(
                splitReadFormat = "livesplit",
                splitWriteLiveSplit = true,
                liveSplitFilePath = lssFile.absolutePath
            )
        )
        LiveSplitWriter().writeToFile(doc, lssFile)
        splitFormatService.initialize()

        val state = splitFormatService.toLiveSplitSplitsState()
        assertNotNull(state)
        assertTrue(state!!.runHistory.isNotEmpty(), "LiveSplit state should include runs in runHistory")
        assertTrue(state.runHistory.any { it.id == "livesplit-pb" }, "runHistory should include synthetic PB run")
        assertTrue(state.personalBests.isNotEmpty())
        val resolvedProfile = splitFormatService.getResolvedLiveSplitProfile()
        assertNotNull(resolvedProfile)
        assertEquals("hundred-percent", resolvedProfile!!.id, "100% LSS keeps its id when no built-in match")
    }

    @Test
    fun `incomplete run (no endTime) still appends to LSS as DNF`() = runBlocking {
        splitFormatService.setWriteLiveSplit(true)
        splitFormatService.setLiveSplitFilePath(lssFile.absolutePath)
        splitProfileService.initialize()

        val now = Clock.System.now()
        val incompleteRun = RunSession(
            id = "run_incomplete",
            profileId = "kpdr-any",
            startTime = now,
            endTime = null,
            completedSplits = listOf(
                CompletedSplit(
                    splitId = "ceres_station",
                    time = SplitTime(totalTime = 90000, segmentTime = 90000),
                    timestamp = now
                )
            ),
            totalTime = 90000
        )

        fileStorage.saveRun(incompleteRun)
        splitFormatService.handleRunSaved(incompleteRun)

        assertTrue(lssFile.exists())
        val parser = LiveSplitParser()
        val doc = parser.parseFile(lssFile)
        assertEquals(1, doc.attemptHistory.size)
        assertNull(doc.attemptHistory[0].realTime, "Incomplete run should be recorded as DNF (no realTime)")
    }
}
