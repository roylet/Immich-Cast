package com.roylet.immichcast.data

import android.util.Log
import com.roylet.immichcast.api.AssetDto
import com.roylet.immichcast.api.ImmichApi
import com.roylet.immichcast.api.ImmichException
import com.roylet.immichcast.api.SearchMetadataRequest
import com.roylet.immichcast.core.DateRange
import com.roylet.immichcast.core.MediaFilter
import com.roylet.immichcast.core.Modes
import com.roylet.immichcast.core.Photo
import com.roylet.immichcast.core.PlaylistBuilder
import com.roylet.immichcast.core.Season
import com.roylet.immichcast.core.SeasonSource
import com.roylet.immichcast.core.Seasons
import com.roylet.immichcast.core.SlideshowConfig
import com.roylet.immichcast.core.SlideshowMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File
import java.time.LocalDate
import java.time.ZoneOffset

@Serializable
data class PlaylistStats(
    val mode: SlideshowMode = SlideshowMode.SEASONAL,
    val season: Season? = null,
    /** Distinct years the photos come from (seasonal and on-this-day modes). */
    val years: Int = 0,
    val excludedCount: Int = 0,
    /** Albums the slideshow is limited to; 0 means the whole library. */
    val includedAlbums: Int = 0,
    val total: Int = 0,
    val fellBackToAll: Boolean = false,
    val builtAtMillis: Long = 0,
)

@Serializable
data class Playlist(
    val key: String,
    val photos: List<Photo>,
    val stats: PlaylistStats,
)

/**
 * Builds the slideshow playlist from Immich and caches it on disk so the screensaver
 * can start instantly (and keep working offline) while a fresh copy loads.
 */
class PlaylistRepository(
    private val cacheFile: File,
    private val apiFor: (SlideshowConfig) -> ImmichApi,
    private val today: () -> LocalDate = { LocalDate.now() },
    private val now: () -> Long = { System.currentTimeMillis() },
    private val log: (message: String, error: Throwable?) -> Unit = { message, error -> Log.i(TAG, message, error) },
) {
    private val mutex = Mutex()
    private val latest = MutableStateFlow<Playlist?>(null)

    /** The most recently loaded or built playlist, for status display. */
    val playlist: StateFlow<Playlist?> = latest.asStateFlow()

    fun effectiveSeason(config: SlideshowConfig): Season? = Modes.season(config, today())

    /**
     * Identifies the photo selection. Besides the settings it includes whatever moves with
     * the calendar: the season, the month, or today's date for the recent and on-this-day modes.
     */
    fun cacheKey(config: SlideshowConfig): String {
        val moving = when (config.mode) {
            SlideshowMode.SEASONAL -> effectiveSeason(config)?.name
            SlideshowMode.THIS_MONTH -> today().withDayOfMonth(1).toString()
            SlideshowMode.RECENT, SlideshowMode.ON_THIS_DAY -> today().toString()
            SlideshowMode.RANDOM -> null
        }
        return "${config.playlistKey}|${moving.orEmpty()}"
    }

    /** Cached playlist for this exact configuration and date, regardless of age. */
    suspend fun cached(config: SlideshowConfig): Playlist? = withContext(Dispatchers.IO) {
        val playlist = latest.value?.takeIf { it.key == cacheKey(config) } ?: readCache()
        playlist?.takeIf { it.key == cacheKey(config) }?.also { latest.value = it }
    }

    fun isStale(playlist: Playlist, config: SlideshowConfig): Boolean =
        now() - playlist.stats.builtAtMillis > config.refreshHours * 3_600_000L

    /** Returns a usable playlist: fresh cache if available, otherwise rebuilt from the server. */
    suspend fun get(config: SlideshowConfig, forceRefresh: Boolean = false): Playlist {
        if (!forceRefresh) cached(config)?.takeUnless { isStale(it, config) }?.let { return it }
        return refresh(config)
    }

    suspend fun refresh(config: SlideshowConfig): Playlist = mutex.withLock {
        val api = apiFor(config)
        val season = effectiveSeason(config)
        val excluded = excludedAssetIds(api, config.excludedAlbumIds)
        var photos = build(api, config, selection(api, config, season), excluded)
        var fellBack = false
        // A fixed season the user picked stays as asked; anything that follows the calendar can come up empty.
        val followsCalendar = config.mode != SlideshowMode.RANDOM &&
            !(config.mode == SlideshowMode.SEASONAL && config.seasonSource != SeasonSource.AUTOMATIC)
        if (photos.isEmpty() && followsCalendar) {
            log("Nothing for ${config.mode} today, falling back to the whole library", null)
            photos = build(api, config, null, excluded)
            fellBack = true
        }
        val years = if (fellBack || config.mode == SlideshowMode.RANDOM || config.mode == SlideshowMode.RECENT) 0 else {
            photos.mapNotNull { p -> p.localDate()?.let { if (season != null) Seasons.seasonYear(it) else it.year } }.distinct().size
        }
        val playlist = Playlist(
            key = cacheKey(config),
            photos = photos,
            stats = PlaylistStats(
                mode = config.mode,
                season = season,
                years = years,
                excludedCount = excluded.size,
                includedAlbums = config.includedAlbumIds.size,
                total = photos.size,
                fellBackToAll = fellBack,
                builtAtMillis = now(),
            ),
        )
        withContext(Dispatchers.IO) { writeCache(playlist) }
        latest.value = playlist
        log("Playlist built: ${photos.size} items, mode=${config.mode}, season=$season, excluded=${excluded.size}", null)
        playlist
    }

    suspend fun clear() = mutex.withLock {
        latest.value = null
        withContext(Dispatchers.IO) { cacheFile.delete() }
    }

    /** Date ranges to search for this mode; null means the whole library. */
    private suspend fun selection(api: ImmichApi, config: SlideshowConfig, season: Season?): List<DateRange>? =
        when (config.mode) {
            SlideshowMode.RANDOM -> null
            SlideshowMode.RECENT -> listOf(Modes.recent(today(), config.recentDays))
            SlideshowMode.SEASONAL -> {
                val thisSeasonYear = Seasons.seasonYear(today())
                val years = timelineMonths(api)?.let { Seasons.seasonYearsWithContent(season!!, it) }
                    ?: (thisSeasonYear downTo FALLBACK_FIRST_YEAR).toList()
                years.filter { it <= thisSeasonYear }.map { Seasons.range(season!!, it) }
            }
            SlideshowMode.ON_THIS_DAY -> {
                val years = timelineMonths(api)?.let(Modes::yearsWithContent)
                    ?: (today().year - 1 downTo FALLBACK_FIRST_YEAR).toList()
                Modes.onThisDay(today(), years)
            }
            SlideshowMode.THIS_MONTH -> {
                val years = timelineMonths(api)?.filter { it.month == today().month }?.let(Modes::yearsWithContent)
                    ?: (today().year downTo FALLBACK_FIRST_YEAR).toList()
                Modes.thisMonth(today(), years)
            }
        }

    private suspend fun build(api: ImmichApi, config: SlideshowConfig, ranges: List<DateRange>?, excluded: Set<String>): List<Photo> {
        val base = SearchMetadataRequest(
            type = if (config.mediaFilter == MediaFilter.IMAGES_ONLY) "IMAGE" else null,
            isFavorite = if (config.favoritesOnly) true else null,
            withExif = true,
        )
        val candidates = if (config.includedAlbumIds.isNotEmpty()) {
            // Album searches are small, so fetch each included album once and apply the dates here.
            config.includedAlbumIds.flatMap { albumAssets(api, it, base, displayableOnly = true) }
                .map { it.toPhoto() }
                .filter { photo -> ranges == null || photo.localDate()?.let { date -> ranges.any { date in it } } == true }
        } else if (ranges == null) {
            searchAll(api, base).map { it.toPhoto() }
        } else {
            ranges.flatMap { range ->
                // Immich filters on UTC; pad by a day and filter on local time below.
                searchAll(
                    api,
                    base.copy(
                        takenAfter = isoUtc(range.start.minusDays(1)),
                        takenBefore = isoUtc(range.endExclusive.plusDays(1)),
                    ),
                ).map { it.toPhoto() }.filter { photo -> photo.localDate()?.let { it in range } ?: true }
            }
        }
        return PlaylistBuilder.build(candidates, excluded, config.mediaFilter, config.favoritesOnly, config.effectiveOrder)
    }

    /** Months that have photos, from the timeline, so empty years cost no requests. Null if unavailable. */
    private suspend fun timelineMonths(api: ImmichApi): List<LocalDate>? = try {
        api.timeBuckets().filter { it.count > 0 }.mapNotNull {
            runCatching { LocalDate.parse(it.timeBucket.take(10)) }.getOrNull()
        }
    } catch (e: ImmichException) {
        if (e is ImmichException.Unauthorized || e is ImmichException.Unreachable) throw e
        log("Timeline buckets unavailable, scanning every year", e)
        null
    }

    private suspend fun excludedAssetIds(api: ImmichApi, albumIds: Set<String>): Set<String> {
        val ids = HashSet<String>()
        for (albumId in albumIds) {
            albumAssets(api, albumId, SearchMetadataRequest(), displayableOnly = false).mapTo(ids) { it.id }
        }
        return ids
    }

    /** Assets in [albumId] matching [request]; empty if the album has since been deleted. */
    private suspend fun albumAssets(
        api: ImmichApi,
        albumId: String,
        request: SearchMetadataRequest,
        displayableOnly: Boolean,
    ): List<AssetDto> = try {
        searchAll(api, request.copy(albumIds = listOf(albumId)), displayableOnly)
    } catch (e: ImmichException.Http) {
        if (e.code == 404) {
            emptyList()
        } else {
            // Older servers cannot filter search by album; read the album directly.
            api.album(albumId).assets.filter { !displayableOnly || it.isDisplayable }
        }
    }

    private var legacySearch = false

    private suspend fun searchAll(
        api: ImmichApi,
        request: SearchMetadataRequest,
        displayableOnly: Boolean = true,
    ): List<AssetDto> {
        val results = ArrayList<AssetDto>()
        // Immich >= 3.2 pages by cursor; older servers by page number.
        var page = request.copy(page = 1, cursor = null)
        while (true) {
            val response = try {
                api.searchMetadata(withVisibility(page))
            } catch (e: ImmichException.Http) {
                if (e.code != 400 || legacySearch) throw e
                legacySearch = true
                api.searchMetadata(withVisibility(page))
            }
            val items = response.assets.items
            results += if (displayableOnly) items.filter { it.isDisplayable } else items
            if (items.isEmpty()) break
            page = response.assets.nextCursor?.let { page.copy(page = null, cursor = it) }
                ?: response.assets.nextPage?.toIntOrNull()?.let { page.copy(page = it) }
                ?: break
        }
        return results
    }

    private fun withVisibility(request: SearchMetadataRequest) =
        if (legacySearch) request.copy(visibility = null, withArchived = false)
        else request.copy(visibility = "timeline", withArchived = null)

    private fun readCache(): Playlist? = try {
        if (cacheFile.exists()) ImmichApi.json.decodeFromString(Playlist.serializer(), cacheFile.readText()) else null
    } catch (e: Exception) {
        log("Discarding unreadable playlist cache", e)
        cacheFile.delete()
        null
    }

    private fun writeCache(playlist: Playlist) {
        val tmp = File(cacheFile.parentFile, cacheFile.name + ".tmp")
        tmp.writeText(ImmichApi.json.encodeToString(Playlist.serializer(), playlist))
        tmp.renameTo(cacheFile)
    }

    private fun isoUtc(date: LocalDate) = date.atStartOfDay().atOffset(ZoneOffset.UTC).toInstant().toString()

    companion object {
        private const val TAG = "ImmichCast"
        private const val FALLBACK_FIRST_YEAR = 1990
    }
}

fun Photo.localDate(): LocalDate? = takenAt?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
