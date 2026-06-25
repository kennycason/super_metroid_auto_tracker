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
import com.supermetroid.AppDependencies
import com.supermetroid.autosplits.AutoSplitsEngine
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

// Central dependency container - initialized in main()
lateinit var appDependencies: AppDependencies

// Convenience accessors for backward compatibility (will be removed in future refactor)
val fileStorageService: FileStorageService get() = appDependencies.fileStorageService
val gameStateService: GameStateService get() = appDependencies.gameStateService
val autoSplitsEngine: AutoSplitsEngine get() = appDependencies.autoSplitsEngine
val themeService: com.supermetroid.service.ThemeService get() = appDependencies.themeService
val iconSizeService: com.supermetroid.service.IconSizeService get() = appDependencies.iconSizeService
val splitIconSizeService: com.supermetroid.service.SplitIconSizeService get() = appDependencies.splitIconSizeService
val splitDisplayModeService: com.supermetroid.service.SplitDisplayModeService get() = appDependencies.splitDisplayModeService
val splitProfileService: com.supermetroid.service.SplitProfileService get() = appDependencies.splitProfileService
val iconConfigService: com.supermetroid.service.IconConfigService get() = appDependencies.iconConfigService
val roomNameService: com.supermetroid.service.RoomNameService get() = appDependencies.roomNameService
val iconViewModeService: com.supermetroid.service.IconViewModeService get() = appDependencies.iconViewModeService
val uiVisibilityService: com.supermetroid.service.UIVisibilityService get() = appDependencies.uiVisibilityService
val soundService: com.supermetroid.service.SoundService get() = appDependencies.soundService
val gameGenieService: com.supermetroid.service.GameGenieService get() = appDependencies.gameGenieService
val mapRandoInfoFontSizeService: com.supermetroid.service.MapRandoInfoFontSizeService get() = appDependencies.mapRandoInfoFontSizeService
val mapRandoDataService: com.supermetroid.service.MapRandoDataService get() = appDependencies.mapRandoDataService
val mapRandoInfoConfigService: com.supermetroid.service.MapRandoInfoConfigService get() = appDependencies.mapRandoInfoConfigService
val splitFormatService: com.supermetroid.service.SplitFormatService get() = appDependencies.splitFormatService
val statsFontSizeService: com.supermetroid.service.StatsFontSizeService get() = appDependencies.statsFontSizeService

// Flag to indicate if we're in replay mode (don't load saved state)
var isReplayMode = false

fun main(args: Array<String>) {
    // Parse command-line arguments
    val (customDataDir, currentRunFile) = parseCommandLineArgs(args)
    
    if (customDataDir != null) {
        logger.info { "🗂️  Using custom data directory: $customDataDir" }
    }
    
    if (currentRunFile != null) {
        isReplayMode = true
        logger.info { "🎬 Replay mode: Loading run from $currentRunFile" }
    }
    
    // Initialize all services via AppDependencies
    appDependencies = AppDependencies.create(customDataDir)
    
    // Initialize autoSplitsEngine to restore saved timer OR load replay run
    runBlocking {
        if (currentRunFile != null) {
            autoSplitsEngine.loadReplayRun(currentRunFile)
        } else {
            autoSplitsEngine.initialize()
        }
    }
    
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
 * Parse command-line arguments
 * Returns: Pair(customDataDir, currentRunFile)
 */
private fun parseCommandLineArgs(args: Array<String>): Pair<String?, String?> {
    var customDataDir: String? = null
    var currentRunFile: String? = null
    
    for (arg in args) {
        when {
            arg.startsWith("--data-dir=") -> {
                customDataDir = arg.substring("--data-dir=".length)
            }
            arg.startsWith("--current-run=") -> {
                currentRunFile = arg.substring("--current-run=".length)
            }
            arg == "--help" || arg == "-h" -> {
                printHelp()
                System.exit(0)
            }
        }
    }
    
    return Pair(customDataDir, currentRunFile)
}

private fun printHelp() {
    println("""
        Super Metroid Auto Tracker
        
        Usage: SuperMetroidTracker [options]
        
        Options:
          --data-dir=<path>        Use a custom data directory instead of ~/.smtracker/
                                   NOTE: Specify the PARENT directory that CONTAINS runs/
                                   NOT the runs/ directory itself!
          --current-run=<file>     Replay mode: Load a specific run file and display its stats
                                   Example: --current-run=2025-11-16_04-50-35_kpdr-any_1763297435790.json
                                   This loads all previous runs for BP calculation, then displays
                                   the specified run as if you just finished it.
          --help, -h               Show this help message
        
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
    val showStats by uiVisibilityService.showStats.collectAsState()
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
        
        // Initialize Map Rando info font size service
        mapRandoInfoFontSizeService.initialize()

        // Initialize stats font size service
        statsFontSizeService.initialize()
        
        // Initialize Map Rando info config service
        mapRandoInfoConfigService.initialize()
        
        // Mark services as initialized
        servicesInitialized = true
        
        // Initialize split profile from saved config (notifies AutoSplitsEngine)
        splitProfileService.initialize()

        // Wire up profile change -> LSS switch callback
        splitProfileService.setOnProfileChangedCallback { profileId ->
            splitFormatService.onProfileChanged(profileId)
        }

        // Initialize split format (LSS is always primary)
        splitFormatService.initialize()

        // When the LSS file changes, reload PB data into the engine
        splitFormatService.setOnFormatChangedCallback {
            val state = splitFormatService.loadCurrentState()
            autoSplitsEngine.loadSavedState(state)

            val resolvedProfile = splitFormatService.getResolvedLiveSplitProfile()
            if (resolvedProfile != null) {
                splitProfileService.setProfile(resolvedProfile, notifyFormatService = false)
            }
        }

        // Load saved splits state and resume from current position (unless in replay mode)
        if (!isReplayMode) {
            val savedSplitsState = splitFormatService.loadCurrentState()
            autoSplitsEngine.loadSavedState(savedSplitsState)

            val resolvedProfile = splitFormatService.getResolvedLiveSplitProfile()
            if (resolvedProfile != null) {
                splitProfileService.setProfile(resolvedProfile, notifyFormatService = false)
            }
        }

        // CRITICAL: Set up game state callback BEFORE starting the service
        // This ensures we don't miss any auto-start transitions
        gameStateService.setGameStateCallback { gameState ->
            // Only process splits when not in Map Rando mode
            val currentMode = iconViewModeService.iconViewMode.value
            if (currentMode != com.supermetroid.model.IconViewMode.MAP_RANDO) {
                autoSplitsEngine.processGameState(gameState)
            }
        }

        try {
            gameStateService.start()
        } catch (e: Exception) {
            // Connection will show as disconnected
        }
    }


    // Auto-disable/enable splits when switching between Map Rando and other modes
    val iconViewMode by iconViewModeService.iconViewMode.collectAsState()
    LaunchedEffect(iconViewMode) {
        if (iconViewMode == com.supermetroid.model.IconViewMode.MAP_RANDO && showSplits) {
            logger.info { "📊 Map Rando mode detected - automatically hiding splits (item tracking only)" }
            uiVisibilityService.setShowSplits(false)
        } else if (iconViewMode != com.supermetroid.model.IconViewMode.MAP_RANDO && !showSplits) {
            // Auto-enable splits when switching back to Default/Bosses mode for KPDR runs
            logger.info { "📊 Switched to ${iconViewMode.displayName} mode - automatically showing splits for speedrun tracking" }
            uiVisibilityService.setShowSplits(true)
        }
    }
    
    // Process sound effects for item collection and boss defeats
    LaunchedEffect(trackerState.gameState) {
        soundService.processGameStateChange(trackerState.gameState)
    }
    
    // Update Map Rando data from game state (uses its own stable coroutine scope)
    // This runs outside LaunchedEffect to avoid cancellation during recomposition
    mapRandoDataService.updateFromGameState(trackerState.gameState)

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
                        showStats = showStats,
                        gameGenieEnabled = gameGenieEnabled,
                        uiVisibilityService = uiVisibilityService,
                        themeService = themeService,
                        iconConfigService = iconConfigService,
                        roomNameService = roomNameService,
                        iconViewModeService = iconViewModeService
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
    showStats: Boolean,
    gameGenieEnabled: Boolean,
    uiVisibilityService: com.supermetroid.service.UIVisibilityService,
    themeService: com.supermetroid.service.ThemeService,
    iconConfigService: com.supermetroid.service.IconConfigService,
    roomNameService: com.supermetroid.service.RoomNameService,
    iconViewModeService: com.supermetroid.service.IconViewModeService
) {
    val scope = rememberCoroutineScope()
    val iconViewMode by iconViewModeService.iconViewMode.collectAsState()
    val showMapRandoInfo by uiVisibilityService.showMapRandoInfo.collectAsState()
    
    // Determine if Map Rando info panel should be visible
    val showMapRandoPanel = showMapRandoInfo && showIcons && iconViewMode == com.supermetroid.model.IconViewMode.MAP_RANDO

    // Determine if an overlay panel is active
    val overlayActive = showSettings || (showGameGenie && gameGenieEnabled) || showStats

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp) // Minimal padding for very compact layout
    ) {
        // Main content area with overlay panels on top
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            // Base layer: main content (timer, room name, icons, splits)
            Column(modifier = Modifier.fillMaxSize()) {
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
                        },
                        onManualSplit = {
                            logger.debug { "🖱️ Timer UI manual split clicked" }
                            autoSplitsEngine.manualSplit()
                        },
                        onUndoSplit = {
                            logger.debug { "🖱️ Timer UI undo split clicked" }
                            autoSplitsEngine.undoSplit()
                        },
                        onDiscardRun = {
                            logger.debug { "🖱️ Timer UI discard run clicked" }
                            autoSplitsEngine.discardRun()
                        }
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                }

                // Room Name Display - separate row
                RoomNameDisplay(
                    gameState = trackerState.gameState,
                    roomNameService = roomNameService,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(3.dp))

                // Status Grid (Icons) with optional Map Rando info panel
                if (showIcons) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SimpleStatusGrid(
                            gameState = trackerState.gameState,
                            iconConfigService = iconConfigService,
                            iconSizeService = iconSizeService,
                            iconViewModeService = iconViewModeService,
                            modifier = Modifier.weight(1f)
                        )

                        if (showMapRandoPanel) {
                            MapRandoInfoPanel(
                                fontSizeService = mapRandoInfoFontSizeService,
                                mapRandoDataService = mapRandoDataService,
                                mapRandoInfoConfigService = mapRandoInfoConfigService,
                                modifier = Modifier.wrapContentHeight()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                }

                // Splits list - Takes remaining space
                if (showSplits) {
                    SplitsList(
                        splitsState = splitsState,
                        autoSplitsEngine = autoSplitsEngine,
                        splitIconSizeService = splitIconSizeService,
                        splitDisplayModeService = splitDisplayModeService,
                        splitProfileService = splitProfileService,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        maxHeight = 2000
                    )

                    val currentProfile by splitProfileService.currentProfile.collectAsState()
                    val currentProfilePB = splitsState.personalBests[currentProfile.id]
                    val showBestPossibleSummary by splitDisplayModeService.showBestPossibleSummary.collectAsState()
                    if (currentProfilePB != null) {
                        PersonalBestSummary(
                            splitsState = splitsState,
                            profileId = currentProfile.id,
                            profile = currentProfile,
                            showBestPossible = showBestPossibleSummary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))
                }

                if (!showSplits) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            // Overlay layer: settings, genie, or stats panel on top of main content
            if (overlayActive) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.97f)
                ) {
                    when {
                        showSettings -> SettingsPanel(
                            themeService = themeService,
                            iconSizeService = iconSizeService,
                            fileStorageService = fileStorageService,
                            splitIconSizeService = splitIconSizeService,
                            splitDisplayModeService = splitDisplayModeService,
                            splitProfileService = splitProfileService,
                            splitFormatService = splitFormatService,
                            iconConfigService = iconConfigService,
                            roomNameService = roomNameService,
                            autoSplitsEngine = autoSplitsEngine,
                            iconViewModeService = iconViewModeService,
                            soundService = soundService,
                            gameGenieService = gameGenieService,
                            uiVisibilityService = uiVisibilityService,
                            mapRandoInfoFontSizeService = mapRandoInfoFontSizeService,
                            mapRandoInfoConfigService = mapRandoInfoConfigService,
                            modifier = Modifier.fillMaxSize()
                        )
                        showGameGenie && gameGenieEnabled -> GameGenieTab(
                            modifier = Modifier.fillMaxSize()
                        )
                        showStats -> {
                            val currentProfile by splitProfileService.currentProfile.collectAsState()
                            com.supermetroid.ui.components.StatsPanel(
                                splitsState = splitsState,
                                profile = currentProfile,
                                statsFontSizeService = statsFontSizeService,
                                fileStorageService = fileStorageService,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
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
                            val newShowTimer = !showTimer
                            uiVisibilityService.setShowTimer(newShowTimer)
                            // Disable auto-start when timer is hidden, enable when shown
                            autoSplitsEngine.setAutoStartEnabled(newShowTimer)
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

                // Stats toggle
                TextButton(
                    onClick = {
                        CoroutineScope(Dispatchers.Swing).launch {
                            val newShow = !showStats
                            if (newShow) {
                                uiVisibilityService.setShowSettings(false)
                                uiVisibilityService.setShowGameGenie(false)
                            }
                            uiVisibilityService.setShowStats(newShow)
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (showStats) TrackerColors.Success else TrackerColors.OnSurfaceVariant
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    modifier = Modifier.height(20.dp)
                ) {
                    Text(
                        text = "stats",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                // Game Genie toggle (only show if Game Genie is enabled in settings)
                if (gameGenieEnabled) {
                    TextButton(
                        onClick = {
                            CoroutineScope(Dispatchers.Swing).launch {
                                val newShow = !showGameGenie
                                if (newShow) {
                                    uiVisibilityService.setShowSettings(false)
                                    uiVisibilityService.setShowStats(false)
                                }
                                uiVisibilityService.setShowGameGenie(newShow)
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
                            val newShow = !showSettings
                            if (newShow) {
                                uiVisibilityService.setShowStats(false)
                                uiVisibilityService.setShowGameGenie(false)
                            }
                            uiVisibilityService.setShowSettings(newShow)
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
