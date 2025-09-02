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
import com.supermetroid.service.EffectType
import com.supermetroid.service.GameStateService
import com.supermetroid.service.PaletteEffectsService
import com.supermetroid.storage.FileStorageService
import com.supermetroid.ui.components.*
import com.supermetroid.ui.theme.TrackerColors
import com.supermetroid.ui.theme.TrackerTypography
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing

// Global services
val gameStateService = GameStateService()
val autoSplitsEngine = AutoSplitsEngine()
val fileStorageService = FileStorageService()
val paletteEffectsService = PaletteEffectsService(gameStateService.getUdpClient())
val logoEffectsService = com.supermetroid.service.LogoEffectsService()

fun main() = application {
    // Move showSplits state to Window level so keyboard shortcuts can access it
    var showSplits by remember { mutableStateOf(true) }
    
    Window(
        onCloseRequest = {
            gameStateService.stop()
            logoEffectsService.stop()
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
        SimpleTrackerApp(
            showSplits = showSplits,
            onShowSplitsChanged = { showSplits = it }
        )
    }
}

@Composable
@Preview
fun SimpleTrackerApp(
    showSplits: Boolean = true,
    onShowSplitsChanged: (Boolean) -> Unit = {}
) {
    val trackerState by gameStateService.trackerState.collectAsState()
    val splitsState by autoSplitsEngine.splitsState.collectAsState()
    val effectsState by paletteEffectsService.effectsState.collectAsState()

    // Initialize services
    LaunchedEffect(Unit) {
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

    // Clean up effects service on exit
    DisposableEffect(Unit) {
        onDispose {
            // Use runBlocking to handle the suspend function in a non-suspend context
            runBlocking {
                try {
                    // Always attempt to stop the service, regardless of its current state
                    // This ensures we clean up properly even if the state is inconsistent
                    paletteEffectsService.stop()
                    println("[DEBUG_LOG] Successfully stopped palette effects service on exit")
                } catch (e: Exception) {
                    println("[DEBUG_LOG] Failed to stop palette effects service on exit: ${e.message}")
                }
            }
        }
    }

    // Process game state for autosplits ONLY when splits are visible
    // This prevents auto-splitting when user is playing other games (like map rando)
    LaunchedEffect(trackerState.gameState, showSplits) {
        if (showSplits) {
            println("[DEBUG_LOG] Processing game state due to gameState change (splits visible)")
            autoSplitsEngine.processGameState(trackerState.gameState)
        } else {
            println("[DEBUG_LOG] Skipping game state processing - splits are hidden")
        }
    }

    // Save splits state periodically
    LaunchedEffect(splitsState) {
        try {
            fileStorageService.saveSplitsState(splitsState)
        } catch (e: Exception) {
            // Handle save error gracefully
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = TrackerColors.Primary,
            onPrimary = TrackerColors.OnPrimary,
            background = TrackerColors.Background,
            surface = TrackerColors.Surface,
            onBackground = TrackerColors.OnBackground,
            onSurface = TrackerColors.OnSurface
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
                                TrackerColors.Background,
                                TrackerColors.BackgroundVariant,
                                TrackerColors.Background
                            )
                        )
                    )
            ) {
                SimpleTwoColumnLayout(
                    trackerState = trackerState,
                    splitsState = splitsState,
                    effectsState = effectsState,
                    showSplits = showSplits,
                    onShowSplitsChanged = onShowSplitsChanged
                )
            }
        }
    }
}

@Composable
fun SimpleTwoColumnLayout(
    trackerState: com.supermetroid.model.TrackerState,
    splitsState: com.supermetroid.model.SplitsState,
    effectsState: com.supermetroid.service.PaletteEffectsState,
    showSplits: Boolean,
    onShowSplitsChanged: (Boolean) -> Unit
) {
    // UI visibility toggles (removed showSplits since it's now passed in)
    var showIcons by remember { mutableStateOf(true) }
    var showTimer by remember { mutableStateOf(true) }
    var showEffects by remember { mutableStateOf(false) }
    var showLogo by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp) // Reduced from 12dp to 4dp for more compact layout
    ) {
        // Header with connection status
        // TrackerHeader(
        //     connectionInfo = trackerState.connection,
        //     isFullscreen = false,
        //     onToggleFullscreen = {}
        // )

        // Spacer(modifier = Modifier.height(8.dp))



        // Logo Effects panel - Above status grid when visible
        if (showLogo) {
            LogoEffectsPanel(
                logoEffectsService = logoEffectsService,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        // TALL LAYOUT: Status Grid at top, Timer below, then Splits at bottom
        // Status Grid (Icons) - Fixed height, non-stretchable
        if (showIcons) {
            SimpleStatusGrid(
                gameState = trackerState.gameState,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp)) // Halved from 12dp
        }

        // Timer section - Centered and compact
        if (showTimer) {
            SimpleEnhancedTimer(
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
            Spacer(modifier = Modifier.height(6.dp)) // Halved from 12dp
        }

        // Splits list - Takes remaining space
        if (showSplits) {
            SplitsList(
                splitsState = splitsState,
                autoSplitsEngine = autoSplitsEngine,
                modifier = Modifier.fillMaxWidth().weight(1f),
                maxHeight = 900 // Increased height for taller window
            )
            Spacer(modifier = Modifier.height(6.dp)) // Halved from 12dp
        }

        // Effects panel
        if (showEffects) {
            EffectsPanel(
                effectsState = effectsState,
                onEffectTypeChanged = { effectType ->
                    CoroutineScope(Dispatchers.Swing).launch {
                        paletteEffectsService.setEffectType(effectType)
                    }
                },
                onIntensityChanged = { intensity ->
                    CoroutineScope(Dispatchers.Swing).launch {
                        paletteEffectsService.setIntensity(intensity)
                    }
                },
                logoEffectsService = logoEffectsService,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp)) // Halved from 12dp
        }

        // Spacer to push footer to bottom when splits are hidden
        if (!showSplits) {
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

                // Logo toggle
                TextButton(
                    onClick = {
                        showLogo = !showLogo
                        println("[DEBUG_LOG] Logo button clicked: ${if (showLogo) "showing" else "hiding"} logo panel")
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (showLogo) TrackerColors.Success else TrackerColors.OnSurfaceVariant
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    modifier = Modifier.height(20.dp)
                ) {
                    Text(
                        text = "logo",
                        style = MaterialTheme.typography.labelSmall
                    )
                }



                // Effects toggle
                TextButton(
                    onClick = {
                        val wasShowing = showEffects
                        showEffects = !showEffects
                        println("[DEBUG_LOG] Effects button clicked: ${if (wasShowing) "hiding" else "showing"} effects panel")

                        // Start or stop the effects services when toggled
                        CoroutineScope(Dispatchers.Swing).launch {
                            if (showEffects && !effectsState.enabled) {
                                println("[DEBUG_LOG] Starting effects services...")
                                try {
                                    paletteEffectsService.start()
                                    logoEffectsService.start()
                                    println("[DEBUG_LOG] Effects services started successfully")
                                } catch (e: Exception) {
                                    // Error is already logged and error message is set in the service
                                    // Just catch the exception to prevent app crash
                                    println("[DEBUG_LOG] Failed to start effects services: ${e.message}")
                                    println("[DEBUG_LOG] Exception type: ${e.javaClass.name}")
                                    e.printStackTrace() // Print stack trace for debugging
                                }
                            } else if (!showEffects && effectsState.enabled) {
                                println("[DEBUG_LOG] Stopping effects services...")
                                try {
                                    paletteEffectsService.stop()
                                    logoEffectsService.stop()
                                    println("[DEBUG_LOG] Effects services stopped successfully")
                                } catch (e: Exception) {
                                    println("[DEBUG_LOG] Failed to stop effects services: ${e.message}")
                                    println("[DEBUG_LOG] Exception type: ${e.javaClass.name}")
                                    e.printStackTrace() // Print stack trace for debugging
                                }
                            } else {
                                println("[DEBUG_LOG] No service state change needed: showing=${showEffects}, enabled=${effectsState.enabled}")
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (showEffects) TrackerColors.Success else TrackerColors.OnSurfaceVariant
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    modifier = Modifier.height(20.dp)
                ) {
                    Text(
                        text = "effects",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
