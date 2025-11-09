package com.supermetroid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermetroid.ui.theme.TrackerColors

/**
 * Map Rando info panel showing seed settings
 * Displayed on the right side when Map Rando icon view is active
 */
@Composable
fun MapRandoInfoPanel(
    fontSizeService: com.supermetroid.service.MapRandoInfoFontSizeService,
    mapRandoDataService: com.supermetroid.service.MapRandoDataService,
    mapRandoInfoConfigService: com.supermetroid.service.MapRandoInfoConfigService,
    modifier: Modifier = Modifier
) {
    val fontSize by fontSizeService.fontSize.collectAsState()
    val settings by mapRandoDataService.settings.collectAsState()
    val config by mapRandoInfoConfigService.config.collectAsState()
    
    // Get visible items in order
    val visibleItems = config.items.filter { it.visible }.sortedBy { it.order }
    
    Card(
        modifier = modifier.width(fontSize.panelWidth.dp), // Dynamic width based on font size
        colors = CardDefaults.cardColors(
            containerColor = TrackerColors.Surface
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Display only visible items in configured order
            visibleItems.forEach { item ->
                RenderMapRandoInfoItem(
                    itemId = item.id,
                    settings = settings,
                    labelSize = fontSize.labelSize,
                    valueSize = fontSize.valueSize
                )
            }
        }
    }
}

/**
 * Render a single Map Rando info item based on its ID
 */
@Composable
private fun RenderMapRandoInfoItem(
    itemId: String,
    settings: com.supermetroid.model.MapRandoSettings,
    labelSize: Int,
    valueSize: Int
) {
    when (itemId) {
        "objectives" -> MapRandoInfoItem(
            label = "OBJECTIVES",
            value = settings.objectives,
            valueColor = if (settings.objectives != "OFF") TrackerColors.Primary else TrackerColors.OnSurfaceVariant,
            labelSize = labelSize,
            valueSize = valueSize
        )
        "difficulty" -> MapRandoInfoItem(
            label = "DIFFICULTY",
            value = settings.difficulty,
            valueColor = if (settings.difficulty != "OFF") TrackerColors.Success else TrackerColors.OnSurfaceVariant,
            labelSize = labelSize,
            valueSize = valueSize
        )
        "itemProgression" -> MapRandoInfoItem(
            label = "ITEM PROGRESSION",
            value = settings.itemProgression,
            valueColor = if (settings.itemProgression != "OFF") TrackerColors.Primary else TrackerColors.OnSurfaceVariant,
            labelSize = labelSize,
            valueSize = valueSize
        )
        "qualityOfLife" -> MapRandoInfoItem(
            label = "QUALITY OF LIFE",
            value = settings.qualityOfLife,
            valueColor = if (settings.qualityOfLife != "OFF") TrackerColors.Warning else TrackerColors.OnSurfaceVariant,
            labelSize = labelSize,
            valueSize = valueSize
        )
        "mapLayout" -> MapRandoInfoItem(
            label = "MAP LAYOUT",
            value = settings.mapLayout,
            valueColor = if (settings.mapLayout != "OFF") TrackerColors.Primary else TrackerColors.OnSurfaceVariant,
            labelSize = labelSize,
            valueSize = valueSize
        )
        "deaths" -> MapRandoInfoItem(
            label = "DEATHS",
            value = settings.deathCount.toString(),
            valueColor = if (settings.deathCount > 0) TrackerColors.Error else TrackerColors.OnSurfaceVariant,
            labelSize = labelSize,
            valueSize = valueSize
        )
        "reloads" -> MapRandoInfoItem(
            label = "RELOADS",
            value = settings.reloadCount.toString(),
            valueColor = if (settings.reloadCount > 0) TrackerColors.Warning else TrackerColors.OnSurfaceVariant,
            labelSize = labelSize,
            valueSize = valueSize
        )
        "resets" -> MapRandoInfoItem(
            label = "RESETS",
            value = settings.resetCount.toString(),
            valueColor = if (settings.resetCount > 0) TrackerColors.OnSurfaceVariant else TrackerColors.OnSurfaceVariant.copy(alpha = 0.5f),
            labelSize = labelSize,
            valueSize = valueSize
        )
    }
}

@Composable
private fun MapRandoInfoItem(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
    labelSize: Int,
    valueSize: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Label
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = TrackerColors.OnSurfaceVariant,
                fontSize = labelSize.sp, // Dynamic font size
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.5.sp
            )
        )
        
        // Value
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                color = valueColor,
                fontSize = valueSize.sp, // Dynamic font size
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        )
    }
}

