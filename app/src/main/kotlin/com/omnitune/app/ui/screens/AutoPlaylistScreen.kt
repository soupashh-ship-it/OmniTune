package com.omnitune.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@Composable
fun AutoPlaylistScreen(navController: NavController, playlistType: String) {
    Box { Text("AutoPlaylistScreen Port Placeholder for $playlistType") }
}