package com.roylet.immichcast.ui

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.roylet.immichcast.ImmichCastApp
import com.roylet.immichcast.R
import com.roylet.immichcast.api.AlbumDto
import com.roylet.immichcast.api.ImmichApi
import com.roylet.immichcast.api.ImmichException
import com.roylet.immichcast.core.SlideshowConfig
import com.roylet.immichcast.data.AppSettings
import com.roylet.immichcast.data.Playlist
import com.roylet.immichcast.data.PlaylistRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

sealed interface AlbumsState {
    data object Loading : AlbumsState
    data class Loaded(val albums: List<AlbumDto>) : AlbumsState
    data class Failed(val message: String) : AlbumsState
}

sealed interface ConnectionState {
    data object Idle : ConnectionState
    data object Testing : ConnectionState
    data class Connected(val userName: String, val serverVersion: String) : ConnectionState
    data class Failed(val message: String) : ConnectionState
}

data class StatusState(
    val playlist: Playlist? = null,
    val refreshing: Boolean = false,
    val error: String? = null,
)

@OptIn(FlowPreview::class)
class SettingsViewModel(
    private val app: Application,
    private val settings: AppSettings,
    private val repo: PlaylistRepository,
    private val httpClient: OkHttpClient,
    private val apiFor: (SlideshowConfig) -> ImmichApi,
) : AndroidViewModel(app) {

    val config: StateFlow<SlideshowConfig> = settings.config

    private val _albums = MutableStateFlow<AlbumsState>(AlbumsState.Loading)
    val albums: StateFlow<AlbumsState> = _albums.asStateFlow()

    private val _connection = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connection: StateFlow<ConnectionState> = _connection.asStateFlow()

    private val _status = MutableStateFlow(StatusState())
    val status: StateFlow<StatusState> = _status.asStateFlow()

    private var refreshJob: Job? = null

    init {
        repo.playlist.onEach { p -> _status.value = _status.value.copy(playlist = p) }.launchIn(viewModelScope)

        viewModelScope.launch {
            val current = settings.current
            if (!current.isConfigured) return@launch
            val cached = repo.cached(current)
            if (cached == null || repo.isStale(cached, current)) refreshNow()
        }

        // Rebuild the playlist shortly after anything that changes which photos are shown.
        settings.config
            .map { if (it.isConfigured) repo.cacheKey(it) else null }
            .distinctUntilChanged()
            .drop(1)
            .filter { it != null }
            .debounce(1_500)
            .onEach { refreshNow() }
            .launchIn(viewModelScope)
    }

    fun apiOrNull(): ImmichApi? = runCatching { apiFor(settings.current) }.getOrNull()

    fun effectiveSeason(config: SlideshowConfig) = repo.effectiveSeason(config)

    fun hemisphere(config: SlideshowConfig) = repo.hemisphere(config)

    /** The hemisphere the TV appears to be in, whatever the setting says. */
    fun detectedHemisphere() = repo.hemisphere(SlideshowConfig())

    fun update(transform: (SlideshowConfig) -> SlideshowConfig) = settings.update(transform)

    fun isScreensaverActive(): Boolean = runCatching {
        Settings.Secure.getString(app.contentResolver, "screensaver_components")
            ?.contains(app.packageName) == true
    }.getOrDefault(false)

    /** Verifies [url] + [apiKey] and saves them when they work. */
    fun connect(url: String, apiKey: String, onSuccess: () -> Unit = {}) {
        _connection.value = ConnectionState.Testing
        viewModelScope.launch {
            try {
                val api = ImmichApi(url, httpClient, apiKey.trim())
                val user = api.me()
                val version = runCatching { api.version().toString() }.getOrDefault("?")
                // Album and search access are what the screensaver actually needs.
                api.albums()
                settings.update { it.copy(serverUrl = api.baseUrl.toString().removeSuffix("api/").trimEnd('/'), apiKey = apiKey.trim()) }
                _connection.value = ConnectionState.Connected(user.name.ifBlank { user.email }, version)
                loadAlbums()
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _connection.value = ConnectionState.Failed(describe(e))
            }
        }
    }

    fun loadAlbums() {
        val current = settings.current
        if (!current.isConfigured) return
        // Keep showing the previous list while it reloads.
        if (_albums.value !is AlbumsState.Loaded) _albums.value = AlbumsState.Loading
        viewModelScope.launch {
            _albums.value = try {
                AlbumsState.Loaded(apiFor(current).albums().sortedBy { it.albumName.lowercase() })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AlbumsState.Failed(describe(e))
            }
        }
    }

    fun toggleExcluded(albumId: String) = settings.update {
        it.copy(excludedAlbumIds = if (albumId in it.excludedAlbumIds) it.excludedAlbumIds - albumId else it.excludedAlbumIds + albumId)
    }

    fun toggleIncluded(albumId: String) = settings.update {
        it.copy(includedAlbumIds = if (albumId in it.includedAlbumIds) it.includedAlbumIds - albumId else it.includedAlbumIds + albumId)
    }

    fun refreshNow() {
        if (refreshJob?.isActive == true) return
        val current = settings.current
        if (!current.isConfigured) return
        _status.value = _status.value.copy(refreshing = true, error = null)
        refreshJob = viewModelScope.launch {
            try {
                repo.refresh(current)
                _status.value = _status.value.copy(refreshing = false, error = null)
            } catch (e: CancellationException) {
                _status.value = _status.value.copy(refreshing = false)
                throw e
            } catch (e: Exception) {
                _status.value = _status.value.copy(refreshing = false, error = describe(e))
            }
        }
    }

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    fun clearCache() {
        viewModelScope.launch {
            refreshJob?.cancel()
            repo.clear()
            coil.Coil.imageLoader(app).apply {
                memoryCache?.clear()
                diskCache?.clear()
            }
            refreshNow()
        }
    }

    fun signOut() {
        viewModelScope.launch {
            refreshJob?.cancel()
            repo.clear()
            settings.reset()
            _albums.value = AlbumsState.Loading
            _connection.value = ConnectionState.Idle
            _status.value = StatusState()
        }
    }

    fun resetConnectionState() {
        _connection.value = ConnectionState.Idle
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ImmichCastApp
                SettingsViewModel(app, app.settings, app.playlists, app.httpClient, app::api)
            }
        }
    }

    fun describe(e: Throwable): String = when (e) {
        is ImmichException.InvalidUrl -> app.getString(R.string.error_invalid_url)
        is ImmichException.Unauthorized -> app.getString(R.string.error_unauthorized)
        is ImmichException.Forbidden -> app.getString(R.string.error_forbidden)
        is ImmichException.Unreachable -> app.getString(R.string.error_unreachable_short)
        is ImmichException.Http -> app.getString(R.string.error_http, e.code)
        is ImmichException.BadResponse -> app.getString(R.string.error_bad_response)
        else -> app.getString(R.string.error_generic, e.message ?: e.javaClass.simpleName)
    }
}
