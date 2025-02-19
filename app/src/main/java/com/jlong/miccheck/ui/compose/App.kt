package com.jlong.miccheck.ui.compose

import android.app.Activity
import android.app.Application
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BackdropScaffoldState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.TopAppBar
import androidx.compose.material.BackdropScaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.BackdropValue
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Recommend
import androidx.compose.material.icons.rounded.TheaterComedy
import androidx.compose.material.rememberBackdropScaffoldState
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.android.play.core.review.ReviewManagerFactory
import com.jlong.miccheck.*
import com.jlong.miccheck.billing.Billing
import com.jlong.miccheck.billing.PRO_SKU
import com.jlong.miccheck.ui.theme.surfaceColorAtElevation
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

val backdropContainerColor = @Composable { if (false) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp) }
val backdropContentColor = @Composable { if (false) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface }

@OptIn(ExperimentalMaterialApi::class, DelicateCoroutinesApi::class)
@Composable
fun MicTopBar (navHost: NavHostController, backdropScaffoldState: BackdropScaffoldState, viewModel: MicCheckViewModel, exportData: () -> Unit, importData: () -> Unit) {
    val navBackStackEntry by navHost.currentBackStackEntryAsState()
    var dropDownMenuExpanded by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showComingSoonDialog by remember { mutableStateOf(false) }
    var showImportExportDialog by remember { mutableStateOf(viewModel.stats.appLaunches == 5) }
    var showBitrateDialog by remember { mutableStateOf(viewModel.stats.appLaunches == 5) }

    val context = LocalContext.current

    Crossfade(targetState = navBackStackEntry?.destination?.route == Destination.Search.route) {
        if (it)
            SearchBar(
                viewModel.currentSearchString,
                { viewModel.currentSearchString = it },
                {
                    viewModel.clearSelectedRecordings()
                    navHost.navigateUp()
                }
            )
        else
            TopAppBar(
                title = {
                    Text(
                        "micCheck",
                        color = backdropContentColor(),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    AnimatedVisibility(visible = navBackStackEntry?.destination?.route != Destination.WhatsNew.route){
                        Row {
                            IconButton({
                                navHost.navigate(Destination.Search.createRoute())
                            }) {
                                Icon(Icons.Outlined.Search, "Search", tint = backdropContentColor())
                            }
                            Box(Modifier.wrapContentSize(Alignment.BottomCenter)) {
                                IconButton(onClick = { dropDownMenuExpanded = true }) {
                                    Icon(
                                        Icons.Outlined.MoreVert,
                                        "More",
                                        tint = backdropContentColor()
                                    )
                                }
                                DropdownMenu(
                                    expanded = dropDownMenuExpanded,
                                    onDismissRequest = { dropDownMenuExpanded = false },
                                    offset = DpOffset((8).dp, -(8).dp)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Theme") },
                                        onClick = {
                                            showThemeDialog = true; dropDownMenuExpanded = false
                                        })
                                    DropdownMenuItem(
                                        text = { Text("Bitrate Options") },
                                        onClick = {
                                            showBitrateDialog = true; dropDownMenuExpanded =
                                            false
                                        })
                                    DropdownMenuItem(
                                        text = { Text("Import and Export") },
                                        onClick = {
                                            showImportExportDialog = true; dropDownMenuExpanded =
                                            false
                                        })
                                    MenuDefaults.Divider()
                                    DropdownMenuItem(
                                        text = { Text("Support the Developer") },
                                        onClick = {
                                            navHost.navigate(Destination.GetPro.route)
                                            dropDownMenuExpanded = false
                                        })
                                    DropdownMenuItem(
                                        text = { Text("About") },
                                        onClick = {
                                            navHost.navigate(Destination.About.route); dropDownMenuExpanded =
                                            false
                                        })
                                }
                            }
                        }
                    }
                },
                backgroundColor = backdropContainerColor(),
                contentColor = backdropContentColor(),
                elevation = 0.dp,
                modifier = Modifier.height(64.dp)
            )
    }

    BitrateDialog(
        visible = showBitrateDialog,
        isPro = viewModel.isPro,
        currentBitrate = viewModel.settings.encodingBitRate,
        onReset = {
            viewModel.setBitrate(UserAndSettings().encodingBitRate)
            showBitrateDialog = false
        },
        onClose = { showBitrateDialog = false },
        onOpenGetPro = {
            showBitrateDialog = false
            navHost.navigate(Destination.GetPro.route)
        }
    ) {
        viewModel.setBitrate(it)
        showBitrateDialog = false
    }

    SetThemeDialog(
        visible = showThemeDialog,
        currentSelection = viewModel.settings.theme,
        onClose = { showThemeDialog = false },
        onSelect = {
            if (it != viewModel.settings.theme)
                viewModel.setTheme(it)
        }
    )

    ComingSoonDialog(visible = showComingSoonDialog) {
        showComingSoonDialog = false
    }

    ImportExportDialog(visible = showImportExportDialog, onClose = { showImportExportDialog = false }, onExport = { exportData(); showImportExportDialog = false }) {
        importData()
        showImportExportDialog = false
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalAnimationApi::class)
@Composable
fun App(
    viewModel: MicCheckViewModel,
    recordingClientControls: RecorderClientControls,
    playbackClientControls: PlaybackClientControls,
    pickImage: ((Uri) -> Unit, () -> Unit) -> Unit,
    pickFile: ((Uri) -> Unit) -> Unit,
    launchUri: (String, Uri) -> Unit,
    exportData: () -> Unit,
    importData: () -> Unit
) {
    Log.i("Composition", "Root composable composed.")
    val backdropScaffoldState = rememberBackdropScaffoldState(initialValue = BackdropValue.Concealed)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var showReviewDialog by remember { mutableStateOf(viewModel.stats.appLaunches == 5) }
    var showProDialog by remember { mutableStateOf(!viewModel.settings.dismissedExtras.contains(DismissableExtraId.ProV2_2) && !viewModel.isPro) }
    val navController = rememberAnimatedNavController()

    BackHandler(backdropScaffoldState.isRevealed) {
        coroutineScope.launch {
            backdropScaffoldState.conceal()
        }
    }
    val setBackdrop: (Boolean) -> Unit = {
        coroutineScope.launch {
            if (it)
                backdropScaffoldState.reveal()
            else
                backdropScaffoldState.conceal()
        }
    }

    Column (Modifier.fillMaxSize()) {
        Surface (color = backdropContainerColor()){
            Spacer(
                modifier = Modifier
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .fillMaxWidth()
            )
        }
        Box {
            BackdropScaffold(
                appBar = { MicTopBar(navController, backdropScaffoldState, viewModel, exportData, importData) },
                backLayerContent =
                    micBackLayer(
                        viewModel,
                        navController,
                        recordingClientControls,
                        playbackClientControls,
                        setBackdrop,
                        pickImage
                    ),
                frontLayerContent =
                    micFrontLayer(
                        viewModel,
                        navController,
                        recordingClientControls,
                        playbackClientControls,
                        setBackdrop,
                        pickImage,
                        pickFile,
                        launchUri
                    ),
                frontLayerBackgroundColor = MaterialTheme.colorScheme.background,
                frontLayerContentColor = MaterialTheme.colorScheme.onBackground,
                backLayerBackgroundColor = backdropContainerColor(),
                backLayerContentColor = backdropContentColor(),
                scaffoldState = backdropScaffoldState,
                peekHeight = 64.dp,
                frontLayerShape = RoundedCornerShape(18.dp, 18.dp, 0.dp, 0.dp),
                frontLayerScrimColor = MaterialTheme.colorScheme.surface.copy(alpha = .6f),
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.TopStart)
            )
        }
    }

    ReviewDialog(visible = showReviewDialog, onClose = { showReviewDialog = false }) {
        val manager = ReviewManagerFactory.create(context)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val flow = manager.launchReviewFlow(context as Activity, reviewInfo)
                flow.addOnCompleteListener {
                    Toast.makeText(context, "Thanks!", Toast.LENGTH_LONG).show()
                }
            }
        }
        showReviewDialog = false
    }

    GetProDialog(visible = showProDialog, onClose = { showProDialog = false; viewModel.dismissExtra(DismissableExtraId.ProV2_2) }) {
        showProDialog = false; viewModel.dismissExtra(DismissableExtraId.ProV2_2)
        navController.navigate(Destination.GetPro.route)
    }

    LatePermissionsDialog(visible = viewModel.showLatePermissionsDialog, onClose = { viewModel.showLatePermissionsDialog = false }) {
        playbackClientControls.openPermissions()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetThemeDialog(visible: Boolean, currentSelection: ThemeOptions, onClose: () -> Unit, onSelect: (ThemeOptions) -> Unit) {
    if (visible)
        AlertDialog(
            onDismissRequest = onClose,
            icon = {Icon(Icons.Rounded.NightsStay, null)},
            title = { Text("App Theme") },
            text = {
                Column {
                    Row (Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically){
                        RadioButton(selected = currentSelection == ThemeOptions.Light, onClick = { onSelect(ThemeOptions.Light) })
                        Spacer (Modifier.width(8.dp))
                        Text("Light", style = MaterialTheme.typography.labelLarge)
                    }
                    Row (Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically){
                        RadioButton(selected = currentSelection == ThemeOptions.Dark, onClick = { onSelect(ThemeOptions.Dark) })
                        Spacer (Modifier.width(8.dp))
                        Text("Dark", style = MaterialTheme.typography.labelLarge)
                    }
                    Row (Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically){
                        RadioButton(selected = currentSelection == ThemeOptions.System, onClick = { onSelect(ThemeOptions.System) })
                        Spacer (Modifier.width(8.dp))
                        Text("System Theme", style = MaterialTheme.typography.labelLarge)
                    }
                }
            },
            confirmButton = {
                Button(onClick = onClose) {
                    Text("Okay")
                }
            }
        )
}

@Composable
fun ComingSoonDialog (visible: Boolean, onClose: () -> Unit) {
    if (visible)
        AlertDialog(
            onDismissRequest = onClose,
            icon = {Icon(Icons.Rounded.TheaterComedy, null)},
            title = { Text("Coming Soon") },
            text = {
                Column {
                    Text("This feature is in progress, but it's not ready just yet.")
                }
            },
            confirmButton = {
                Button(onClick = onClose) {
                    Text("Okay")
                }
            }
        )
}

@Composable
fun ReviewDialog (visible: Boolean, onClose: () -> Unit, onConfirm: () -> Unit) {
    if (visible)
        AlertDialog(
            onDismissRequest = onClose,
            icon = {Icon(Icons.Rounded.Recommend, null)},
            title = { Text("Enjoying the app?") },
            text = {
                Column {
                    Text("This app was made by a solo app developer and student, if you enjoy the app please leave a rating! If not, let me know how I can do better.")
                }
            },
            confirmButton = {
                Button(onClick = onConfirm) {
                    Text("Sure")
                }
            },
            dismissButton = {
                TextButton(onClick = onClose) {
                    Text("No Thanks")
                }
            }
        )
}

@Composable
fun GetProDialog (visible: Boolean, onClose: () -> Unit, onConfirm: () -> Unit) {
    if (visible)
        AlertDialog(
            onDismissRequest = onClose,
            title = { Text("Just a moment of your time...") },
            text = {
                Column {
                    Text("This app was made by a solo app developer and student, and now you can help support the developer with micCheck Pro!")
                }
            },
            confirmButton = {
                Button(onClick = onConfirm) {
                    Text("Learn More")
                }
            },
            dismissButton = {
                TextButton(onClick = onClose) {
                    Text("No Thanks")
                }
            }
        )
}


@Composable
fun ImportExportDialog (visible: Boolean, onClose: () -> Unit, onExport: () -> Unit, onImport: () -> Unit) {
    if (visible)
        AlertDialog(
            onDismissRequest = onClose,
            icon = {Icon(Icons.Rounded.Description, null)},
            title = { Text("Import and Export your Data") },
            text = {
                Column {
                    Text("Backup or import your recordings data. This will create a file that contains recording descriptions, timestamps, groups, and just about everything else.")
                }
            },
            confirmButton = {
                Button(onClick = onImport) {
                    Text("Import")
                }
            },
            dismissButton = {
                Button(onClick = onExport) {
                    Text("Export")
                }
            }
        )
}