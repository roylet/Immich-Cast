package com.roylet.immichcast.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import com.roylet.immichcast.R
import com.roylet.immichcast.core.ClockFormat
import com.roylet.immichcast.core.Hemisphere
import com.roylet.immichcast.core.ImageFit
import com.roylet.immichcast.core.ImageQuality
import com.roylet.immichcast.core.MediaFilter
import com.roylet.immichcast.core.OverlayItem
import com.roylet.immichcast.core.OverlayPosition
import com.roylet.immichcast.core.PlaybackOrder
import com.roylet.immichcast.core.Season
import com.roylet.immichcast.core.SeasonSource
import com.roylet.immichcast.core.SlideshowMode
import com.roylet.immichcast.core.Transition
import java.time.format.TextStyle

@get:StringRes
val Season.labelRes: Int
    get() = when (this) {
        Season.SPRING -> R.string.season_spring
        Season.SUMMER -> R.string.season_summer
        Season.AUTUMN -> R.string.season_autumn
        Season.WINTER -> R.string.season_winter
    }

/** E.g. "September – November, every year", in the device's language. */
@Composable
fun Season.monthsLabel(hemisphere: Hemisphere): String {
    val months = months(hemisphere)
    val locale = LocalConfiguration.current.locales[0]
    return stringResource(
        R.string.season_months,
        months.first().getDisplayName(TextStyle.FULL_STANDALONE, locale),
        months.last().getDisplayName(TextStyle.FULL_STANDALONE, locale),
    )
}

@get:StringRes
val Hemisphere.labelRes: Int
    get() = when (this) {
        Hemisphere.NORTHERN -> R.string.hemisphere_northern
        Hemisphere.SOUTHERN -> R.string.hemisphere_southern
    }

val Season.accent: Color
    get() = when (this) {
        Season.SPRING -> Color(0xFF8BD17C)
        Season.SUMMER -> Color(0xFFFFC857)
        Season.AUTUMN -> Color(0xFFF08A4B)
        Season.WINTER -> Color(0xFF8EC5FF)
    }

@get:StringRes
val SeasonSource.labelRes: Int
    get() = when (this) {
        SeasonSource.AUTOMATIC -> R.string.season_source_automatic
        else -> fixedSeason!!.labelRes
    }

@get:StringRes
val SlideshowMode.labelRes: Int
    get() = when (this) {
        SlideshowMode.SEASONAL -> R.string.mode_seasonal
        SlideshowMode.RANDOM -> R.string.mode_random
        SlideshowMode.RECENT -> R.string.mode_recent
        SlideshowMode.ON_THIS_DAY -> R.string.mode_on_this_day
        SlideshowMode.THIS_MONTH -> R.string.mode_this_month
    }

@get:StringRes
val SlideshowMode.descriptionRes: Int
    get() = when (this) {
        SlideshowMode.SEASONAL -> R.string.mode_seasonal_desc
        SlideshowMode.RANDOM -> R.string.mode_random_desc
        SlideshowMode.RECENT -> R.string.mode_recent_desc
        SlideshowMode.ON_THIS_DAY -> R.string.mode_on_this_day_desc
        SlideshowMode.THIS_MONTH -> R.string.mode_this_month_desc
    }

/** Accent used for a mode's badge; seasonal uses the season's own colour. */
val SlideshowMode.accent: Color
    get() = when (this) {
        SlideshowMode.SEASONAL -> Color(0xFF8BD17C)
        SlideshowMode.RANDOM -> Color(0xFFFF8A5C)
        SlideshowMode.RECENT -> Color(0xFF8EC5FF)
        SlideshowMode.ON_THIS_DAY -> Color(0xFFFFC857)
        SlideshowMode.THIS_MONTH -> Color(0xFFC9A0FF)
    }

@get:StringRes
val MediaFilter.labelRes: Int
    get() = when (this) {
        MediaFilter.IMAGES_ONLY -> R.string.media_images_only
        MediaFilter.IMAGES_AND_VIDEOS -> R.string.media_images_and_videos
    }

@get:StringRes
val PlaybackOrder.labelRes: Int
    get() = when (this) {
        PlaybackOrder.SHUFFLE -> R.string.order_shuffle
        PlaybackOrder.OLDEST_FIRST -> R.string.order_oldest
        PlaybackOrder.NEWEST_FIRST -> R.string.order_newest
    }

@get:StringRes
val Transition.labelRes: Int
    get() = when (this) {
        Transition.CROSSFADE -> R.string.transition_crossfade
        Transition.KEN_BURNS -> R.string.transition_ken_burns
        Transition.NONE -> R.string.transition_none
    }

@get:StringRes
val ImageQuality.labelRes: Int
    get() = when (this) {
        ImageQuality.PREVIEW -> R.string.quality_preview
        ImageQuality.FULL -> R.string.quality_full
    }

@get:StringRes
val ImageQuality.descriptionRes: Int
    get() = when (this) {
        ImageQuality.PREVIEW -> R.string.quality_preview_desc
        ImageQuality.FULL -> R.string.quality_full_desc
    }

@get:StringRes
val ImageFit.labelRes: Int
    get() = when (this) {
        ImageFit.FIT_BLURRED -> R.string.fit_blurred
        ImageFit.FILL -> R.string.fit_fill
        ImageFit.FIT_BLACK -> R.string.fit_black
    }

@get:StringRes
val OverlayItem.labelRes: Int
    get() = when (this) {
        OverlayItem.CLOCK -> R.string.overlay_clock
        OverlayItem.DATE -> R.string.overlay_date
        OverlayItem.LOCATION -> R.string.overlay_location
        OverlayItem.ALBUM -> R.string.overlay_album
        OverlayItem.DESCRIPTION -> R.string.overlay_description
        OverlayItem.SEASON -> R.string.overlay_season
    }

@get:StringRes
val OverlayPosition.labelRes: Int
    get() = when (this) {
        OverlayPosition.BOTTOM_START -> R.string.position_bottom_start
        OverlayPosition.BOTTOM_END -> R.string.position_bottom_end
        OverlayPosition.TOP_START -> R.string.position_top_start
        OverlayPosition.TOP_END -> R.string.position_top_end
    }

@get:StringRes
val ClockFormat.labelRes: Int
    get() = when (this) {
        ClockFormat.SYSTEM -> R.string.clock_system
        ClockFormat.H12 -> R.string.clock_12
        ClockFormat.H24 -> R.string.clock_24
    }
