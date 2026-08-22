package com.omnitune.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.omnitune.app.update.ChangelogSource
import com.omnitune.app.update.ChangelogViewModel
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ChangelogScreen(
    navController: NavController,
    viewModel: ChangelogViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val release = state.release

    SettingsSubScreenScaffold(
        title = "Changelog",
        onBack = { navController.popBackStack() },
    ) {
        OmniPreferenceCard(title = "Latest changes") {
            Column(modifier = Modifier.padding(OmniSpacing.medium)) {
                Text(
                    text = release.releaseName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OmniColors.TextPrimary,
                )
                Spacer(modifier = Modifier.height(OmniSpacing.compact))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when (release.source) {
                            ChangelogSource.Bundled -> "Installed app release"
                            ChangelogSource.GitHub -> "Latest GitHub release"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = OmniColors.TextSecondary,
                    )
                    release.publishedAt?.let {
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = it.take(10),
                            style = MaterialTheme.typography.bodySmall,
                            color = OmniColors.TextTertiary,
                        )
                    }
                }

                if (state.loading) {
                    Spacer(modifier = Modifier.height(OmniSpacing.medium))
                    LinearProgressIndicator(
                        color = OmniColors.OmniAccentPrimary,
                        trackColor = OmniColors.OmniGlassSubtle,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                state.errorMessage?.let {
                    Spacer(modifier = Modifier.height(OmniSpacing.medium))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = OmniColors.Error,
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        SettingsActionButton(
            label = if (state.loading) "Refreshing..." else "Refresh latest release notes",
            onClick = {
                if (!state.loading) {
                    viewModel.refreshLatestRelease()
                }
            },
        )

        Spacer(Modifier.height(8.dp))

        ChangelogMarkdown(body = release.body)
    }
}

@Composable
private fun ChangelogMarkdown(body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(OmniShapes.Medium)
            .background(OmniColors.OmniGlassMedium)
            .padding(OmniSpacing.medium),
    ) {
        body.lines()
            .map { it.trimEnd() }
            .filterNot { it.isBlank() }
            .forEach { line ->
                when {
                    line.startsWith("# ") -> {
                        Text(
                            text = line.removePrefix("# ").trim(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = OmniColors.TextPrimary,
                            modifier = Modifier.padding(bottom = OmniSpacing.small),
                        )
                    }
                    line.startsWith("## ") -> {
                        Spacer(modifier = Modifier.height(OmniSpacing.medium))
                        Text(
                            text = line.removePrefix("## ").trim(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = OmniColors.OmniAccentSecondary,
                        )
                    }
                    line.startsWith("- ") -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = OmniSpacing.small),
                        ) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OmniColors.OmniAccentPrimary,
                                modifier = Modifier.padding(end = OmniSpacing.compact),
                            )
                            Text(
                                text = line.removePrefix("- ").trim(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = OmniColors.TextSecondary,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = line.trim(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OmniColors.TextSecondary,
                            modifier = Modifier.padding(top = OmniSpacing.small),
                        )
                    }
                }
            }
    }
}
