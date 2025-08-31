package com.supermetroid.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermetroid.service.*
import com.supermetroid.ui.theme.TrackerColors

/**
 * Panel for webcam display with effects and camera switching
 */
@Composable
fun WebcamPanel(
    webcamService: WebcamService,
    webcamEffectsService: WebcamEffectsService,
    modifier: Modifier = Modifier
) {
    val webcamState by webcamService.webcamState.collectAsState()
    val effectsState by webcamEffectsService.webcamEffectsState.collectAsState()
    val currentTileSize by webcamEffectsService.currentTileSize.collectAsState()
    
    // Process webcam frames with effects only when effects are active
    LaunchedEffect(webcamState.currentFrame, effectsState.activeEffect) {
        if (effectsState.activeEffect != com.supermetroid.service.LogoEffectType.NONE) {
            webcamState.currentFrame?.let { frame ->
                webcamEffectsService.processFrame(frame)
            }
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = TrackerColors.Surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with camera name and controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Camera name
                Text(
                    text = if (webcamState.availableCameras.isNotEmpty() && webcamState.selectedCameraIndex >= 0) {
                        webcamState.availableCameras[webcamState.selectedCameraIndex].name
                    } else {
                        "No Camera"
                    },
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = TrackerColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                )
                
                // Camera controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Next camera button
                    Button(
                        onClick = { webcamService.nextCamera() },
                        enabled = webcamState.availableCameras.size > 1,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TrackerColors.Primary,
                            contentColor = TrackerColors.Background
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = "Next Cam",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    
                    // Start/Stop button
                    Button(
                        onClick = {
                            if (webcamState.isCapturing) {
                                webcamService.stopCapture()
                                webcamEffectsService.stop()
                            } else {
                                webcamService.startCapture()
                                webcamEffectsService.start()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (webcamState.isCapturing) TrackerColors.Error else TrackerColors.Success,
                            contentColor = TrackerColors.Background
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = if (webcamState.isCapturing) "Stop" else "Start",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Webcam display area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f) // Standard webcam aspect ratio
                    .clip(RoundedCornerShape(8.dp))
                    .background(TrackerColors.SurfaceOverlayLight)
                    .border(1.dp, TrackerColors.OnSurfaceVariant, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    webcamState.errorMessage != null -> {
                        // Error state
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Camera Error",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = TrackerColors.Error
                                )
                            )
                            Text(
                                text = webcamState.errorMessage ?: "Unknown error",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TrackerColors.OnSurfaceVariant
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                    !webcamState.isCapturing -> {
                        // Not capturing state
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "📹",
                                style = MaterialTheme.typography.displayMedium
                            )
                            Text(
                                text = "Press Start to begin capture",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TrackerColors.OnSurfaceVariant
                                )
                            )
                        }
                    }
                    effectsState.processedFrame != null && effectsState.activeEffect != com.supermetroid.service.LogoEffectType.NONE -> {
                        // Show processed frame with effects
                        effectsState.processedFrame?.let { frame ->
                            key(webcamState.frameId) {
                                WebcamImage(
                                    image = frame,
                                    frameId = webcamState.frameId,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                    webcamState.currentFrame != null -> {
                        // Show original frame (no effects or effects disabled)
                        webcamState.currentFrame?.let { frame ->
                            key(webcamState.frameId) {
                                WebcamImage(
                                    image = frame,
                                    frameId = webcamState.frameId,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                    else -> {
                        // Loading state
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = TrackerColors.Primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Connecting to camera...",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TrackerColors.OnSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
            

        }
    }
}

/**
 * Webcam image display with proper scaling
 */
@Composable
private fun WebcamImage(
    image: ImageBitmap,
    frameId: String,
    modifier: Modifier = Modifier
) {
    Image(
        bitmap = image,
        contentDescription = "Webcam feed - $frameId",
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}


