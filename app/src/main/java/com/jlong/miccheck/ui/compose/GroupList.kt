package com.jlong.miccheck.ui.compose

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jlong.miccheck.MicCheckViewModel
import com.jlong.miccheck.RecorderClientControls
import com.jlong.miccheck.RecordingState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupList(
    navController: NavController,
    viewModel: MicCheckViewModel,
    recorderClientControls: RecorderClientControls,
    pickImage: ((Uri) -> Unit, () -> Unit) -> Unit
) {
    val lazyColumnState = rememberLazyListState()

    val chipColors = ChipColors.outlinedChipColors().copy(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        outline = Color.Transparent
    )

    var showNewGroupDialog by remember { mutableStateOf(false)}

    BackHandler(true) {
        viewModel.showingGroupsList = false
    }

    LazyColumn (
        Modifier
            .fillMaxSize(),
        state = lazyColumnState
    ) {
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp, 0.dp)) {
                Spacer(Modifier.height(12.dp))
                Row {
                    OutlinedChip(
                        text = { Text("Recordings", style = MaterialTheme.typography.labelLarge) },
                        leadingIcon = Icons.Rounded.ArrowBack,
                        showLeadingIcon = true,
                        colors = chipColors,
                        enabled = true
                    ) {
                        viewModel.showingGroupsList = !viewModel.showingGroupsList
                    }
                }
            }
        }

        stickyHeader {
            AnimatedVisibility(
                visible = (viewModel.recordingState != RecordingState.WAITING &&
                        viewModel.recordingState != RecordingState.STOPPED) || viewModel.currentRecordingUri != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    RecordingControlsCard(lazyColumnState, viewModel, recorderClientControls)
                }
            }
        }

        item {
            Spacer(Modifier.height(12.dp))
        }

        item {
            viewModel.groups.find { it.uuid == starredGroupUUID }?.let {
                Column (Modifier.padding(12.dp, 0.dp)) {
                    GroupCard(group = it) {navController.navigate(Destination.Group.createRoute(it.uuid))}
                    Spacer(Modifier.height(12.dp))
                }
            }
        }

        if (viewModel.groups.size > 1)
            items(viewModel.groups.filter { it.uuid != starredGroupUUID }) {
                Column (Modifier.padding(12.dp, 0.dp)) {
                    GroupCard(group = it) {navController.navigate(Destination.Group.createRoute(it.uuid))}
                    Spacer(Modifier.height(12.dp))
                }
            }
        else
            item {
                NoGroupsTip { showNewGroupDialog = true }
            }

        item {
            Spacer(modifier = Modifier.height(WindowInsets.navigationBars
                .asPaddingValues()
                .calculateBottomPadding()))
        }

    }

    NewGroupDialog(
        visible = showNewGroupDialog,
        onClose = { showNewGroupDialog = false },
        pickImage = pickImage
    ) { title, imageUri ->
        viewModel.createGroup(
            title,
            imageUri
        )
        showNewGroupDialog = false
    }
}

@Composable
fun NoGroupsTip (
    onClickNewGroup: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(12.dp, 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Rounded.Inventory2,
            null,
            modifier = Modifier.size(52.dp)
        )
        Spacer(Modifier.height(18.dp))
        Text(
            "Groups live here,",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Create some groups to keep your recordings organized.",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))
        Button(onClick = onClickNewGroup) {
            Text("Create a Group")
        }
    }

}