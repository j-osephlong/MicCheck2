package com.jlong.miccheck.ui.compose

import android.net.Uri
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import coil.compose.AsyncImage
import com.google.accompanist.navigation.animation.composable
import com.jlong.miccheck.MicCheckViewModel
import com.jlong.miccheck.PlaybackClientControls
import com.jlong.miccheck.Recording
import com.jlong.miccheck.RecordingGroup
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import java.lang.Integer.max
import java.lang.Integer.min

private const val TEXT_SCALE_REDUCTION_INTERVAL = 0.9f

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.addGroupScreenGraph(
    navController: NavController,
    viewModel: MicCheckViewModel,
    playbackClientControls: PlaybackClientControls,
    pickImage: ((Uri)->Unit, ()->Unit ) -> Unit
) {
    Log.i("Navigation", "Nav component list composed.")

    composable(
        Destination.Group.route,
    ) { backStackEntry ->

        val uuidString = requireNotNull(backStackEntry.arguments?.getString("uuid"))
        val group = viewModel.getGroup(uuidString)
        viewModel.currentGroupScreen = group

        GroupScreen(
            viewModel = viewModel,
            playbackClientControls = playbackClientControls,
            navController = navController,
            pickImage = pickImage,
            group = group ?: RecordingGroup(
                " ", null, ""
            )
        )

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupScreen (
    viewModel: MicCheckViewModel,
    playbackClientControls: PlaybackClientControls,
    navController: NavController,
    pickImage: ((Uri)->Unit, ()->Unit ) -> Unit,
    group: RecordingGroup
) {
    val coroutineScope = rememberCoroutineScope()

    var recordings by remember {
        mutableStateOf(
            group.recordings
        )
    }

    var isEditing by remember {
        mutableStateOf(false)
    }

    var fieldTextPadding by remember {
        mutableStateOf(0.dp)
    }

    val titleFont = MaterialTheme.typography.displaySmall
    var titleFontSize by remember { mutableStateOf(titleFont.fontSize) }

    val toggleEditing: () -> Unit = {
        coroutineScope.launch {
            if (!isEditing)
                animate(0f, 8f, animationSpec = tween(200)) { value, _ ->
                    fieldTextPadding = value.dp
                }
            isEditing = !isEditing
            if (!isEditing) {
                delay(300)
                animate(8f, 0f, animationSpec = tween(200)) { value, _ ->
                    fieldTextPadding = value.dp
                }
            }
        }
    }

    var showDeleteGroupDialog by remember { mutableStateOf(false) }

    var titleFieldText by remember { mutableStateOf("") }
    var selectedForRemoval = remember { mutableStateListOf<Recording>() }

    val lazyListState = rememberReorderableLazyListState(onMove = { from, to ->
        Pair(
            from.copy(index = min(max(from.index-3, 0), recordings.lastIndex)),
            to.copy(index = min(max(to.index-3, 0), recordings.lastIndex))
        ).let { (f, t) ->
            recordings = recordings.toMutableList().apply {
                add(t.index, removeAt(f.index))
            }

            viewModel.reorderGroup(recordings, group)
        }

    })

    Surface(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val viewHeight = this.maxHeight
            var imageExpanded by remember { mutableStateOf(false) }
            val imageHeight by animateDpAsState(
                if (imageExpanded) viewHeight else (viewHeight.value*0.35).dp
            )

            Column(Modifier.fillMaxSize()) {
                LazyColumn (
                    state = lazyListState.listState,
                    modifier = Modifier
                        .reorderable(lazyListState)
                ) {
                    item {
                        BoxWithConstraints(
                            Modifier
                                .fillMaxWidth()
                                .then(
                                    if (group.imgUri != null) Modifier.height(imageHeight) else Modifier
                                )
                                .clickable { imageExpanded = !imageExpanded && !isEditing }
                                .clip(
                                    animateDpAsState(
                                        if (imageExpanded) 0.dp else 18.dp
                                    ).value.let {
                                        RoundedCornerShape(
                                            0.dp, 0.dp,
                                            it, it
                                        )
                                    }
                                )
                        ) {
                            val boxMaxWidth = this.maxWidth
                            group.imgUri?.let {
                                AsyncImage(
                                    model = Uri.parse(it),
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    contentDescription = null
                                )
                            }
                            Surface (
                                color = Color.Transparent,
                                modifier = Modifier.align(Alignment.BottomStart).then (
                                    if (group.imgUri != null) Modifier else Modifier.fillMaxWidth()
                                )
                            ){
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                ) {
                                    Spacer(Modifier.height(18.dp))
                                    Row {
                                        Spacer(Modifier.width(12.dp))
                                        Surface(
                                            shape = MaterialTheme.shapes.small,
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            onClick = { },
                                            modifier = Modifier.height(24.dp),
                                            shadowElevation = if (group.imgUri != null) 8.dp else 0.dp
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
                                            modifier = Modifier.height(24.dp),
                                            shadowElevation = if (group.imgUri != null) 8.dp else 0.dp
                                        ) {
                                            Text(
                                                group.recordings.sumOf {
                                                    viewModel.getRecording(Uri.parse(it))!!.duration
                                                }.toLong().toTimestamp(),
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
                                    Surface(
                                        shape = MaterialTheme.shapes.medium,
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 1f),
                                        modifier = Modifier
                                            .padding(12.dp)
                                            .widthIn(max = (0.75 * boxMaxWidth.value).dp),
                                        shadowElevation = if (group.imgUri != null) 8.dp else 0.dp
                                    ) {
                                        Crossfade(
                                            targetState = isEditing,
                                            modifier = Modifier.animateContentSize()
                                        ) {
                                            if (!it)
                                                Text(
                                                    group.name,
                                                    style = MaterialTheme.typography.displaySmall.copy(
                                                        fontSize = titleFontSize
                                                    ),
                                                    modifier = Modifier.padding(8.dp + fieldTextPadding),
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
                                            else
                                                TextField(
                                                    value = titleFieldText,
                                                    onValueChange = { titleFieldText = it },
                                                    shape = MaterialTheme.shapes.medium,
                                                    colors = TextFieldDefaults.textFieldColors(
                                                        focusedIndicatorColor = Color.Transparent,
                                                        unfocusedIndicatorColor = Color.Transparent,
                                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                                    ),
                                                    textStyle = MaterialTheme.typography.displaySmall.copy(
                                                        fontSize = titleFontSize
                                                    )
                                                )
                                        }
                                    }
                                    Spacer(
                                        modifier = Modifier.height(
                                            animateDpAsState(
                                                if (imageExpanded) WindowInsets.navigationBars.asPaddingValues()
                                                    .calculateBottomPadding() else 0.dp
                                            ).value
                                        )
                                    )
                                }
                            }

                            if (group.uuid != starredGroupUUID)
                                Row (
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 0.dp, y = 12.dp)
                                ){
                                    AnimatedVisibility (isEditing) {
                                        FilledTonalIconButton(
                                            onClick = {
                                                pickImage(
                                                    {
                                                        viewModel.setGroupImage(it, group)
                                                    },
                                                    {}
                                                )
                                            }
                                        ) {
                                            Icon(Icons.Outlined.Image, null)
                                        }
                                    }

                                    AnimatedVisibility(
                                        visible = !imageExpanded,
                                        exit = slideOutHorizontally(targetOffsetX = { it / 1 }) + fadeOut(),
                                        modifier = Modifier.padding(end = 12.dp)
                                    ){
                                        FilledTonalIconButton(
                                            onClick = {
                                                if (isEditing) {
                                                    if (selectedForRemoval.isNotEmpty())
                                                        viewModel.removeRecordingsFromGroup(
                                                            selectedForRemoval,
                                                            group
                                                        )
                                                    recordings =
                                                        recordings.filter { it !in selectedForRemoval.map { it.uri.toString() } }
                                                    selectedForRemoval = mutableStateListOf()
                                                    viewModel.editGroupName(titleFieldText, group)
                                                }
                                                titleFieldText = group.name
                                                toggleEditing()
                                            }
                                        ) {
                                            Crossfade(targetState = isEditing) {
                                                if (it)
                                                    Icon(Icons.Outlined.Save, null)
                                                else
                                                    Icon(Icons.Outlined.Edit, null)
                                            }
                                        }
                                    }
                                    Spacer(Modifier.width(0.dp))
                                }
                        }
                    }
                    item {
                        Spacer(Modifier.height(12.dp))
                    }

                    item {
                        Row (
                            Modifier
                                .padding(12.dp, 0.dp)
                                .fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            if (group.recordings.isNotEmpty())
                                RecordingListGroupHeader(
                                    "Recordings"
                                )
                            else
                                Column (
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Spacer(Modifier.height(36.dp))
                                    Text("This group is empty!", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                                    Spacer(Modifier.height(4.dp))
                                    Text("Add recordings to keep organized.", style = MaterialTheme.typography.bodyLarge)
                                    Spacer(Modifier.height(36.dp))

                                }
                        }
                    }

                    items(
                        recordings,
                        { it }
                    ) { rec ->
                        val item = viewModel.getRecording(Uri.parse(rec))!!.let {
                            Triple(
                                it,
                                viewModel.getRecordingData(it),
                                null
                            )
                        }
                        ReorderableItem(
                            lazyListState,
                            key = rec,
                            modifier = Modifier
                                .onGloballyPositioned {
//                                    itemHeight = it.size.height.dp
                                }
                                .padding(12.dp, 0.dp)
                        ) { isDragging ->
                            val elevation = animateDpAsState(if (isDragging) 16.dp else 0.dp)

                            Column {
                                RecordingListItem(
                                    recording = item,
                                    onClick = {
                                        playbackClientControls.play(
                                            group,
                                            recordings.indexOf(item.second.recordingUri)
                                        )
                                    },
                                    onSelect = {
                                        if (selectedForRemoval.contains(item.first))
                                            selectedForRemoval.remove(item.first)
                                        else
                                            selectedForRemoval.add(item.first)
                                    },
                                    isSelected =
                                        isEditing && !selectedForRemoval.contains(item.first),
                                    showSelectButton = isEditing,
                                    roundBottom = recordings.indexOf(item.second.recordingUri) == recordings.lastIndex,
                                    draggable = true,
                                    customActions =
                                        if (isEditing) {
                                            {
                                                IconButton(
                                                    onClick = { /*TODO*/ },
                                                    modifier = Modifier.detectReorder(lazyListState)
                                                ) {
                                                    Icon(Icons.Rounded.DragIndicator, null)
                                                }
                                            }
                                        } else null,
                                    modifier = Modifier
                                        .shadow(elevation.value)
                                ) {
                                    navController.navigate(
                                        Destination.RecordingInfo.createRoute(item.first.uri)
                                    )
                                }
                                if (recordings.indexOf(item.second.recordingUri) != recordings.lastIndex && !isDragging)
                                    Divider(
                                        Modifier.fillMaxWidth(),
                                        thickness = 1.dp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .15f)
                                    )
                            }
                        }
                    }

                    item {
                        Column (horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth()){
                            Button({
                                navController.navigate(
                                    Destination.Search.createRoute(
                                        selectMode = true
                                    )
                                )
                            }) {
                                Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Add Recordings")
                            }
                            if (group.uuid != starredGroupUUID) {
                                Spacer(Modifier.height(4.dp))
                                Button(
                                    {
                                        showDeleteGroupDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Delete Group")
                                }
                            }
                        }
                    }

                    item {
                        Spacer(
                            Modifier.height(12.dp + WindowInsets.navigationBars
                                .asPaddingValues()
                                .calculateBottomPadding())
                        )
                    }
                }
            }
        }
    }

    DeleteGroupDialog(visible = showDeleteGroupDialog, onClose = { showDeleteGroupDialog = false }) {
        navController.navigateUp()
        viewModel.deleteGroup(group)
        showDeleteGroupDialog = false
    }
}

@Composable
fun DeleteGroupDialog(visible: Boolean, onClose: () -> Unit, onConfirm: () -> Unit) {
    if (visible)
        AlertDialog(
            onDismissRequest = onClose,
            icon = {Icon(Icons.Rounded.DeleteForever, null)},
            title = { Text("Delete Group?") },
            text = {
                Column {
                    Text("Deleting a group is permanent and cannot be undone. Note this will not delete any recordings.")
                }
            },
            confirmButton = {
                Button(onClick = onConfirm) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = onClose) {
                    Text("Close")
                }
            }
        )
}