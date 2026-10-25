package com.roylet.immichcast.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PlaylistBuilderTest {

    private val a = Photo("a", takenAt = "2020-10-01T10:00:00", isFavorite = true)
    private val b = Photo("b", takenAt = "2022-09-12T08:30:00")
    private val c = Photo("c", takenAt = "2021-11-30T23:59:00", isFavorite = true)
    private val video = Photo("v", type = MediaType.VIDEO, takenAt = "2023-10-10T12:00:00")
    private val undated = Photo("u")

    private fun build(
        photos: List<Photo>,
        excluded: Set<String> = emptySet(),
        media: MediaFilter = MediaFilter.IMAGES_ONLY,
        favorites: Boolean = false,
        order: PlaybackOrder = PlaybackOrder.OLDEST_FIRST,
    ) = PlaylistBuilder.build(photos, excluded, media, favorites, order, Random(42))

    @Test
    fun `excluded album assets are removed`() {
        val result = build(listOf(a, b, c), excluded = setOf("b"))
        assertEquals(listOf("a", "c"), result.map { it.id })
    }

    @Test
    fun `excluding wins even when the asset appears more than once`() {
        // The same asset can come back from several season-year queries.
        val result = build(listOf(a, b, b, c, a), excluded = setOf("a"))
        assertEquals(listOf("c", "b"), result.map { it.id })
    }

    @Test
    fun `no exclusions keeps everything once`() {
        val result = build(listOf(a, b, a, c))
        assertEquals(3, result.size)
        assertEquals(setOf("a", "b", "c"), result.map { it.id }.toSet())
    }

    @Test
    fun `videos only appear when allowed`() {
        assertEquals(listOf("a", "b"), build(listOf(a, video, b)).map { it.id })
        assertEquals(listOf("a", "b", "v"), build(listOf(a, video, b), media = MediaFilter.IMAGES_AND_VIDEOS).map { it.id })
    }

    @Test
    fun `favorites filter`() {
        assertEquals(listOf("a", "c"), build(listOf(a, b, c), favorites = true).map { it.id })
    }

    @Test
    fun `ordering puts undated items last`() {
        val photos = listOf(undated, b, a, c)
        assertEquals(listOf("a", "c", "b", "u"), build(photos, order = PlaybackOrder.OLDEST_FIRST).map { it.id })
        assertEquals(listOf("b", "c", "a", "u"), build(photos, order = PlaybackOrder.NEWEST_FIRST).map { it.id })
    }

    @Test
    fun `shuffle keeps the same items`() {
        val photos = (1..50).map { Photo("p$it") }
        val shuffled = build(photos, order = PlaybackOrder.SHUFFLE)
        assertEquals(photos.toSet(), shuffled.toSet())
        assertTrue(shuffled != photos)
    }
}
