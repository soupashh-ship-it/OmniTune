/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */



package com.omnitune.app.utils

import android.content.Context
import android.content.res.Configuration
import com.omnitune.app.BuildConfig
import java.util.Locale
import timber.log.Timber

fun reportException(throwable: Throwable) {
    if (BuildConfig.DEBUG) {
        Timber.e(throwable, "Unexpected failure")
    } else {
        // Exception messages can contain provider URLs, query text, or credentials.
        Timber.e("Unexpected failure: %s", throwable.javaClass.simpleName)
    }
}

@Suppress("DEPRECATION")
fun setAppLocale(context: Context, locale: Locale) {
    val config = Configuration(context.resources.configuration)
    config.setLocale(locale)
    context.resources.updateConfiguration(config, context.resources.displayMetrics)
}
