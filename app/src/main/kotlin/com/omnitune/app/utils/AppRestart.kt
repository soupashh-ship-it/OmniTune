/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.utils

import android.app.Activity
import android.content.Intent

fun Activity.restartOmniTune() {
    val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
    val component = launchIntent?.component
    if (component != null) {
        startActivity(Intent.makeRestartActivityTask(component))
    } else {
        launchIntent
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            ?.let(::startActivity)
    }
    @Suppress("DEPRECATION")
    overridePendingTransition(0, 0)
}
