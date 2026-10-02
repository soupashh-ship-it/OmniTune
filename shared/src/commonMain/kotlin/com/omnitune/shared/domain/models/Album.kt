package com.omnitune.shared.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class Album(
    val title: String,
    val id: String? = null,
)
