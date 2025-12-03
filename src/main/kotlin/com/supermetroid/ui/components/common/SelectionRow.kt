package com.supermetroid.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.supermetroid.ui.theme.TrackerColors

/**
 * Reusable dropdown selection row for settings screens.
 * Displays a label on the left and a dropdown button on the right.
 *
 * @param label The text label displayed on the left
 * @param selectedValue The currently selected value's display name
 * @param options List of available options (pairs of value to display name)
 * @param onOptionSelected Callback when an option is selected
 * @param modifier Optional modifier
 * @param enabled Whether the selection is enabled (default true)
 */
@Composable
fun <T> SelectionRow(
    label: String,
    selectedValue: String,
    options: List<Pair<T, String>>,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(0.9f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = if (enabled) TrackerColors.OnSurface else TrackerColors.OnSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        )

        Box {
            Button(
                onClick = { if (enabled) expanded = !expanded },
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TrackerColors.SurfaceOverlayLight,
                    contentColor = TrackerColors.OnSurface,
                    disabledContainerColor = TrackerColors.SurfaceOverlayLight.copy(alpha = 0.5f),
                    disabledContentColor = TrackerColors.OnSurfaceVariant
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = selectedValue, style = MaterialTheme.typography.bodySmall)
                    Text(text = if (expanded) "▲" else "▼", style = MaterialTheme.typography.bodySmall)
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(TrackerColors.Surface)
            ) {
                options.forEach { (value, displayName) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                displayName,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (displayName == selectedValue) TrackerColors.Primary else TrackerColors.OnSurface
                                )
                            )
                        },
                        onClick = {
                            onOptionSelected(value)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * Simplified selection row for enum values.
 * Automatically extracts display names from enum values with displayName property.
 */
@Composable
inline fun <reified T : Enum<T>> EnumSelectionRow(
    label: String,
    selectedValue: T,
    crossinline displayNameProvider: (T) -> String,
    crossinline onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val options = enumValues<T>().map { it to displayNameProvider(it) }
    
    SelectionRow(
        label = label,
        selectedValue = displayNameProvider(selectedValue),
        options = options,
        onOptionSelected = { onOptionSelected(it) },
        modifier = modifier,
        enabled = enabled
    )
}

