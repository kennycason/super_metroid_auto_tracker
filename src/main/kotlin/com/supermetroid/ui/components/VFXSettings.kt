package com.supermetroid.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.supermetroid.ui.theme.TrackerColors

@Composable
fun VFXSettingsTab(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Visual Effects",
            style = MaterialTheme.typography.headlineSmall.copy(
                color = TrackerColors.Primary,
                fontWeight = FontWeight.Bold
            )
        )
        
        Text(
            text = "Visual effects and animations for game events.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = TrackerColors.OnSurfaceVariant
            )
        )
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = TrackerColors.SurfaceOverlayLight
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Coming Soon",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = TrackerColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                )
                
                Text(
                    text = "• Animated boss death overlays\n• Speed booster visual effects\n• Custom particle effects for item collection\n• Screen transitions and animations",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TrackerColors.OnSurfaceVariant
                    )
                )
            }
        }
    }
}

