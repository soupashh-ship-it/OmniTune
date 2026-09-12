/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.content

import com.omnitune.app.models.HomeItem
import com.omnitune.app.models.HomeSection
import java.util.Locale

enum class MusicContentSurface {
    HOME,
    DISCOVERY,
    CHARTS,
    SEARCH_SUGGESTIONS,
    RELATED,
    LIBRARY,
    DOWNLOADS,
    LIKED_SONGS,
    HISTORY,
    AUTH,
    SETTINGS,
}

data class MusicContentInvalidationPlan(
    val invalidate: Set<MusicContentSurface>,
    val preserve: Set<MusicContentSurface>,
) {
    fun invalidates(surface: MusicContentSurface): Boolean = surface in invalidate
    fun preserves(surface: MusicContentSurface): Boolean = surface in preserve
}

object MusicContentDiscoveryPolicy {
    private val preservedSurfaces = setOf(
        MusicContentSurface.LIBRARY,
        MusicContentSurface.DOWNLOADS,
        MusicContentSurface.LIKED_SONGS,
        MusicContentSurface.HISTORY,
        MusicContentSurface.AUTH,
        MusicContentSurface.SETTINGS,
    )

    private val languageDependentSurfaces = setOf(
        MusicContentSurface.HOME,
        MusicContentSurface.DISCOVERY,
        MusicContentSurface.CHARTS,
        MusicContentSurface.SEARCH_SUGGESTIONS,
        MusicContentSurface.RELATED,
    )

    private val englishCompetingMarkers = listOf("india", "indian", "desi")

    fun invalidationPlan(
        previous: MusicContentLanguage,
        next: MusicContentLanguage,
    ): MusicContentInvalidationPlan =
        if (previous == next) {
            MusicContentInvalidationPlan(invalidate = emptySet(), preserve = preservedSurfaces)
        } else {
            MusicContentInvalidationPlan(
                invalidate = languageDependentSurfaces,
                preserve = preservedSurfaces,
            )
        }

    fun explicitSearchQuery(rawQuery: String): String = rawQuery.trim()

    fun languageWeightedQuery(
        language: MusicContentLanguage,
        rawQuery: String,
    ): String {
        val query = rawQuery.trim()
        if (query.isBlank() || language == MusicContentLanguage.AUTOMATIC) return query
        if (query.containsAny(language.sectionMarkers)) return query
        return "$query ${language.displayName} music"
    }

    fun songDiscoveryQueries(language: MusicContentLanguage): List<String> =
        language.songDiscoveryQueries

    fun playlistDiscoveryQueries(language: MusicContentLanguage): List<String> =
        language.playlistDiscoveryQueries

    fun artistDiscoveryQueries(language: MusicContentLanguage): List<String> =
        language.artistDiscoveryQueries

    fun trendingSearches(language: MusicContentLanguage): List<String> =
        if (language == MusicContentLanguage.AUTOMATIC || language == MusicContentLanguage.ENGLISH) {
            listOf(
                "Trending Hits",
                "New Music",
                "Pop Favorites",
                "Lo-Fi Beats",
                "Workout Energy",
                "Acoustic Chill",
                "Deep Focus",
            )
        } else {
            listOf(
                "${language.displayName} hits",
                "New ${language.displayName} songs",
                "${language.displayName} playlists",
                "${language.displayName} artists",
                "Lo-Fi Beats",
                "Workout Energy",
                "Acoustic Chill",
            )
        }

    fun songSectionTitle(language: MusicContentLanguage): String =
        if (language == MusicContentLanguage.AUTOMATIC) {
            "Recommended Songs"
        } else {
            "${language.displayName} Essentials"
        }

    fun playlistSectionTitle(language: MusicContentLanguage): String =
        if (language == MusicContentLanguage.AUTOMATIC) {
            "Recommended Playlists"
        } else {
            "${language.displayName} Playlists"
        }

    fun artistSectionTitle(language: MusicContentLanguage): String =
        if (language == MusicContentLanguage.AUTOMATIC) {
            "Recommended Artists"
        } else {
            "${language.displayName} Artists"
        }

    fun mergeHomeSections(
        language: MusicContentLanguage,
        providerSections: List<HomeSection>,
        seededSections: List<HomeSection>,
    ): List<HomeSection> {
        val providerForLanguage = providerSections
            .filterNot { section -> section.isClearlyCompetingLanguageSection(language) }
            .ifEmpty { providerSections }

        return (seededSections + providerForLanguage)
            .filter { section -> section.items.isNotEmpty() }
            .distinctBy { section -> section.title.normalizedKey() to section.items.map(HomeItem::id).take(6) }
    }

    fun isExplicitNavigationPreserved(surface: MusicContentSurface): Boolean =
        surface in preservedSurfaces

    private fun HomeSection.isClearlyCompetingLanguageSection(language: MusicContentLanguage): Boolean {
        if (language == MusicContentLanguage.AUTOMATIC) return false

        val titleText = title.normalizedKey()
        if (titleText.containsAny(language.sectionMarkers)) return false

        val competingMarkers = MusicContentLanguage.settingsOptions
            .filter { it != language && it != MusicContentLanguage.AUTOMATIC }
            .flatMap { it.sectionMarkers }
            .let { markers ->
                if (language == MusicContentLanguage.ENGLISH) markers + englishCompetingMarkers else markers
            }

        return titleText.containsAny(competingMarkers)
    }

    private fun String.containsAny(markers: List<String>): Boolean {
        if (markers.isEmpty()) return false
        val normalized = normalizedKey()
        return markers.any { marker -> normalized.contains(marker.normalizedKey()) }
    }

    private fun String.normalizedKey(): String =
        lowercase(Locale.ROOT).replace(Regex("\\s+"), " ").trim()
}
