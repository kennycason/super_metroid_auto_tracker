package com.supermetroid.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermetroid.model.DeathEvent
import com.supermetroid.model.RoomDatabase
import com.supermetroid.ui.theme.TrackerColors

/** Compact counter beside the timer. Clicking it opens the timestamped death log. */
@Composable
fun DeathCounter(
    deaths: List<DeathEvent>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxHeight()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .clickable { expanded = !expanded },
            colors = CardDefaults.cardColors(containerColor = TrackerColors.Surface),
            shape = RoundedCornerShape(4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "☠",
                    color = if (deaths.isEmpty()) TrackerColors.OnSurfaceVariant else TrackerColors.Error,
                    // This glyph occupies much less of its em box than a digit,
                    // so it needs a larger font size to look equally prominent.
                    fontSize = 42.sp,
                    lineHeight = 42.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.offset(y = (-2).dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = deaths.size.toString(),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = if (deaths.isEmpty()) TrackerColors.OnSurface else TrackerColors.Error,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 260.dp, max = 380.dp).heightIn(max = 420.dp)
        ) {
            if (deaths.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No deaths this run") },
                    onClick = { expanded = false }
                )
            } else {
                deaths.forEachIndexed { index, death ->
                    val roomName = RoomDatabase.getRoomById(death.roomId)?.name
                    val roomId = "0x${death.roomId.toString(16).uppercase().padStart(4, '0')}"
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = "${index + 1}. ${formatDeathTime(death.runTimeMs)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = TrackerColors.Error,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = if (roomName != null) "$roomName · $roomId" else "Room $roomId",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TrackerColors.OnSurfaceVariant
                                    )
                                )
                            }
                        },
                        onClick = { expanded = false }
                    )
                }
            }
        }
    }
}

internal fun formatDeathTime(milliseconds: Long): String {
    val clamped = milliseconds.coerceAtLeast(0L)
    val totalSeconds = clamped / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val centiseconds = (clamped % 1000) / 10
    return "%d:%02d:%02d.%02d".format(hours, minutes, seconds, centiseconds)
}
