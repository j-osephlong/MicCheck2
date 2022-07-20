package com.jlong.miccheck.ui.compose

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.navigation.animation.composable
import com.jlong.miccheck.MicCheckViewModel
import com.jlong.miccheck.ui.theme.surfaceColorAtElevation

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.addWhatsNewScreenGraph(
    navController: NavController,
    viewModel: MicCheckViewModel
) {
    Log.i("Navigation", "What's New screen composed.")

    composable(
        Destination.WhatsNew.route,
    ) {
        WhatsNewScreen(viewModel = viewModel, navController = navController)
    }
}

@Composable
fun WhatsNewScreen (viewModel: MicCheckViewModel, navController: NavController) {
    val list = listOf(
        Triple(
            "Material You Redesign",
            "The update features a whole new look and feel, featuring a revamped user interface, material you design, and dynamic color support.",
            Icons.Rounded.FormatPaint
        ),
        Triple(
            "Various Improvements",
            " • Sorting options (length, oldest, size...)" +
                    "\n • Group Reordering" +
                    "\n • Ability to favorite recordings" +
                    "\n • Better searching" +
                    "\n • Better timestamps" +
                    "\n • More...",
            Icons.Rounded.Construction
        ),
        Triple(
            "Smart Playback Card",
            "When playing back a recording, the revamped playback screen will automatically display the description and nearby timestamps.",
            Icons.Rounded.PinDrop
        ),
        Triple(
            "Advanced Looping Controls",
            "Tap on the loop button when listening to a recording to reveal new controls for looping, where you can loop as big or small of a section as you would like.\nHere you can also speed up or slow down the recording, as well as save your selection as a new recording.",
            Icons.Rounded.AllInclusive
        ),
        Triple(
            "Export as Video",
            "Ever want to share a track to another app, but the app doesn't support sharing audio? MicCheck now supports exporting your files as a video, with your choice of visuals.",
            Icons.Rounded.Movie
        ),
    )

    Surface(Modifier.fillMaxSize()) {

        Column (
            Modifier
                .fillMaxWidth()
                .padding(12.dp, 0.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
                modifier = Modifier.fillMaxWidth()
            ){
                Column (Modifier.padding(18.dp)){
                    Text(
                        "What's New",
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Version 2.0", style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            list.forEach {
                WhatsNewCard(title = it.first, description = it.second, icon = it.third)
                Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }

    }
}

@Composable
fun WhatsNewCard (title: String, description: String, icon: ImageVector) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {

            Row(Modifier.fillMaxWidth()) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
                Icon(icon, null, modifier = Modifier.padding(end = 12.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(description)
        }
    }
}

@Composable
fun DismissableExtra (
    visible: Boolean,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    text: (@Composable () -> Unit)? = null,
    leadingIcon: ImageVector? = null,
    dismissIcon: ImageVector = Icons.Rounded.Close,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    titleColor: Color? = null,
    onClick: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(visible = visible, exit = shrinkVertically() + fadeOut(), enter = expandVertically() + fadeIn()) {
        Surface(
            modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null)
                        Modifier.clickable { onClick() }
                    else
                        Modifier
                ),
            shape = RoundedCornerShape(18.dp),
            color = containerColor,
            contentColor = contentColor
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = if (text!=null) Alignment.Top else Alignment.CenterVertically,
            ) {
                leadingIcon?.let {
                    IconButton(
                        {},
                        modifier = Modifier.padding(start = 10.dp, top = 6.dp)
                    ) {
                        Icon(it, null, tint = titleColor?:contentColor)
                    }
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(if (leadingIcon != null) 10.dp else 18.dp, 18.dp)
                ) {
                    CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.titleMedium.copy(color = titleColor?:contentColor)) {
                        title()
                    }
                    if (text != null) {
                        Spacer(Modifier.height(4.dp))
                        CompositionLocalProvider(
                            LocalTextStyle provides MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Normal
                            )
                        ) {
                            text()
                        }
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.padding(end = 10.dp, top = if (text != null) 8.dp else 0.dp)) {
                    Icon(dismissIcon, null)
                }
            }
        }
    }
}