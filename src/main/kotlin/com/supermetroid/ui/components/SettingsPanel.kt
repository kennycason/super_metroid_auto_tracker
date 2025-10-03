package com.supermetroid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.supermetroid.model.IconItem
import com.supermetroid.ui.theme.TrackerColors

/**
 * Settings panel with theme selection
 */
@Composable
fun SettingsPanel(
    themeService: com.supermetroid.service.ThemeService,
    iconConfigService: com.supermetroid.service.IconConfigService,
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Icon Management Section
            IconManagementSection(
                iconConfigService = iconConfigService,
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

@Composable
private fun IconManagementSection(
    iconConfigService: com.supermetroid.service.IconConfigService,
    modifier: Modifier = Modifier
) {
    val iconConfig by iconConfigService.iconConfig.collectAsState()
    val allIcons = iconConfig.icons.sortedBy { it.order }
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Text(
            text = "Icon Management",
            style = MaterialTheme.typography.titleSmall.copy(
                color = TrackerColors.Primary,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Instructions
        Text(
            text = "Click icons to toggle visibility • Use arrows to reorder",
            style = MaterialTheme.typography.bodySmall.copy(
                color = TrackerColors.OnSurfaceVariant
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Icon list with controls
        Card(
            colors = CardDefaults.cardColors(
                containerColor = TrackerColors.SurfaceOverlayLight
            ),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp) // Fixed height for scrollable list
        ) {
            LazyColumn(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(allIcons) { index, icon ->
                    IconManagementItem(
                        icon = icon,
                        index = index,
                        totalItems = allIcons.size,
                        onToggleEnabled = { iconConfigService.setIconEnabled(icon.id, !icon.enabled) },
                        onMoveUp = { if (index > 0) iconConfigService.moveIcon(index, index - 1) },
                        onMoveDown = { if (index < allIcons.size - 1) iconConfigService.moveIcon(index, index + 1) }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Reset button
        TextButton(
            onClick = { iconConfigService.resetToDefault() },
            colors = ButtonDefaults.textButtonColors(
                contentColor = TrackerColors.OnSurfaceVariant
            )
        ) {
            Text(
                text = "Reset to Default",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun IconManagementItem(
    icon: IconItem,
    index: Int,
    totalItems: Int,
    onToggleEnabled: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Icon with click to toggle
        Box(
            modifier = Modifier
                .size(32.dp)
                .clickable { onToggleEnabled() },
            contentAlignment = Alignment.Center
        ) {
            SpriteIcon(
                itemId = icon.id,
                isObtained = icon.enabled, // Use enabled state for grayscale/color
                size = 32
            )
        }
        
        // Icon name and category
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = icon.name,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (icon.enabled) TrackerColors.OnSurface else TrackerColors.OnSurfaceVariant,
                    fontWeight = if (icon.enabled) FontWeight.Medium else FontWeight.Normal
                )
            )
            Text(
                text = icon.category.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TrackerColors.OnSurfaceVariant.copy(alpha = 0.7f)
                )
            )
        }
        
        // Move up button
        IconButton(
            onClick = onMoveUp,
            enabled = index > 0,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                Icons.Default.KeyboardArrowUp,
                contentDescription = "Move Up",
                tint = if (index > 0) TrackerColors.OnSurface else TrackerColors.OnSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(16.dp)
            )
        }
        
        // Move down button
        IconButton(
            onClick = onMoveDown,
            enabled = index < totalItems - 1,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Move Down",
                tint = if (index < totalItems - 1) TrackerColors.OnSurface else TrackerColors.OnSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
