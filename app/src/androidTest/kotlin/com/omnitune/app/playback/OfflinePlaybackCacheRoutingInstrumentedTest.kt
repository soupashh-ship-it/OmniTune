package com.omnitune.app.playback

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.ByteArrayDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.IOException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
@androidx.media3.common.util.UnstableApi
class OfflinePlaybackCacheRoutingInstrumentedTest {

    @Test
    fun completedBytesSurviveCacheReopenAndAreGoneAfterRemoval() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cacheDirectory = File(context.cacheDir, "offline-cache-routing-${UUID.randomUUID()}")
        val cacheKey = "offline-track-key"
        val payload = ByteArray(8_192) { it.toByte() }
        val databaseProvider = StandaloneDatabaseProvider(context)
        var initialCache: SimpleCache? = null
        var reopenedCache: SimpleCache? = null

        try {
            initialCache = SimpleCache(cacheDirectory, NoOpCacheEvictor(), databaseProvider)
            val writer = CacheDataSource.Factory()
                .setCache(initialCache)
                .setUpstreamDataSourceFactory(DataSource.Factory { ByteArrayDataSource(payload) })
                .createDataSource()
            try {
                writer.open(
                    DataSpec.Builder()
                        .setUri(Uri.parse("https://offline.test/track"))
                        .setKey(cacheKey)
                        .build(),
                )
                val buffer = ByteArray(1_024)
                while (writer.read(buffer, 0, buffer.size) != C.RESULT_END_OF_INPUT) Unit
            } finally {
                writer.close()
            }

            assertTrue(
                OfflinePlaybackCacheRouting.isFullyCached(
                    isCompleted = true,
                    expectedContentLength = payload.size.toLong(),
                    cachedPrefixLength = initialCache.getCachedLength(cacheKey, 0, payload.size.toLong()),
                ),
            )

            initialCache.release()
            initialCache = null
            reopenedCache = SimpleCache(cacheDirectory, NoOpCacheEvictor(), databaseProvider)
            assertTrue(
                OfflinePlaybackCacheRouting.isFullyCached(
                    isCompleted = true,
                    expectedContentLength = payload.size.toLong(),
                    cachedPrefixLength = reopenedCache.getCachedLength(cacheKey, 0, payload.size.toLong()),
                ),
            )

            reopenedCache.removeResource(cacheKey)
            assertFalse(
                OfflinePlaybackCacheRouting.isFullyCached(
                    isCompleted = true,
                    expectedContentLength = payload.size.toLong(),
                    cachedPrefixLength = reopenedCache.getCachedLength(cacheKey, 0, payload.size.toLong()),
                ),
            )
        } finally {
            initialCache?.release()
            reopenedCache?.release()
            cacheDirectory.deleteRecursively()
        }
    }

    @Test
    fun completedRouteFailsWithoutStreamFallbackWhenCacheDisappearsBeforeIndexUpdate() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cacheDirectory = File(context.cacheDir, "offline-cache-route-${UUID.randomUUID()}")
        val cacheKey = "completed-route-key"
        val payload = ByteArray(4_096) { (it % 127).toByte() }
        val cache = SimpleCache(cacheDirectory, NoOpCacheEvictor(), StandaloneDatabaseProvider(context))
        val streamFactory = CountingByteArrayDataSourceFactory(payload)

        try {
            val writer = CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(DataSource.Factory { ByteArrayDataSource(payload) })
                .createDataSource()
            writeFully(writer, cacheKey)

            val completedOnlyFactory = CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(null)
                .setCacheWriteDataSinkFactory(null)
                .setFlags(CacheDataSource.FLAG_BLOCK_ON_CACHE)
            val routingFactory = OfflineCacheRoutingDataSourceFactory(
                completedDownloadSourceFactory = completedOnlyFactory,
                streamSourceFactory = streamFactory,
                isCompletedDownloadCacheKey = { it == cacheKey },
            )

            val cachedSource = routingFactory.createDataSource()
            val cachedRead = readFully(cachedSource, cacheKey)
            assertEquals(payload.toList(), cachedRead.toList())
            assertEquals(0, streamFactory.createCount.get())

            cache.removeResource(cacheKey)
            val missingSource = routingFactory.createDataSource()
            try {
                missingSource.open(dataSpec(cacheKey))
                fail("A completed route with removed bytes must fail rather than use the stream source")
            } catch (_: IOException) {
                // Expected: the completed-download cache has no upstream factory.
            } finally {
                missingSource.close()
            }
            assertEquals(0, streamFactory.createCount.get())
        } finally {
            cache.release()
            cacheDirectory.deleteRecursively()
        }
    }

    @Test
    fun removedDownloadIndexRoutesToStreamAndCannotRemainAvailableOffline() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cacheDirectory = File(context.cacheDir, "offline-cache-removal-${UUID.randomUUID()}")
        val cacheKey = "removed-download-key"
        val payload = ByteArray(4_096) { (it % 127).toByte() }
        val cache = SimpleCache(cacheDirectory, NoOpCacheEvictor(), StandaloneDatabaseProvider(context))
        val offlineStreamFactory = FailingDataSourceFactory()
        var completedIndexEntryExists = true

        try {
            val writer = CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(DataSource.Factory { ByteArrayDataSource(payload) })
                .createDataSource()
            writeFully(writer, cacheKey)

            val completedOnlyFactory = CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(null)
                .setCacheWriteDataSinkFactory(null)
                .setFlags(CacheDataSource.FLAG_BLOCK_ON_CACHE)
            val routingFactory = OfflineCacheRoutingDataSourceFactory(
                completedDownloadSourceFactory = completedOnlyFactory,
                streamSourceFactory = offlineStreamFactory,
                isCompletedDownloadCacheKey = { completedIndexEntryExists && it == cacheKey },
            )

            assertEquals(payload.toList(), readFully(routingFactory.createDataSource(), cacheKey).toList())
            assertEquals(0, offlineStreamFactory.createCount.get())

            // This mirrors DownloadManager removal: the cache resource and completed index record
            // are both gone. With network unavailable, the item must not still be playable.
            cache.removeResource(cacheKey)
            completedIndexEntryExists = false
            val sourceAfterRemoval = routingFactory.createDataSource()
            try {
                sourceAfterRemoval.open(dataSpec(cacheKey))
                fail("A removed download must not remain available from the download cache")
            } catch (_: IOException) {
                // Expected: routing now reaches the stream-only source, which represents offline.
            } finally {
                sourceAfterRemoval.close()
            }
            assertEquals(1, offlineStreamFactory.createCount.get())
        } finally {
            cache.release()
            cacheDirectory.deleteRecursively()
        }
    }

    private fun writeFully(dataSource: DataSource, cacheKey: String) {
        try {
            dataSource.open(dataSpec(cacheKey))
            val buffer = ByteArray(1_024)
            while (dataSource.read(buffer, 0, buffer.size) != C.RESULT_END_OF_INPUT) Unit
        } finally {
            dataSource.close()
        }
    }

    private fun readFully(dataSource: DataSource, cacheKey: String): ByteArray {
        val output = ArrayList<Byte>()
        try {
            dataSource.open(dataSpec(cacheKey))
            val buffer = ByteArray(1_024)
            while (true) {
                val read = dataSource.read(buffer, 0, buffer.size)
                if (read == C.RESULT_END_OF_INPUT) return output.toByteArray()
                repeat(read) { index -> output += buffer[index] }
            }
        } finally {
            dataSource.close()
        }
    }

    private fun dataSpec(cacheKey: String) = DataSpec.Builder()
        .setUri(Uri.parse("https://offline.test/$cacheKey"))
        .setKey(cacheKey)
        .build()

    private class CountingByteArrayDataSourceFactory(
        private val payload: ByteArray,
    ) : DataSource.Factory {
        val createCount = AtomicInteger(0)

        override fun createDataSource(): DataSource {
            createCount.incrementAndGet()
            return ByteArrayDataSource(payload)
        }
    }

    private class FailingDataSourceFactory : DataSource.Factory {
        val createCount = AtomicInteger(0)

        override fun createDataSource(): DataSource {
            createCount.incrementAndGet()
            return object : DataSource {
                override fun addTransferListener(transferListener: androidx.media3.datasource.TransferListener) = Unit

                override fun open(dataSpec: DataSpec): Long = throw IOException("Network unavailable in fixture")

                override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
                    throw IOException("Network unavailable in fixture")

                override fun getUri(): Uri? = null

                override fun getResponseHeaders(): Map<String, List<String>> = emptyMap()

                override fun close() = Unit
            }
        }
    }
}
