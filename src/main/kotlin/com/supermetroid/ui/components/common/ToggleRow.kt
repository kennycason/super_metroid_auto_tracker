package com.supermetroid.ui.components.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.supermetroid.ui.theme.TrackerColors

/**
 * Reusable toggle row component for settings screens.
 * Displays a label on the left and a switch on the right.
 *
 * @param label The text label displayed on the left
 * @param checked Current checked state of the toggle
 * @param onCheckedChange Callback when toggle state changes
 * @param modifier Optional modifier
 * @param enabled Whether the toggle is enabled (default true)
 * @param description Optional description text below the label
 */
@Composable
fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    description: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .height(32.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (enabled) TrackerColors.OnSurface else TrackerColors.OnSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TrackerColors.OnSurfaceVariant
                    )
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TrackerColors.Primary,
                checkedTrackColor = TrackerColors.Primary.copy(alpha = 0.3f),
                uncheckedThumbColor = TrackerColors.OnSurfaceVariant,
                uncheckedTrackColor = TrackerColors.SurfaceVariant,
                disabledCheckedThumbColor = TrackerColors.Primary.copy(alpha = 0.5f),
                disabledCheckedTrackColor = TrackerColors.Primary.copy(alpha = 0.15f),
                disabledUncheckedThumbColor = TrackerColors.OnSurfaceVariant.copy(alpha = 0.5f),
                disabledUncheckedTrackColor = TrackerColors.SurfaceVariant.copy(alpha = 0.5f)
            )
        )
    }
}

/**
 * Highlighted toggle row for primary settings (like enabling major features).
 * Uses primary color for the label.
 */
@Composable
fun PrimaryToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier.height(36.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(
                color = TrackerColors.Primary,
                fontWeight = FontWeight.Bold
            )
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TrackerColors.Primary,
                checkedTrackColor = TrackerColors.Primary.copy(alpha = 0.3f),
                uncheckedThumbColor = TrackerColors.OnSurfaceVariant,
                uncheckedTrackColor = TrackerColors.SurfaceVariant
            )
        )
    }
}

