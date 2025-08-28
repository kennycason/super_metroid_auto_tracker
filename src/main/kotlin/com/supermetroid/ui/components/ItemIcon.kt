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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermetroid.ui.theme.TrackerColors

data class ItemSpriteInfo(
    val id: String,
    val name: String,
    val spriteFile: String, // "item_sprites.png" or "boss_sprites.png"
    val x: Int, // X position in sprite sheet
    val y: Int, // Y position in sprite sheet
    val width: Int = 32,
    val height: Int = 32
)

// Item sprite coordinates from the original tracker
val ITEM_SPRITES = mapOf(
    "morph" to ItemSpriteInfo("morph", "Morph Ball", "item_sprites.png", 0, 0),
    "bombs" to ItemSpriteInfo("bombs", "Bombs", "item_sprites.png", 32, 0),
    "varia" to ItemSpriteInfo("varia", "Varia Suit", "item_sprites.png", 64, 0),
    "gravity" to ItemSpriteInfo("gravity", "Gravity Suit", "item_sprites.png", 96, 0),
    "hijump" to ItemSpriteInfo("hijump", "Hi-Jump", "item_sprites.png", 128, 0),
    "speed" to ItemSpriteInfo("speed", "Speed Booster", "item_sprites.png", 160, 0),
    "space" to ItemSpriteInfo("space", "Space Jump", "item_sprites.png", 192, 0),
    "screw" to ItemSpriteInfo("screw", "Screw Attack", "item_sprites.png", 224, 0),
    "spring" to ItemSpriteInfo("spring", "Spring Ball", "item_sprites.png", 0, 32),
    "grapple" to ItemSpriteInfo("grapple", "Grappling Beam", "item_sprites.png", 32, 32),
    "xray" to ItemSpriteInfo("xray", "X-Ray Scope", "item_sprites.png", 64, 32),
    "charge" to ItemSpriteInfo("charge", "Charge Beam", "item_sprites.png", 96, 32),
    "ice" to ItemSpriteInfo("ice", "Ice Beam", "item_sprites.png", 128, 32),
    "wave" to ItemSpriteInfo("wave", "Wave Beam", "item_sprites.png", 160, 32),
    "spazer" to ItemSpriteInfo("spazer", "Spazer", "item_sprites.png", 192, 32),
    "plasma" to ItemSpriteInfo("plasma", "Plasma Beam", "item_sprites.png", 224, 32)
)

val BOSS_SPRITES = mapOf(
    "ceres" to ItemSpriteInfo("ceres", "Ceres Station", "boss_sprites.png", 0, 0),
    "kraid" to ItemSpriteInfo("kraid", "Kraid", "boss_sprites.png", 32, 0),
    "phantoon" to ItemSpriteInfo("phantoon", "Phantoon", "boss_sprites.png", 64, 0),
    "draygon" to ItemSpriteInfo("draygon", "Draygon", "boss_sprites.png", 96, 0),
    "ridley" to ItemSpriteInfo("ridley", "Ridley", "boss_sprites.png", 128, 0),
    "mother_brain" to ItemSpriteInfo("mother_brain", "Mother Brain", "boss_sprites.png", 160, 0)
)

@Composable
fun ItemIcon(
    itemId: String,
    isObtained: Boolean,
    modifier: Modifier = Modifier,
    size: Int = 48,
    showName: Boolean = false
) {
    val spriteInfo = ITEM_SPRITES[itemId] ?: BOSS_SPRITES[itemId]
    
    Card(
        modifier = modifier.size(size.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isObtained) {
                TrackerColors.Success.copy(alpha = 0.2f)
            } else {
                TrackerColors.SurfaceOverlayLight
            }
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    2.dp,
                    if (isObtained) TrackerColors.Success else TrackerColors.Border,
                    RoundedCornerShape(4.dp)
                )
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (spriteInfo != null) {
                // For now, use a simple colored box until we implement sprite cropping
                // In a real implementation, you'd crop the sprite sheet here
                Box(
                    modifier = Modifier
                        .size((size - 16).dp)
                        .background(
                            if (isObtained) TrackerColors.Success else TrackerColors.Inactive,
                            RoundedCornerShape(2.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Simple text representation for now
                    Text(
                        text = spriteInfo.name.take(3).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isObtained) TrackerColors.Background else TrackerColors.OnSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Fallback text
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
}
