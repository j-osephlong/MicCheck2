package com.jlong.miccheck.ui.compose

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.composable
import com.jlong.miccheck.*
import com.jlong.miccheck.ui.theme.surfaceColorAtElevation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.xdrop.fuzzywuzzy.FuzzySearch

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.addSearchScreenGraph(
    navController: NavController,
    viewModel: MicCheckViewModel,
    playbackClientControls: PlaybackClientControls,
    pickImage: ((Uri)->Unit, ()->Unit ) -> Unit
) {
    composable(
        Destination.Search.route,
        arguments = listOf(
            navArgument("tag") {
                defaultValue = null
                type = NavType.StringType
                nullable = true
            },
            navArgument("selectMode") {
                defaultValue = false
                type = NavType.BoolType
                nullable = false
            }
        )
    ) { backStackEntry ->
        val tagFilter = backStackEntry.arguments?.getString("tag")?.let { tag ->
            viewModel.tags.find { it.name == tag }
        }

        val selectMode = backStackEntry.arguments?.getBoolean("selectMode") ?: false
        viewModel.searchScreenInSelectMode = selectMode

        SearchScreen(
            navController,
            viewModel,
            playbackClientControls,
            pickImage,
            tagFilter,
            selectMode = selectMode
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchScreen (
    navController: NavController,
    viewModel: MicCheckViewModel,
    playbackClientControls: PlaybackClientControls,
    pickImage: ((Uri)->Unit, ()->Unit ) -> Unit,
    tagFilterFromNav: Tag? = null,
    selectMode: Boolean = false
) {
    val coroutineScope = rememberCoroutineScope()

    var results by remember {
        mutableStateOf(listOf<Searchable>())
    }

    LaunchedEffect(key1 = true) {
        viewModel.clearSelectedRecordings()
    }

    BackHandler(true) {
        viewModel.clearSelectedRecordings()
        navController.navigateUp()
    }

    var typeFilter by remember { mutableStateOf(if (!selectMode) 0 else 1) }
    var tagFilter by remember {
        mutableStateOf(tagFilterFromNav)
    }

    LaunchedEffect(key1 = viewModel.currentSearchString) {
        results = searchQuery(viewModel, viewModel.currentSearchString, typeFilter, tagFilter)
    }
    LaunchedEffect(key1 = typeFilter) {
        results = searchQuery(viewModel, viewModel.currentSearchString, typeFilter, tagFilter)
    }
    LaunchedEffect(key1 = tagFilter) {
        results = searchQuery(viewModel, viewModel.currentSearchString, typeFilter, tagFilter)
    }

    var showExistingGroupDialog by remember { mutableStateOf(false) }
    var showNewGroupDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }

    var showFilterRow by remember { mutableStateOf(true) }
    var showSelectionRow by remember { mutableStateOf(false) }
    val rowAnimationSpecTime = 100

    val setShowSelectionRow: (Boolean) -> Unit = {
        if (it) {
            coroutineScope.launch {
                showFilterRow = false
                delay(rowAnimationSpecTime.toLong() + 50)
                showSelectionRow = true
            }
        } else {
            coroutineScope.launch {
                showSelectionRow = false
                delay(rowAnimationSpecTime.toLong() + 50)
                showFilterRow = true
            }
        }
    }

    LaunchedEffect(key1 = viewModel.selectedRecordings.isNotEmpty()) {
        if (!selectMode)
            setShowSelectionRow(viewModel.selectedRecordings.isNotEmpty())
    }

    val chipColors = ChipColors.outlinedChipColors().copy(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        outline = Color.Transparent
    )

    Surface (Modifier.fillMaxSize()) {
        LazyColumn (
            Modifier
                .fillMaxWidth()
        ) {
            item {
                Spacer(Modifier.height(12.dp))
            }

            item {
                Box (Modifier.height(32.dp)) {
                    AnimatedVisibility(
                        visible = showFilterRow,
                        enter = fadeIn(tween(rowAnimationSpecTime)) + slideInVertically(tween(rowAnimationSpecTime)),
                        exit = fadeOut(tween(rowAnimationSpecTime)) + slideOutVertically(tween(rowAnimationSpecTime))
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            Spacer(Modifier.width(12.dp))
                            OutlinedChip(
                                text = { Text(tagFilter?.name ?: "Tag") },
                                enabled = true,
                                leadingIcon = if (tagFilter != null) Icons.Outlined.Close else Icons.Outlined.Label,
                                showLeadingIcon = true,
                                onClickLeadingIcon = if (tagFilter != null) {
                                    {
                                        tagFilter = null
                                    }
                                } else null
                            ) {
                                showTagDialog = true
                            }
                            if (!selectMode) {
                                Spacer(Modifier.width(8.dp))
                                OutlinedChip(
                                    text = { Text("Recordings") },
                                    enabled = typeFilter == 1,
                                    colors = chipColors
                                ) {
                                    typeFilter = if (typeFilter == 1) 0 else 1
                                }
                                Spacer(Modifier.width(8.dp))
                                OutlinedChip(
                                    text = { Text("Groups") },
                                    enabled = typeFilter == 2,
                                    colors = chipColors
                                ) {
                                    typeFilter = if (typeFilter == 2) 0 else 2
                                }
                                Spacer(Modifier.width(8.dp))
                                OutlinedChip(
                                    text = { Text("Timestamps") },
                                    enabled = typeFilter == 3,
                                    colors = chipColors
                                ) {
                                    typeFilter = if (typeFilter == 3) 0 else 3
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                        }
                    }

                    AnimatedVisibility(
                        visible = showSelectionRow,
                        enter = fadeIn(tween(rowAnimationSpecTime)) + slideInVertically(tween(rowAnimationSpecTime)),
                        exit = fadeOut(tween(rowAnimationSpecTime)) + slideOutVertically(tween(rowAnimationSpecTime))
                    ) {
                        Row(Modifier.horizontalScroll(rememberScrollState())) {
                            Spacer(Modifier.width(12.dp))
                            OutlinedChip(
                                text = { Text("Clear Selection") },
                                leadingIcon = Icons.Rounded.Clear,
                                showLeadingIcon = true,
                                colors = ChipColors.outlinedChipColors().copy(
                                    color = MaterialTheme.colorScheme.background
                                )
                            ) {
                                viewModel.clearSelectedRecordings()
                            }
                            Spacer(Modifier.width(12.dp))
                            OutlinedChip(
                                text = { Text("Group") },
                                leadingIcon = Icons.Rounded.Inventory2,
                                showLeadingIcon = true,
                                colors = ChipColors.outlinedChipColors().copy(
                                    color = MaterialTheme.colorScheme.background
                                )
                            ) {
                                if (viewModel.groups.isNotEmpty())
                                    showExistingGroupDialog = true
                                else
                                    showNewGroupDialog = true
                            }
                            Spacer(Modifier.width(12.dp))
                            OutlinedChip(
                                text = { Text("Delete") },
                                leadingIcon = Icons.Rounded.Delete,
                                showLeadingIcon = true,
                                colors = ChipColors.outlinedChipColors().copy(
                                    color = MaterialTheme.colorScheme.background
                                )
                            ) {
                                playbackClientControls.deleteRecordings(viewModel.selectedRecordings)
                            }
                            Spacer(Modifier.width(12.dp))
                        }
                    }
                }

            }

            item {
                if (results.isNotEmpty())
                    Column (Modifier.padding(12.dp, 0.dp)) {
                        Spacer(Modifier.height(12.dp))
                        RecordingListGroupHeader(label = "Results")
                    }
            }

            itemsIndexed(results) { index, it ->
                Column (
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp, 0.dp)
                        .animateItemPlacement()
                ){
                    when (it) {
                        is Recording -> it.let {
                            RecordingListItem(
                                recording = Triple(
                                    it,
                                    viewModel.getRecordingData(it),
                                    viewModel.getGroups(it).elementAtOrNull(0)
                                ),
                                onClick = {
                                    if (viewModel.selectedRecordings.isEmpty())
                                        playbackClientControls.play(
                                            Triple(
                                                it,
                                                viewModel.getRecordingData(it),
                                                viewModel.getGroups(it).elementAtOrNull(0)
                                            )
                                        )
                                    else {
                                        if (viewModel.selectedRecordings.contains(it))
                                            viewModel.selectedRecordings.remove(it)
                                        else
                                            viewModel.selectedRecordings.add(it)
                                    }
                                },
                                onSelect = {
                                    if (viewModel.selectedRecordings.contains(it))
                                        viewModel.selectedRecordings.remove(it)
                                    else
                                        viewModel.selectedRecordings.add(it)
                                },
                                isSelected = viewModel.selectedRecordings.contains(it),
                                showSelectButton = viewModel.selectedRecordings.isNotEmpty() || selectMode,
                                roundBottom = index == results.lastIndex
                            ) {
                                navController.navigate(Destination.RecordingInfo.createRoute(it.uri))
                            }
                        }
                        is TimeStamp -> {
                            it.let { timestamp ->
                                viewModel.getRecording(timestamp)?.let { recording ->
                                    TimestampSearchResult(
                                        timeStamp = timestamp,
                                        recording = recording,
                                        onClickRecordingTag = {
                                            navController.navigate(
                                                Destination.RecordingInfo.createRoute(
                                                    recording.uri
                                                )
                                            )
                                        },
                                        onClick = {
                                            playbackClientControls.playFromTimestamp(
                                                recording = Triple(
                                                    recording,
                                                    viewModel.getRecordingData(recording),
                                                    viewModel.getGroups(recording).elementAtOrNull(0)
                                                ),
                                                timestamp
                                            )
                                        },
                                        roundBottom = index == results.lastIndex
                                    )
                                }
                            }
                        }
                        is RecordingGroup -> {
                            it.let {
                                RecordingGroupSearchResult(
                                    group = it,
                                    roundBottom = index == results.lastIndex
                                ) {navController.navigate(Destination.Group.createRoute(it.uuid))}
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(12.dp + WindowInsets.navigationBars
                    .asPaddingValues()
                    .calculateBottomPadding()))
            }
        }
    }

    AddToExistingGroupDialog(
        visible = showExistingGroupDialog,
        groups = viewModel.groups,
        onClose = { showExistingGroupDialog = false },
        onCreateNew = { showExistingGroupDialog = false; showNewGroupDialog = true },
        onConfirm = {
            viewModel.selectedRecordings.forEach { recording ->
                viewModel.addRecordingToGroup(
                    it,
                    recording
                )
            }
            viewModel.clearSelectedRecordings()
            showExistingGroupDialog = false
        }
    )

    NewGroupDialog(visible = showNewGroupDialog, onClose = { showNewGroupDialog = false }, pickImage = pickImage) { name, uri ->
        val group = viewModel.createGroup(name, uri)

        viewModel.selectedRecordings.forEach { recording ->
            viewModel.addRecordingToGroup(
                group,
                recording
            )
        }
        viewModel.clearSelectedRecordings()
        showNewGroupDialog = false
    }

    SelectTagDialog(visible = showTagDialog, tags = viewModel.tags, onClose = { showTagDialog = false }) {
        tagFilter = it
        showTagDialog = false
    }
}

fun searchQuery (
    viewModel: MicCheckViewModel,
    query: String,
    typeFilter: Int,
    tagFilter: Tag?
) : List<Searchable> {
    val recordingsFilteredByTag = if (tagFilter != null)
        viewModel.recordings.filter {
            viewModel.getRecordingData(it).tags.find { tag -> tag.name == tagFilter.name } != null
        } else viewModel.recordings

    if (query.isBlank() && tagFilter != null && typeFilter == 1)
        return recordingsFilteredByTag.toMutableList()

    val timeStamps = recordingsFilteredByTag
        .map { viewModel.getRecordingData(it) }
        .let {
            val t = mutableListOf<TimeStamp>()
            it.forEach {
                t += it.timeStamps
            }
            t
        }

    val combinedSet = mutableListOf<Searchable>().apply {
        if (typeFilter == 0 || typeFilter == 1) addAll(recordingsFilteredByTag)
        if ((typeFilter == 0 || typeFilter == 2) && tagFilter == null) addAll(viewModel.groups)
        if ((typeFilter == 0 || typeFilter == 3) && tagFilter == null) addAll(timeStamps)
    }

    val results = if (tagFilter != null) FuzzySearch.extractSorted(
            query,
            combinedSet.toMutableList()
        ) { item -> item.name }
    else
        FuzzySearch.extractSorted(
            query,
            combinedSet.toMutableList(),
            {item -> item.name},
            40
        )

    return results.map {
        it.referent
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onSetQuery: (String) -> Unit,
    onBack: () -> Unit
) {
    val vT = VisualTransformation.None
    val interactionSource = remember {
        MutableInteractionSource()
    }

    Surface (
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier
            .padding(12.dp, 4.dp)
            .fillMaxWidth()
            .height(48.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(38.dp),
    ) {
        BasicTextField(
            value = query,
            onValueChange = onSetQuery,
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = vT,
            interactionSource = interactionSource,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = Brush.linearGradient(listOf(MaterialTheme.colorScheme.onSurface,MaterialTheme.colorScheme.onSurface))
        ) { innerTextField ->
            TextFieldDefaults.TextFieldDecorationBox(
                value = query,
                visualTransformation = vT,
                innerTextField = innerTextField,
                singleLine = true,
                enabled = true,
                interactionSource = interactionSource,
                placeholder = { Text("Search recordings") },
                colors = TextFieldDefaults.textFieldColors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(28.dp),
                    textColor = MaterialTheme.colorScheme.onSurface
                ),
                contentPadding = PaddingValues(16.dp, 8.dp),
                leadingIcon = {
                    IconButton(onClick = onBack){ Icon(Icons.Rounded.ArrowBack, null) }
                },
                trailingIcon = if (query.isNotBlank()) {
                    {
                        IconButton(onClick = {onSetQuery("")}){ Icon(Icons.Rounded.Clear, null) }
                    }
                } else null
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimestampSearchResult (
    timeStamp: TimeStamp,
    recording: Recording,
    onClickRecordingTag: () -> Unit,
    onClick: () -> Unit,
    roundBottom: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface (
        shape = RoundedCornerShape(
            0.dp, 0.dp,
            if (roundBottom) 18.dp else 0.dp,
            if (roundBottom) 18.dp else 0.dp
        ),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row (
            modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 0.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ){
            Column {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    onClick = onClickRecordingTag
                ) {
                    Text(
                        "Timestamp of \"${recording.name}\"",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(8.dp, 4.dp)
                    )
                }
                Spacer(Modifier.height(0.dp))
                Text(
                    timeStamp.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    timeStamp.description.ifBlank { "No description." },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .weight(1f))
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ) {
                Text(
                    timeStamp.timeMilli.toTimestamp(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(8.dp, 4.dp)
                )
            }
        }
    }
}

@Composable
fun RecordingGroupSearchResult (
    group: RecordingGroup,
    roundBottom: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface (
        shape = RoundedCornerShape(
            0.dp, 0.dp,
            if (roundBottom) 18.dp else 0.dp,
            if (roundBottom) 18.dp else 0.dp
        ),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box (
            Modifier
                .fillMaxWidth()
                .padding(
                    top = 18.dp,
                    start = 18.dp,
                    end = 18.dp,
                    bottom = if (roundBottom) 18.dp else 0.dp
                )) {
            GroupCard(group = group) {onClick()}
        }
    }
}