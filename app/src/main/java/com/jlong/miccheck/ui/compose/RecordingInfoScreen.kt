package com.jlong.miccheck.ui.compose

import android.net.Uri
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.flowlayout.FlowRow
import com.google.accompanist.navigation.animation.composable
import com.jlong.miccheck.*
import com.jlong.miccheck.ui.theme.surfaceColorAtElevation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.addRecordingInfoScreenGraph(
    navController: NavController,
    viewModel: MicCheckViewModel,
    playbackClientControls: PlaybackClientControls,
    pickImage: ((Uri)->Unit, ()->Unit ) -> Unit,
    pickFile: ((Uri) -> Unit) -> Unit,
    launchUri: (String, Uri) -> Unit
) {
    Log.i("Navigation", "Nav component list composed.")

    composable(
        Destination.RecordingInfo.route,
    ) { backStackEntry ->
        val uriString = requireNotNull(backStackEntry.arguments?.getString("uri"))
        val uri = Uri.parse("content://media/external/audio/media/$uriString")
        val recording = viewModel.getRecording(uri)

        viewModel.currentRecordingInfoScreen = recording?.let {
            Triple(
                it,
                viewModel.getRecordingData(it),
                viewModel.getGroups(it).elementAtOrNull(0)
            )
        } ?: Triple(
            Recording(Uri.EMPTY, "", 0, 0, "", path = ""),
            RecordingData(""),
            null
        )
        RecordingInfoScreen(
            viewModel,
            playbackClientControls,
            requireNotNull(viewModel.currentRecordingInfoScreen) {"The currentRecordingInfoScreen changed between setting it and passing it."},
            pickImage,
            pickFile,
            launchUri,
            navController
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingInfoScreen(
    viewModel: MicCheckViewModel,
    playbackClientControls: PlaybackClientControls,
    recording: Triple<Recording, RecordingData, RecordingGroup?>,
    pickImage: ((Uri)->Unit, ()->Unit ) -> Unit,
    pickFile: ((Uri) -> Unit) -> Unit,
    launchUri: (String, Uri) -> Unit,
    navController: NavController,
) {
    fun dateString (date: LocalDateTime) = date.format(DateTimeFormatter.ofPattern("h:mm a, MMMM dd" + if (date.year != LocalDate.now().year) " yyyy" else ""))

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val chipColors = ChipColors.outlinedChipColors().copy(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        outline = Color.Transparent
    )

    var timestampsExpanded by remember { mutableStateOf(recording.second.timeStamps.isNotEmpty()) }
    val timestampsVerticalPadding by animateDpAsState(if (timestampsExpanded) 18.dp else 8.dp)
    val timestampsExpandButtonRotation by animateFloatAsState(if (timestampsExpanded) 180f else 0f)

    var groupsExpanded by remember {
        mutableStateOf(viewModel.getGroups(recording.first).isNotEmpty() && recording.second.timeStamps.isEmpty())
    }
    val groupsVerticalPadding by animateDpAsState(if (groupsExpanded) 18.dp else 8.dp)
    val groupsExpandButtonRotation by animateFloatAsState(if (groupsExpanded) 180f else 0f)

    var showTimestampInfoDialog by remember { mutableStateOf(false) }
    var timestampForDialog by remember { mutableStateOf<TimeStamp?>(null) }

    var showTimestampEditDialog by remember { mutableStateOf(false) }

    var attachmentsExpanded by remember {
        mutableStateOf(recording.second.attachments.isNotEmpty() && recording.second.timeStamps.isEmpty())
    }
    val attachmentsVerticalPadding by animateDpAsState(if (attachmentsExpanded) 18.dp else 8.dp)
    val attachmentsExpandButtonRotation by animateFloatAsState(if (attachmentsExpanded) 180f else 0f)

    var showTagDialog by remember { mutableStateOf(false) }

    var showExistingGroupDialog by remember { mutableStateOf(false) }
    var showNewGroupDialog by remember { mutableStateOf(false) }

    var showPathInfoDialog by remember { mutableStateOf(false) }

    var showDeleteDialog by remember { mutableStateOf(false) }

    var attachmentForDialog by remember {
        mutableStateOf<Attachment?>(null)
    }
    var showAttachmentDialog by remember { mutableStateOf(false) }
    var showAddLinkAttachmentDialog by remember { mutableStateOf(false) }

    var isStarred by remember { mutableStateOf(false) }
    isStarred = viewModel.groups.find { it.uuid == starredGroupUUID }
        ?.recordings?.contains(recording.first.uri.toString())
        ?: false

    var isEditing by remember { mutableStateOf(false) }
    var titleFieldText by remember { mutableStateOf("") }
    var descriptionFieldText by remember { mutableStateOf("") }
    var fieldTextPadding by remember { mutableStateOf(0.dp) }

    val s1 = MaterialTheme.typography.displaySmall.toParagraphStyle()
    val s2 = MaterialTheme.typography.displaySmall.toSpanStyle()
    val s3 = MaterialTheme.typography.headlineLarge.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold).toSpanStyle()

    val buildTitleString: () -> AnnotatedString = {
        buildAnnotatedString {
            withStyle(s1) {
                withStyle(s2) {
                    append(recording.first.name)
                }
                withStyle(s3) {
                    append(" ")
                    append(recording.first.duration.toLong().toTimestamp())
                }
            }
        }
    }
    var titleString by remember {
        mutableStateOf(buildTitleString())
    }

    val toggleEditing: () -> Unit = {
        coroutineScope.launch {
            if (!isEditing)
                animate(0f, 16f, animationSpec = tween(200)) { value, _ ->
                    fieldTextPadding = value.dp
                }
            isEditing = !isEditing
            if (!isEditing) {
                titleString = buildTitleString()
                delay(300)
                animate(16f, 0f, animationSpec = tween(200)) { value, _ ->
                    fieldTextPadding = value.dp
                }
            }
        }
    }

    Surface (Modifier.fillMaxSize()) {

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.Start
            ) {
                Spacer(Modifier.width(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .animateContentSize()
                ) {

                    Crossfade(
                        targetState = isEditing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                    ) {
                        if (it) {
                            TextField(
                                value = titleFieldText, onValueChange = { titleFieldText = it },
                                shape = RoundedCornerShape(18.dp),
                                colors = TextFieldDefaults.textFieldColors(
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                textStyle = MaterialTheme.typography.displaySmall
                            )
                        } else
                            Text(
                                titleString,
                                style = MaterialTheme.typography.displaySmall,
                                modifier = Modifier.padding(fieldTextPadding)
                            )
                    }
//                    Text(
//                        dateString(recording.first.date),
//                        style = MaterialTheme.typography.titleMedium.copy(fontStyle = FontStyle.Italic),
//                        modifier = Modifier.padding(0.dp, 4.dp, 0.dp, 18.dp)
//                    )
                }
                FilledTonalIconToggleButton(
                    checked = isStarred,
                    onCheckedChange = {
                        if (viewModel.isRecordingStarred(recording.first))
                            viewModel.unstarRecording(recording.first)
                        else
                            viewModel.starRecording(recording.first)
                    }
                ) {
                    Icon(
                        if (isStarred)
                            Icons.Rounded.Star
                        else Icons.Rounded.StarBorder,
                        null
                    )
                }
                Spacer(Modifier.width(12.dp))
            }
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                Spacer(Modifier.width(12.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { },
                    modifier = Modifier.height(24.dp)
                ) {
                    Text(
                        dateString(recording.first.date),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
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
                        recording.first.sizeStr,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(8.dp, 4.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                recording.second.clipParentUri?.let {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = {
                            navController.navigate(
                                Destination.RecordingInfo.createRoute(
                                    Uri.parse(it)
                                )
                            )
                        },
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text(
                            "Cropped from \"${viewModel.getRecording(Uri.parse(it))?.name}\"",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(8.dp, 4.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
            }
            Spacer(Modifier.height(12.dp))

            LazyRow(Modifier.fillMaxWidth()) {
                item {
                    Spacer(Modifier.width(12.dp))
                }

                item {
                    Row {
                        OutlinedChip(
                            text = { Text("Add Tag") },
                            leadingIcon = Icons.Rounded.Add
                        ) {
                            showTagDialog = true
                        }
                    }
                }

                items(recording.second.tags) {
                    Row {
                        Spacer(Modifier.width(8.dp))
                        OutlinedChip(
                            text = { Text(it.name) },
                            colors = chipColors,
                            leadingIcon = Icons.Rounded.Clear,
                            showLeadingIcon = isEditing,
                            onClickLeadingIcon = {
                                viewModel.removeTagFromRecording(recording.second, it)
                            }
                        ) {
                            navController.navigate(Destination.Search.createRoute(tag = it))
                        }
                    }
                }

                item {
                    Spacer(Modifier.width(12.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Surface(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp, 0.dp),
                shape = RoundedCornerShape(18.dp),
                tonalElevation = 1.dp
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Crossfade(isEditing, modifier = Modifier.animateContentSize()) {
                        if (it) {
                            TextField(
                                value = descriptionFieldText,
                                onValueChange = { descriptionFieldText = it },
                                shape = RoundedCornerShape(18.dp),
                                colors = TextFieldDefaults.textFieldColors(
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                                        3.dp
                                    )
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateContentSize()
                            )
                        } else
                            Text(
                                recording.second.description.ifBlank { "This recording has no description." },
                                modifier = Modifier.padding(fieldTextPadding)
                            )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            recording.first.path,
                            Modifier
                                .fillMaxWidth()
                                .weight(1f), style = MaterialTheme.typography.labelLarge
                        )
                        IconButton(
                            onClick = { showPathInfoDialog = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Info,
                                "Info",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = { showDeleteDialog = true }) {
                            Text("Delete Recording")
                        }
                        Spacer(Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = {
                                if (isEditing) {
                                    playbackClientControls.finishRecordingEdit(
                                        recording.first,
                                        titleFieldText,
                                        descriptionFieldText
                                    ) {
                                        toggleEditing()
                                    }
                                } else {
                                    titleFieldText = recording.first.name
                                    descriptionFieldText = recording.second.description
                                    toggleEditing()
                                }
                            }
                        ) {
                            Crossfade(isEditing, modifier = Modifier.animateContentSize()) {
                                Row {
                                    if (it) {
                                        Icon(
                                            Icons.Rounded.Save,
                                            null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("Save")
                                    } else {
                                        Icon(
                                            Icons.Rounded.Edit,
                                            null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("Edit")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            //timestamps
            Spacer(Modifier.height(12.dp))
            Surface(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp, 0.dp),
                shape = RoundedCornerShape(18.dp),
                tonalElevation = 1.dp
            ) {
                val iconOffset by animateDpAsState(
                    targetValue = if (timestampsExpanded && recording.second.timeStamps.isNotEmpty())
                        8.dp
                    else
                        0.dp,
                    animationSpec = tween(500)
                )
                val iconTint by animateColorAsState(
                    targetValue = if (timestampsExpanded && recording.second.timeStamps.isNotEmpty())
                        MaterialTheme.colorScheme.secondary
                    else
                        MaterialTheme.colorScheme.onSurface,
                    animationSpec = tween(500)
                )
                Surface(
                    color = Color.Transparent,
                    onClick = { timestampsExpanded = !timestampsExpanded }) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(18.dp, timestampsVerticalPadding)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Place,
                                null,
                                modifier = Modifier.offset(
                                    0.dp,
                                    iconOffset
                                ),
                                tint = iconTint
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Timestamps",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Spacer(
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                            IconButton({ timestampsExpanded = !timestampsExpanded }) {
                                Icon(
                                    Icons.Rounded.ExpandMore,
                                    null,
                                    modifier = Modifier.rotate(timestampsExpandButtonRotation)
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = timestampsExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            val lineColor = MaterialTheme.colorScheme.secondary

                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (recording.second.timeStamps.isNotEmpty())
                                            Modifier.drawBehind {
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
                                        else Modifier
                                    )
                            ) {
                                if (recording.second.timeStamps.isEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "This recording has no timestamps.",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontStyle = FontStyle.Italic
                                        )
                                    )
                                }
                                recording.second.timeStamps.forEachIndexed { index, it ->
                                    Spacer(Modifier.height(18.dp))
                                    TimestampListItem(
                                        timeStamp = it,
                                        lastItem = index == recording.second.timeStamps.lastIndex
                                    ) {
                                        timestampForDialog = it
                                        showTimestampInfoDialog = true
                                    }
                                }
                            }
                        }
                    }
                }
            }

            //groups
            Spacer(Modifier.height(12.dp))
            Surface(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp, 0.dp),
                shape = RoundedCornerShape(18.dp),
                tonalElevation = 1.dp
            ) {
                Surface(
                    color = Color.Transparent,
                    onClick = { groupsExpanded = !groupsExpanded }) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(18.dp, groupsVerticalPadding)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.LibraryMusic, null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Groups${if (viewModel.getGroups(recording.first).isNotEmpty()) " (${viewModel.getGroups(recording.first).size})" else ""}",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Spacer(
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                            IconButton({ groupsExpanded = !groupsExpanded }) {
                                Icon(
                                    Icons.Rounded.ExpandMore,
                                    null,
                                    modifier = Modifier.rotate(groupsExpandButtonRotation)
                                )
                            }
                        }
                        AnimatedVisibility(
                            visible = groupsExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            BoxWithConstraints(Modifier.fillMaxWidth()) {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                ) {
                                    Spacer(Modifier.height(12.dp))
                                    FlowRow(
                                        Modifier.fillMaxWidth(),
                                        mainAxisSpacing = 12.dp,
                                        crossAxisSpacing = 8.dp
                                    ) {
                                        viewModel.getGroups(recording.first).forEach {
                                            SmallGroupListItem(group = it) {
                                                navController.navigate(
                                                    Destination.Group.createRoute(it.uuid)
                                                )
                                            }
                                        }

                                        OutlinedIconButton(
                                            onClick = {
                                                if (viewModel.groups.isNotEmpty())
                                                    showExistingGroupDialog = true
                                                else
                                                    showNewGroupDialog = true
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.size(72.dp)
                                        ) {
                                            Icon(Icons.Rounded.Add, null)
                                        }

                                    }
                                }
                            }
                        }
                    }
                }
            }

            //attachments
            Spacer(Modifier.height(12.dp))
            Surface(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp, 0.dp),
                shape = RoundedCornerShape(18.dp),
                tonalElevation = 1.dp
            ) {
                Surface(
                    color = Color.Transparent,
                    onClick = { attachmentsExpanded = !attachmentsExpanded }) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(18.dp, attachmentsVerticalPadding)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.LibraryMusic, null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Attachments${if (recording.second.attachments.isNotEmpty()) " (${recording.second.attachments.size})" else ""}",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Spacer(
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                            IconButton({ attachmentsExpanded = !attachmentsExpanded }) {
                                Icon(
                                    Icons.Rounded.ExpandMore,
                                    null,
                                    modifier = Modifier.rotate(attachmentsExpandButtonRotation)
                                )
                            }
                        }
                        AnimatedVisibility(
                            visible = attachmentsExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column {
                                Spacer(Modifier.height(18.dp))

                                recording.second.attachments.forEach {
                                    Column {
                                        FileListItem(attachment = it) {
                                            attachmentForDialog = it
                                            showAttachmentDialog = true
                                        }
                                        Spacer(Modifier.height(8.dp))
                                    }
                                }

                                AddAttachmentButtons ({showAddLinkAttachmentDialog = true}) {
                                    pickFile { uri ->
                                        DocumentFile.fromSingleUri(context, uri).also {
                                            viewModel.addAttachmentToRecording(
                                                recording.first,
                                                Attachment(
                                                    uri.toString(),
                                                    it?.name ?: return@pickFile,
                                                    it.name ?: return@pickFile,
                                                    it.type ?: return@pickFile
                                                )
                                            )
                                        }
                                    }
                                }

                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(144.dp + WindowInsets.navigationBars
                .asPaddingValues()
                .calculateBottomPadding()))
        }
    }
    
    NewLinkAttachmentDialog(visible = showAddLinkAttachmentDialog, onClose = { showAddLinkAttachmentDialog = false }) {
        val url = it.let {
            if (!it.startsWith("https://") && !it.startsWith("http://"))
                "https://" + it
            else it
        }
        viewModel.addAttachmentToRecording(recording.first, Attachment(url, url, url, "http"))
        showAddLinkAttachmentDialog = false
    }
    
    AttachmentDialog (
        visible = showAttachmentDialog,
        attachment = attachmentForDialog,
        onClose = {showAttachmentDialog = false},
        onEdit = {showAttachmentDialog = false}
    ) {
        attachmentForDialog?.let { launchUri(it.mimeType, Uri.parse(it.attachmentUri)) }
        showAttachmentDialog = false
    }

    TimestampDialog(visible = showTimestampEditDialog, onClose = { showTimestampEditDialog = false; timestampForDialog = null }, timestampForDialog) { title, description ->
        timestampForDialog?.also{ viewModel.editTimestamp(recording, it, title, description) }
        showTimestampEditDialog = false
        timestampForDialog = null
    }

    TimestampInfoAlert(
        visible = showTimestampInfoDialog,
        timestamp = timestampForDialog ?: TimeStamp(0L, "Timestamp", "", ""),
        onClose = { showTimestampInfoDialog = false; },
        onPlay = {
            timestampForDialog?.also { playbackClientControls.playFromTimestamp(recording, it) }
        },
        onEdit = {
            showTimestampInfoDialog = false
            showTimestampEditDialog = true
        },
        onDelete =  {
            showTimestampInfoDialog = false
            timestampForDialog?.also { viewModel.deleteTimestamp(recording, it) }
            timestampForDialog = null
        }
    )

    AddTagDialog(visible = showTagDialog, tags = viewModel.tags.filter { !recording.second.tags.contains(it) }, onClose = { showTagDialog = false }) {
        viewModel.addTagsToRecording(recording.second, it)
        showTagDialog = false
    }

    AddToExistingGroupDialog(
        visible = showExistingGroupDialog,
        groups = viewModel.groups,
        onClose = { showExistingGroupDialog = false },
        onCreateNew = { showExistingGroupDialog = false; showNewGroupDialog = true },
        onConfirm = {
            viewModel.addRecordingToGroup(it, recording.first)
            showExistingGroupDialog = false
        }
    )

    NewGroupDialog(visible = showNewGroupDialog, onClose = { showNewGroupDialog = false }, pickImage = pickImage) { name, uri ->
        val group = viewModel.createGroup(name, uri)
        viewModel.addRecordingToGroup(group, recording.first)
        showNewGroupDialog = false
    }

    PathInfoDialog(visible = showPathInfoDialog, onClose = { showPathInfoDialog = false }, path = recording.first.path)

    DeleteDialog(visible = showDeleteDialog, onClose = { showDeleteDialog = false }) {
        showDeleteDialog = false
        navController.popBackStack()
        playbackClientControls.deleteRecording(recording.first)
    }


}

@Composable
fun TimestampInfoAlert(
    visible: Boolean,
    timestamp: TimeStamp,
    onClose: () -> Unit,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    if (visible)
        AlertDialog(
            onDismissRequest = onClose,
            title = { Text(timestamp.name) },
            icon = {Icon(Icons.Outlined.Place, null)},
            text = {
                Text(timestamp.description.ifBlank { "This timestamp has no description." })
            },
            confirmButton = {
                Button(onClick = onPlay) {
                    Text("Play")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = onDelete) {
                        Text("Delete")
                    }
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = onEdit) {
                        Text("Edit")
                    }
                }
            }
        )

}

@Composable
fun PathInfoDialog(visible: Boolean, onClose: () -> Unit, path: String) {
    if (visible)
        AlertDialog(
            onDismissRequest = onClose,
            icon = {Icon(Icons.Rounded.FindInPage, null)},
            title = { Text("Where is my recording?") },
            text = {
                Column {
                    Text("You can find your recording at the file path below by using a file explorer app on your phone or by plugging your phone into a computer.")
                    Spacer(Modifier.height(12.dp))
                    Text(path, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(onClick = onClose) {
                    Text("Okay")
                }
            }
        )
}

@Composable
fun DeleteDialog(visible: Boolean, onClose: () -> Unit, onConfirm: () -> Unit) {
    if (visible)
        AlertDialog(
            onDismissRequest = onClose,
            icon = {Icon(Icons.Rounded.DeleteForever, null)},
            title = { Text("Delete Recording?") },
            text = {
                Column {
                    Text("Deleting a recording is permanent and cannot be undone. This will also delete all timestamps associated with the recording, but not any clips.")
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