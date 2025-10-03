package com.supermetroid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.supermetroid.ui.theme.TrackerColors

/**
 * Settings panel with theme selection
 */
@Composable
fun SettingsPanel(
    themeService: com.supermetroid.service.ThemeService,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = TrackerColors.Surface
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // Title
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = TrackerColors.Primary,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Theme Selection Section
            ThemeSelectionSection(
                themeService = themeService,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

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
                modifier = Modifier.fillMaxWidth(0.8f)
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
                                            CircleShape
                                        )
                                        .border(
                                            2.dp,
                                            theme.colors.primary,
                                            CircleShape
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
