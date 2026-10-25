package com.roylet.immichcast.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.roylet.immichcast.R
import com.roylet.immichcast.api.AlbumDto
import com.roylet.immichcast.api.ImmichApi
import com.roylet.immichcast.ui.components.Spinner
import com.roylet.immichcast.ui.theme.BrutalBadge
import com.roylet.immichcast.ui.theme.BrutalButton
import com.roylet.immichcast.ui.theme.BrutalDivider
import com.roylet.immichcast.ui.theme.BrutalSurface
import com.roylet.immichcast.ui.theme.Palette
import com.roylet.immichcast.ui.theme.dotGrid
import java.text.NumberFormat

/** Grid of the user's albums; selecting one excludes every photo in it from the screensaver. */
@Composable
fun AlbumPicker(
    vm: SettingsViewModel,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
) {
    val albums by vm.albums.collectAsStateWithLifecycle()
    val config by vm.config.collectAsStateWithLifecycle()
    LaunchedEffect(config.serverUrl, config.apiKey) { vm.loadAlbums() }

    Column(modifier.fillMaxSize()) {
        header?.invoke()
        when (val state = albums) {
            AlbumsState.Loading -> CenterMessage { Spinner(32.dp) }
            is AlbumsState.Failed -> CenterMessage {
                Text(state.message, style = MaterialTheme.typography.bodyLarge, color = Palette.Destructive)
                Spacer(Modifier.size(16.dp))
                BrutalButton(stringResource(R.string.action_retry), onClick = vm::loadAlbums)
            }
            is AlbumsState.Loaded -> if (state.albums.isEmpty()) {
                CenterMessage {
                    Text(stringResource(R.string.albums_empty), style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                val api = remember(config.serverUrl, config.apiKey) { vm.apiOrNull() }
                val first = remember { FocusRequester() }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    items(state.albums, key = { it.id }) { album ->
                        AlbumCard(
                            album = album,
                            api = api,
                            excluded = album.id in config.excludedAlbumIds,
                            onClick = { vm.toggleExcluded(album.id) },
                            modifier = if (album == state.albums.first()) Modifier.focusRequester(first) else Modifier,
                        )
                    }
                }
                LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
            }
        }
        footer?.invoke()
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.CenterMessage(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) { content() }
}

@Composable
private fun AlbumCard(album: AlbumDto, api: ImmichApi?, excluded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    BrutalSurface(onClick, modifier.fillMaxWidth(), shadow = if (excluded) 0.dp else 4.dp) { _ ->
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 10f).background(Palette.Secondary)) {
                val thumb = album.albumThumbnailAssetId
                if (thumb != null && api != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(api.thumbnailUrl(thumb, "thumbnail"))
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = null,
                        modifier = Modifier.size(40.dp).align(Alignment.Center), tint = Palette.TextMuted)
                }
                if (excluded) {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)))
                    Icon(Icons.Rounded.Block, contentDescription = null, tint = Palette.Destructive,
                        modifier = Modifier.size(44.dp).align(Alignment.Center))
                    BrutalBadge(stringResource(R.string.albums_excluded_badge), Palette.Destructive,
                        Modifier.align(Alignment.TopEnd).padding(8.dp))
                }
            }
            BrutalDivider()
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(album.albumName.ifBlank { stringResource(R.string.albums_untitled) },
                    style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    pluralStringResource(R.plurals.items_count, album.assetCount, NumberFormat.getIntegerInstance().format(album.assetCount)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.TextMuted,
                )
            }
        }
    }
}

/** Full-screen album picker reached from Settings. */
@Composable
fun AlbumPickerScreen(vm: SettingsViewModel) {
    val config by vm.config.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize().background(Palette.Background).dotGrid().padding(horizontal = 56.dp, vertical = 32.dp)) {
        AlbumPicker(vm, header = {
            Column(Modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.albums_title), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.albums_subtitle), style = MaterialTheme.typography.bodyMedium, color = Palette.TextMuted)
                val count = config.excludedAlbumIds.size
                BrutalBadge(
                    if (count == 0) stringResource(R.string.albums_none_excluded)
                    else pluralStringResource(R.plurals.albums_excluded_count, count, count),
                    if (count == 0) Palette.Ink else Palette.Destructive,
                    Modifier.padding(top = 4.dp),
                )
            }
        })
    }
}
