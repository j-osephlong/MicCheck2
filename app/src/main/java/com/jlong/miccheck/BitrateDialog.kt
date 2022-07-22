package com.jlong.miccheck

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.text.isDigitsOnly

@Composable
fun BitrateDialog (visible: Boolean, currentBitrate: Int, onClose: () -> Unit, onConfirm: (Int) -> Unit) {
    val (bitrateText, setBitrateText) = remember { mutableStateOf(currentBitrate.toString()) }
    var isError by remember { mutableStateOf(false) }

    if (visible)
        AlertDialog(
            onDismissRequest = onClose,
            title = { Text("Set Bitrate") },
            text = {
                TextField(
                    value = bitrateText,
                    onValueChange = { setBitrateText(it); isError = false },
                    placeholder = { Text("Bitrate") },
                    label = if (isError) {{ Text("Must only contain digits") }} else null,
                    isError = isError,
                    shape = RoundedCornerShape(28.dp),
                    colors = TextFieldDefaults.textFieldColors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    maxLines = 1,
                    trailingIcon = {
                        IconButton(onClick = { setBitrateText("") }) {
                            Icon(Icons.Rounded.Close, null)
                        }
                    }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (bitrateText.isDigitsOnly())
                        onConfirm(bitrateText.toInt())
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