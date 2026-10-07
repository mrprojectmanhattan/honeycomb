package com.mark.moodlogger.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mark.moodlogger.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About Honeycomb") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .honeycombBackground(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Para(
                "Honeycomb is a small mental and physical hygiene tracker: mood, " +
                    "water, sleep, movement, habits, and goals.",
            )

            Head("Your data stays here")
            Para(
                "Everything you log lives in a single file on this phone and nowhere " +
                    "else. There is no account, no sign-in, no internet connection, no " +
                    "analytics, and no ads. The app has no permission to send anything " +
                    "anywhere. Export to CSV from Settings whenever you want a copy.",
            )

            Head("Nothing automatic is required")
            Para(
                "Every number in this app can be typed in by hand. Water, sleep times, " +
                    "steps, workouts, goal check-ins - all manual if you prefer.",
            )
            Para(
                "There are two optional helpers, and both are OFF until you switch them " +
                    "on in Settings:",
            )
            Para(
                "•  Sleep guess - notices when you plug in and unplug your charger " +
                    "overnight and pre-fills a bed/wake time you confirm. It reads only " +
                    "those two events.",
            )
            Para(
                "•  Step count - reads your phone's built-in step sensor, which is " +
                    "just a running count of steps. It does not track where you go, what " +
                    "apps you use, or anything else.",
            )
            Para("Turn either one off and the app forgets it and goes back to manual.")

            Head("Version")
            Para("Honeycomb ${BuildConfig.VERSION_NAME}")

            Spacer(Modifier.size(20.dp))
        }
    }
}

@Composable
private fun Head(text: String) {
    Text(text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
}

@Composable
private fun Para(text: String) {
    Text(text, fontSize = 13.sp, color = Color.Gray, lineHeight = 19.sp)
}
