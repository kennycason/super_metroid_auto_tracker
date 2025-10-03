package com.supermetroid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermetroid.service.EffectType
import com.supermetroid.service.PaletteEffectsState
import com.supermetroid.ui.theme.TrackerColors

/**
 * Panel for controlling visual effects applied to the game
 */
@Composable
fun EffectsPanel(
    effectsState: PaletteEffectsState,
    onEffectTypeChanged: (EffectType) -> Unit,
    onIntensityChanged: (Float) -> Unit,
    logoEffectsService: com.supermetroid.service.LogoEffectsService,
    themeService: com.supermetroid.service.ThemeService,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = TrackerColors.Surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // First Row - Game and Logo Effects
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left Column - Map Effects
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                // Title
                Text(
                    text = "Game Effects",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = TrackerColors.Primary,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // Effect type selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    EffectButton(
                        text = "Psyche",
                        selected = effectsState.activeEffect == EffectType.PSYCHEDELIC,
                        enabled = effectsState.enabled,
                        onClick = { onEffectTypeChanged(EffectType.PSYCHEDELIC) }
                    )

                    EffectButton(
                        text = "Neon",
                        selected = effectsState.activeEffect == EffectType.NEON,
                        enabled = effectsState.enabled,
                        onClick = { onEffectTypeChanged(EffectType.NEON) }
                    )
                }

                // Second row of effect buttons
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    EffectButton(
                        text = "Rainbow",
                        selected = effectsState.activeEffect == EffectType.RAINBOW,
                        enabled = effectsState.enabled,
                        onClick = { onEffectTypeChanged(EffectType.RAINBOW) }
                    )

                    EffectButton(
                        text = "Gray",
                        selected = effectsState.activeEffect == EffectType.GRAYSCALE,
                        enabled = effectsState.enabled,
                        onClick = { onEffectTypeChanged(EffectType.GRAYSCALE) }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Intensity slider
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Intensity",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TrackerColors.OnSurface
                        )
                    )

                    Slider(
                        value = effectsState.intensity,
                        onValueChange = onIntensityChanged,
                        enabled = effectsState.enabled,
                        valueRange = 0f..1f,
                        steps = 10,
                        colors = SliderDefaults.colors(
                            thumbColor = TrackerColors.Primary,
                            activeTrackColor = TrackerColors.Primary,
                            inactiveTrackColor = TrackerColors.OnSurfaceVariant
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            // Right Column - Logo Effects
            LogoEffectsSection(
                logoEffectsService = logoEffectsService,
                modifier = Modifier.weight(1f)
            )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Theme Selection Section
            ThemeSelectionSection(
                themeService = themeService,
                modifier = Modifier.fillMaxWidth()
            )


        }

        // Status text at bottom
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            if (effectsState.enabled) {
                Text(
                    text = "Effects Active",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TrackerColors.Success
                    )
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Effects Disabled",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TrackerColors.Error
                        )
                    )

                    // Show error message if available
                    if (effectsState.errorMessage != null) {
                        Text(
                            text = effectsState.errorMessage,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TrackerColors.Error,
                                fontSize = 10.sp
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Button for selecting an effect type
 */
@Composable
private fun EffectButton(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) TrackerColors.Primary else TrackerColors.SurfaceOverlayLight,
            contentColor = if (selected) TrackerColors.OnPrimary else TrackerColors.OnSurface,
            disabledContainerColor = TrackerColors.SurfaceOverlayLight.copy(alpha = 0.5f),
            disabledContentColor = TrackerColors.OnSurfaceVariant
        ),
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 3.dp),
        modifier = Modifier.height(24.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun TileSizeButton(
    text: String,
    size: Int,
    logoEffectsService: com.supermetroid.service.LogoEffectsService,
    currentTileSize: Int
) {
    val isSelected = currentTileSize == size
    
    Button(
        onClick = { logoEffectsService.setTileSwapSize(size) },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) TrackerColors.Primary else TrackerColors.SurfaceOverlayLight,
            contentColor = if (isSelected) TrackerColors.OnPrimary else TrackerColors.OnSurface
        ),
        shape = RoundedCornerShape(4.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
        modifier = Modifier.height(20.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp)
        )
    }
}

/**
 * Logo effects section for the right column
 */
@Composable
private fun LogoEffectsSection(
    logoEffectsService: com.supermetroid.service.LogoEffectsService,
    modifier: Modifier = Modifier
) {
    val logoState by logoEffectsService.logoState.collectAsState()
    val currentTileSize by logoEffectsService.currentTileSize.collectAsState()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Text(
            text = "Logo Effects",
            style = MaterialTheme.typography.titleSmall.copy(
                color = TrackerColors.Primary,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // Logo effect buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            EffectButton(
                text = "None",
                selected = logoState.activeEffect == com.supermetroid.service.LogoEffectType.NONE,
                enabled = true,
                onClick = { 
                    logoEffectsService.setEffectType(com.supermetroid.service.LogoEffectType.NONE)
                    if (logoState.activeEffect != com.supermetroid.service.LogoEffectType.NONE) {
                        logoEffectsService.stop()
                    }
                }
            )

            EffectButton(
                text = "Noise",
                selected = logoState.activeEffect == com.supermetroid.service.LogoEffectType.NOISE,
                enabled = true,
                onClick = { 
                    logoEffectsService.setEffectType(com.supermetroid.service.LogoEffectType.NOISE)
                    if (!logoState.isRunning) {
                        logoEffectsService.start()
                    }
                }
            )
        }

        // Second row
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            EffectButton(
                text = "Swap",
                selected = logoState.activeEffect == com.supermetroid.service.LogoEffectType.PIXEL_SWAP,
                enabled = true,
                onClick = { 
                    logoEffectsService.setEffectType(com.supermetroid.service.LogoEffectType.PIXEL_SWAP)
                    if (!logoState.isRunning) {
                        logoEffectsService.start()
                    }
                }
            )

            // Wave effect button
            EffectButton(
                text = "Wave",
                selected = logoState.activeEffect == com.supermetroid.service.LogoEffectType.WAVE,
                enabled = true,
                onClick = { 
                    logoEffectsService.setEffectType(com.supermetroid.service.LogoEffectType.WAVE)
                    if (!logoState.isRunning) {
                        logoEffectsService.start()
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        
        // Tile size controls
        Text(
            text = "Tile Size",
            style = MaterialTheme.typography.bodySmall.copy(color = TrackerColors.OnSurface)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TileSizeButton(text = "5px", size = 5, logoEffectsService = logoEffectsService, currentTileSize = currentTileSize)
            TileSizeButton(text = "10px", size = 10, logoEffectsService = logoEffectsService, currentTileSize = currentTileSize)
            TileSizeButton(text = "30px", size = 30, logoEffectsService = logoEffectsService, currentTileSize = currentTileSize)
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Logo intensity slider
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Intensity",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TrackerColors.OnSurface
                )
            )

            Slider(
                value = logoState.intensity,
                onValueChange = { logoEffectsService.setIntensity(it) },
                enabled = true,
                valueRange = 0f..1f,
                steps = 20,
                colors = SliderDefaults.colors(
                    thumbColor = TrackerColors.Primary,
                    activeTrackColor = TrackerColors.Primary,
                    inactiveTrackColor = TrackerColors.OnSurfaceVariant
                ),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

/**
 * Theme selection section with dropdown
 */
@Composable
private fun ThemeSelectionSection(
    themeService: com.supermetroid.service.ThemeService,
    modifier: Modifier = Modifier
) {
    val currentTheme by themeService.currentTheme.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Text(
            text = "Theme",
            style = MaterialTheme.typography.titleSmall.copy(
                color = TrackerColors.Primary,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // Theme Dropdown
        Box {
            Button(
                onClick = { expanded = !expanded },
                colors = ButtonDefaults.buttonColors(
                    containerColor = TrackerColors.SurfaceOverlayLight,
                    contentColor = TrackerColors.OnSurface
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.fillMaxWidth(0.5f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentTheme.displayName,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = if (expanded) "▲" else "▼",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(TrackerColors.Surface)
            ) {
                com.supermetroid.service.AppTheme.values().forEach { theme ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Color preview circle with border showing theme colors
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .background(
                                            theme.colors.background,
                                            androidx.compose.foundation.shape.CircleShape
                                        )
                                        .border(
                                            2.dp,
                                            theme.colors.primary,
                                            androidx.compose.foundation.shape.CircleShape
                                        )
                                )
                                Text(
                                    text = theme.displayName,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (theme == currentTheme) TrackerColors.Primary else TrackerColors.OnSurface
                                    )
                                )
                            }
                        },
                        onClick = {
                            themeService.setTheme(theme)
                            expanded = false
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = TrackerColors.OnSurface
                        )
                    )
                }
            }
        }
    }
}

