/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.kizzy

class DefaultKizzyLogger : KizzyLogger {
    override fun info(message: String) = println("[Kizzy] $message")
    override fun fine(message: String) = println("[Kizzy][DEBUG] $message")
    override fun warning(message: String) = println("[Kizzy][WARN] $message")
    override fun severe(message: String, throwable: Throwable?) {
        println("[Kizzy][ERROR] $message")
        throwable?.printStackTrace()
    }
}

/** Backward-compatible singleton for existing callers (DiscordWebSocket, etc.) */
object KizzyLoggers : KizzyLogger {
    private val delegate = DefaultKizzyLogger()
    override fun info(message: String) = delegate.info(message)
    override fun fine(message: String) = delegate.fine(message)
    override fun warning(message: String) = delegate.warning(message)
    override fun severe(message: String, throwable: Throwable?) = delegate.severe(message, throwable)

    // Legacy API for backward compatibility
    fun log(message: String) = info(message)
    fun error(message: String, throwable: Throwable? = null) = severe(message, throwable)
    fun debug(message: String) = fine(message)
}
