package com.jlong.miccheck.ui.compose

import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jlong.miccheck.Attachment
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileListItem(attachment: Attachment, onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row (
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
                ) {
            Icon(
                when (attachment.mimeType.substringBefore("/")) {
                    "image" -> Icons.Rounded.Image
                    "audio" -> Icons.Rounded.MusicNote
                    "video" -> Icons.Rounded.SmartDisplay
                    "text" -> Icons.Rounded.Description
                    "http" -> Icons.Rounded.Link
                    else -> when (attachment.mimeType.substringAfterLast("/")) {
                        "pdf" -> Icons.Rounded.PictureAsPdf
                        else -> Icons.Rounded.FilePresent
                    }
                },
                null
            )
            Spacer(modifier = Modifier.width(12.dp))
            Row (modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically){
                Text(
                    if (attachment.mimeType != "http")
                        attachment.name.substringBefore(".")
                    else
                        attachment.name.substringAfter("https://"),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
                Text(
                    attachment.mimeType.substringAfterLast("/").uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    modifier = Modifier.padding(12.dp, 0.dp)
                )
            }
            Icon(Icons.Rounded.Launch, null, Modifier.padding(end = 4.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAttachmentButtons(
    onClickAddLink: () -> Unit,
    onClickAddFile: () -> Unit
) {
    Row (Modifier.fillMaxWidth()){
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            onClick = onClickAddFile
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.DocumentScanner, null)
                Spacer(modifier = Modifier.width(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "File",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
                Icon(Icons.Rounded.Add, null, Modifier.padding(end = 4.dp))
            }
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            onClick = onClickAddLink
        ) {
            Row (
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Attachment, null)
                Spacer(modifier = Modifier.width(12.dp))
                Row (modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                    verticalAlignment = Alignment.CenterVertically){
                    Text(
                        "Link",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
                Icon(Icons.Rounded.Add, null, Modifier.padding(end = 4.dp))
            }
        }
    }
}

@Composable
fun AttachmentDialog(visible: Boolean, attachment: Attachment?, onClose: () -> Unit, onEdit: () -> Unit, onOpen: () -> Unit) {
    if (visible)
        AlertDialog(
            onDismissRequest = onClose,
            title = { Text("Attachment") },
            text = {
                attachment?.let {
                    FileListItem(attachment = it) {}
                }
            },
            confirmButton = {
                Button(onClick = onOpen) {
                    Text("Open")
                }
            },
            dismissButton = {
                TextButton(onClick = onEdit) {
                    Text("Edit")
                }
            }
        )
}
@Composable
fun NewLinkAttachmentDialog(visible: Boolean, onClose: () -> Unit, onConfirm: (String) -> Unit) {
    val (urlText, setUrlText) = remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    if (visible)
        AlertDialog(
            onDismissRequest = onClose,
            title = { Text("Add a Link") },
            text = {
                TextField(
                    value = urlText,
                    onValueChange = { setUrlText(it); isError = false },
                    placeholder = { Text("URL") },
                    label = if (isError) {{Text("This is an invalid URL")}} else null,
                    isError = isError,
                    shape = RoundedCornerShape(28.dp),
                    colors = TextFieldDefaults.textFieldColors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    maxLines = 1,
                    trailingIcon = {
                        IconButton(onClick = { setUrlText("") }) {
                            Icon(Icons.Rounded.Close, null)
                        }
                    }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (Patterns.WEB_URL.matcher(urlText).matches())
                        onConfirm(urlText)
                    else
                        isError = true

                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = onClose) {
                    Text("Cancel")
                }
            }
        )
}