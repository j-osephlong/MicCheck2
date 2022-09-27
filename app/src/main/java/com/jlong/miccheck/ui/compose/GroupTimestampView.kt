package com.jlong.miccheck.ui.compose

import android.net.Uri
import android.util.Log
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.navigation.animation.composable
import com.jlong.miccheck.*
import kotlin.math.abs

private const val TEXT_SCALE_REDUCTION_INTERVAL = 0.9f

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.addGroupTimestampViewScreenGraph(
    navController: NavController,
    viewModel: MicCheckViewModel,
    playbackClientControls: PlaybackClientControls,
    setBackdrop: (Boolean) -> Unit
) {
    Log.i("Navigation", "Nav component list composed.")

    composable(
        Destination.GroupTimestampView.route,
    ) { backStackEntry ->

        val uuidString = requireNotNull(backStackEntry.arguments?.getString("uuid"))
        val group = requireNotNull(viewModel.getGroup(uuidString))
        viewModel.currentGroupScreen = group

        GroupTimestampView(
            viewModel = viewModel,
            navController = navController,
            playbackClientControls = playbackClientControls,
            setBackdrop = setBackdrop,
            group = group
        )

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupTimestampView(
    viewModel: MicCheckViewModel,
    navController: NavController,
    playbackClientControls: PlaybackClientControls,
    setBackdrop: (Boolean) -> Unit,
    group: RecordingGroup /* = com.jlong.miccheck.VersionedRecordingGroup.V6 */
) {
    val groupDuration by remember { mutableStateOf(
        viewModel.getGroupRecordings(group).fold(0) { i, r -> i + r.duration }
    ) }

    val titleFont = MaterialTheme.typography.displaySmall
    var titleFontSize by remember { mutableStateOf(titleFont.fontSize) }

    Surface (
        Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Column (Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(6.dp))
            Row {
                Spacer(Modifier.width(0.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { },
                    modifier = Modifier.height(24.dp)
                ) {
                    Text(
                        "${group.recordings.size} Recording${if (group.recordings.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(8.dp, 4.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { },
                    modifier = Modifier.height(24.dp)
                ) {
                    Text(
                        groupDuration.toLong().toTimestamp(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(
                            8.dp,
                            4.dp
                        )
                    )

                }
                Spacer(Modifier.width(12.dp))
            }
            Spacer(Modifier.height(12.dp))

            Text(
                group.name,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = titleFontSize
                ),
                modifier = Modifier.padding(8.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { textLayoutResult ->
                    val maxCurrentLineIndex: Int =
                        textLayoutResult.lineCount - 1

                    if (textLayoutResult.isLineEllipsized(
                            maxCurrentLineIndex
                        )
                    ) {
                        titleFontSize = titleFontSize.times(
                            TEXT_SCALE_REDUCTION_INTERVAL
                        )
                    }
                }
            )

            Spacer(Modifier.height(18.dp))
            Icon(
                Icons.Outlined.LocationOn,
                null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.offset(0.dp, 0.dp)
            )
            Spacer(Modifier.height(4.dp))
            TimestampView(
                viewModel = viewModel,
                navController = navController,
                playbackClientControls = playbackClientControls,
                setBackdrop = setBackdrop,
                group = group
            )
            Spacer(Modifier.height(88.dp + WindowInsets.navigationBars
                .asPaddingValues()
                .calculateBottomPadding()))
        }
    }
}

@Composable
fun TimestampView(
    viewModel: MicCheckViewModel,
    navController: NavController,
    playbackClientControls: PlaybackClientControls,
    setBackdrop: (Boolean) -> Unit,
    group: RecordingGroup /* = com.jlong.miccheck.VersionedRecordingGroup.V6 */
) {
    val recordings = remember {
        mutableStateListOf<Pair<Recording, RecordingData>>()
    }

    var nearbyTimestamp by remember { mutableStateOf<TimeStamp?>(null) }

    if (viewModel.currentPlaybackRec?.second?.timeStamps?.isNotEmpty() == true)
        LaunchedEffect(key1 = viewModel.playbackProgress/500) {
            var closestTimestamp: TimeStamp? = null
            viewModel.currentPlaybackRec?.second?.timeStamps?.forEach {
                if (closestTimestamp == null)
                    closestTimestamp = it
                if (closestTimestamp != null)
                    if (abs(it.timeMilli-viewModel.playbackProgress) < abs(closestTimestamp!!.timeMilli-viewModel.playbackProgress))
                        closestTimestamp = it
            }
            nearbyTimestamp = closestTimestamp
        }
    if (viewModel.currentPlaybackRec?.second?.timeStamps?.isEmpty() == true)
        nearbyTimestamp = null
    LaunchedEffect(key1 = group) {
        val new = viewModel.getGroupRecordings(group).map {
            Pair(it, viewModel.getRecordingData(it))
        }.toMutableStateList()
        if (recordings.isNotEmpty())
            recordings.removeRange(0, recordings.lastIndex)
        new.forEach {
            recordings.add(it)
        }

        Log.i("TV", recordings.joinToString { it.toString() + "\n"})
    }

    val lineColor = MaterialTheme.colorScheme.secondary
    Column (
        Modifier
            .fillMaxWidth()
            .drawBehind {
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
            }
    ) {
        recordings.forEachIndexed { index, it ->
            Log.i("TV-i", it.toString())
            Spacer(Modifier.height(12.dp))
            TimestampViewRecordingElm(
                recording = it.first,
                timePosition = recordings.subList(0, index).fold(0) { i, r -> i + r.first.duration},
                lastItem = false,
//                    it.second.timeStamps.isEmpty() && recordings.indexOf(it) == recordings.lastIndex,
                backgroundColor = MaterialTheme.colorScheme.background,
                isPlaying = it.first.uri == viewModel.currentPlaybackRec?.first?.uri,
                onClick = {
                    playbackClientControls.play(group, index)
                    setBackdrop(true)
                }
            ) {
                navController.navigate(Destination.RecordingInfo.createRoute(it.first.uri))
            }
            it.second.timeStamps.forEachIndexed { timestampIndex, timestamp ->
                Spacer(Modifier.height(18.dp))
                TimestampListItem(
                    timeStamp = timestamp,
                    lastItem = false,
//                    timestampIndex == it.second.timeStamps.lastIndex && index == recordings.lastIndex,
                    focused = timestamp.uuid == nearbyTimestamp?.uuid,
                    backgroundColor = MaterialTheme.colorScheme.background
                ) {
                    playbackClientControls.playFromTimestamp(
                        Triple(it.first, it.second, group),
                        timestamp
                    )
                    setBackdrop(true)
                }
            }

        }
        Spacer(Modifier.height(18.dp))
        GroupTimestampViewEndMarker(MaterialTheme.colorScheme.background)

    }
}
