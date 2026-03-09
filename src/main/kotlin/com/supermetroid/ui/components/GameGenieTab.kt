package com.supermetroid.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.supermetroid.model.GameState
import com.supermetroid.service.ItemWriteService
import com.supermetroid.ui.gameStateService
import com.supermetroid.ui.theme.TrackerColors
import kotlinx.coroutines.launch

/**
 * Item/equipment management tab for toggling items, beams, and ammo via live SRAM writes.
 *
 * Reads from the existing GameStateService.trackerState flow (already polled every 200ms)
 * so NO duplicate memory reads are performed. Only writes go through ItemWriteService.
 */
@Composable
fun GameGenieTab(
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val itemWriteService = remember {
        ItemWriteService(gameStateService.getDualAdapter())
    }
    // Subscribe to existing tracker state — no extra memory reads
    val trackerState by gameStateService.trackerState.collectAsState()
    val gameState = trackerState.gameState
    val connected = trackerState.connection.connected

    var statusMessage by remember { mutableStateOf("") }
    var isApplying by remember { mutableStateOf(false) }

    fun doAction(action: suspend () -> Boolean, successMsg: String) {
        if (isApplying) return
        isApplying = true
        scope.launch {
            try {
                val success = action()
                statusMessage = if (success) successMsg else "Write failed — check connection"
            } catch (e: Exception) {
                statusMessage = "Error: ${e.message}"
            } finally {
                isApplying = false
            }
        }
    }

    Column(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Items & Equipment",
            style = MaterialTheme.typography.headlineSmall.copy(
                color = TrackerColors.Primary,
                fontWeight = FontWeight.Bold
            )
        )

        // WIP banner
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0x40FF4444)),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = "WIP — Writes are unreliable via SNI. Use at your own risk.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFFFF6666),
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        if (!connected) {
            Text(
                "Waiting for emulator connection...",
                style = MaterialTheme.typography.bodyMedium.copy(color = TrackerColors.OnSurfaceVariant)
            )
        }

        // Status message
        if (statusMessage.isNotBlank()) {
            Text(
                statusMessage,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (statusMessage.startsWith("Error") || statusMessage.contains("failed"))
                        TrackerColors.Error else TrackerColors.Success
                )
            )
        }

        // Quick actions row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ActionButton("Refill All", Modifier.weight(1f)) {
                doAction({ itemWriteService.refillAll(gameState) }, "Refilled all ammo & energy")
            }
            ActionButton("Give All Items", Modifier.weight(1f)) {
                doAction({ itemWriteService.giveAllItems() }, "All items equipped")
            }
        }

        // Items section — reads from existing GameState flow
        SectionCard("Items") {
            val itemStates = remember(gameState) { getItemStates(gameState) }
            itemStates.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for ((item, has) in row) {
                        ItemToggleChip(
                            label = item.displayName,
                            enabled = has,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                doAction(
                                    { itemWriteService.toggleItem(item, !has, gameState) },
                                    "${if (!has) "+" else "-"} ${item.displayName}"
                                )
                            }
                        )
                    }
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Beams section — reads from existing GameState flow
        SectionCard("Beams") {
            val beamStates = remember(gameState) { getBeamStates(gameState) }
            beamStates.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for ((beam, has) in row) {
                        ItemToggleChip(
                            label = beam.displayName,
                            enabled = has,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                doAction(
                                    { itemWriteService.toggleBeam(beam, !has, gameState) },
                                    "${if (!has) "+" else "-"} ${beam.displayName}"
                                )
                            }
                        )
                    }
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Ammo section — all values from existing GameState flow, writes only
        SectionCard("Ammo & Energy") {
            AmmoRow("Energy", gameState.health, gameState.maxHealth,
                onRefill = {
                    doAction({
                        itemWriteService.refillAmmo(ItemWriteService.CURRENT_ENERGY, gameState.maxHealth)
                    }, "Energy refilled to ${gameState.maxHealth}")
                },
                onSetMax = { value ->
                    doAction({
                        itemWriteService.setAmmo(
                            ItemWriteService.CURRENT_ENERGY, ItemWriteService.MAX_ENERGY,
                            value, value
                        )
                    }, "Energy set to $value")
                }
            )
            AmmoRow("Missiles", gameState.missiles, gameState.maxMissiles,
                onRefill = {
                    doAction({
                        itemWriteService.refillAmmo(ItemWriteService.CURRENT_MISSILES, gameState.maxMissiles)
                    }, "Missiles refilled to ${gameState.maxMissiles}")
                },
                onSetMax = { value ->
                    doAction({
                        itemWriteService.setAmmo(
                            ItemWriteService.CURRENT_MISSILES, ItemWriteService.MAX_MISSILES,
                            value, value
                        )
                    }, "Missiles set to $value")
                }
            )
            AmmoRow("Supers", gameState.supers, gameState.maxSupers,
                onRefill = {
                    doAction({
                        itemWriteService.refillAmmo(ItemWriteService.CURRENT_SUPERS, gameState.maxSupers)
                    }, "Supers refilled to ${gameState.maxSupers}")
                },
                onSetMax = { value ->
                    doAction({
                        itemWriteService.setAmmo(
                            ItemWriteService.CURRENT_SUPERS, ItemWriteService.MAX_SUPERS,
                            value, value
                        )
                    }, "Supers set to $value")
                }
            )
            AmmoRow("PBs", gameState.powerBombs, gameState.maxPowerBombs,
                onRefill = {
                    doAction({
                        itemWriteService.refillAmmo(ItemWriteService.CURRENT_POWER_BOMBS, gameState.maxPowerBombs)
                    }, "PBs refilled to ${gameState.maxPowerBombs}")
                },
                onSetMax = { value ->
                    doAction({
                        itemWriteService.setAmmo(
                            ItemWriteService.CURRENT_POWER_BOMBS, ItemWriteService.MAX_POWER_BOMBS,
                            value, value
                        )
                    }, "Power Bombs set to $value")
                }
            )
            AmmoRow("Reserve", gameState.reserveEnergy, gameState.maxReserveEnergy,
                onRefill = {
                    doAction({
                        itemWriteService.refillAmmo(ItemWriteService.CURRENT_RESERVE, gameState.maxReserveEnergy)
                    }, "Reserves refilled to ${gameState.maxReserveEnergy}")
                },
                onSetMax = { value ->
                    doAction({
                        itemWriteService.setAmmo(
                            ItemWriteService.CURRENT_RESERVE, ItemWriteService.MAX_RESERVE,
                            value, value
                        )
                    }, "Reserves set to $value")
                }
            )
        }
    }
}

/** Map GameState.Items booleans to ItemWriteService.Item enums for the UI grid. */
private fun getItemStates(gs: GameState): List<Pair<ItemWriteService.Item, Boolean>> = listOf(
    ItemWriteService.Item.MORPH_BALL to gs.items.morph,
    ItemWriteService.Item.BOMBS to gs.items.bombs,
    ItemWriteService.Item.SPRING_BALL to gs.items.spring,
    ItemWriteService.Item.SCREW_ATTACK to gs.items.screw,
    ItemWriteService.Item.VARIA_SUIT to gs.items.varia,
    ItemWriteService.Item.GRAVITY_SUIT to gs.items.gravity,
    ItemWriteService.Item.HI_JUMP_BOOTS to gs.items.hiJump,
    ItemWriteService.Item.SPACE_JUMP to gs.items.spaceJump,
    ItemWriteService.Item.SPEED_BOOSTER to gs.items.speed,
    ItemWriteService.Item.GRAPPLE_BEAM to gs.items.grapple,
    ItemWriteService.Item.X_RAY_SCOPE to gs.items.xray
)

/** Map GameState.Beams booleans to ItemWriteService.Beam enums for the UI grid. */
private fun getBeamStates(gs: GameState): List<Pair<ItemWriteService.Beam, Boolean>> = listOf(
    ItemWriteService.Beam.CHARGE to gs.beams.charge,
    ItemWriteService.Beam.ICE to gs.beams.ice,
    ItemWriteService.Beam.WAVE to gs.beams.wave,
    ItemWriteService.Beam.SPAZER to gs.beams.spazer,
    ItemWriteService.Beam.PLASMA to gs.beams.plasma
)

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TrackerColors.SurfaceOverlayLight),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = TrackerColors.Primary,
                    fontWeight = FontWeight.Bold
                )
            )
            content()
        }
    }
}

@Composable
private fun ItemToggleChip(
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(34.dp),
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (enabled) TrackerColors.Primary else TrackerColors.SurfaceVariant,
            contentColor = if (enabled) TrackerColors.OnPrimary else TrackerColors.OnSurface
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            maxLines = 1
        )
    }
}

@Composable
private fun ActionButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = TrackerColors.Primary,
            contentColor = TrackerColors.OnPrimary
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1
        )
    }
}

@Composable
private fun AmmoRow(
    label: String,
    current: Int,
    max: Int,
    onRefill: () -> Unit,
    onSetMax: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "$label: $current / $max",
            style = MaterialTheme.typography.bodySmall.copy(color = TrackerColors.OnSurface),
            modifier = Modifier.weight(1f)
        )

        SmallActionButton("Refill", onClick = onRefill)
        SmallActionButton("+10") { onSetMax(max + 10) }
        SmallActionButton("+50") { onSetMax(max + 50) }
    }
}

@Composable
private fun SmallActionButton(
    label: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(28.dp),
        shape = RoundedCornerShape(4.dp),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = TrackerColors.SurfaceVariant,
            contentColor = TrackerColors.OnSurface
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}
