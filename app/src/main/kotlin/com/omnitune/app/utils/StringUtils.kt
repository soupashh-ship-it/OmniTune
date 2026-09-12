/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */



package com.omnitune.app.utils

import java.math.BigInteger
import java.security.MessageDigest
import java.util.Locale

fun makeTimeString(duration: Long?): String {
    if (duration == null || duration < 0) return ""

    // Heuristic: if the value looks like an epoch millis (greater than ~1e12),
    // format as a human-readable date/time rather than a duration.
    // (1_000_000_000_000L ~= 2001-09-09 UTC)
    if (duration > 1_000_000_000_000L) {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        sdf.timeZone = java.util.TimeZone.getDefault()
        return sdf.format(java.util.Date(duration))
    }

    var sec = duration / 1000
    val day = sec / 86400
    sec %= 86400
    val hour = sec / 3600
    sec %= 3600
    val minute = sec / 60
    sec %= 60

    // More human-friendly duration strings:
    return when {
        day > 0 -> String.format(Locale.US, "%dd %dh %dm %ds", day, hour, minute, sec)
        hour > 0 -> String.format(Locale.US, "%dh %dm %ds", hour, minute, sec)
        minute > 0 -> String.format(Locale.US, "%d:%02d", minute, sec)
        else -> String.format(Locale.US, "%d:%02d", 0, sec)
    }
}

fun md5(str: String): String {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(str.toByteArray())).toString(16).padStart(32, '0')
}

fun joinByBullet(vararg str: String?) =
    str
        .filterNot {
            it.isNullOrEmpty()
        }.joinToString(separator = " • ")

fun formatDurationMs(durationMs: Long): String {
    if (durationMs <= 0L) return "0:00"
    val totalSeconds = durationMs / 1000
    return String.format(Locale.US, "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}

fun formatDurationSeconds(seconds: Long): String {
    if (seconds <= 0L) return "0:00"
    return String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60)
}
