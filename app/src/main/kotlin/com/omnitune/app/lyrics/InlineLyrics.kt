/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.lyrics

import com.omnitune.app.db.entities.LyricsEntity
import com.omnitune.app.models.LyricsLine

data class InlineLyricState(
    val currentLine: String? = null,
    val nextLine: String? = null,
    val isSynced: Boolean = false,
    val isLoading: Boolean = false,
    val hasLyrics: Boolean = false,
)

object InlineLyrics {
    private const val LeadMs = 300L

    fun parseSyncedEntries(lyrics: String?): List<LyricsEntry> {
        val text = lyrics
            ?.takeIf { it.isNotBlank() && it != LyricsEntity.LYRICS_NOT_FOUND }
            ?: return emptyList()

        return when {
            LyricsUtils.isTtml(text) -> LyricsUtils.parseTtml(text)
            text.lineSequence().any { LyricsUtils.LINE_REGEX.matches(it.trim()) } -> LyricsUtils.parseLyrics(text)
            else -> emptyList()
        }.filter { it.time >= 0L && it.text.isNotBlank() }
            .sorted()
    }

    fun stateFor(
        entries: List<LyricsEntry>,
        positionMs: Long,
        loading: Boolean = false,
    ): InlineLyricState {
        if (entries.isEmpty()) {
            return InlineLyricState(isLoading = loading)
        }

        val index = LyricsUtils.findCurrentLineIndex(entries, positionMs, LeadMs)
            .coerceIn(0, entries.lastIndex)
        val current = entries.getOrNull(index)?.text?.cleanLyricLine()
        val next = entries.drop(index + 1)
            .firstOrNull { it.text.isNotBlank() }
            ?.text
            ?.cleanLyricLine()

        return InlineLyricState(
            currentLine = current,
            nextLine = next,
            isSynced = true,
            isLoading = loading,
            hasLyrics = current != null,
        )
    }

    fun syncedEntriesFromLines(lines: List<LyricsLine>): List<LyricsEntry> =
        lines
            .filter { it.timestamp >= 0L && it.text.isNotBlank() }
            .map { LyricsEntry(time = it.timestamp, text = it.text) }
            .sorted()

    fun entriesFromLines(lines: List<LyricsLine>): List<LyricsEntry> =
        lines
            .filter { it.text.isNotBlank() }
            .map { LyricsEntry(time = it.timestamp, text = it.text) }

    fun staticStateFor(lines: List<LyricsLine>): InlineLyricState {
        val first = lines.firstOrNull()?.text?.cleanLyricLine()
        return InlineLyricState(
            currentLine = first,
            isSynced = false,
            hasLyrics = first != null,
        )
    }
}

private fun String.cleanLyricLine(): String? =
    trim()
        .replace(Regex("\\s+"), " ")
        .takeIf { it.isNotBlank() }
