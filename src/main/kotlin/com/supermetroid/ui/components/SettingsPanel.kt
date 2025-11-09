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
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.supermetroid.model.IconItem
import com.supermetroid.model.IconSize
import com.supermetroid.ui.theme.TrackerColors

/**
 * Settings panel with tabbed organization
 */
@Composable
fun SettingsPanel(
    themeService: com.supermetroid.service.ThemeService,
    iconSizeService: com.supermetroid.service.IconSizeService,

    splitIconSizeService: com.supermetroid.service.SplitIconSizeService,
    splitDisplayModeService: com.supermetroid.service.SplitDisplayModeService,
    iconConfigService: com.supermetroid.service.IconConfigService,
    roomNameService: com.supermetroid.service.RoomNameService,
    autoSplitsEngine: com.supermetroid.autosplits.AutoSplitsEngine,
    iconViewModeService: com.supermetroid.service.IconViewModeService,
    soundService: com.supermetroid.service.SoundService,
    gameGenieService: com.supermetroid.service.GameGenieService,
    uiVisibilityService: com.supermetroid.service.UIVisibilityService,
    mapRandoInfoFontSizeService: com.supermetroid.service.MapRandoInfoFontSizeService,
    mapRandoInfoConfigService: com.supermetroid.service.MapRandoInfoConfigService,

    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("General", "Icons", "Splits", "SFX", "VFX")

    Card(
        modifier = modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(
            containerColor = TrackerColors.Surface
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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

            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = TrackerColors.SurfaceOverlayLight,
                contentColor = TrackerColors.Primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                ),
                                maxLines = 1
                            )
                        },
                        selectedContentColor = TrackerColors.Primary,
                        unselectedContentColor = TrackerColors.OnSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 0.dp) // No horizontal padding
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Content
            when (selectedTab) {
                0 -> GeneralSettingsTab(
                    themeService = themeService,
                    roomNameService = roomNameService,
                    autoSplitsEngine = autoSplitsEngine,
                    gameGenieService = gameGenieService,
                    modifier = Modifier.fillMaxSize()
                )

                1 -> IconsSettingsTab(
                    iconSizeService = iconSizeService,
                    iconConfigService = iconConfigService,
                    iconViewModeService = iconViewModeService,
                    uiVisibilityService = uiVisibilityService,
                    mapRandoInfoFontSizeService = mapRandoInfoFontSizeService,
                    mapRandoInfoConfigService = mapRandoInfoConfigService,
                    modifier = Modifier.fillMaxSize()
                )

                2 -> SplitsSettingsTab(
                    splitIconSizeService = splitIconSizeService,
                    splitDisplayModeService = splitDisplayModeService,
                    modifier = Modifier.fillMaxSize()
                )

                3 -> SFXSettingsTab(
                    soundService = soundService,
                    modifier = Modifier.fillMaxSize()
                )

                4 -> VFXSettingsTab(
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun GeneralSettingsTab(
    themeService: com.supermetroid.service.ThemeService,
    roomNameService: com.supermetroid.service.RoomNameService,
    autoSplitsEngine: com.supermetroid.autosplits.AutoSplitsEngine,
    gameGenieService: com.supermetroid.service.GameGenieService,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Theme Selection Section
        ThemeSelectionSection(
            themeService = themeService,
            modifier = Modifier.fillMaxWidth()
        )

        // Timer Set Section
        TimerSetSection(
            autoSplitsEngine = autoSplitsEngine,
            modifier = Modifier.fillMaxWidth()
        )

        // Room Name Toggle Section
        RoomNameToggleSection(
            roomNameService = roomNameService,
            modifier = Modifier.fillMaxWidth()
        )

        // Game Genie Toggle Section
        GameGenieToggleSection(
            gameGenieService = gameGenieService,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun GameGenieToggleSection(
    gameGenieService: com.supermetroid.service.GameGenieService,
    modifier: Modifier = Modifier
) {
    val gameGenieEnabled by gameGenieService.gameGenieEnabled.collectAsState()

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Enable Game Genie",
            style = MaterialTheme.typography.titleSmall.copy(color = TrackerColors.Primary, fontWeight = FontWeight.Bold)
        )

        Switch(
            checked = gameGenieEnabled,
            onCheckedChange = { enabled ->
                kotlinx.coroutines.GlobalScope.launch {
                    gameGenieService.setGameGenieEnabled(enabled)
                }
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = TrackerColors.Primary,
                checkedTrackColor = TrackerColors.Primary.copy(alpha = 0.3f),
                uncheckedThumbColor = TrackerColors.OnSurfaceVariant,
                uncheckedTrackColor = TrackerColors.SurfaceVariant
            )
        )
    }
}

@Composable
private fun IconsSettingsTab(
    iconSizeService: com.supermetroid.service.IconSizeService,
    iconConfigService: com.supermetroid.service.IconConfigService,
    iconViewModeService: com.supermetroid.service.IconViewModeService,
    uiVisibilityService: com.supermetroid.service.UIVisibilityService,
    mapRandoInfoFontSizeService: com.supermetroid.service.MapRandoInfoFontSizeService,
    mapRandoInfoConfigService: com.supermetroid.service.MapRandoInfoConfigService,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icon Size Selection Section
        IconSizeSelectionSection(
            iconSizeService = iconSizeService,
            modifier = Modifier.fillMaxWidth()
        )

        IconLayoutModeSection(
            iconViewModeService = iconViewModeService,
            modifier = Modifier.fillMaxWidth()
        )

        // Map Rando Info Settings - only show when Map Rando mode is selected
        val iconViewMode by iconViewModeService.iconViewMode.collectAsState()
        if (iconViewMode == com.supermetroid.model.IconViewMode.MAP_RANDO) {
            MapRandoInfoSettingsSection(
                uiVisibilityService = uiVisibilityService,
                mapRandoInfoFontSizeService = mapRandoInfoFontSizeService,
                mapRandoInfoConfigService = mapRandoInfoConfigService,
                modifier = Modifier.fillMaxWidth()
            )
        }

        IconAmmoDisplayModeSection(
            iconViewModeService = iconViewModeService,
            modifier = Modifier.fillMaxWidth()
        )

        // Icon Management Section - takes remaining space
        IconManagementSection(
            iconConfigService = iconConfigService,
            iconSizeService = iconSizeService,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}

@Composable
private fun IconLayoutModeSection(
    iconViewModeService: com.supermetroid.service.IconViewModeService,
    modifier: Modifier = Modifier
) {
    val currentMode by iconViewModeService.iconViewMode.collectAsState()
    var layoutExpanded by remember { mutableStateOf(false) }

    // Icon Layout row
    Row(
        modifier = modifier.fillMaxWidth(0.9f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Icon Layout",
            style = MaterialTheme.typography.bodySmall.copy(
                color = TrackerColors.OnSurface,
                fontWeight = FontWeight.Medium
            )
        )

        Box {
            Button(
                onClick = { layoutExpanded = !layoutExpanded },
                colors = ButtonDefaults.buttonColors(
                    containerColor = TrackerColors.SurfaceOverlayLight,
                    contentColor = TrackerColors.OnSurface
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = currentMode.displayName, style = MaterialTheme.typography.bodySmall)
                    Text(text = if (layoutExpanded) "▲" else "▼", style = MaterialTheme.typography.bodySmall)
                }
            }
            DropdownMenu(
                expanded = layoutExpanded,
                onDismissRequest = { layoutExpanded = false },
                modifier = Modifier.background(TrackerColors.Surface)
            ) {
                com.supermetroid.model.IconViewMode.values().forEach { mode ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                mode.displayName,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (mode == currentMode) TrackerColors.Primary else TrackerColors.OnSurface
                                )
                            )
                        },
                        onClick = {
                            kotlinx.coroutines.GlobalScope.launch { iconViewModeService.setIconViewMode(mode) }
                            layoutExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun IconAmmoDisplayModeSection(
    iconViewModeService: com.supermetroid.service.IconViewModeService,
    modifier: Modifier = Modifier
) {
    val ammoMode by iconViewModeService.ammoNumberMode.collectAsState()
    var ammoExpanded by remember { mutableStateOf(false) }

    // Ammo Numbers row
    Row(
        modifier = modifier.fillMaxWidth(0.9f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Ammo Numbers",
            style = MaterialTheme.typography.bodySmall.copy(
                color = TrackerColors.OnSurface,
                fontWeight = FontWeight.Medium
            )
        )

        Box {
            Button(
                onClick = { ammoExpanded = !ammoExpanded },
                colors = ButtonDefaults.buttonColors(
                    containerColor = TrackerColors.SurfaceOverlayLight,
                    contentColor = TrackerColors.OnSurface
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = ammoMode.displayName, style = MaterialTheme.typography.bodySmall)
                    Text(text = if (ammoExpanded) "▲" else "▼", style = MaterialTheme.typography.bodySmall)
                }
            }
            DropdownMenu(
                expanded = ammoExpanded,
                onDismissRequest = { ammoExpanded = false },
                modifier = Modifier.background(TrackerColors.Surface)
            ) {
                com.supermetroid.model.AmmoNumberMode.values().forEach { mode ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                mode.displayName,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (mode == ammoMode) TrackerColors.Primary else TrackerColors.OnSurface
                                )
                            )
                        },
                        onClick = {
                            kotlinx.coroutines.GlobalScope.launch { iconViewModeService.setAmmoNumberMode(mode) }
                            ammoExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SplitsSettingsTab(
    splitIconSizeService: com.supermetroid.service.SplitIconSizeService,
    splitDisplayModeService: com.supermetroid.service.SplitDisplayModeService,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Split Profile Section (placeholder for now)
        SplitProfileSection(
            modifier = Modifier.fillMaxWidth()
        )

        // Split Icon Size Selection Section
        SplitIconSizeSelectionSection(
            splitIconSizeService = splitIconSizeService,
            modifier = Modifier.fillMaxWidth()
        )

        // Split Display Mode Section
        SplitDisplayModeSection(
            splitDisplayModeService = splitDisplayModeService,
            modifier = Modifier.fillMaxWidth()
        )

        // Segment Deltas Toggle Section
        SegmentDeltasToggleSection(
            splitDisplayModeService = splitDisplayModeService,
            modifier = Modifier.fillMaxWidth()
        )

        BobColumnToggleSection(
            splitDisplayModeService = splitDisplayModeService,
            modifier = Modifier.fillMaxWidth()
        )

        BestColumnToggleSection(
            splitDisplayModeService = splitDisplayModeService,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SplitProfileSection(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(0.9f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Label on left
        Text(
            text = "Split Profile",
            style = MaterialTheme.typography.bodySmall.copy(
                color = TrackerColors.OnSurface,
                fontWeight = FontWeight.Medium
            )
        )

        // Profile Display (not a dropdown for now since only one profile exists)
        Card(
            colors = CardDefaults.cardColors(
                containerColor = TrackerColors.SurfaceOverlayLight
            ),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = "KPDR Any% (24 splits)",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TrackerColors.OnSurface
                ),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
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

    Row(
        modifier = modifier.fillMaxWidth(0.9f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Label on left
        Text(
            text = "Theme",
            style = MaterialTheme.typography.bodySmall.copy(
                color = TrackerColors.OnSurface,
                fontWeight = FontWeight.Medium
            )
        )

        // Theme Dropdown on right
        Box {
            Button(
                onClick = { expanded = !expanded },
                colors = ButtonDefaults.buttonColors(
                    containerColor = TrackerColors.SurfaceOverlayLight,
                    contentColor = TrackerColors.OnSurface
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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
private fun IconSizeSelectionSection(
    iconSizeService: com.supermetroid.service.IconSizeService,
    modifier: Modifier = Modifier
) {
    val currentIconSize by iconSizeService.currentIconSize.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Row(
        modifier = modifier.fillMaxWidth(0.9f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Label on left
        Text(
            text = "Icon Size",
            style = MaterialTheme.typography.bodySmall.copy(
                color = TrackerColors.OnSurface,
                fontWeight = FontWeight.Medium
            )
        )

        // Icon Size Dropdown on right
        Box {
            Button(
                onClick = { expanded = !expanded },
                colors = ButtonDefaults.buttonColors(
                    containerColor = TrackerColors.SurfaceOverlayLight,
                    contentColor = TrackerColors.OnSurface
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentIconSize.displayName,
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
                IconSize.values().forEach { iconSize ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = iconSize.displayName,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (iconSize == currentIconSize) TrackerColors.Primary else TrackerColors.OnSurface
                                )
                            )
                        },
                        onClick = {
                            scope.launch {
                                iconSizeService.setIconSize(iconSize)
                            }
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
private fun SplitIconSizeSelectionSection(
    splitIconSizeService: com.supermetroid.service.SplitIconSizeService,
    modifier: Modifier = Modifier
) {
    val currentSplitIconSize by splitIconSizeService.currentSplitIconSize.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Row(
        modifier = modifier.fillMaxWidth(0.9f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Label on left
        Text(
            text = "Split Icon Size",
            style = MaterialTheme.typography.bodySmall.copy(
                color = TrackerColors.OnSurface,
                fontWeight = FontWeight.Medium
            )
        )

        // Split Icon Size Dropdown on right
        Box {
            Button(
                onClick = { expanded = !expanded },
                colors = ButtonDefaults.buttonColors(
                    containerColor = TrackerColors.SurfaceOverlayLight,
                    contentColor = TrackerColors.OnSurface
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentSplitIconSize.displayName,
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
                IconSize.values().forEach { iconSize ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = iconSize.displayName,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (iconSize == currentSplitIconSize) TrackerColors.Primary else TrackerColors.OnSurface
                                )
                            )
                        },
                        onClick = {
                            scope.launch {
                                splitIconSizeService.setSplitIconSize(iconSize)
                            }
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
private fun SplitDisplayModeSection(
    splitDisplayModeService: com.supermetroid.service.SplitDisplayModeService,
    modifier: Modifier = Modifier
) {
    val currentDisplayMode by splitDisplayModeService.currentDisplayMode.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Row(
        modifier = modifier.fillMaxWidth(0.9f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Label on left
        Text(
            text = "Split Display Mode",
            style = MaterialTheme.typography.bodySmall.copy(
                color = TrackerColors.OnSurface,
                fontWeight = FontWeight.Medium
            )
        )

        // Split Display Mode Dropdown on right
        Box {
            Button(
                onClick = { expanded = !expanded },
                colors = ButtonDefaults.buttonColors(
                    containerColor = TrackerColors.SurfaceOverlayLight,
                    contentColor = TrackerColors.OnSurface
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentDisplayMode.displayName,
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
                com.supermetroid.model.SplitDisplayMode.values().forEach { mode ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = mode.displayName,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (mode == currentDisplayMode) TrackerColors.Primary else TrackerColors.OnSurface
                                )
                            )
                        },
                        onClick = {
                            scope.launch {
                                splitDisplayModeService.setDisplayMode(mode)
                            }
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
private fun RoomNameToggleSection(
    roomNameService: com.supermetroid.service.RoomNameService,
    modifier: Modifier = Modifier
) {
    val showRoomName by roomNameService.showRoomName.collectAsState()

    // Toggle switch
    Row(
        modifier = modifier.fillMaxWidth(0.9f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Show room names in status display",
            style = MaterialTheme.typography.bodySmall.copy(
                color = TrackerColors.OnSurface
            )
        )

        Switch(
            checked = showRoomName,
            onCheckedChange = { roomNameService.setShowRoomName(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = TrackerColors.Primary,
                checkedTrackColor = TrackerColors.Primary.copy(alpha = 0.5f),
                uncheckedThumbColor = TrackerColors.OnSurfaceVariant,
                uncheckedTrackColor = TrackerColors.SurfaceVariant
            )
        )
    }
}

@Composable
private fun SegmentDeltasToggleSection(
    splitDisplayModeService: com.supermetroid.service.SplitDisplayModeService,
    modifier: Modifier = Modifier
) {
    val showSegmentDeltas by splitDisplayModeService.showSegmentDeltas.collectAsState()
    val scope = rememberCoroutineScope()

    Row(
        modifier = modifier.fillMaxWidth(0.9f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Show Segment Deltas",
            style = MaterialTheme.typography.bodySmall.copy(
                color = TrackerColors.OnSurface,
                fontWeight = FontWeight.Medium
            )
        )

        Switch(
            checked = showSegmentDeltas,
            onCheckedChange = { enabled ->
                scope.launch {
                    splitDisplayModeService.setShowSegmentDeltas(enabled)
                }
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = TrackerColors.Primary,
                checkedTrackColor = TrackerColors.Primary.copy(alpha = 0.5f),
                uncheckedThumbColor = TrackerColors.OnSurfaceVariant,
                uncheckedTrackColor = TrackerColors.SurfaceVariant
            )
        )
    }
}

@Composable
private fun BobColumnToggleSection(
    splitDisplayModeService: com.supermetroid.service.SplitDisplayModeService,
    modifier: Modifier = Modifier
) {
    val showBobColumn by splitDisplayModeService.showBobColumn.collectAsState()
    val scope = rememberCoroutineScope()

    Row(
        modifier = modifier.fillMaxWidth(0.9f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Show BoB Column",
            style = MaterialTheme.typography.bodySmall.copy(
                color = TrackerColors.OnSurface,
                fontWeight = FontWeight.Medium
            )
        )

        Switch(
            checked = showBobColumn,
            onCheckedChange = { enabled ->
                scope.launch {
                    splitDisplayModeService.setShowBobColumn(enabled)
                }
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = TrackerColors.Primary,
                checkedTrackColor = TrackerColors.Primary.copy(alpha = 0.5f),
                uncheckedThumbColor = TrackerColors.OnSurfaceVariant,
                uncheckedTrackColor = TrackerColors.SurfaceVariant
            )
        )
    }
}

@Composable
private fun BestColumnToggleSection(
    splitDisplayModeService: com.supermetroid.service.SplitDisplayModeService,
    modifier: Modifier = Modifier
) {
    val showBestColumn by splitDisplayModeService.showBestColumn.collectAsState()
    val scope = rememberCoroutineScope()

    Row(
        modifier = modifier.fillMaxWidth(0.9f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Show BEST Column",
            style = MaterialTheme.typography.bodySmall.copy(
                color = TrackerColors.OnSurface,
                fontWeight = FontWeight.Medium
            )
        )

        Switch(
            checked = showBestColumn,
            onCheckedChange = { enabled ->
                scope.launch {
                    splitDisplayModeService.setShowBestColumn(enabled)
                }
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = TrackerColors.Primary,
                checkedTrackColor = TrackerColors.Primary.copy(alpha = 0.5f),
                uncheckedThumbColor = TrackerColors.OnSurfaceVariant,
                uncheckedTrackColor = TrackerColors.SurfaceVariant
            )
        )
    }
}

@Composable
private fun TimerSetSection(
    autoSplitsEngine: com.supermetroid.autosplits.AutoSplitsEngine,
    modifier: Modifier = Modifier
) {
    var hours by remember { mutableStateOf(0) }
    var minutes by remember { mutableStateOf(0) }
    var seconds by remember { mutableStateOf(0) }
    var centiseconds by remember { mutableStateOf(0) }

    var hoursExpanded by remember { mutableStateOf(false) }
    var minutesExpanded by remember { mutableStateOf(false) }
    var secondsExpanded by remember { mutableStateOf(false) }
    var centisecondsExpanded by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        // Text(
        //     text = "Set Timer",
        //     style = MaterialTheme.typography.titleSmall.copy(
        //         color = TrackerColors.Primary,
        //         fontWeight = FontWeight.Bold
        //     ),
        //     modifier = Modifier.padding(bottom = 6.dp)
        // )

        // Dropdowns row
        Row(
            modifier = Modifier.fillMaxWidth(0.9f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hours dropdown
            Box(modifier = Modifier.weight(1f)) {
                Button(
                    onClick = { hoursExpanded = !hoursExpanded },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TrackerColors.SurfaceOverlayLight,
                        contentColor = TrackerColors.OnSurface
                    ),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "%02d".format(hours),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "h",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TrackerColors.OnSurfaceVariant
                            )
                        )
                    }
                }

                DropdownMenu(
                    expanded = hoursExpanded,
                    onDismissRequest = { hoursExpanded = false },
                    modifier = Modifier.background(TrackerColors.Surface).heightIn(max = 200.dp)
                ) {
                    (0..100).forEach { h ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "%02d".format(h),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (h == hours) TrackerColors.Primary else TrackerColors.OnSurface
                                    )
                                )
                            },
                            onClick = {
                                hours = h
                                hoursExpanded = false
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = TrackerColors.OnSurface
                            )
                        )
                    }
                }
            }

            Text(":", style = MaterialTheme.typography.bodySmall.copy(color = TrackerColors.OnSurface))

            // Minutes dropdown
            Box(modifier = Modifier.weight(1f)) {
                Button(
                    onClick = { minutesExpanded = !minutesExpanded },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TrackerColors.SurfaceOverlayLight,
                        contentColor = TrackerColors.OnSurface
                    ),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "%02d".format(minutes),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "m",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TrackerColors.OnSurfaceVariant
                            )
                        )
                    }
                }

                DropdownMenu(
                    expanded = minutesExpanded,
                    onDismissRequest = { minutesExpanded = false },
                    modifier = Modifier.background(TrackerColors.Surface).heightIn(max = 200.dp)
                ) {
                    (0..59).forEach { m ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "%02d".format(m),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (m == minutes) TrackerColors.Primary else TrackerColors.OnSurface
                                    )
                                )
                            },
                            onClick = {
                                minutes = m
                                minutesExpanded = false
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = TrackerColors.OnSurface
                            )
                        )
                    }
                }
            }

            Text(":", style = MaterialTheme.typography.bodySmall.copy(color = TrackerColors.OnSurface))

            // Seconds dropdown
            Box(modifier = Modifier.weight(1f)) {
                Button(
                    onClick = { secondsExpanded = !secondsExpanded },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TrackerColors.SurfaceOverlayLight,
                        contentColor = TrackerColors.OnSurface
                    ),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "%02d".format(seconds),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "s",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TrackerColors.OnSurfaceVariant
                            )
                        )
                    }
                }

                DropdownMenu(
                    expanded = secondsExpanded,
                    onDismissRequest = { secondsExpanded = false },
                    modifier = Modifier.background(TrackerColors.Surface).heightIn(max = 200.dp)
                ) {
                    (0..59).forEach { s ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "%02d".format(s),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (s == seconds) TrackerColors.Primary else TrackerColors.OnSurface
                                    )
                                )
                            },
                            onClick = {
                                seconds = s
                                secondsExpanded = false
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = TrackerColors.OnSurface
                            )
                        )
                    }
                }
            }

            Text(".", style = MaterialTheme.typography.bodySmall.copy(color = TrackerColors.OnSurface))

            // Centiseconds dropdown
            Box(modifier = Modifier.weight(1f)) {
                Button(
                    onClick = { centisecondsExpanded = !centisecondsExpanded },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TrackerColors.SurfaceOverlayLight,
                        contentColor = TrackerColors.OnSurface
                    ),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "%02d".format(centiseconds),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "cs",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TrackerColors.OnSurfaceVariant
                            )
                        )
                    }
                }

                DropdownMenu(
                    expanded = centisecondsExpanded,
                    onDismissRequest = { centisecondsExpanded = false },
                    modifier = Modifier.background(TrackerColors.Surface).heightIn(max = 200.dp)
                ) {
                    (0..99).forEach { cs ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "%02d".format(cs),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (cs == centiseconds) TrackerColors.Primary else TrackerColors.OnSurface
                                    )
                                )
                            },
                            onClick = {
                                centiseconds = cs
                                centisecondsExpanded = false
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = TrackerColors.OnSurface
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Apply button
        Button(
            onClick = {
                scope.launch {
                    // Calculate total time in milliseconds
                    val totalMs = (hours * 3600L + minutes * 60L + seconds) * 1000L + centiseconds * 10L
                    autoSplitsEngine.setTimer(totalMs)
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = TrackerColors.Primary,
                contentColor = TrackerColors.OnPrimary
            ),
            shape = RoundedCornerShape(4.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            modifier = Modifier.fillMaxWidth(0.5f)
        ) {
            Text(
                text = "Set Timer",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun IconManagementSection(
    iconConfigService: com.supermetroid.service.IconConfigService,
    iconSizeService: com.supermetroid.service.IconSizeService,
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

        // Icon list with controls - now fills remaining space
        Card(
            colors = CardDefaults.cardColors(
                containerColor = TrackerColors.SurfaceOverlayLight
            ),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // Takes all remaining vertical space in the section
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
                        iconSizeService = iconSizeService,
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
    iconSizeService: com.supermetroid.service.IconSizeService,
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
        // Icon with click to toggle (fixed 32x32 size for settings consistency)
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

@Composable
private fun SFXSettingsTab(
    soundService: com.supermetroid.service.SoundService,
    modifier: Modifier = Modifier
) {
    val soundEnabled by soundService.soundEnabled.collectAsState()
    val volume by soundService.volume.collectAsState()

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Sound Effects",
            style = MaterialTheme.typography.headlineSmall.copy(
                color = TrackerColors.Primary,
                fontWeight = FontWeight.Bold
            )
        )

        Text(
            text = "Configure sound effects for item collection and boss defeats. Sound files should be placed in ~/.smtracker/sounds/ and configured in sounds.json",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = TrackerColors.OnSurfaceVariant
            )
        )

        // Sound Controls
        Card(
            colors = CardDefaults.cardColors(
                containerColor = TrackerColors.SurfaceOverlayLight
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Sound Controls",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = TrackerColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                )

                // Enable/Disable Sound Effects
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enable Sound Effects",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TrackerColors.OnSurface
                        )
                    )

                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = { enabled ->
                            CoroutineScope(Dispatchers.IO).launch {
                                soundService.setSoundEnabled(enabled)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TrackerColors.Primary,
                            checkedTrackColor = TrackerColors.Primary.copy(alpha = 0.3f),
                            uncheckedThumbColor = TrackerColors.OnSurfaceVariant,
                            uncheckedTrackColor = TrackerColors.OnSurfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                }

                // Volume Control
                if (soundEnabled) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Volume",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TrackerColors.OnSurface
                                )
                            )

                            Text(
                                text = "${(volume * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TrackerColors.Primary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Slider(
                            value = volume,
                            onValueChange = { newVolume ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    soundService.setVolume(newVolume)
                                }
                            },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = TrackerColors.Primary,
                                activeTrackColor = TrackerColors.Primary,
                                inactiveTrackColor = TrackerColors.OnSurfaceVariant.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }
        }

    }
}

@Composable
private fun MapRandoInfoSettingsSection(
    uiVisibilityService: com.supermetroid.service.UIVisibilityService,
    mapRandoInfoFontSizeService: com.supermetroid.service.MapRandoInfoFontSizeService,
    mapRandoInfoConfigService: com.supermetroid.service.MapRandoInfoConfigService,
    modifier: Modifier = Modifier
) {
    val showMapRandoInfo by uiVisibilityService.showMapRandoInfo.collectAsState()
    val fontSize by mapRandoInfoFontSizeService.fontSize.collectAsState()
    var fontSizeExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(0.9f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Show Map Rando Info Panel toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Show Info Panel",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TrackerColors.OnSurface,
                    fontWeight = FontWeight.Medium
                )
            )
            
            Switch(
                checked = showMapRandoInfo,
                onCheckedChange = { enabled ->
                    kotlinx.coroutines.GlobalScope.launch {
                        uiVisibilityService.setShowMapRandoInfo(enabled)
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TrackerColors.Primary,
                    checkedTrackColor = TrackerColors.Primary.copy(alpha = 0.3f),
                    uncheckedThumbColor = TrackerColors.OnSurfaceVariant,
                    uncheckedTrackColor = TrackerColors.SurfaceVariant
                )
            )
        }

        // Font Size selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Info Panel Font Size",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TrackerColors.OnSurface,
                    fontWeight = FontWeight.Medium
                )
            )

            Box {
                Button(
                    onClick = { fontSizeExpanded = !fontSizeExpanded },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TrackerColors.SurfaceOverlayLight,
                        contentColor = TrackerColors.OnSurface
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = fontSize.displayName, style = MaterialTheme.typography.bodySmall)
                        Text(text = if (fontSizeExpanded) "▲" else "▼", style = MaterialTheme.typography.bodySmall)
                    }
                }
                DropdownMenu(
                    expanded = fontSizeExpanded,
                    onDismissRequest = { fontSizeExpanded = false },
                    modifier = Modifier.background(TrackerColors.Surface)
                ) {
                    com.supermetroid.model.MapRandoInfoFontSize.values().forEach { size ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    size.displayName,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (size == fontSize) TrackerColors.Primary else TrackerColors.OnSurface
                                    )
                                )
                            },
                            onClick = {
                                kotlinx.coroutines.GlobalScope.launch { mapRandoInfoFontSizeService.setFontSize(size) }
                                fontSizeExpanded = false
                            }
                        )
                    }
                }
            }
        }
        
        // Info Item Management Section
        Divider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = TrackerColors.OnSurfaceVariant.copy(alpha = 0.2f)
        )
        
        MapRandoInfoManagementSection(
            mapRandoInfoConfigService = mapRandoInfoConfigService,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MapRandoInfoManagementSection(
    mapRandoInfoConfigService: com.supermetroid.service.MapRandoInfoConfigService,
    modifier: Modifier = Modifier
) {
    val config by mapRandoInfoConfigService.config.collectAsState()
    val allItems = config.items.sortedBy { it.order }
    
    Column(
        modifier = modifier.fillMaxWidth(0.9f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Info Items",
            style = MaterialTheme.typography.titleSmall.copy(
                color = TrackerColors.OnSurface,
                fontWeight = FontWeight.Bold
            )
        )
        
        // Scrollable list of info items
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            colors = CardDefaults.cardColors(
                containerColor = TrackerColors.SurfaceOverlayLight
            ),
            shape = RoundedCornerShape(6.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(allItems) { index, item ->
                    MapRandoInfoItemRow(
                        item = item,
                        index = index,
                        totalItems = allItems.size,
                        onToggleVisible = { mapRandoInfoConfigService.setItemVisible(item.id, !item.visible) },
                        onMoveUp = { if (index > 0) mapRandoInfoConfigService.moveItem(index, index - 1) },
                        onMoveDown = { if (index < allItems.size - 1) mapRandoInfoConfigService.moveItem(index, index + 1) }
                    )
                }
            }
        }
        
        // Reset button
        TextButton(
            onClick = { mapRandoInfoConfigService.resetToDefaults() },
            colors = ButtonDefaults.textButtonColors(
                contentColor = TrackerColors.OnSurfaceVariant
            )
        ) {
            Text("Reset to Default", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun MapRandoInfoItemRow(
    item: com.supermetroid.model.MapRandoInfoItem,
    index: Int,
    totalItems: Int,
    onToggleVisible: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (item.visible) TrackerColors.Surface else TrackerColors.SurfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Item name
        Text(
            text = item.displayName,
            style = MaterialTheme.typography.bodySmall.copy(
                color = if (item.visible) TrackerColors.OnSurface else TrackerColors.OnSurfaceVariant.copy(alpha = 0.5f),
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.weight(1f)
        )
        
        // Controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Move up button
            IconButton(
                onClick = onMoveUp,
                enabled = index > 0,
                modifier = Modifier.size(24.dp)
            ) {
                Text(
                    "▲",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (index > 0) TrackerColors.OnSurface else TrackerColors.OnSurfaceVariant.copy(alpha = 0.3f)
                    )
                )
            }
            
            // Move down button
            IconButton(
                onClick = onMoveDown,
                enabled = index < totalItems - 1,
                modifier = Modifier.size(24.dp)
            ) {
                Text(
                    "▼",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (index < totalItems - 1) TrackerColors.OnSurface else TrackerColors.OnSurfaceVariant.copy(alpha = 0.3f)
                    )
                )
            }
            
            // Toggle visibility
            Box(
                modifier = Modifier.scale(0.7f)
            ) {
                Switch(
                    checked = item.visible,
                    onCheckedChange = { onToggleVisible() },
                    colors = SwitchDefaults.colors(
                    checkedThumbColor = TrackerColors.Primary,
                    checkedTrackColor = TrackerColors.Primary.copy(alpha = 0.3f),
                    uncheckedThumbColor = TrackerColors.OnSurfaceVariant,
                    uncheckedTrackColor = TrackerColors.SurfaceVariant
                    )
                )
            }
        }
    }
}
