package com.roylet.immichcast.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SlideshowConfigTest {

    @Test
    fun `defaults match the shipped experience`() {
        val config = SlideshowConfig()
        assertEquals(30, config.secondsPerSlide)
        assertTrue(config.overlayItems.isEmpty())
        assertEquals(MediaFilter.IMAGES_ONLY, config.mediaFilter)
        assertEquals(ImageQuality.PREVIEW, config.quality)
        assertEquals(SlideshowMode.SEASONAL, config.mode)
        assertEquals(SeasonSource.AUTOMATIC, config.seasonSource)
        assertTrue(config.recentDays in SlideshowConfig.RECENT_SPANS)
        assertEquals(Transition.CROSSFADE, config.transition)
        assertEquals(ImageFit.FIT_BLURRED, config.fit)
        assertEquals(0, config.dimPercent)
        assertFalse(config.isConfigured)
        assertTrue(config.secondsPerSlide in SlideshowConfig.SLIDE_DURATIONS)
        assertTrue(config.refreshHours in SlideshowConfig.REFRESH_INTERVALS)
    }

    @Test
    fun `only photo-selection settings change the playlist key`() {
        val base = SlideshowConfig(serverUrl = "http://x", apiKey = "k")
        assertEquals(base.playlistKey, base.copy(secondsPerSlide = 60, overlayItems = setOf(OverlayItem.CLOCK)).playlistKey)
        assertEquals(base.playlistKey, base.copy(apiKey = "other").playlistKey)
        assertNotEquals(base.playlistKey, base.copy(excludedAlbumIds = setOf("album")).playlistKey)
        assertNotEquals(base.playlistKey, base.copy(mediaFilter = MediaFilter.IMAGES_AND_VIDEOS).playlistKey)
        assertNotEquals(base.playlistKey, base.copy(seasonSource = SeasonSource.WINTER).playlistKey)
        assertNotEquals(base.playlistKey, base.copy(favoritesOnly = true).playlistKey)
        assertNotEquals(base.playlistKey, base.copy(mode = SlideshowMode.RANDOM).playlistKey)
        // Settings for other modes don't force a rebuild.
        assertEquals(base.playlistKey, base.copy(recentDays = 30).playlistKey)
        val recent = base.copy(mode = SlideshowMode.RECENT)
        assertNotEquals(recent.playlistKey, recent.copy(recentDays = 30).playlistKey)
        assertEquals(recent.playlistKey, recent.copy(seasonSource = SeasonSource.WINTER).playlistKey)
        assertEquals(
            base.copy(excludedAlbumIds = setOf("1", "2")).playlistKey,
            base.copy(excludedAlbumIds = setOf("2", "1")).playlistKey,
        )
    }
}
