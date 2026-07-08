package com.omnitune.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.omnitune.app.ui.theme.OmniColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearInMusicScreen(
    navController: NavController,
    viewModel: YearInMusicViewModel = hiltViewModel()
) {
    val selectedYear by viewModel.selectedYear.collectAsState()
    val totalListeningTime by viewModel.totalListeningTime.collectAsState()
    val totalSongsPlayed by viewModel.totalSongsPlayed.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Year in Music") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OmniColors.OmniBackgroundBase,
                    titleContentColor = OmniColors.TextPrimary
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = selectedYear.toString(),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = OmniColors.OmniAccentPrimary
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OmniColors.OmniBackgroundElevated)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Listening Time", color = OmniColors.TextSecondary)
                    Text(
                        formatListeningTime(totalListeningTime),
                        color = OmniColors.TextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OmniColors.OmniBackgroundElevated)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Songs Played", color = OmniColors.TextSecondary)
                    Text("$totalSongsPlayed", color = OmniColors.TextPrimary, style = MaterialTheme.typography.titleLarge)
                }
            }
            
            Button(onClick = { navController.popBackStack() }) {
                Text("Back to Stats")
            }
        }
    }
}

internal fun formatListeningTime(durationMs: Long): String {
    val totalMinutes = (durationMs.coerceAtLeast(0L) / 60_000L)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        totalMinutes == 1L -> "1 minute"
        totalMinutes > 0 -> "$totalMinutes minutes"
        else -> "0 minutes"
    }
}
