/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.kizzy.rpc

sealed class RpcImage {
    data class DiscordImage(val id: String) : RpcImage()
    data class ExternalImage(val url: String) : RpcImage()
    data class Unresolved(val url: String) : RpcImage()
}
