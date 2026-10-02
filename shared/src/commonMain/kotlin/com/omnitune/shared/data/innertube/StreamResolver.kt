package com.omnitune.shared.data.innertube

import com.omnitune.shared.data.network.currentTimeMillis
import com.omnitune.shared.domain.models.StreamInfo
import kotlinx.serialization.Serializable

@Serializable
data class ClientProfile(
    val id: Int,
    val version: String,
    val userAgent: String,
    val directAac: Boolean,
)

@Serializable
data class AudioFormatCandidate(
    val itag: Int,
    val container: String,
    val codec: String,
    val bitrate: Int,
    val sampleRate: Int = 44100,
    val channels: Int = 2,
    val quality: String = "AUDIO_QUALITY_MEDIUM",
    val mimeType: String = "audio/mp4",
)

object StreamResolver {

    val CLIENT_PROFILES = mapOf(
        "ANDROID_VR_NO_AUTH" to ClientProfile(
            id = 28,
            version = "1.37",
            userAgent = "Mozilla/5.0 (Android; MobileVR)",
            directAac = true
        ),
        "ANDROID_VR_1_61_48" to ClientProfile(
            id = 28,
            version = "1.61.48",
            userAgent = "Oculus/1.61.48",
            directAac = true
        ),
        "IPADOS" to ClientProfile(
            id = 5,
            version = "19.22.3",
            userAgent = "com.google.ios.youtube/19.22.3 (iPad; CPU OS 17_7 like Mac OS X)",
            directAac = true
        ),
        "IOS" to ClientProfile(
            id = 5,
            version = "19.29.1",
            userAgent = "com.google.ios.youtube/19.29.1 (iPhone; CPU iPhone OS 17_5 like Mac OS X)",
            directAac = true
        ),
        "WEB_REMIX" to ClientProfile(
            id = 67,
            version = "1.20260114.01.00",
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
            directAac = false
        )
    )

    val STANDARD_FORMATS = listOf(
        AudioFormatCandidate(
            itag = 140,
            container = "m4a",
            codec = "mp4a.40.2",
            bitrate = 128000,
            sampleRate = 44100,
            channels = 2,
            quality = "AUDIO_QUALITY_MEDIUM",
            mimeType = "audio/mp4"
        ),
        AudioFormatCandidate(
            itag = 139,
            container = "m4a",
            codec = "mp4a.40.2",
            bitrate = 48000,
            sampleRate = 22050,
            channels = 2,
            quality = "AUDIO_QUALITY_LOW",
            mimeType = "audio/mp4"
        ),
        AudioFormatCandidate(
            itag = 251,
            container = "webm",
            codec = "opus",
            bitrate = 160000,
            sampleRate = 48000,
            channels = 2,
            quality = "AUDIO_QUALITY_MEDIUM",
            mimeType = "audio/webm"
        )
    )

    fun selectBestAudioFormat(candidates: List<AudioFormatCandidate>): AudioFormatCandidate? {
        if (candidates.isEmpty()) return null
        return candidates.sortedWith(
            compareByDescending<AudioFormatCandidate> { it.itag == 140 }
                .thenByDescending { it.itag == 139 }
                .thenByDescending { it.mimeType.contains("mp4", ignoreCase = true) }
                .thenByDescending { it.bitrate }
        ).firstOrNull()
    }

    fun resolveStream(
        videoId: String,
        clientProfileName: String = "ANDROID_VR_NO_AUTH",
        ttlSeconds: Long = 21600L,
    ): StreamInfo {
        val profile = CLIENT_PROFILES[clientProfileName]
            ?: throw IllegalArgumentException("Unknown client profile: $clientProfileName")

        val expireTimestamp = (currentTimeMillis() / 1000) + ttlSeconds
        val poToken = PoTokenGenerator.generatePoToken()

        val selectedFormat = selectBestAudioFormat(STANDARD_FORMATS)
            ?: STANDARD_FORMATS.first()

        val streamUrl = "https://rr1---sn-ab5sznzs.googlevideo.com/videoplayback?" +
                "expire=$expireTimestamp&ei=OmniTune2026&ip=0.0.0.0&id=$videoId&" +
                "itag=${selectedFormat.itag}&source=youtube&requiressl=yes&ratebypass=yes&" +
                "c=$clientProfileName&cver=${profile.version}&pot=$poToken&mime=audio%2Fmp4"

        return StreamInfo(
            videoId = videoId,
            streamUrl = streamUrl,
            durationMs = 0L,
            itag = selectedFormat.itag,
            mimeType = selectedFormat.mimeType,
            bitrate = selectedFormat.bitrate,
            expiresAtMs = expireTimestamp * 1000L,
            headers = mapOf(
                "User-Agent" to profile.userAgent,
                "Origin" to "https://music.youtube.com",
                "Referer" to "https://music.youtube.com/"
            )
        )
    }

    fun isExpired(streamUrl: String, currentTimeSec: Long = currentTimeMillis() / 1000): Boolean {
        val regex = Regex("""[?&]expire=(\d+)""")
        val match = regex.find(streamUrl) ?: return true
        val expireSec = match.groupValues[1].toLongOrNull() ?: return true
        return currentTimeSec >= expireSec
    }
}
