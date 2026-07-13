package com.omnitune.app.viewmodels

import androidx.lifecycle.ViewModel
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.db.entities.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class TopPlaylistViewModel @Inject constructor(
    database: MusicDatabase
) : ViewModel() {
    val top = 100

    val topPeriod = MutableStateFlow(com.omnitune.app.constants.MyTopFilter.ALL_TIME)

    val topSongs: Flow<List<Song>> = topPeriod.flatMapLatest { period ->
        val fromTimeStamp = when (period) {
            com.omnitune.app.constants.MyTopFilter.ALL_TIME -> 0L
            com.omnitune.app.constants.MyTopFilter.DAY -> System.currentTimeMillis() - 24 * 60 * 60 * 1000L
            com.omnitune.app.constants.MyTopFilter.WEEK -> System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
            com.omnitune.app.constants.MyTopFilter.MONTH -> System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L
            com.omnitune.app.constants.MyTopFilter.YEAR -> System.currentTimeMillis() - 365L * 24 * 60 * 60 * 1000L
        }
        database.mostPlayedSongs(fromTimeStamp, top)
    }
}
