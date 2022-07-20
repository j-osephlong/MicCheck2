package com.jlong.miccheck.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NewLabel
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.google.accompanist.flowlayout.FlowRow
import com.jlong.miccheck.Tag

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AddTagDialog(
    visible: Boolean,
    tags: List<Tag>,
    onClose: () -> Unit,
    onConfirm: (List<Tag>) -> Unit
) {
    val (nameText, setNameText) = remember { mutableStateOf("") }
    val selectedTags = remember { mutableStateListOf<Tag>() }

    val chipColors = ChipColors.outlinedChipColors().copy(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        outline = Color.Transparent
    )

    if (visible)
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier =
            Modifier
                .padding(48.dp)
                .widthIn(280.dp, 560.dp)
                .heightIn(max = 560.dp),
            onDismissRequest = { onClose(); setNameText(""); selectedTags.removeAll { true } },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirm(if (nameText.isNotBlank()) selectedTags + Tag(nameText) else selectedTags)
                        setNameText("")
                        selectedTags.removeAll { true }
                    },
                    enabled = nameText.isNotBlank() || selectedTags.isNotEmpty()
                ) {
                    Text("Done")
                }
            },
            dismissButton = { TextButton(onClick = { onClose(); setNameText(""); selectedTags.removeAll { true } }) { Text("Discard") } },
            title = { Text("Add Tag") },
            icon = { Icon(Icons.Rounded.NewLabel, null) },
            text = {
                Column (
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())) {
                    TextField(
                        value = nameText,
                        onValueChange = setNameText,
                        placeholder = { Text("Tag Name") },
                        shape = RoundedCornerShape(28.dp),
                        colors = TextFieldDefaults.textFieldColors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        maxLines = 1,
                        trailingIcon = {
                            IconButton(onClick = { setNameText(""); selectedTags.removeAll { true } }) {
                                Icon(Icons.Rounded.Close, null)
                            }
                        }
                    )
                    if (tags.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .15f))
                        Spacer(Modifier.height(12.dp))
                        FlowRow(
                            Modifier
                                .fillMaxWidth(),
                            mainAxisSpacing = 8.dp,
                            crossAxisSpacing = 8.dp
                        ) {
                            tags
                                .sortedByDescending { it.useCount }
                                .forEach {
                                OutlinedChip(
                                    text = { Text(it.name) },
                                    leadingIcon = Icons.Rounded.Check,
                                    showLeadingIcon = selectedTags.contains(it),
                                    colors = chipColors
                                ) {
                                    if (selectedTags.contains(it))
                                        selectedTags.remove(it)
                                    else
                                        selectedTags.add(it)
                                }
                            }
                        }
                    }
                }
            }
        )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SelectTagDialog(
    visible: Boolean,
    tags: List<Tag>,
    onClose: () -> Unit,
    onConfirm: (Tag) -> Unit
) {

    val chipColors = ChipColors.outlinedChipColors().copy(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        outline = Color.Transparent
    )

    if (visible)
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier =
            Modifier
                .padding(48.dp)
                .widthIn(280.dp, 560.dp)
                .heightIn(max = 560.dp),
            onDismissRequest = { onClose() },
            confirmButton = { TextButton(onClick = { onClose() }) { Text("Discard") } },
            title = { Text("Choose Tag") },
            icon = { Icon(Icons.Rounded.NewLabel, null) },
            text = {
                Column (
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())) {
                    if (tags.isNotEmpty()) {
                        FlowRow(
                            Modifier
                                .fillMaxWidth(),
                            mainAxisSpacing = 8.dp,
                            crossAxisSpacing = 8.dp
                        ) {
                            tags
                                .sortedByDescending { it.useCount }
                                .forEach {
                                    OutlinedChip(
                                        text = { Text(it.name) },
                                        colors = chipColors
                                    ) {
                                        onConfirm(it)
                                    }
                                }
                        }
                    } else {
                        Row (Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            Text("Add tags to recordings to find them here.")
                        }
                    }
                }
            }
        )
}
