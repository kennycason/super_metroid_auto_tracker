package com.supermetroid.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermetroid.ui.theme.TrackerColors

data class VisibilitySettings(
    val showIcons: Boolean = true,
    val showSplits: Boolean = true,
    val showTimer: Boolean = true,
    val showDebug: Boolean = false
)

@Composable
fun VisibilityControls(
    settings: VisibilitySettings,
    onSettingsChanged: (VisibilitySettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = TrackerColors.SurfaceOverlayLight
        ),
        shape = RoundedCornerShape(3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    TrackerColors.Border,
                    RoundedCornerShape(3.dp)
                )
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SHOW:",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = TrackerColors.Primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            )
            
            TrackerCheckbox(
                label = "Icons",
                checked = settings.showIcons,
                onCheckedChange = { onSettingsChanged(settings.copy(showIcons = it)) }
            )
            
            TrackerCheckbox(
                label = "Splits",
                checked = settings.showSplits,
                onCheckedChange = { onSettingsChanged(settings.copy(showSplits = it)) }
            )
            
            TrackerCheckbox(
                label = "Timer",
                checked = settings.showTimer,
                onCheckedChange = { onSettingsChanged(settings.copy(showTimer = it)) }
            )
            
            TrackerCheckbox(
                label = "Debug",
                checked = settings.showDebug,
                onCheckedChange = { onSettingsChanged(settings.copy(showDebug = it)) }
            )
        }
    }
}

@Composable
private fun TrackerCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .padding(horizontal = 4.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = TrackerColors.Primary,
                uncheckedColor = TrackerColors.Border,
                checkmarkColor = TrackerColors.Background
            ),
            modifier = Modifier.size(14.dp)
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = TrackerColors.Primary,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        )
    }
}
