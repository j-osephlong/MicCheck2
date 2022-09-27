package com.jlong.miccheck.ui.compose

import android.app.Activity
import android.net.Uri
import android.util.Log
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.rounded.AddLink
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.navigation.animation.composable
import com.jlong.miccheck.MicCheckViewModel
import com.jlong.miccheck.PlaybackClientControls
import com.jlong.miccheck.RecorderClientControls
import com.jlong.miccheck.billing.Billing
import com.jlong.miccheck.billing.PRO_SKU
import kotlinx.coroutines.GlobalScope

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.addGetProScreenGraph(
    navController: NavController,
    viewModel: MicCheckViewModel
) {
    Log.i("Navigation", "Nav component list composed.")

    composable(
        Destination.GetPro.route,
    ) {
        GetProScreen(
            navController,
            viewModel
        )
    }
}

@Composable
fun GetProScreen (navController: NavController, viewModel: MicCheckViewModel) {
    val context = LocalContext.current
    val billingInstance = Billing.getInstance(
        (context as Activity).application, GlobalScope, arrayOf(PRO_SKU), {}
    )
    val proPriceFlow = billingInstance.getSkuPrice(PRO_SKU).collectAsState(initial = "")

    val features = listOf(
        Triple(
            "Group Timestamp View",
            "Timestamps are a first class feature in MicCheck, and they only get better with Pro. Group Timestamps View gives you a dynamic view of all the timestamps found in the recordings of your groups.",
            Icons.Outlined.LocationOn
        ),
        Triple(
            "Export as Video",
            "Ever want to share a track to another app, but the app doesn't support sharing audio? MicCheck now supports exporting your files as a video, with your choice of visuals.\n\nThis can be accessed from the recording share menu.",
            Icons.Rounded.Movie
        ),
        Triple(
            "Bitrate Recording Controls",
            "For the more advanced users, you can now manually control the bitrate used when making a recording.\n\nThis can be accessed from the triple dot menu anywhere in the app.",
            Icons.Rounded.Build
        ),
        Triple(
            "Attachments",
            "Attach files or websites to a recording in order to keep track of corresponding sheet music, album art, or lecture notes.",
            Icons.Rounded.AddLink
        )
    )

    Surface (
        Modifier.fillMaxSize()
            ) {
        Column (
            Modifier
                .fillMaxSize()
                .padding(24.dp, 0.dp)
                .verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(24.dp))
            Text(
                "micCheck",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Get Pro",
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "micCheck is built and maintained by a solo developer/student, and contains zero ads. If you'd like to, you can help support the developer with micCheck Pro, a single purchase which gives you a few added perks.",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(18.dp))
            Column (Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,){
                if (!viewModel.isPro) {
                    Button(onClick = {
                        billingInstance.launchBillingFlow(
                            context as Activity, PRO_SKU
                        )
                        navController.navigateUp()
                    }) {
                        Text("Get Pro for ${proPriceFlow.value}")
                    }
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(onClick = { navController.navigateUp() }) {
                        Text("No Thanks")
                    }
                } else {
                    Button(onClick = { /*TODO*/ }) {
                        Text("Thanks you for getting Pro!")
                    }
                }
            }
            Spacer(Modifier.height(6.dp))

            features.forEach {
                Spacer(Modifier.height(12.dp))
                WhatsNewCard(title = it.first, description = it.second, icon = it.third)
            }

            Spacer(Modifier.height(24.dp + WindowInsets.navigationBars
                .asPaddingValues()
                .calculateBottomPadding()))
        }
    }
}