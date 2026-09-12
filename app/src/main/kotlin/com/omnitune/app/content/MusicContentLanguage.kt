/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.content

import androidx.datastore.preferences.core.Preferences
import com.omnitune.app.constants.DefaultMusicLanguageKey
import com.omnitune.innertube.models.YouTubeLocale
import java.util.Locale

data class MusicContentRequestLocale(
    val hl: String,
    val gl: String,
) {
    fun toYouTubeLocale(): YouTubeLocale = YouTubeLocale(gl = gl, hl = hl)
}

data class MusicContentRequestContext(
    val selectedLanguage: MusicContentLanguage,
    val effectiveLanguageCode: String,
    val effectiveRegion: String,
    val requestLocale: YouTubeLocale,
)

enum class MusicContentLanguage(
    val displayName: String,
    val requestLocale: MusicContentRequestLocale?,
    internal val songDiscoveryQueries: List<String>,
    internal val playlistDiscoveryQueries: List<String>,
    internal val artistDiscoveryQueries: List<String>,
    internal val sectionMarkers: List<String>,
) {
    ENGLISH(
        displayName = "English",
        requestLocale = MusicContentRequestLocale(hl = "en", gl = "US"),
        songDiscoveryQueries = listOf("new english songs", "english pop hits", "global music hits"),
        playlistDiscoveryQueries = listOf("english music playlist", "global pop playlist"),
        artistDiscoveryQueries = listOf("english music artists", "global pop artists"),
        sectionMarkers = listOf("english", "global", "international", "western", "us hits", "uk hits"),
    ),
    HINDI(
        displayName = "Hindi",
        requestLocale = MusicContentRequestLocale(hl = "hi", gl = "IN"),
        songDiscoveryQueries = listOf("new hindi songs", "bollywood hits", "hindi music hits"),
        playlistDiscoveryQueries = listOf("hindi music playlist", "bollywood playlist"),
        artistDiscoveryQueries = listOf("hindi music artists", "bollywood singers"),
        sectionMarkers = listOf("hindi", "bollywood"),
    ),
    PUNJABI(
        displayName = "Punjabi",
        requestLocale = MusicContentRequestLocale(hl = "pa", gl = "IN"),
        songDiscoveryQueries = listOf("new punjabi songs", "punjabi music hits", "punjabi party songs"),
        playlistDiscoveryQueries = listOf("punjabi music playlist", "punjabi party playlist"),
        artistDiscoveryQueries = listOf("punjabi music artists", "punjabi singers"),
        sectionMarkers = listOf("punjabi"),
    ),
    TELUGU(
        displayName = "Telugu",
        requestLocale = MusicContentRequestLocale(hl = "te", gl = "IN"),
        songDiscoveryQueries = listOf("new telugu songs", "telugu music hits", "tollywood hits"),
        playlistDiscoveryQueries = listOf("telugu music playlist", "tollywood playlist"),
        artistDiscoveryQueries = listOf("telugu music artists", "telugu singers"),
        sectionMarkers = listOf("telugu", "tollywood"),
    ),
    TAMIL(
        displayName = "Tamil",
        requestLocale = MusicContentRequestLocale(hl = "ta", gl = "IN"),
        songDiscoveryQueries = listOf("new tamil songs", "tamil music hits", "kollywood hits"),
        playlistDiscoveryQueries = listOf("tamil music playlist", "kollywood playlist"),
        artistDiscoveryQueries = listOf("tamil music artists", "tamil singers"),
        sectionMarkers = listOf("tamil", "kollywood"),
    ),
    MALAYALAM(
        displayName = "Malayalam",
        requestLocale = MusicContentRequestLocale(hl = "ml", gl = "IN"),
        songDiscoveryQueries = listOf("new malayalam songs", "malayalam music hits", "mollywood hits"),
        playlistDiscoveryQueries = listOf("malayalam music playlist", "mollywood playlist"),
        artistDiscoveryQueries = listOf("malayalam music artists", "malayalam singers"),
        sectionMarkers = listOf("malayalam", "mollywood"),
    ),
    KANNADA(
        displayName = "Kannada",
        requestLocale = MusicContentRequestLocale(hl = "kn", gl = "IN"),
        songDiscoveryQueries = listOf("new kannada songs", "kannada music hits", "sandalwood hits"),
        playlistDiscoveryQueries = listOf("kannada music playlist", "sandalwood playlist"),
        artistDiscoveryQueries = listOf("kannada music artists", "kannada singers"),
        sectionMarkers = listOf("kannada", "sandalwood"),
    ),
    BENGALI(
        displayName = "Bengali",
        requestLocale = MusicContentRequestLocale(hl = "bn", gl = "IN"),
        songDiscoveryQueries = listOf("new bengali songs", "bengali music hits", "bangla songs"),
        playlistDiscoveryQueries = listOf("bengali music playlist", "bangla playlist"),
        artistDiscoveryQueries = listOf("bengali music artists", "bangla singers"),
        sectionMarkers = listOf("bengali", "bangla"),
    ),
    MARATHI(
        displayName = "Marathi",
        requestLocale = MusicContentRequestLocale(hl = "mr", gl = "IN"),
        songDiscoveryQueries = listOf("new marathi songs", "marathi music hits", "marathi songs"),
        playlistDiscoveryQueries = listOf("marathi music playlist", "marathi playlist"),
        artistDiscoveryQueries = listOf("marathi music artists", "marathi singers"),
        sectionMarkers = listOf("marathi"),
    ),
    GUJARATI(
        displayName = "Gujarati",
        requestLocale = MusicContentRequestLocale(hl = "gu", gl = "IN"),
        songDiscoveryQueries = listOf("new gujarati songs", "gujarati music hits", "gujarati songs"),
        playlistDiscoveryQueries = listOf("gujarati music playlist", "gujarati playlist"),
        artistDiscoveryQueries = listOf("gujarati music artists", "gujarati singers"),
        sectionMarkers = listOf("gujarati"),
    ),
    AUTOMATIC(
        displayName = "Automatic",
        requestLocale = null,
        songDiscoveryQueries = listOf("music discovery", "new music"),
        playlistDiscoveryQueries = listOf("music playlist"),
        artistDiscoveryQueries = listOf("music artists"),
        sectionMarkers = emptyList(),
    );

    val preferenceValue: String get() = name

    fun toYouTubeLocale(
        automaticLocale: YouTubeLocale = automaticDeviceLocale(),
    ): YouTubeLocale = requestLocale?.toYouTubeLocale() ?: automaticLocale

    fun toRequestContext(
        automaticLocale: YouTubeLocale = automaticDeviceLocale(),
    ): MusicContentRequestContext {
        val locale = toYouTubeLocale(automaticLocale)
        return MusicContentRequestContext(
            selectedLanguage = this,
            effectiveLanguageCode = locale.hl,
            effectiveRegion = locale.gl,
            requestLocale = locale,
        )
    }

    companion object {
        val Default: MusicContentLanguage = ENGLISH

        val settingsOptions: List<MusicContentLanguage> = listOf(
            ENGLISH,
            HINDI,
            PUNJABI,
            TELUGU,
            TAMIL,
            MALAYALAM,
            KANNADA,
            BENGALI,
            MARATHI,
            GUJARATI,
            AUTOMATIC,
        )

        fun fromPreference(value: String?): MusicContentLanguage =
            value
                ?.let { stored -> entries.firstOrNull { it.name == stored } }
                ?: Default

        fun fromDisplayName(value: String): MusicContentLanguage? =
            settingsOptions.firstOrNull { it.displayName.equals(value, ignoreCase = true) }

        fun fromTasteSelections(selected: Set<String>): MusicContentLanguage? =
            selected.asSequence().mapNotNull(::fromDisplayName).firstOrNull()

        fun automaticDeviceLocale(locale: Locale = Locale.getDefault()): YouTubeLocale {
            val languageTag = locale.toLanguageTag().replace("-Hant", "")
            return YouTubeLocale(
                gl = locale.country.takeIf { it.isNotBlank() } ?: "US",
                hl = locale.language.takeIf { it.isNotBlank() }
                    ?: languageTag.takeIf { it.isNotBlank() }
                    ?: "en",
            )
        }
    }
}

fun Preferences.defaultMusicLanguage(): MusicContentLanguage =
    MusicContentLanguage.fromPreference(this[DefaultMusicLanguageKey])
