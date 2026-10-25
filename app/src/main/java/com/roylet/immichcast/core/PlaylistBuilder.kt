package com.roylet.immichcast.core

import kotlin.random.Random

object PlaylistBuilder {

    /**
     * Turns raw search results into the slideshow order:
     * drops duplicates, anything in an excluded album, filtered media types and
     * (optionally) non-favorites, then applies the requested order.
     */
    fun build(
        candidates: List<Photo>,
        excludedAssetIds: Set<String>,
        mediaFilter: MediaFilter,
        favoritesOnly: Boolean,
        order: PlaybackOrder,
        random: Random = Random.Default,
    ): List<Photo> {
        val filtered = candidates
            .distinctBy { it.id }
            .filterNot { it.id in excludedAssetIds }
            .filter { mediaFilter == MediaFilter.IMAGES_AND_VIDEOS || it.type == MediaType.IMAGE }
            .filter { !favoritesOnly || it.isFavorite }
        return applyOrder(filtered, order, random)
    }

    fun applyOrder(photos: List<Photo>, order: PlaybackOrder, random: Random = Random.Default): List<Photo> =
        when (order) {
            PlaybackOrder.SHUFFLE -> photos.shuffled(random)
            // ISO local timestamps sort lexically; undated items go last.
            PlaybackOrder.OLDEST_FIRST -> photos.sortedWith(compareBy(nullsLast()) { it.takenAt })
            PlaybackOrder.NEWEST_FIRST -> photos.sortedWith(compareByDescending<Photo, String?>(nullsFirst()) { it.takenAt })
        }
}
