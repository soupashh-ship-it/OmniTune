@file:kotlin.jvm.JvmName("PlatformDesktopKt")
package com.omnitune.shared

import com.omnitune.shared.playback.AudioPlayer
import com.omnitune.shared.playback.DesktopAudioPlayer

class DesktopPlatform : Platform {
    override val name: String = "Desktop JVM (${System.getProperty("java.version") ?: "unknown"})"
    override val isIos: Boolean = false
}

actual fun getPlatform(): Platform = DesktopPlatform()
actual fun createAudioPlayer(): AudioPlayer = DesktopAudioPlayer()
