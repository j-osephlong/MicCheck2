package com.jlong.miccheck.ui.compose

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.jlong.miccheck.*
import com.jlong.miccheck.R
import com.jlong.miccheck.ui.theme.surfaceColorAtElevation

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RecordingOptionsDialog (visible: Boolean, tags: List<Tag>, onClose: () -> Unit, onCancel: () -> Unit, onConfirm: (String, String, List<Tag>) -> Unit) {
    val selectedTags = remember { mutableStateListOf<Tag>() }
    val (titleText, setTitleText) = remember { mutableStateOf("") }
    val (descText, setDescText) = remember { mutableStateOf("") }

    if (visible)
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier =
            Modifier
                .padding(48.dp)
                .widthIn(280.dp, 560.dp),
            onDismissRequest = onClose,
            confirmButton = {
                Button(
                    onClick = { onConfirm(titleText, descText, selectedTags.toList()) },
                    enabled = titleText.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = { TextButton(onClick = { onClose(); onCancel() }) { Text("Discard") } },
            title = { Text("New Recording") },
            icon = { Icon(Icons.Rounded.Save, null) },
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
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = false
                    )
                    Spacer(Modifier.height(12.dp))
                    if (tags.isNotEmpty()){
                        Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .15f))
                        Spacer(Modifier.height(12.dp))
                        Box {
                            val state = rememberLazyListState()
                            LazyRow(
                                Modifier
                                    .fillMaxWidth()
                                    .height(32.dp),
                                state = state
                            ) {
                                items(tags.sortedByDescending { it.useCount }) {
                                    Row {
                                        OutlinedChip(
                                            text = { Text(it.name) },
                                            colors = ChipColors.filledChipColors()
                                                .copy(color = MaterialTheme.colorScheme.secondaryContainer),
                                            leadingIcon = Icons.Rounded.Check,
                                            showLeadingIcon = selectedTags.contains(it)
                                        ) {
                                            if (selectedTags.contains(it))
                                                selectedTags.remove(it)
                                            else
                                                selectedTags.add(it)
                                        }
                                        Spacer(Modifier.width(8.dp))
                                    }
                                }
                            }
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(32.dp)
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            listOf(
                                                Color.Transparent,
                                                Color.Transparent,
                                                Color.Transparent,
                                                Color.Transparent,
                                                Color.Transparent,
                                                Color.Transparent,
                                                MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
                                            ).let {
                                                if (state.firstVisibleItemScrollOffset > 0 || state.firstVisibleItemIndex > 0)
                                                    listOf(
                                                        MaterialTheme.colorScheme.surfaceColorAtElevation(
                                                            6.dp
                                                        )
                                                    ) + it.minusElement(Color.Transparent)
                                                else
                                                    it
                                            }
                                        )
                                    )
                            ) {

                            }
                        }
                    }

                }
            }
        )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingControlsCard(listState: LazyListState, viewModel: MicCheckViewModel, recordingClientControls: RecorderClientControls) {
    var expanded by remember { mutableStateOf(true) }

    LaunchedEffect(key1 = listState.isScrollInProgress) {
        if (listState.isScrollInProgress)
            expanded = false
    }

    var currentRotation by remember { mutableStateOf(0f) }

    val rotation = remember { Animatable(currentRotation) }

    var showRecordingDialog by remember { mutableStateOf(false) }
    var showTimestampDialog by remember { mutableStateOf(false) }
    var timestampTimeMilli by remember { mutableStateOf(0L) }

    LaunchedEffect(viewModel.recordingState) {
            if (viewModel.recordingState == RecordingState.RECORDING) {
                // Infinite repeatable rotation when is playing
                rotation.animateTo(
                    targetValue = currentRotation + 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(7200, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    )
                ) {
                    currentRotation = value
                }
            } else {
                if (currentRotation > 0f) {
                    // Slow down rotation on pause
                    rotation.animateTo(
                        targetValue = currentRotation + 20,
                        animationSpec = tween(
                            durationMillis = 1250,
                            easing = LinearOutSlowInEasing
                        )
                    ) {
                        currentRotation = value
                    }
                }
            }
        }
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp, 0.dp)
                .animateContentSize(),
            onClick = {expanded = !expanded}
        ) {
            Crossfade(targetState = expanded, modifier = Modifier.animateContentSize()){
                if (it)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    )
                    {
                        Row(
                            Modifier
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {},
                                modifier = Modifier.size(78.dp)
                            ) {
                                Box {
                                    Icon(
                                        ImageVector.vectorResource(id = R.drawable.ic_flower_button),
                                        null,
                                        modifier = Modifier
                                            .size(350.dp)
                                            .align(Alignment.Center)
                                            .rotate(rotation.value)
                                    )
                                    Text(
                                        viewModel.recordTime.toTimestamp(),
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.align(Alignment.Center),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    )
                                }

                            }
                            Spacer(
                                Modifier.width(18.dp)
                            )
                            Text(
                                "Recording",
                                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            IconButton(onClick = {
                                timestampTimeMilli = viewModel.recordTime
                                showTimestampDialog = true
                            }) {
                                Icon(Icons.Rounded.Place, null)
                            }
                            IconButton(onClick = { recordingClientControls.onPausePlayRecord() }) {
                                when (viewModel.recordingState) {
                                    RecordingState.RECORDING ->
                                        IconButton(onClick = recordingClientControls::onPausePlayRecord) {
                                            Icon(Icons.Rounded.Pause, null)
                                        }
                                    RecordingState.PAUSED ->
                                        IconButton(onClick = recordingClientControls::onPausePlayRecord) {
                                            Icon(Icons.Rounded.Mic, null)
                                        }
                                    else -> Unit
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    recordingClientControls.onStopRecord()
                                    showRecordingDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    contentColor = MaterialTheme.colorScheme.primaryContainer,
                                    containerColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Text("Done")
                            }
                        }
                    }
                else
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp, 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(ImageVector.vectorResource(id = R.drawable.ic_flower_button), null, modifier = Modifier
                            .rotate(rotation.value)
                            .size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            viewModel.recordTime.toTimestamp(),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Recording", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Normal))
                        Spacer(
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                        IconButton(onClick = { expanded = true }) {
                            Icon(
                                Icons.Rounded.ExpandMore,
                                null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
            }
        }

    RecordingOptionsDialog(
        visible = showRecordingDialog,
        tags = viewModel.tags,
        onClose = { showRecordingDialog = false },
        onCancel = recordingClientControls::onCancelRecording
    ) { title, desc, tags ->
        recordingClientControls.onRecordingFinalized(title, desc, tags)
        showRecordingDialog = false
    }

    TimestampDialog(visible = showTimestampDialog, onClose = { showTimestampDialog = false }) { title, desc ->
        viewModel.queueTimestamp(
            timestampTimeMilli,
            title,
            desc
        )
        showTimestampDialog = false
    }
}
