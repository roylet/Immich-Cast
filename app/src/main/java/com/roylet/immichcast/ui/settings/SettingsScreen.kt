package com.roylet.immichcast.ui.settings

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.DenseListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.roylet.immichcast.R
import com.roylet.immichcast.ui.PreviewActivity
import com.roylet.immichcast.ui.SettingsViewModel

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
    onOpenAlbums: () -> Unit,
    onRunSetup: () -> Unit,
    initialCategory: Category = Category.PHOTOS,
) {
    val config by vm.config.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    var category by rememberSaveable { mutableStateOf(initialCategory) }
    val context = LocalContext.current
    val season = remember(config.seasonSource) { vm.effectiveSeason(config) }

    Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Navigation rail
        Column(
            Modifier
                .width(320.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState())
                .padding(start = 32.dp, end = 20.dp, top = 28.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.ic_logo), contentDescription = null, modifier = Modifier.size(36.dp))
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.size(14.dp))
            StatusCard(status, season)
            Spacer(Modifier.size(10.dp))
            Category.entries.forEach { item ->
                DenseListItem(
                    selected = item == category,
                    onClick = { category = item },
                    leadingContent = { Icon(item.icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    headlineContent = { Text(stringResource(item.label)) },
                    colors = ListItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    shape = ListItemDefaults.shape(MaterialTheme.shapes.medium),
                    modifier = Modifier.onFocusChanged { if (it.isFocused) category = item },
                )
            }
        }

        // Detail pane
        Box(Modifier.weight(1f).fillMaxHeight().padding(horizontal = 40.dp)) {
            LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 32.dp, bottom = 48.dp)) {
                item {
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(category.label), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                        Button(
                            onClick = { context.startActivity(Intent(context, PreviewActivity::class.java)) },
                            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                            Text(stringResource(R.string.action_preview))
                        }
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
