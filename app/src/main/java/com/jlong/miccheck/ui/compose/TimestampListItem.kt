package com.jlong.miccheck.ui.compose

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jlong.miccheck.Recording
import com.jlong.miccheck.TimeStamp
import com.jlong.miccheck.ui.theme.MicCheckTheme
import com.jlong.miccheck.ui.theme.surfaceColorAtElevation
import java.time.LocalDateTime

@Preview
@Composable
fun TimestampListItemPreview () {
    val t = TimeStamp(
        5000,
        "Test Timestamp",
    "Heyo!",
    ""
    )

    MicCheckTheme {
        Surface {
            TimestampListItem(timeStamp = t, lastItem = false) {

            }
        }
    }
}
@Preview
@Composable
fun TimestampViewRecordingElmPreview () {
    val r = Recording(
        Uri.EMPTY,
        "Test Recording",
        36000,
        0,
        "",
        LocalDateTime.now(),
        ""
    )


    MicCheckTheme {
        val lineColor = MaterialTheme.colorScheme.secondary
        Surface (
                ) {
            Column(
                modifier = Modifier.drawBehind {
                    val pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(
                            8.dp.toPx(),
                            6.dp.toPx()
                        ), 0f
                    )
                    drawLine(
                        color = lineColor,
                        start = Offset(12.dp.toPx(), 0f),
                        end = Offset(12.dp.toPx(), size.height),
                        pathEffect = pathEffect,
                        strokeWidth = 2.dp.toPx()
                    )
                }) {

                Spacer(Modifier.height(18.dp))
                TimestampViewRecordingElm(recording = r, timePosition = 36000L, lastItem = false, onClick = {}, isPlaying = false) {

                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimestampViewRecordingElm (
    recording: Recording,
    timePosition: Long,
    lastItem: Boolean,
    backgroundColor: Color? = null,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onClickOpen: () -> Unit
) {
    val dotColor by animateColorAsState(if (!isPlaying) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary)
    val surfaceColor by animateColorAsState(if (!isPlaying) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer)
    val onSurfaceColor by animateColorAsState(if (!isPlaying) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer)
    val bgColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                if (lastItem)
                    drawLine(
                        backgroundColor ?: bgColor,
                        Offset(12.dp.toPx(), size.height / 2),
                        Offset(12.dp.toPx(), size.height),
                        strokeWidth = 8.dp.toPx()
                    )
                drawCircle(
                    backgroundColor ?: bgColor,
                    center = Offset(12.dp.toPx(), size.height / 2),
                    radius = 8.dp.toPx(),
                    style = Fill
                )
                drawCircle(
                    dotColor,
                    center = Offset(12.dp.toPx(), size.height / 2),
                    radius = 4.dp.toPx(),
                    style = Fill
                )
                drawCircle(
                    dotColor,
                    center = Offset(12.dp.toPx(), size.height / 2),
                    radius = 8.dp.toPx(),
                    style = Stroke(width = 2.dp.toPx())
                )
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(28.dp))
        Surface(
            shape = MaterialTheme.shapes.large,
            color = surfaceColor,
            contentColor = onSurfaceColor,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            onClick = onClick
        ) {
            Row (
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp, 8.dp),
                verticalAlignment = Alignment.CenterVertically){
                Text(
                    timePosition.toTimestamp(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        recording.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        recording.duration.toLong().toTimestamp(),
                    )
                }
                Spacer(modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f))
                IconButton(onClick = onClickOpen) {
                    Icon(Icons.Rounded.ArrowForward, null)
                }
            }
        }
//        Spacer(Modifier.width(12.dp))
//        Column {
//            Text(
//                timeStamp.name,
//                style = MaterialTheme.typography.titleMedium
//            )
//            Spacer(Modifier.height(4.dp))
//            Text(
//                timeStamp.description.ifBlank { "This timestamp has no description." },
//                maxLines = 2,
//                overflow = TextOverflow.Ellipsis
//            )
//        }
    }
}

@Composable
fun TimestampListItem (
    timeStamp: TimeStamp,
    lastItem: Boolean,
    backgroundColor: Color? = null,
    focused: Boolean = false,
    onClick: () -> Unit
) {
    val timestampFontSize by animateFloatAsState(
        if (focused)
            MaterialTheme.typography.headlineSmall.fontSize.value
        else
            MaterialTheme.typography.labelMedium.fontSize.value
    )
    val timestampExtraPadding by animateDpAsState(
        if (focused)
            2.dp
        else
            0.dp
    )
    val timestampColor by animateColorAsState(
        if (focused)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.secondary
    )
    val timestampContentColor by animateColorAsState(
        if (focused)
            MaterialTheme.colorScheme.onPrimary
        else
            MaterialTheme.colorScheme.onSecondary
    )
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
                        backgroundColor ?: bgColor,
                        Offset(12.dp.toPx(), size.height / 2),
                        Offset(12.dp.toPx(), size.height),
                        strokeWidth = 8.dp.toPx()
                    )
                drawCircle(
                    backgroundColor ?: bgColor,
                    center = Offset(12.dp.toPx(), size.height / 2),
                    radius = 8.dp.toPx(),
                    style = Fill
                )
                drawCircle(
                    lineColor,
                    center = Offset(12.dp.toPx(), size.height / 2),
                    radius = 4.dp.toPx(),
                    style = Fill
                )
                drawCircle(
                    lineColor,
                    center = Offset(12.dp.toPx(), size.height / 2),
                    radius = 8.dp.toPx(),
                    style = Stroke(width = 2.dp.toPx())
                )
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(28.dp))
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = timestampColor,
            contentColor = timestampContentColor,
            modifier = Modifier
                .animateContentSize()
                .padding(end = 12.dp, top = 2.dp, bottom = 2.dp)
        ) {
            Text(
                timeStamp.timeMilli.toTimestamp(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = timestampFontSize.sp
                ),
                modifier = Modifier.padding(8.dp + timestampExtraPadding, 2.dp + timestampExtraPadding)
            )
        }
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

@Composable
fun GroupTimestampViewEndMarker (
    backgroundColor: Color? = null,
) {
    val lineColor = MaterialTheme.colorScheme.secondary
    val bgColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    val dotColor by animateColorAsState(MaterialTheme.colorScheme.secondary)

    Column(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    backgroundColor ?: bgColor,
                    Offset(12.dp.toPx(), size.height / 2),
                    Offset(12.dp.toPx(), size.height),
                    strokeWidth = 8.dp.toPx()
                )
                drawCircle(
                    backgroundColor ?: bgColor,
                    center = Offset(12.dp.toPx(), size.height / 2),
                    radius = 8.dp.toPx(),
                    style = Fill
                )
                drawCircle(
                    dotColor,
                    center = Offset(12.dp.toPx(), size.height / 2),
                    radius = 4.dp.toPx(),
                    style = Fill
                )
                drawCircle(
                    dotColor,
                    center = Offset(12.dp.toPx(), size.height / 2),
                    radius = 8.dp.toPx(),
                    style = Stroke(width = 2.dp.toPx())
                )
            },) {
        Row {
            Spacer(Modifier.width(28.dp))
            Text(
                "fin",
                style = MaterialTheme.typography.headlineLarge.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}