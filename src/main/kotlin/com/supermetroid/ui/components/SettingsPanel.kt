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
import com.supermetroid.ui.components.common.ToggleRow
import com.supermetroid.ui.components.common.PrimaryToggleRow
import com.supermetroid.ui.components.common.SelectionRow
import com.supermetroid.ui.theme.TrackerColors

/**
 * Settings panel with tabbed organization
 */
@Composable
fun SettingsPanel(
    themeService: com.supermetroid.service.ThemeService,
    iconSizeService: com.supermetroid.service.IconSizeService,
    fileStorageService: com.supermetroid.storage.FileStorageService,
    splitIconSizeService: com.supermetroid.service.SplitIconSizeService,
    splitDisplayModeService: com.supermetroid.service.SplitDisplayModeService,
    splitProfileService: com.supermetroid.service.SplitProfileService,
    splitFormatService: com.supermetroid.service.SplitFormatService,
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
                    fileStorageService = fileStorageService,
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
                    splitProfileService = splitProfileService,
                    splitFormatService = splitFormatService,
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
    fileStorageService: com.supermetroid.storage.FileStorageService,
    gameGenieService: com.supermetroid.service.GameGenieService,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Load Run Section (for reviewing historical runs)
        LoadRunSection(
            fileStorageService = fileStorageService,
            autoSplitsEngine = autoSplitsEngine,
            modifier = Modifier.fillMaxWidth()
        )

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

    PrimaryToggleRow(
        label = "Enable Game Genie",
        checked = gameGenieEnabled,
        onCheckedChange = { kotlinx.coroutines.GlobalScope.launch { gameGenieService.setGameGenieEnabled(it) } },
        modifier = modifier
    )
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
    
    SelectionRow(
        label = "Icon Layout",
        selectedValue = currentMode.displayName,
        options = com.supermetroid.model.IconViewMode.values().map { it to it.displayName },
        onOptionSelected = { kotlinx.coroutines.GlobalScope.launch { iconViewModeService.setIconViewMode(it) } },
        modifier = modifier
    )
}

@Composable
private fun IconAmmoDisplayModeSection(
    iconViewModeService: com.supermetroid.service.IconViewModeService,
    modifier: Modifier = Modifier
) {
    val ammoMode by iconViewModeService.ammoNumberMode.collectAsState()

    SelectionRow(
        label = "Ammo Numbers",
        selectedValue = ammoMode.displayName,
        options = com.supermetroid.model.AmmoNumberMode.values().map { it to it.displayName },
        onOptionSelected = { kotlinx.coroutines.GlobalScope.launch { iconViewModeService.setAmmoNumberMode(it) } },
        modifier = modifier
    )
}

@Composable
private fun SplitsSettingsTab(
    splitProfileService: com.supermetroid.service.SplitProfileService,
    splitFormatService: com.supermetroid.service.SplitFormatService,
    splitIconSizeService: com.supermetroid.service.SplitIconSizeService,
    splitDisplayModeService: com.supermetroid.service.SplitDisplayModeService,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Split Profile Section
        SplitProfileSection(
            splitProfileService = splitProfileService,
            modifier = Modifier.fillMaxWidth()
        )

        // Split Format Section (Read/Write format + LSS file picker)
        SplitFormatSection(
            splitFormatService = splitFormatService,
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

        BestPossibleColumnToggleSection(
            splitDisplayModeService = splitDisplayModeService,
            modifier = Modifier.fillMaxWidth()
        )

        BestPossibleDeltaToggleSection(
            splitDisplayModeService = splitDisplayModeService,
            modifier = Modifier.fillMaxWidth()
        )

        BestColumnToggleSection(
            splitDisplayModeService = splitDisplayModeService,
            modifier = Modifier.fillMaxWidth()
        )

        AverageColumnToggleSection(
            splitDisplayModeService = splitDisplayModeService,
            modifier = Modifier.fillMaxWidth()
        )

        AverageDeltaToggleSection(
            splitDisplayModeService = splitDisplayModeService,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SplitProfileSection(
    splitProfileService: com.supermetroid.service.SplitProfileService,
    modifier: Modifier = Modifier
) {
    val currentProfile by splitProfileService.currentProfile.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
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

        // Profile Dropdown
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
                        text = "${currentProfile.name} (${currentProfile.splits.size} splits)",
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
                com.supermetroid.autosplits.SplitProfiles.ALL_PROFILES.forEach { profile ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "${profile.name} (${profile.splits.size} splits)",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (profile.id == currentProfile.id) TrackerColors.Primary else TrackerColors.OnSurface
                                )
                            )
                        },
                        onClick = {
                            scope.launch { splitProfileService.setProfile(profile) }
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
private fun SplitFormatSection(
    splitFormatService: com.supermetroid.service.SplitFormatService,
    modifier: Modifier = Modifier
) {
    val readFormat by splitFormatService.readFormat.collectAsState()
    val writeJson by splitFormatService.writeJson.collectAsState()
    val writeLiveSplit by splitFormatService.writeLiveSplit.collectAsState()
    val lssFilePath by splitFormatService.liveSplitFilePath.collectAsState()
    val lssDoc by splitFormatService.liveSplitDocument.collectAsState()
    val scope = rememberCoroutineScope()

    Card(
        modifier = modifier.fillMaxWidth(0.95f),
        colors = CardDefaults.cardColors(
            containerColor = TrackerColors.SurfaceVariant
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Split Format",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TrackerColors.Primary,
                    fontWeight = FontWeight.Bold
                )
            )

            // Read Format toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Read Format",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TrackerColors.OnSurface,
                        fontWeight = FontWeight.Medium
                    )
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    com.supermetroid.service.SplitFormatService.ReadFormat.values().forEach { format ->
                        val isSelected = readFormat == format
                        Button(
                            onClick = {
                                scope.launch { splitFormatService.setReadFormat(format) }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) TrackerColors.Primary else TrackerColors.SurfaceOverlayLight,
                                contentColor = if (isSelected) TrackerColors.OnPrimary else TrackerColors.OnSurface
                            ),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(
                                text = format.displayName,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            // Write Format checkboxes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Write Format",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TrackerColors.OnSurface,
                        fontWeight = FontWeight.Medium
                    )
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Checkbox(
                            checked = writeJson,
                            onCheckedChange = { scope.launch { splitFormatService.setWriteJson(it) } },
                            modifier = Modifier.size(20.dp),
                            colors = CheckboxDefaults.colors(
                                checkedColor = TrackerColors.Primary,
                                uncheckedColor = TrackerColors.OnSurfaceVariant
                            )
                        )
                        Text("JSON", style = MaterialTheme.typography.labelSmall.copy(color = TrackerColors.OnSurface))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Checkbox(
                            checked = writeLiveSplit,
                            onCheckedChange = { scope.launch { splitFormatService.setWriteLiveSplit(it) } },
                            modifier = Modifier.size(20.dp),
                            colors = CheckboxDefaults.colors(
                                checkedColor = TrackerColors.Primary,
                                uncheckedColor = TrackerColors.OnSurfaceVariant
                            )
                        )
                        Text("LiveSplit", style = MaterialTheme.typography.labelSmall.copy(color = TrackerColors.OnSurface))
                    }
                }
            }

            // LiveSplit file picker
            if (readFormat == com.supermetroid.service.SplitFormatService.ReadFormat.LIVESPLIT || writeLiveSplit) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "LiveSplit File (.lss)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TrackerColors.OnSurface,
                            fontWeight = FontWeight.Medium
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // File path display
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val path = showFilePickerDialog()
                                    if (path != null) {
                                        splitFormatService.setLiveSplitFilePath(path)
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = TrackerColors.OnSurface
                            )
                        ) {
                            Text(
                                text = lssFilePath?.let { java.io.File(it).name } ?: "Select .lss file...",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Reload button (only if file is loaded)
                        if (lssFilePath != null) {
                            TextButton(
                                onClick = { splitFormatService.reloadLiveSplitFile() },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text(
                                    "↻",
                                    style = MaterialTheme.typography.bodyLarge.copy(color = TrackerColors.Primary)
                                )
                            }
                        }
                    }

                    // LSS file info (when loaded)
                    if (lssDoc != null) {
                        val doc = lssDoc!!
                        val completedAttempts = doc.attemptHistory.count { it.realTime != null }
                        Text(
                            text = "${doc.gameName} - ${doc.categoryName} | ${doc.segments.size} splits | $completedAttempts/${doc.attemptHistory.size} attempts",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TrackerColors.OnSurfaceVariant
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Opens a native file picker dialog for .lss files.
 * Uses AWT FileDialog for cross-platform support in Compose Desktop.
 */
private fun showFilePickerDialog(): String? {
    val dialog = java.awt.FileDialog(null as java.awt.Frame?, "Select LiveSplit File", java.awt.FileDialog.LOAD)
    dialog.setFilenameFilter { _, name -> name.endsWith(".lss", ignoreCase = true) }
    dialog.isVisible = true

    val dir = dialog.directory
    val file = dialog.file
    return if (dir != null && file != null) {
        java.io.File(dir, file).absolutePath
    } else {
        null
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
    val scope = rememberCoroutineScope()

    SelectionRow(
        label = "Icon Size",
        selectedValue = currentIconSize.displayName,
        options = IconSize.values().map { it to it.displayName },
        onOptionSelected = { scope.launch { iconSizeService.setIconSize(it) } },
        modifier = modifier
    )
}

@Composable
private fun SplitIconSizeSelectionSection(
    splitIconSizeService: com.supermetroid.service.SplitIconSizeService,
    modifier: Modifier = Modifier
) {
    val currentSplitIconSize by splitIconSizeService.currentSplitIconSize.collectAsState()
    val scope = rememberCoroutineScope()

    SelectionRow(
        label = "Split Icon Size",
        selectedValue = currentSplitIconSize.displayName,
        options = IconSize.values().map { it to it.displayName },
        onOptionSelected = { scope.launch { splitIconSizeService.setSplitIconSize(it) } },
        modifier = modifier
    )
}

@Composable
private fun SplitDisplayModeSection(
    splitDisplayModeService: com.supermetroid.service.SplitDisplayModeService,
    modifier: Modifier = Modifier
) {
    val currentDisplayMode by splitDisplayModeService.currentDisplayMode.collectAsState()
    val scope = rememberCoroutineScope()

    SelectionRow(
        label = "Split Display Mode",
        selectedValue = currentDisplayMode.displayName,
        options = com.supermetroid.model.SplitDisplayMode.values().map { it to it.displayName },
        onOptionSelected = { scope.launch { splitDisplayModeService.setDisplayMode(it) } },
        modifier = modifier
    )
}

@Composable
private fun RoomNameToggleSection(
    roomNameService: com.supermetroid.service.RoomNameService,
    modifier: Modifier = Modifier
) {
    val showRoomName by roomNameService.showRoomName.collectAsState()
    
    ToggleRow(
        label = "Show room names in status display",
        checked = showRoomName,
        onCheckedChange = { roomNameService.setShowRoomName(it) },
        modifier = modifier
    )
}

@Composable
private fun SegmentDeltasToggleSection(
    splitDisplayModeService: com.supermetroid.service.SplitDisplayModeService,
    modifier: Modifier = Modifier
) {
    val showSegmentDeltas by splitDisplayModeService.showSegmentDeltas.collectAsState()
    val scope = rememberCoroutineScope()

    ToggleRow(
        label = "Show Segment Deltas",
        checked = showSegmentDeltas,
        onCheckedChange = { scope.launch { splitDisplayModeService.setShowSegmentDeltas(it) } },
        modifier = modifier
    )
}

@Composable
private fun BestPossibleColumnToggleSection(
    splitDisplayModeService: com.supermetroid.service.SplitDisplayModeService,
    modifier: Modifier = Modifier
) {
    val showBestPossibleColumn by splitDisplayModeService.showBestPossibleColumn.collectAsState()
    val scope = rememberCoroutineScope()

    ToggleRow(
        label = "Show Best Possible Column",
        checked = showBestPossibleColumn,
        onCheckedChange = { scope.launch { splitDisplayModeService.setShowBestPossibleColumn(it) } },
        modifier = modifier
    )
}

@Composable
private fun BestPossibleDeltaToggleSection(
    splitDisplayModeService: com.supermetroid.service.SplitDisplayModeService,
    modifier: Modifier = Modifier
) {
    val showBestPossibleDelta by splitDisplayModeService.showBestPossibleDelta.collectAsState()
    val scope = rememberCoroutineScope()

    ToggleRow(
        label = "Show Best Possible Δ Column",
        checked = showBestPossibleDelta,
        onCheckedChange = { scope.launch { splitDisplayModeService.setShowBestPossibleDelta(it) } },
        modifier = modifier
    )
}

@Composable
private fun BestColumnToggleSection(
    splitDisplayModeService: com.supermetroid.service.SplitDisplayModeService,
    modifier: Modifier = Modifier
) {
    val showBestColumn by splitDisplayModeService.showBestColumn.collectAsState()
    val scope = rememberCoroutineScope()

    ToggleRow(
        label = "Show BEST Column",
        checked = showBestColumn,
        onCheckedChange = { scope.launch { splitDisplayModeService.setShowBestColumn(it) } },
        modifier = modifier
    )
}

@Composable
private fun AverageColumnToggleSection(
    splitDisplayModeService: com.supermetroid.service.SplitDisplayModeService,
    modifier: Modifier = Modifier
) {
    val showAverageColumn by splitDisplayModeService.showAverageColumn.collectAsState()
    val scope = rememberCoroutineScope()

    ToggleRow(
        label = "Show Average Column",
        checked = showAverageColumn,
        onCheckedChange = { scope.launch { splitDisplayModeService.setShowAverageColumn(it) } },
        modifier = modifier
    )
}

@Composable
private fun AverageDeltaToggleSection(
    splitDisplayModeService: com.supermetroid.service.SplitDisplayModeService,
    modifier: Modifier = Modifier
) {
    val showAverageDelta by splitDisplayModeService.showAverageDelta.collectAsState()
    val scope = rememberCoroutineScope()

    ToggleRow(
        label = "Show Average Δ Column",
        checked = showAverageDelta,
        onCheckedChange = { scope.launch { splitDisplayModeService.setShowAverageDelta(it) } },
        modifier = modifier
    )
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

    Column(
        modifier = modifier.fillMaxWidth(0.9f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ToggleRow(
            label = "Show Info Panel",
            checked = showMapRandoInfo,
            onCheckedChange = { kotlinx.coroutines.GlobalScope.launch { uiVisibilityService.setShowMapRandoInfo(it) } },
            modifier = Modifier.fillMaxWidth()
        )

        SelectionRow(
            label = "Info Panel Font Size",
            selectedValue = fontSize.displayName,
            options = com.supermetroid.model.MapRandoInfoFontSize.values().map { it to it.displayName },
            onOptionSelected = { kotlinx.coroutines.GlobalScope.launch { mapRandoInfoFontSizeService.setFontSize(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        
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

/**
 * Load Run Section - Allows loading historical runs for review
 */
@Composable
private fun LoadRunSection(
    fileStorageService: com.supermetroid.storage.FileStorageService,
    autoSplitsEngine: com.supermetroid.autosplits.AutoSplitsEngine,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var runFiles by remember { mutableStateOf<List<com.supermetroid.storage.FileStorageService.RunFileMetadata>>(emptyList()) }
    var selectedRun by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var confirmDeleteFileName by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // Load run files when dropdown is opened
    LaunchedEffect(expanded) {
        if (expanded && runFiles.isEmpty()) {
            isLoading = true
            runFiles = fileStorageService.listRunFiles()
            isLoading = false
        }
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = TrackerColors.SurfaceVariant
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = "Run History",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TrackerColors.OnSurface,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Review or delete past runs",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TrackerColors.OnSurfaceVariant
                ),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Status message
            if (statusMessage.isNotBlank()) {
                Text(
                    statusMessage,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (statusMessage.startsWith("Deleted")) TrackerColors.Success else TrackerColors.OnSurfaceVariant
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Dropdown button
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TrackerColors.OnSurface
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedRun ?: "Select a run...",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (expanded) "Collapse" else "Expand"
                        )
                    }
                }

                // Dropdown menu
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .heightIn(max = 400.dp)
                ) {
                    if (isLoading) {
                        DropdownMenuItem(
                            text = { Text("Loading runs...") },
                            onClick = { }
                        )
                    } else if (runFiles.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No runs found") },
                            onClick = { }
                        )
                    } else {
                        // "Reset to Current" option - always at top
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Reset to Current (Show True PB)",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TrackerColors.Primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            },
                            onClick = {
                                selectedRun = null
                                expanded = false
                                scope.launch {
                                    autoSplitsEngine.resetToCurrentState()
                                }
                            }
                        )

                        Divider(color = TrackerColors.OnSurfaceVariant.copy(alpha = 0.3f))

                        // Historical runs
                        runFiles.forEach { runFile ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = runFile.displayName,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        // Delete button
                                        IconButton(
                                            onClick = {
                                                confirmDeleteFileName = runFile.fileName
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Text(
                                                "X",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = TrackerColors.Error,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedRun = runFile.displayName
                                    expanded = false

                                    // Load the run
                                    scope.launch {
                                        try {
                                            autoSplitsEngine.loadReplayRun(runFile.fileName)
                                        } catch (e: Exception) {
                                            // Log error (handled in engine)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Refresh button
            if (runFiles.isNotEmpty()) {
                TextButton(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            runFiles = fileStorageService.listRunFiles()
                            isLoading = false
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp)
                ) {
                    Text(
                        text = "Refresh",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TrackerColors.Primary
                        )
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (confirmDeleteFileName != null) {
        val fileName = confirmDeleteFileName!!
        val runMeta = runFiles.find { it.fileName == fileName }
        AlertDialog(
            onDismissRequest = { confirmDeleteFileName = null },
            title = {
                Text(
                    "Delete Run?",
                    style = MaterialTheme.typography.titleSmall.copy(color = TrackerColors.OnSurface)
                )
            },
            text = {
                Text(
                    "Delete ${runMeta?.displayName ?: fileName}?\n\nA backup will be saved to ~/.smtracker/backups/",
                    style = MaterialTheme.typography.bodySmall.copy(color = TrackerColors.OnSurfaceVariant)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDeleteFileName = null
                        scope.launch {
                            val success = fileStorageService.deleteRun(fileName)
                            if (success) {
                                statusMessage = "Deleted (backup saved)"
                                // Refresh the list
                                runFiles = fileStorageService.listRunFiles()
                                if (selectedRun == runMeta?.displayName) {
                                    selectedRun = null
                                    autoSplitsEngine.resetToCurrentState()
                                }
                            } else {
                                statusMessage = "Failed to delete"
                            }
                        }
                    }
                ) {
                    Text("Delete", color = TrackerColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteFileName = null }) {
                    Text("Cancel", color = TrackerColors.OnSurfaceVariant)
                }
            },
            containerColor = TrackerColors.Surface
        )
    }
}
