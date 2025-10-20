package com.supermetroid.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermetroid.ui.theme.TrackerColors

/**
 * Real sprite icon that crops from sprite sheets
 */
@Composable
fun SpriteIcon(
    itemId: String,
    isObtained: Boolean,
    modifier: Modifier = Modifier,
    size: Int = 48
) {
    // Handle empty slots
    if (itemId.isEmpty()) {
        Box(
            modifier = modifier
                .size(size.dp)
                .aspectRatio(1f)
        ) {
            // Empty space - invisible placeholder
        }
        return
    }

    val spriteInfo = getSpriteInfo(itemId)

    Box(
        modifier = modifier
            .size(size.dp) // Always square - width and height are the same
            .aspectRatio(1f), // Force 1:1 aspect ratio to prevent stretching
        contentAlignment = Alignment.Center
    ) {
            if (spriteInfo != null) {
                SpriteImage(
                    spriteFile = spriteInfo.spriteFile,
                    spriteX = spriteInfo.x,
                    spriteY = spriteInfo.y,
                    spriteWidth = spriteInfo.width,
                    spriteHeight = spriteInfo.height,
                    displaySize = size,
                    isObtained = isObtained
                )
            } else {
                // Fallback for unknown items
                Text(
                    text = itemId.take(3).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isObtained) TrackerColors.Success else TrackerColors.Inactive,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }
    }
}

@Composable
fun SpriteImage(
    spriteFile: String,
    spriteX: Int,
    spriteY: Int,
    spriteWidth: Int,
    spriteHeight: Int,
    displaySize: Int,
    isObtained: Boolean
) {
    // Load sprite sheet
    val spriteBitmap = remember(spriteFile) {
        try {
            useResource(spriteFile) { inputStream ->
                loadImageBitmap(inputStream)
            }
        } catch (e: Exception) {
            null
        }
    }

    if (spriteBitmap != null) {
        Canvas(
            modifier = Modifier.size(displaySize.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // FILL the entire display area (stretch sprite to fit)
            // This makes item sprites (16x16) fill the same size as boss sprites (64x64)
            val scaledWidth = canvasWidth
            val scaledHeight = canvasHeight
            val offsetX = 0f
            val offsetY = 0f

            // Draw the actual sprite
            drawImage(
                image = spriteBitmap,
                srcOffset = IntOffset(spriteX, spriteY),
                srcSize = IntSize(spriteWidth, spriteHeight),
                dstOffset = IntOffset(offsetX.toInt(), offsetY.toInt()),
                dstSize = IntSize(scaledWidth.toInt(), scaledHeight.toInt()),
                alpha = if (isObtained) 1.0f else 0.7f,
                colorFilter = if (isObtained) null else ColorFilter.colorMatrix(
                    ColorMatrix().apply { setToSaturation(0f) } // Grayscale
                )
            )
        }
    } else {
        // Fallback if sprite loading fails
        Box(
            modifier = Modifier.size(displaySize.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "?",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isObtained) TrackerColors.Success else TrackerColors.Inactive,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            )
        }
    }
}

/**
 * Sprite information with correct coordinates from TypeScript CSS
 */
data class SpriteInfo(
    val spriteFile: String,
    val x: Int,
    val y: Int,
    val width: Int = 64,
    val height: Int = 64
)

/**
 * Get sprite coordinates based on itemId
 * Coordinates from TypeScript Status.css background-position values (converted from negative to positive)
 */
fun getSpriteInfo(itemId: String): SpriteInfo? {
    return when (itemId) {
        // === ITEM SPRITES (item_sprites.png) - 16x16 sprites in 512x512 sheet ===
        // Need to convert from TypeScript 64px grid coordinates to 16px coordinates
        // TypeScript uses -128px = 128/64 = position 2, so 2*16 = 32px

        // Quantity items
        "missile", "missiles" -> SpriteInfo("item_sprites.png", 0, 16, 16, 16)  // Row 1, Col 0
        "super", "supers", "super_missile" -> SpriteInfo("item_sprites.png", 32, 16, 16, 16)  // Row 1, Col 2
        "power_bomb", "power_bombs", "powerbombs" -> SpriteInfo("item_sprites.png", 64, 16, 16, 16)  // Row 1, Col 4
        "health", "energy_tank" -> SpriteInfo("item_sprites.png", 64, 0, 16, 16)  // Row 0, Col 4
        "reserve_tank" -> SpriteInfo("item_sprites.png", 96, 16, 16, 16)  // Row 1, Col 6

        // Major items - Row 0
        "morph", "morph_ball" -> SpriteInfo("item_sprites.png", 0, 0, 16, 16)
        "bomb", "bombs" -> SpriteInfo("item_sprites.png", 32, 0, 16, 16)

        // Major items - Row 2
        "hijump", "hi_jump" -> SpriteInfo("item_sprites.png", 0, 32, 16, 16)
        "speed", "speed_booster" -> SpriteInfo("item_sprites.png", 32, 32, 16, 16)
        "grapple", "grappling_beam" -> SpriteInfo("item_sprites.png", 64, 32, 16, 16)
        "xray", "x_ray" -> SpriteInfo("item_sprites.png", 96, 32, 16, 16)

        // Major items - Row 3
        "spring", "spring_ball" -> SpriteInfo("item_sprites.png", 0, 48, 16, 16)
        "space", "space_jump" -> SpriteInfo("item_sprites.png", 32, 48, 16, 16)
        "screw", "screw_attack" -> SpriteInfo("item_sprites.png", 64, 48, 16, 16)
        "charge", "charge_beam" -> SpriteInfo("item_sprites.png", 96, 48, 16, 16)

        // Beams - Row 4
        "spazer" -> SpriteInfo("item_sprites.png", 0, 64, 16, 16)
        "wave", "wave_beam" -> SpriteInfo("item_sprites.png", 32, 64, 16, 16)
        "ice", "ice_beam" -> SpriteInfo("item_sprites.png", 64, 64, 16, 16)
        "plasma", "plasma_beam" -> SpriteInfo("item_sprites.png", 96, 64, 16, 16)

        // Suits - Row 5
        "varia", "varia_suit" -> SpriteInfo("item_sprites.png", 0, 80, 16, 16)
        "gravity", "gravity_suit" -> SpriteInfo("item_sprites.png", 32, 80, 16, 16)

        // === BOSS SPRITES (boss_sprites.png) - Single row, 64px each ===
        "bomb_torizo" -> SpriteInfo("boss_sprites.png", 0, 0, 64, 64)
        "spore_spawn" -> SpriteInfo("boss_sprites.png", 64, 0, 64, 64)
        "kraid" -> SpriteInfo("boss_sprites.png", 128, 0, 64, 64)
        "crocomire" -> SpriteInfo("boss_sprites.png", 192, 0, 64, 64)
        "phantoon" -> SpriteInfo("boss_sprites.png", 256, 0, 64, 64)
        "botwoon" -> SpriteInfo("boss_sprites.png", 320, 0, 64, 64)
        "draygon" -> SpriteInfo("boss_sprites.png", 384, 0, 64, 64)
        "gold_torizo", "golden_torizo" -> SpriteInfo("boss_sprites.png", 448, 0, 64, 64)
        "ridley" -> SpriteInfo("boss_sprites.png", 512, 0, 64, 64)
        "mother_brain_1", "mb1", "mother_brain" -> SpriteInfo("boss_sprites.png", 576, 0, 64, 64)
        "mother_brain_2", "mb2" -> SpriteInfo("boss_sprites.png", 640, 0, 64, 64)
        "samus_ship", "ship" -> SpriteInfo("boss_sprites.png", 704, 0, 64, 64)
        "ceres_station" -> SpriteInfo("boss_sprites.png", 768, 0, 64, 64)
        "golden_four", "g4" -> SpriteInfo("boss_sprites.png", 832, 0, 64, 64)

        else -> {
            println("❌ No sprite mapping found for itemId: '$itemId'")
            null
        }
    }
}
