package com.roylet.immichcast.data

import com.roylet.immichcast.core.Hemisphere
import com.roylet.immichcast.core.Hemispheres
import java.util.Locale
import java.util.TimeZone

/**
 * Works out which hemisphere the TV is in. The time zone is set from the network, so its
 * country is the best guess; the language region is the fallback, then the north, where most
 * people live.
 */
object HemisphereDetector {

    fun detect(): Hemisphere {
        val zoneCountry = runCatching { android.icu.util.TimeZone.getRegion(TimeZone.getDefault().id) }.getOrNull()
        return Hemispheres.forCountry(zoneCountry)
            ?: Hemispheres.forCountry(Locale.getDefault().country)
            ?: Hemisphere.NORTHERN
    }
}
