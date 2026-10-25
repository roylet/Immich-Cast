package com.roylet.immichcast.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/** Failures surfaced to the user, mapped from HTTP and network errors. */
sealed class ImmichException(message: String, cause: Throwable? = null) : IOException(message, cause) {
    class InvalidUrl : ImmichException("The server URL is not valid")
    class Unreachable(cause: Throwable) : ImmichException("Could not reach the server", cause)
    class Unauthorized : ImmichException("The API key was rejected")
    class Forbidden(val path: String) : ImmichException("The API key lacks permission for $path")
    class Http(val code: Int, path: String) : ImmichException("Server returned HTTP $code for $path")
    class BadResponse(cause: Throwable) : ImmichException("Unexpected response from the server", cause)
}

/**
 * Minimal Immich REST client. Requests carry [apiKey] when given; otherwise the
 * shared [client]'s [ApiKeyInterceptor] supplies the saved key.
 */
class ImmichApi(
    serverUrl: String,
    private val client: OkHttpClient,
    private val apiKey: String? = null,
) {

    val baseUrl: HttpUrl = normalizeBaseUrl(serverUrl) ?: throw ImmichException.InvalidUrl()

    suspend fun version(): ServerVersionDto = get("server/version")

    suspend fun me(): UserDto = get("users/me")

    suspend fun albums(): List<AlbumDto> = get("albums")

    suspend fun albumsContaining(assetId: String): List<AlbumDto> =
        get("albums") { addQueryParameter("assetId", assetId) }

    suspend fun album(id: String): AlbumDto = get("albums/$id")

    suspend fun timeBuckets(): List<TimeBucketDto> = try {
        get("timeline/buckets")
    } catch (e: ImmichException.Http) {
        // Immich < 1.133 requires an explicit bucket size.
        if (e.code == 400) get("timeline/buckets") { addQueryParameter("size", "MONTH") } else throw e
    }

    suspend fun searchMetadata(request: SearchMetadataRequest): SearchResponse =
        post("search/metadata", json.encodeToString(SearchMetadataRequest.serializer(), request))

    fun url(path: String, build: HttpUrl.Builder.() -> Unit = {}): HttpUrl =
        baseUrl.newBuilder().addPathSegments(path).apply(build).build()

    fun thumbnailUrl(assetId: String, size: String): String =
        url("assets/$assetId/thumbnail") { addQueryParameter("size", size) }.toString()

    fun originalUrl(assetId: String): String = url("assets/$assetId/original").toString()

    fun videoUrl(assetId: String): String = url("assets/$assetId/video/playback").toString()

    private suspend inline fun <reified T> get(path: String, noinline build: HttpUrl.Builder.() -> Unit = {}): T =
        execute(path, request(url(path, build)).get().build())

    private suspend inline fun <reified T> post(path: String, body: String): T =
        execute(path, request(url(path)).post(body.toRequestBody(JSON)).build())

    private fun request(url: HttpUrl) = Request.Builder().url(url).apply {
        if (!apiKey.isNullOrBlank()) header(ApiKeyInterceptor.HEADER, apiKey)
    }

    private suspend inline fun <reified T> execute(path: String, request: Request): T {
        val body = withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    when {
                        response.code == 401 -> throw ImmichException.Unauthorized()
                        response.code == 403 -> throw ImmichException.Forbidden(path)
                        !response.isSuccessful -> throw ImmichException.Http(response.code, path)
                    }
                    response.body?.string().orEmpty()
                }
            } catch (e: ImmichException) {
                throw e
            } catch (e: IOException) {
                throw ImmichException.Unreachable(e)
            }
        }
        return try {
            json.decodeFromString<T>(body)
        } catch (e: IllegalArgumentException) {
            throw ImmichException.BadResponse(e)
        }
    }

    companion object {
        private val JSON = "application/json".toMediaType()

        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            coerceInputValues = true
        }

        /**
         * Accepts what people actually type: "192.168.1.5:2283", "https://photos.example.com/",
         * or a URL that already ends in "/api". Returns the URL of the API root.
         */
        fun normalizeBaseUrl(input: String): HttpUrl? {
            var text = input.trim().trimEnd('/')
            if (text.isEmpty()) return null
            if (!text.startsWith("http://", true) && !text.startsWith("https://", true)) text = "http://$text"
            if (!text.endsWith("/api", true)) text = "$text/api"
            return "$text/".toHttpUrlOrNull()
        }
    }
}
