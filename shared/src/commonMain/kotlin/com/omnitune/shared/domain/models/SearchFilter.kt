package com.omnitune.shared.domain.models

import kotlinx.serialization.Serializable

@Serializable
enum class SearchFilter(val param: String?) {
    ALL(null),
    SONGS("EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D"),
    ALBUMS("EgWKAQIYAWoKEAkQChAFEAMQBA%3D%3D"),
    ARTISTS("EgWKAQIgAWoKEAkQChAFEAMQBA%3D%3D"),
    PLAYLISTS("EgeKAQQoADgBagwQDhAKEAMQBRAJEAQ%3D")
}
