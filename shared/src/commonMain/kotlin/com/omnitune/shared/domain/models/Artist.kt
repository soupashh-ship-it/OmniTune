package com.omnitune.shared.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class Artist(
    val name: String,
    val id: String? = null,
)
