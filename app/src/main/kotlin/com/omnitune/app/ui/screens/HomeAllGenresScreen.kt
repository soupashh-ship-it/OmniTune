package com.omnitune.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omnitune.app.R
import com.omnitune.app.ui.component.OmniChrome
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import com.omnitune.app.ui.theme.OmniTextStyles

@Composable
fun HomeAllGenresScreen(
    onBack: () -> Unit,
    onChipClick: (MoodChip) -> Unit,
) {
    val chips = GenreChipsHolder.chips

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = OmniSpacing.small, vertical = OmniSpacing.compact),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "Back",
                    tint = OmniColors.TextPrimary,
                )
            }
            Spacer(modifier = Modifier.width(OmniSpacing.small))
            Text(
                text = "Mood and Genres",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = OmniColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = OmniSpacing.large),
            horizontalArrangement = Arrangement.spacedBy(OmniSpacing.small),
            verticalArrangement = Arrangement.spacedBy(OmniSpacing.small),
            contentPadding = PaddingValues(bottom = OmniChrome.BottomContentPadding),
        ) {
            itemsIndexed(chips, key = { _, c -> c.id }) { index, chip ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(delayMillis = index * 40)) +
                        slideInVertically(
                            animationSpec = spring(dampingRatio = 0.7f),
                            initialOffsetY = { it / 4 },
                        ),
                ) {
                    AllGenreCard(
                        chip = chip,
                        index = index,
                        onClick = { onChipClick(chip) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AllGenreCard(
    chip: MoodChip,
    index: Int,
    onClick: () -> Unit,
) {
    val accents = listOf(
        OmniColors.OmniAccentSecondary,
        OmniColors.OmniAccentPrimary,
        OmniColors.OmniAccentTertiary,
        OmniColors.Hot,
        OmniColors.Warning,
    )
    val accent = accents[index % accents.size]
    val icons = listOf(
        R.drawable.ic_play_arrow,
        R.drawable.ic_album,
        R.drawable.ic_history,
        R.drawable.ic_search,
        R.drawable.ic_list,
    )
    val iconRes = icons[index % icons.size]

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 62.dp)
            .clip(OmniShapes.Medium)
            .background(
                Brush.linearGradient(
                    listOf(
                        OmniColors.SurfaceSubtle.copy(alpha = 0.80f),
                        accent.copy(alpha = 0.08f),
                    ),
                ),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = OmniSpacing.medium, vertical = OmniSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(OmniShapes.Pill)
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(modifier = Modifier.width(OmniSpacing.compact))
        Text(
            text = chip.label,
            style = OmniTextStyles.songTitle,
            fontWeight = FontWeight.SemiBold,
            color = OmniColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
