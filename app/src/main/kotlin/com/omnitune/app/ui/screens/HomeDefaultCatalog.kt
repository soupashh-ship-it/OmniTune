/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

object HomeDefaultCatalog {
    private const val QUERY_COLLECTION_PREFIX = "query:"
    private const val PROVIDER_COLLECTION_PREFIX = "provider:"
    private const val PROVIDER_SEPARATOR = "||"

    val moodChips = listOf(
        MoodChip("chill", "Chill", "chill mix"),
        MoodChip("relax", "Relax", "relaxing music"),
        MoodChip("feel_good", "Feel good", "feel good songs"),
        MoodChip("energize", "Energize", "energizing music"),
        MoodChip("romance", "Romance", "romance songs hindi"),
        MoodChip("sad", "Sad", "sad songs hindi"),
        MoodChip("focus", "Focus", "focus music"),
        MoodChip("commute", "Commute", "commute playlist"),
        MoodChip("workout", "Workout", "workout music"),
        MoodChip("party", "Party", "party songs"),
        MoodChip("gaming", "Gaming", "gaming music"),
    )

    val genreGrid = listOf(
        MoodChip("grid_chill", "Chill", "chill songs"),
        MoodChip("grid_focus", "Focus", "focus beats"),
        MoodChip("grid_commute", "Commute", "road trip songs"),
        MoodChip("grid_gaming", "Gaming", "gaming music"),
        MoodChip("grid_energize", "Energize", "energy songs"),
        MoodChip("grid_party", "Party", "party mix"),
        MoodChip("grid_feel_good", "Feel good", "feel good pop"),
        MoodChip("grid_romance", "Romance", "romantic hindi songs"),
        MoodChip("grid_sad", "Sad", "sad hindi songs"),
        MoodChip("grid_workout", "Workout", "workout songs"),
    )

    val heroItems = listOf(
        hero("hindi_hits", "Hindi Hits", "Big sing-along tracks for today", "hindi hits"),
        hero("chill_mix", "Chill Mix", "Easy tracks for slower moments", "chill mix"),
        hero("workout_energy", "Workout Energy", "High-tempo music for movement", "workout songs"),
        hero("romantic_hindi", "Romantic Hindi", "Melodies for late-night listening", "romantic hindi songs"),
        hero("punjabi_party", "Punjabi Party", "Bright party starters and dance picks", "punjabi party songs"),
        hero("lofi_focus", "Lo-fi Focus", "Soft beats for deep work", "lofi focus"),
        hero("english_pop", "English Pop", "Fresh pop starting points", "english pop songs"),
        hero("sad_songs", "Sad Songs", "Emotional songs for quiet listening", "sad songs"),
    )

    val quickPicks = listOf(
        quick("arijit_hits", "Arijit Singh Hits", "Popular starting point", "arijit singh hits"),
        quick("bollywood_romantic", "Bollywood Romantic", "Made for exploring", "bollywood romantic songs"),
        quick("hindi_lofi", "Hindi Lo-fi", "Fresh starts", "hindi lofi songs"),
        quick("punjabi_hits", "Punjabi Hits", "Popular starting point", "punjabi hits"),
        quick("workout_songs", "Workout Songs", "Energy search", "workout songs"),
        quick("chill_english_pop", "Chill English Pop", "Browse by mood", "chill english pop"),
        quick("sad_hindi", "Sad Hindi Songs", "Emotional search", "sad hindi songs"),
        quick("party_mix", "Party Mix", "Made for exploring", "party mix songs"),
        quick("coke_studio_hindi", "Coke Studio Hindi", "Popular sessions search", "coke studio hindi"),
        quick("bollywood_2000s", "2000s Bollywood", "Throwback starting point", "2000s bollywood songs"),
        quick("focus_beats", "Focus Beats", "Work and study", "focus beats"),
        quick("road_trip", "Road Trip Songs", "Commute and travel", "road trip songs"),
    )

    val freshDiscovery = shelf(
        id = "fresh_discovery",
        title = "Fresh discovery",
        items = listOf(
            card("fresh_tracks", "Fresh tracks", "New music starting point", "new songs"),
            card("viral_music", "Viral music", "Songs people search often", "viral songs"),
            card("indie_finds", "Indie finds", "Independent sounds to explore", "indie music"),
            card("acoustic_covers", "Acoustic covers", "Stripped-back versions", "acoustic covers"),
            card("late_night", "Late-night listens", "Soft after-hours search", "late night songs"),
            card("lofi_focus", "Lo-fi focus", "Low-friction beats", "lofi focus"),
            card("fresh_pop", "Fresh pop", "Bright recent pop searches", "new pop songs"),
            card("new_hindi", "New Hindi", "Fresh Hindi searches", "new hindi songs"),
        ),
        actionLabel = null,
    )

    val shelves = listOf(
        shelf(
            id = "popular_starting_points",
            title = "Popular starting points",
            items = listOf(
                card("hindi_chart_starters", "Hindi chart starters", "Explore recent favorites", "hindi popular songs"),
                card("global_pop", "Global pop", "Bright pop searches", "global pop songs"),
                card("viral_reels", "Viral music", "Songs people search often", "viral songs"),
                card("acoustic_covers", "Acoustic covers", "Stripped-back versions", "acoustic covers"),
                card("indie_discoveries", "Indie discoveries", "New-to-you sounds", "indie music"),
                card("dance_hits", "Dance hits", "Move-first picks", "dance hits"),
                card("soft_hits", "Soft hits", "Easy listening", "soft songs"),
                card("sing_along", "Sing-along", "Familiar hooks", "sing along songs"),
            ),
        ),
        shelf(
            id = "bollywood_moods",
            title = "Bollywood moods",
            items = listOf(
                card("bollywood_romance", "Romance", "Warm Hindi melodies", "bollywood romantic songs"),
                card("bollywood_rain", "Rainy day", "Soft monsoon listening", "bollywood rain songs"),
                card("bollywood_party", "Party", "Dance-floor Hindi tracks", "bollywood party songs"),
                card("bollywood_sad", "Sad", "Emotional ballads", "bollywood sad songs"),
                card("bollywood_drive", "Drive", "Highway-ready Hindi", "bollywood road trip songs"),
                card("bollywood_90s", "90s Bollywood", "Classic throwbacks", "90s bollywood songs"),
            ),
        ),
        shelf(
            id = "focus_and_chill",
            title = "Focus and chill",
            items = listOf(
                card("lofi_focus", "Lo-fi focus", "Low-friction beats", "lofi focus"),
                card("deep_work", "Deep work", "Steady background music", "deep work music"),
                card("sleepy_acoustic", "Sleepy acoustic", "Soft evening search", "sleep acoustic songs"),
                card("ambient_calm", "Ambient calm", "Minimal textures", "ambient calm music"),
                card("study_beats", "Study beats", "Quiet momentum", "study beats"),
                card("coffeehouse", "Coffeehouse", "Warm acoustic mood", "coffeehouse music"),
            ),
        ),
        shelf(
            id = "party_and_workout",
            title = "Party and workout",
            items = listOf(
                card("gym_bollywood", "Gym Bollywood", "Hindi energy", "bollywood workout songs"),
                card("punjabi_party", "Punjabi party", "Big hooks and rhythm", "punjabi party songs"),
                card("edm_energy", "EDM energy", "Fast electronic picks", "edm workout"),
                card("dance_pop", "Dance pop", "Upbeat pop movement", "dance pop songs"),
                card("hip_hop_gym", "Hip-hop gym", "Heavy workout search", "hip hop workout songs"),
                card("cardio_hits", "Cardio hits", "High-tempo search", "cardio workout songs"),
            ),
        ),
        shelf(
            id = "india_biggest_sounds",
            title = "India's biggest sounds",
            items = listOf(
                card("hindi_hits", "Hindi hits", "Popular Hindi searches", "hindi hits"),
                card("tamil_hits", "Tamil hits", "Big Tamil starting point", "tamil hits"),
                card("telugu_hits", "Telugu hits", "Popular Telugu searches", "telugu hits"),
                card("punjabi_hits", "Punjabi hits", "Bright Punjabi songs", "punjabi hits"),
                card("malayalam_hits", "Malayalam hits", "Regional discovery", "malayalam hits"),
                card("indie_india", "Indie India", "Independent sounds", "indie india songs"),
            ),
        ),
        shelf(
            id = "new_music_searches",
            title = "New music searches",
            items = listOf(
                card("new_hindi", "New Hindi", "Fresh Hindi searches", "new hindi songs"),
                card("new_indie", "New indie", "Recent indie music", "new indie music"),
                card("new_pop", "New pop", "Fresh pop search", "new pop songs"),
                card("new_punjabi", "New Punjabi", "Recent Punjabi songs", "new punjabi songs"),
                card("new_romantic", "New romantic", "Fresh romantic songs", "new romantic songs"),
                card("fresh_workout", "Fresh workout", "New energy tracks", "new workout songs"),
            ),
        ),
        shelf(
            id = "throwbacks",
            title = "2000s and throwbacks",
            items = listOf(
                card("bollywood_2000s", "2000s Bollywood", "Familiar Hindi memories", "2000s bollywood songs"),
                card("early_2010s", "Early 2010s", "Pop and film throwbacks", "2010 bollywood songs"),
                card("classic_indipop", "Classic Indipop", "Old-school pop searches", "classic indipop"),
                card("old_romance", "Old romance", "Evergreen love songs", "old romantic songs"),
                card("retro_party", "Retro party", "Throwback dance energy", "retro party songs"),
                card("90s_hindi", "90s Hindi", "Evergreen Hindi picks", "90s hindi songs"),
            ),
        ),
    )

    fun findCollection(id: String): HomeCollectionMetadata? {
        if (id.startsWith(PROVIDER_COLLECTION_PREFIX)) {
            val parts = id.removePrefix(PROVIDER_COLLECTION_PREFIX).split(PROVIDER_SEPARATOR)
            val kind = parts.getOrNull(0).orEmpty()
            val providerId = parts.getOrNull(1).orEmpty()
            val title = parts.getOrNull(2).orEmpty().ifBlank { "Collection" }
            val subtitle = parts.getOrNull(3).orEmpty().ifBlank { providerSubtitle(kind) }
            val params = parts.getOrNull(4).orEmpty().ifBlank { null }
            if (providerId.isNotBlank()) {
                return HomeCollectionMetadata(
                    id = id,
                    title = title,
                    subtitle = subtitle,
                    query = title,
                    collectionType = providerCollectionType(kind),
                    artworkKey = "${kind}_$providerId",
                    maxItems = 100,
                    source = HomeCatalogSource.ProviderBrowse,
                    actionType = providerAction(kind),
                    providerId = providerId,
                    browseParams = params,
                )
            }
        }
        if (id.startsWith(QUERY_COLLECTION_PREFIX)) {
            val query = id.removePrefix(QUERY_COLLECTION_PREFIX).trim()
            if (query.isNotBlank()) {
                return HomeCollectionMetadata(
                    id = id,
                    title = query.replaceFirstChar { it.titlecase() },
                    subtitle = "From search history",
                    query = query,
                    collectionType = HomeCollectionType.TrendingSearch,
                    artworkKey = "history_${query.hashCode()}",
                    maxItems = 100,
                    source = HomeCatalogSource.UserHistory,
                    actionType = HomeActionType.OPEN_COLLECTION,
                )
            }
        }
        heroItems.firstOrNull { it.id == id }?.let { item ->
            return item.toCollectionMetadata(item.collectionType)
        }
        quickPicks.firstOrNull { it.id == id }?.let { item ->
            return item.toCollectionMetadata(item.collectionType)
        }
        freshDiscovery.items.firstOrNull { it.id == id }?.let { item ->
            return item.toCollectionMetadata(item.collectionType)
        }
        shelves.asSequence()
            .flatMap { it.items.asSequence() }
            .firstOrNull { it.id == id }
            ?.let { item -> return item.toCollectionMetadata(item.collectionType) }
        moodChips.firstOrNull { it.id == id }?.let { chip ->
            return chip.toCollectionMetadata()
        }
        genreGrid.firstOrNull { it.id == id }?.let { chip ->
            return chip.toCollectionMetadata(HomeCollectionType.Genre)
        }
        return null
    }

    private fun hero(id: String, title: String, subtitle: String, query: String) = HomeCarouselItem(
        id = "curated_hero_$id",
        title = title,
        subtitle = subtitle,
        query = query,
        artworkKey = id,
        collectionType = HomeCollectionType.Playlist,
        source = HomeCatalogSource.CuratedDefault,
    )

    private fun quick(id: String, title: String, subtitle: String, query: String) = QuickPickItem(
        id = "curated_quick_$id",
        title = title,
        subtitle = subtitle,
        query = query,
        artworkKey = id,
        collectionType = when {
            id.contains("arijit") -> HomeCollectionType.ArtistMix
            id.contains("lofi") || id.contains("workout") || id.contains("party") || id.contains("sad") -> HomeCollectionType.Mood
            else -> HomeCollectionType.QuickPick
        },
        actionType = if (id.contains("arijit")) HomeActionType.OPEN_ARTIST else HomeActionType.OPEN_COLLECTION,
        source = HomeCatalogSource.CuratedDefault,
    )

    fun queryCollectionId(query: String): String = QUERY_COLLECTION_PREFIX + query.trim()

    fun providerCollectionId(
        kind: String,
        providerId: String,
        title: String,
        subtitle: String = providerSubtitle(kind),
        params: String? = null,
    ): String = buildString {
        append(PROVIDER_COLLECTION_PREFIX)
        append(kind)
        append(PROVIDER_SEPARATOR)
        append(providerId)
        append(PROVIDER_SEPARATOR)
        append(title)
        append(PROVIDER_SEPARATOR)
        append(subtitle)
        if (!params.isNullOrBlank()) {
            append(PROVIDER_SEPARATOR)
            append(params)
        }
    }

    private fun providerSubtitle(kind: String): String = when (kind) {
        "artist" -> "Artist"
        "album" -> "Album"
        "playlist" -> "Playlist"
        "browse" -> "Browse"
        else -> "From provider"
    }

    private fun providerCollectionType(kind: String): HomeCollectionType = when (kind) {
        "artist" -> HomeCollectionType.ArtistMix
        "album" -> HomeCollectionType.NewReleases
        "browse" -> HomeCollectionType.Mood
        else -> HomeCollectionType.Playlist
    }

    private fun providerAction(kind: String): HomeActionType = when (kind) {
        "artist" -> HomeActionType.OPEN_ARTIST
        "album" -> HomeActionType.OPEN_ALBUM
        "playlist" -> HomeActionType.OPEN_PLAYLIST
        "browse" -> HomeActionType.OPEN_BROWSE
        else -> HomeActionType.OPEN_COLLECTION
    }

    private fun shelf(
        id: String,
        title: String,
        items: List<PlaylistShelfItem>,
        actionLabel: String? = "Explore",
    ) = HomeSection(
        id = id,
        title = title,
        actionLabel = actionLabel,
        items = items,
    )

    private fun card(id: String, title: String, subtitle: String, query: String) = PlaylistShelfItem(
        id = "curated_card_$id",
        title = title,
        subtitle = subtitle,
        query = query,
        artworkKey = id,
        collectionType = when {
            id.contains("mood") || id.contains("romance") || id.contains("sad") || id.contains("party") ||
                id.contains("focus") || id.contains("workout") || id.contains("rain") -> HomeCollectionType.Mood
            id.contains("hits") || id.contains("pop") || id.contains("bollywood") || id.contains("punjabi") ||
                id.contains("tamil") || id.contains("telugu") || id.contains("malayalam") -> HomeCollectionType.Genre
            id.contains("new") || id.contains("viral") -> HomeCollectionType.TrendingSearch
            else -> HomeCollectionType.Playlist
        },
        source = HomeCatalogSource.CuratedDefault,
    )

    private fun HomeCarouselItem.toCollectionMetadata(type: HomeCollectionType) = HomeCollectionMetadata(
        id = id,
        title = title,
        subtitle = subtitle,
        query = query.orEmpty(),
        collectionType = type,
        artworkKey = artworkKey ?: id,
        maxItems = maxItems,
        source = source,
        actionType = actionType,
    )

    private fun QuickPickItem.toCollectionMetadata(type: HomeCollectionType) = HomeCollectionMetadata(
        id = id,
        title = title,
        subtitle = subtitle,
        query = query.orEmpty(),
        collectionType = type,
        artworkKey = artworkKey ?: id,
        maxItems = maxItems,
        source = source,
        actionType = actionType,
    )

    private fun PlaylistShelfItem.toCollectionMetadata(type: HomeCollectionType) = HomeCollectionMetadata(
        id = id,
        title = title,
        subtitle = subtitle,
        query = query.orEmpty(),
        collectionType = type,
        artworkKey = artworkKey ?: id,
        maxItems = maxItems,
        source = source,
        actionType = actionType,
    )

    private fun MoodChip.toCollectionMetadata(
        type: HomeCollectionType = collectionType,
    ) = HomeCollectionMetadata(
        id = id,
        title = label,
        subtitle = "Made for exploring",
        query = query,
        collectionType = type,
        artworkKey = artworkKey,
        maxItems = maxItems,
        source = source,
        actionType = actionType,
    )
}
