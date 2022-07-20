package com.jlong.miccheck.ui.compose

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.jlong.miccheck.Recording
import com.jlong.miccheck.RecordingData
import com.jlong.miccheck.RecordingGroup
import com.jlong.miccheck.TimeStamp
import com.jlong.miccheck.ui.theme.MicCheckTheme
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackDynamicCard(
    modifier: Modifier = Modifier,
    recording: Triple<Recording, RecordingData, RecordingGroup?>?,
    currentPlaybackTime: Long,
    onMoreInfo: () -> Unit,
    expanded: Boolean,
    starred: Boolean,
    toggleStarred: () -> Unit
) {

    val cardVerticalPadding by animateDpAsState(if (expanded) 18.dp else 8.dp)
    val titleFontSize by animateFloatAsState(if (expanded) MaterialTheme.typography.displaySmall.fontSize.value else MaterialTheme.typography.titleLarge.fontSize.value)
    val titleFontWeight by animateIntAsState(if (expanded) MaterialTheme.typography.displaySmall.fontWeight!!.weight else MaterialTheme.typography.titleLarge.fontWeight!!.weight)

    var nearbyTimestamp by remember { mutableStateOf<TimeStamp?>(null) }
    var nearbyTimeStampTemp by remember { mutableStateOf<TimeStamp?>(null) }

    if (recording?.second?.timeStamps?.isNotEmpty() == true)
        LaunchedEffect(key1 = currentPlaybackTime/500) {
            var closestTimestamp: TimeStamp? = null
            recording.second.timeStamps.forEach {
                if (closestTimestamp == null && abs(it.timeMilli-currentPlaybackTime) <= 10000)
                    closestTimestamp = it
                if (closestTimestamp != null)
                    if (abs(it.timeMilli-currentPlaybackTime) < abs(closestTimestamp!!.timeMilli-currentPlaybackTime))
                        closestTimestamp = it
            }
            nearbyTimeStampTemp = nearbyTimestamp
            nearbyTimestamp = closestTimestamp
        }

    LaunchedEffect(key1 = recording) {
        nearbyTimestamp = null
    }

    OutlinedCard (
        modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column (
            Modifier
                .padding(18.dp, cardVerticalPadding)
                .animateContentSize()) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (recording == null) Arrangement.Center else Arrangement.Start
            ) {
                Text(
                    recording?.first?.name ?: "INSERT CASSETTE",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = titleFontSize.sp,
                        fontWeight = FontWeight(titleFontWeight)
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
                if (recording!=null)
                    FilledTonalIconToggleButton(
                        checked = starred,
                        onCheckedChange = {
                            toggleStarred()
                        }
                    ) {
                        Icon(
                            if (starred)
                                Icons.Rounded.Star
                            else Icons.Rounded.StarBorder,
                            null
                        )
                    }
            }
            AnimatedVisibility (recording!=null && expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    AnimatedVisibility(visible = nearbyTimestamp != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                        Column {
                            TimestampCard(nearbyTimestamp ?: nearbyTimeStampTemp ?: TimeStamp(0L, "", "", ""))
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    AnimatedVisibility(
                        visible = nearbyTimestamp == null && recording?.second?.description?.isNotBlank() == true,
                        enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            DescriptionCard(
                                description = recording?.second?.description
                                    ?: "No description found."
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(onClick = onMoreInfo) {
                            Text("View More")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimestampCard (
    timeStamp: TimeStamp
) {
    Column (Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column (horizontalAlignment = Alignment.CenterHorizontally){
                Spacer(Modifier.height(1.dp))
                Icon(Icons.Rounded.Schedule, null)
                Spacer(Modifier.height(1.dp))
                Text(timeStamp.timeMilli.toTimestamp(), style = MaterialTheme.typography.labelSmall)
            }
            Spacer(modifier = Modifier.width(18.dp))
            Text(timeStamp.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (timeStamp.description.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(timeStamp.description, maxLines = 5, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun DescriptionCard(
    description: String
) {
    Text(description, maxLines = 5, overflow = TextOverflow.Ellipsis)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TimestampDialog (visible: Boolean, onClose: () -> Unit, existing: TimeStamp? = null, onConfirm: (String, String) -> Unit) {
    val (titleText, setTitleText) = remember { mutableStateOf(existing?.name?: "") }
    val (descText, setDescText) = remember { mutableStateOf(existing?.description?: "") }

    LaunchedEffect(key1 = existing) {
        existing?.also {
            setTitleText(existing.name)
            setDescText(existing.description)
        }
    }

    if (visible)
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier =
            Modifier
                .padding(48.dp)
                .widthIn(280.dp, 560.dp),
            onDismissRequest = onClose,
            confirmButton = {
                val buttonColor by animateColorAsState(targetValue =
                    if (titleText.isNotBlank()) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )
                val buttonTextColor by animateColorAsState(targetValue =
                    if (titleText.isNotBlank()) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onError
                )
                Button(
                    onClick = { onConfirm(titleText.ifBlank { "Quick Timestamp" }, descText) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor,
                        contentColor = buttonTextColor
                    )
                ) {
                    Crossfade(targetState = titleText.isNotBlank(), modifier = Modifier.animateContentSize()) {
                        if (it)
                            Text("Save")
                        else
                            Row {
                                Icon(Icons.Rounded.Timer, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Quick Timestamp")
                            }
                    }
                }
            },
            dismissButton = { TextButton(onClick = onClose) { Text("Discard") } },
            title = { Text(if (existing!= null) "Edit Timestamp" else "Add Timestamp") },
            icon = { Icon(Icons.Rounded.Timer, null) },
            text = {
                Column {
                    TextField(
                        value = titleText,
                        onValueChange = setTitleText,
                        placeholder = { Text("Title") },
                        shape = RoundedCornerShape(28.dp),
                        colors = TextFieldDefaults.textFieldColors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        maxLines = 2
                    )
                    Spacer(Modifier.height(8.dp))
                    TextField(
                        value = descText,
                        onValueChange = setDescText,
                        placeholder = { Text("Description") },
                        shape = RoundedCornerShape(28.dp),
                        colors = TextFieldDefaults.textFieldColors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        singleLine = false
                    )
                }
            }
        )
}


@Preview
@Composable
fun p () {
    val  coroutineScope = rememberCoroutineScope()

    MicCheckTheme {
        Surface(Modifier.fillMaxSize()) {
            var log by remember {
                mutableStateOf("log")
            }
            var barColor = MaterialTheme.colorScheme.primary
            var pos by remember { mutableStateOf(0f)}

            Text(log)
            Canvas (Modifier.fillMaxSize()) {
                log = "i"


                val amplitudes = arrayOf(
                    15, 10, 5, 90, 65, 60, 70, 40, 20, 30, 60, 50, 15
                )

                val canvasWidth = size.width
                val canvasHeight = size.height

                drawCircle(
                    barColor,
                    12.dp.toPx(),
                    Offset(
                        pos,
                        (size.height/2- 106.dp.toPx())

                    )
                )

                drawLine(
                    start = Offset(x = 0f, y = canvasHeight/2),
                    end = Offset(x = canvasWidth, y = canvasHeight/2),
                    color = Color.Blue
                )

                amplitudes.forEachIndexed { index, amp ->
                    val height = (amp.toFloat() / amplitudes.maxOf { it }) * 164.dp.toPx()
                    log += "index $index - \n\t${height} - $amp - ${amplitudes.maxOf { it }}\n"
                    drawLine(
                        barColor,
                        start = Offset(
                            (index + 1f) * (size.width/(amplitudes.size+1)),
                            canvasHeight/2 - height/2

                        ),
                        end = Offset(
                            (index + 1f) * (size.width/(amplitudes.size+1)),
                            height/2 + canvasHeight/2
                        ),
                        strokeWidth = 12.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                }


            }
        }
    }
}