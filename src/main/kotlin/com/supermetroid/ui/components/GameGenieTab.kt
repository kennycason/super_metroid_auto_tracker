package com.supermetroid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.supermetroid.ui.theme.TrackerColors
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import com.supermetroid.service.GameGenieDecoder

/**
 * Game Genie tab for entering SNES Game Genie codes and applying preset cheats
 */
@Composable
fun GameGenieTab(
    modifier: Modifier = Modifier
) {
    var gameGenieEnabled by remember { mutableStateOf(false) }
    var customCode by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableStateOf("") }
    var presetExpanded by remember { mutableStateOf(false) }
    var isApplying by remember { mutableStateOf(false) }

    // Load Game Genie enabled state
    LaunchedEffect(Unit) {
        try {
            val config = com.supermetroid.storage.FileStorageService().loadAppConfig()
            gameGenieEnabled = config.gameGenieEnabled
        } catch (e: Exception) {
            // Use default value
        }
    }

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Game Genie",
            style = MaterialTheme.typography.headlineSmall.copy(
                color = TrackerColors.Primary,
                fontWeight = FontWeight.Bold
            )
        )

        if (!gameGenieEnabled) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = TrackerColors.SurfaceOverlayLight
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Game Genie Disabled",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TrackerColors.Primary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Enable Game Genie in General Settings to use cheat codes and presets.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TrackerColors.OnSurfaceVariant
                        )
                    )
                }
            }
        } else {
            // Custom Game Genie Code Section
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = TrackerColors.SurfaceOverlayLight
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Custom Game Genie Code",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TrackerColors.Primary,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    OutlinedTextField(
                        value = customCode,
                        onValueChange = { 
                            // Auto-format the input as user types
                            customCode = formatGameGenieCode(it.uppercase())
                        },
                        label = { Text("Enter SNES Game Genie Code") },
                        placeholder = { Text("e.g., DCD7-F26D or 7E0998:00FF") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TrackerColors.Primary,
                            focusedLabelColor = TrackerColors.Primary
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (customCode.isNotBlank()) {
                                    isApplying = true
                                    GlobalScope.launch {
                                        try {
                                            applyGameGenieCode(customCode)
                                        } catch (e: Exception) {
                                            // Handle error
                                        } finally {
                                            isApplying = false
                                        }
                                    }
                                }
                            },
                            enabled = customCode.isNotBlank() && !isApplying,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TrackerColors.Primary,
                                contentColor = Color.White,
                                disabledContainerColor = TrackerColors.SurfaceVariant,
                                disabledContentColor = TrackerColors.OnSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (isApplying) "Applying..." else "Apply Code",
                                color = Color.White
                            )
                        }

                        Button(
                            onClick = { customCode = "" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TrackerColors.SurfaceVariant,
                                contentColor = TrackerColors.OnSurfaceVariant
                            )
                        ) {
                            Text("Clear")
                        }
                    }
                }
            }

            // Preset Cheats Section
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = TrackerColors.SurfaceOverlayLight
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Preset Cheats",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TrackerColors.Primary,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Box {
                        Button(
                            onClick = { presetExpanded = !presetExpanded },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TrackerColors.SurfaceVariant,
                                contentColor = TrackerColors.OnSurfaceVariant
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = selectedPreset.ifBlank { "Select a preset cheat..." },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        DropdownMenu(
                            expanded = presetExpanded,
                            onDismissRequest = { presetExpanded = false },
                            modifier = Modifier.background(TrackerColors.Surface)
                        ) {
                            GameGeniePreset.values().forEach { preset ->
                                DropdownMenuItem(
                                    text = { Text(preset.displayName, style = MaterialTheme.typography.bodyMedium) },
                                    onClick = {
                                        selectedPreset = preset.displayName
                                        presetExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (selectedPreset.isNotBlank()) {
                        Button(
                            onClick = {
                                val preset = GameGeniePreset.values().find { it.displayName == selectedPreset }
                                if (preset != null) {
                                    isApplying = true
                                    GlobalScope.launch {
                                        try {
                                            applyPresetCheat(preset)
                                        } catch (e: Exception) {
                                            // Handle error
                                        } finally {
                                            isApplying = false
                                        }
                                    }
                                }
                            },
                            enabled = !isApplying,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TrackerColors.Primary,
                                contentColor = Color.White,
                                disabledContainerColor = TrackerColors.SurfaceVariant,
                                disabledContentColor = TrackerColors.OnSurfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isApplying) "Applying..." else "Apply Preset",
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Preset cheat options for Game Genie
 * Based on actual Game Genie codes from game_genie.txt
 */
enum class GameGeniePreset(val displayName: String, val description: String, val codes: List<String>) {
    // Direct memory writes (these should work immediately)
    FULL_HEALTH("Full Health", "Set health to maximum", listOf("7E09C2:00FF", "7E09C4:00FF")),
    INFINITE_MISSILES("Infinite Missiles", "Set missiles to maximum", listOf("7E09C6:00FF", "7E09C8:00FF")),
    INFINITE_SUPERS("Infinite Super Missiles", "Set super missiles to maximum", listOf("7E09CA:00FF", "7E09CC:00FF")),
    INFINITE_POWER_BOMBS("Infinite Power Bombs", "Set power bombs to maximum", listOf("7E09CE:00FF", "7E09D0:00FF")),
    
    // Game Genie codes (these need to be decoded first)
    NO_ENERGY_LOSS("No Energy Loss", "No energy loss from enemies", listOf("C225-3005")),
    SUPER_JUMPS("Super Jumps", "Super jumps don't drain energy", listOf("C22A-456D")),
    ALMOST_INFINITE_MISSILES("Almost Infinite Missiles", "Almost infinite missiles", listOf("C288-C5A7")),
    ALMOST_INFINITE_SUPERS("Almost Infinite Super Missiles", "Almost infinite super missiles", listOf("C28A-C9D7")),
    ALMOST_INFINITE_BOMBS("Almost Infinite Power Bombs", "Almost infinite power bombs", listOf("3CA4-450D")),
    
    // Missile count presets from game_genie.txt
    MISSILES_10("Missiles = 10", "Maximum missiles = 10", listOf("DCD7-F26D")),
    MISSILES_25("Missiles = 25", "Maximum missiles = 25", listOf("FBD7-F26D")),
    MISSILES_50("Missiles = 50", "Maximum missiles = 50", listOf("74D7-F26D")),
    MISSILES_75("Missiles = 75", "Maximum missiles = 75", listOf("08D7-F26D")),
    MISSILES_100("Missiles = 100", "Maximum missiles = 100", listOf("10D7-F26D")),
    
    // Super missile count presets
    SUPERS_5("Super Missiles = 5", "Maximum super missiles = 5", listOf("D9D7-F36D")),
    SUPERS_10("Super Missiles = 10", "Maximum super missiles = 10", listOf("DCD7-F36D")),
    SUPERS_25("Super Missiles = 25", "Maximum super missiles = 25", listOf("FBD7-F36D")),
    SUPERS_50("Super Missiles = 50", "Maximum super missiles = 50", listOf("74D7-F36D")),
    
    // Power bomb count presets
    BOMBS_5("Power Bombs = 5", "Maximum power bombs = 5", listOf("D9D7-FE6D")),
    BOMBS_10("Power Bombs = 10", "Maximum power bombs = 10", listOf("DCD7-FE6D")),
    BOMBS_25("Power Bombs = 25", "Maximum power bombs = 25", listOf("FBD7-FE6D")),
    BOMBS_50("Power Bombs = 50", "Maximum power bombs = 50", listOf("74D7-FE6D"));
}

/**
 * Format Game Genie code input to handle various formats
 */
private fun formatGameGenieCode(input: String): String {
    // Remove all non-alphanumeric characters except colons
    val cleaned = input.replace(Regex("[^A-F0-9:]"), "")
    
    // If it contains a colon, it's already in ADDRESS:VALUE format
    if (cleaned.contains(":")) {
        return cleaned
    }
    
    // If it's 8 characters, format as Game Genie code (DCD7-F26D)
    if (cleaned.length == 8) {
        return "${cleaned.substring(0, 4)}-${cleaned.substring(4, 8)}"
    }
    
    // If it's 4 characters, add placeholder for second part
    if (cleaned.length == 4) {
        return "$cleaned-"
    }
    
    // If it's longer than 8, truncate to 8
    if (cleaned.length > 8) {
        return "${cleaned.substring(0, 4)}-${cleaned.substring(4, 8)}"
    }
    
    return cleaned
}

/**
 * Apply a custom Game Genie code to the emulator
 */
private suspend fun applyGameGenieCode(code: String) {
    val gameGenieService = com.supermetroid.service.GameGenieCodeService()
    
    // Check if it's a direct memory address format (7E0998:00FF)
    if (code.contains(":")) {
        val success = gameGenieService.applyGameGenieCode(code)
        if (success) {
            println("✅ Memory address code applied: $code")
        } else {
            println("❌ Failed to apply memory address code: $code")
        }
    } else {
        // It's a Game Genie code format - convert to memory address
        val memoryCode = convertGameGenieToMemoryAddress(code)
        if (memoryCode != null) {
            val success = gameGenieService.applyGameGenieCode(memoryCode)
            if (success) {
                println("✅ Game Genie code applied: $code -> $memoryCode")
            } else {
                println("❌ Failed to apply Game Genie code: $code")
            }
        } else {
            println("❌ Invalid Game Genie code format: $code")
        }
    }
}

/**
 * Convert Game Genie code to memory address format using the full SNES decoder
 */
private fun convertGameGenieToMemoryAddress(gameGenieCode: String): String? {
    val decoder = com.supermetroid.service.GameGenieDecoder()
    
    // Clean the input code
    val cleaned = gameGenieCode.replace(Regex("[^A-Za-z0-9]"), "").uppercase()
    
    // Validate the code format
    if (!decoder.isValidGameGenieCode(cleaned)) {
        println("❌ Invalid Game Genie code format: $gameGenieCode")
        println("💡 Game Genie codes must be 8 characters using only: A,P,Z,L,G,I,T,O,X,U,K,S,V,Y,E,N")
        return null
    }
    
    // Decode the Game Genie code
    val decoded = decoder.decodeGameGenieCode(cleaned)
    
    if (!decoded.isValid) {
        println("❌ Failed to decode Game Genie code: $gameGenieCode")
        println("💡 Error: ${decoded.errorMessage}")
        return null
    }
    
    // Convert to RetroArch format
    val retroArchFormat = decoder.convertToRetroArchFormat(decoded)
    
    if (retroArchFormat != null) {
        val description = decoder.describeGameGenieCode(decoded)
        println("✅ Game Genie code decoded: $gameGenieCode")
        println("📋 $description")
        println("🎯 RetroArch format: $retroArchFormat")
        return retroArchFormat
    } else {
        println("❌ Failed to convert to RetroArch format")
        return null
    }
}

/**
 * Apply a preset cheat to the emulator
 */
private suspend fun applyPresetCheat(preset: GameGeniePreset) {
    val gameGenieService = com.supermetroid.service.GameGenieCodeService()
    val decoder = GameGenieDecoder()
    
    // Convert Game Genie codes to memory addresses
    val memoryCodes = mutableListOf<String>()
    
    for (code in preset.codes) {
        if (code.contains(":")) {
            // Already a memory address format
            memoryCodes.add(code)
        } else {
            // Game Genie code - decode it
            val cleaned = code.replace(Regex("[^A-Za-z0-9]"), "").uppercase()
            if (decoder.isValidGameGenieCode(cleaned)) {
                val decoded = decoder.decodeGameGenieCode(cleaned)
                val retroArchFormat = decoder.convertToRetroArchFormat(decoded)
                if (retroArchFormat != null) {
                    memoryCodes.add(retroArchFormat)
                    println("✅ Decoded Game Genie code: $code -> $retroArchFormat")
                } else {
                    println("❌ Failed to decode Game Genie code: $code")
                }
            } else {
                println("❌ Invalid Game Genie code format: $code")
            }
        }
    }
    
    if (memoryCodes.isNotEmpty()) {
        val success = gameGenieService.applyGameGenieCodes(memoryCodes)
        if (success) {
            println("✅ Preset cheat applied: ${preset.displayName}")
        } else {
            println("❌ Failed to apply preset cheat: ${preset.displayName}")
        }
    } else {
        println("❌ No valid codes to apply for preset: ${preset.displayName}")
    }
}
