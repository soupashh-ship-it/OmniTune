/*
 * This file was adapted from SuvMusic.
 * Original copyright follows:
 *
 * Copyright (C) Suvojeet
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */

package com.omnitune.app.ui.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omnitune.app.ui.screens.settings.about.AboutDescriptionSection
import com.omnitune.app.ui.screens.settings.about.AboutDeveloperSection
import com.omnitune.app.ui.screens.settings.about.AboutFeaturesSection
import com.omnitune.app.ui.screens.settings.about.AboutFooterSection
import com.omnitune.app.ui.screens.settings.about.AboutHeroSection
import com.omnitune.app.ui.screens.settings.about.AboutInformationSection
import com.omnitune.app.ui.screens.settings.about.AboutStorySection
import com.omnitune.app.ui.screens.settings.about.AboutTechStackSection
import com.omnitune.app.ui.navigation.LocalRouteChromeInsets
import com.omnitune.app.ui.utils.animateEnter
import com.omnitune.app.ui.utils.dpadFocusable

/**
 * About screen using the Material 3 Expressive section layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBackClick: () -> Unit,
    onHowItWorksClick: () -> Unit = {},
    onCreditsClick: () -> Unit = {},
    onChangelogClick: () -> Unit = {},
    onSupportClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val chromeInsets = LocalRouteChromeInsets.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("About OmniTune", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .dpadFocusable(onClick = onBackClick, shape = CircleShape)
                            .padding(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = chromeInsets.contentBottomPadding),
        ) {
            item { Column(modifier = Modifier.animateEnter(0)) { AboutHeroSection() } }
            item { Column(modifier = Modifier.animateEnter(1)) { AboutDescriptionSection() } }
            item { Column(modifier = Modifier.animateEnter(2)) { AboutStorySection() } }
            item { Column(modifier = Modifier.animateEnter(3)) { AboutFeaturesSection() } }
            item {
                Column(modifier = Modifier.animateEnter(4)) {
                    AboutDeveloperSection(onOpenUri = context::openExternalUrl)
                }
            }
            item { Column(modifier = Modifier.animateEnter(5)) { AboutTechStackSection() } }
            item {
                Column(modifier = Modifier.animateEnter(6)) {
                    AboutInformationSection(
                        onOpenUri = context::openExternalUrl,
                        onHowItWorksClick = onHowItWorksClick,
                        onCreditsClick = onCreditsClick,
                        onChangelogClick = onChangelogClick,
                        onSupportClick = onSupportClick,
                    )
                }
            }
            item { Column(modifier = Modifier.animateEnter(7)) { AboutFooterSection() } }
        }
    }
}
