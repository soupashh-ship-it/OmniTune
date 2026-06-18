package com.omnitune.app.data

import com.omnitune.app.models.StreamInfo
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamCache @Inject constructor() {
    private val cache = ConcurrentHashMap<String, StreamInfo>()

    fun get(songId: String): StreamInfo? = cache[songId]

    fun put(songId: String, streamInfo: StreamInfo) {
        cache[songId] = streamInfo
    }

    fun clear() {
        cache.clear()
    }
}
