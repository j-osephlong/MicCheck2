package com.jlong.miccheck

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LatePermissionsDialog(
    visible: Boolean,
    onClose: () -> Unit,
    onOpenPermissions: () -> Unit
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
            confirmButton = { Button(onClick = onOpenPermissions) { Text("Open Settings") } },
            dismissButton = {
                TextButton(onClick = onClose) {
                    Text("Close")
                }
            },
            title = { Text("We need some permissions for that...") },
            icon = { Icon(Icons.Rounded.VerifiedUser, null) },
            text = {
                Text("micCheck doesn't have the permissions needed from you in order to work normally. Tap the open settings button below to fix this.")
            }
        )
}