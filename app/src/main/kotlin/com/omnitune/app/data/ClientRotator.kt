package com.omnitune.app.data

import com.omnitune.app.constants.PlayerStreamClient
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClientRotator @Inject constructor() {
    private val failureCounts = ConcurrentHashMap<String, Int>()

    private val clients = listOf(
        PlayerStreamClient.ANDROID_VR,
        PlayerStreamClient.IOS,
        PlayerStreamClient.WEB_REMIX,
        PlayerStreamClient.ANDROID_MUSIC
    )

    fun getNextClient(videoId: String): PlayerStreamClient {
        val failures = failureCounts[videoId] ?: 0
        val index = failures % clients.size
        return clients[index]
    }

    fun reportFailure(videoId: String) {
        val count = failureCounts.getOrDefault(videoId, 0)
        failureCounts[videoId] = count + 1
    }

    fun reportSuccess(videoId: String) {
        failureCounts.remove(videoId)
    }
}
