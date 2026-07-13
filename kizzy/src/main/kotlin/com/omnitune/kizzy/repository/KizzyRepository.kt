/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 *
 * Based on Kizzy (c) yzziK(Vaibhav) 2022
 */

package com.omnitune.kizzy.repository

import com.omnitune.kizzy.remote.ApiService
import com.omnitune.kizzy.rpc.RpcImage

class KizzyRepository(
    private val apiService: ApiService,
) {
    private val imageCache = object : LinkedHashMap<String, String>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>): Boolean =
            size > 64
    }

    fun putToCache(key: String, value: String) {
        synchronized(imageCache) { imageCache[key] = value }
    }

    fun peekCache(key: String): String? = synchronized(imageCache) { imageCache[key] }

    fun removeCache(key: String) {
        synchronized(imageCache) { imageCache.remove(key) }
    }

    suspend fun getImage(url: String): String? {
        peekCache(url)?.let { return it }

        val result = apiService.getImage(url)
        val imageId = result.getOrNull()?.id ?: return null

        putToCache(url, imageId)
        return imageId
    }

    suspend fun prefetchImage(url: String) {
        getImage(url)
    }
}

fun String.toRpcImage(): RpcImage = when {
    startsWith("mp:") || startsWith("external/") || startsWith("attachments/") ->
        RpcImage.DiscordImage(this)
    startsWith("http://") || startsWith("https://") ->
        RpcImage.ExternalImage(this)
    else -> RpcImage.Unresolved(this)
}
