package com.omnitune.app.ui.utils

import java.util.Locale

fun String.toDisplayLabel(locale: Locale = Locale.getDefault()): String =
    lowercase(locale)
        .replace('_', ' ')
        .replaceFirstChar { it.toString().uppercase(locale) }

fun Enum<*>.displayLabel(locale: Locale = Locale.getDefault()): String =
    name.toDisplayLabel(locale)
