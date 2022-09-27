package com.jlong.miccheck.ui.compose

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.jlong.miccheck.MicCheckViewModel
import com.jlong.miccheck.ThemeOptions
import com.jlong.miccheck.billing.Billing
import com.jlong.miccheck.billing.PRO_SKU
import com.jlong.miccheck.ui.theme.surfaceColorAtElevation
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalPagerApi::class)
@Composable
fun FirstLaunchScreen (
    viewModel: MicCheckViewModel,
    requestPermissions: () -> Unit,
    onComplete: () -> Unit,
    onOpenProScreen: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(0)

    Surface(
        Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Box(modifier = Modifier.fillMaxSize()){
            HorizontalPager(
                count = 6,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxSize(),
                userScrollEnabled = false,
                state = pagerState
            ) { page ->
                when (page) {
                    0 -> FirstLaunchScreenOne()
                    1 -> FirstLaunchScreenTwo()
                    2 -> FirstLaunchScreenThree(viewModel.settings.theme, viewModel::setTheme)
                    3 -> FirstLaunchScreenFour()
                    4 -> FirstLaunchScreenFive()
                    5 -> FirstLaunchScreenSix (isPro = viewModel.isPro) {
                        onOpenProScreen()
                    }
                    else -> {

                    }
                }
            }

            Row (
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        12.dp,
                        12.dp,
                        12.dp,
                        WindowInsets.navigationBars
                            .asPaddingValues()
                            .calculateBottomPadding() + 12.dp
                    )) {
                AnimatedVisibility(visible = pagerState.currentPage > 1 && pagerState.currentPage != 5){
                    OutlinedButton(
                        onClick = {
                            onComplete()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.background)
                    ) {
                        Text("Skip Introduction")
                    }
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    when (pagerState.currentPage) {
                        1 -> {
                            requestPermissions()
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage+1)
                            }
                        }
                        5 -> onComplete()
                        else -> coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage+1)
                        }
                    }
                }) {
                    Crossfade (pagerState.currentPage, modifier = Modifier.animateContentSize()) {
                        when (it) {
                            1 -> Text("Request Permissions")
                            2 -> Text("Save")
                            5 -> Text("Done")
                            else -> Text("Next")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FirstLaunchScreenOne () {
    val tagLine = buildAnnotatedString {
        withStyle(MaterialTheme.typography.headlineSmall.toSpanStyle()) {
            append("The modern audio recorder and gallery ")
        }
        withStyle(MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.tertiary
        ).toSpanStyle()) {
            append("For You")
        }
    }

    Column (
        Modifier
            .fillMaxSize()
            .padding(18.dp),
    verticalArrangement = Arrangement.Center) {
        Text("Welcome to", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold))
        Text(
            "micCheck",
            style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(12.dp))
        Text(tagLine)
        Spacer(Modifier.height(68.dp))
    }
}

@Composable
fun FirstLaunchScreenTwo() {
    Column (
        Modifier
            .fillMaxSize()
            .padding(18.dp),
        verticalArrangement = Arrangement.Center) {
        Text(
            "micCheck",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Let's get things set up",
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "micCheck needs a few permissions for things to run smoothly. Note that the app doesn't have access to the internet, so your data is safe here.",
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirstLaunchScreenThree(themeOption: ThemeOptions, setThemeOption: (ThemeOptions) -> Unit) {
    Column (
        Modifier
            .fillMaxSize()
            .padding(18.dp),
        verticalArrangement = Arrangement.Center) {
        Text(
            "micCheck",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Everyone has a preference",
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Choose a theme, or let micCheck match your phone automatically.",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = themeOption == ThemeOptions.System, onClick = { setThemeOption(ThemeOptions.System) })
            Spacer(Modifier.width(4.dp))
            Text("System Theme")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = themeOption == ThemeOptions.Light, onClick = { setThemeOption(ThemeOptions.Light) })
            Spacer(Modifier.width(4.dp))
            Text("Light Theme")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = themeOption == ThemeOptions.Dark, onClick = { setThemeOption(ThemeOptions.Dark) })
            Spacer(Modifier.width(4.dp))
            Text("Dark Theme")
        }
    }
}

@Composable
fun FirstLaunchScreenFour() {
    Column (
        Modifier
            .fillMaxSize()
            .padding(18.dp),
        verticalArrangement = Arrangement.Center) {
        Text(
            "micCheck",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Record, listen, simple.",
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Here you can record and playback audio with ease. Start recording by pressing the microphone button in the corner, and view what's playing by swiping down from anywhere in the app.",
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
fun FirstLaunchScreenFive() {
    Column (
        Modifier
            .fillMaxSize()
            .padding(18.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center) {
        Text(
            "micCheck",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "The photo gallery for audio.",
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "micCheck is packed with powerful features that help you organize and manage your recordings.",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(12.dp))

        Text(buildAnnotatedString {
            withStyle(MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold).toSpanStyle()) { append("1") }
            withStyle(MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold).toSpanStyle()) { append(" Tags")}
        })
        Text("Tag your recordings to make searching and staying on topic easier.")
        Spacer(Modifier.height(4.dp))
        Text(buildAnnotatedString {
            withStyle(MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold).toSpanStyle()) { append("2") }
            withStyle(MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold).toSpanStyle()) { append(" Timestamps")}
        })
        Text("Create timestamps to keep track of the contents of your longer recordings.")
        Spacer(Modifier.height(4.dp))
        Text(buildAnnotatedString {
            withStyle(MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold).toSpanStyle()) { append("3") }
            withStyle(MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold).toSpanStyle()) { append(" Groups")}
        })
        Text("Group your recordings in a way that looks and feels like an album.")
        Spacer(Modifier.height(4.dp))
        Text(buildAnnotatedString {
            withStyle(MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold).toSpanStyle()) { append("4") }
            withStyle(MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold).toSpanStyle()) { append(" Trimming")}
        })
        Text("Create new recordings from sections of existing tracks.")
        Spacer(Modifier.height(4.dp))
        Text(buildAnnotatedString {
            withStyle(MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold).toSpanStyle()) { append("5") }
            withStyle(MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold).toSpanStyle()) { append(" Search")}
        })
        Text("Search by tag, timestamp, group, date, and more.")
        Spacer(Modifier.height(4.dp))
    }
}

@Preview
@Composable
fun FirstLaunchScreenSix(isPro: Boolean, openProScreen: () -> Unit) {
    val context = LocalContext.current
    val billingInstance = Billing.getInstance(
        (context as Activity).application, GlobalScope, arrayOf(PRO_SKU), {}
    )
    val proPriceFlow = billingInstance.getSkuPrice(PRO_SKU).collectAsState(initial = "")

    Column (
        Modifier
            .fillMaxSize()
            .padding(18.dp),
        verticalArrangement = Arrangement.Center) {
        Text(
            "micCheck",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (!isPro) "Consider Pro" else "Thank you for supporting the app!",
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "micCheck is built and maintained by a solo developer/student, and contains zero ads." +
                    if(!isPro)
                        "\nYou can now help support the developer with micCheck Pro, a single ${proPriceFlow.value} purchase which gives you a few added perks."
                    else
                        "\nYour support is greatly appreciated :)",
            style = MaterialTheme.typography.titleMedium
        )
        if (!isPro) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = openProScreen) {
                Text("Learn More")
            }
        }
    }
}

//@Preview
//@Composable
//fun FirstLaunchPreview () {
//    MicCheckTheme (darkTheme = false) { FirstLaunchScreen() }
//}
