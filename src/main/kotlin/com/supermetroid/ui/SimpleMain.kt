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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing

// Global services
val gameStateService = GameStateService()
val autoSplitsEngine = AutoSplitsEngine()
val fileStorageService = FileStorageService()

fun main() = application {
    Window(
        onCloseRequest = {
            gameStateService.stop()
            exitApplication()
        },
        title = "Super Metroid Tracker",
        state = androidx.compose.ui.window.rememberWindowState(
            width = 420.dp,  // Optimized for tall/skinny like image 2
            height = 1100.dp // Increased by 300dp for better splits visibility
        ),
        onKeyEvent = { keyEvent ->
            when {
                keyEvent.key == Key.Spacebar && keyEvent.type == KeyEventType.KeyDown -> {
                    // Spacebar to start/pause timer
                    // Adding a log to track when this is called
                    println("[DEBUG_LOG] Spacebar key event detected in Window.onKeyEvent")
                    // Use the same CoroutineScope as the UI buttons for consistency
                    CoroutineScope(Dispatchers.Swing).launch {
                        println("[DEBUG_LOG] Executing toggleRunState from Spacebar key event on Swing dispatcher")
                        autoSplitsEngine.toggleRunState()
                    }
                    true
                }

                keyEvent.key == Key.R && keyEvent.type == KeyEventType.KeyDown -> {
                    // R key to reset run
                    println("[DEBUG_LOG] R key event detected")
                    // Use the same CoroutineScope as the UI buttons for consistency
                    CoroutineScope(Dispatchers.Swing).launch {
                        println("[DEBUG_LOG] Executing resetRun from R key event on Swing dispatcher")
                        autoSplitsEngine.resetRun()
                    }
                    true
                }

                else -> false
            }
        }
    ) {
        SimpleTrackerApp()
    }
}

@Composable
@Preview
fun SimpleTrackerApp() {
    val trackerState by gameStateService.trackerState.collectAsState()
    val splitsState by autoSplitsEngine.splitsState.collectAsState()

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

    // Process game state for autosplits (trigger only on gameState changes, not run state changes)
    // This prevents potential loops where processGameState changes run state, which triggers this effect again
    LaunchedEffect(trackerState.gameState) {
        println("[DEBUG_LOG] Processing game state due to gameState change")
        autoSplitsEngine.processGameState(trackerState.gameState)
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
                SimpleTwoColumnLayout(trackerState, splitsState)
            }
        }
    }
}

@Composable
fun SimpleTwoColumnLayout(
    trackerState: com.supermetroid.model.TrackerState,
    splitsState: com.supermetroid.model.SplitsState
) {
    // UI visibility toggles
    var showIcons by remember { mutableStateOf(true) }
    var showSplits by remember { mutableStateOf(true) }
    var showTimer by remember { mutableStateOf(true) }

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

        // UI Toggle footer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            // Connection status (left side)
            Text(
                text = "${if (trackerState.connection.connected) "Connected" else "Disconnected"}${if (trackerState.connection.gameLoaded) " | Game Loaded" else ""}",
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
                    onClick = { showSplits = !showSplits },
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
            }
        }
    }
}
