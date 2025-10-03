package com.supermetroid.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.supermetroid.ui.theme.TrackerColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI
import kotlin.random.Random

/**
 * Panel for displaying the Metroid logo with visual effects
 */
@Composable
fun LogoEffectsPanel(
    logoEffectsService: com.supermetroid.service.LogoEffectsService,
    modifier: Modifier = Modifier
) {
    val logoState by logoEffectsService.logoState.collectAsState()

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
            // Title
            // Text(
            //     text = "Metroid Logo Effects",
            //     style = MaterialTheme.typography.titleMedium.copy(
            //         color = TrackerColors.Primary,
            //         fontWeight = FontWeight.Bold
            //     ),
            //     modifier = Modifier.padding(bottom = 8.dp)
            // )

            // Logo display - Full width, square aspect ratio
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f) // Square aspect ratio
                    .background(
                        color = TrackerColors.SurfaceOverlayLight,
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                logoState.logoImage?.let { image ->
                    Image(
                        bitmap = image,
                        contentDescription = "Metroid Logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } ?: Text(
                    text = "Loading logo...",
                    color = TrackerColors.OnSurfaceVariant
                )
            }
        }
    }
}

/**
 * Compact effect button
 */
@Composable
private fun CompactEffectButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) TrackerColors.Primary else TrackerColors.SurfaceOverlayLight,
            contentColor = if (selected) TrackerColors.OnPrimary else TrackerColors.OnSurface
        ),
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 3.dp),
        modifier = Modifier.height(24.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

/**
 * Apply sine/cosine noise effect to the image
 */
private fun applyNoiseEffect(original: BufferedImage, intensity: Float): ImageBitmap {
    val result = BufferedImage(original.width, original.height, BufferedImage.TYPE_INT_RGB)
    val time = System.currentTimeMillis() / 1000.0
    
    for (y in 0 until original.height) {
        for (x in 0 until original.width) {
            val originalColor = original.getRGB(x, y)
            
            // Extract RGB components
            val red = (originalColor shr 16) and 0xFF
            val green = (originalColor shr 8) and 0xFF
            val blue = originalColor and 0xFF
            
            // Apply sine/cosine noise
            val noiseX = sin(x * 0.05 + time * 2) * intensity * 50
            val noiseY = cos(y * 0.05 + time * 1.5) * intensity * 50
            val noiseZ = sin((x + y) * 0.03 + time * 3) * intensity * 30
            
            // Apply noise to each color channel
            val newRed = (red + noiseX).coerceIn(0.0, 255.0).toInt()
            val newGreen = (green + noiseY).coerceIn(0.0, 255.0).toInt()
            val newBlue = (blue + noiseZ).coerceIn(0.0, 255.0).toInt()
            
            val newColor = (newRed shl 16) or (newGreen shl 8) or newBlue
            result.setRGB(x, y, newColor)
        }
    }
    
    return result.toComposeImageBitmap()
}

/**
 * Apply random pixel swap effect to the image
 */
private fun applyPixelSwapEffect(original: BufferedImage, intensity: Float): ImageBitmap {
    val result = BufferedImage(original.width, original.height, BufferedImage.TYPE_INT_RGB)
    
    // Copy original image first
    for (y in 0 until original.height) {
        for (x in 0 until original.width) {
            result.setRGB(x, y, original.getRGB(x, y))
        }
    }
    
    // Perform random pixel swaps based on intensity
    val swapCount = (original.width * original.height * intensity * 0.1).toInt()
    val random = Random(System.currentTimeMillis())
    
    repeat(swapCount) {
        val x1 = random.nextInt(original.width)
        val y1 = random.nextInt(original.height)
        val x2 = random.nextInt(original.width)
        val y2 = random.nextInt(original.height)
        
        val color1 = result.getRGB(x1, y1)
        val color2 = result.getRGB(x2, y2)
        
        result.setRGB(x1, y1, color2)
        result.setRGB(x2, y2, color1)
    }
    
    return result.toComposeImageBitmap()
}

/**
 * Convert BufferedImage to Compose ImageBitmap
 */
private fun BufferedImage.toComposeImageBitmap(): ImageBitmap {
    return org.jetbrains.skia.Image.makeFromEncoded(
        java.io.ByteArrayOutputStream().use { baos ->
            ImageIO.write(this, "png", baos)
            baos.toByteArray()
        }
    ).asImageBitmap()
}

/**
 * Logo effect types
 */
enum class LogoEffectType {
    NONE,
    NOISE,
    PIXEL_SWAP
}
