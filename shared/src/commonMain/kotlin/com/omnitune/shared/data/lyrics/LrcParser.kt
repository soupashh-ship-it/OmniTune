package com.omnitune.shared.data.lyrics

import com.omnitune.shared.domain.models.LyricLine
import kotlinx.serialization.Serializable

@Serializable
data class ParsedLrc(
    val metadata: Map<String, String> = emptyMap(),
    val lines: List<LyricLine> = emptyList(),
    val isSynced: Boolean = false,
)

object LrcParser {

    private val META_REGEX = Regex("""^\[(ti|ar|al|au|by|offset|length):(.*)\]$""", RegexOption.IGNORE_CASE)
    private val TIMECODE_REGEX = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{2,3}))?\]""")

    fun parse(lrcText: String?): ParsedLrc {
        if (lrcText.isNullOrBlank()) {
            return ParsedLrc(emptyMap(), emptyList(), isSynced = false)
        }

        val metadata = mutableMapOf<String, String>()
        val parsedLines = mutableListOf<LyricLine>()
        var foundSynced = false

        val rawLines = lrcText.lines()
        for (raw in rawLines) {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) continue

            val metaMatch = META_REGEX.matchEntire(trimmed)
            if (metaMatch != null) {
                val key = metaMatch.groupValues[1].lowercase()
                val value = metaMatch.groupValues[2].trim()
                metadata[key] = value
                continue
            }

            val timeMatches = TIMECODE_REGEX.findAll(trimmed).toList()
            if (timeMatches.isNotEmpty()) {
                val lastMatch = timeMatches.last()
                val textStartIndex = lastMatch.range.last + 1
                val text = if (textStartIndex < trimmed.length) {
                    trimmed.substring(textStartIndex).trim()
                } else {
                    ""
                }

                for (match in timeMatches) {
                    val minutes = match.groupValues[1].toLongOrNull() ?: 0L
                    val seconds = match.groupValues[2].toLongOrNull() ?: 0L
                    val fractionStr = match.groupValues[3]
                    val ms = when (fractionStr.length) {
                        2 -> (fractionStr.toLongOrNull() ?: 0L) * 10L
                        3 -> fractionStr.toLongOrNull() ?: 0L
                        else -> 0L
                    }
                    val totalMs = minutes * 60 * 1000L + seconds * 1000L + ms
                    parsedLines.add(LyricLine(timestampMs = totalMs, text = text))
                    foundSynced = true
                }
            } else {
                parsedLines.add(LyricLine(timestampMs = -1L, text = trimmed))
            }
        }

        if (foundSynced) {
            val syncedOnly = parsedLines.filter { it.timestampMs >= 0L }.sortedBy { it.timestampMs }
            return ParsedLrc(
                metadata = metadata,
                lines = syncedOnly,
                isSynced = true
            )
        }

        return ParsedLrc(
            metadata = metadata,
            lines = parsedLines,
            isSynced = false
        )
    }

    fun findActiveLineIndex(lines: List<LyricLine>, positionMs: Long): Int {
        if (lines.isEmpty() || positionMs < 0) return -1
        if (positionMs < lines[0].timestampMs) return -1

        var low = 0
        var high = lines.lastIndex
        var result = -1

        while (low <= high) {
            val mid = (low + high) ushr 1
            if (lines[mid].timestampMs <= positionMs) {
                result = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }

        return result
    }
}
