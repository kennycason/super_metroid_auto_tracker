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
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing

private val logger = KotlinLogging.logger {}

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
val iconViewModeService = com.supermetroid.service.IconViewModeService(fileStorageService)
val uiVisibilityService = com.supermetroid.service.UIVisibilityService(fileStorageService)
val soundService = com.supermetroid.service.SoundService(fileStorageService)

fun main() = application {
    // Load saved window dimensions
    val config = runBlocking { fileStorageService.loadAppConfig() }
    val windowState = rememberWindowState(
        width = config.windowWidth.dp,
        height = config.windowHeight.dp
    )
    
    Window(
        onCloseRequest = {
            // Save window dimensions before closing
            runBlocking {
                val currentConfig = fileStorageService.loadAppConfig()
                fileStorageService.saveAppConfig(
                    currentConfig.copy(
                        windowWidth = windowState.size.width.value.toInt(),
                        windowHeight = windowState.size.height.value.toInt()
                    )
                )
            }
            gameStateService.stop()
            exitApplication()
        },
        title = "Super Metroid Tracker",
        state = windowState,
        onKeyEvent = { keyEvent ->
            // Only process keyboard shortcuts when splits are visible
            if (uiVisibilityService.showSplits.value) {
                when {
                    keyEvent.key == Key.Spacebar && keyEvent.type == KeyEventType.KeyDown -> {
                        // Spacebar to start/pause timer
                        logger.debug { "⌨️ Spacebar key event detected in Window.onKeyEvent" }
                        CoroutineScope(Dispatchers.Swing).launch {
                            logger.debug { "⌨️ Executing toggleRunState from Spacebar key event on Swing dispatcher" }
                            autoSplitsEngine.toggleRunState()
                        }
                        true
                    }

                    keyEvent.key == Key.R && keyEvent.type == KeyEventType.KeyDown -> {
                        // R key to reset run
                        logger.debug { "⌨️ R key event detected" }
                        CoroutineScope(Dispatchers.Swing).launch {
                            logger.debug { "⌨️ Executing resetRun from R key event on Swing dispatcher" }
                            autoSplitsEngine.resetRun()
                        }
                        true
                    }

                    else -> false
                }
            } else {
                // Splits are hidden - ignore all keyboard shortcuts
                logger.debug { "⌨️ Ignoring keyboard shortcuts - splits are hidden" }
                false
            }
        }
    ) {
        SuperMetroidTrackerApp()
    }
}

@Composable
@Preview
fun SuperMetroidTrackerApp() {
    val trackerState by gameStateService.trackerState.collectAsState()
    val splitsState by autoSplitsEngine.splitsState.collectAsState()
    val currentTheme by themeService.currentTheme.collectAsState()
    val showSplits by uiVisibilityService.showSplits.collectAsState()
    val showIcons by uiVisibilityService.showIcons.collectAsState()
    val showTimer by uiVisibilityService.showTimer.collectAsState()
    val showSettings by uiVisibilityService.showSettings.collectAsState()
    
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
        
        // Initialize icon view mode service
        iconViewModeService.initialize()
        
        // Initialize UI visibility service
        uiVisibilityService.initialize()
        
        // Initialize room name service
        roomNameService.initialize()
        
        // Initialize sound service
        soundService.initialize()
        
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
        logger.debug { "🎮 Auto-splits processing: area=${trackerState.gameState.areaId}, room=${trackerState.gameState.roomId}, gameState=${trackerState.gameState.gameState}" }
        autoSplitsEngine.processGameState(trackerState.gameState)
        
        // Process sound effects for item collection and boss defeats
        soundService.processGameStateChange(trackerState.gameState)
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
                        showIcons = showIcons,
                        showTimer = showTimer,
                        showSettings = showSettings,
                        uiVisibilityService = uiVisibilityService,
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
    showIcons: Boolean,
    showTimer: Boolean,
    showSettings: Boolean,
    uiVisibilityService: com.supermetroid.service.UIVisibilityService,
    themeService: com.supermetroid.service.ThemeService,
    iconConfigService: com.supermetroid.service.IconConfigService,
    roomNameService: com.supermetroid.service.RoomNameService
) {

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
                    logger.debug { "🖱️ Timer UI button clicked - onToggleRun callback" }
                    CoroutineScope(Dispatchers.Swing).launch {
                        autoSplitsEngine.toggleRunState()
                    }
                },
                onResetRun = {
                    logger.debug { "🖱️ Timer UI reset button clicked" }
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
                iconViewModeService = iconViewModeService,
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
                autoSplitsEngine = autoSplitsEngine,
                iconViewModeService = iconViewModeService,
                soundService = soundService,
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
                    onClick = { 
                        CoroutineScope(Dispatchers.Swing).launch {
                            uiVisibilityService.setShowIcons(!showIcons)
                        }
                    },
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
                    onClick = { 
                        CoroutineScope(Dispatchers.Swing).launch {
                            uiVisibilityService.setShowSplits(!showSplits)
                        }
                    },
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
                    onClick = { 
                        CoroutineScope(Dispatchers.Swing).launch {
                            uiVisibilityService.setShowTimer(!showTimer)
                        }
                    },
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
                    onClick = { 
                        CoroutineScope(Dispatchers.Swing).launch {
                            uiVisibilityService.setShowSettings(!showSettings)
                        }
                    },
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
