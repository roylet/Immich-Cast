package com.roylet.immichcast.dream

import android.graphics.drawable.Drawable
import android.util.Log
import coil.imageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Precision
import coil.size.Scale
import com.roylet.immichcast.ImmichCastApp
import com.roylet.immichcast.R
import com.roylet.immichcast.api.ImmichApi
import com.roylet.immichcast.api.ImmichException
import com.roylet.immichcast.core.Hemisphere
import com.roylet.immichcast.core.ImageFit
import com.roylet.immichcast.core.ImageQuality
import com.roylet.immichcast.core.MediaType
import com.roylet.immichcast.core.OverlayItem
import com.roylet.immichcast.core.Panorama
import com.roylet.immichcast.core.Panoramas
import com.roylet.immichcast.core.Photo
import com.roylet.immichcast.core.PlaybackOrder
import com.roylet.immichcast.core.PlaylistBuilder
import com.roylet.immichcast.core.Season
import com.roylet.immichcast.core.Seasons
import com.roylet.immichcast.core.SlideshowConfig
import com.roylet.immichcast.data.Playlist
import com.roylet.immichcast.data.localDate
import com.roylet.immichcast.ui.labelRes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal data class LoadedSlide(
    val photo: Photo,
    val drawable: Drawable?,
    val caption: SlideCaption,
    val kind: Panorama = Panorama.NONE,
)

/** Drives a [SlideshowView]: loads the playlist, preloads the next slide, and paces the show. */
internal class SlideshowController(
    private val app: ImmichCastApp,
    private val view: SlideshowView,
    private val scope: CoroutineScope,
) {
    private val config: SlideshowConfig = app.settings.current
    private val repo = app.playlists
    private val api: ImmichApi? = runCatching { app.api(config) }.getOrNull()
    private val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)

    private var pending: Playlist? = null
    private var refreshJob: Job? = null
    private var lastRefreshAttempt = 0L

    fun start() {
        scope.launch { run() }
    }

    private suspend fun run() {
        view.applyConfig(config)
        if (!config.isConfigured || api == null) {
            view.showMessage(str(R.string.dream_not_setup_title), str(R.string.dream_not_setup_body), busy = false)
            return
        }
        var playlist = loadInitialPlaylist()
        if (playlist.stats.fellBackToAll) view.showNotice(str(R.string.dream_notice_fallback))
        while (playlist.photos.isEmpty()) {
            view.showMessage(str(R.string.dream_empty_title), str(R.string.dream_empty_body), busy = false)
            delay(RETRY_MS * 4)
            playlist = runCatching { repo.refresh(config) }.getOrDefault(playlist)
        }
        slideLoop(playlist)
    }

    /** Cached playlist first (instant start), rebuilt in the background if old; network otherwise. */
    private suspend fun loadInitialPlaylist(): Playlist {
        repo.cached(config)?.takeIf { it.photos.isNotEmpty() }?.let { cached ->
            if (repo.isStale(cached, config)) refreshInBackground()
            return cached
        }
        view.showMessage(str(R.string.dream_loading_title), str(R.string.dream_loading_body), busy = true)
        var attempt = 0
        while (true) {
            try {
                return repo.refresh(config)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Playlist load failed", e)
                view.showMessage(str(R.string.dream_error_title), describe(e), busy = true)
                delay(RETRY_MS * (1L shl attempt.coerceAtMost(4)))
                attempt++
            }
        }
    }

    private fun refreshInBackground() {
        val now = System.currentTimeMillis()
        if (refreshJob?.isActive == true || now - lastRefreshAttempt < MIN_REFRESH_GAP_MS) return
        lastRefreshAttempt = now
        refreshJob = scope.launch {
            try {
                pending = repo.refresh(config)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Background refresh failed; keeping current playlist", e)
            }
        }
    }

    private suspend fun slideLoop(initial: Playlist) {
        // The cached playlist keeps the order it was shuffled in when built; reshuffle per start
        // so each screensaver session doesn't open on the same photos.
        var queue = if (config.effectiveOrder == PlaybackOrder.SHUFFLE) {
            PlaylistBuilder.applyOrder(initial.photos, PlaybackOrder.SHUFFLE)
        } else {
            initial.photos
        }
        var key = initial.key
        var index = 0
        var failures = 0
        var next: Deferred<LoadedSlide?> = scope.async { load(queue[index]) }

        while (true) {
            val slide = next.await()

            // Pick the following item. A refreshed playlist takes over straight away rather than
            // after a full pass, which can take a day for a large library.
            val shown = queue[index]
            val fresh = pending?.takeIf { it.photos.isNotEmpty() }
            pending = null
            if (fresh != null) {
                queue = fresh.photos
                key = fresh.key
                // Shuffled playlists start on the fresh shuffle; ordered ones carry on after the photo just shown.
                index = if (config.effectiveOrder == PlaybackOrder.SHUFFLE) 0 else queue.indexOfFirst { it.id == shown.id } + 1
                if (index >= queue.size) index = 0
            } else if (++index >= queue.size) {
                index = 0
                if (config.effectiveOrder == PlaybackOrder.SHUFFLE) queue = PlaylistBuilder.applyOrder(queue, PlaybackOrder.SHUFFLE)
            }
            // Season or day rolled over, or the playlist is due a refresh.
            if (key != repo.cacheKey(config) || repo.playlist.value?.let { repo.isStale(it, config) } != false) {
                refreshInBackground()
            }
            val upcoming = queue[index]
            next = scope.async { load(upcoming) }

            if (slide == null) {
                failures++
                if (failures >= minOf(queue.size, MAX_CONSECUTIVE_FAILURES)) {
                    view.showMessage(str(R.string.dream_offline_title), str(R.string.dream_offline_body), busy = true)
                    delay(RETRY_MS)
                    failures = 0
                }
                continue
            }
            failures = 0
            // Panoramas and 360° photos need longer to look around than a normal photo.
            val slideMs = if (slide.kind == Panorama.NONE) config.secondsPerSlide * 1000L
            else maxOf(config.secondsPerSlide * 1000L, LOOK_AROUND_MS)
            view.showSlide(slide, config, slideMs)

            if (slide.photo.type == MediaType.VIDEO && api != null) {
                val max = (slide.photo.durationMs ?: MAX_VIDEO_MS).coerceIn(config.secondsPerSlide * 1000L, MAX_VIDEO_MS) + 2_000
                val limit = config.videoSeconds.takeIf { it > 0 }?.times(1000L)
                view.playVideo(api.videoUrl(slide.photo.id), config.fit, max, limit, inSphere = slide.kind == Panorama.SPHERE)
            } else {
                delay(slideMs)
            }
        }
    }

    private suspend fun load(photo: Photo): LoadedSlide? {
        val api = api ?: return null
        val kind = if (config.lookAround) Panoramas.of(photo) else Panorama.NONE
        if (kind != Panorama.NONE) {
            loadImage(lookAroundUrl(api, photo), kind)?.let { return LoadedSlide(photo, it, caption(api, photo), kind) }
            Log.d(TAG, "Showing ${photo.id} flat: full-size image failed to load")
        }
        val drawable = loadImage(primaryUrl(api, photo)) ?: if (config.quality == ImageQuality.FULL) {
            loadImage(api.thumbnailUrl(photo.id, "preview"))
        } else null
        if (drawable == null) {
            Log.w(TAG, "Skipping asset ${photo.id}: image failed to load")
            return null
        }
        return LoadedSlide(photo, drawable, caption(api, photo))
    }

    private fun primaryUrl(api: ImmichApi, photo: Photo): String = when {
        config.quality == ImageQuality.PREVIEW || photo.type == MediaType.VIDEO -> api.thumbnailUrl(photo.id, "preview")
        else -> fullSizeUrl(api, photo)
    }

    /** Panning and turning show a slice of the image at a time, so the preview size is too soft. */
    private fun lookAroundUrl(api: ImmichApi, photo: Photo): String =
        if (photo.type == MediaType.VIDEO) api.thumbnailUrl(photo.id, "preview") else fullSizeUrl(api, photo)

    private fun fullSizeUrl(api: ImmichApi, photo: Photo): String =
        if (photo.mimeType?.lowercase() in DECODABLE_ORIGINALS) api.originalUrl(photo.id)
        // RAW and other formats Android cannot decode: Immich's full-size JPEG rendition.
        else api.thumbnailUrl(photo.id, "fullsize")

    private suspend fun loadImage(url: String, kind: Panorama = Panorama.NONE): Drawable? {
        val request = ImageRequest.Builder(app).data(url).apply {
            when (kind) {
                Panorama.NONE -> size(view.targetWidth, view.targetHeight)
                    .precision(Precision.INEXACT)
                    .scale(if (config.fit == ImageFit.FILL) Scale.FILL else Scale.FIT)
                // Full screen height, as wide as that makes it, within what the GPU can draw.
                Panorama.WIDE -> size(MAX_PAN_WIDTH, view.targetHeight).precision(Precision.EXACT).scale(Scale.FIT)
                // Uploaded to OpenGL, so it must be a plain bitmap.
                Panorama.SPHERE -> size(SPHERE_WIDTH, SPHERE_WIDTH / 2).precision(Precision.EXACT).scale(Scale.FIT)
                    .allowHardware(false)
            }
        }.build()
        return when (val result = app.imageLoader.execute(request)) {
            is SuccessResult -> result.drawable
            is ErrorResult -> {
                Log.d(TAG, "Image load failed: $url", result.throwable)
                null
            }
        }
    }

    private suspend fun caption(api: ImmichApi, photo: Photo): SlideCaption {
        val items = config.overlayItems
        if (items.isEmpty()) return SlideCaption()
        val date = photo.localDate()
        val album = if (OverlayItem.ALBUM in items) {
            runCatching { api.albumsContaining(photo.id).map { it.albumName }.filter { it.isNotBlank() } }
                .getOrDefault(emptyList()).take(2).joinToString(" · ").ifBlank { null }
        } else null
        return SlideCaption(
            date = date?.format(dateFormat),
            location = listOfNotNull(photo.city, photo.country).distinct().joinToString(", ").ifBlank { null },
            album = album,
            description = photo.description,
            season = date?.let {
                val hemisphere = repo.hemisphere(config)
                seasonCaption(Season.of(it, hemisphere), Seasons.seasonYear(it, hemisphere), hemisphere)
            },
        )
    }

    private fun seasonCaption(season: Season, year: Int, hemisphere: Hemisphere): String {
        val years = if (season.spansNewYear(hemisphere)) "$year–%02d".format((year + 1) % 100) else year.toString()
        return "${str(season.labelRes)} $years"
    }

    private fun describe(e: Throwable): String = when (e) {
        is ImmichException.Unauthorized -> str(R.string.error_unauthorized)
        is ImmichException.Forbidden -> str(R.string.error_forbidden)
        is ImmichException.Unreachable -> str(R.string.error_unreachable, config.serverUrl)
        is ImmichException.InvalidUrl -> str(R.string.error_invalid_url)
        else -> str(R.string.error_generic, e.message ?: e.javaClass.simpleName)
    }

    private fun str(res: Int, vararg args: Any): String = app.getString(res, *args)

    private companion object {
        const val TAG = "ImmichCast"
        const val RETRY_MS = 15_000L
        const val MAX_CONSECUTIVE_FAILURES = 8
        const val MAX_VIDEO_MS = 120_000L
        const val MIN_REFRESH_GAP_MS = 10 * 60_000L

        /** Least time a panorama or 360° photo stays up: one pan across, or one and a half turns. */
        const val LOOK_AROUND_MS = 30_000L
        const val MAX_PAN_WIDTH = 8192
        const val SPHERE_WIDTH = 4096
        val DECODABLE_ORIGINALS = setOf("image/jpeg", "image/jpg", "image/png", "image/webp", "image/heic", "image/heif", "image/avif", "image/bmp")
    }
}
