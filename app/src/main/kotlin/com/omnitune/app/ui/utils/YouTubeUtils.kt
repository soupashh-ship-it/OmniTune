/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */



package com.omnitune.app.ui.utils

fun String.resize(
    width: Int? = null,
    height: Int? = null,
): String {
    if (width == null && height == null) return this
    "https://lh3\\.googleusercontent\\.com/.*=w(\\d+)-h(\\d+).*".toRegex()
        .matchEntire(this)?.groupValues?.let { group ->
        val (W, H) = group.drop(1).map { it.toInt() }
        var w = width
        var h = height
        if (w != null && h == null) h = (w / W) * H
        if (w == null && h != null) w = (h / H) * W
        return "${split("=w")[0]}=w$w-h$h-p-l90-rj"
    }
    val requestedSize = width ?: height
    if (requestedSize != null) {
        "https://yt3\\.ggpht\\.com/.*=s(\\d+).*".toRegex()
            .matchEntire(this)?.let {
                return "${substringBefore("=s")}=s$requestedSize"
            }
    }
    if (contains("i.ytimg.com/vi/") || contains("i.ytimg.com/vi_webp/")) {
        return replace("mqdefault.jpg", "maxresdefault.jpg")
            .replace("hqdefault.jpg", "maxresdefault.jpg")
            .replace("sddefault.jpg", "maxresdefault.jpg")
            .replace("mqdefault.webp", "maxresdefault.webp")
            .replace("hqdefault.webp", "maxresdefault.webp")
            .replace("sddefault.webp", "maxresdefault.webp")
    }
    return this
}
