package com.jlong.miccheck.ui.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jlong.miccheck.FFMPEGState
import com.jlong.miccheck.Recording

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoopControlsCard (
    recording: Recording?,
    selection: ClosedFloatingPointRange<Float>,
    setSelection: (ClosedFloatingPointRange<Float>) -> Unit,
    loopMode: Boolean,
    setLoopMode: (Boolean) -> Unit,
    onCrop: () -> Unit,
    cropState: FFMPEGState,
    playbackSpeed: Float,
    setPlaybackSpeed: (Float) -> Unit
) {
    var visibleRange by remember {
        mutableStateOf(0f..1f)
    }
    var selectingRange by remember {
        mutableStateOf<ClosedFloatingPointRange<Float>?>(null)
    }

    Surface (
        shape = RoundedCornerShape(18.dp),
        modifier =
            Modifier
                .fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(Modifier.padding(12.dp)) {
            SegmentedButton {
                SegmentButtton(
                    selected = !loopMode,
                    icon = Icons.Rounded.KeyboardTab,
                    text = { Text("One Shot") }
                ) {
                    setLoopMode(false)
                }
                SegmentButtton(
                    selected = loopMode,
                    icon = Icons.Rounded.AllInclusive,
                    text = { Text("Loop") }
                ) {
                    setLoopMode(true)
                }
            }
            Spacer(Modifier.height(8.dp))

            RangeSlider(
                valueRange = visibleRange,
                values = selectingRange ?: selection,
                onValueChange = {
                    selectingRange = it
                },
                onValueChangeFinished = {
                    selectingRange?.also {
                        setSelection(it)
                    }
                    selectingRange = null
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(0.dp))
            Row (horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()){
                Text(
                    (
                        (recording?.duration?:0)*(selectingRange?.start?:selection.start)
                    ).toLong().toTimestamp(),
                    style = MaterialTheme.typography.labelMedium,
                    color = backdropContentColor()
                )
                Text(
                    (
                        (recording?.duration?:0)*(selectingRange?.endInclusive?:selection.endInclusive)
                    ).toLong().toTimestamp(),
                    style = MaterialTheme.typography.labelMedium,
                    color = backdropContentColor()
                )
            }

            Spacer(Modifier.height(12.dp))
            Row (Modifier.fillMaxWidth()) {
                FilledTonalIconButton(onClick = {
                    setPlaybackSpeed (
                        when (playbackSpeed) {
                            .25f -> .5f
                            .5f -> 1f
                            1f -> 2f
                            2f -> .25f
                            else -> 1f
                        }
                    )
                }) {
                    Text(
                        when (playbackSpeed) {
                            .25f -> "¼"
                            .5f -> "½"
                            1f -> "1x"
                            2f -> "2x"
                            else -> "?"
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                AnimatedVisibility(visible = selection != visibleRange) {
                    OutlinedIconButton(onClick = { visibleRange = selection }) {
                        Icon(Icons.Rounded.ZoomIn, null)
                    }
                }
                AnimatedVisibility(visible = visibleRange != 0f..1f) {
                    OutlinedIconButton(onClick = { visibleRange = 0f..1f }) {
                        Icon(Icons.Rounded.ZoomOut, null)
                    }
                }
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f))
                Button(onClick = onCrop) {
                    Crossfade(targetState = cropState) {
                        when(it) {
                            FFMPEGState.None -> Icon(Icons.Rounded.Crop, null, Modifier.size(18.dp))
                            FFMPEGState.Failed -> Icon(Icons.Rounded.Error, null, Modifier.size(18.dp))
                            FFMPEGState.Finished -> Icon(Icons.Rounded.Check, null, Modifier.size(18.dp))
                            FFMPEGState.Running -> CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Save")
                }
            }
        }
    }
}

//@Preview
//@Composable
//fun LCCPreview() {
//    MicCheckTheme {
//        Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp)) {
//            val (range, setRange) = remember {
//                mutableStateOf(0f..1f)
//            }
//
//            LoopControlsCard(
//                range, setRange
//            )
//        }
//    }
//}