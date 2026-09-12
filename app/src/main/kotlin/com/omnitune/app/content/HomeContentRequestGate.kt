/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.content

import com.omnitune.app.models.HomeSection

data class HomeContentRequest(
    val generation: Long,
    val language: MusicContentLanguage,
)

class HomeContentRequestGate(
    initialLanguage: MusicContentLanguage = MusicContentLanguage.Default,
) {
    private var generation = 0L
    private var activeLanguage = initialLanguage

    fun begin(language: MusicContentLanguage): HomeContentRequest {
        activeLanguage = language
        generation += 1
        return HomeContentRequest(generation = generation, language = language)
    }

    fun invalidate() {
        generation += 1
    }

    fun accepts(request: HomeContentRequest): Boolean =
        request.generation == generation && request.language == activeLanguage
}

class LanguageScopedHomeContinuationStore {
    private val continuations = mutableMapOf<MusicContentLanguage, String?>()

    fun continuationFor(language: MusicContentLanguage): String? = continuations[language]

    fun setContinuation(language: MusicContentLanguage, continuation: String?) {
        continuations[language] = continuation
    }

    fun resetAll() {
        continuations.clear()
    }
}

class LanguageScopedHomeSectionCache {
    private val sectionsByLanguage = mutableMapOf<MusicContentLanguage, List<HomeSection>>()

    fun sectionsFor(language: MusicContentLanguage): List<HomeSection> =
        sectionsByLanguage[language].orEmpty()

    fun put(language: MusicContentLanguage, sections: List<HomeSection>) {
        sectionsByLanguage[language] = sections
    }

    fun resetAll() {
        sectionsByLanguage.clear()
    }
}
