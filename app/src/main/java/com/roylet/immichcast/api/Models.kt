package com.roylet.immichcast.api

import com.roylet.immichcast.core.MediaType
import com.roylet.immichcast.core.Photo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull

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
    val exifImageWidth: Int? = null,
    val exifImageHeight: Int? = null,
    /** EXIF orientation; Immich has sent it as both a string and a number. */
    val orientation: JsonPrimitive? = null,
    /** "EQUIRECTANGULAR" for 360° photos and videos, plus Google's partial panoramas. */
    val projectionType: String? = null,
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
    /** Immich 3+ sends milliseconds; older servers send "H:MM:SS.ffffff". */
    val duration: JsonPrimitive? = null,
    /** Displayed size, already rotated for orientation (Immich 2+). */
    val width: Int? = null,
    val height: Int? = null,
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
        width = displaySize()?.first,
        height = displaySize()?.second,
        projection = exifInfo?.projectionType?.takeIf { it.isNotBlank() }?.uppercase(),
    )

    /** Width and height as shown, from the asset or else from EXIF, turned for 90° orientations. */
    private fun displaySize(): Pair<Int, Int>? {
        if (width != null && height != null && width > 0 && height > 0) return width to height
        val w = exifInfo?.exifImageWidth ?: return null
        val h = exifInfo.exifImageHeight ?: return null
        if (w <= 0 || h <= 0) return null
        return if (exifInfo.orientation?.content in ROTATED_ORIENTATIONS) h to w else w to h
    }

    companion object {
        private val ROTATED_ORIENTATIONS = setOf("5", "6", "7", "8")

        fun parseDurationMs(raw: JsonPrimitive?): Long? {
            if (raw == null) return null
            if (!raw.isString) return raw.longOrNull?.takeIf { it > 0 }
            return parseDurationMs(raw.content)
        }

        /** Immich < 3 reports durations as "H:MM:SS.ffffff"; "0:00:00.00000" for stills. */
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
    val page: Int? = 1,
    /** Immich >= 3.2; replaces [page]. */
    val cursor: String? = null,
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
    /** Immich >= 3.2; replaces [nextPage]. */
    val nextCursor: String? = null,
)

@Serializable
data class SearchResponse(val assets: SearchAssetPage = SearchAssetPage())

@Serializable
data class TimeBucketDto(
    @SerialName("timeBucket") val timeBucket: String,
    val count: Int = 0,
)
