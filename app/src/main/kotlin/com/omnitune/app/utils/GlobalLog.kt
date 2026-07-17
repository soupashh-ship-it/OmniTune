/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.utils

import android.util.Log
import timber.log.Timber

object GlobalLog {
    fun append(priority: Int, tag: String, message: String) {
        when (priority) {
            Log.DEBUG -> Timber.tag(tag).d(message)
            Log.INFO -> Timber.tag(tag).i(message)
            Log.WARN -> Timber.tag(tag).w(message)
            Log.ERROR -> Timber.tag(tag).e(message)
            else -> Timber.tag(tag).log(priority, message)
        }
    }
}
