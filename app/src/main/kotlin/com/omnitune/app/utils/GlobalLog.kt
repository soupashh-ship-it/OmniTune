/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.utils

import android.util.Log
import timber.log.Timber

object GlobalLog {
    fun append(priority: Int, tag: String, message: String) {
        Log.println(priority, tag, message)
    }
}
