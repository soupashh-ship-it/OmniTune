package com.omnitune.app.ui.player

import com.omnitune.app.data.LyricsRepository
import com.omnitune.app.models.AppResult
import com.omnitune.app.models.LyricsLine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LyricsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        kotlinx.coroutines.Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun `previous song lyrics cannot overwrite current song`() = runTest(dispatcher) {
        val repository = FakeLyricsRepository()
        val viewModel = LyricsViewModel(repository)

        viewModel.loadLyrics("old", "Old Song", "Artist", 180)
        viewModel.loadLyrics("new", "New Song", "Artist", 180)

        repository.complete("old", listOf(LyricsLine(timestamp = -1, text = "old lyrics")))
        advanceUntilIdle()
        assertEquals(LyricsUiState.Loading, viewModel.uiState.value)

        repository.complete("new", listOf(LyricsLine(timestamp = -1, text = "new lyrics")))
        advanceUntilIdle()

        assertEquals(
            LyricsUiState.Success(listOf(LyricsLine(timestamp = -1, text = "new lyrics"))),
            viewModel.uiState.value,
        )
    }

    private class FakeLyricsRepository : LyricsRepository {
        private val requests = mutableMapOf<String, CompletableDeferred<AppResult<List<LyricsLine>>>>()

        override suspend fun loadLyrics(
            songId: String,
            title: String,
            artist: String,
            duration: Long,
        ): AppResult<List<LyricsLine>> =
            requests.getOrPut(songId) { CompletableDeferred() }.await()

        override fun parseLrc(lrcText: String): List<LyricsLine> = emptyList()

        fun complete(songId: String, lines: List<LyricsLine>) {
            requests.getOrPut(songId) { CompletableDeferred() }.complete(AppResult.Success(lines))
        }
    }
}
