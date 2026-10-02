package com.omnitune.shared.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnitune.shared.data.lyrics.LrcParser
import com.omnitune.shared.data.lyrics.ParsedLrc
import com.omnitune.shared.domain.models.Lyrics

/**
 * Synchronized real-time time-coded lyrics display with active line highlighting,
 * smooth auto-scroll centering, tap-to-seek, and unsynced fallback.
 */
@Composable
fun LyricsScreen(
    lyrics: Lyrics?,
    currentPositionMs: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    val parsedLrc: ParsedLrc = remember(lyrics) {
        if (lyrics == null) {
            ParsedLrc()
        } else if (lyrics.syncedLyrics.isNotEmpty()) {
            ParsedLrc(lines = lyrics.syncedLyrics, isSynced = true)
        } else if (!lyrics.plainText.isNullOrBlank()) {
            LrcParser.parse(lyrics.plainText)
        } else {
            ParsedLrc()
        }
    }

    val activeIndex = remember(parsedLrc, currentPositionMs) {
        if (parsedLrc.isSynced && parsedLrc.lines.isNotEmpty()) {
            LrcParser.findActiveLineIndex(parsedLrc.lines, currentPositionMs)
        } else {
            -1
        }
    }

    val listState = rememberLazyListState()

    // Smoothly auto-scroll so the active lyric line is centered
    LaunchedEffect(activeIndex) {
        if (activeIndex in parsedLrc.lines.indices) {
            val targetScroll = (activeIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(targetScroll)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (parsedLrc.lines.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No lyrics available",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 72.dp)
            ) {
                itemsIndexed(
                    items = parsedLrc.lines,
                    key = { index, _ -> "lyric_$index" }
                ) { index, line ->
                    val isActive = (index == activeIndex)

                    val textColor by animateColorAsState(
                        targetValue = if (isActive) {
                            accentColor
                        } else if (parsedLrc.isSynced) {
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
                        },
                        animationSpec = tween(durationMillis = 250),
                        label = "lyric_color_$index"
                    )

                    val fontSize = if (isActive) 22.sp else 18.sp
                    val fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = parsedLrc.isSynced && line.timestampMs >= 0L) {
                                onSeekTo(line.timestampMs)
                            }
                            .padding(vertical = 4.dp, horizontal = 8.dp)
                    ) {
                        Text(
                            text = line.text.ifBlank { "♪" },
                            fontSize = fontSize,
                            fontWeight = fontWeight,
                            lineHeight = (fontSize.value * 1.35f).sp,
                            color = textColor,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
