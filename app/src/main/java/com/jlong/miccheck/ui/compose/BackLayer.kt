package com.jlong.miccheck.ui.compose

import android.net.Uri
import android.os.Build
import android.support.v4.media.session.PlaybackStateCompat
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.jlong.miccheck.*

@OptIn(ExperimentalMaterial3Api::class)
fun micBackLayer(
    viewModel: MicCheckViewModel,
    navController: NavHostController,
    recordingClientControls: RecorderClientControls,
    playbackClientControls: PlaybackClientControls,
    setBackdrop: (Boolean) -> Unit,
    pickImage: ((Uri) -> Unit, () -> Unit) -> Unit
) = @Composable {
    var showRecordingDialog by remember { mutableStateOf(false) }
    var showTimestampDialog by remember { mutableStateOf(false) }
    var showExistingGroupDialog by remember { mutableStateOf(false) }
    var showNewGroupDialog by remember { mutableStateOf(false) }
    var showCropDialog by remember { mutableStateOf(false) }

    var timestampTimeMilli by remember { mutableStateOf(0L) }

    val chipColors = ChipColors.outlinedChipColors().copy(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        outline = MaterialTheme.colorScheme.outline
    )

    val seekbarPosition by animateFloatAsState(
        (viewModel.playbackProgress /
            (viewModel.currentPlaybackRec?.first?.duration?.toFloat() ?: 1f).let {
                if (it != 0f) it else 1f
            }
        )
    )

    var seekInputPosition by remember { mutableStateOf<Float?>(null) }

    var showLoopControls by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = viewModel.currentPlaybackRec) {
        showLoopControls = viewModel.loopMode || viewModel.loopRange != 0f..1f
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(12.dp)) {
        PlaybackDynamicCard(
            recording = viewModel.currentPlaybackRec,
            currentPlaybackTime = viewModel.playbackProgress,
            onMoreInfo = {
                viewModel.currentPlaybackRec?.first?.uri?.let{ navController.navigate(Destination.RecordingInfo.createRoute(it)) }
                setBackdrop(false)
            },
            expanded = !showLoopControls,
            starred = viewModel.currentPlaybackRec?.first?.let {viewModel.isRecordingStarred(it)} ?: false,
            toggleStarred = {
                viewModel.currentPlaybackRec?.first?.let {
                    if (viewModel.isRecordingStarred(it))
                        viewModel.unstarRecording(it)
                    else
                        viewModel.starRecording(it)
                }
            }
        )
        AnimatedVisibility(visible = viewModel.currentPlaybackRec != null && !showLoopControls, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()){
            Column {
                Spacer(Modifier.height(8.dp))
                Row {
                    OutlinedChip(
                        text = {
                            Text("Add to Group")
                        },
                        leadingIcon = Icons.Outlined.LibraryMusic,
                        colors = chipColors
                    ) {
                        showExistingGroupDialog = true
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedChip(
                        text = {
                            Text("Timestamp")
                        },
                        leadingIcon = Icons.Outlined.Place,
                        colors = chipColors
                    ) {
                        timestampTimeMilli = viewModel.playbackProgress
                        showTimestampDialog = true
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row (Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                viewModel.playbackProgress.toTimestamp(),
                style = MaterialTheme.typography.labelMedium,
                color = backdropContentColor()
            )
            Spacer(Modifier.width(8.dp))
            Slider(
                value = seekInputPosition ?: seekbarPosition,
                onValueChange = {
                    seekInputPosition = it
                },
                onValueChangeFinished = {
                    playbackClientControls.seek(seekInputPosition ?: seekbarPosition)
                    seekInputPosition = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = SliderDefaults.colors(
                    inactiveTrackColor = MaterialTheme.colorScheme.secondary
                ),
                enabled = viewModel.currentPlaybackRec != null
            )
            Spacer(Modifier.width(8.dp))
            Text(
                viewModel.currentPlaybackRec?.first?.duration?.toLong()?.toTimestamp() ?: 0L.toTimestamp(),
                style = MaterialTheme.typography.labelMedium,
                color = backdropContentColor()
            )
        }
        Spacer(Modifier.height(8.dp))
        Row (Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(48.dp))
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .weight(1f))
            Crossfade (viewModel.isGroupPlayback) {
                if (!it)
                    IconButton(
                        onClick = { playbackClientControls.seekDiff(-10000) },
                        enabled = viewModel.currentPlaybackRec != null
                    ) {
                        Icon(
                            Icons.Rounded.Replay10,
                            "Replay",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                else
                    IconButton(
                        onClick = { playbackClientControls.skipTrackPrevious() },
                        enabled = viewModel.currentPlaybackRec != null
                    ) {
                        Icon(
                            Icons.Rounded.SkipPrevious,
                            "Previous",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
            }
            Spacer(modifier = Modifier.width(8.dp))
            FilledIconButton(
                onClick = {
                    if (viewModel.currentPlaybackState == PlaybackStateCompat.STATE_PLAYING)
                        playbackClientControls.pause()
                    else if (viewModel.currentPlaybackState == PlaybackStateCompat.STATE_PAUSED ||
                        viewModel.currentPlaybackState == PlaybackStateCompat.STATE_STOPPED)
                        playbackClientControls.play()
                },
                Modifier.size(56.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                enabled = viewModel.currentPlaybackRec != null
            ) {
                Crossfade(targetState = viewModel.currentPlaybackState) {
                    if (it == PlaybackStateCompat.STATE_PAUSED || it == PlaybackStateCompat.STATE_STOPPED)
                        Icon(Icons.Rounded.PlayArrow, "Play")
                    else
                        Icon(Icons.Rounded.Pause, "Pause")

                }
            }
            Spacer(Modifier.width(8.dp))
            Crossfade (viewModel.isGroupPlayback) {
                if (!it)
                    IconButton(
                        onClick = { playbackClientControls.seekDiff(10000) },
                        enabled = viewModel.currentPlaybackRec != null
                    ) {
                        Icon(
                            Icons.Rounded.Forward10,
                            "Forward",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                else
                    IconButton(
                        onClick = { playbackClientControls.skipTrackNext() },
                        enabled = viewModel.currentPlaybackRec != null
                    ) {
                        Icon(
                            Icons.Rounded.SkipNext,
                            "Next",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
            }
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .weight(1f))
            FilledTonalIconToggleButton(
                checked = showLoopControls,
                onCheckedChange = {
                    if (!viewModel.settings.dismissedExtras.contains(DismissableExtraId.LoopUpdateV2))
                        viewModel.dismissExtra(DismissableExtraId.LoopUpdateV2)
                    showLoopControls = it
                    if (!it) {
                        playbackClientControls.disableLoopPlayback()
                        playbackClientControls.setLoopSelection(0f, 1f)
                    }
                },
                enabled = viewModel.currentPlaybackRec != null,
                colors = IconButtonDefaults.filledIconToggleButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(Icons.Rounded.AllInclusive, "Show loop controls")
            }
        }
        AnimatedVisibility(
            visible = viewModel.currentPlaybackRec != null && showLoopControls,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ){
            Column (Modifier.fillMaxWidth()) {
                Spacer(Modifier.height(18.dp))
                LoopControlsCard(
                    recording = viewModel.currentPlaybackRec?.first,
                    selection = viewModel.loopRange,
                    setSelection = { playbackClientControls.setLoopSelection(it.start, it.endInclusive) },
                    loopMode = viewModel.loopMode,
                    setLoopMode = {
                        if (it)
                            playbackClientControls.enableLoopPlayback()
                        else
                            playbackClientControls.disableLoopPlayback()
                    },
                    onCrop = {
                        showCropDialog = true
                    },
                    cropState = viewModel.ffmpegState,
                    playbackSpeed = viewModel.playbackSpeed,
                    setPlaybackSpeed = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            playbackClientControls.setPlaybackSpeed(it)
                        }
                    }
                )
            }
        }
        DismissableExtra(
            visible = !viewModel.settings.dismissedExtras.contains(DismissableExtraId.LoopUpdateV2),
            title = { Text("Try the new looper") },
            text = { Text("You can loop a selection of your recording, change playback speed, and save your selection as a new recording.") },
            leadingIcon = Icons.Rounded.AllInclusive,
            titleColor = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(top = 18.dp)
        ) {
            viewModel.dismissExtra(DismissableExtraId.LoopUpdateV2)
        }
        Spacer(Modifier.height(8.dp))
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

    TimestampDialog(visible = showTimestampDialog, onClose = { showTimestampDialog = false; timestampTimeMilli = 0L }) { title, desc ->
        if (viewModel.currentPlaybackRec != null)
            viewModel.timestampRecording(viewModel.currentPlaybackRec!!, timestampTimeMilli, title, desc)
        timestampTimeMilli = 0L
        showTimestampDialog = false
    }

    AddToExistingGroupDialog(
        visible = showExistingGroupDialog,
        groups = viewModel.groups,
        onClose = { showExistingGroupDialog = false },
        onCreateNew = { showExistingGroupDialog = false; showNewGroupDialog = true },
        onConfirm = {
            viewModel.currentPlaybackRec?.also { recording ->
                viewModel.addRecordingToGroup(
                    it,
                    recording.first
                )
            }
            viewModel.clearSelectedRecordings()
            showExistingGroupDialog = false
        }
    )

    NewGroupDialog(visible = showNewGroupDialog, onClose = { showNewGroupDialog = false }, pickImage = pickImage) { name, uri ->
        val group = viewModel.createGroup(name, uri)

        viewModel.currentPlaybackRec?.let {
            viewModel.addRecordingToGroup(
                group,
                it.first
            )
        }

        viewModel.clearSelectedRecordings()
        showNewGroupDialog = false
    }

    RecordingOptionsDialog(
        visible = showCropDialog,
        tags = viewModel.tags,
        onClose = { showCropDialog = false },
        onCancel = { showCropDialog = false },
        onConfirm = { title, description, tags ->
            recordingClientControls.onTrimRecording(
                title = title,
                description = description,
                tags = tags
            )
            showCropDialog = false
        }
    )
}
