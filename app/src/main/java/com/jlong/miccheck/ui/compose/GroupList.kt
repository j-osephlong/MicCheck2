package com.jlong.miccheck.ui.compose

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    recorderClientControls: RecorderClientControls
) {
    val lazyColumnState = rememberLazyListState()

    val chipColors = ChipColors.outlinedChipColors().copy(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        outline = Color.Transparent
    )

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
                        text = { Text("Groups", style = MaterialTheme.typography.labelLarge) },
                        leadingIcon = Icons.Rounded.LibraryMusic,
                        showLeadingIcon = true,
                        colors = chipColors,
                        enabled = viewModel.showingGroupsList
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

        items(viewModel.groups.filter { it.uuid != starredGroupUUID }) {
            Column (Modifier.padding(12.dp, 0.dp)) {
                GroupCard(group = it) {navController.navigate(Destination.Group.createRoute(it.uuid))}
                Spacer(Modifier.height(12.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(WindowInsets.navigationBars
                .asPaddingValues()
                .calculateBottomPadding()))
        }

    }
}
