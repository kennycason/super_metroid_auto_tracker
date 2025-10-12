package com.supermetroid.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.supermetroid.autosplits.AutoSplitsEngine
import com.supermetroid.autosplits.KpdrAnyProfile
import com.supermetroid.service.GameStateService
import com.supermetroid.storage.FileStorageService
import com.supermetroid.ui.components.*
import com.supermetroid.ui.theme.TrackerColors
import com.supermetroid.ui.theme.TrackerTypography
import com.supermetroid.ui.theme.ProvideThemeService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing

// Global services
val fileStorageService = FileStorageService()
val gameStateService = GameStateService()
val autoSplitsEngine = AutoSplitsEngine(fileStorageService)
val themeService = com.supermetroid.service.ThemeService(fileStorageService)
val iconSizeService = com.supermetroid.service.IconSizeService(fileStorageService)
val splitIconSizeService = com.supermetroid.service.SplitIconSizeService(fileStorageService)
val splitDisplayModeService = com.supermetroid.service.SplitDisplayModeService(fileStorageService)
val iconConfigService = com.supermetroid.service.IconConfigService(fileStorageService)
val roomNameService = com.supermetroid.service.RoomNameService(fileStorageService)

fun main() = application {
    // Move showSplits state to Window level so keyboard shortcuts can access it
    var showSplits by remember { mutableStateOf(true) }
    
    Window(
        onCloseRequest = {
            gameStateService.stop()
            exitApplication()
        },
        title = "Super Metroid Tracker",
        state = androidx.compose.ui.window.rememberWindowState(
            width = 416.dp,  // Optimized for tall/skinny like image 2
            height = 1100.dp // Increased by 300dp for better splits visibility
        ),
        onKeyEvent = { keyEvent ->
            // Only process keyboard shortcuts when splits are visible
            if (showSplits) {
                when {
                    keyEvent.key == Key.Spacebar && keyEvent.type == KeyEventType.KeyDown -> {
                        // Spacebar to start/pause timer
                        println("[DEBUG_LOG] Spacebar key event detected in Window.onKeyEvent")
                        CoroutineScope(Dispatchers.Swing).launch {
                            println("[DEBUG_LOG] Executing toggleRunState from Spacebar key event on Swing dispatcher")
                            autoSplitsEngine.toggleRunState()
                        }
                        true
                    }

                    keyEvent.key == Key.R && keyEvent.type == KeyEventType.KeyDown -> {
                        // R key to reset run
                        println("[DEBUG_LOG] R key event detected")
                        CoroutineScope(Dispatchers.Swing).launch {
                            println("[DEBUG_LOG] Executing resetRun from R key event on Swing dispatcher")
                            autoSplitsEngine.resetRun()
                        }
                        true
                    }

                    else -> false
                }
            } else {
                // Splits are hidden - ignore all keyboard shortcuts
                println("[DEBUG_LOG] Ignoring keyboard shortcuts - splits are hidden")
                false
            }
        }
    ) {
        SuperMetroidTrackerApp(
            showSplits = showSplits,
            onShowSplitsChanged = { showSplits = it }
        )
    }
}

@Composable
@Preview
fun SuperMetroidTrackerApp(
    showSplits: Boolean = true,
    onShowSplitsChanged: (Boolean) -> Unit = {}
) {
    val trackerState by gameStateService.trackerState.collectAsState()
    val splitsState by autoSplitsEngine.splitsState.collectAsState()
    val currentTheme by themeService.currentTheme.collectAsState()
    
    // Track if services are initialized
    var servicesInitialized by remember { mutableStateOf(false) }

    // Initialize services
    LaunchedEffect(Unit) {
        // Initialize theme service first to load saved theme
        themeService.initialize()
        
        // Initialize icon size service
        iconSizeService.initialize()
        
        // Initialize split icon size service
        splitIconSizeService.initialize()
        
        // Initialize split display mode service
        splitDisplayModeService.initialize()
        
        // Initialize icon config service
        iconConfigService.initialize()
        
        // Initialize room name service
        roomNameService.initialize()
        
        // Mark services as initialized
        servicesInitialized = true
        
        // Load split profile
        autoSplitsEngine.loadProfile(KpdrAnyProfile.profile)

        // Load saved splits state and resume from current position
        val savedSplitsState = fileStorageService.loadSplitsState()
        autoSplitsEngine.loadSavedState(savedSplitsState)

        try {
            gameStateService.start()
        } catch (e: Exception) {
            // Connection will show as disconnected
        }
    }


    // Process game state for autosplits ONLY when splits are visible
    // This prevents auto-splitting when user is playing other games (like map rando)
    LaunchedEffect(trackerState.gameState, showSplits) {
        if (showSplits) {
            autoSplitsEngine.processGameState(trackerState.gameState)
        }
    }
    
    // TEMPORARY FIX: Always enable splits to ensure auto-start works
    LaunchedEffect(trackerState.gameState) {
        println("[DEBUG_LOG] Auto-splits processing: area=${trackerState.gameState.areaId}, room=${trackerState.gameState.roomId}, gameState=${trackerState.gameState.gameState}")
        autoSplitsEngine.processGameState(trackerState.gameState)
    }

    // Save splits state periodically (but not if it's empty - prevents overwriting loaded data)
    LaunchedEffect(splitsState) {
        // Don't save empty states - this prevents overwriting good data with empty state on startup
        if (splitsState.personalBests.isNotEmpty() || splitsState.runHistory.isNotEmpty() || splitsState.currentRun != null) {
            try {
                fileStorageService.saveSplitsState(splitsState)
            } catch (e: Exception) {
                // Handle save error gracefully
            }
        }
    }

    ProvideThemeService(themeService) {
        if (servicesInitialized) {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = currentTheme.colors.primary,
                    onPrimary = currentTheme.colors.onPrimary,
                    primaryContainer = currentTheme.colors.primaryVariant,
                    onPrimaryContainer = currentTheme.colors.onPrimary,
                    secondary = currentTheme.colors.primaryLight,
                    onSecondary = currentTheme.colors.onPrimary,
                    secondaryContainer = currentTheme.colors.surfaceVariant,
                    onSecondaryContainer = currentTheme.colors.onSurface,
                    tertiary = currentTheme.colors.success,
                    onTertiary = currentTheme.colors.onPrimary,
                    error = currentTheme.colors.error,
                    onError = currentTheme.colors.onPrimary,
                    background = currentTheme.colors.background,
                    onBackground = currentTheme.colors.onBackground,
                    surface = currentTheme.colors.surface,
                    onSurface = currentTheme.colors.onSurface,
                    surfaceVariant = currentTheme.colors.surfaceVariant,
                    onSurfaceVariant = currentTheme.colors.onSurfaceVariant,
                    outline = currentTheme.colors.border,
                    outlineVariant = currentTheme.colors.borderActive,
                    scrim = currentTheme.colors.background,
                    inverseSurface = currentTheme.colors.primary,
                    inverseOnSurface = currentTheme.colors.onPrimary,
                    inversePrimary = currentTheme.colors.background
                ),
                typography = TrackerTypography
            ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    currentTheme.colors.background,
                                    currentTheme.colors.backgroundVariant,
                                    currentTheme.colors.background
                                )
                            )
                        )
                ) {
                    SuperMetroidTrackerLayout(
                        trackerState = trackerState,
                        splitsState = splitsState,
                        showSplits = showSplits,
                        onShowSplitsChanged = onShowSplitsChanged,
                        themeService = themeService,
                        iconConfigService = iconConfigService,
                        roomNameService = roomNameService
                    )
                }
            }
            }
        }
    }
}

@Composable
fun SuperMetroidTrackerLayout(
    trackerState: com.supermetroid.model.TrackerState,
    splitsState: com.supermetroid.model.SplitsState,
    showSplits: Boolean,
    onShowSplitsChanged: (Boolean) -> Unit,
    themeService: com.supermetroid.service.ThemeService,
    iconConfigService: com.supermetroid.service.IconConfigService,
    roomNameService: com.supermetroid.service.RoomNameService
) {
    // UI visibility toggles (removed showSplits since it's now passed in)
    var showIcons by remember { mutableStateOf(true) }
    var showTimer by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp) // Minimal padding for very compact layout
    ) {
        // Header with connection status
        // TrackerHeader(
        //     connectionInfo = trackerState.connection,
        //     isFullscreen = false,
        //     onToggleFullscreen = {}
        // )

        // Spacer(modifier = Modifier.height(8.dp))


        // Timer section - Centered and compact
        if (showTimer) {
            Timer(
                splitsState = splitsState,
                onToggleRun = {
                    println("[DEBUG_LOG] Timer UI button clicked - onToggleRun callback")
                    CoroutineScope(Dispatchers.Swing).launch {
                        autoSplitsEngine.toggleRunState()
                    }
                },
                onResetRun = {
                    println("[DEBUG_LOG] Timer UI reset button clicked")
                    CoroutineScope(Dispatchers.Swing).launch {
                        autoSplitsEngine.resetRun()
                    }
                }
            )
            Spacer(modifier = Modifier.height(3.dp)) // Minimal spacing for compact layout
        }

        // Room Name Display - separate row
        RoomNameDisplay(
            gameState = trackerState.gameState,
            roomNameService = roomNameService,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(3.dp)) // Minimal spacing for compact layout

        // TALL LAYOUT: Status Grid at top, Timer below, then Splits at bottom
        // Status Grid (Icons) - Fixed height, non-stretchable
        if (showIcons) {
            SimpleStatusGrid(
                gameState = trackerState.gameState,
                iconConfigService = iconConfigService,
                iconSizeService = iconSizeService,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(3.dp)) // Minimal spacing for compact layout
        }

        // Splits list - Takes remaining space
        if (showSplits) {
            SplitsList(
                splitsState = splitsState,
                autoSplitsEngine = autoSplitsEngine,
                splitIconSizeService = splitIconSizeService,
                splitDisplayModeService = splitDisplayModeService,
                modifier = Modifier.fillMaxWidth().weight(1f),
                maxHeight = 900 // Increased height for taller window
            )
            
            // Personal Best section - appears below splits when splits are showing
            if (splitsState.personalBests.isNotEmpty()) {
                PersonalBestSummary(
                    splitsState = splitsState,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.height(3.dp)) // Minimal spacing for compact layout
        }

        // Settings panel - takes remaining space when visible
        if (showSettings) {
            SettingsPanel(
                themeService = themeService,
                iconSizeService = iconSizeService,
                splitIconSizeService = splitIconSizeService,
                splitDisplayModeService = splitDisplayModeService,
                iconConfigService = iconConfigService,
                roomNameService = roomNameService,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Takes all remaining vertical space
            )
            Spacer(modifier = Modifier.height(3.dp)) // Minimal spacing for compact layout
        }

        // Spacer to push footer to bottom when splits are hidden and settings not shown
        if (!showSplits && !showSettings) {
            Spacer(modifier = Modifier.weight(1f))
        }

        // UI Toggle footer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            // Connection status (left side)
            Text(
                text = if (trackerState.connection.connected) {
                    if (trackerState.connection.gameLoaded) "Connected" else "No Game"
                } else "Disconnected",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (trackerState.connection.connected) TrackerColors.Success else TrackerColors.Error
                )
            )

            // Toggle buttons (right side)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                // Icons toggle
                TextButton(
                    onClick = { showIcons = !showIcons },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (showIcons) TrackerColors.Success else TrackerColors.OnSurfaceVariant
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    modifier = Modifier.height(20.dp)
                ) {
                    Text(
                        text = "icons",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                // Splits toggle
                TextButton(
                    onClick = { onShowSplitsChanged(!showSplits) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (showSplits) TrackerColors.Success else TrackerColors.OnSurfaceVariant
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    modifier = Modifier.height(20.dp)
                ) {
                    Text(
                        text = "splits",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                // Timer toggle
                TextButton(
                    onClick = { showTimer = !showTimer },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (showTimer) TrackerColors.Success else TrackerColors.OnSurfaceVariant
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    modifier = Modifier.height(20.dp)
                ) {
                    Text(
                        text = "timer",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                // Settings toggle
                TextButton(
                    onClick = { showSettings = !showSettings },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (showSettings) TrackerColors.Success else TrackerColors.OnSurfaceVariant
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    modifier = Modifier.height(20.dp)
                ) {
                    Text(
                        text = "settings",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
