package com.supermetroid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.supermetroid.model.AmmoNumberMode
import com.supermetroid.model.GameState
import com.supermetroid.model.RoomDatabase
import com.supermetroid.ui.theme.TrackerColors
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Get room name from room ID using RoomDatabase
 */
private fun getRoomName(roomId: Int): String {
    return RoomDatabase.getRoomById(roomId)?.name ?: ""
}

@Composable
private fun MapRandoGrid(
    gameState: GameState,
    iconSizeService: com.supermetroid.service.IconSizeService,
    ammoNumberModeService: com.supermetroid.service.IconViewModeService
) {
    val currentIconSize by iconSizeService.currentIconSize.collectAsState()
    val tile = currentIconSize.size.dp
    val spacing = 2.dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        // Column 1: bosses (4 tall, stretched to 5*tile height)
        val bossTileDim = ((tile * 5) + (spacing * 4) - (spacing * 3)) / 4
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing),
            modifier = Modifier.width(bossTileDim),
        ) {
            // Make four boss tiles stack to the exact height of five standard tiles
            // We have 3 internal spacings between 4 tiles; total column height should equal (5*tile + 4*spacing)
            listOf("kraid", "phantoon", "draygon", "ridley").forEach { id ->
                val sprite = getSpriteInfo(id)
                if (sprite != null) {
                    SpriteImageSized(
                        spriteFile = sprite.spriteFile,
                        spriteX = sprite.x,
                        spriteY = sprite.y,
                        spriteWidth = sprite.width,
                        spriteHeight = sprite.height,
                        displayWidth = bossTileDim,
                        displayHeight = bossTileDim,
                        isObtained = getBossObtained(gameState, id)
                    )
                } else {
                    Box(modifier = Modifier.size(bossTileDim, bossTileDim))
                }
            }
        }

        // Helper to render a vertical list of ids
        @Composable
        fun ColumnOf(ids: List<String>) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                ids.forEach { id ->
                    Box(modifier = Modifier.size(tile)) {
                        if (isAmmo(id)) {
                            val ammoMode by ammoNumberModeService.ammoNumberMode.collectAsState()
                            val (cur, max) = ammoValues(gameState, id)
                            val label = when (ammoMode) {
                                AmmoNumberMode.COUNT -> countFromMax(id, max)
                                else -> "$cur/$max"
                            }
                            val fontSize = when (ammoMode) {
                                AmmoNumberMode.COUNT -> 16.sp
                                else -> if (cur < 100 && max < 100) 14.sp
                                else 8.sp
                            }
                            val sprite = getSpriteInfo(id)
                            if (sprite != null) {
                                SpriteImage(
                                    spriteFile = sprite.spriteFile,
                                    spriteX = sprite.x,
                                    spriteY = sprite.y,
                                    spriteWidth = sprite.width,
                                    spriteHeight = sprite.height,
                                    displaySize = currentIconSize.size,
                                    isObtained = max > 0
                                )
                            }
                            if (cur > 0) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = fontSize,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    modifier = Modifier.align(Alignment.BottomEnd).padding(1.dp)
                                )
                            }
                        } else {
                            SpriteIcon(
                                itemId = id,
                                isObtained = getObtained(gameState, id),
                                size = currentIconSize.size
                            )
                        }
                    }
                }
                // pad to 5 rows if fewer
                val pad = 5 - ids.size
                repeat(maxOf(0, pad)) { Spacer(modifier = Modifier.size(tile)) }
            }
        }

        ColumnOf(listOf("charge", "ice", "wave", "spazer", "plasma"))
        ColumnOf(listOf("varia", "gravity", "grapple", "xray"))
        ColumnOf(listOf("morph_ball", "bombs", "spring"))
        ColumnOf(listOf("hijump", "space", "speed_booster", "screw"))
        ColumnOf(listOf("missiles", "supers", "powerbombs", "health", "reserve_tank"))
    }
}

// Utility lookups for Map Rando grid
private fun getBossObtained(gameState: GameState, id: String): Boolean = when (id) {
    "kraid" -> gameState.bosses.kraid
    "phantoon" -> gameState.bosses.phantoon
    "draygon" -> gameState.bosses.draygon
    "ridley" -> gameState.bosses.ridley
    else -> false
}

private fun getObtained(gameState: GameState, id: String): Boolean = when (id) {
    "morph_ball" -> gameState.items.morph
    "bombs" -> gameState.items.bombs
    "spring" -> gameState.items.spring
    "screw" -> gameState.items.screw
    "hijump" -> gameState.items.hiJump
    "space" -> gameState.items.spaceJump
    "speed_booster" -> gameState.items.speed
    "varia" -> gameState.items.varia
    "gravity" -> gameState.items.gravity
    "grapple" -> gameState.items.grapple
    "xray" -> gameState.items.xray
    "charge" -> gameState.beams.charge
    "ice" -> gameState.beams.ice
    "wave" -> gameState.beams.wave
    "spazer" -> gameState.beams.spazer
    "plasma" -> gameState.beams.plasma
    else -> false
}

private fun isAmmo(id: String) = id in setOf("health", "reserve_tank", "missiles", "supers", "powerbombs")

private fun ammoValues(gameState: GameState, id: String): Pair<Int, Int> = when (id) {
    "health" -> gameState.health to gameState.maxHealth
    "reserve_tank" -> gameState.reserveEnergy to gameState.maxReserveEnergy
    "missiles" -> gameState.missiles to gameState.maxMissiles
    "supers" -> gameState.supers to gameState.maxSupers
    "powerbombs" -> gameState.powerBombs to gameState.maxPowerBombs
    else -> 0 to 0
}

private fun countFromMax(id: String, max: Int): String = when (id) {
    "health", "reserve_tank" -> (max / 100).toString()
    "missiles", "supers", "powerbombs" -> (max / 5).toString()
    else -> "0"
}

@Composable
fun RoomNameDisplay(
    gameState: GameState,
    roomNameService: com.supermetroid.service.RoomNameService,
    modifier: Modifier = Modifier
) {
    val showRoomName by roomNameService.showRoomName.collectAsState()

    if (showRoomName) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = TrackerColors.Surface
            ),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = "${gameState.areaName} ${getRoomName(gameState.roomId)}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TrackerColors.Primary,
                    fontFamily = FontFamily.Monospace
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SimpleStatusGrid(
    gameState: GameState,
    iconConfigService: com.supermetroid.service.IconConfigService,
    iconSizeService: com.supermetroid.service.IconSizeService,
    iconViewModeService: com.supermetroid.service.IconViewModeService,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = TrackerColors.Surface
        ),
        shape = RoundedCornerShape(4.dp) // Reduced corner radius for more compact look
    ) {
        // Add vertical scroll support with max height constraint
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp) // Limit maximum height to prevent pushing UI off screen
                .verticalScroll(rememberScrollState()) // Make scrollable
                .padding(2.dp) // Minimal padding for very compact layout
        ) {
            val iconViewMode by iconViewModeService.iconViewMode.collectAsState()
            when (iconViewMode) {
                com.supermetroid.model.IconViewMode.DEFAULT -> {
                    // Unified fixed-size grid for enabled items and bosses in configured order
                    FixedTileGrid(
                        allItems = getFilteredItemsAndBosses(gameState, iconConfigService),
                        iconSizeService = iconSizeService,
                        ammoNumberModeService = iconViewModeService
                    )
                }
                com.supermetroid.model.IconViewMode.MAP_RANDO -> {
                    MapRandoGrid(
                        gameState = gameState,
                        iconSizeService = iconSizeService,
                        ammoNumberModeService = iconViewModeService
                    )
                }
                else -> { /* no-op */ }
            }
        }
    }
}

@Composable
fun FixedTileGrid(
    allItems: List<ItemStatus>,
    iconSizeService: com.supermetroid.service.IconSizeService,
    ammoNumberModeService: com.supermetroid.service.IconViewModeService
) {
    val currentIconSize by iconSizeService.currentIconSize.collectAsState()
    val fixedTileSize = currentIconSize.size.dp // Use configurable icon size
    val spacing = 1.dp // Further reduced spacing for more compact layout

    // FlowRow-style layout that fills from left, wraps naturally
    FlowRowLayout(
        items = allItems,
        tileSize = fixedTileSize,
        spacing = spacing,
        iconSizeService = iconSizeService,
        ammoNumberModeService = ammoNumberModeService
        )
}

@Composable
fun FlowRowLayout(
    items: List<ItemStatus>,
    tileSize: androidx.compose.ui.unit.Dp,
    spacing: androidx.compose.ui.unit.Dp,
    iconSizeService: com.supermetroid.service.IconSizeService,
    ammoNumberModeService: com.supermetroid.service.IconViewModeService
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
                                    val ammoMode by ammoNumberModeService.ammoNumberMode.collectAsState()
                                    // Render ammo tile content directly in the Box to match size
                                    Box(
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        // Ammo icon - use SpriteImage directly to avoid double border
                                        val spriteInfo = getSpriteInfo(item.id)
                                        if (spriteInfo != null) {
                                            Box(
                                                modifier = Modifier.align(Alignment.Center)
                                            ) {
                                                val currentIconSize by iconSizeService.currentIconSize.collectAsState()
                                                SpriteImage(
                                                    spriteFile = spriteInfo.spriteFile,
                                                    spriteX = spriteInfo.x,
                                                    spriteY = spriteInfo.y,
                                                    spriteWidth = spriteInfo.width,
                                                    spriteHeight = spriteInfo.height,
                                                    displaySize = currentIconSize.size,
                                                    isObtained = item.current > 0
                                                )
                                            }
                                        }

                                        val label = when (ammoMode) {
                                            AmmoNumberMode.AMOUNT -> "${item.current}/${item.max}"
                                            AmmoNumberMode.COUNT -> {
                                                // Derive count by unit sizes
                                                when (item.id) {
                                                    "health" -> (item.max / 100).toString() // 1 tank = 100 max health
                                                    "reserve_tank" -> (item.max / 100).toString()
                                                    "missiles" -> (item.max / 5).toString()
                                                    "supers" -> (item.max / 5).toString()
                                                    "powerbombs" -> (item.max / 5).toString()
                                                    else -> item.current.toString()
                                                }
                                            }
                                        }
                                        // Number in bottom right
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 8.sp,
                                                fontFamily = FontFamily.Monospace
                                            ),
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(1.dp)
                                        )
                                    }
                                } else {
                                    val currentIconSize by iconSizeService.currentIconSize.collectAsState()
                                    SpriteIcon(
                                        itemId = item.id,
                                        isObtained = item.isObtained,
                                        size = currentIconSize.size
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

// Filtered list of items and bosses based on icon configuration
@Composable
private fun getFilteredItemsAndBosses(
    gameState: GameState,
    iconConfigService: com.supermetroid.service.IconConfigService
): List<ItemStatus> {
    val iconConfig by iconConfigService.iconConfig.collectAsState()
    val enabledIcons = iconConfig.icons.filter { it.enabled }.sortedBy { it.order }
    val allItems = getAllItemsAndBosses(gameState)

    // Filter and reorder based on icon configuration
    return enabledIcons.mapNotNull { iconItem ->
        allItems.find { it.id == iconItem.id }
    }
}

// Unified list of all items and bosses
private fun getAllItemsAndBosses(gameState: GameState): List<ItemStatus> = listOf(
    // Ammo/Collectibles Row 1
    ItemStatus("health", "Energy Tanks", gameState.maxHealth > 99, gameState.health, gameState.maxHealth),
    ItemStatus("reserve_tank", "Reserve Tank", gameState.maxReserveEnergy > 0, gameState.reserveEnergy, gameState.maxReserveEnergy),
    ItemStatus("missiles", "Missiles", gameState.maxMissiles > 0, gameState.missiles, gameState.maxMissiles),
    ItemStatus("supers", "Super Missiles", gameState.maxSupers > 0, gameState.supers, gameState.maxSupers),
    ItemStatus("powerbombs", "Power Bombs", gameState.maxPowerBombs > 0, gameState.powerBombs, gameState.maxPowerBombs),

    ItemStatus("morph_ball", "Morph Ball", gameState.items.morph),
    ItemStatus("bombs", "Bombs", gameState.items.bombs),
    ItemStatus("charge", "Charge Beam", gameState.beams.charge),
    ItemStatus("spazer", "Spazer", gameState.beams.spazer),
    ItemStatus("varia", "Varia Suit", gameState.items.varia),
    ItemStatus("hijump", "Hi-Jump", gameState.items.hiJump),
    ItemStatus("speed_booster", "Speed Booster", gameState.items.speed),
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
    ItemStatus("crocomire", "Crocomire", gameState.bosses.crocomire),
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
