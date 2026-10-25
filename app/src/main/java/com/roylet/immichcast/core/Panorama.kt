package com.roylet.immichcast.core

/** How a slide is framed when "look around" is on. */
enum class Panorama {
    /** An ordinary photo or video. */
    NONE,

    /** A wide photo, panned across at full screen height. */
    WIDE,

    /** A full 360° sphere, viewed from inside and slowly turned. */
    SPHERE,
}

object Panoramas {
    /** Wider than this is panned; 21:9 photos and phone screenshots still fit well enough. */
    const val WIDE_ASPECT = 2.4f

    /** A full equirectangular sphere is 2:1; partial panoramas carry the same tag at other shapes. */
    private val SPHERE_ASPECT = 1.9f..2.1f

    fun of(photo: Photo): Panorama {
        val equirect = photo.projection == "EQUIRECTANGULAR"
        val aspect = aspect(photo)
        return when {
            // Video sizes are often missing, and only 360° cameras tag them.
            equirect && photo.type == MediaType.VIDEO -> Panorama.SPHERE
            photo.type == MediaType.VIDEO || aspect == null -> Panorama.NONE
            equirect && aspect in SPHERE_ASPECT -> Panorama.SPHERE
            aspect >= WIDE_ASPECT -> Panorama.WIDE
            else -> Panorama.NONE
        }
    }

    private fun aspect(photo: Photo): Float? {
        val w = photo.width ?: return null
        val h = photo.height?.takeIf { it > 0 } ?: return null
        return w.toFloat() / h
    }
}
