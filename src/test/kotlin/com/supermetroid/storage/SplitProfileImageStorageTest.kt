package com.supermetroid.storage

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isTrue
import java.io.File
import java.nio.file.Path
import javax.imageio.ImageIO

class SplitProfileImageStorageTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `wide upload preserves original and scales shorter side to 128`() = runBlocking {
        val storage = FileStorageService(tempDir.toString())
        val source = fixture("kraid.png")

        val asset = storage.saveSplitProfileImage("custom-test", "kraid", source.absolutePath)

        expectThat(asset) {
            get { originalWidth }.isEqualTo(1232)
            get { originalHeight }.isEqualTo(693)
            get { previewWidth }.isEqualTo(228)
            get { previewHeight }.isEqualTo(128)
            get { originalPath }.contains("split-profile-images/custom-test/kraid/original-")
            get { previewPath }.contains("split-profile-images/custom-test/kraid/preview-")
        }
        val original = storage.resolveSplitProfileImage(asset.originalPath)
        val preview = storage.resolveSplitProfileImage(asset.previewPath)
        expectThat(original).isNotNull().and {
            get { readBytes().contentEquals(source.readBytes()) }.isTrue()
        }
        expectThat(preview).isNotNull().and {
            get { ImageIO.read(this).width }.isEqualTo(228)
            get { ImageIO.read(this).height }.isEqualTo(128)
        }
        Unit
    }

    @Test
    fun `near-square tall upload remains proportional and at least 128 on both sides`() = runBlocking {
        val storage = FileStorageService(tempDir.toString())

        val asset = storage.saveSplitProfileImage(
            "custom-test",
            "crocomire",
            fixture("crocomire.png").absolutePath
        )

        expectThat(asset) {
            get { originalWidth }.isEqualTo(903)
            get { originalHeight }.isEqualTo(948)
            get { previewWidth }.isEqualTo(128)
            get { previewHeight }.isEqualTo(134)
        }
        expectThat(asset.previewWidth >= 128 && asset.previewHeight >= 128).isTrue()
        Unit
    }

    @Test
    fun `small side is never upscaled or reduced below 128`() {
        expectThat(calculateSplitImagePreviewDimensions(800, 100))
            .isEqualTo(SplitImageDimensions(800, 100))
        expectThat(calculateSplitImagePreviewDimensions(128, 900))
            .isEqualTo(SplitImageDimensions(128, 900))
        expectThat(calculateSplitImagePreviewDimensions(256, 512))
            .isEqualTo(SplitImageDimensions(128, 256))
    }

    @Test
    fun `stored image resolver rejects paths outside tracker directory`() {
        val storage = FileStorageService(tempDir.toString())
        expectThat(storage.resolveSplitProfileImage("../outside.png")).isNull()
    }

    private fun fixture(name: String): File = File(
        requireNotNull(javaClass.getResource("/split-images/$name")) {
            "Missing split image fixture $name"
        }.toURI()
    )
}
