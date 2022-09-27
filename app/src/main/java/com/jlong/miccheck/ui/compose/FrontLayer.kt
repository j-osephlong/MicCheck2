package com.jlong.miccheck.ui.compose

import android.content.ContentUris
import android.net.Uri
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.jlong.miccheck.*

sealed class Destination (val route: String) {
    object RecordingsList: Destination("recordings")
    object RecordingInfo: Destination("recordingInfo/{uri}") {
        fun createRoute(uri: Uri) = "recordingInfo/${ContentUris.parseId(uri)}"
    }
    object Group: Destination("group/{uuid}") {
        fun createRoute(uuid: String) = "group/$uuid"
    }
    object Search: Destination("search?tag={tag}&selectMode={selectMode}") {
        fun createRoute(tag: Tag? = null, selectMode: Boolean = false) = "search?tag=${tag?.name}&selectMode=$selectMode"
    }
    object About: Destination("about")
    object WhatsNew: Destination("whatsNew")
    object GetPro: Destination("getPro")
    object GroupTimestampView: Destination("timestampView/{uuid}") {
        fun createRoute(uuid: String) = "timestampView/$uuid"
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun micFrontLayer(
    viewModel: MicCheckViewModel,
    navController: NavHostController,
    recordingClientControls: RecorderClientControls,
    playbackClientControls: PlaybackClientControls,
    setBackdrop: (Boolean) -> Unit,
    pickImage: ((Uri) -> Unit, () -> Unit) -> Unit,
    pickFile: ((Uri) -> Unit) -> Unit,
    launchUri: (String, Uri) -> Unit
) = @Composable {
    Log.i("Composition", "Front Layer composed.")

    var showExportDialog by remember { mutableStateOf(false) }
    var showDebugExportDialog by remember { mutableStateOf(false) }

    Surface(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            val navBackStackEntry by navController.currentBackStackEntryAsState()

            AnimatedNavHost(
                navController = navController,
                startDestination = Destination.RecordingsList.route,
                enterTransition = { slideIntoContainer(AnimatedContentScope.SlideDirection.Left, tween(300)) + fadeIn(tween(450)) },
                exitTransition = { slideOutOfContainer(AnimatedContentScope.SlideDirection.Left, tween(300)) + fadeOut(tween(450))},
                popEnterTransition = { slideIntoContainer(AnimatedContentScope.SlideDirection.Right, tween(300)) + fadeIn(tween(450)) },
                popExitTransition = { slideOutOfContainer(AnimatedContentScope.SlideDirection.Right, tween(300)) + fadeOut(tween(450))}
            ) {
                addRecordingsListGraph(navController, viewModel, playbackClientControls, recordingClientControls, setBackdrop, pickImage)
                addRecordingInfoScreenGraph(navController, viewModel, playbackClientControls, pickImage, pickFile, launchUri)
                addSearchScreenGraph(navController, viewModel, playbackClientControls, pickImage)
                addGroupScreenGraph(navController,viewModel,playbackClientControls,pickImage)
                addAboutScreenGraph(navController, viewModel)
                addWhatsNewScreenGraph(navController, viewModel)
                addGetProScreenGraph(navController, viewModel)
                addGroupTimestampViewScreenGraph(navController, viewModel, playbackClientControls, setBackdrop)
            }

            Column (
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(
                        (-16).dp,
                        (-WindowInsets.navigationBars
                            .asPaddingValues()
                            .calculateBottomPadding() - 16.dp)
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(visible = navBackStackEntry?.destination?.route == Destination.RecordingInfo.route, enter = fadeIn() + expandIn(), exit = fadeOut() + shrinkOut()) {
                    Column {
                        SmallFloatingActionButton(onClick = {
                            showExportDialog = true
                        }) {
                            Icon(Icons.Rounded.Share, null)
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }

                AnimatedVisibility(
                    visible = navBackStackEntry?.destination?.route == Destination.RecordingsList.route,
                    enter = fadeIn() + expandIn(),
                    exit = fadeOut() + shrinkOut()
                ) {
                    Column {
                        SmallFloatingActionButton(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer, onClick = {
                            setBackdrop(true)
                        }) {
                            Icon(Icons.Rounded.ExpandCircleDown, null)
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }

                AnimatedVisibility(visible =
                    (navBackStackEntry?.destination?.route != Destination.Search.route ||
                            viewModel.searchScreenInSelectMode) &&
                            navBackStackEntry?.destination?.route != Destination.About.route &&
                            navBackStackEntry?.destination?.route != Destination.GetPro.route
                ){
                    FloatingActionButton(
                        onClick = {
                            when (navBackStackEntry?.destination?.route) {
                                Destination.RecordingInfo.route -> {
                                    viewModel.currentRecordingInfoScreen?.let {
                                        playbackClientControls.play(
                                            it
                                        )
                                    }
                                    setBackdrop(true)
                                }
                                Destination.Group.route, Destination.GroupTimestampView.route -> {
                                    viewModel.currentGroupScreen?.also {
                                        playbackClientControls.play(
                                            it,
                                            0
                                        )
                                    }
                                }
                                Destination.Search.route -> {
                                    viewModel.currentGroupScreen?.also {
                                        viewModel.addRecordingsToGroup(
                                            viewModel.selectedRecordings,
                                            it
                                        )
                                    }
                                    navController.navigateUp()
                                }
                                Destination.WhatsNew.route -> {
                                    navController.navigateUp()
                                }
                                else ->
                                    if (viewModel.recordingState == RecordingState.WAITING || viewModel.recordingState == RecordingState.STOPPED)
                                        recordingClientControls.onStartRecord()
                                    else if (viewModel.recordingState == RecordingState.RECORDING || viewModel.recordingState == RecordingState.PAUSED)
                                        recordingClientControls.onPausePlayRecord()
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Crossfade(targetState = navBackStackEntry?.destination?.route) {
                            when (it) {
                                Destination.RecordingInfo.route -> Icon(
                                    Icons.Rounded.PlayArrow,
                                    null
                                )
                                Destination.Group.route, Destination.GroupTimestampView.route -> Icon(Icons.Rounded.VideoLibrary, null)
                                Destination.Search.route -> Icon(Icons.Rounded.Check, null)
                                Destination.WhatsNew.route -> Icon(Icons.Rounded.ArrowForward, null)
                                else -> {
                                    when (viewModel.recordingState) {
                                        RecordingState.RECORDING ->
                                            Icon(
                                                Icons.Rounded.Pause,
                                                null
                                            )
                                        else ->
                                            Icon(
                                                Icons.Rounded.Mic,
                                                null
                                            )

                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(viewModel.firstStartShowProScreen) {
        if (viewModel.firstStartShowProScreen)
            navController.navigate(Destination.GetPro.route)
        viewModel.firstStartShowProScreen = false
    }

    ExportDialog(
        visible = showExportDialog,
        isPro = viewModel.isPro,
        onClose = { showExportDialog = false },
        onShare = {
            viewModel.currentRecordingInfoScreen?.also {
                playbackClientControls.shareRecording(listOf(it.first))
            }
            showExportDialog = false
        },
        onOpenGetPro = {
            showExportDialog = false
            navController.navigate(Destination.GetPro.route)
        }
    ) {
        if (!viewModel.inDebugMode) {
            viewModel.currentRecordingInfoScreen?.also {
                playbackClientControls.shareRecordingAsVideo(
                    recording = it.first,
                    loopVideo = true
                )
            }
        } else {
            showDebugExportDialog = true
        }
        showExportDialog = false
    }

    DebugExportAsVideoPhotoCommand(
        visible = showDebugExportDialog,
        onClose = { showDebugExportDialog = false },
        onConfirm = { debugCommand ->
            viewModel.currentRecordingInfoScreen?.also {
                playbackClientControls.shareRecordingAsVideo(
                    recording = it.first,
                    loopVideo = true,
                    debugCommand = debugCommand
                )
            }

            showDebugExportDialog = false
        },
        viewModel = viewModel
    )
}