/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import androidx.annotation.VisibleForTesting
import androidx.media3.common.C
import kotlin.math.roundToLong
import org.json.JSONArray
import org.json.JSONException
import timber.log.Timber

internal data class SponsorBlockSegment(
    val startMs: Long,
    val endMs: Long,
    val category: String,
    val uuid: String?,
)

internal object SponsorBlockDefaults {
    val SkipCategories = listOf(
        "sponsor",
        "selfpromo",
        "interaction",
        "intro",
        "outro",
        "preview",
        "music_offtopic",
    )
}

internal object SponsorBlockSegmentParser {
    fun parseSkipSegments(json: String): List<SponsorBlockSegment> {
        if (json.isBlank()) return emptyList()

        return try {
            val segments = JSONArray(json)
            buildList {
                for (index in 0 until segments.length()) {
                    val item = segments.optJSONObject(index) ?: continue
                    val actionType = item.optString("actionType", "skip")
                    if (actionType != "skip") continue

                    val segment = item.optJSONArray("segment") ?: continue
                    val startSeconds = segment.optDouble(0, Double.NaN)
                    val endSeconds = segment.optDouble(1, Double.NaN)
                    if (!startSeconds.isFinite() || !endSeconds.isFinite()) continue

                    val startMs = (startSeconds * 1_000L).roundToLong().coerceAtLeast(0L)
                    val endMs = (endSeconds * 1_000L).roundToLong()
                    if (endMs <= startMs) continue

                    add(
                        SponsorBlockSegment(
                            startMs = startMs,
                            endMs = endMs,
                            category = item.optString("category").takeIf { it.isNotBlank() }.orEmpty(),
                            uuid = item.optString("UUID").takeIf { it.isNotBlank() },
                        ),
                    )
                }
            }.sortedWith(compareBy<SponsorBlockSegment> { it.startMs }.thenBy { it.endMs })
        } catch (error: JSONException) {
            Timber.tag("SponsorBlock").w(error, "Invalid SponsorBlock response")
            emptyList()
        }
    }
}

internal object SponsorBlockSkipPolicy {
    @VisibleForTesting
    internal const val SeekPastSegmentPaddingMs = 250L

    @VisibleForTesting
    internal const val MinimumSeekDeltaMs = 250L

    fun seekTargetMs(
        segments: List<SponsorBlockSegment>,
        positionMs: Long,
        durationMs: Long,
    ): Long? {
        if (positionMs < 0L || segments.isEmpty()) return null

        val activeSegment = segments
            .asSequence()
            .filter { segment ->
                positionMs >= segment.startMs &&
                    positionMs < segment.endMs - MinimumSeekDeltaMs
            }
            .maxByOrNull { it.endMs }
            ?: return null

        val rawTarget = activeSegment.endMs + SeekPastSegmentPaddingMs
        val boundedTarget = when {
            durationMs == C.TIME_UNSET || durationMs <= 0L -> rawTarget
            else -> rawTarget.coerceAtMost(durationMs)
        }

        return boundedTarget.takeIf { it > positionMs + MinimumSeekDeltaMs }
    }
}
