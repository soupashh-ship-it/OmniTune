package com.omnitune.shared

import com.omnitune.shared.playback.AudioPlayer
import com.omnitune.shared.playback.PlaybackController
import com.omnitune.shared.playback.PlaybackControllerImpl

interface Platform {
    val name: String
    val isIos: Boolean
}

expect fun getPlatform(): Platform
expect fun createAudioPlayer(): AudioPlayer

fun createPlaybackController(audioPlayer: AudioPlayer = createAudioPlayer()): PlaybackController {
    return PlaybackControllerImpl(audioPlayer = audioPlayer)
}
