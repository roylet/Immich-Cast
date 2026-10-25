package com.roylet.immichcast.api

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds the saved API key to requests that do not already carry one. The key is read
 * per request so settings changes apply immediately to Coil and ExoPlayer too.
 */
class ApiKeyInterceptor(private val apiKey: () -> String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val key = apiKey()
        if (key.isBlank() || original.header(HEADER) != null) return chain.proceed(original)
        return chain.proceed(original.newBuilder().header(HEADER, key).build())
    }

    companion object {
        const val HEADER = "x-api-key"
    }
}
