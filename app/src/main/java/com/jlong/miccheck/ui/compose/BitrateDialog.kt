package com.jlong.miccheck.ui.compose

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.text.isDigitsOnly

@Composable
fun BitrateDialog(
    visible: Boolean,
    isPro: Boolean,
    currentBitrate: Int,
    onClose: () -> Unit,
    onReset: () -> Unit,
    onOpenGetPro: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val (bitrateText, setBitrateText) = remember { mutableStateOf(currentBitrate.toString()) }
    var isError by remember { mutableStateOf(false) }

    if (visible)
        AlertDialog(
            onDismissRequest = onClose,
            title = { Text("Set Bitrate") },
            text = {
                if (!isPro) {
                    Text("This is a micCheck Pro feature.")
                } else
                    TextField(
                        value = bitrateText,
                        onValueChange = { setBitrateText(it); isError = false },
                        placeholder = { Text("Bitrate") },
                        label = if (isError) {{ Text("Must be a positive integer") }} else null,
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
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        enabled = isPro
                    )
            },
            confirmButton = {
                if (isPro)
                    Button(onClick = {
                        if (bitrateText.isDigitsOnly() && bitrateText.toInt() > 0)
                            onConfirm(bitrateText.toInt())
                        else
                            isError = true

                    }, enabled = isPro) {
                        Text("Set")
                    }
                else
                    Button(onClick = { onOpenGetPro() }) {
                        Text("Get Pro")
                    }
            },
            dismissButton = {
                if (isPro) {
                    FilledTonalButton(onClick = onReset, enabled = isPro) {
                        Text("Reset")
                    }
                }
                TextButton(onClick = onClose) {
                    Text("Cancel")
                }
            }
        )
}