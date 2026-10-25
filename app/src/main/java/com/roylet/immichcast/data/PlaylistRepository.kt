package com.roylet.immichcast.data

import android.util.Log
import com.roylet.immichcast.api.AssetDto
import com.roylet.immichcast.api.ImmichApi
import com.roylet.immichcast.api.ImmichException
import com.roylet.immichcast.api.SearchMetadataRequest
import com.roylet.immichcast.core.DateRange
import com.roylet.immichcast.core.MediaFilter
import com.roylet.immichcast.core.Photo
import com.roylet.immichcast.core.PlaylistBuilder
import com.roylet.immichcast.core.Season
import com.roylet.immichcast.core.SeasonSource
import com.roylet.immichcast.core.Seasons
import com.roylet.immichcast.core.SlideshowConfig
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
    val season: Season? = null,
    val seasonYears: Int = 0,
    val excludedCount: Int = 0,
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

    fun effectiveSeason(config: SlideshowConfig): Season? = when (config.seasonSource) {
        SeasonSource.AUTOMATIC -> Season.of(today())
        SeasonSource.ALL_PHOTOS -> null
        else -> config.seasonSource.fixedSeason
    }

    fun cacheKey(config: SlideshowConfig): String = "${config.playlistKey}|${effectiveSeason(config)?.name ?: "ALL"}"

    /** Cached playlist for this exact configuration and season, regardless of age. */
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
        var (photos, years) = loadSeason(api, config, season, excluded)
        var fellBack = false
        if (photos.isEmpty() && season != null && config.seasonSource == SeasonSource.AUTOMATIC) {
            log("No photos for $season, falling back to the whole library", null)
            photos = loadSeason(api, config, null, excluded).first
            years = 0
            fellBack = true
        }
        val playlist = Playlist(
            key = cacheKey(config),
            photos = photos,
            stats = PlaylistStats(
                season = season,
                seasonYears = years,
                excludedCount = excluded.size,
                total = photos.size,
                fellBackToAll = fellBack,
                builtAtMillis = now(),
            ),
        )
        withContext(Dispatchers.IO) { writeCache(playlist) }
        latest.value = playlist
        log("Playlist built: ${photos.size} items, season=$season, excluded=${excluded.size}", null)
        playlist
    }

    suspend fun clear() = mutex.withLock {
        latest.value = null
        withContext(Dispatchers.IO) { cacheFile.delete() }
    }

    private suspend fun loadSeason(
        api: ImmichApi,
        config: SlideshowConfig,
        season: Season?,
        excluded: Set<String>,
    ): Pair<List<Photo>, Int> {
        val base = SearchMetadataRequest(
            type = if (config.mediaFilter == MediaFilter.IMAGES_ONLY) "IMAGE" else null,
            isFavorite = if (config.favoritesOnly) true else null,
            withExif = true,
        )
        if (season == null) {
            val photos = searchAll(api, base).map { it.toPhoto() }
            return build(photos, excluded, config) to 0
        }
        val years = seasonYears(api, season)
        val candidates = years.flatMap { year ->
            val range = Seasons.range(season, year)
            // Immich filters on UTC; pad by a day and filter on local time below.
            searchAll(
                api,
                base.copy(
                    takenAfter = isoUtc(range.start.minusDays(1)),
                    takenBefore = isoUtc(range.endExclusive.plusDays(1)),
                ),
            ).map { it.toPhoto() }.filter { photo -> photo.localDate()?.let { it in range } ?: true }
        }
        val photos = build(candidates, excluded, config)
        val yearsWithPhotos = photos.mapNotNull { it.localDate()?.let(Seasons::seasonYear) }.distinct().size
        return photos to yearsWithPhotos
    }

    private fun build(candidates: List<Photo>, excluded: Set<String>, config: SlideshowConfig) =
        PlaylistBuilder.build(candidates, excluded, config.mediaFilter, config.favoritesOnly, config.order)

    /** Season years worth querying, derived from the timeline so empty years cost nothing. */
    private suspend fun seasonYears(api: ImmichApi, season: Season): List<Int> {
        val thisSeasonYear = Seasons.seasonYear(today())
        val fromTimeline = try {
            val months = api.timeBuckets().filter { it.count > 0 }.mapNotNull {
                runCatching { LocalDate.parse(it.timeBucket.take(10)) }.getOrNull()
            }
            Seasons.seasonYearsWithContent(season, months)
        } catch (e: ImmichException) {
            if (e is ImmichException.Unauthorized || e is ImmichException.Unreachable) throw e
            log("Timeline buckets unavailable, scanning every year", e)
            null
        }
        return (fromTimeline ?: (thisSeasonYear downTo FALLBACK_FIRST_YEAR).toList())
            .filter { it <= thisSeasonYear }
    }

    private suspend fun excludedAssetIds(api: ImmichApi, albumIds: Set<String>): Set<String> {
        val ids = HashSet<String>()
        for (albumId in albumIds) {
            try {
                searchAll(api, SearchMetadataRequest(albumIds = listOf(albumId)), displayableOnly = false)
                    .mapTo(ids) { it.id }
            } catch (e: ImmichException.Http) {
                if (e.code == 404) continue // Album deleted since it was excluded.
                // Older servers cannot filter search by album; read the album directly.
                api.album(albumId).assets.mapTo(ids) { it.id }
            }
        }
        return ids
    }

    private var legacySearch = false

    private suspend fun searchAll(
        api: ImmichApi,
        request: SearchMetadataRequest,
        displayableOnly: Boolean = true,
    ): List<AssetDto> {
        val results = ArrayList<AssetDto>()
        var page = 1
        while (true) {
            val response = try {
                api.searchMetadata(withVisibility(request.copy(page = page)))
            } catch (e: ImmichException.Http) {
                if (e.code != 400 || legacySearch) throw e
                legacySearch = true
                api.searchMetadata(withVisibility(request.copy(page = page)))
            }
            val items = response.assets.items
            results += if (displayableOnly) items.filter { it.isDisplayable } else items
            page = response.assets.nextPage?.toIntOrNull() ?: break
            if (items.isEmpty()) break
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
