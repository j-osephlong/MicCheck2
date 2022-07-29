package com.jlong.miccheck.ui.compose

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ExportDialog(
    visible: Boolean,
    onClose: () -> Unit,
    onShare: () -> Unit,
    onExportAsVideo: () -> Unit
) {
    if (visible)
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier =
                Modifier
                    .padding(48.dp)
                    .widthIn(280.dp, 560.dp)
                    .heightIn(max = 560.dp),
            onDismissRequest = { onClose() },
            confirmButton = { Button(onClick = { onShare() }) { Text("Share") } },
            dismissButton = {
                FilledTonalButton(onClick = { onExportAsVideo() }) {
                    Text("Export as Video")
                }
            },
            title = { Text("Export Recording") },
            icon = { Icon(Icons.Rounded.FileDownload, null) },
            text = {
                Text("With the Pro Version of MicCheck, you can export recordings as videos for apps without audio support.")
            }
        )
}