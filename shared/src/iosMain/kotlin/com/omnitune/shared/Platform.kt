package com.omnitune.shared

import com.omnitune.shared.playback.AudioPlayer
import com.omnitune.shared.playback.IosAudioPlayer
import platform.UIKit.UIDevice

class IosPlatform : Platform {
    override val name: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val isIos: Boolean = true
}

actual fun getPlatform(): Platform = IosPlatform()
actual fun createAudioPlayer(): AudioPlayer = IosAudioPlayer()
