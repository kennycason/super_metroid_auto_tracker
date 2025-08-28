package com.supermetroid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermetroid.model.GameState
import com.supermetroid.ui.theme.TrackerColors
import kotlin.math.max

/**
 * Get room name from room ID, returns empty string if unknown
 */
private fun getRoomName(roomId: Int): String {
    return when (roomId) {
        0x91F8 -> "Landing Site"
        0x93AA -> "Crateria Power Bomb Room"
        0x93FE -> "West Ocean"
        0x94CC -> "Elevator to Maridia"
        0x95FF -> "Crateria Moat"
        0x962A -> "Elevator to Caterpillar"
        0x965B -> "Gauntlet Energy Tank Room"
        0x96BA -> "Climb"
        0x975C -> "Pit Room"
        0x97B5 -> "Elevator to Morph Ball"
        0x9804 -> "Bomb Torizo"
        0x990D -> "Terminator"
        0x9938 -> "Elevator to Green Brinstar"
        0x99BD -> "Green Pirate Shaft"
        0x99F9 -> "Crateria Supers Room"
        0x9A90 -> "The Final Missile"
        0x9AD9 -> "Green Brinstar Main Shaft"
        0x9B5B -> "Spore Spawn Super"
        0x9BC8 -> "Early Supers"
        0x9C07 -> "Brinstar Reserve Room"
        0x9D19 -> "Big Pink"
        0x9D9C -> "Spore Spawn Keyhunter"
        0x9DC7 -> "Spore Spawn"
        0x9E11 -> "Pink Brinstar Power Bomb Room"
        0x9E52 -> "Green Hills"
        0x9FBA -> "Noob Bridge"
        0x9E9F -> "Morph Ball"
        0x9F64 -> "Blue Brinstar Energy Tank Room"
        0xA011 -> "Etacoon Energy Tank Room"
        0xA051 -> "Etacoon Super Room"
        0xA0D2 -> "Waterway"
        0xA107 -> "Alpha Missile Room"
        0xA15B -> "Hopper Energy Tank Room"
        0xA1D8 -> "Billy Mays"
        0xA253 -> "Red Tower"
        0xA2CE -> "X-Ray"
        0xA322 -> "Caterpillar"
        0xA37C -> "Beta Power Bomb Room"
        0xA3AE -> "Alpha Power Bombs Room"
        0xA3DD -> "Bat"
        0xA447 -> "Spazer"
        0xA4B1 -> "Warehouse Energy Tank Room"
        0xA471 -> "Warehouse Zeela"
        0xA4DA -> "Warehouse Ki Hunters"
        0xA56B -> "Kraid Eye Door"
        0xA59F -> "Kraid"
        0xA5ED -> "Statues Hallway"
        0xA66A -> "Statues"
        0xA6A1 -> "Warehouse Entrance"
        0xA6E2 -> "Varia"
        0xA788 -> "Cathedral"
        0xA7DE -> "Business Center"
        0xA890 -> "Ice Beam"
        0xA8F8 -> "Crumble Shaft"
        0xA923 -> "Crocomire Speedway"
        0xA98D -> "Crocomire"
        0xA9E5 -> "Hi Jump"
        0xAA0E -> "Crocomire Escape"
        0xAA41 -> "Hi Jump Shaft"
        0xAADE -> "Post Crocomire Power Bomb Room"
        0xAB3B -> "Cosine Room"
        0xAB8F -> "Pre Grapple"
        0xAC2B -> "Grapple"
        0xAC5A -> "Norfair Reserve Room"
        0xAC83 -> "Green Bubbles Room"
        0xACB3 -> "Bubble Mountain"
        0xACF0 -> "Speed Boost Hall"
        0xAD1B -> "Speed Booster"
        0xAD5E -> "Single Chamber"
        0xADAD -> "Double Chamber"
        0xADDE -> "Wave Beam"
        0xAE32 -> "Volcano"
        0xAE74 -> "Kronic Boost"
        0xAEB4 -> "Magdollite Tunnel"
        0xAF3F -> "Lower Norfair Elevator"
        0xAFA3 -> "Rising Tide"
        0xAFFB -> "Spiky Acid Snakes"
        0xB1E5 -> "Acid Statue"
        0xB236 -> "Main Hall"
        0xB283 -> "Golden Torizo"
        0xB32E -> "Ridley"
        0xB37A -> "Lower Norfair Farming"
        0xB40A -> "Mickey Mouse"
        0xB457 -> "Pillars"
        0xB4AD -> "Worst Room in the Game"
        0xB4E5 -> "Amphitheatre"
        0xB510 -> "Lower Norfair Spring Maze"
        0xB55A -> "Lower Norfair Escape Power Bomb Room"
        0xB585 -> "Red Ki Shaft"
        0xB5D5 -> "Wasteland"
        0xB62B -> "Metal Pirates"
        0xB656 -> "Three Musketeers"
        0xB698 -> "Ridley Energy Tank Room"
        0xB6C1 -> "Screw Attack"
        0xB6EE -> "Lower Norfair Fireflea"
        0xC98E -> "Bowling"
        0xCA08 -> "Wrecked Ship Entrance"
        0xCA52 -> "Attic"
        0xCAAE -> "Attic Worker Robot Room"
        0xCAF6 -> "Wrecked Ship Main Shaft"
        0xCC27 -> "Wrecked Ship Energy Tank Room"
        0xCC6F -> "Basement"
        0xCD13 -> "Phantoon"
        0xCDA8 -> "Wrecked Ship Left Super Room"
        0xCDF1 -> "Wrecked Ship Right Super Room"
        0xCE40 -> "Gravity"
        0xCEFB -> "Glass Tunnel"
        0xCFC9 -> "Main Street"
        0xD055 -> "Mama Turtle"
        0xD13B -> "Watering Hole"
        0xD1DD -> "Beach"
        0xD2AA -> "Plasma Beam"
        0xD30B -> "Maridia Elevator"
        0xD340 -> "Plasma Spark"
        0xD408 -> "Toilet Bowl"
        0xD48E -> "Oasis"
        0xD4EF -> "Left Sand Pit"
        0xD51E -> "Right Sand Pit"
        0xD5A7 -> "Aqueduct"
        0xD5EC -> "Butterfly Room"
        0xD617 -> "Botwoon Hallway"
        0xD6D0 -> "Spring Ball"
        0xD78F -> "Precious"
        0xD7E4 -> "Botwoon Energy Tank Room"
        0xD95E -> "Botwoon"
        0xD9AA -> "Space Jump"
        0xD9FE -> "West Cactus Alley"
        0xDA60 -> "Draygon"
        0xDAAE -> "Tourian Elevator"
        0xDAE1 -> "Metroid One"
        0xDB31 -> "Metroid Two"
        0xDB7D -> "Metroid Three"
        0xDBCD -> "Metroid Four"
        0xDC65 -> "Dust Torizo"
        0xDC19 -> "Tourian Hopper"
        0xDDC4 -> "Tourian Eye Door"
        0xDCB1 -> "Big Boy"
        0xDD58 -> "Mother Brain"
        0xDDF3 -> "Rinka Shaft"
        0xDEDE -> "Tourian Escape 4"
        0xDF45 -> "Ceres Elevator"
        0xE06B -> "Flat Room"
        0xE0B5 -> "Ceres Ridley"
        else -> ""
    }
}

@Composable
fun SimpleStatusGrid(
    gameState: GameState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = TrackerColors.Surface
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp) // Halved from 16dp to 8dp for more compact layout
        ) {


            // Area info
            Text(
                text = "${gameState.areaName} ${getRoomName(gameState.roomId)}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TrackerColors.Primary,
                    fontFamily = FontFamily.Monospace
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp)) // Halved from 16dp to 8dp

            // Unified fixed-size grid for all items and bosses
            FixedTileGrid(
                allItems = getAllItemsAndBosses(gameState)
            )
        }
    }
}

@Composable
fun FixedTileGrid(
    allItems: List<ItemStatus>
) {
    val fixedTileSize = 52.dp // FIXED SIZE - never changes
    val spacing = 4.dp

    // FlowRow-style layout that fills from left, wraps naturally
    FlowRowLayout(
        items = allItems,
        tileSize = fixedTileSize,
        spacing = spacing
        )
}

@Composable
fun FlowRowLayout(
    items: List<ItemStatus>,
    tileSize: androidx.compose.ui.unit.Dp,
    spacing: androidx.compose.ui.unit.Dp
) {
    // Custom layout that behaves like CSS flexbox
    // Items flow left-to-right, wrap to next row when needed

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth()
    ) {
        val maxWidth = maxWidth
        val tileWithSpacing = tileSize + spacing
        val tilesPerRow = max(1, ((maxWidth + spacing) / tileWithSpacing).toInt())
        val totalRows = (items.size + tilesPerRow - 1) / tilesPerRow

        Column(
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            for (rowIndex in 0 until totalRows) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (colIndex in 0 until tilesPerRow) {
                        val itemIndex = rowIndex * tilesPerRow + colIndex
                        if (itemIndex < items.size) {
                            val item = items[itemIndex]
                            // Fixed size tile that NEVER changes
                            Box(
                                modifier = Modifier.size(tileSize) // Force exact size
                            ) {
                                // Check if this is an ammo item (has current/max values)
                                if (item.max > 0) {
                                    // Render ammo tile content directly in the Box to match size
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .border(
                                                2.dp,
                                                if (item.isObtained) TrackerColors.Success else TrackerColors.Border,
                                                RoundedCornerShape(4.dp)
                                            )
                                            .background(
                                                if (item.isObtained) TrackerColors.Success.copy(alpha = 0.2f) else TrackerColors.Surface,
                                                RoundedCornerShape(4.dp)
                                            )
                                    ) {
                                        // Ammo icon - use SpriteImage directly to avoid double border
                                        val spriteInfo = getSpriteInfo(item.id)
                                        if (spriteInfo != null) {
                                            Box(
                                                modifier = Modifier.align(Alignment.Center)
                                            ) {
                                                SpriteImage(
                                                    spriteFile = spriteInfo.spriteFile,
                                                    spriteX = spriteInfo.x,
                                                    spriteY = spriteInfo.y,
                                                    spriteWidth = spriteInfo.width,
                                                    spriteHeight = spriteInfo.height,
                                                    displaySize = 44, // Slightly smaller to fit within border
                                                    isObtained = item.current > 0
                                                )
                                            }
                                        }

                                        // Total in bottom right
                                        Text(
                                            text = "${item.current}/${item.max}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 8.sp,
                                                fontFamily = FontFamily.Monospace
                                            ),
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(2.dp)
                                        )
                                    }
                                } else {
                                    SpriteIcon(
                                        itemId = item.id,
                                        isObtained = item.isObtained,
                                        size = 52 // Match the fixed tile size
                                    )
                                }
                            }
                        } else {
                            // Empty space for incomplete rows
                            Spacer(modifier = Modifier.size(tileSize))
                        }
                    }
                }
            }
        }
    }
}



data class ItemStatus(
    val id: String,
    val name: String,
    val isObtained: Boolean,
    val current: Int = 0,
    val max: Int = 0
)

// Helper function for empty slots
private fun ItemStatus.isEmpty(): Boolean = id.isEmpty()

// Unified list of all items and bosses
private fun getAllItemsAndBosses(gameState: GameState): List<ItemStatus> = listOf(
    // Ammo/Collectibles Row 1
    ItemStatus("health", "Energy Tanks", gameState.maxHealth > 99, gameState.health, gameState.maxHealth),
    ItemStatus("reserve_tank", "Reserve Tank", gameState.maxReserveEnergy > 0, gameState.reserveEnergy, gameState.maxReserveEnergy),
    ItemStatus("missiles", "Missiles", gameState.maxMissiles > 0, gameState.missiles, gameState.maxMissiles),
    ItemStatus("supers", "Super Missiles", gameState.maxSupers > 0, gameState.supers, gameState.maxSupers),
    ItemStatus("powerbombs", "Power Bombs", gameState.maxPowerBombs > 0, gameState.powerBombs, gameState.maxPowerBombs),

    ItemStatus("morph", "Morph Ball", gameState.items.morph),
    ItemStatus("bombs", "Bombs", gameState.items.bombs),
    ItemStatus("charge", "Charge Beam", gameState.beams.charge),
    ItemStatus("spazer", "Spazer", gameState.beams.spazer),
    ItemStatus("varia", "Varia Suit", gameState.items.varia),
    ItemStatus("hijump", "Hi-Jump", gameState.items.hiJump),
    ItemStatus("speed", "Speed Booster", gameState.items.speed),
    ItemStatus("wave", "Wave Beam", gameState.beams.wave),
    ItemStatus("ice", "Ice Beam", gameState.beams.ice),
    ItemStatus("gravity", "Gravity Suit", gameState.items.gravity),
    ItemStatus("space", "Space Jump", gameState.items.spaceJump),
    ItemStatus("spring", "Spring Ball", gameState.items.spring),
    ItemStatus("screw", "Screw Attack", gameState.items.screw),
    ItemStatus("plasma", "Plasma Beam", gameState.beams.plasma),
    ItemStatus("grapple", "Grappling Beam", gameState.items.grapple),
    ItemStatus("xray", "X-Ray Scope", gameState.items.xray),

    ItemStatus("ceres_station", "Ceres Station", gameState.bosses.ceresStation),
    ItemStatus("bomb_torizo", "Bomb Torizo", gameState.bosses.bombTorizo),
    ItemStatus("spore_spawn", "Spore Spawn", gameState.bosses.sporeSpawn),
    ItemStatus("kraid", "Kraid", gameState.bosses.kraid),
    ItemStatus("phantoon", "Phantoon", gameState.bosses.phantoon),
    ItemStatus("botwoon", "Botwoon", gameState.bosses.botwoon),
    ItemStatus("draygon", "Draygon", gameState.bosses.draygon),
    ItemStatus("gold_torizo", "Gold Torizo", gameState.bosses.goldenTorizo),
    ItemStatus("ridley", "Ridley", gameState.bosses.ridley),
    ItemStatus("golden_four", "G4", gameState.bosses.kraid && gameState.bosses.phantoon && gameState.bosses.draygon && gameState.bosses.ridley),
    ItemStatus("mother_brain_1", "Mother Brain 1", gameState.bosses.motherBrain1),
    ItemStatus("mother_brain_2", "Mother Brain 2", gameState.bosses.motherBrain2),
    // ItemStatus("mother_brain_3", "Mother Brain 3", gameState.bosses.motherBrain),
    ItemStatus("ship", "Ship", gameState.bosses.samusShip)
)
