package com.omnitune.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.omnitune.app.R
import com.omnitune.app.ui.screens.settings.SettingsSubScreenScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeCreatorScreen(navController: NavController) {
    SettingsSubScreenScaffold(
        title = "Theme Creator",
        onBack = { navController.popBackStack() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Advanced Theme Engine",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "The dynamic theme generator is currently being optimized for OmniTune's shell. Stay tuned for the next update!",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
