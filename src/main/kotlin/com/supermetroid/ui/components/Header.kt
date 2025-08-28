package com.supermetroid.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermetroid.model.ConnectionInfo
import com.supermetroid.ui.theme.TrackerColors

@Composable
fun TrackerHeader(
    connectionInfo: ConnectionInfo,
    isFullscreen: Boolean = false,
    onToggleFullscreen: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = TrackerColors.Surface
        ),
        shape = RoundedCornerShape(0.dp) // No rounding to match original
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            TrackerColors.Surface,
                            TrackerColors.SurfaceVariant
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    color = TrackerColors.SurfaceOverlay,
                    shape = RoundedCornerShape(0.dp)
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side - Title
                Box(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "METROID",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            letterSpacing = 2.sp,
                            color = TrackerColors.Primary,
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = TrackerColors.Primary.copy(alpha = 0.6f),
                                blurRadius = 8f
                            )
                        )
                    )
                }
                
                // Right side - Status and controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Connection status
                    ConnectionStatusIndicator(connectionInfo)
                    
                    // Fullscreen toggle button
                    IconButton(
                        onClick = onToggleFullscreen,
                        modifier = Modifier
                            .background(
                                TrackerColors.SurfaceOverlay,
                                RoundedCornerShape(4.dp)
                            )
                            .border(
                                1.dp,
                                TrackerColors.BorderActive.copy(alpha = 0.4f),
                                RoundedCornerShape(4.dp)
                            )
                    ) {
                        Icon(
                            imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = if (isFullscreen) "Exit Fullscreen" else "Enter Fullscreen",
                            tint = TrackerColors.Primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionStatusIndicator(connectionInfo: ConnectionInfo) {
    val isConnected = connectionInfo.connected
    val isGameLoaded = connectionInfo.gameLoaded
    
    // Pulsing animation for status dot
    val infiniteTransition = rememberInfiniteTransition(label = "statusPulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "statusAlpha"
    )
    
    Card(
        modifier = Modifier
            .padding(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) {
                TrackerColors.SurfaceOverlay
            } else {
                TrackerColors.Error.copy(alpha = 0.3f)
            }
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier
                .border(
                    1.dp,
                    if (isConnected) TrackerColors.BorderActive.copy(alpha = 0.4f)
                    else TrackerColors.Error.copy(alpha = 0.4f),
                    RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Status dot with pulsing animation
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = (if (isConnected) TrackerColors.Connected else TrackerColors.Disconnected)
                            .copy(alpha = alpha),
                        shape = CircleShape
                    )
            )
            
            // Status text
            Column {
                Text(
                    text = if (isConnected) "CONNECTED" else "DISCONNECTED",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = if (isConnected) TrackerColors.Connected else TrackerColors.Disconnected,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                )
                if (isConnected && connectionInfo.retroarchVersion != null) {
                    Text(
                        text = "RetroArch ${connectionInfo.retroarchVersion}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = TrackerColors.OnSurfaceVariant,
                            fontSize = 10.sp
                        )
                    )
                }
                if (isGameLoaded) {
                    Text(
                        text = "Super Metroid",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = TrackerColors.OnSurfaceVariant,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }
}
