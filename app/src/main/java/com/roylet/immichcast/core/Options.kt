package com.roylet.immichcast.core

/** Every user-facing option, with the defaults the app ships with. */

enum class MediaFilter { IMAGES_ONLY, IMAGES_AND_VIDEOS }

/** What the slideshow draws from. */
enum class SlideshowMode {
    /** The current (or a chosen) season, across every year. */
    SEASONAL,

    /** The whole library, shuffled. */
    RANDOM,

    /** Photos from the last [SlideshowConfig.recentDays] days. */
    RECENT,

    /** Photos taken around today's date in previous years. */
    ON_THIS_DAY,

    /** Photos taken in the current calendar month, across every year. */
    THIS_MONTH,
}

enum class SeasonSource(val fixedSeason: Season?) {
    AUTOMATIC(null),
    SPRING(Season.SPRING),
    SUMMER(Season.SUMMER),
    AUTUMN(Season.AUTUMN),
    WINTER(Season.WINTER),
}

enum class PlaybackOrder { SHUFFLE, OLDEST_FIRST, NEWEST_FIRST }

enum class Transition { CROSSFADE, KEN_BURNS, NONE }

enum class ImageQuality { PREVIEW, FULL }

enum class ImageFit { FIT_BLURRED, FILL, FIT_BLACK }

enum class OverlayItem { CLOCK, DATE, LOCATION, ALBUM, DESCRIPTION, SEASON }

enum class OverlayPosition { BOTTOM_START, BOTTOM_END, TOP_START, TOP_END }

enum class ClockFormat { SYSTEM, H12, H24 }

data class SlideshowConfig(
    val serverUrl: String = "",
    val apiKey: String = "",
    val excludedAlbumIds: Set<String> = emptySet(),
    /** When non-empty, only photos in these albums are shown. */
    val includedAlbumIds: Set<String> = emptySet(),
    val mode: SlideshowMode = SlideshowMode.SEASONAL,
    val recentDays: Int = DEFAULT_RECENT_DAYS,
    val mediaFilter: MediaFilter = MediaFilter.IMAGES_ONLY,
    val seasonSource: SeasonSource = SeasonSource.AUTOMATIC,
    val favoritesOnly: Boolean = false,
    val order: PlaybackOrder = PlaybackOrder.SHUFFLE,
    val secondsPerSlide: Int = DEFAULT_SECONDS_PER_SLIDE,
    /** Stops each video after this long; 0 plays it to the end. */
    val videoSeconds: Int = 0,
    val transition: Transition = Transition.CROSSFADE,
    val quality: ImageQuality = ImageQuality.PREVIEW,
    val fit: ImageFit = ImageFit.FIT_BLURRED,
    /** Pan across wide panoramas and turn slowly through 360° photos and videos, instead of showing them flat. */
    val lookAround: Boolean = true,
    /** Seconds for one full turn of a 360° photo or video. */
    val turnSeconds: Int = DEFAULT_TURN_SECONDS,
    val overlayItems: Set<OverlayItem> = emptySet(),
    val overlayPosition: OverlayPosition = OverlayPosition.BOTTOM_START,
    val clockFormat: ClockFormat = ClockFormat.SYSTEM,
    val refreshHours: Int = DEFAULT_REFRESH_HOURS,
    val dimPercent: Int = 0,
) {
    val isConfigured: Boolean get() = serverUrl.isNotBlank() && apiKey.isNotBlank()

    /** Random means shuffled; the other modes follow the chosen order. */
    val effectiveOrder: PlaybackOrder get() = if (mode == SlideshowMode.RANDOM) PlaybackOrder.SHUFFLE else order

    /** Fields that change which photos are selected; a change invalidates the cached playlist. */
    val playlistKey: String
        get() = listOf(
            serverUrl.trimEnd('/'),
            excludedAlbumIds.sorted().joinToString(","),
            includedAlbumIds.sorted().joinToString(","),
            mediaFilter.name,
            mode.name,
            when (mode) {
                SlideshowMode.SEASONAL -> seasonSource.name
                SlideshowMode.RECENT -> recentDays.toString()
                else -> ""
            },
            favoritesOnly.toString(),
        ).joinToString("|")

    companion object {
        const val DEFAULT_SECONDS_PER_SLIDE = 30
        const val DEFAULT_REFRESH_HOURS = 12
        const val DEFAULT_RECENT_DAYS = 90
        val RECENT_SPANS = listOf(30, 90, 180, 365)
        val SLIDE_DURATIONS = listOf(10, 15, 20, 30, 45, 60, 120, 300)
        val VIDEO_DURATIONS = listOf(0, 10, 20, 30, 60)
        const val DEFAULT_TURN_SECONDS = 20
        val TURN_SPEEDS = listOf(60, 40, 20, 12)
        val REFRESH_INTERVALS = listOf(6, 12, 24)
        val DIM_LEVELS = listOf(0, 10, 20, 30, 40, 50, 60)
    }
}
