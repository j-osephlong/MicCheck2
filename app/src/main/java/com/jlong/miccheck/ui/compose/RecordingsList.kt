package com.jlong.miccheck.ui.compose

import android.net.Uri
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.HourglassFull
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import coil.compose.AsyncImage
import com.google.accompanist.navigation.animation.composable
import com.jlong.miccheck.*
import com.jlong.miccheck.ui.theme.surfaceColorAtElevation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

enum class SortType (val text: String) {
    DateNewest ("Date"),
    DateOldest ("Date (Oldest)"),
    AlphabeticalAZ ("Alphabetical (A-Z)"),
    AlphabeticalZA ("Alphabetical (Z-A)"),
    LengthShortest ("Length (Shortest)"),
    LengthLongest ("Length (Longest)")
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.addRecordingsListGraph(
    navController: NavController,
    viewModel: MicCheckViewModel,
    playbackClientControls: PlaybackClientControls,
    recorderClientControls: RecorderClientControls,
    setBackdrop: (Boolean) -> Unit,
    pickImage: ((Uri)->Unit, ()->Unit ) -> Unit
) {
    Log.i("Navigation", "Nav component list composed.")

    composable(Destination.RecordingsList.route,
    ) {
        RecordingsListFrame(
            navController,
            viewModel,
            playbackClientControls,
            recorderClientControls,
            setBackdrop,
            pickImage
        )
    }
}

@Composable
fun RecordingsListFrame(
    navController: NavController,
    viewModel: MicCheckViewModel,
    playbackClientControls: PlaybackClientControls,
    recorderClientControls: RecorderClientControls,
    setBackdrop: (Boolean) -> Unit,
    pickImage: ((Uri)->Unit, ()->Unit ) -> Unit
) {

    Log.i("Navigation", "Recordings list frame composed.")
    Crossfade(targetState = viewModel.showingGroupsList) {
        if (!it)
            RecordingsList(
                navController,
                viewModel,
                playbackClientControls,
                recorderClientControls,
                setBackdrop,
                pickImage
            )
        else {
            GroupList(
                navController = navController,
                viewModel = viewModel,
                recorderClientControls = recorderClientControls,
                pickImage
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecordingsList(
    navController: NavController,
    viewModel: MicCheckViewModel,
    playbackClientControls: PlaybackClientControls,
    recorderClientControls: RecorderClientControls,
    setBackdrop: (Boolean) -> Unit,
    pickImage: ((Uri)->Unit, ()->Unit ) -> Unit
) {
    Log.i("Navigation", "Recordings list composed.")
    val coroutineScope = rememberCoroutineScope()
    var recordingTriples by remember {
        mutableStateOf(listOf<Triple<Recording, RecordingData, RecordingGroup?>>())
    }

    recordingTriples = viewModel.recordings
        .map { rec ->
            val data = viewModel.getRecordingData(rec)
            val group = viewModel.getGroups(rec).elementAtOrNull(0)
            Triple(
                rec,
                data,
                group
            )
        }

    val todayRecordings = recordingTriples.filter { it.first.date.toLocalDate() == LocalDate.now() }
    val yesterdayRecordings = recordingTriples.filter { it.first.date.toLocalDate() == LocalDate.now().minusDays(1) }
    val thisWeekRecordings = recordingTriples.filter {
        it.first.date.toLocalDate().toEpochDay() > LocalDate.now().minusDays(7).toEpochDay() &&
                !todayRecordings.contains(it) &&
                !yesterdayRecordings.contains(it)
    }

    val onSelectRecording: (Recording) -> Unit = {
        if (viewModel.selectedRecordings.contains(it))
            viewModel.selectedRecordings -= it
        else
            viewModel.selectedRecordings += it
    }

    val onClickRecording: (Triple<Recording, RecordingData, RecordingGroup?>) -> Unit = {
        if (viewModel.selectedRecordings.isEmpty()) {
            playbackClientControls.play(it)
            setBackdrop(true)
        } else {
            onSelectRecording(it.first)
        }
    }

    val onClickOpenRecording: (Recording) -> Unit = {
        navController.navigate(Destination.RecordingInfo.createRoute(it.uri))
    }

    val chipColors = ChipColors.outlinedChipColors().copy(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        outline = Color.Transparent
    )

    val lazyColumnState = rememberLazyListState()

    var showExistingGroupDialog by remember { mutableStateOf(false) }
    var showNewGroupDialog by remember { mutableStateOf(false) }
    var showAddTagDialog by remember { mutableStateOf(false) }

    var showSortScrim by remember {
        mutableStateOf(false)
    }

    Box (Modifier.fillMaxSize()){
        LazyColumn(
            Modifier
                .fillMaxSize(),
            state = lazyColumnState
        ) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp, 0.dp)
                ) {
                    Spacer(Modifier.height(12.dp))
                    Row (Modifier.fillMaxWidth()){
                        OutlinedChip(
                            text = { Text("By ${viewModel.recordingsSortType.text}") },
                            leadingIcon = when (viewModel.recordingsSortType) {
                                SortType.DateOldest -> Icons.Outlined.Sort
                                SortType.DateNewest -> Icons.Outlined.Sort
                                SortType.AlphabeticalAZ, SortType.AlphabeticalZA -> Icons.Outlined.SortByAlpha
                                SortType.LengthShortest -> Icons.Outlined.HourglassBottom
                                SortType.LengthLongest -> Icons.Outlined.HourglassFull
                            },
                            showLeadingIcon = true,
                            colors = chipColors
                        ) {
                            coroutineScope.launch {
                                lazyColumnState.animateScrollToItem(0)
                                showSortScrim = true
                            }
                        }
                        Spacer(Modifier.fillMaxWidth().weight(1f))
                        OutlinedChip(
                            text = { Text("Groups") },
                            leadingIcon = Icons.Rounded.LibraryMusic,
                            showLeadingIcon = true,
                            trailingIcon = Icons.Rounded.ArrowForward,
                            colors = chipColors.copy(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                leadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            enabled = true
                        ) {
                            viewModel.showingGroupsList = !viewModel.showingGroupsList
                        }
                    }
                }
            }

            item {
                DismissableExtra(
                    visible = viewModel.deniedPermissions.isNotEmpty(),
                    title = { Text("Needed Permissions Denied") },
                    text = {Text("micCheck only asks for permissions explicitly needed for the app to function normally. Tap here to fix.")},
                    dismissIcon = Icons.Rounded.ArrowForward,
                    leadingIcon = Icons.Rounded.VerifiedUser,
                    modifier = Modifier.padding(top = 12.dp, start = 12.dp, end = 12.dp),
                    onClick = {playbackClientControls.openPermissions()}
                ) {
                    playbackClientControls.openPermissions()
                }
            }

            item {
                DismissableExtra(
                    visible = !viewModel.settings.dismissedExtras.contains(DismissableExtraId.WhatsNewV2),
                    title = { Text("Check out what's new") },
                    dismissIcon = Icons.Rounded.ArrowForward,
                    modifier = Modifier.padding(top = 12.dp, start = 12.dp, end = 12.dp)
                ) {
                    navController.navigate(Destination.WhatsNew.route)
                    viewModel.dismissExtra(DismissableExtraId.WhatsNewV2)
                }
            }

            stickyHeader {
                Column {
                    AnimatedVisibility(
                        visible = (viewModel.recordingState != RecordingState.WAITING &&
                                viewModel.recordingState != RecordingState.STOPPED) || viewModel.currentRecordingUri != null,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(Modifier.height(12.dp))
                            RecordingControlsCard(
                                lazyColumnState,
                                viewModel,
                                recorderClientControls
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = viewModel.selectedRecordings.isNotEmpty(),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(Modifier.height(12.dp))
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
                                    text = { Text("Tag") },
                                    leadingIcon = Icons.Rounded.Label,
                                    showLeadingIcon = true,
                                    colors = ChipColors.outlinedChipColors().copy(
                                        color = MaterialTheme.colorScheme.background
                                    )
                                ) {
                                    showAddTagDialog = true
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
            }

            item {
                if (viewModel.recordings.isEmpty())
                    NoRecordingsTip()
            }

            item {
                Spacer(Modifier.height(12.dp))
            }

            if (todayRecordings.isNotEmpty() && viewModel.recordingsSortType == SortType.DateNewest) {
                recordingListGroup(
                    todayRecordings,
                    onClickRecording,
                    onClickOpenRecording,
                    viewModel.selectedRecordings,
                    onSelectRecording,
                    label = "Today"
                )
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            if (yesterdayRecordings.isNotEmpty() && viewModel.recordingsSortType == SortType.DateNewest) {
                recordingListGroup(
                    yesterdayRecordings,
                    onClickRecording,
                    onClickOpenRecording,
                    viewModel.selectedRecordings,
                    onSelectRecording,
                    label = "Yesterday"
                )
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            if (thisWeekRecordings.isNotEmpty() && viewModel.recordingsSortType == SortType.DateNewest) {
                recordingListGroup(
                    thisWeekRecordings,
                    onClickRecording,
                    onClickOpenRecording,
                    viewModel.selectedRecordings,
                    onSelectRecording,
                    label = "This Week"
                )
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            val remaining by mutableStateOf(

                    when (viewModel.recordingsSortType) {
                        SortType.AlphabeticalAZ ->
                            recordingTriples.sortedBy {
                                it.first.name.lowercase(Locale.getDefault())
                            }
                        SortType.AlphabeticalZA ->
                            recordingTriples.sortedByDescending {
                                it.first.name.lowercase(Locale.getDefault())
                            }
                        SortType.LengthLongest ->
                            recordingTriples.sortedByDescending {
                                it.first.duration
                            }
                        SortType.LengthShortest ->
                            recordingTriples.sortedBy {
                                it.first.duration
                            }
                        SortType.DateOldest ->
                            recordingTriples.sortedBy {
                                it.first.date
                            }
                        SortType.DateNewest ->
                            recordingTriples.filter {
                                !todayRecordings.contains(it) && !yesterdayRecordings.contains(it) &&
                                        !thisWeekRecordings.contains(it)
                            }
                    }
            )

            recordingListGroup(
                recordings = remaining,
                onClickRecording = onClickRecording,
                onClickOpenRecording = onClickOpenRecording,
                viewModel.selectedRecordings,
                onSelectRecording = onSelectRecording,
                label = if (viewModel.recordingsSortType == SortType.DateNewest || viewModel.recordingsSortType == SortType.DateOldest)
                    null
                else
                    "Recordings"
            )
            item {
                Spacer(modifier = Modifier.height(WindowInsets.navigationBars
                    .asPaddingValues()
                    .calculateBottomPadding()))
            }
        }

        SortScrim(
            selectedSort = viewModel.recordingsSortType,
            visible = showSortScrim,
            setType = { viewModel.recordingsSortType = it }
        ) {
            showSortScrim = false
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

    AddTagDialog(visible = showAddTagDialog, tags = viewModel.tags, onClose = { showAddTagDialog = false }) {
        viewModel.selectedRecordings.forEach { recording ->
            viewModel.addTagsToRecording(viewModel.getRecordingData(recording = recording), it)
        }
        showAddTagDialog = false
    }
}

@Composable
fun RecordingListGroupHeader (
    label: String
) {
    Surface(
        shape = RoundedCornerShape(18.dp, 18.dp, 0.dp, 0.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(18.dp, 18.dp, 18.dp, 0.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.recordingListGroup(
    recordings: List<Triple<Recording, RecordingData, RecordingGroup?>>,
    onClickRecording: (Triple<Recording, RecordingData, RecordingGroup?>) -> Unit,
    onClickOpenRecording: ((Recording) -> Unit),
    selectedRecordings: List<Recording>,
    onSelectRecording: (Recording) -> Unit,
    label: String? = null
) {
    itemsIndexed (recordings) { index, recording ->
        Column (
            Modifier
                .fillMaxWidth()
                .padding(12.dp, 0.dp)
                .animateItemPlacement()
        ) {
            if (index != 0) {
                if (recordings[index - 1].first.date.toLocalDate().month != recording.first.date.toLocalDate().month && label == null) {
                    Spacer(Modifier.height(12.dp))
                    RecordingListGroupHeader(
                        label ?: recording.first.date.month.getDisplayName(
                            TextStyle.FULL, Locale.getDefault()
                        )
                    )
                }
            }
            else {
                RecordingListGroupHeader(label ?: recording.first.date.month.getDisplayName(
                    TextStyle.FULL, Locale.getDefault()
                ))
            }

            RecordingListItem(
                recording,
                { onClickRecording(recording) },
                { onSelectRecording(recording.first) },
                roundBottom = let {
                    if (index != recordings.lastIndex) {
                        recordings[index + 1].first.date.month != recording.first.date.month &&
                                label == null
                    } else true
                },
                isSelected = selectedRecordings.contains(recording.first),
                showSelectButton = selectedRecordings.isNotEmpty()
            ) { onClickOpenRecording(recording.first) }

            if (index != recordings.lastIndex)
                if (recordings[index+1].first.date.month == recording.first.date.month ||
                        label != null)
                    Divider(
                        Modifier.fillMaxWidth(),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .15f)
                    )
        }
    }
}


@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecordingListItem(
    recording: Triple<Recording, RecordingData, RecordingGroup?>,
    onClick: () -> Unit,
    onSelect: () -> Unit,
    isSelected: Boolean,
    showSelectButton: Boolean,
    customActions: (@Composable RowScope.() -> Unit)? = null,
    roundBottom: Boolean = false,
    draggable: Boolean = false,
    modifier: Modifier = Modifier,
    onClickOpen: () -> Unit
) {
    fun dateString (date: LocalDateTime) = date.format(DateTimeFormatter.ofPattern("h:mm a, MMMM dd" + if (date.year != LocalDate.now().year) " yyyy" else ""))

    val bottomCornerDp by animateDpAsState(if (roundBottom) 18.dp else 0.dp, tween(500))

    val tags = buildAnnotatedString {
        (listOf(Tag(recording.first.duration.toLong().toTimestamp()))+recording.second.tags).let{
            it.forEachIndexed { index, tag ->
                if (index == 0)
                    withStyle(
                        MaterialTheme.typography.labelMedium.toSpanStyle().copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(tag.name)
                    }
                else
                    withStyle(
                        MaterialTheme.typography.labelMedium.toSpanStyle().copy(color = MaterialTheme.colorScheme.primary)
                    ) {
                        append(tag.name)
                    }
                if (index != it.lastIndex)
                    withStyle(MaterialTheme.typography.labelMedium.toSpanStyle()) {
                        append(" • ")
                    }
            }
        }

    }

    Surface (
        shape = RoundedCornerShape(
            0.dp, 0.dp,
            bottomCornerDp, bottomCornerDp
        ),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = if (!draggable) {
                    { onSelect() }
                } else null
            ),
        tonalElevation = 1.dp
    ) {
        Column {
            Spacer(Modifier.height(18.dp))
            Row(
                Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                AnimatedVisibility(
                    visible = showSelectButton,
                    enter = fadeIn(tween(100)) + slideInHorizontally(tween(100)),
                    exit = fadeOut(tween(100)) + slideOutHorizontally(tween(100)),
                    modifier = Modifier.animateContentSize()
                ) {
                    Row {
                        Spacer(Modifier.width(18.dp))
                        FilledTonalIconToggleButton(
                            checked = isSelected,
                            onCheckedChange = { onSelect() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Rounded.Check, null, modifier = Modifier.size(
                                animateDpAsState(targetValue = if (isSelected) 16.dp else 12.dp).value
                            ))
                        }
                    }
                }
                Spacer(Modifier.width(18.dp))
                recording.third?.imgUri?.let {
                    AsyncImage(
                        model = Uri.parse(it),
                        contentDescription = null,
                        modifier = Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(modifier = Modifier.width(18.dp))
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .animateContentSize()
                ) {

                    Text(
                        recording.first.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    if (tags.isNotBlank()) {
                        Text(tags, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(4.dp))
                    }
                    Text(
                        dateString(recording.first.date),
                        style = MaterialTheme.typography.labelMedium.copy(fontStyle = FontStyle.Italic)
                    )
                }
                Crossfade(targetState = customActions == null){
                    if (it) {
                        IconButton(onClick = onClickOpen) {
                            Icon(Icons.Rounded.ArrowForward, null)
                        }
                    } else {
                        if (customActions != null) {
                            customActions()
                        }
                    }
                }
                Spacer(Modifier.width(18.dp))
            }
            Spacer(Modifier.height(18.dp))
        }

    }
}

@Composable
fun SortScrim (
    selectedSort: SortType,
    visible: Boolean,
    setType: (SortType) -> Unit,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    val chipColors = ChipColors.outlinedChipColors().copy(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        outline = Color.Transparent
    )

    var showOptions by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(key1 = visible) {
        if (visible)
            coroutineScope.launch {
                delay(200)
                showOptions = true
            }
    }

    val onDismissAnimated: () -> Unit = {
        coroutineScope.launch {
            delay(200)
            showOptions = false
            delay(100)
            onDismiss()
        }
    }

    AnimatedVisibility (visible, enter = fadeIn(), exit = fadeOut()) {
        Surface(
            Modifier
                .fillMaxSize()
                .clickable { onDismissAnimated() },
            color = MaterialTheme.colorScheme.surface.copy(alpha = .9f),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                OutlinedChip(
                    text = { Text("By ${selectedSort.text}") },
                    leadingIcon = when (selectedSort) {
                        SortType.DateOldest -> Icons.Outlined.Sort
                        SortType.DateNewest -> Icons.Outlined.Sort
                        SortType.AlphabeticalAZ, SortType.AlphabeticalZA -> Icons.Outlined.SortByAlpha
                        SortType.LengthShortest -> Icons.Outlined.HourglassBottom
                        SortType.LengthLongest -> Icons.Outlined.HourglassFull
                    },
                    showLeadingIcon = true,
                    colors = chipColors,
                    shadowElevation = 8.dp
                ) {

                }
                AnimatedVisibility(
                    visible = showOptions,
                    enter = fadeIn(tween(100)) + slideInHorizontally(tween(100)),
                    exit = fadeOut(tween(100)) + slideOutHorizontally(tween(100))
                ) {
                    Column {
                        SortType.values().forEach {
                            if (it != selectedSort) {
                                Spacer(Modifier.height(12.dp))
                                OutlinedChip(
                                    text = { Text("By ${it.text}") },
                                    leadingIcon = when (it) {
                                        SortType.DateOldest -> Icons.Outlined.Sort
                                        SortType.DateNewest -> Icons.Outlined.Sort
                                        SortType.AlphabeticalAZ, SortType.AlphabeticalZA -> Icons.Outlined.SortByAlpha
                                        SortType.LengthShortest -> Icons.Outlined.HourglassBottom
                                        SortType.LengthLongest -> Icons.Outlined.HourglassFull
                                    },
                                    showLeadingIcon = true,
                                    colors = chipColors,
                                    shadowElevation = 8.dp
                                ) {
                                    setType(it)
                                    onDismissAnimated()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun NoRecordingsTip () {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(12.dp, 62.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "It's pretty quiet here 🦗",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Tap the microphone button in the bottom right corner to create your first recording.",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
    }

}