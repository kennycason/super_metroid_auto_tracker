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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(130.dp),
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
            // TODO: Read seed name from memory ($dffef0)
            // TODO: Fetch full metadata from maprando.com API
            // For now, show placeholders
            
            MapRandoInfoItem(
                label = "OBJECTIVES",
                value = "BOSSES",
                valueColor = TrackerColors.OnSurface
            )
            
            MapRandoInfoItem(
                label = "DIFFICULTY",
                value = "BASIC",
                valueColor = TrackerColors.Primary
            )
            
            MapRandoInfoItem(
                label = "ITEM PROGRESSION",
                value = "NORMAL",
                valueColor = TrackerColors.Primary
            )
            
            MapRandoInfoItem(
                label = "QUALITY OF LIFE",
                value = "OFF",
                valueColor = TrackerColors.OnSurfaceVariant
            )
            
            MapRandoInfoItem(
                label = "MAP LAYOUT",
                value = "VANILLA",
                valueColor = TrackerColors.OnSurface
            )
        }
    }
}

@Composable
private fun MapRandoInfoItem(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
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
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.5.sp
            )
        )
        
        // Value
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                color = valueColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        )
    }
}

