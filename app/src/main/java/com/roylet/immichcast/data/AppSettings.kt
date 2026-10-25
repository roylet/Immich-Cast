package com.roylet.immichcast.data

import android.content.Context
import androidx.core.content.edit
import com.roylet.immichcast.core.ClockFormat
import com.roylet.immichcast.core.ImageFit
import com.roylet.immichcast.core.ImageQuality
import com.roylet.immichcast.core.MediaFilter
import com.roylet.immichcast.core.OverlayItem
import com.roylet.immichcast.core.OverlayPosition
import com.roylet.immichcast.core.PlaybackOrder
import com.roylet.immichcast.core.SeasonSource
import com.roylet.immichcast.core.SlideshowConfig
import com.roylet.immichcast.core.Transition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Persists [SlideshowConfig] in SharedPreferences and exposes it as a flow. */
class AppSettings(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(read())

    val config: StateFlow<SlideshowConfig> = state.asStateFlow()

    val current: SlideshowConfig get() = state.value

    fun update(transform: (SlideshowConfig) -> SlideshowConfig) {
        val next = transform(state.value)
        if (next == state.value) return
        write(next)
        state.value = next
    }

    fun reset() {
        prefs.edit { clear() }
        state.value = SlideshowConfig()
    }

    private fun read(): SlideshowConfig {
        val d = SlideshowConfig()
        return SlideshowConfig(
            serverUrl = prefs.getString(K_URL, d.serverUrl).orEmpty(),
            apiKey = prefs.getString(K_KEY, d.apiKey).orEmpty(),
            excludedAlbumIds = prefs.getStringSet(K_EXCLUDED, d.excludedAlbumIds).orEmpty().toSet(),
            includedAlbumIds = prefs.getStringSet(K_INCLUDED, d.includedAlbumIds).orEmpty().toSet(),
            mediaFilter = enumPref(K_MEDIA, d.mediaFilter),
            mode = enumPref(K_MODE, d.mode),
            recentDays = prefs.getInt(K_RECENT, d.recentDays),
            seasonSource = enumPref(K_SEASON, d.seasonSource),
            favoritesOnly = prefs.getBoolean(K_FAVORITES, d.favoritesOnly),
            order = enumPref(K_ORDER, d.order),
            secondsPerSlide = prefs.getInt(K_SECONDS, d.secondsPerSlide),
            videoSeconds = prefs.getInt(K_VIDEO_SECONDS, d.videoSeconds),
            transition = enumPref(K_TRANSITION, d.transition),
            quality = enumPref(K_QUALITY, d.quality),
            fit = enumPref(K_FIT, d.fit),
            lookAround = prefs.getBoolean(K_LOOK_AROUND, d.lookAround),
            turnSeconds = prefs.getInt(K_TURN_SECONDS, d.turnSeconds),
            overlayItems = prefs.getStringSet(K_OVERLAY, null)
                ?.mapNotNull { name -> OverlayItem.entries.firstOrNull { it.name == name } }?.toSet()
                ?: d.overlayItems,
            overlayPosition = enumPref(K_OVERLAY_POS, d.overlayPosition),
            clockFormat = enumPref(K_CLOCK, d.clockFormat),
            refreshHours = prefs.getInt(K_REFRESH, d.refreshHours),
            dimPercent = prefs.getInt(K_DIM, d.dimPercent),
        )
    }

    private fun write(c: SlideshowConfig) = prefs.edit {
        putString(K_URL, c.serverUrl)
        putString(K_KEY, c.apiKey)
        putStringSet(K_EXCLUDED, c.excludedAlbumIds)
        putStringSet(K_INCLUDED, c.includedAlbumIds)
        putString(K_MEDIA, c.mediaFilter.name)
        putString(K_MODE, c.mode.name)
        putInt(K_RECENT, c.recentDays)
        putString(K_SEASON, c.seasonSource.name)
        putBoolean(K_FAVORITES, c.favoritesOnly)
        putString(K_ORDER, c.order.name)
        putInt(K_SECONDS, c.secondsPerSlide)
        putInt(K_VIDEO_SECONDS, c.videoSeconds)
        putString(K_TRANSITION, c.transition.name)
        putString(K_QUALITY, c.quality.name)
        putString(K_FIT, c.fit.name)
        putBoolean(K_LOOK_AROUND, c.lookAround)
        putInt(K_TURN_SECONDS, c.turnSeconds)
        putStringSet(K_OVERLAY, c.overlayItems.map { it.name }.toSet())
        putString(K_OVERLAY_POS, c.overlayPosition.name)
        putString(K_CLOCK, c.clockFormat.name)
        putInt(K_REFRESH, c.refreshHours)
        putInt(K_DIM, c.dimPercent)
    }

    private inline fun <reified E : Enum<E>> enumPref(key: String, default: E): E {
        val name = prefs.getString(key, null) ?: return default
        return enumValues<E>().firstOrNull { it.name == name } ?: default
    }

    private companion object {
        const val K_URL = "server_url"
        const val K_KEY = "api_key"
        const val K_EXCLUDED = "excluded_album_ids"
        const val K_INCLUDED = "included_album_ids"
        const val K_MEDIA = "media_filter"
        const val K_MODE = "mode"
        const val K_RECENT = "recent_days"
        const val K_SEASON = "season_source"
        const val K_FAVORITES = "favorites_only"
        const val K_ORDER = "order"
        const val K_SECONDS = "seconds_per_slide"
        const val K_VIDEO_SECONDS = "video_seconds"
        const val K_TRANSITION = "transition"
        const val K_QUALITY = "quality"
        const val K_FIT = "fit"
        const val K_LOOK_AROUND = "look_around"
        const val K_TURN_SECONDS = "turn_seconds"
        const val K_OVERLAY = "overlay_items"
        const val K_OVERLAY_POS = "overlay_position"
        const val K_CLOCK = "clock_format"
        const val K_REFRESH = "refresh_hours"
        const val K_DIM = "dim_percent"
    }
}

