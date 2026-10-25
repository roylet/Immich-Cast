package com.roylet.immichcast.core

import org.junit.Assert.assertEquals
import org.junit.Test

class PanoramaTest {

    private fun photo(w: Int?, h: Int?, projection: String? = null, type: MediaType = MediaType.IMAGE) =
        Photo(id = "p", type = type, width = w, height = h, projection = projection)

    @Test
    fun `full equirectangular images are spheres`() {
        assertEquals(Panorama.SPHERE, Panoramas.of(photo(8000, 4000, "EQUIRECTANGULAR")))
        assertEquals(Panorama.SPHERE, Panoramas.of(photo(6080, 3040, "EQUIRECTANGULAR")))
    }

    @Test
    fun `partial panoramas tagged equirectangular are panned, or shown normally when not wide`() {
        assertEquals(Panorama.WIDE, Panoramas.of(photo(8664, 1628, "EQUIRECTANGULAR")))
        assertEquals(Panorama.NONE, Panoramas.of(photo(1936, 1696, "EQUIRECTANGULAR")))
    }

    @Test
    fun `wide flat photos are panned but ordinary wide shots are not`() {
        assertEquals(Panorama.WIDE, Panoramas.of(photo(7948, 1349)))
        assertEquals(Panorama.NONE, Panoramas.of(photo(4624, 2080))) // 20:9
        assertEquals(Panorama.NONE, Panoramas.of(photo(2960, 1440))) // phone screenshot
        assertEquals(Panorama.NONE, Panoramas.of(photo(1920, 1080)))
        assertEquals(Panorama.NONE, Panoramas.of(photo(null, null)))
    }

    @Test
    fun `360 videos are spheres even without a size`() {
        assertEquals(Panorama.SPHERE, Panoramas.of(photo(null, null, "EQUIRECTANGULAR", MediaType.VIDEO)))
        assertEquals(Panorama.NONE, Panoramas.of(photo(3840, 1080, type = MediaType.VIDEO)))
    }
}
