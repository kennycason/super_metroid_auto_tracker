package com.supermetroid.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.painterResource
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

// Global services - initialized in main()
lateinit var fileStorageService: FileStorageService
lateinit var gameStateService: GameStateService
lateinit var autoSplitsEngine: AutoSplitsEngine
lateinit var themeService: com.supermetroid.service.ThemeService
lateinit var iconSizeService: com.supermetroid.service.IconSizeService
lateinit var splitIconSizeService: com.supermetroid.service.SplitIconSizeService
lateinit var splitDisplayModeService: com.supermetroid.service.SplitDisplayModeService
lateinit var iconConfigService: com.supermetroid.service.IconConfigService
lateinit var roomNameService: com.supermetroid.service.RoomNameService
lateinit var iconViewModeService: com.supermetroid.service.IconViewModeService
lateinit var uiVisibilityService: com.supermetroid.service.UIVisibilityService
lateinit var soundService: com.supermetroid.service.SoundService
lateinit var gameGenieService: com.supermetroid.service.GameGenieService

fun main(args: Array<String>) {
    // Parse command-line arguments
    val customDataDir = parseDataDirArg(args)
    
    if (customDataDir != null) {
        logger.info { "🗂️  Using custom data directory: $customDataDir" }
    }
    
    // Initialize services with optional custom data directory
    fileStorageService = FileStorageService(customDataDir)
    gameStateService = GameStateService()
    autoSplitsEngine = AutoSplitsEngine(fileStorageService)
    themeService = com.supermetroid.service.ThemeService(fileStorageService)
    iconSizeService = com.supermetroid.service.IconSizeService(fileStorageService)
    splitIconSizeService = com.supermetroid.service.SplitIconSizeService(fileStorageService)
    splitDisplayModeService = com.supermetroid.service.SplitDisplayModeService(fileStorageService)
    iconConfigService = com.supermetroid.service.IconConfigService(fileStorageService)
    roomNameService = com.supermetroid.service.RoomNameService(fileStorageService)
    iconViewModeService = com.supermetroid.service.IconViewModeService(fileStorageService)
    uiVisibilityService = com.supermetroid.service.UIVisibilityService(fileStorageService)
    soundService = com.supermetroid.service.SoundService(fileStorageService)
    gameGenieService = com.supermetroid.service.GameGenieService(fileStorageService)
    
    application {
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
        icon = painterResource("icon.png"),
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
}

/**
 * Parse command-line arguments for custom data directory
 * Usage: --data-dir=/path/to/directory
 */
private fun parseDataDirArg(args: Array<String>): String? {
    for (arg in args) {
        when {
            arg.startsWith("--data-dir=") -> {
                return arg.substring("--data-dir=".length)
            }
            arg == "--help" || arg == "-h" -> {
                printHelp()
                System.exit(0)
            }
        }
    }
    return null
}

private fun printHelp() {
    println("""
        Super Metroid Auto Tracker
        
        Usage: SuperMetroidTracker [options]
        
        Options:
          --data-dir=<path>    Use a custom data directory instead of ~/.smtracker/
                               NOTE: Specify the PARENT directory that CONTAINS runs/
                               NOT the runs/ directory itself!
          --help, -h           Show this help message
        
        Directory Structure:
          <data-dir>/
            ├── runs/*.json          ← Run files
            ├── smtracker.json       ← Config
            └── run-summaries.json   ← Cache
        
        Examples:
          # Use default directory (~/.smtracker/)
          SuperMetroidTracker
          
          # ✅ CORRECT - Use custom test directory
          SuperMetroidTracker --data-dir=test_data
          
          # ✅ CORRECT - Use real data with explicit path
          SuperMetroidTracker --data-dir=/Users/kenny/.smtracker
          
          # ❌ WRONG - Don't include /runs in the path!
          SuperMetroidTracker --data-dir=test_data/runs
    """.trimIndent())
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
    val showGameGenie by uiVisibilityService.showGameGenie.collectAsState()
    val gameGenieEnabled by gameGenieService.gameGenieEnabled.collectAsState()
    
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
        
        // Initialize Game Genie service
        gameGenieService.initialize()
        
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
    
    // Process autosplits on every game state to ensure reliable auto-start and phase detection
    LaunchedEffect(trackerState.gameState) {
        logger.debug { "🎮 Auto-splits processing: area=${trackerState.gameState.areaId}, room=${trackerState.gameState.roomId}, gameState=${trackerState.gameState.gameState}" }
        autoSplitsEngine.processGameState(trackerState.gameState)
        
        // Process sound effects for item collection and boss defeats
        soundService.processGameStateChange(trackerState.gameState)
    }

    // Note: Splits are now saved automatically to runs/ directory by AutoSplitsEngine
    // No need to save to legacy splits-data.json format anymore

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
                        showGameGenie = showGameGenie,
                        gameGenieEnabled = gameGenieEnabled,
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
    showGameGenie: Boolean,
    gameGenieEnabled: Boolean,
    uiVisibilityService: com.supermetroid.service.UIVisibilityService,
    themeService: com.supermetroid.service.ThemeService,
    iconConfigService: com.supermetroid.service.IconConfigService,
    roomNameService: com.supermetroid.service.RoomNameService
) {
    val scope = rememberCoroutineScope()

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
                maxHeight = 900 // Maximum list height for tall windows
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
                gameGenieService = gameGenieService,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Takes all remaining vertical space
            )
            Spacer(modifier = Modifier.height(3.dp)) // Minimal spacing for compact layout
        }

        // Game Genie panel - takes remaining space when visible
        if (showGameGenie && gameGenieEnabled) {
            GameGenieTab(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Takes all remaining vertical space
            )
            Spacer(modifier = Modifier.height(3.dp)) // Minimal spacing for compact layout
        }

        // Spacer to push footer to bottom when splits are hidden and settings/Game Genie not shown
        if (!showSplits && !showSettings && !(showGameGenie && gameGenieEnabled)) {
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
                    if (trackerState.connection.gameLoaded) "Connected" else "Connected (No Game)"
                } else "Disconnected",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (trackerState.connection.connected) TrackerColors.Success else TrackerColors.Error
                ),
                modifier = Modifier.clickable {
                    // Manual reconnect to SNI/RetroArch: tries both and restarts backoff
                    scope.launch {
                        gameStateService.reconnectNow()
                    }
                }
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

                // Game Genie toggle (only show if Game Genie is enabled in settings)
                if (gameGenieEnabled) {
                    TextButton(
                        onClick = { 
                            CoroutineScope(Dispatchers.Swing).launch {
                                uiVisibilityService.setShowGameGenie(!showGameGenie)
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (showGameGenie) TrackerColors.Success else TrackerColors.OnSurfaceVariant
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                        modifier = Modifier.height(20.dp)
                    ) {
                        Text(
                            text = "genie",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
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
