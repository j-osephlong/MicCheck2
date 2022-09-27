package com.jlong.miccheck

import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog

@Composable
fun DebugExportAsVideoPhotoCommand (visible: Boolean, onClose: () -> Unit, onConfirm: (String) -> Unit, viewModel: MicCheckViewModel) {
    assert(viewModel.inDebugMode || !visible)
    val (p1, setP1) = remember { mutableStateOf("-r 1 -loop 1 -y -i") }
    val (p2, setP2) = remember { mutableStateOf("-i") }
    val (p3, setP3) = remember { mutableStateOf("-r 1 -shortest") }
    val (p4, setP4) = remember { mutableStateOf("") }

    //"-r 1 -loop 1 -y -i \"${visuals.path}\" -i \"${recording.path}\" -r 1 -shortest \"$tempPath\""
    if (visible)
        Dialog(onDismissRequest = onClose) {
            Column (
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextField(value = p1, onValueChange = setP1)
                Text("\\\"\${visuals.path}\\\"")
                TextField(value = p2, onValueChange = setP2)
                Text("\\\"\${recording.path}\\\"")
                TextField(value = p3, onValueChange = setP3)
                Text("\\\"\$tempPath\\\"")
                TextField(value = p4, onValueChange = setP4)

                Button(onClick = {
                    onConfirm(
                        "$p1 __VPATH__ $p2 __RPATH__ $p3 __TPATH__ $p4"
                    )
                }) {
                    Text("go mode")
                }
            }
        }
}
