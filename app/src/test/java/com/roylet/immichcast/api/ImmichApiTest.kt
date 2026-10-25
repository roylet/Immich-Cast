package com.roylet.immichcast.api

import com.roylet.immichcast.core.MediaType
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ImmichApiTest {

    private val server = MockWebServer()
    private lateinit var api: ImmichApi

    @Before
    fun setUp() {
        server.start()
        api = ImmichApi(server.url("/").toString(), OkHttpClient(), apiKey = "secret")
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `normalizes the addresses people type`() {
        assertEquals("http://192.168.1.5:2283/api/", ImmichApi.normalizeBaseUrl("192.168.1.5:2283")?.toString())
        assertEquals("https://photos.example.com/api/", ImmichApi.normalizeBaseUrl(" https://photos.example.com/ ")?.toString())
        assertEquals("https://photos.example.com/api/", ImmichApi.normalizeBaseUrl("https://photos.example.com/api")?.toString())
        assertNull(ImmichApi.normalizeBaseUrl(""))
    }

    @Test
    fun `search sends the api key and parses assets`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """
                {"albums":{"items":[]},"assets":{"total":2,"count":2,"nextPage":"2","items":[
                  {"id":"one","type":"IMAGE","originalMimeType":"image/jpeg","localDateTime":"2023-10-04T15:12:01.000Z",
                   "isFavorite":true,"visibility":"timeline","duration":"0:00:00.00000",
                   "exifInfo":{"city":"Hobart","country":"Australia","description":""},"unknownField":1},
                  {"id":"two","type":"VIDEO","fileCreatedAt":"2021-01-02T03:04:05.000Z","duration":"0:01:02.500000","isArchived":true}
                ]}}
                """.trimIndent(),
            ),
        )
        val page = api.searchMetadata(SearchMetadataRequest(type = "IMAGE", visibility = "timeline")).assets
        val request = server.takeRequest()
        assertEquals("/api/search/metadata", request.path)
        assertEquals("secret", request.getHeader("x-api-key"))
        assertTrue(request.body.readUtf8().contains("\"visibility\":\"timeline\""))

        assertEquals("2", page.nextPage)
        val one = page.items[0].toPhoto()
        assertEquals(MediaType.IMAGE, one.type)
        assertEquals("2023-10-04T15:12:01", one.takenAt)
        assertEquals("Hobart", one.city)
        assertNull(one.description)
        assertNull(one.durationMs)
        assertTrue(page.items[0].isDisplayable)

        val two = page.items[1].toPhoto()
        assertEquals(MediaType.VIDEO, two.type)
        assertEquals(62_500L, two.durationMs)
        assertEquals(false, page.items[1].isDisplayable)
    }

    @Test(expected = ImmichException.Unauthorized::class)
    fun `401 maps to unauthorized`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))
        api.me()
    }

    @Test
    fun `timeline buckets fall back to the legacy size parameter`() = runTest {
        server.enqueue(MockResponse().setResponseCode(400))
        server.enqueue(MockResponse().setBody("""[{"timeBucket":"2024-03-01T00:00:00.000Z","count":4}]"""))
        val buckets = api.timeBuckets()
        assertEquals("/api/timeline/buckets", server.takeRequest().path)
        assertEquals("/api/timeline/buckets?size=MONTH", server.takeRequest().path)
        assertEquals(4, buckets.single().count)
    }
}
