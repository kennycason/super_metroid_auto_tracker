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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermetroid.model.GameState
import com.supermetroid.ui.theme.TrackerColors

data class StatusItem(
    val id: String,
    val name: String,
    val shortName: String,
    val category: String,
    val isObtained: Boolean,
    val count: Int? = null,
    val maxCount: Int? = null
)

@Composable
fun StatusGrid(
    gameState: GameState,
    modifier: Modifier = Modifier,
    showItemNames: Boolean = true
) {
    val statusItems = remember(gameState) {
        createStatusItems(gameState)
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = TrackerColors.Surface
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    TrackerColors.Border,
                    RoundedCornerShape(4.dp)
                )
                .padding(8.dp) // Reduced padding for more compact layout
        ) {
            // Title
            Text(
                text = "GAME STATUS",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TrackerColors.Primary,
                    letterSpacing = 1.sp
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Stats row - Health, Missiles, etc.
            StatsRow(gameState)
            
            Spacer(modifier = Modifier.height(8.dp)) // Reduced spacing
            
            // Location info
            LocationInfo(gameState)
            
            Spacer(modifier = Modifier.height(8.dp)) // Reduced spacing
            
            // Items and beams grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp), // Reduced spacing
                verticalArrangement = Arrangement.spacedBy(2.dp), // Reduced spacing
                modifier = Modifier.height(200.dp)
            ) {
                items(statusItems) { item ->
                    StatusItemCard(
                        item = item,
                        showName = showItemNames
                    )
                }
            }
            
            // Bosses section
            Spacer(modifier = Modifier.height(8.dp))
            BossesSection(gameState)
        }
    }
}

@Composable
private fun StatsRow(gameState: GameState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem("HEALTH", "${gameState.health}/${gameState.maxHealth}")
        StatItem("MISSILES", "${gameState.missiles}/${gameState.maxMissiles}")
        StatItem("SUPERS", "${gameState.supers}/${gameState.maxSupers}")
        StatItem("PBs", "${gameState.powerBombs}/${gameState.maxPowerBombs}")
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                color = TrackerColors.OnSurfaceVariant,
                fontSize = 10.sp
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = TrackerColors.Primary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        )
    }
}

@Composable
private fun LocationInfo(gameState: GameState) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = TrackerColors.SurfaceOverlayLight
        ),
        shape = RoundedCornerShape(3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "AREA:",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = TrackerColors.OnSurfaceVariant
                )
            )
            Text(
                text = "${gameState.areaName} (${gameState.roomId})",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TrackerColors.Primary,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun StatusItemCard(
    item: StatusItem,
    showName: Boolean
) {
    Box(
        modifier = Modifier
            .size(width = 76.dp, height = if (showName) 60.dp else 40.dp)
            .padding(2.dp), // Reduced padding for more compact layout
        contentAlignment = Alignment.Center
    ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Item symbol/initial
                Text(
                    text = item.shortName,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = if (item.isObtained) TrackerColors.Success else TrackerColors.Inactive,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    textAlign = TextAlign.Center
                )
                
                // Count if applicable
                if (item.count != null && item.maxCount != null) {
                    Text(
                        text = "${item.count}/${item.maxCount}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (item.isObtained) TrackerColors.Success else TrackerColors.Inactive,
                            fontSize = 8.sp
                        )
                    )
                }
                
                // Name if enabled
                if (showName) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (item.isObtained) TrackerColors.Success else TrackerColors.Inactive,
                            fontSize = 8.sp
                        ),
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
        }
    }
}

@Composable
private fun BossesSection(gameState: GameState) {
    val bosses = listOf(
        "Ceres" to gameState.bosses.ceresStation,
        "Bomb Torizo" to gameState.bosses.bombTorizo,
        "Kraid" to gameState.bosses.kraid,
        "Phantoon" to gameState.bosses.phantoon,
        "Draygon" to gameState.bosses.draygon,
        "Ridley" to gameState.bosses.ridley,
        "MB1" to gameState.bosses.motherBrain1,
        "MB2" to gameState.bosses.motherBrain2,
        "Ship" to gameState.bosses.samusShip
    )
    
    Column {
        Text(
            text = "BOSSES",
            style = MaterialTheme.typography.labelLarge.copy(
                color = TrackerColors.Primary,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(2.dp), // Reduced spacing
            verticalArrangement = Arrangement.spacedBy(2.dp), // Reduced spacing
            modifier = Modifier.height(80.dp)
        ) {
            items(bosses) { (name, defeated) ->
                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .padding(1.dp), // Minimal padding for compact layout
                    contentAlignment = Alignment.Center
                ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (defeated) TrackerColors.Success else TrackerColors.Inactive,
                                fontWeight = if (defeated) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 9.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                }
            }
        }
    }
}

private fun createStatusItems(gameState: GameState): List<StatusItem> {
    return listOf(
        // Items
        StatusItem("morph", "Morph Ball", "MORPH", "item", gameState.items.morph),
        StatusItem("bombs", "Bombs", "BOMB", "item", gameState.items.bombs),
        StatusItem("varia", "Varia Suit", "VARIA", "item", gameState.items.varia),
        StatusItem("gravity", "Gravity Suit", "GRAV", "item", gameState.items.gravity),
        StatusItem("hijump", "Hi-Jump", "HI-J", "item", gameState.items.hiJump),
        StatusItem("speed", "Speed Booster", "SPEED", "item", gameState.items.speed),
        StatusItem("space", "Space Jump", "SPACE", "item", gameState.items.spaceJump),
        StatusItem("screw", "Screw Attack", "SCREW", "item", gameState.items.screw),
        StatusItem("spring", "Spring Ball", "SPRING", "item", gameState.items.spring),
        StatusItem("grapple", "Grappling Beam", "GRAP", "item", gameState.items.grapple),
        StatusItem("xray", "X-Ray Scope", "X-RAY", "item", gameState.items.xray),
        
        // Beams
        StatusItem("charge", "Charge Beam", "CHARGE", "beam", gameState.beams.charge),
        StatusItem("ice", "Ice Beam", "ICE", "beam", gameState.beams.ice),
        StatusItem("wave", "Wave Beam", "WAVE", "beam", gameState.beams.wave),
        StatusItem("spazer", "Spazer", "SPAZER", "beam", gameState.beams.spazer),
        StatusItem("plasma", "Plasma Beam", "PLASMA", "beam", gameState.beams.plasma),
        StatusItem("hyper", "Hyper Beam", "HYPER", "beam", gameState.beams.hyper)
    )
}
