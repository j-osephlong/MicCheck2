package com.jlong.miccheck.ui.compose

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.rounded.Create
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.jlong.miccheck.RecordingGroup
import com.jlong.miccheck.ui.theme.surfaceColorAtElevation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DialogGroupCard(title: String, image: Uri?, onOpenPicker: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(key1 = image) {
        if (image == null)
            return@LaunchedEffect
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder
                .createSource(context.contentResolver, image)
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    bitmap = ImageDecoder.decodeBitmap(source)
                }
            }
        } else {
            bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, image)
        }
    }

    Surface(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceColorAtElevation(18.dp)) {
        Column {
            if (bitmap != null)
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .height(112.dp)
                        .clickable { onOpenPicker() },
                    contentScale = ContentScale.Crop
                )
            else
                Surface(
                    Modifier
                        .fillMaxWidth()
                        .height(112.dp)
                        .clickable { onOpenPicker() },
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(12.dp),
                ) {
                    Row (Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Outlined.AddAPhoto, null, tint = MaterialTheme.colorScheme.tertiary)
                    }
                }
            Text(title.ifBlank { "Group Name" }, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(12.dp))
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NewGroupDialog(
    visible: Boolean,
    onClose: () -> Unit,
    pickImage: ((Uri)->Unit, ()->Unit ) -> Unit,
    onConfirm: (String, Uri?) -> Unit
) {
    val (nameText, setNameText) = remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    if (visible)
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier =
            Modifier
                .padding(48.dp)
                .widthIn(280.dp, 560.dp)
                .heightIn(max = 560.dp),
            onDismissRequest = onClose,
            confirmButton = {
                Button(
                    onClick = {
                        onConfirm(nameText, imageUri)
                        setNameText("")
                        imageUri = null
                    },
                    enabled = nameText.isNotBlank()
                ) {
                    Text("Done")
                }
            },
            dismissButton = { TextButton(onClick = onClose) { Text("Discard") } },
            title = { Text("New Group") },
            icon = { Icon(Icons.Rounded.LibraryMusic, null) },
            text = {
                Column (
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    DialogGroupCard(title = nameText, image = imageUri) { pickImage({imageUri = it}, {}) }
                    Spacer(Modifier.height(12.dp))
                    TextField(
                        value = nameText,
                        onValueChange = setNameText,
                        placeholder = { Text("Group Name") },
                        shape = RoundedCornerShape(28.dp),
                        colors = TextFieldDefaults.textFieldColors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        maxLines = 1
                    )
                }
            }
        )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniGroupCard (group: RecordingGroup, selected: Boolean, onClick: () -> Unit) {
    val color by animateColorAsState(
        if (selected)
            MaterialTheme.colorScheme.surfaceColorAtElevation(48.dp)
        else
            MaterialTheme.colorScheme.surfaceColorAtElevation(12.dp)
    )

    Surface (
        shape = RoundedCornerShape(18.dp),
        color = color,
        onClick = onClick
    ) {
        Row (verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp).fillMaxWidth()) {
            if (group.imgUri != null){
                AsyncImage(
                    model = Uri.parse(group.imgUri),
                    contentDescription = null,
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.width(18.dp))
            }
            Text(group.name, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun AddToExistingGroupDialog (
    visible: Boolean,
    groups: List<RecordingGroup>,
    onClose: () -> Unit,
    onCreateNew: () -> Unit,
    onConfirm: (RecordingGroup) -> Unit,
) {
    var selectedGroup by remember { mutableStateOf<RecordingGroup?>(null) }

    if (visible)
        AlertDialog(
            onDismissRequest = onClose,
            confirmButton = {
                Button(
                    onClick = {
                        selectedGroup?.also {
                            onConfirm(it)
                            selectedGroup = null
                        }
                    },
                    enabled = selectedGroup != null
                ) {
                    Text("Done")
                }
            },
            dismissButton = { TextButton(onClick = { onClose(); selectedGroup = null }) { Text("Discard") } },
            title = { Text("Choose Group") },
            icon = { Icon(Icons.Rounded.LibraryMusic, null) },
            text = {
                Column (
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onCreateNew() },
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Row (
                            Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ){
                            Icon(Icons.Rounded.Create, null)
                            Spacer(Modifier.width(12.dp))
                            Text("Create New Group", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    groups.forEach {
                        MiniGroupCard(group = it, it == selectedGroup) {
                            selectedGroup = if (selectedGroup == it)
                                null
                            else
                                it
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        )
}
