package com.omnitune.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.db.entities.EventWithSong
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class ListeningStatsViewModel @Inject constructor(
    database: MusicDatabase
) : ViewModel() {
    val uiState: StateFlow<ListeningStatsUiState> = database.events()
        .map { events -> events.toListeningStatsUiState() }
        .catch { error ->
            emit(
                ListeningStatsUiState(
                    isLoading = false,
                    error = error.message ?: "Listening stats could not be loaded"
                )
            )
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ListeningStatsUiState()
        )
}

data class ListeningStatsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val reportYear: Int = LocalDateTime.now().year,
    val totalSongsPlayed: Int = 0,
    val uniqueSongsPlayed: Int = 0,
    val totalListeningTimeMs: Long = 0,
    val averageDailyMs: Long = 0,
    val totalMonthsListened: Double = 0.0,
    val musicPersonality: MusicPersonality = MusicPersonality(
        title = "Getting Started",
        description = "Play more songs to unlock your listening profile"
    ),
    val weeklyTrends: List<DailyListening> = emptyList(),
    val topSongs: List<ListeningSongStats> = emptyList(),
    val topArtists: List<ArtistStats> = emptyList(),
    val topArtistThisMonth: ArtistStats? = null,
    val timeOfDayStats: Map<TimeOfDay, Int> = emptyTimeOfDayStats()
)

data class DailyListening(
    val dayName: String,
    val minutesListen: Long
)

data class MusicPersonality(
    val title: String,
    val description: String
)

data class ListeningSongStats(
    val id: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
    val playCount: Int,
    val timeListenedMs: Long
)

data class ArtistStats(
    val id: String,
    val artist: String,
    val totalPlays: Int,
    val timeListenedMs: Long,
    val thumbnailUrl: String?
)

enum class TimeOfDay {
    MORNING,
    AFTERNOON,
    EVENING,
    NIGHT
}

private fun List<EventWithSong>.toListeningStatsUiState(): ListeningStatsUiState {
    val now = LocalDateTime.now()
    if (isEmpty()) {
        return ListeningStatsUiState(
            isLoading = false,
            reportYear = now.year,
            weeklyTrends = buildWeeklyTrends(now),
            timeOfDayStats = emptyTimeOfDayStats()
        )
    }

    val totalPlayTime = sumOf { it.event.playTime.coerceAtLeast(0L) }
    val totalPlays = size
    val uniqueSongs = map { it.event.songId }.distinct().size
    val firstListen = minOf { it.event.timestamp }
    val daysListened = (ChronoUnit.DAYS.between(firstListen.toLocalDate(), now.toLocalDate()) + 1)
        .coerceAtLeast(1)
    val monthStart = now.withDayOfMonth(1).toLocalDate().atStartOfDay()
    val timeOfDay = buildTimeOfDayStats()
    val topArtists = buildArtistStats()

    return ListeningStatsUiState(
        isLoading = false,
        reportYear = now.year,
        totalSongsPlayed = totalPlays,
        uniqueSongsPlayed = uniqueSongs,
        totalListeningTimeMs = totalPlayTime,
        averageDailyMs = totalPlayTime / daysListened,
        totalMonthsListened = daysListened / 30.44,
        musicPersonality = classifyMusicPersonality(
            totalPlays = totalPlays,
            uniqueSongs = uniqueSongs,
            totalListeningTimeMs = totalPlayTime,
            topArtists = topArtists,
            timeOfDayStats = timeOfDay
        ),
        weeklyTrends = buildWeeklyTrends(now),
        topSongs = buildTopSongStats(),
        topArtists = topArtists,
        topArtistThisMonth = filter { !it.event.timestamp.isBefore(monthStart) }
            .buildArtistStats(limit = 1)
            .firstOrNull(),
        timeOfDayStats = timeOfDay
    )
}

private fun List<EventWithSong>.buildTopSongStats(limit: Int = 8): List<ListeningSongStats> =
    groupBy { it.event.songId }
        .mapNotNull { (songId, events) ->
            val latest = events.maxByOrNull { it.event.timestamp } ?: return@mapNotNull null
            val song = latest.song.song
            ListeningSongStats(
                id = songId,
                title = song.title,
                artist = latest.song.artists.joinToString { it.name }.ifBlank { "Unknown artist" },
                thumbnailUrl = song.thumbnailUrl,
                playCount = events.size,
                timeListenedMs = events.sumOf { it.event.playTime.coerceAtLeast(0L) }
            )
        }
        .sortedWith(
            compareByDescending<ListeningSongStats> { it.timeListenedMs }
                .thenByDescending { it.playCount }
        )
        .take(limit)

private fun List<EventWithSong>.buildArtistStats(limit: Int = 8): List<ArtistStats> {
    val aggregates = linkedMapOf<String, ArtistAccumulator>()

    forEach { event ->
        event.song.artists.forEach { artist ->
            val key = artist.id.ifBlank { artist.name }
            val current = aggregates[key]
            val playTime = event.event.playTime.coerceAtLeast(0L)
            aggregates[key] = if (current == null) {
                ArtistAccumulator(
                    id = key,
                    name = artist.name,
                    plays = 1,
                    playTimeMs = playTime,
                    thumbnailUrl = artist.thumbnailUrl
                )
            } else {
                current.copy(
                    plays = current.plays + 1,
                    playTimeMs = current.playTimeMs + playTime,
                    thumbnailUrl = current.thumbnailUrl ?: artist.thumbnailUrl
                )
            }
        }
    }

    return aggregates.values
        .map { it.toArtistStats() }
        .sortedWith(
            compareByDescending<ArtistStats> { it.timeListenedMs }
                .thenByDescending { it.totalPlays }
        )
        .take(limit)
}

private fun List<EventWithSong>.buildWeeklyTrends(now: LocalDateTime): List<DailyListening> {
    val today = now.toLocalDate()
    val byDate = groupBy { it.event.timestamp.toLocalDate() }
    return (6 downTo 0).map { daysAgo ->
        val day = today.minusDays(daysAgo.toLong())
        val minutes = byDate[day]
            .orEmpty()
            .sumOf { it.event.playTime.coerceAtLeast(0L) }
            .let { TimeUnit.MILLISECONDS.toMinutes(it) }
        DailyListening(
            dayName = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
            minutesListen = minutes
        )
    }
}

private fun List<EventWithSong>.buildTimeOfDayStats(): Map<TimeOfDay, Int> {
    val counts = TimeOfDay.values().associateWith { 0 }.toMutableMap()
    forEach { event ->
        val time = when (event.event.timestamp.hour) {
            in 5..11 -> TimeOfDay.MORNING
            in 12..16 -> TimeOfDay.AFTERNOON
            in 17..21 -> TimeOfDay.EVENING
            else -> TimeOfDay.NIGHT
        }
        counts[time] = (counts[time] ?: 0) + 1
    }
    return counts
}

private fun classifyMusicPersonality(
    totalPlays: Int,
    uniqueSongs: Int,
    totalListeningTimeMs: Long,
    topArtists: List<ArtistStats>,
    timeOfDayStats: Map<TimeOfDay, Int>
): MusicPersonality {
    val topArtistShare = topArtists.firstOrNull()?.totalPlays ?: 0
    val nightPlays = timeOfDayStats[TimeOfDay.NIGHT] ?: 0
    val totalHours = TimeUnit.MILLISECONDS.toHours(totalListeningTimeMs)

    return when {
        totalPlays == 0 -> MusicPersonality(
            title = "Getting Started",
            description = "Play more songs to unlock your listening profile"
        )
        nightPlays >= totalPlays / 2 && totalPlays >= 5 -> MusicPersonality(
            title = "Night Owl",
            description = "Your listening comes alive after dark"
        )
        uniqueSongs >= 50 || topArtists.size >= 6 -> MusicPersonality(
            title = "Explorer",
            description = "You move across artists and styles with an open ear"
        )
        totalHours >= 40 -> MusicPersonality(
            title = "Power Listener",
            description = "Music is a steady part of your everyday rhythm"
        )
        topArtistShare >= maxOf(3, totalPlays / 3) -> MusicPersonality(
            title = "Loyal Fan",
            description = "You know what you love and keep coming back to it"
        )
        else -> MusicPersonality(
            title = "Groove Keeper",
            description = "Your listening has a steady, familiar pulse"
        )
    }
}

private fun emptyTimeOfDayStats(): Map<TimeOfDay, Int> =
    TimeOfDay.values().associateWith { 0 }

private data class ArtistAccumulator(
    val id: String,
    val name: String,
    val plays: Int,
    val playTimeMs: Long,
    val thumbnailUrl: String?
) {
    fun toArtistStats() = ArtistStats(
        id = id,
        artist = name,
        totalPlays = plays,
        timeListenedMs = playTimeMs,
        thumbnailUrl = thumbnailUrl
    )
}
