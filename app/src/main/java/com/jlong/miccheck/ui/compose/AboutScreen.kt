package com.jlong.miccheck.ui.compose

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.navigation.animation.composable
import com.jlong.miccheck.BuildConfig
import com.jlong.miccheck.MicCheckViewModel
import com.jlong.miccheck.ui.theme.surfaceColorAtElevation

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.addAboutScreenGraph(
    navController: NavController,
    viewModel: MicCheckViewModel
) {
    Log.i("Navigation", "About screen composed.")

    composable(
        Destination.About.route,
    ) {
        AboutScreen(viewModel = viewModel, navController = navController)

    }
}

@Composable
fun AboutScreen (
    viewModel: MicCheckViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val openPrivacyPolicy: () -> Unit = {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.termsfeed.com/live/fdb75c3d-8b5d-4a38-a28e-ddd1b95cd1d8")))
    }

    var showComingSoonDialog by remember { mutableStateOf(false)}

    Surface(Modifier.fillMaxSize()) {
        Column (Modifier.fillMaxWidth()) {
            Spacer(Modifier.height(12.dp))
            Surface(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp, 0.dp), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)) {
                Column (
                    Modifier
                        .fillMaxWidth()
                        .padding(18.dp)) {
                    Text("micCheck", style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(Modifier.height(4.dp))
                    Text("Developed by Joseph Long for fun.", style = MaterialTheme.typography.labelLarge.copy(fontStyle = FontStyle.Italic))
                    Spacer(Modifier.height(18.dp))
                    Text("Version ${BuildConfig.VERSION_NAME} - ${if (viewModel.isPro) "Pro Edition" else "Free Edition"}", style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.height(12.dp))
            Surface(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp, 0.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Row (
                    Modifier
                        .fillMaxWidth()
                        .padding(18.dp, 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Check out what's new",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    IconButton(onClick = { navController.navigate(Destination.WhatsNew.route) }) {
                        Icon(Icons.Rounded.ArrowForward, null)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Surface(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp, 0.dp)
                    .clickable { navController.navigate(Destination.GetPro.route) },
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Row (
                    Modifier
                        .fillMaxWidth()
                        .padding(18.dp, 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Learn about the Pro version",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    IconButton(onClick = { navController.navigate(Destination.GetPro.route) }) {
                        Icon(Icons.Rounded.ArrowForward, null)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Surface(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp, 0.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Column (
                    Modifier
                        .fillMaxWidth()
                        .padding(18.dp, 18.dp)){
                    Row(
                        Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Statistics",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                        Icon(Icons.Rounded.Info, null, modifier = Modifier.padding(end = 12.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("App Launches - ${viewModel.stats.appLaunches}")
                    Text("Recordings Recorded - ${viewModel.recordings.size}")
                    Text("Groups Created - ${viewModel.groups.size}")
                }
            }
            Spacer(Modifier.height(12.dp))
            Surface(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp, 0.dp)
                    .clickable { viewModel.clearFirstLaunch() },
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Row (
                    Modifier
                        .fillMaxWidth()
                        .padding(18.dp, 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Reset out of box experience",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    IconButton(onClick = viewModel::clearFirstLaunch) {
                        Icon(Icons.Rounded.ArrowForward, null)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Surface(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp, 0.dp)
                    .clickable { openPrivacyPolicy() },
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Row (
                    Modifier
                        .fillMaxWidth()
                        .padding(18.dp, 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Privacy Policy",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    IconButton(onClick = openPrivacyPolicy) {
                        Icon(Icons.Rounded.Policy, null)
                    }
                }
            }
        }
    }

    ComingSoonDialog(visible = showComingSoonDialog) {
        showComingSoonDialog = false
    }
}