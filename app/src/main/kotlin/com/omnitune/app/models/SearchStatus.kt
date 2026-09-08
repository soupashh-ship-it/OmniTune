package com.omnitune.app.models

enum class SearchStatus {
    IDLE,
    LOADING,
    SEARCHING,
    SUCCESS,
    ERROR,
    EMPTY,
    ParserChanged,
    NoNetwork,
}

enum class SearchFilterTab {
    ALL,
    SONGS,
    VIDEOS,
    ALBUMS,
    ARTISTS,
    PLAYLISTS,
    FEATURED
}
