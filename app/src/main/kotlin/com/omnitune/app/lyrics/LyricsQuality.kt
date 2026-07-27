/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.lyrics

import com.omnitune.app.models.MediaMetadata
import java.text.Normalizer
import java.util.Locale

internal object LyricsQuality {
    fun score(
        providerName: String,
        lyrics: String,
        mediaMetadata: MediaMetadata,
        isSynced: Boolean,
        isTrackBound: Boolean = false,
        isMetadataBound: Boolean = false,
    ): Int? {
        if (!hasMeaningfulText(lyrics)) return null
        if (!isTrackBound && hasLikelyLanguageMismatch(lyrics, mediaMetadata)) return null
        if (!isTrackBound && !isMetadataBound && !hasStrongFallbackIdentityEvidence(lyrics, mediaMetadata)) return null

        var score = 10
        if (isTrackBound) score += 1_000
        if (isMetadataBound) score += 950
        if (isSynced) score += 40
        score += providerScore(providerName)

        val text = lyrics.normalizedSearchText()
        val titleTokens = mediaMetadata.title.normalizedSearchText().tokens()
        val artistTokens = mediaMetadata.artists.joinToString(" ") { it.name }.normalizedSearchText().tokens()
        if (titleTokens.isNotEmpty() && titleTokens.any { it.length >= 4 && it in text }) score += 8
        if (artistTokens.isNotEmpty() && artistTokens.any { it.length >= 4 && it in text }) score += 4

        return score
    }

    /**
     * Search providers expose only lyrics text, not the matched recording's
     * metadata. Do not accept an unverified same-title result. Requiring both
     * a meaningful title token and artist token is deliberately conservative:
     * no lyrics is safer than attaching another song's words to this track.
     */
    private fun hasStrongFallbackIdentityEvidence(
        lyrics: String,
        mediaMetadata: MediaMetadata,
    ): Boolean {
        val text = lyrics.normalizedSearchText()
        val titleMatches = mediaMetadata.title.normalizedSearchText().tokens()
            .filter { it.length >= 4 }
            .count { it in text }
        val artistMatches = mediaMetadata.artists.joinToString(" ") { it.name }
            .normalizedSearchText()
            .tokens()
            .filter { it.length >= 4 }
            .any { it in text }
        return titleMatches >= 1 && artistMatches
    }

    fun hasMeaningfulText(lyrics: String): Boolean {
        val normalized = lyrics
            .replace("\uFEFF", "")
            .replace(InvisibleCharsRegex, "")
            .trim { it.isWhitespace() || it == '\u00A0' }

        if (normalized.isEmpty()) return false
        val remaining = TimestampRegex
            .replace(normalized, "")
            .replace(InvisibleCharsRegex, "")
            .trim { it.isWhitespace() || it == '\u00A0' }

        return remaining.lineSequence()
            .map { it.trim() }
            .any { line -> line.any { !it.isWhitespace() && it != '\u00A0' } }
    }

    private fun providerScore(providerName: String): Int =
        when (providerName.lowercase(Locale.US)) {
            "youtube" -> 20
            "simpmusic" -> 16
            "lrclib" -> 12
            "betterlyrics" -> 10
            "kugou" -> 7
            else -> 0
        }

    private fun hasLikelyLanguageMismatch(lyrics: String, mediaMetadata: MediaMetadata): Boolean {
        val queryScripts = scriptProfile(
            mediaMetadata.title + " " + mediaMetadata.artists.joinToString(" ") { it.name },
        )
        val lyricScripts = scriptProfile(stripTimingAndMarkup(lyrics))
        if (lyricScripts.letters < 12) return false

        val queryHasCjk = queryScripts.cjkRatio > 0.15f || queryScripts.kanaRatio > 0.15f || queryScripts.hangulRatio > 0.15f
        val lyricsMostlyCjk = lyricScripts.cjkRatio > 0.35f || lyricScripts.kanaRatio > 0.35f || lyricScripts.hangulRatio > 0.35f
        if (!queryHasCjk && lyricsMostlyCjk) return true

        val queryHasCyrillic = queryScripts.cyrillicRatio > 0.20f
        if (!queryHasCyrillic && lyricScripts.cyrillicRatio > 0.55f) return true

        val queryHasDevanagari = queryScripts.devanagariRatio > 0.20f
        if (!queryHasDevanagari && lyricScripts.devanagariRatio > 0.55f) return true

        return false
    }

    private fun scriptProfile(text: String): ScriptProfile {
        var letters = 0
        var cjk = 0
        var kana = 0
        var hangul = 0
        var cyrillic = 0
        var devanagari = 0

        text.forEach { char ->
            if (!char.isLetter()) return@forEach
            letters++
            when (Character.UnicodeBlock.of(char)) {
                Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS,
                Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A,
                Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B,
                Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS -> cjk++
                Character.UnicodeBlock.HIRAGANA,
                Character.UnicodeBlock.KATAKANA,
                Character.UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS -> kana++
                Character.UnicodeBlock.HANGUL_SYLLABLES,
                Character.UnicodeBlock.HANGUL_JAMO,
                Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO -> hangul++
                Character.UnicodeBlock.CYRILLIC,
                Character.UnicodeBlock.CYRILLIC_SUPPLEMENTARY,
                Character.UnicodeBlock.CYRILLIC_EXTENDED_A,
                Character.UnicodeBlock.CYRILLIC_EXTENDED_B -> cyrillic++
                Character.UnicodeBlock.DEVANAGARI -> devanagari++
            }
        }

        fun ratio(count: Int) = if (letters == 0) 0f else count.toFloat() / letters.toFloat()
        return ScriptProfile(
            letters = letters,
            cjkRatio = ratio(cjk),
            kanaRatio = ratio(kana),
            hangulRatio = ratio(hangul),
            cyrillicRatio = ratio(cyrillic),
            devanagariRatio = ratio(devanagari),
        )
    }

    private fun stripTimingAndMarkup(text: String): String =
        text
            .replace(TimestampRegex, " ")
            .replace(Regex("<[^>]+>"), " ")

    private fun String.normalizedSearchText(): String =
        Normalizer.normalize(this, Normalizer.Form.NFKD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9 ]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun String.tokens(): List<String> = split(' ').filter { it.isNotBlank() }

    private data class ScriptProfile(
        val letters: Int,
        val cjkRatio: Float,
        val kanaRatio: Float,
        val hangulRatio: Float,
        val cyrillicRatio: Float,
        val devanagariRatio: Float,
    )

    private val TimestampRegex = Regex("""\[[0-9]{1,2}:[0-9]{2}(?:\.[0-9]{1,3})?]""")
    private val InvisibleCharsRegex = Regex("""[\u200B\u200C\u200D\u2060\u00AD]""")
}
