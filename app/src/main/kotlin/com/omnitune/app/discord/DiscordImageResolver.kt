/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 *
 * Based on Velune Discord integration
 */

package com.omnitune.app.discord

import com.omnitune.kizzy.repository.KizzyRepository
import com.omnitune.kizzy.rpc.RpcImage
import javax.inject.Inject
import javax.inject.Singleton

data class ResolvedDiscordImages(
    val thumbnailUrl: String? = null,
    val artistUrl: String? = null,
    val thumbnailId: String? = null,
    val artistId: String? = null,
)

@Singleton
class DiscordImageResolver @Inject constructor(
    private val repository: KizzyRepository,
) {
    private var cachedSongId: String = ""
    private var cachedImages: ResolvedDiscordImages? = null

    suspend fun resolveImagesForSong(
        songId: String,
        thumbnailUrl: String?,
        artistUrl: String?,
    ): ResolvedDiscordImages {
        if (songId == cachedSongId) {
            return cachedImages ?: ResolvedDiscordImages(null, null, null, null)
        }

        val thumbnailId = thumbnailUrl?.let { resolveImage(it) }
        val artistImageId = artistUrl?.let { resolveImage(it) }

        val result = ResolvedDiscordImages(
            thumbnailUrl = thumbnailUrl,
            artistUrl = artistUrl,
            thumbnailId = thumbnailId,
            artistId = artistImageId,
        )

        cachedSongId = songId
        cachedImages = result
        return result
    }

    suspend fun resolveImage(url: String): String? {
        val cached = repository.peekCache(url)
        if (cached != null) return cached

        return repository.getImage(url)
    }

    suspend fun prefetch(songId: String, thumbnailUrl: String?) {
        if (thumbnailUrl != null) {
            repository.prefetchImage(thumbnailUrl)
        }
    }

    fun buildRpcImage(imageType: ImageSourceType, images: ResolvedDiscordImages): RpcImage? {
        return when (imageType) {
            ImageSourceType.THUMBNAIL -> {
                images.thumbnailId?.let { RpcImage.DiscordImage(it) }
                    ?: images.thumbnailUrl?.let { RpcImage.ExternalImage(it) }
            }
            ImageSourceType.ARTIST -> {
                images.artistId?.let { RpcImage.DiscordImage(it) }
                    ?: images.artistUrl?.let { RpcImage.ExternalImage(it) }
            }
            ImageSourceType.APP_ICON -> RpcImage.ExternalImage(
                "https://cdn.discordapp.com/attachments/1165706613961789445/1165706613961789445/omnitune.png"
            )
            ImageSourceType.ALBUM -> {
                images.thumbnailId?.let { RpcImage.DiscordImage(it) }
                    ?: images.thumbnailUrl?.let { RpcImage.ExternalImage(it) }
            }
            ImageSourceType.CUSTOM -> null
            ImageSourceType.NONE -> null
        }
    }

    fun clearCache() {
        cachedSongId = ""
        cachedImages = null
    }
}

fun String.isValidHttpUrl(): Boolean =
    startsWith("http://") || startsWith("https://")

fun String.isResolvedId(): Boolean =
    startsWith("mp:") || startsWith("external/") || startsWith("attachments/")
