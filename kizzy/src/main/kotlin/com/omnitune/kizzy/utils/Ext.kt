/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 *
 * Based on Kizzy (c) yzziK(Vaibhav) 2022
 */

package com.omnitune.kizzy.utils

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText

fun HttpResponse.isSuccessful() = status.value in 200..299

suspend fun HttpResponse.bodyAsStringOrNull(): String? = try {
    bodyAsText()
} catch (_: Exception) {
    null
}
