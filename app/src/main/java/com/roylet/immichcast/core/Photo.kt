package com.roylet.immichcast.core

import kotlinx.serialization.Serializable

enum class MediaType { IMAGE, VIDEO }

/** A slideshow item, trimmed down from an Immich asset to what the screensaver needs. */
@Serializable
data class Photo(
    val id: String,
    val type: MediaType = MediaType.IMAGE,
    /** Local capture time as ISO-8601 (no zone), e.g. "2023-10-04T15:12:01". */
    val takenAt: String? = null,
    val isFavorite: Boolean = false,
    val mimeType: String? = null,
    val city: String? = null,
    val country: String? = null,
    val description: String? = null,
    val durationMs: Long? = null,
    /** Size as displayed, when Immich knows it. */
    val width: Int? = null,
    val height: Int? = null,
    /** Immich's projection type, e.g. "EQUIRECTANGULAR" for 360° content. */
    val projection: String? = null,
)
