/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 *
 * Based on Kizzy (c) yzziK(Vaibhav) 2022
 */

package com.omnitune.kizzy.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class ApiService(
    private val client: HttpClient,
) {
    private companion object {
        const val BASE_URL = "https://metrolist-discord-rpc-api.vercel.app"
    }

    suspend fun getImage(url: String): Result<ImageResult> = runCatching {
        val response = client.get("$BASE_URL/api/image") {
            parameter("url", url)
            parameter("size", 256)
        }
        response.body<ImageResult>()
    }
}

@kotlinx.serialization.Serializable
data class ImageResult(
    val id: String? = null,
    val url: String? = null,
)
