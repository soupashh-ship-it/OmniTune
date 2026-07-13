/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.discovery

import com.omnitune.app.ui.screens.HomeCollectionMetadata
import com.omnitune.app.ui.screens.HomeCollectionType

data class MoodGenreCategory(
    val id: String,
    val title: String,
    val type: MoodGenreCategoryType,
    val description: String,
    val primaryQueries: List<String>,
    val fallbackQueries: List<String>,
    val includeKeywords: List<String> = emptyList(),
    val excludeKeywords: List<String> = defaultExcludeKeywords,
    val preferredTerms: List<String> = emptyList(),
    val aliases: List<String> = emptyList(),
    val minResultCount: Int = 12,
    val queryVersion: Int = 1,
)

enum class MoodGenreCategoryType {
    MOOD,
    GENRE,
    ACTIVITY,
    LANGUAGE,
    ERA,
    CUSTOM,
}

object MoodGenreCategories {
    private val profiles = listOf(
        category(
            id = "chill",
            title = "Chill",
            type = MoodGenreCategoryType.MOOD,
            description = "Calm, soft, mellow listening",
            primary = listOf("chill songs playlist", "chill pop songs", "lofi chill songs", "acoustic chill songs"),
            fallback = listOf("relaxing chill music", "mellow songs playlist"),
            include = listOf("chill", "calm", "relaxing", "mellow", "acoustic", "lofi", "soft"),
            exclude = listOf("hardstyle", "gym", "workout", "gaming", "bass boosted", "item song", "dj remix"),
            aliases = listOf("grid_chill", "chill_mix", "chill_english_pop", "soft_hits", "late_night", "ambient_calm", "coffeehouse"),
        ),
        category(
            id = "relax",
            title = "Relax",
            type = MoodGenreCategoryType.MOOD,
            description = "Relaxed, quiet listening",
            primary = listOf("relaxing songs playlist", "relaxing music", "calm songs playlist", "soft relaxing songs"),
            fallback = listOf("peaceful music playlist", "mellow relaxing songs"),
            include = listOf("relax", "relaxing", "calm", "peaceful", "soft", "mellow", "ambient"),
            exclude = listOf("workout", "gym", "hardstyle", "bass boosted", "gaming", "party"),
        ),
        category(
            id = "gaming",
            title = "Gaming",
            type = MoodGenreCategoryType.ACTIVITY,
            description = "High-energy gaming, montage, electronic, phonk, and hype tracks",
            primary = listOf("gaming music playlist", "gaming songs mix", "high energy gaming music", "edm gaming music", "phonk gaming music"),
            fallback = listOf("no copyright gaming music", "gaming montage music", "hype gaming music"),
            include = listOf("gaming", "edm", "phonk", "energetic", "battle", "hype", "montage", "trap", "electronic"),
            exclude = listOf("sleep", "chill sleep", "acoustic sad", "bhajan", "kids", "nursery", "lullaby"),
            aliases = listOf("grid_gaming"),
        ),
        category(
            id = "workout",
            title = "Workout",
            type = MoodGenreCategoryType.ACTIVITY,
            description = "Gym, cardio, and high-energy movement music",
            primary = listOf("workout songs playlist", "gym music mix", "high energy workout songs", "workout edm songs", "gym motivation music"),
            fallback = listOf("cardio workout songs", "hip hop workout songs", "edm workout playlist"),
            include = listOf("workout", "gym", "energy", "motivation", "cardio", "training", "edm", "hype"),
            exclude = listOf("sleep", "lullaby", "sad acoustic", "calm sleep", "nursery"),
            aliases = listOf("grid_workout", "workout_energy", "workout_songs", "gym_bollywood", "fresh_workout", "cardio_hits", "hip_hop_gym"),
        ),
        category(
            id = "focus",
            title = "Focus",
            type = MoodGenreCategoryType.ACTIVITY,
            description = "Study, deep work, lo-fi, and instrumental focus music",
            primary = listOf("focus music playlist", "deep focus music", "study music playlist", "lofi study beats", "instrumental focus music"),
            fallback = listOf("lofi focus music", "study beats playlist", "deep work music"),
            include = listOf("focus", "study", "lofi", "instrumental", "deep work", "beats", "ambient"),
            exclude = listOf("party", "club", "hardstyle", "bass boosted", "kids", "nursery", "reaction"),
            aliases = listOf("grid_focus", "lofi_focus", "deep_work", "study_beats", "focus_beats"),
        ),
        category(
            id = "romance",
            title = "Romance",
            type = MoodGenreCategoryType.MOOD,
            description = "Romantic and love-song listening",
            primary = listOf("romantic songs playlist", "love songs playlist", "soft romantic songs", "romantic acoustic songs"),
            fallback = listOf("romantic hindi songs", "bollywood romantic songs", "new romantic songs"),
            include = listOf("romantic", "romance", "love", "soft", "acoustic", "heart"),
            exclude = listOf("workout", "gaming", "hardstyle", "news", "tutorial"),
            aliases = listOf("grid_romance", "romantic_hindi", "bollywood_romantic", "bollywood_romance", "new_romantic", "old_romance"),
        ),
        category(
            id = "sad",
            title = "Sad",
            type = MoodGenreCategoryType.MOOD,
            description = "Sad, emotional, and heartbreak songs",
            primary = listOf("sad songs playlist", "heartbreak songs", "emotional songs playlist", "sad acoustic songs"),
            fallback = listOf("sad hindi songs", "bollywood sad songs", "emotional heartbreak songs"),
            include = listOf("sad", "heartbreak", "emotional", "cry", "acoustic", "ballad"),
            exclude = listOf("party", "workout", "gaming", "club", "dance remix"),
            aliases = listOf("grid_sad", "sad_songs", "sad_hindi", "bollywood_sad"),
        ),
        category(
            id = "party",
            title = "Party",
            type = MoodGenreCategoryType.ACTIVITY,
            description = "Dance, club, and party music",
            primary = listOf("party songs playlist", "dance party music", "club songs playlist", "party hits"),
            fallback = listOf("punjabi party songs", "bollywood party songs", "dance hits playlist"),
            include = listOf("party", "dance", "club", "hits", "punjabi", "edm", "remix"),
            exclude = listOf("sleep", "study", "sad acoustic", "lullaby", "nursery"),
            aliases = listOf("grid_party", "party_mix", "punjabi_party", "bollywood_party", "retro_party", "dance_hits", "dance_pop"),
        ),
        category(
            id = "feel_good",
            title = "Feel good",
            type = MoodGenreCategoryType.MOOD,
            description = "Bright, upbeat, positive music",
            primary = listOf("feel good songs playlist", "happy songs playlist", "uplifting pop songs", "feel good pop"),
            fallback = listOf("positive songs playlist", "happy music mix"),
            include = listOf("feel good", "happy", "uplifting", "positive", "pop", "bright"),
            exclude = listOf("sad", "heartbreak", "sleep", "funeral", "news"),
            aliases = listOf("grid_feel_good", "sing_along"),
        ),
        category(
            id = "energize",
            title = "Energize",
            type = MoodGenreCategoryType.ACTIVITY,
            description = "Energetic tracks for momentum",
            primary = listOf("high energy songs playlist", "energizing music", "hype songs playlist", "upbeat pop songs"),
            fallback = listOf("edm energy songs", "dance pop songs", "motivation music"),
            include = listOf("energy", "energizing", "hype", "upbeat", "edm", "dance", "motivation"),
            exclude = listOf("sleep", "calm", "sad acoustic", "lullaby"),
            aliases = listOf("grid_energize", "edm_energy"),
        ),
        category(
            id = "commute",
            title = "Commute",
            type = MoodGenreCategoryType.ACTIVITY,
            description = "Road-trip, drive, and commute listening",
            primary = listOf("road trip songs playlist", "driving songs playlist", "commute playlist songs", "travel songs playlist"),
            fallback = listOf("bollywood road trip songs", "drive songs playlist"),
            include = listOf("road trip", "drive", "driving", "travel", "commute", "highway"),
            exclude = listOf("sleep", "lullaby", "nursery", "tutorial"),
            aliases = listOf("grid_commute", "road_trip", "bollywood_drive"),
        ),
        category(
            id = "sleep",
            title = "Sleep",
            type = MoodGenreCategoryType.MOOD,
            description = "Calm sleep and ambient night music",
            primary = listOf("sleep music", "calm sleep songs", "relaxing sleep music", "ambient sleep music"),
            fallback = listOf("sleep acoustic songs", "peaceful sleep music"),
            include = listOf("sleep", "calm", "relaxing", "ambient", "peaceful", "acoustic"),
            exclude = listOf("party", "gaming", "workout", "hardstyle", "bass boosted"),
            aliases = listOf("sleepy_acoustic"),
        ),
        category(
            id = "bollywood_hindi",
            title = "Bollywood / Hindi",
            type = MoodGenreCategoryType.LANGUAGE,
            description = "Hindi and Bollywood songs",
            primary = listOf("bollywood songs playlist", "hindi songs playlist", "bollywood hits", "hindi hits"),
            fallback = listOf("new hindi songs", "popular hindi songs", "bollywood romantic songs"),
            include = listOf("hindi", "bollywood", "filmi", "india"),
            exclude = listOf("tutorial", "news", "reaction", "movie trailer"),
            aliases = listOf("hindi_hits", "hindi_chart_starters", "new_hindi", "bollywood_2000s", "90s_hindi", "early_2010s"),
        ),
        category(
            id = "lofi",
            title = "Lo-fi",
            type = MoodGenreCategoryType.MOOD,
            description = "Lo-fi beats and soft focus music",
            primary = listOf("lofi songs playlist", "lofi chill songs", "lofi study beats", "lofi focus music"),
            fallback = listOf("hindi lofi songs", "lofi beats playlist"),
            include = listOf("lofi", "lo-fi", "chill", "study", "beats", "focus"),
            exclude = listOf("party", "workout", "hardstyle", "bass boosted"),
            aliases = listOf("hindi_lofi"),
        ),
        category(
            id = "electronic",
            title = "Electronic",
            type = MoodGenreCategoryType.GENRE,
            description = "Electronic and EDM songs",
            primary = listOf("edm songs playlist", "electronic music playlist", "edm hits", "electronic dance music"),
            fallback = listOf("edm energy songs", "electronic workout music"),
            include = listOf("edm", "electronic", "dance", "house", "trap", "future bass"),
            exclude = listOf("sleep", "nursery", "bhajan", "acoustic sad"),
            aliases = listOf("edm_energy"),
        ),
    )

    private val byId = buildMap {
        profiles.forEach { profile ->
            put(profile.id.normalizedKey(), profile)
            profile.aliases.forEach { put(it.normalizedKey(), profile) }
        }
    }

    private val byTitle = profiles.associateBy { it.title.normalizedKey() }

    fun forCollection(collection: HomeCollectionMetadata): MoodGenreCategory? {
        byId[collection.id.normalizedKey()]?.let { return it }
        byId[collection.artworkKey.normalizedKey()]?.let { return it }
        byTitle[collection.title.normalizedKey()]?.let { return it }
        byTitle[collection.query.normalizedKey()]?.let { return it }
        return collection.toCustomProfile()
    }

    fun requireProfile(id: String): MoodGenreCategory? =
        byId[id.normalizedKey()] ?: byTitle[id.normalizedKey()]

    private fun category(
        id: String,
        title: String,
        type: MoodGenreCategoryType,
        description: String,
        primary: List<String>,
        fallback: List<String>,
        include: List<String>,
        exclude: List<String> = emptyList(),
        aliases: List<String> = emptyList(),
    ) = MoodGenreCategory(
        id = id,
        title = title,
        type = type,
        description = description,
        primaryQueries = primary.distinct(),
        fallbackQueries = fallback.distinct(),
        includeKeywords = include.distinct(),
        excludeKeywords = (defaultExcludeKeywords + exclude).distinct(),
        preferredTerms = include.distinct(),
        aliases = aliases.distinct(),
    )
}

private val defaultExcludeKeywords = listOf(
    "news",
    "podcast",
    "full movie",
    "movie trailer",
    "reaction",
    "tutorial",
    "ringtone",
    "shorts compilation",
    "kids nursery",
    "nursery rhyme",
)

private fun HomeCollectionMetadata.toCustomProfile(): MoodGenreCategory? {
    if (collectionType != HomeCollectionType.Mood && collectionType != HomeCollectionType.Genre) return null

    val baseQuery = query.trim().takeIf { it.isMeaningfulCategoryQuery() } ?: return null
    val preferred = (title.splitTerms() + baseQuery.splitTerms())
        .filterNot { it in genericQueryWords }
        .distinct()

    if (preferred.isEmpty()) return null

    val type = when {
        baseQuery.contains("hindi", ignoreCase = true) || baseQuery.contains("bollywood", ignoreCase = true) ->
            MoodGenreCategoryType.LANGUAGE
        collectionType == HomeCollectionType.Genre -> MoodGenreCategoryType.GENRE
        else -> MoodGenreCategoryType.MOOD
    }

    return MoodGenreCategory(
        id = id,
        title = title,
        type = type,
        description = "Category-specific Home discovery",
        primaryQueries = listOf(baseQuery, "$title songs playlist", "$title music playlist")
            .filter { it.isMeaningfulCategoryQuery() }
            .distinct(),
        fallbackQueries = listOf("$baseQuery playlist", "$baseQuery mix")
            .filter { it.isMeaningfulCategoryQuery() }
            .distinct(),
        includeKeywords = preferred,
        preferredTerms = preferred,
    )
}

private val genericQueryWords = setOf("song", "songs", "music", "playlist", "mix", "hits")

private fun String.isMeaningfulCategoryQuery(): Boolean {
    val terms = splitTerms()
    if (terms.size < 2) return false
    return terms.any { it !in genericQueryWords }
}

private fun String.splitTerms(): List<String> =
    lowercase()
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }

private fun String.normalizedKey(): String =
    lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
