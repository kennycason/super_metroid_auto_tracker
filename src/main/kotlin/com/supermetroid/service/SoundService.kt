package com.supermetroid.service

import com.supermetroid.storage.FileStorageService
import com.supermetroid.util.Logging
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File
import java.nio.file.Paths
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Service to manage sound effects for item collection and boss defeats
 */
class SoundService(private val fileStorageService: FileStorageService) : Logging {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _volume = MutableStateFlow(0.7f) // 0.0 to 1.0
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val soundsConfig = MutableStateFlow<SoundConfig?>(null)
    private val soundsDirectory = Paths.get(System.getProperty("user.home"), ".smtracker", "sounds").toFile()
    private val soundsConfigFile = Paths.get(System.getProperty("user.home"), ".smtracker", "sounds.json").toFile()

    // Track previous states to detect changes
    private var previousGameState: com.supermetroid.model.GameState? = null
    private var isInitialized = false

    suspend fun initialize() {
        try {
            // Load sound settings from app config
            val config = fileStorageService.loadAppConfig()
            _soundEnabled.value = config.soundEnabled
            _volume.value = config.volume

            // Create sounds directory if it doesn't exist
            if (!soundsDirectory.exists()) {
                soundsDirectory.mkdirs()
                logger.info { "📁 Created sounds directory: ${soundsDirectory.absolutePath}" }
            }

            // Load or create sounds.json
            loadOrCreateSoundsConfig()

            isInitialized = true
            logger.info { "🔊 Sound service initialized: ${if (_soundEnabled.value) "enabled" else "disabled"}, volume: ${(_volume.value * 100).toInt()}%" }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to initialize sound service" }
        }
    }

    private suspend fun loadOrCreateSoundsConfig() {
        try {
            if (soundsConfigFile.exists()) {
                val json = soundsConfigFile.readText()
                soundsConfig.value = Json.decodeFromString<SoundConfig>(json)
                logger.info { "📄 Loaded existing sounds.json" }
            } else {
                // Create default sounds.json with all tracked items
                val defaultConfig = createDefaultSoundsConfig()
                soundsConfig.value = defaultConfig
                saveSoundsConfig(defaultConfig)
                logger.info { "📄 Created default sounds.json" }
            }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to load/create sounds config" }
            // Create a minimal config as fallback
            soundsConfig.value = SoundConfig(emptyList())
        }
    }

    private fun createDefaultSoundsConfig(): SoundConfig {
        val allItems = listOf(
            // Ammo/Collectibles
            "health", "reserve_tank", "missile", "super", "powerbomb",

            // Major items
            "morph_ball", "bombs", "charge", "spazer", "varia", "hijump", "speed_booster",
            "wave", "ice", "gravity", "space", "spring", "screw", "plasma",
            "grapple", "xray",

            // Bosses
            "ceres_station", "bomb_torizo", "spore_spawn", "kraid", "phantoon",
            "botwoon", "draygon", "gold_torizo", "ridley", "golden_four",
            "mother_brain_1", "mother_brain_2", "ship"
        )

        val events = allItems.map { itemName ->
            SoundEvent(
                name = itemName,
                activeSound = "", // Empty by default
                volume = 1.0f
            )
        }

        return SoundConfig(events)
    }

    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    private suspend fun saveSoundsConfig(config: SoundConfig) {
        try {
            val json = Json {
                prettyPrint = true
                prettyPrintIndent = "  "
            }.encodeToString(config)
            soundsConfigFile.writeText(json)
            logger.info { "💾 Saved sounds.json" }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to save sounds config" }
        }
    }

    fun processGameStateChange(newGameState: com.supermetroid.model.GameState) {
        if (!isInitialized || !_soundEnabled.value || soundsConfig.value == null) {
            return
        }

        val previous = previousGameState
        if (previous == null) {
            // First time - don't play sounds on startup
            previousGameState = newGameState
            return
        }

        // Check for item collection events
        checkItemCollection(previous, newGameState)
        checkBossDefeats(previous, newGameState)

        previousGameState = newGameState
    }

    private fun checkItemCollection(previous: com.supermetroid.model.GameState, current: com.supermetroid.model.GameState) {

        // Check beams
        if (!previous.beams.charge && current.beams.charge) playSound("charge")
        if (!previous.beams.ice && current.beams.ice) playSound("ice")
        if (!previous.beams.wave && current.beams.wave) playSound("wave")
        if (!previous.beams.spazer && current.beams.spazer) playSound("spazer")
        if (!previous.beams.plasma && current.beams.plasma) playSound("plasma")

        // Check items
        if (!previous.items.morph && current.items.morph) playSound("morph_ball")
        if (!previous.items.bombs && current.items.bombs) playSound("bombs")
        if (!previous.items.hiJump && current.items.hiJump) playSound("hijump")
        if (!previous.items.speed && current.items.speed) playSound("speed_booster")
        if (!previous.items.gravity && current.items.gravity) playSound("gravity")
        if (!previous.items.spaceJump && current.items.spaceJump) playSound("space")
        if (!previous.items.spring && current.items.spring) playSound("spring")
        if (!previous.items.screw && current.items.screw) playSound("screw")
        if (!previous.items.grapple && current.items.grapple) playSound("grapple")
        if (!previous.items.xray && current.items.xray) playSound("xray")
        if (!previous.items.varia && current.items.varia) playSound("varia")

        // Check ammo increases (max capacity changes indicate item pickup)
        if (previous.maxMissiles < current.maxMissiles) {
            logger.info { "🎯 Missile expansion detected: ${previous.maxMissiles} -> ${current.maxMissiles}" }
            playSound("missile")
        }
        if (previous.maxSupers < current.maxSupers) {
            logger.info { "🎯 Super missile expansion detected: ${previous.maxSupers} -> ${current.maxSupers}" }
            playSound("super")
        }
        if (previous.maxPowerBombs < current.maxPowerBombs) {
            logger.info { "🎯 Power bomb expansion detected: ${previous.maxPowerBombs} -> ${current.maxPowerBombs}" }
            playSound("powerbomb")
        }
        if (previous.maxHealth < current.maxHealth) {
            logger.info { "🎯 Energy tank detected: ${previous.maxHealth} -> ${current.maxHealth}" }
            playSound("health")
        }
        if (previous.maxReserveEnergy < current.maxReserveEnergy) {
            logger.info { "🎯 Reserve tank detected: ${previous.maxReserveEnergy} -> ${current.maxReserveEnergy}" }
            playSound("reserve_tank")
        }
    }

    private fun checkBossDefeats(previous: com.supermetroid.model.GameState, current: com.supermetroid.model.GameState) {
        // Mini-bosses and major bosses
        if (!previous.bosses.bombTorizo && current.bosses.bombTorizo) playSound("bomb_torizo")
        if (!previous.bosses.sporeSpawn && current.bosses.sporeSpawn) playSound("spore_spawn")
        if (!previous.bosses.kraid && current.bosses.kraid) playSound("kraid")
        if (!previous.bosses.phantoon && current.bosses.phantoon) playSound("phantoon")
        if (!previous.bosses.botwoon && current.bosses.botwoon) playSound("botwoon")
        if (!previous.bosses.draygon && current.bosses.draygon) playSound("draygon")
        if (!previous.bosses.ridley && current.bosses.ridley) playSound("ridley")
        if (!previous.bosses.goldenTorizo && current.bosses.goldenTorizo) playSound("gold_torizo")

        // Mother Brain phases
        if (!previous.bosses.motherBrain1 && current.bosses.motherBrain1) playSound("mother_brain_1")
        if (!previous.bosses.motherBrain2 && current.bosses.motherBrain2) playSound("mother_brain_2")

        // Endgame
        if (!previous.bosses.samusShip && current.bosses.samusShip) playSound("ship")
    }

    private fun playSound(eventName: String) {
        val config = soundsConfig.value ?: return
        val event = config.events.find { it.name == eventName } ?: return

        if (event.activeSound.isBlank()) {
            return // No sound configured
        }

        coroutineScope.launch {
            try {
                val soundFile = if (event.activeSound.startsWith("/") || event.activeSound.contains(":")) {
                    // Absolute path
                    File(event.activeSound)
                } else {
                    // Relative to sounds directory
                    File(soundsDirectory, event.activeSound)
                }

                if (!soundFile.exists()) {
                    logger.warn { "🔊 Sound file not found: ${soundFile.absolutePath}" }
                    return@launch
                }

                // Check file extension
                val extension = soundFile.extension.lowercase()

                if (extension == "mp3") {
                    // Use JLayer for MP3 playback (note: per-file volume is not supported here)
                    playMP3Sound(soundFile, event.volume)
                } else {
                    // Use Java Sound (AudioSystem) for WAV/AIFF playback with per-file volume support
                    playWavSound(soundFile, event.volume)
                }

                logger.info { "🔊 Playing sound for event: $eventName" }
            } catch (e: Exception) {
                logger.error(e) { "❌ Failed to play sound: $eventName" }
            }
        }
    }

    private fun playWavSound(soundFile: File, eventVolume: Float) {
        val audioInputStream = AudioSystem.getAudioInputStream(soundFile)
        val clip = AudioSystem.getClip()
        clip.open(audioInputStream)

        // Set volume
        val gainControl = clip.getControl(FloatControl.Type.MASTER_GAIN) as? FloatControl
        if (gainControl != null) {
            val volume = _volume.value * eventVolume
            val gain = 20f * kotlin.math.log10(volume.toDouble()).toFloat()
            gainControl.value = gain.coerceIn(gainControl.minimum, gainControl.maximum)
        }

        // Play once (don't loop)
        clip.framePosition = 0
        clip.start()

        // Clean up when done
        clip.addLineListener { event ->
            if (event.type == javax.sound.sampled.LineEvent.Type.STOP) {
                clip.close()
            }
        }
    }

    private fun playMP3Sound(soundFile: File, @Suppress("UNUSED_PARAMETER") eventVolume: Float) {
        try {
            // Use JLayer for MP3 playback
            val inputStream = java.io.FileInputStream(soundFile)
            val bufferedStream = java.io.BufferedInputStream(inputStream)
            val player = javazoom.jl.player.Player(bufferedStream)

            // Note: JLayer doesn't have volume control built-in.
            // We'd need to implement our own audio device for volume control.
            // For now, play at system volume (volume control works for WAV files).
            // Assumption: sounds are short and non-looping; resources are closed after playback.

            // Play in a separate thread so it doesn't block
            Thread {
                try {
                    player.play()
                } catch (e: Exception) {
                    logger.error(e) { "❌ Error during MP3 playback" }
                } finally {
                    try { player.close() } catch (_: Exception) {}
                    try { bufferedStream.close() } catch (_: Exception) {}
                    try { inputStream.close() } catch (_: Exception) {}
                }
            }.start()
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to play MP3 sound: ${soundFile.name}" }
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        _soundEnabled.value = enabled
        // Save to app config
        val config = fileStorageService.loadAppConfig()
        val updatedConfig = config.copy(soundEnabled = enabled)
        fileStorageService.saveAppConfig(updatedConfig)
        logger.info { "🔊 Sound effects ${if (enabled) "enabled" else "disabled"}" }
    }

    suspend fun setVolume(volume: Float) {
        _volume.value = volume.coerceIn(0.0f, 1.0f)
        // Save to app config
        val config = fileStorageService.loadAppConfig()
        val updatedConfig = config.copy(volume = _volume.value)
        fileStorageService.saveAppConfig(updatedConfig)
        logger.info { "🔊 Volume set to ${(_volume.value * 100).toInt()}%" }
    }
}

@Serializable
data class SoundConfig(
    val events: List<SoundEvent>
)

@Serializable
data class SoundEvent(
    val name: String,
    val activeSound: String,
    val volume: Float = 1.0f
)
