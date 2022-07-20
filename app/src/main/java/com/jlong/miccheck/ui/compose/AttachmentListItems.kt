package com.jlong.miccheck.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.tooling.preview.Preview
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
                    attachment.name.substringBefore("."),
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
fun AddAttachmentButton(onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
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
                    "Add an Attachment",
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

@Composable
fun AttachementDialog(visible: Boolean, attachment: Attachment?, onClose: () -> Unit, onEdit: () -> Unit, onOpen: () -> Unit) {
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