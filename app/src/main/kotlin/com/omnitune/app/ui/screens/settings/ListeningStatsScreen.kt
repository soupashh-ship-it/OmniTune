/*
 * This file was adapted from SuvMusic.
 * Original copyright follows:
 *
 * Copyright (C) Suvojeet
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */

package com.omnitune.app.ui.screens.settings

import android.content.Intent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.omnitune.app.ui.component.DominantColors
import com.omnitune.app.ui.component.LoadingIndicator
import com.omnitune.app.ui.component.MeshGradientBackground
import com.omnitune.app.ui.utils.displayLabel
import java.util.Locale
import java.util.concurrent.TimeUnit
import androidx.compose.runtime.mutableStateOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeningStatsScreen(
    onBackClick: () -> Unit,
    onWrappedClick: (() -> Unit)? = null,
    viewModel: ListeningStatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showShareDialog by remember { mutableStateOf(false) }

    if (showShareDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showShareDialog = false }
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MusicInsightsShareCard(
                        uiState = uiState,
                        modifier = Modifier.clip(RoundedCornerShape(24.dp))
                    )
                    Button(
                        onClick = {
                            val topArtist = uiState.topArtists.firstOrNull()?.artist ?: "Unknown"
                            val totalMinutes = uiState.totalListeningTimeMs / 60000
                            val months = String.format(Locale.US, "%.1f", uiState.totalMonthsListened)
                            val text = "My Music Insights on OmniTune\n\n" +
                                "Personality: ${uiState.musicPersonality.title}\n" +
                                "Total Playtime: $totalMinutes mins\n" +
                                "Top Artist: $topArtist\n" +
                                "Months with OmniTune: $months\n\n" +
                                "#OmniTune #MusicInsights"

                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share your insights"))
                        },
                        modifier = Modifier.padding(16.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Share as Text")
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Your Insights", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (onWrappedClick != null) {
                        IconButton(onClick = onWrappedClick) {
                            Icon(Icons.Default.AutoAwesome, "Your Wrapped")
                        }
                    }
                    IconButton(onClick = { showShareDialog = true }) {
                        Icon(Icons.Default.Share, "Share")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            uiState.totalSongsPlayed == 0 -> EmptyStatsState(padding, uiState.error)

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item(key = "personality") {
                        AnimatedEntry {
                            MusicPersonalityHero(uiState.musicPersonality)
                        }
                    }

                    item(key = "metrics") {
                        AnimatedEntry(delay = 100) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                GlobalStatsRow(uiState)
                            }
                        }
                    }

                    item(key = "weekly") {
                        AnimatedEntry(delay = 200) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                WeeklyActivitySection(uiState.weeklyTrends)
                            }
                        }
                    }

                    uiState.topArtistThisMonth?.let { artist ->
                        item(key = "monthly") {
                            AnimatedEntry(delay = 300) {
                                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    MonthlyHighlightSection(artist)
                                }
                            }
                        }
                    }

                    item(key = "top_content") {
                        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                            if (uiState.topSongs.isNotEmpty()) {
                                AnimatedEntry(delay = 400) {
                                    Column {
                                        SectionHeaderWithPadding("Most Played Songs", Icons.Default.Audiotrack)
                                        Spacer(Modifier.height(12.dp))
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            contentPadding = PaddingValues(horizontal = 16.dp)
                                        ) {
                                            items(uiState.topSongs, key = { it.id }) { song ->
                                                TopSongCard(song)
                                            }
                                        }
                                    }
                                }
                            }

                            if (uiState.topArtists.isNotEmpty()) {
                                AnimatedEntry(delay = 500) {
                                    Column {
                                        SectionHeaderWithPadding("Top Artists", Icons.Default.Person)
                                        Spacer(Modifier.height(12.dp))
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            contentPadding = PaddingValues(horizontal = 16.dp)
                                        ) {
                                            items(uiState.topArtists, key = { it.id }) { artist ->
                                                TopArtistCard(artist)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item(key = "time_of_day") {
                        AnimatedEntry(delay = 600) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                TimeOfDaySection(uiState.timeOfDayStats)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedEntry(
    delay: Int = 0,
    content: @Composable () -> Unit
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!visible) {
            kotlinx.coroutines.delay((delay / 2).toLong())
            visible = true
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400),
        label = "entryAlpha"
    )
    val translateY by animateFloatAsState(
        targetValue = if (visible) 0f else 24f,
        animationSpec = tween(400),
        label = "entryTranslateY"
    )
    Box(
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha
            this.translationY = translateY
        }
    ) {
        content()
    }
}

@Composable
private fun MusicInsightsShareCard(
    uiState: ListeningStatsUiState,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = modifier
            .width(360.dp)
            .height(640.dp)
            .background(
                Brush.sweepGradient(
                    0.0f to primaryColor.copy(alpha = 0.8f),
                    0.3f to Color(0xFF1DB954),
                    0.6f to secondaryColor.copy(alpha = 0.8f),
                    1.0f to primaryColor.copy(alpha = 0.8f)
                )
            )
            .background(Color.Black.copy(alpha = 0.2f))
            .padding(28.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawPath(
                path = Path().apply {
                    moveTo(0f, size.height * 0.7f)
                    quadraticTo(size.width * 0.5f, size.height * 0.8f, size.width, size.height * 0.6f)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                },
                color = Color.White.copy(alpha = 0.1f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = 200.dp.toPx(),
                center = Offset(size.width * 0.8f, size.height * 0.2f)
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "OMNITUNE",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            }

            Spacer(Modifier.height(60.dp))

            Text(
                "My music\npersonality is",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Normal,
                lineHeight = 32.sp
            )
            Text(
                uiState.musicPersonality.title.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp
            )

            Spacer(Modifier.height(40.dp))

            Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatValue("Total Minutes", (uiState.totalListeningTimeMs / 60000).toString(), Modifier.weight(1f))
                    StatValue("Top Songs", uiState.uniqueSongsPlayed.toString(), Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatValue("Months Listened", String.format(Locale.US, "%.1f", uiState.totalMonthsListened), Modifier.weight(1f))
                    StatValue("Top Artist", uiState.topArtists.firstOrNull()?.artist ?: "None", Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "${uiState.reportYear} INSIGHTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "OMNITUNE INSIGHTS",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 5.sp
                )
            }
        }
    }
}

@Composable
private fun StatValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Black,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SectionHeaderWithPadding(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MusicPersonalityHero(personality: MusicPersonality) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(32.dp))
    ) {
        MeshGradientBackground(
            dominantColors = DominantColors(
                primary = MaterialTheme.colorScheme.primaryContainer,
                secondary = MaterialTheme.colorScheme.tertiaryContainer,
                accent = MaterialTheme.colorScheme.secondaryContainer,
                onBackground = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "YOUR PERSONALITY",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = personality.title,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = personality.description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeeklyActivitySection(trends: List<DailyListening>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Weekly Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Listening minutes per day",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Default.Timeline, null, tint = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(32.dp))

            BezierChart(trends)
        }
    }
}

@Composable
private fun BezierChart(trends: List<DailyListening>) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()

    val maxMinutes = trends.maxOfOrNull { it.minutesListen }?.toFloat() ?: 1f
    val chartMax = maxOf(maxMinutes, 30f) * 1.2f

    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "ChartAnimation"
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        val width = size.width
        val height = size.height
        val spacing = if (trends.size > 1) width / (trends.size - 1) else width

        val points = trends.mapIndexed { index, daily ->
            Offset(
                x = index * spacing,
                y = height - ((daily.minutesListen / chartMax) * height * animationProgress)
            )
        }

        val path = Path()
        val fillPath = Path()

        if (points.isNotEmpty()) {
            path.moveTo(points[0].x, points[0].y)
            fillPath.moveTo(0f, height)
            fillPath.lineTo(points[0].x, points[0].y)

            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2, p0.y)
                val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2, p1.y)

                path.cubicTo(
                    controlPoint1.x,
                    controlPoint1.y,
                    controlPoint2.x,
                    controlPoint2.y,
                    p1.x,
                    p1.y
                )
                fillPath.cubicTo(
                    controlPoint1.x,
                    controlPoint1.y,
                    controlPoint2.x,
                    controlPoint2.y,
                    p1.x,
                    p1.y
                )
            }

            fillPath.lineTo(width, height)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.3f * animationProgress), Color.Transparent),
                    startY = 0f,
                    endY = height
                )
            )

            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )

            trends.forEachIndexed { index, daily ->
                val point = points[index]
                if ((daily.minutesListen > 0 || index == 0 || index == trends.size - 1) && animationProgress > 0.8f) {
                    drawCircle(color = primaryColor, radius = 4.dp.toPx(), center = point)
                    drawCircle(color = surfaceColor, radius = 2.dp.toPx(), center = point)
                }

                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = labelColor
                        textSize = 28f
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        alpha = (255 * animationProgress).toInt()
                    }
                    drawText(daily.dayName, point.x, height + 40f, paint)
                }
            }
        }
    }
}

@Composable
private fun MonthlyHighlightSection(artist: ArtistStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    artist.artist.take(1).uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.width(20.dp))

            Column {
                Text(
                    "MONTHLY STAR",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    artist.artist,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "You've played them ${artist.totalPlays} times this month!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun GlobalStatsRow(uiState: ListeningStatsUiState) {
    val totalHours = TimeUnit.MILLISECONDS.toHours(uiState.totalListeningTimeMs)
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(uiState.totalListeningTimeMs) % 60
    val avgHours = TimeUnit.MILLISECONDS.toHours(uiState.averageDailyMs)
    val avgMinutes = TimeUnit.MILLISECONDS.toMinutes(uiState.averageDailyMs) % 60

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCardSmall(
                modifier = Modifier.weight(1f),
                title = "Total Plays",
                value = uiState.totalSongsPlayed.toString(),
                icon = Icons.Default.GraphicEq
            )
            StatCardSmall(
                modifier = Modifier.weight(1f),
                title = "Total Time",
                value = "${totalHours}h ${totalMinutes}m",
                icon = Icons.Default.AccessTime
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCardSmall(
                modifier = Modifier.weight(1f),
                title = "Months With Us",
                value = String.format(Locale.US, "%.1f", uiState.totalMonthsListened),
                icon = Icons.Default.Timeline
            )
            StatCardSmall(
                modifier = Modifier.weight(1f),
                title = "Daily Average",
                value = if (avgHours > 0) "${avgHours}h ${avgMinutes}m" else "${avgMinutes}m",
                icon = Icons.Default.Timeline
            )
        }
    }
}

@Composable
private fun StatCardSmall(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TopSongCard(song: ListeningSongStats) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.width(140.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(song.thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${song.playCount} plays",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun TopArtistCard(artist: ArtistStats) {
    Card(
        modifier = Modifier.width(120.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (artist.thumbnailUrl.isNullOrBlank()) {
                    Text(
                        text = artist.artist.take(1).uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    AsyncImage(
                        model = artist.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = artist.artist,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${artist.totalPlays} plays",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun TimeOfDaySection(stats: Map<TimeOfDay, Int>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "When do you listen?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            val max = stats.values.maxOrNull()?.toFloat() ?: 1f

            TimeOfDay.values().forEach { time ->
                val count = stats[time] ?: 0
                val progress = if (max > 0) count / max else 0f

                val animatedProgress by animateFloatAsState(
                    targetValue = progress,
                    animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                    label = "BarAnimation"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = time.displayLabel(),
                        modifier = Modifier.width(88.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStatsState(
    padding: PaddingValues,
    error: String? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Audiotrack,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (error == null) "No listening history yet" else "Listening stats unavailable",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = error ?: "Start playing songs to see your stats!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}
