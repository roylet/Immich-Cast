package com.roylet.immichcast.api

import com.roylet.immichcast.core.MediaType
import com.roylet.immichcast.core.Photo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServerVersionDto(val major: Int = 0, val minor: Int = 0, val patch: Int = 0) {
    override fun toString() = "$major.$minor.$patch"
}

@Serializable
data class UserDto(val id: String, val name: String = "", val email: String = "")

@Serializable
data class AlbumDto(
    val id: String,
    val albumName: String = "",
    val assetCount: Int = 0,
    val albumThumbnailAssetId: String? = null,
    val shared: Boolean = false,
    val assets: List<AssetDto> = emptyList(),
)

@Serializable
data class ExifDto(
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val description: String? = null,
)

@Serializable
data class AssetDto(
    val id: String,
    val type: String = "IMAGE",
    val originalMimeType: String? = null,
    val localDateTime: String? = null,
    val fileCreatedAt: String? = null,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val visibility: String? = null,
    val duration: String? = null,
    val exifInfo: ExifDto? = null,
) {
    /** Hidden/locked/archived/trashed assets never belong in the slideshow. */
    val isDisplayable: Boolean
        get() = !isTrashed && !isArchived && (visibility == null || visibility == "timeline")

    fun toPhoto(): Photo = Photo(
        id = id,
        type = if (type.equals("VIDEO", ignoreCase = true)) MediaType.VIDEO else MediaType.IMAGE,
        takenAt = (localDateTime ?: fileCreatedAt)?.take(19),
        isFavorite = isFavorite,
        mimeType = originalMimeType,
        city = exifInfo?.city?.takeIf { it.isNotBlank() },
        country = exifInfo?.country?.takeIf { it.isNotBlank() },
        description = exifInfo?.description?.takeIf { it.isNotBlank() },
        durationMs = parseDurationMs(duration),
    )

    companion object {
        /** Immich reports durations as "H:MM:SS.ffffff"; "0:00:00.00000" for stills. */
        fun parseDurationMs(raw: String?): Long? {
            val parts = raw?.split(":") ?: return null
            if (parts.size != 3) return null
            val hours = parts[0].toLongOrNull() ?: return null
            val minutes = parts[1].toLongOrNull() ?: return null
            val seconds = parts[2].toDoubleOrNull() ?: return null
            val total = hours * 3_600_000 + minutes * 60_000 + (seconds * 1000).toLong()
            return total.takeIf { it > 0 }
        }
    }
}

@Serializable
data class SearchMetadataRequest(
    val page: Int = 1,
    val size: Int = 1000,
    val type: String? = null,
    val takenAfter: String? = null,
    val takenBefore: String? = null,
    val albumIds: List<String>? = null,
    val isFavorite: Boolean? = null,
    /** Immich >= 1.133. */
    val visibility: String? = null,
    /** Immich < 1.133. */
    val withArchived: Boolean? = null,
    val withExif: Boolean? = null,
)

@Serializable
data class SearchAssetPage(
    val total: Int = 0,
    val count: Int = 0,
    val items: List<AssetDto> = emptyList(),
    val nextPage: String? = null,
)

@Serializable
data class SearchResponse(val assets: SearchAssetPage = SearchAssetPage())

@Serializable
data class TimeBucketDto(
    @SerialName("timeBucket") val timeBucket: String,
    val count: Int = 0,
)
