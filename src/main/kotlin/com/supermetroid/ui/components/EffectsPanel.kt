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
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = "Map Effects",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = TrackerColors.Primary,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Effect type selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                EffectButton(
                    text = "Psychedelic",
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
                    text = "Grayscale",
                    selected = effectsState.activeEffect == EffectType.GRAYSCALE,
                    enabled = effectsState.enabled,
                    onClick = { onEffectTypeChanged(EffectType.GRAYSCALE) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Intensity slider
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Intensity",
                    style = MaterialTheme.typography.bodyMedium.copy(
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
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Status text
            if (effectsState.enabled) {
                Text(
                    text = "Effects Active",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TrackerColors.Success
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 4.dp)
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
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier.height(32.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
