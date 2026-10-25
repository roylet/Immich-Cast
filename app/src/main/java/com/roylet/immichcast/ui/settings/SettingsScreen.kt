package com.roylet.immichcast.ui.settings

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.roylet.immichcast.R
import com.roylet.immichcast.ui.AlbumSelection
import com.roylet.immichcast.ui.PreviewActivity
import com.roylet.immichcast.ui.SettingsViewModel
import com.roylet.immichcast.ui.theme.BrutalButton
import com.roylet.immichcast.ui.theme.BrutalStyle
import com.roylet.immichcast.ui.theme.BrutalSurface
import com.roylet.immichcast.ui.theme.Palette
import com.roylet.immichcast.ui.theme.dotGrid

enum class Category(val icon: ImageVector, val label: Int) {
    PHOTOS(Icons.Rounded.PhotoLibrary, R.string.category_photos),
    DISPLAY(Icons.Rounded.Palette, R.string.category_display),
    OVERLAY(Icons.Rounded.Subtitles, R.string.category_overlay),
    CONNECTION(Icons.Rounded.Link, R.string.category_connection),
    ADVANCED(Icons.Rounded.Tune, R.string.category_advanced),
    ABOUT(Icons.Rounded.Info, R.string.category_about),
}

@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    onOpenAlbums: (AlbumSelection) -> Unit,
    onRunSetup: () -> Unit,
    initialCategory: Category = Category.PHOTOS,
) {
    val config by vm.config.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    var category by rememberSaveable { mutableStateOf(initialCategory) }
    val context = LocalContext.current
    val season = remember(config.mode, config.seasonSource, config.hemisphereSource) { vm.effectiveSeason(config) }
    val hemisphere = remember(config.hemisphereSource) { vm.hemisphere(config) }
    val railFocus = remember { Category.entries.associateWith { FocusRequester() } }

    Row(Modifier.fillMaxSize().background(Palette.Background).dotGrid()) {
        // Navigation rail
        Row(Modifier.width(332.dp).fillMaxHeight()) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    // Coming back from the detail pane lands on the open category, not whichever
                    // rail item happens to sit level with the focused row (which would switch panes).
                    .focusProperties { onEnter = { railFocus.getValue(category).requestFocus() } }
                    .focusGroup()
                    .background(Palette.Background)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 32.dp, end = 24.dp, top = 28.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.ic_logo), contentDescription = null, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
                }
                Spacer(Modifier.height(4.dp))
                StatusCard(status, config, season, hemisphere)
                Spacer(Modifier.height(6.dp))
                Category.entries.forEach { item ->
                    BrutalSurface(
                        onClick = { category = item },
                        selected = item == category,
                        style = BrutalStyle.GHOST,
                        shadow = 3.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 4.dp)
                            .focusRequester(railFocus.getValue(item))
                            .onFocusChanged { if (it.isFocused) category = item },
                    ) {
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(item.icon, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(item.label).uppercase(), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            // Rule between rail and content.
            Box(Modifier.width(1.dp).fillMaxHeight().background(Palette.Line))
        }

        // Detail pane
        Box(Modifier.weight(1f).fillMaxHeight().padding(horizontal = 40.dp)) {
            // Returning from the rail resumes on the row that was focused last.
            LazyColumn(Modifier.focusRestorer(), contentPadding = PaddingValues(top = 28.dp, bottom = 48.dp)) {
                item {
                    Row(Modifier.fillMaxWidth().padding(bottom = 2.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(category.label), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                        BrutalButton(
                            text = stringResource(R.string.action_preview),
                            onClick = { context.startActivity(Intent(context, PreviewActivity::class.java)) },
                            icon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null) },
                        )
                    }
                }
                when (category) {
                    Category.PHOTOS -> photosPane(config, vm, onOpenAlbums)
                    Category.DISPLAY -> displayPane(config, vm)
                    Category.OVERLAY -> overlayPane(config, vm)
                    Category.CONNECTION -> connectionPane(config, vm, onRunSetup)
                    Category.ADVANCED -> advancedPane(config, vm)
                    Category.ABOUT -> aboutPane(vm)
                }
            }
        }
    }
}
