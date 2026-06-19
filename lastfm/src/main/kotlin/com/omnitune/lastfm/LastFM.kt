package com.omnitune.lastfm

import com.omnitune.lastfm.models.Session
import com.omnitune.lastfm.models.TokenResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.security.MessageDigest

object LastFM {

    private var apiKey: String = ""
    private var secret: String = ""
    private var session: Session? = null
    var sessionKey: String?
        get() = session?.key
        set(value) { if (value != null) session = Session(key = value) }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val apiUrl = "https://ws.audioscrobbler.com/2.0"

    fun initialize(apiKey: String, secret: String) {
        this.apiKey = apiKey
        this.secret = secret
    }

    suspend fun getToken(): String? {
        val response = client.get(apiUrl) {
            parameter("method", "auth.getToken")
            parameter("api_key", apiKey)
            parameter("format", "json")
        }
        val body = response.body<TokenResponse>()
        return body.token
    }

    suspend fun getSession(token: String): Session? {
        val sig = apiSig(mapOf(
            "api_key" to apiKey,
            "method" to "auth.getSession",
            "token" to token
        ))
        val response = client.get(apiUrl) {
            parameter("method", "auth.getSession")
            parameter("api_key", apiKey)
            parameter("token", token)
            parameter("api_sig", sig)
            parameter("format", "json")
        }
        val body = response.body<TokenResponse>()
        session = body.session
        return body.session
    }

    suspend fun getMobileSession(username: String, password: String): Session? {
        val sig = apiSig(mapOf(
            "api_key" to apiKey,
            "method" to "auth.getMobileSession",
            "password" to password,
            "username" to username
        ))
        val response = client.post(apiUrl) {
            parameter("method", "auth.getMobileSession")
            parameter("api_key", apiKey)
            parameter("password", password)
            parameter("username", username)
            parameter("api_sig", sig)
            parameter("format", "json")
        }
        val body = response.body<TokenResponse>()
        session = body.session
        return body.session
    }

    suspend fun updateNowPlaying(artist: String, track: String, album: String? = null) {
        val sk = session?.key ?: return
        val params = buildMap<String, String> {
            put("method", "track.updateNowPlaying")
            put("api_key", apiKey)
            put("sk", sk)
            put("artist", artist)
            put("track", track)
            album?.let { put("album", it) }
        }
        val sig = apiSig(params)
        client.post(apiUrl) {
            params.forEach { (k, v) -> parameter(k, v) }
            parameter("api_sig", sig)
            parameter("format", "json")
        }
    }

    suspend fun scrobble(artist: String, track: String, timestamp: Long, album: String? = null) {
        val sk = session?.key ?: return
        val params = buildMap<String, String> {
            put("method", "track.scrobble")
            put("api_key", apiKey)
            put("sk", sk)
            put("artist", artist)
            put("track", track)
            put("timestamp", timestamp.toString())
            album?.let { put("album", it) }
        }
        val sig = apiSig(params)
        client.post(apiUrl) {
            params.forEach { (k, v) -> parameter(k, v) }
            parameter("api_sig", sig)
            parameter("format", "json")
        }
    }

    private fun apiSig(params: Map<String, String>): String {
        val sorted = params.entries.sortedBy { it.key }
        val str = sorted.joinToString("") { "${it.key}${it.value}" } + secret
        return md5(str)
    }

    private fun md5(input: String): String {
        val digest = MessageDigest.getInstance("MD5")
        return digest.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
