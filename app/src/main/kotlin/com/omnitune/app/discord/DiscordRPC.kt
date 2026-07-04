/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.discord

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.omnitune.app.constants.DiscordActivityDetailsKey
import com.omnitune.app.constants.DiscordActivityNameKey
import com.omnitune.app.constants.DiscordActivityStateKey
import com.omnitune.app.constants.DiscordActivityTypeKey
import com.omnitune.app.constants.DiscordLargeImageCustomUrlKey
import com.omnitune.app.constants.DiscordLargeImageTypeKey
import com.omnitune.app.constants.DiscordLargeTextCustomKey
import com.omnitune.app.constants.DiscordLargeTextSourceKey
import com.omnitune.app.constants.DiscordSmallImageTypeKey
import com.omnitune.app.constants.DiscordSmallImageCustomUrlKey
import com.omnitune.app.constants.DiscordPresenceStatusKey
import com.omnitune.app.constants.DiscordActivityButton1EnabledKey
import com.omnitune.app.constants.DiscordActivityButton1LabelKey
import com.omnitune.app.constants.DiscordActivityButton1UrlSourceKey
import com.omnitune.app.constants.DiscordActivityButton1CustomUrlKey
import com.omnitune.app.constants.DiscordActivityButton2EnabledKey
import com.omnitune.app.constants.DiscordActivityButton2LabelKey
import com.omnitune.app.constants.DiscordActivityButton2UrlSourceKey
import com.omnitune.app.constants.DiscordActivityButton2CustomUrlKey
import com.omnitune.app.constants.DiscordShowWhenPausedKey
import com.omnitune.kizzy.rpc.ActivityType
import com.omnitune.kizzy.rpc.KizzyRPC
import com.omnitune.kizzy.rpc.RpcImage
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private const val APPLICATION_ID = "1165706613961789445"

enum class ImageSourceType(val value: String) {
    THUMBNAIL("thumbnail"),
    ARTIST("artist"),
    ALBUM("album"),
    APP_ICON("appicon"),
    CUSTOM("custom"),
    NONE("none"),
}

@Singleton
class DiscordRPC @Inject constructor(
    private val kizzy: KizzyRPC,
    private val dataStore: DataStore<Preferences>,
) {

    private var currentSongTitle: String = ""
    private var currentVideoId: String = ""
    private var currentArtist: String = ""
    private var currentAlbum: String = ""
    private var currentThumbnail: String = ""
    private var currentArtistImage: String = ""
    private var currentPosition: Long = 0
    private var currentDuration: Long = 0
    private var isPaused: Boolean = false
    private var enabled: Boolean = true

    suspend fun connect(token: String) {
        kizzy.connect(token)
    }

    suspend fun updateSong(
        title: String,
        videoId: String = "",
        artist: String = "",
        album: String = "",
        thumbnail: String = "",
        artistImage: String = "",
        position: Long = 0,
        duration: Long = 0,
        paused: Boolean = false,
    ) {
        currentSongTitle = title
        currentVideoId = videoId
        currentArtist = artist
        currentAlbum = album
        currentThumbnail = thumbnail
        currentArtistImage = artistImage
        currentPosition = position
        currentDuration = duration
        isPaused = paused

        if (!enabled || title.isBlank()) return

        val prefs = dataStore.data.first()
        val showWhenPaused = prefs[DiscordShowWhenPausedKey] ?: true

        if (paused && !showWhenPaused) {
            kizzy.stopActivity()
            return
        }

        val activityName = prefs[DiscordActivityNameKey] ?: "OmniTune"
        val activityDetails = prefs[DiscordActivityDetailsKey] ?: title
        val activityState = prefs[DiscordActivityStateKey] ?: artist
        val activityTypeStr = prefs[DiscordActivityTypeKey] ?: "LISTENING"
        val statusStr = prefs[DiscordPresenceStatusKey] ?: "ONLINE"

        val resolvedDetails = resolveTemplate(activityDetails, title, artist, album)
        val resolvedState = resolveTemplate(activityState, title, artist, album)

        val activityType = try { ActivityType.valueOf(activityTypeStr) } catch (_: Exception) { ActivityType.LISTENING }
        val status = when (statusStr) {
            "ONLINE" -> "online"
            "IDLE" -> "idle"
            "DND" -> "dnd"
            "INVISIBLE" -> "invisible"
            else -> "online"
        }

        // Resolve images
        val largeImage = resolveLargeImage(prefs)
        val smallImage = resolveSmallImage(prefs)
        val largeText = resolveLargeText(prefs, title, artist, album)
        val smallText = if (paused) "Paused" else "Playing"

        // Calculate timestamps
        val startTime: Long? = if (!paused && duration > 0) {
            (System.currentTimeMillis() / 1000) - (position / 1000)
        } else null

        // Resolve buttons
        val buttons = resolveButtons(prefs)

        kizzy.buildActivity(
            name = resolveTemplate(activityName, title, artist, album),
            type = activityType,
            state = resolvedState.take(128),
            details = resolvedDetails.take(128),
            largeImage = largeImage,
            largeText = largeText?.take(128),
            smallImage = smallImage,
            smallText = smallText.take(128),
            startTimestamp = startTime,
            buttons = buttons,
            status = status,
        )
    }

    suspend fun stop() {
        kizzy.stopActivity()
    }

    suspend fun close() {
        kizzy.closeRPC()
    }

    fun isRunning(): Boolean = kizzy.isConnected()

    private fun resolveTemplate(template: String, title: String, artist: String, album: String): String =
        template
            .replace("{song}", title)
            .replace("{title}", title)
            .replace("{artist}", artist)
            .replace("{album}", album)

    private suspend fun resolveLargeImage(prefs: Preferences): RpcImage? {
        val type = prefs[DiscordLargeImageTypeKey] ?: "thumbnail"
        return when (type) {
            "thumbnail" -> if (currentThumbnail.isNotBlank()) RpcImage.ExternalImage(currentThumbnail) else null
            "artist" -> if (currentArtistImage.isNotBlank()) RpcImage.ExternalImage(currentArtistImage) else null
            "appicon" -> RpcImage.ExternalImage("https://cdn.discordapp.com/attachments/1165706613961789445/1165706613961789445/omnitune.png")
            "custom" -> {
                val url = prefs[DiscordLargeImageCustomUrlKey]
                url?.takeIf { it.isNotBlank() }?.let { RpcImage.ExternalImage(it) }
            }
            else -> null
        }
    }

    private suspend fun resolveSmallImage(prefs: Preferences): RpcImage? {
        val type = prefs[DiscordSmallImageTypeKey] ?: "none"
        return when (type) {
            "thumbnail" -> if (currentThumbnail.isNotBlank()) RpcImage.ExternalImage(currentThumbnail) else null
            "artist" -> if (currentArtistImage.isNotBlank()) RpcImage.ExternalImage(currentArtistImage) else null
            "appicon" -> RpcImage.ExternalImage("https://cdn.discordapp.com/attachments/1165706613961789445/1165706613961789445/omnitune.png")
            "custom" -> {
                val url = prefs[DiscordSmallImageCustomUrlKey]
                url?.takeIf { it.isNotBlank() }?.let { RpcImage.ExternalImage(it) }
            }
            else -> null
        }
    }

    private fun resolveLargeText(prefs: Preferences, title: String, artist: String, album: String): String? {
        val source = prefs[DiscordLargeTextSourceKey] ?: "song"
        val custom = prefs[DiscordLargeTextCustomKey]
        return when (source) {
            "song" -> title
            "artist" -> artist
            "album" -> album
            "app" -> "OmniTune"
            "custom" -> custom?.takeIf { it.isNotBlank() } ?: title
            else -> title
        }
    }

    private suspend fun resolveButtons(prefs: Preferences): List<Pair<String, String>>? {
        val buttons = mutableListOf<Pair<String, String>>()

        val btn1Enabled = prefs[DiscordActivityButton1EnabledKey] ?: false
        val btn2Enabled = prefs[DiscordActivityButton2EnabledKey] ?: false

        if (btn1Enabled) {
            val label = prefs[DiscordActivityButton1LabelKey] ?: "Listen on OmniTune"
            val url = resolveButtonUrl(prefs, DiscordActivityButton1UrlSourceKey, DiscordActivityButton1CustomUrlKey)
            if (url != null) buttons.add(label.take(32) to url)
        }

        if (btn2Enabled) {
            val label = prefs[DiscordActivityButton2LabelKey] ?: "View on YouTube"
            val url = resolveButtonUrl(prefs, DiscordActivityButton2UrlSourceKey, DiscordActivityButton2CustomUrlKey)
            if (url != null) buttons.add(label.take(32) to url)
        }

        return buttons.takeIf { it.isNotEmpty() }
    }

    private fun resolveButtonUrl(
        prefs: Preferences,
        urlSourceKey: Preferences.Key<String>,
        customUrlKey: Preferences.Key<String>,
    ): String? {
        val source = prefs[urlSourceKey] ?: "custom"
        return when (source) {
            "song" -> {
                if (currentVideoId.isNotBlank()) {
                    "https://music.youtube.com/watch?v=$currentVideoId"
                } else if (currentSongTitle.isNotBlank()) {
                    "https://music.youtube.com/search?q=${android.net.Uri.encode(currentSongTitle)}"
                } else null
            }
            "artist" -> {
                if (currentArtist.isNotBlank()) {
                    "https://music.youtube.com/search?q=${android.net.Uri.encode(currentArtist)}"
                } else null
            }
            "album" -> {
                if (currentAlbum.isNotBlank()) {
                    "https://music.youtube.com/search?q=${android.net.Uri.encode(currentAlbum)}"
                } else null
            }
            "custom" -> prefs[customUrlKey]?.takeIf { it.isNotBlank() }
            else -> null
        }
    }
}
