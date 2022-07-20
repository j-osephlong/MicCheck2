package com.jlong.miccheck.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jlong.miccheck.TimeStamp
import com.jlong.miccheck.ui.theme.surfaceColorAtElevation

@Composable
fun TimestampListItem (
    timeStamp: TimeStamp,
    lastItem: Boolean,
    onClick: () -> Unit
) {
    val lineColor = MaterialTheme.colorScheme.secondary
    val bgColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            }
            .drawBehind {
                if (lastItem)
                    drawLine(
                        bgColor,
                        Offset(12.dp.toPx(), size.height/2),
                        Offset(12.dp.toPx(), size.height),
                        strokeWidth = 8.dp.toPx()
                    )
                drawCircle(
                    bgColor,
                    center = Offset(12.dp.toPx(), size.height/2),
                    radius = 8.dp.toPx(),
                    style = Fill
                )
                drawCircle(
                    lineColor,
                    center = Offset(12.dp.toPx(), size.height/2),
                    radius = 4.dp.toPx(),
                    style = Fill
                )
                drawCircle(
                    lineColor,
                    center = Offset(12.dp.toPx(), size.height/2),
                    radius = 8.dp.toPx(),
                    style = Stroke(width = 2.dp.toPx())
                )
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(28.dp))
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        ) {
            Text(
                timeStamp.timeMilli.toTimestamp(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(8.dp, 2.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                timeStamp.name,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                timeStamp.description.ifBlank { "This timestamp has no description." },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}