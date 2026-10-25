package com.roylet.immichcast.data

import com.roylet.immichcast.api.ImmichApi
import com.roylet.immichcast.core.Hemisphere
import com.roylet.immichcast.core.Season
import com.roylet.immichcast.core.SeasonSource
import com.roylet.immichcast.core.SlideshowConfig
import com.roylet.immichcast.core.SlideshowMode
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.LocalDate

/** Exercises season selection and album exclusion against a fake Immich server. */
class PlaylistRepositoryTest {

    @get:Rule val tmp = TemporaryFolder()

    private val server = MockWebServer()
    private val searches = mutableListOf<String>()

    /** Library: asset id -> local date. "x*" assets are also in the excluded album. */
    private val library = mapOf(
        "spring22" to "2022-10-05", "spring23" to "2023-09-01", "x-spring23" to "2023-11-20",
        "summer23" to "2024-01-15", "winter24" to "2024-07-01",
    )

    @Before
    fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.path!!.startsWith("/api/timeline/buckets") -> MockResponse().setBody(
                    library.values.map { it.take(7) + "-01" }.distinct()
                        .joinToString(",", "[", "]") { """{"timeBucket":"$it","count":1}""" },
                )
                request.path == "/api/search/metadata" -> {
                    val body = request.body.readUtf8()
                    searches += body
                    val ids = if ("albumIds" in body) {
                        library.keys.filter { it.startsWith("x-") }
                    } else {
                        val after = Regex("\"takenAfter\":\"(\\d{4}-\\d{2}-\\d{2})").find(body)?.groupValues?.get(1)
                        val before = Regex("\"takenBefore\":\"(\\d{4}-\\d{2}-\\d{2})").find(body)?.groupValues?.get(1)
                        library.filter { (_, date) -> (after == null || date >= after) && (before == null || date < before) }.keys
                    }
                    val items = ids.joinToString(",") { """{"id":"$it","type":"IMAGE","localDateTime":"${library[it]}T12:00:00.000Z"}""" }
                    MockResponse().setBody("""{"assets":{"total":${ids.size},"count":${ids.size},"items":[$items],"nextPage":null}}""")
                }
                else -> MockResponse().setResponseCode(404)
            }
        }
        server.start()
    }

    @After
    fun tearDown() = server.shutdown()

    private fun repo(today: LocalDate, hemisphere: Hemisphere = Hemisphere.SOUTHERN) = PlaylistRepository(
        cacheFile = tmp.newFile(),
        apiFor = { ImmichApi(it.serverUrl, OkHttpClient(), it.apiKey) },
        today = { today },
        detectHemisphere = { hemisphere },
        log = { _, _ -> },
    )

    private fun config(
        vararg excluded: String,
        included: Set<String> = emptySet(),
        source: SeasonSource = SeasonSource.AUTOMATIC,
        mode: SlideshowMode = SlideshowMode.SEASONAL,
        recentDays: Int = 90,
    ) = SlideshowConfig(
        serverUrl = server.url("/").toString(),
        apiKey = "k",
        excludedAlbumIds = excluded.toSet(),
        includedAlbumIds = included,
        seasonSource = source,
        mode = mode,
        recentDays = recentDays,
    )

    @Test
    fun `current season across all years, minus excluded albums`() = runTest {
        val playlist = repo(LocalDate.of(2026, 10, 25)).refresh(config("album-x"))
        assertEquals(setOf("spring22", "spring23"), playlist.photos.map { it.id }.toSet())
        assertEquals(Season.SPRING, playlist.stats.season)
        assertEquals(2, playlist.stats.years)
        assertEquals(1, playlist.stats.excludedCount)
        assertFalse(playlist.stats.fellBackToAll)
        // Only years with spring photos are queried (2022, 2023), plus the album lookup.
        assertEquals(3, searches.size)
    }

    @Test
    fun `without exclusions the album photo is included`() = runTest {
        val playlist = repo(LocalDate.of(2026, 10, 25)).refresh(config())
        assertEquals(setOf("spring22", "spring23", "x-spring23"), playlist.photos.map { it.id }.toSet())
    }

    @Test
    fun `summer spans december to february`() = runTest {
        val playlist = repo(LocalDate.of(2027, 1, 10)).refresh(config())
        assertEquals(listOf("summer23"), playlist.photos.map { it.id })
    }

    @Test
    fun `empty season falls back to the whole library`() = runTest {
        val playlist = repo(LocalDate.of(2026, 4, 1)).refresh(config("album-x")) // autumn: nothing
        assertTrue(playlist.stats.fellBackToAll)
        assertEquals(library.keys.filterNot { it.startsWith("x-") }.toSet(), playlist.photos.map { it.id }.toSet())
    }

    @Test
    fun `fixed season ignores the calendar and cache is keyed per configuration`() = runTest {
        val repo = repo(LocalDate.of(2026, 10, 25))
        val winter = config(source = SeasonSource.WINTER)
        repo.refresh(winter)
        assertEquals(listOf("winter24"), repo.cached(winter)!!.photos.map { it.id })
        assertEquals(null, repo.cached(config()))
    }

    @Test
    fun `a pinned season follows the detected hemisphere`() = runTest {
        // Northern winter is December to February.
        val north = repo(LocalDate.of(2026, 10, 25), Hemisphere.NORTHERN)
        val winter = config(source = SeasonSource.WINTER)
        assertEquals(listOf("summer23"), north.refresh(winter).photos.map { it.id })
        // The same day is autumn in the north; the cache tells the hemispheres apart.
        assertEquals(Season.AUTUMN, north.effectiveSeason(config()))
        assertNotEquals(north.cacheKey(config()), repo(LocalDate.of(2026, 10, 25)).cacheKey(config()))
    }

    @Test
    fun `random uses the whole library minus exclusions`() = runTest {
        val playlist = repo(LocalDate.of(2026, 10, 25)).refresh(config("album-x", mode = SlideshowMode.RANDOM))
        assertEquals(library.keys.filterNot { it.startsWith("x-") }.toSet(), playlist.photos.map { it.id }.toSet())
        assertFalse(playlist.stats.fellBackToAll)
    }

    @Test
    fun `recent covers the chosen span`() = runTest {
        // 2024-07-01 is within 365 days of 2025-06-01; 2024-01-15 is not.
        val playlist = repo(LocalDate.of(2025, 6, 1)).refresh(config(mode = SlideshowMode.RECENT, recentDays = 365))
        assertEquals(listOf("winter24"), playlist.photos.map { it.id })
    }

    @Test
    fun `on this day picks the same week in earlier years`() = runTest {
        // 2022-10-05 and 2023-09-01 are in the library; only the first is within 3 days of 10-03.
        val playlist = repo(LocalDate.of(2026, 10, 3)).refresh(config(mode = SlideshowMode.ON_THIS_DAY))
        assertEquals(listOf("spring22"), playlist.photos.map { it.id })
        assertEquals(1, playlist.stats.years)
    }

    @Test
    fun `this month picks the calendar month in every year`() = runTest {
        // Only 2022-10-05 falls in an October.
        val playlist = repo(LocalDate.of(2026, 10, 25)).refresh(config(mode = SlideshowMode.THIS_MONTH))
        assertEquals(listOf("spring22"), playlist.photos.map { it.id })
        assertEquals(1, playlist.stats.years)
    }

    @Test
    fun `included albums limit every mode`() = runTest {
        val seasonal = repo(LocalDate.of(2026, 10, 25)).refresh(config(included = setOf("album-x")))
        assertEquals(listOf("x-spring23"), seasonal.photos.map { it.id })
        assertEquals(1, seasonal.stats.includedAlbums)
        // Dates are applied locally, so there is one search per included album and none per year.
        assertEquals(1, searches.size)

        val random = repo(LocalDate.of(2026, 10, 25)).refresh(config(included = setOf("album-x"), mode = SlideshowMode.RANDOM))
        assertEquals(listOf("x-spring23"), random.photos.map { it.id })
    }

    @Test
    fun `excluded albums win over included ones`() = runTest {
        val playlist = repo(LocalDate.of(2026, 10, 25)).refresh(config("album-x", included = setOf("album-x"), mode = SlideshowMode.RANDOM))
        assertTrue(playlist.photos.isEmpty())
    }

    @Test
    fun `follows search cursors across pages`() = runTest {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (request.path != "/api/search/metadata") return MockResponse().setResponseCode(404)
                val body = request.body.readUtf8()
                searches += body
                val (id, next) = if ("\"cursor\":\"c2\"" in body) "second" to "null" else "first" to "\"c2\""
                return MockResponse().setBody(
                    """{"assets":{"count":1,"items":[{"id":"$id","type":"IMAGE","localDateTime":"2024-01-01T12:00:00.000Z"}],"nextPage":null,"nextCursor":$next}}""",
                )
            }
        }
        val playlist = repo(LocalDate.of(2026, 10, 25)).refresh(config(mode = SlideshowMode.RANDOM))
        assertEquals(setOf("first", "second"), playlist.photos.map { it.id }.toSet())
        assertEquals(2, searches.size)
    }

    @Test
    fun `cache key moves with the day for date-relative modes`() {
        val recent = config(mode = SlideshowMode.RECENT)
        assertNotEquals(repo(LocalDate.of(2026, 10, 3)).cacheKey(recent), repo(LocalDate.of(2026, 10, 4)).cacheKey(recent))
        val month = config(mode = SlideshowMode.THIS_MONTH)
        assertEquals(repo(LocalDate.of(2026, 10, 3)).cacheKey(month), repo(LocalDate.of(2026, 10, 31)).cacheKey(month))
        assertNotEquals(repo(LocalDate.of(2026, 10, 31)).cacheKey(month), repo(LocalDate.of(2026, 11, 1)).cacheKey(month))
        val random = config(mode = SlideshowMode.RANDOM)
        assertEquals(repo(LocalDate.of(2026, 10, 3)).cacheKey(random), repo(LocalDate.of(2026, 10, 4)).cacheKey(random))
    }
}
