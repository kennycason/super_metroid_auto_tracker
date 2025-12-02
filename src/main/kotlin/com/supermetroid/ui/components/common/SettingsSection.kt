package com.supermetroid.ui.components.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.supermetroid.ui.theme.TrackerColors

/**
 * Reusable settings section container with consistent styling.
 * Use this to group related settings together.
 *
 * @param title Optional section title
 * @param modifier Optional modifier
 * @param spacing Vertical spacing between items (default 12.dp)
 * @param content The content composables for this section
 */
@Composable
fun SettingsSection(
    title: String? = null,
    modifier: Modifier = Modifier,
    spacing: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = TrackerColors.Primary,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(bottom = 4.dp)
            )
        }
        content()
    }
}

/**
 * Card-wrapped settings section with a border.
 * Use for visually distinct setting groups.
 */
@Composable
fun SettingsCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    spacing: Dp = 8.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(0.95f),
        colors = CardDefaults.cardColors(
            containerColor = TrackerColors.SurfaceOverlayLight
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = TrackerColors.Primary,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            content()
        }
    }
}

/**
 * Horizontal divider with consistent styling for separating settings.
 */
@Composable
fun SettingsDivider(
    modifier: Modifier = Modifier
) {
    Divider(
        modifier = modifier.fillMaxWidth(0.9f),
        color = TrackerColors.SurfaceVariant,
        thickness = 1.dp
    )
}

/**
 * A labeled row for displaying information (read-only, no interaction).
 */
@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(0.9f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = TrackerColors.OnSurfaceVariant
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                color = TrackerColors.OnSurface,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

