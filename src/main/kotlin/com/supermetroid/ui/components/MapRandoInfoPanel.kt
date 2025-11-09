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
    modifier: Modifier = Modifier
) {
    val fontSize by fontSizeService.fontSize.collectAsState()
    val settings by mapRandoDataService.settings.collectAsState()
    
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
            // Display real data from Map Rando API
            // Shows "OFF" when no seed detected or API fails
            
            MapRandoInfoItem(
                label = "OBJECTIVES",
                value = settings.objectives,
                valueColor = if (settings.objectives != "OFF") TrackerColors.Primary else TrackerColors.OnSurfaceVariant,
                labelSize = fontSize.labelSize,
                valueSize = fontSize.valueSize
            )
            
            MapRandoInfoItem(
                label = "DIFFICULTY",
                value = settings.difficulty,
                valueColor = if (settings.difficulty != "OFF") TrackerColors.Success else TrackerColors.OnSurfaceVariant,
                labelSize = fontSize.labelSize,
                valueSize = fontSize.valueSize
            )
            
            MapRandoInfoItem(
                label = "ITEM PROGRESSION",
                value = settings.itemProgression,
                valueColor = if (settings.itemProgression != "OFF") TrackerColors.Primary else TrackerColors.OnSurfaceVariant,
                labelSize = fontSize.labelSize,
                valueSize = fontSize.valueSize
            )
            
            MapRandoInfoItem(
                label = "QUALITY OF LIFE",
                value = settings.qualityOfLife,
                valueColor = if (settings.qualityOfLife != "OFF") TrackerColors.Warning else TrackerColors.OnSurfaceVariant,
                labelSize = fontSize.labelSize,
                valueSize = fontSize.valueSize
            )
            
            MapRandoInfoItem(
                label = "MAP LAYOUT",
                value = settings.mapLayout,
                valueColor = if (settings.mapLayout != "OFF") TrackerColors.Primary else TrackerColors.OnSurfaceVariant,
                labelSize = fontSize.labelSize,
                valueSize = fontSize.valueSize
            )
            
            MapRandoInfoItem(
                label = "DEATHS",
                value = settings.deathCount.toString(),
                valueColor = if (settings.deathCount > 0) TrackerColors.Error else TrackerColors.OnSurfaceVariant,
                labelSize = fontSize.labelSize,
                valueSize = fontSize.valueSize
            )
        }
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

