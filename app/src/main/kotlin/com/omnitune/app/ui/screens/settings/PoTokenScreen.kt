package com.omnitune.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.omnitune.app.ui.theme.OmniColors
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoTokenScreen(
    navController: NavController,
    viewModel: PoTokenViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("YouTube PO Token") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(androidx.compose.ui.res.painterResource(com.omnitune.app.R.drawable.ic_arrow_back), contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OmniColors.OmniBackgroundBase,
                    titleContentColor = OmniColors.TextPrimary,
                    navigationIconContentColor = OmniColors.TextPrimary
                )
            )
        },
        containerColor = OmniColors.OmniBackgroundBase
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (val currentState = state) {
                is PoTokenState.Success -> {
                    Text("Tokens Generated", color = OmniColors.OmniAccentPrimary, style = MaterialTheme.typography.titleMedium)
                    Text("GVS Token: ${currentState.gvsToken.take(15)}...", color = OmniColors.TextSecondary)
                    Text("Player Token: ${currentState.playerToken.take(15)}...", color = OmniColors.TextSecondary)
                    Text("Visitor Data: ${currentState.visitorData.take(15)}...", color = OmniColors.TextSecondary)
                }
                is PoTokenState.Error -> {
                    Text("Error: ${currentState.message}", color = MaterialTheme.colorScheme.error)
                }
                PoTokenState.Idle -> {
                    Text("No tokens generated yet.", color = OmniColors.TextSecondary)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { /* Launch extraction activity here later */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Regenerate Tokens")
            }
        }
    }
}
