package com.roylet.immichcast

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.roylet.immichcast.api.ApiKeyInterceptor
import com.roylet.immichcast.api.ImmichApi
import com.roylet.immichcast.core.SlideshowConfig
import com.roylet.immichcast.data.AppSettings
import com.roylet.immichcast.data.PlaylistRepository
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

class ImmichCastApp : Application(), ImageLoaderFactory {

    lateinit var settings: AppSettings
        private set

    /** Shared by the API client, Coil and ExoPlayer; adds the API key to every request. */
    val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(ApiKeyInterceptor { settings.current.apiKey })
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    val playlists: PlaylistRepository by lazy {
        PlaylistRepository(File(filesDir, "playlist.json"), ::api)
    }

    fun api(config: SlideshowConfig = settings.current): ImmichApi =
        ImmichApi(config.serverUrl, httpClient, config.apiKey)

    override fun onCreate() {
        super.onCreate()
        settings = AppSettings(this)
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient { httpClient }
            .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.20).build() }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("images"))
                    .maxSizeBytes(256L * 1024 * 1024)
                    .build()
            }
            // Immich sends no-cache headers; the screensaver still wants offline replay.
            .respectCacheHeaders(false)
            .crossfade(false)
            .build()
}
