package com.omnitune.app.ui.screens

import android.content.Context
import androidx.room.Room
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.omnitune.app.data.SearchProvider
import com.omnitune.app.db.InternalDatabase
import com.omnitune.app.db.MusicDatabase
import com.omnitune.innertube.YouTube
import com.omnitune.innertube.models.Artist
import com.omnitune.innertube.models.SongItem
import com.omnitune.innertube.pages.SearchResult
import com.omnitune.innertube.pages.SearchSummaryPage
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchViewModelInstrumentedTest {

    @Test
    fun deterministicProviderDrivesSearchFilterDeduplicationAndEmptyQuery() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val internal = Room.inMemoryDatabaseBuilder(context, InternalDatabase::class.java).build()
        val database = MusicDatabase(internal)
        val viewModel = SearchViewModel(
            context = context,
            database = database,
            searchProvider = DuplicateSongProvider,
            networkStatus = object : SearchNetworkStatus { override fun isOnline() = true },
            searchTiming = object : SearchTiming { override val debounceMillis = 0L },
        )

        try {
            viewModel.onFilterSelected(SearchFilterTab.Songs)
            viewModel.onQueryChanged("runtime")

            val completed = withTimeout(5_000) {
                viewModel.uiState.first { state ->
                    state.query == "runtime" && !state.isSearching && state.status == SearchStatus.Success
                }
            }
            assertEquals(listOf("fixture-song"), completed.songs.map(SongItem::id))

            viewModel.clearQuery()
            val cleared = withTimeout(5_000) {
                viewModel.uiState.first { state -> state.query.isBlank() && state.status == SearchStatus.Idle }
            }
            assertTrue(cleared.songs.isEmpty())
        } finally {
            dispose(viewModel, database)
        }
    }

    @Test
    fun offlineSearchShowsNetworkErrorWithoutCallingProvider() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val internal = Room.inMemoryDatabaseBuilder(context, InternalDatabase::class.java).build()
        val database = MusicDatabase(internal)
        val provider = ScriptedProvider(
            initial = Result.success(SearchResult(items = listOf(song("should-not-load")))),
        )
        val viewModel = SearchViewModel(
            context = context,
            database = database,
            searchProvider = provider,
            networkStatus = object : SearchNetworkStatus { override fun isOnline() = false },
            searchTiming = object : SearchTiming { override val debounceMillis = 0L },
        )

        try {
            viewModel.onFilterSelected(SearchFilterTab.Songs)
            viewModel.onQueryChanged("offline")

            val completed = withTimeout(5_000) {
                viewModel.uiState.first { state ->
                    state.query == "offline" && !state.isSearching && state.status == SearchStatus.NetworkError
                }
            }
            assertTrue(completed.songs.isEmpty())
            assertFalse(provider.wasSearchCalled)
        } finally {
            dispose(viewModel, database)
        }
    }

    @Test
    fun providerFailurePublishesNetworkErrorInsteadOfAnEmptySuccess() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val internal = Room.inMemoryDatabaseBuilder(context, InternalDatabase::class.java).build()
        val database = MusicDatabase(internal)
        val provider = ScriptedProvider(
            initial = Result.failure(IOException("fixture provider unavailable")),
        )
        val viewModel = SearchViewModel(
            context = context,
            database = database,
            searchProvider = provider,
            networkStatus = object : SearchNetworkStatus { override fun isOnline() = true },
            searchTiming = object : SearchTiming { override val debounceMillis = 0L },
        )

        try {
            viewModel.onFilterSelected(SearchFilterTab.Songs)
            viewModel.onQueryChanged("provider-failure")

            val completed = withTimeout(5_000) {
                viewModel.uiState.first { state ->
                    state.query == "provider-failure" && !state.isSearching && state.status == SearchStatus.NetworkError
                }
            }
            assertTrue(completed.error.orEmpty().contains("fixture provider unavailable"))
            assertTrue(provider.wasSearchCalled)
        } finally {
            dispose(viewModel, database)
        }
    }

    @Test
    fun providerFailureDuringRefreshKeepsKnownGoodResultsVisible() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val internal = Room.inMemoryDatabaseBuilder(context, InternalDatabase::class.java).build()
        val database = MusicDatabase(internal)
        val provider = ScriptedProvider(
            initial = Result.success(SearchResult(items = listOf(song("cached-song")))),
        )
        val viewModel = SearchViewModel(
            context = context,
            database = database,
            searchProvider = provider,
            networkStatus = object : SearchNetworkStatus { override fun isOnline() = true },
            searchTiming = object : SearchTiming { override val debounceMillis = 0L },
        )

        try {
            viewModel.onFilterSelected(SearchFilterTab.Songs)
            viewModel.onQueryChanged("cached-provider-failure")
            withTimeout(5_000) {
                viewModel.uiState.first { state ->
                    state.query == "cached-provider-failure" && state.status == SearchStatus.Success &&
                        state.songs.map(SongItem::id) == listOf("cached-song")
                }
            }

            provider.initialResult = Result.failure(IOException("fixture refresh failure"))
            viewModel.retrySearch()
            val refreshed = withTimeout(5_000) {
                viewModel.uiState.first { state ->
                    state.query == "cached-provider-failure" && !state.isSearching &&
                        state.status == SearchStatus.CachedResultsShown
                }
            }
            assertEquals(listOf("cached-song"), refreshed.songs.map(SongItem::id))
            assertTrue(refreshed.error == null)
        } finally {
            dispose(viewModel, database)
        }
    }

    @Test
    fun paginationAppendsOnlyNewResultsAndConsumesContinuation() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val internal = Room.inMemoryDatabaseBuilder(context, InternalDatabase::class.java).build()
        val database = MusicDatabase(internal)
        val provider = ScriptedProvider(
            initial = Result.success(
                SearchResult(
                    items = listOf(song("first")),
                    continuation = "page-2",
                ),
            ),
            continuation = Result.success(
                SearchResult(items = listOf(song("first"), song("second"))),
            ),
        )
        val viewModel = SearchViewModel(
            context = context,
            database = database,
            searchProvider = provider,
            networkStatus = object : SearchNetworkStatus { override fun isOnline() = true },
            searchTiming = object : SearchTiming { override val debounceMillis = 0L },
        )

        try {
            viewModel.onFilterSelected(SearchFilterTab.Songs)
            viewModel.onQueryChanged("paged")
            withTimeout(5_000) {
                viewModel.uiState.first { state ->
                    state.query == "paged" && state.status == SearchStatus.Success && state.continuation == "page-2"
                }
            }

            viewModel.loadMore()
            val loaded = withTimeout(5_000) {
                viewModel.uiState.first { state ->
                    !state.isLoadingMore && state.continuation == null &&
                        state.songs.map(SongItem::id) == listOf("first", "second")
                }
            }
            assertEquals(listOf("first", "second"), loaded.songs.map(SongItem::id))
            assertTrue(provider.wasContinuationCalled)
        } finally {
            dispose(viewModel, database)
        }
    }

    private object DuplicateSongProvider : SearchProvider {
        private val song = song("fixture-song")

        override suspend fun search(query: String, filter: YouTube.SearchFilter): Result<SearchResult> =
            Result.success(SearchResult(items = listOf(song, song)))

        override suspend fun searchSummary(query: String): Result<SearchSummaryPage> =
            Result.success(SearchSummaryPage(emptyList()))

        override suspend fun searchContinuation(continuation: String): Result<SearchResult> =
            Result.success(SearchResult(emptyList()))
    }

    private class ScriptedProvider(
        initial: Result<SearchResult>,
        private val continuation: Result<SearchResult> = Result.success(SearchResult(emptyList())),
    ) : SearchProvider {
        var initialResult = initial
        var wasSearchCalled = false
            private set
        var wasContinuationCalled = false
            private set

        override suspend fun search(query: String, filter: YouTube.SearchFilter): Result<SearchResult> {
            wasSearchCalled = true
            return initialResult
        }

        override suspend fun searchSummary(query: String): Result<SearchSummaryPage> =
            Result.success(SearchSummaryPage(emptyList()))

        override suspend fun searchContinuation(continuation: String): Result<SearchResult> {
            wasContinuationCalled = true
            return this.continuation
        }
    }

    private companion object {
        fun song(id: String) = SongItem(
            id = id,
            title = "Fixture $id",
            artists = listOf(Artist(name = "Fixture Artist", id = "fixture-artist")),
            thumbnail = "https://example.invalid/fixture.jpg",
        )
    }

    private suspend fun dispose(viewModel: SearchViewModel, database: MusicDatabase) {
        // A successful search records its history on Dispatchers.IO. Drain Room before
        // destroying the ViewModel and its in-memory database, then drain once more after
        // cancellation so an already-running Room transaction cannot outlive this test.
        database.awaitIdle()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            ViewModelStore().apply {
                put("search-test", viewModel)
                clear()
            }
        }
        database.awaitIdle()
        database.close()
    }
}
