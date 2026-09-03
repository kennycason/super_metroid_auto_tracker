package com.supermetroid.service

import com.supermetroid.autosplits.AutoSplitsEngine
import com.supermetroid.autosplits.SplitProfiles
import com.supermetroid.storage.FileStorageService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsExactly
import strikt.assertions.containsKey
import strikt.assertions.doesNotContain
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isTrue
import java.io.File
import java.nio.file.Path

class SplitProfileServiceTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var storage: FileStorageService
    private lateinit var engine: AutoSplitsEngine
    private lateinit var service: SplitProfileService

    @BeforeEach
    fun setUp() = runBlocking {
        storage = FileStorageService(tempDir.toString())
        engine = AutoSplitsEngine(storage)
        service = SplitProfileService(storage, engine) { "custom-test" }
        service.initialize()
    }

    @Test
    fun `creates and selects a custom profile with ship last`() = runBlocking {
        val result = service.createCustomProfile(
            name = "My Route",
            splitIds = listOf("morph_ball", "kraid", "ship"),
            splitNameOverrides = mapOf("morph_ball" to "Morph")
        )

        expectThat(result).isA<SplitProfileSaveResult.Success>().and {
            get { profile.id }.isEqualTo("custom-test-v1")
            get { profile.version }.isEqualTo(1)
            get { profile.splits.map { it.id } }
                .containsExactly("morph_ball", "kraid", "ship")
            get { profile.splits.first().name }.isEqualTo("Morph")
        }
        expectThat(service.currentProfile.value.id).isEqualTo("custom-test-v1")
        expectThat(engine.splitsState.value.currentRun).isEqualTo(null)

        val saved = storage.loadSplitProfilesConfig()
        expectThat(saved.customProfiles).hasSize(1)
        expectThat(saved.customProfiles.single().splitDefinitions.map { it.id })
            .containsExactly("morph_ball", "kraid", "ship")
        expectThat(saved.customProfiles.single().splitNameOverrides).containsKey("morph_ball")
        Unit
    }

    @Test
    fun `display name edits retain version and structural edits create a revision`() = runBlocking {
        val created = service.createCustomProfile(
            "My Route",
            listOf("morph_ball", "kraid", "ship"),
            emptyMap()
        ) as SplitProfileSaveResult.Success

        val renamed = service.saveProfile(
            created.profile.id,
            "My Renamed Route",
            listOf("morph_ball", "kraid", "ship"),
            mapOf("kraid" to "Big K")
        ) as SplitProfileSaveResult.Success

        expectThat(renamed.versionChanged).isFalse()
        expectThat(renamed.profile.id).isEqualTo("custom-test-v1")
        expectThat(renamed.profile.splits[1].name).isEqualTo("Big K")

        val revised = service.saveProfile(
            renamed.profile.id,
            renamed.profile.name,
            listOf("kraid", "morph_ball", "ship"),
            mapOf("kraid" to "Big K")
        ) as SplitProfileSaveResult.Success

        expectThat(revised.versionChanged).isTrue()
        expectThat(revised.profile.id).isEqualTo("custom-test-v2")
        expectThat(revised.profile.version).isEqualTo(2)
        expectThat(service.availableProfiles.value.map { it.id }) {
            contains("custom-test-v2")
            doesNotContain("custom-test-v1")
        }
        expectThat(service.findProfileById("custom-test-v1")).isNotNull().and {
            get { splits.map { it.id } }.containsExactly("morph_ball", "kraid", "ship")
        }

        val saved = storage.loadSplitProfilesConfig()
        expectThat(saved.archivedProfiles.map { it.id }).contains("custom-test-v1")
        Unit
    }

    @Test
    fun `built in editor only saves per-profile display overrides`() = runBlocking {
        val profile = service.findProfileById(SplitProfiles.ID_HUNDRED_PERCENT)!!
        val originalIds = profile.splits.map { it.id }

        val result = service.saveProfile(
            profile.id,
            profile.name,
            originalIds,
            mapOf("varia_suit" to "Hot Suit")
        ) as SplitProfileSaveResult.Success

        expectThat(result.versionChanged).isFalse()
        expectThat(result.profile.id).isEqualTo(SplitProfiles.ID_HUNDRED_PERCENT)
        expectThat(result.profile.splits.first { it.id == "varia_suit" }.name).isEqualTo("Hot Suit")

        val resetResult = service.saveProfile(
            profile.id,
            profile.name,
            originalIds,
            mapOf("varia_suit" to "Varia")
        ) as SplitProfileSaveResult.Success
        expectThat(resetResult.profile.splits.first { it.id == "varia_suit" }.name).isEqualTo("Varia")
        expectThat(storage.loadSplitProfilesConfig().builtInSplitNameOverrides.containsKey(profile.id))
            .isFalse()
        Unit
    }

    @Test
    fun `rejects invalid structures and conflicting names`() = runBlocking {
        expectThat(
            service.createCustomProfile("No Finish", listOf("morph_ball"), emptyMap())
        ).isA<SplitProfileSaveResult.Failure>()

        expectThat(
            service.createCustomProfile("Duplicate", listOf("ship", "ship"), emptyMap())
        ).isA<SplitProfileSaveResult.Failure>()

        expectThat(
            service.createCustomProfile("kpdr ANY%", listOf("ship"), emptyMap())
        ).isA<SplitProfileSaveResult.Failure>()
        Unit
    }

    @Test
    fun `deleting custom profile requires backup and keeps archived definition`() = runBlocking {
        val created = service.createCustomProfile(
            "Disposable",
            listOf("morph_ball", "ship"),
            emptyMap(),
            mapOf("morph_ball" to fixture("crocomire.png").absolutePath)
        ) as SplitProfileSaveResult.Success
        val image = created.profile.splitImageOverrides.getValue("morph_ball")

        expectThat(service.deleteCustomProfile(created.profile.id))
            .isA<SplitProfileDeleteResult.Success>()

        expectThat(service.availableProfiles.value.map { it.id })
            .doesNotContain(created.profile.id)
        expectThat(service.findProfileById(created.profile.id)).isNotNull()
        expectThat(service.currentProfile.value.id).isEqualTo(SplitProfiles.DEFAULT.id)

        val backupFiles = tempDir.resolve("backups").toFile().listFiles().orEmpty()
        expectThat(backupFiles.any { it.name.startsWith("split-profiles_") }).isTrue()
        val config = storage.loadSplitProfilesConfig()
        expectThat(config.archivedProfiles.map { it.id }).contains(created.profile.id)
        expectThat(config.archivedProfiles.single().splitImageOverrides).containsKey("morph_ball")
        expectThat(storage.resolveSplitProfileImage(image.originalPath)?.isFile).isEqualTo(true)
        Unit
    }

    @Test
    fun `new runs snapshot custom profile and do not require ceres first`() = runBlocking {
        val created = service.createCustomProfile(
            "Hack Route",
            listOf("morph_ball", "ship"),
            emptyMap()
        ) as SplitProfileSaveResult.Success

        engine.toggleRunState(created.profile.id)

        val run = engine.splitsState.value.currentRun
        expectThat(run).isNotNull().and {
            get { profileId }.isEqualTo(created.profile.id)
            get { profileSnapshot?.id }.isEqualTo(created.profile.id)
            get { profileSnapshot?.splits?.first()?.id }.isEqualTo("morph_ball")
        }
        Unit
    }

    @Test
    fun `structural edits are blocked while that profile has an active run`() = runBlocking {
        val created = service.createCustomProfile(
            "Active Route",
            listOf("morph_ball", "kraid", "ship"),
            emptyMap()
        ) as SplitProfileSaveResult.Success
        engine.toggleRunState(created.profile.id)

        val result = service.saveProfile(
            created.profile.id,
            created.profile.name,
            listOf("kraid", "morph_ball", "ship"),
            emptyMap()
        )

        expectThat(result).isA<SplitProfileSaveResult.Failure>()
        expectThat(service.currentProfile.value.id).isEqualTo("custom-test-v1")
        expectThat(storage.loadSplitProfilesConfig().archivedProfiles).hasSize(0)
        Unit
    }

    @Test
    fun `profile switching is blocked instead of discarding an active run`() = runBlocking {
        engine.toggleRunState(service.currentProfile.value.id)
        val activeRunId = engine.splitsState.value.currentRun!!.id

        val switched = service.setProfile(service.findProfileById(SplitProfiles.ID_HUNDRED_PERCENT)!!)

        expectThat(switched).isFalse()
        expectThat(service.currentProfile.value.id).isEqualTo(SplitProfiles.DEFAULT.id)
        expectThat(engine.splitsState.value.currentRun?.id).isEqualTo(activeRunId)
        Unit
    }

    @Test
    fun `custom profiles and selection load after restart`() = runBlocking {
        val created = service.createCustomProfile(
            "Persistent Route",
            listOf("morph_ball", "ship"),
            mapOf("morph_ball" to "Ball")
        ) as SplitProfileSaveResult.Success

        val restartedEngine = AutoSplitsEngine(storage)
        val restartedService = SplitProfileService(storage, restartedEngine) { "unused" }
        restartedService.initialize()

        expectThat(restartedService.currentProfile.value.id).isEqualTo(created.profile.id)
        expectThat(restartedService.currentProfile.value.splits.map { it.name })
            .containsExactly("Ball", "Ship")
        Unit
    }

    @Test
    fun `saved timer restores against a custom profile after profiles initialize`() = runBlocking {
        val created = service.createCustomProfile(
            "Restored Route",
            listOf("morph_ball", "ship"),
            emptyMap()
        ) as SplitProfileSaveResult.Success
        val currentConfig = storage.loadAppConfig()
        storage.saveAppConfig(
            currentConfig.copy(
                savedTimerMs = 12_345L,
                savedTimerProfileId = created.profile.id
            )
        )

        val restartedEngine = AutoSplitsEngine(storage)
        val restartedService = SplitProfileService(storage, restartedEngine) { "unused" }
        restartedService.initialize()
        restartedEngine.initialize()
        // The desktop UI also invokes service initialization after startup;
        // this must not clear the timer that was just restored.
        restartedService.initialize()

        expectThat(restartedEngine.splitsState.value.currentRun).isNotNull().and {
            get { profileId }.isEqualTo(created.profile.id)
            get { profileSnapshot?.id }.isEqualTo(created.profile.id)
            get { profileSnapshot?.splits?.map { it.id } }
                .isEqualTo(listOf("morph_ball", "ship"))
            get { totalTime }.isEqualTo(12_345L)
        }
        Unit
    }

    @Test
    fun `custom split image persists across restart and can be restored to default`() = runBlocking {
        val created = service.createCustomProfile(
            "Illustrated Route",
            listOf("kraid", "ship"),
            emptyMap(),
            mapOf("kraid" to fixture("kraid.png").absolutePath)
        ) as SplitProfileSaveResult.Success

        val image = created.profile.splitImageOverrides["kraid"]
        expectThat(image).isNotNull().and {
            get { previewWidth }.isEqualTo(228)
            get { previewHeight }.isEqualTo(128)
        }
        expectThat(image?.let(service::resolveSplitImageFile)?.isFile).isEqualTo(true)

        val restartedEngine = AutoSplitsEngine(storage)
        val restartedService = SplitProfileService(storage, restartedEngine) { "unused" }
        restartedService.initialize()
        val restored = restartedService.findProfileById(created.profile.id)!!
        expectThat(restored.splitImageOverrides).containsKey("kraid")

        val removed = restartedService.saveProfile(
            restored.id,
            restored.name,
            restored.splits.map { it.id },
            emptyMap(),
            removedSplitImageIds = setOf("kraid")
        ) as SplitProfileSaveResult.Success
        expectThat(removed.profile.splitImageOverrides).hasSize(0)
        Unit
    }

    @Test
    fun `built-in profiles support per-profile split images without changing structure`() = runBlocking {
        val profile = service.findProfileById(SplitProfiles.ID_HUNDRED_PERCENT)!!

        val saved = service.saveProfile(
            profile.id,
            profile.name,
            profile.splits.map { it.id },
            emptyMap(),
            splitImageSources = mapOf("phantoon" to fixture("crocomire.png").absolutePath)
        ) as SplitProfileSaveResult.Success

        expectThat(saved.versionChanged).isFalse()
        expectThat(saved.profile.splitImageOverrides["phantoon"]).isNotNull().and {
            get { previewWidth }.isEqualTo(128)
            get { previewHeight }.isEqualTo(134)
        }
        expectThat(storage.loadSplitProfilesConfig().builtInSplitImageOverrides[profile.id])
            .isNotNull().and { containsKey("phantoon") }
        Unit
    }

    private fun fixture(name: String): File = File(
        requireNotNull(javaClass.getResource("/split-images/$name")) {
            "Missing split image fixture $name"
        }.toURI()
    )
}
