package com.roylet.immichcast.ui.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.PhotoAlbum
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Icon
import com.roylet.immichcast.BuildConfig
import com.roylet.immichcast.R
import com.roylet.immichcast.core.ClockFormat
import com.roylet.immichcast.core.ImageFit
import com.roylet.immichcast.core.ImageQuality
import com.roylet.immichcast.core.MediaFilter
import com.roylet.immichcast.core.OverlayItem
import com.roylet.immichcast.core.OverlayPosition
import com.roylet.immichcast.core.PlaybackOrder
import com.roylet.immichcast.core.SeasonSource
import com.roylet.immichcast.core.SlideshowConfig
import com.roylet.immichcast.core.Transition
import com.roylet.immichcast.ui.ConnectionState
import com.roylet.immichcast.ui.SettingsViewModel
import com.roylet.immichcast.ui.components.ActionRow
import com.roylet.immichcast.ui.components.ChoiceDialog
import com.roylet.immichcast.ui.components.InfoBlock
import com.roylet.immichcast.ui.components.SectionHeader
import com.roylet.immichcast.ui.components.Spinner
import com.roylet.immichcast.ui.components.SwitchRow
import com.roylet.immichcast.ui.components.TextInputDialog
import com.roylet.immichcast.ui.components.ValueRow
import com.roylet.immichcast.ui.descriptionRes
import com.roylet.immichcast.ui.labelRes
import com.roylet.immichcast.ui.monthsRes

@Composable
private fun <T> ChoiceRow(
    icon: ImageVector,
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    description: String? = null,
    optionDescription: (@Composable (T) -> String?)? = null,
) {
    var open by remember { mutableStateOf(false) }
    ValueRow(icon, title, label(selected), onClick = { open = true }, description = description)
    if (open) {
        ChoiceDialog(
            title = title,
            subtitle = description,
            options = options,
            selected = selected,
            label = label,
            description = optionDescription,
            onSelect = onSelect,
            onDismiss = { open = false },
        )
    }
}

@Composable
fun durationLabel(seconds: Int): String =
    if (seconds < 60) pluralStringResource(R.plurals.duration_seconds, seconds, seconds)
    else pluralStringResource(R.plurals.duration_minutes, seconds / 60, seconds / 60)

fun LazyListScope.photosPane(config: SlideshowConfig, vm: SettingsViewModel, onOpenAlbums: () -> Unit) {
    item { SectionHeader(stringResource(R.string.section_what_to_show)) }
    item {
        ChoiceRow(
            icon = Icons.Rounded.Eco,
            title = stringResource(R.string.setting_season),
            description = stringResource(R.string.setting_season_desc),
            options = SeasonSource.entries,
            selected = config.seasonSource,
            label = { stringResource(it.labelRes) },
            optionDescription = { source ->
                when (source) {
                    SeasonSource.AUTOMATIC -> stringResource(R.string.season_source_automatic_desc)
                    SeasonSource.ALL_PHOTOS -> stringResource(R.string.season_all_desc)
                    else -> stringResource(source.fixedSeason!!.monthsRes)
                }
            },
            onSelect = { v -> vm.update { it.copy(seasonSource = v) } },
        )
    }
    item {
        val count = config.excludedAlbumIds.size
        ValueRow(
            icon = Icons.Rounded.Block,
            title = stringResource(R.string.setting_excluded_albums),
            description = stringResource(R.string.setting_excluded_albums_desc),
            value = if (count == 0) stringResource(R.string.value_none)
            else pluralStringResource(R.plurals.albums_count, count, count),
            onClick = onOpenAlbums,
        )
    }
    item {
        ChoiceRow(
            icon = Icons.Rounded.Movie,
            title = stringResource(R.string.setting_media),
            options = MediaFilter.entries,
            selected = config.mediaFilter,
            label = { stringResource(it.labelRes) },
            optionDescription = { if (it == MediaFilter.IMAGES_AND_VIDEOS) stringResource(R.string.media_videos_desc) else null },
            onSelect = { v -> vm.update { it.copy(mediaFilter = v) } },
        )
    }
    item {
        SwitchRow(
            icon = Icons.Rounded.Favorite,
            title = stringResource(R.string.setting_favorites),
            description = stringResource(R.string.setting_favorites_desc),
            checked = config.favoritesOnly,
            onCheckedChange = { v -> vm.update { it.copy(favoritesOnly = v) } },
        )
    }
    item { SectionHeader(stringResource(R.string.section_order)) }
    item {
        ChoiceRow(
            icon = Icons.Rounded.Shuffle,
            title = stringResource(R.string.setting_order),
            options = PlaybackOrder.entries,
            selected = config.order,
            label = { stringResource(it.labelRes) },
            onSelect = { v -> vm.update { it.copy(order = v) } },
        )
    }
}

fun LazyListScope.displayPane(config: SlideshowConfig, vm: SettingsViewModel) {
    item { SectionHeader(stringResource(R.string.section_timing)) }
    item {
        ChoiceRow(
            icon = Icons.Rounded.Timer,
            title = stringResource(R.string.setting_duration),
            options = SlideshowConfig.SLIDE_DURATIONS,
            selected = config.secondsPerSlide,
            label = { durationLabel(it) },
            onSelect = { v -> vm.update { it.copy(secondsPerSlide = v) } },
        )
    }
    item {
        ChoiceRow(
            icon = Icons.Rounded.AutoAwesome,
            title = stringResource(R.string.setting_transition),
            options = Transition.entries,
            selected = config.transition,
            label = { stringResource(it.labelRes) },
            optionDescription = { if (it == Transition.KEN_BURNS) stringResource(R.string.transition_ken_burns_desc) else null },
            onSelect = { v -> vm.update { it.copy(transition = v) } },
        )
    }
    item { SectionHeader(stringResource(R.string.section_picture)) }
    item {
        ChoiceRow(
            icon = Icons.Rounded.HighQuality,
            title = stringResource(R.string.setting_quality),
            options = ImageQuality.entries,
            selected = config.quality,
            label = { stringResource(it.labelRes) },
            optionDescription = { stringResource(it.descriptionRes) },
            onSelect = { v -> vm.update { it.copy(quality = v) } },
        )
    }
    item {
        ChoiceRow(
            icon = Icons.Rounded.AspectRatio,
            title = stringResource(R.string.setting_fit),
            options = ImageFit.entries,
            selected = config.fit,
            label = { stringResource(it.labelRes) },
            onSelect = { v -> vm.update { it.copy(fit = v) } },
        )
    }
    item {
        ChoiceRow(
            icon = Icons.Rounded.BrightnessLow,
            title = stringResource(R.string.setting_dim),
            description = stringResource(R.string.setting_dim_desc),
            options = SlideshowConfig.DIM_LEVELS,
            selected = config.dimPercent,
            label = { if (it == 0) stringResource(R.string.value_off) else "$it%" },
            onSelect = { v -> vm.update { it.copy(dimPercent = v) } },
        )
    }
}

private val OverlayItem.icon: ImageVector
    get() = when (this) {
        OverlayItem.CLOCK -> Icons.Rounded.Schedule
        OverlayItem.DATE -> Icons.Rounded.Today
        OverlayItem.LOCATION -> Icons.Rounded.LocationOn
        OverlayItem.ALBUM -> Icons.Rounded.PhotoAlbum
        OverlayItem.DESCRIPTION -> Icons.Rounded.Description
        OverlayItem.SEASON -> Icons.Rounded.CalendarMonth
    }

fun LazyListScope.overlayPane(config: SlideshowConfig, vm: SettingsViewModel) {
    item { SectionHeader(stringResource(R.string.section_overlay_items)) }
    OverlayItem.entries.forEach { overlayItem ->
        item(key = overlayItem.name) {
            SwitchRow(
                icon = overlayItem.icon,
                title = stringResource(overlayItem.labelRes),
                checked = overlayItem in config.overlayItems,
                onCheckedChange = { on ->
                    vm.update { it.copy(overlayItems = if (on) it.overlayItems + overlayItem else it.overlayItems - overlayItem) }
                },
            )
        }
    }
    item { SectionHeader(stringResource(R.string.section_overlay_style)) }
    item {
        ChoiceRow(
            icon = Icons.Rounded.OpenWith,
            title = stringResource(R.string.setting_overlay_position),
            options = OverlayPosition.entries,
            selected = config.overlayPosition,
            label = { stringResource(it.labelRes) },
            onSelect = { v -> vm.update { it.copy(overlayPosition = v) } },
        )
    }
    item {
        ChoiceRow(
            icon = Icons.Rounded.AccessTime,
            title = stringResource(R.string.setting_clock_format),
            options = ClockFormat.entries,
            selected = config.clockFormat,
            label = { stringResource(it.labelRes) },
            onSelect = { v -> vm.update { it.copy(clockFormat = v) } },
        )
    }
}

fun LazyListScope.connectionPane(config: SlideshowConfig, vm: SettingsViewModel, onRunSetup: () -> Unit) {
    item { SectionHeader(stringResource(R.string.section_server)) }
    item {
        var editing by remember { mutableStateOf(false) }
        ValueRow(Icons.Rounded.Cloud, stringResource(R.string.setting_server_url), config.serverUrl, onClick = { editing = true })
        if (editing) {
            TextInputDialog(
                title = stringResource(R.string.setting_server_url),
                subtitle = stringResource(R.string.setup_server_hint),
                initial = config.serverUrl,
                placeholder = stringResource(R.string.setup_server_placeholder),
                keyboardType = KeyboardType.Uri,
                onConfirm = { url -> vm.connect(url, config.apiKey) },
                onDismiss = { editing = false },
            )
        }
    }
    item {
        var editing by remember { mutableStateOf(false) }
        ValueRow(Icons.Rounded.Key, stringResource(R.string.setting_api_key), maskKey(config.apiKey), onClick = { editing = true })
        if (editing) {
            TextInputDialog(
                title = stringResource(R.string.setting_api_key),
                subtitle = stringResource(R.string.setup_key_hint),
                initial = "",
                placeholder = stringResource(R.string.setup_key_placeholder),
                keyboardType = KeyboardType.Password,
                onConfirm = { key -> if (key.isNotBlank()) vm.connect(config.serverUrl, key) },
                onDismiss = { editing = false },
            )
        }
    }
    item {
        val connection by vm.connection.collectAsStateWithLifecycle()
        val description = when (val c = connection) {
            ConnectionState.Idle -> stringResource(R.string.connection_idle)
            ConnectionState.Testing -> stringResource(R.string.connection_testing)
            is ConnectionState.Connected -> stringResource(R.string.connection_ok, c.userName, c.serverVersion)
            is ConnectionState.Failed -> c.message
        }
        ActionRow(
            icon = Icons.Rounded.WifiTethering,
            title = stringResource(R.string.action_test_connection),
            description = description,
            onClick = { vm.connect(config.serverUrl, config.apiKey) },
            trailing = when (connection) {
                ConnectionState.Testing -> ({ Spinner() })
                is ConnectionState.Connected -> ({
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = com.roylet.immichcast.ui.theme.Palette.Success)
                })
                else -> null
            },
        )
    }
    item { SectionHeader(stringResource(R.string.section_account)) }
    item {
        ActionRow(Icons.Rounded.Refresh, stringResource(R.string.action_run_setup), onClick = onRunSetup,
            description = stringResource(R.string.action_run_setup_desc))
    }
    item {
        var confirm by remember { mutableStateOf(false) }
        ActionRow(Icons.AutoMirrored.Rounded.Logout, stringResource(R.string.action_sign_out), onClick = { confirm = true },
            description = stringResource(R.string.action_sign_out_desc), danger = true)
        if (confirm) {
            ChoiceDialog(
                title = stringResource(R.string.action_sign_out_confirm),
                options = listOf(false, true),
                selected = false,
                label = { stringResource(if (it) R.string.action_sign_out else R.string.action_cancel) },
                onSelect = { if (it) vm.signOut() },
                onDismiss = { confirm = false },
            )
        }
    }
}

private fun maskKey(key: String): String =
    if (key.length <= 6) "••••••" else "••••••••" + key.takeLast(4)

fun LazyListScope.advancedPane(config: SlideshowConfig, vm: SettingsViewModel) {
    item { SectionHeader(stringResource(R.string.section_library)) }
    item {
        ChoiceRow(
            icon = Icons.Rounded.Update,
            title = stringResource(R.string.setting_refresh_interval),
            description = stringResource(R.string.setting_refresh_interval_desc),
            options = SlideshowConfig.REFRESH_INTERVALS,
            selected = config.refreshHours,
            label = { pluralStringResource(R.plurals.duration_hours, it, it) },
            onSelect = { v -> vm.update { it.copy(refreshHours = v) } },
        )
    }
    item {
        val status by vm.status.collectAsStateWithLifecycle()
        ActionRow(
            icon = Icons.Rounded.Refresh,
            title = stringResource(R.string.action_refresh),
            description = stringResource(R.string.action_refresh_desc),
            onClick = vm::refreshNow,
            enabled = !status.refreshing,
            trailing = if (status.refreshing) ({ Spinner() }) else null,
        )
    }
    item {
        ActionRow(Icons.Rounded.CleaningServices, stringResource(R.string.action_clear_cache), onClick = vm::clearCache,
            description = stringResource(R.string.action_clear_cache_desc))
    }
    item { SectionHeader(stringResource(R.string.section_reset)) }
    item {
        ActionRow(
            icon = Icons.Rounded.DeleteOutline,
            title = stringResource(R.string.action_reset_display),
            description = stringResource(R.string.action_reset_display_desc),
            onClick = {
                vm.update { c ->
                    SlideshowConfig(serverUrl = c.serverUrl, apiKey = c.apiKey, excludedAlbumIds = c.excludedAlbumIds)
                }
            },
        )
    }
}

fun LazyListScope.aboutPane(vm: SettingsViewModel) {
    item { SectionHeader(stringResource(R.string.section_screensaver)) }
    item {
        val context = LocalContext.current
        var active by remember { mutableStateOf(vm.isScreensaverActive()) }
        LaunchedEffect(Unit) { active = vm.isScreensaverActive() }
        ActionRow(
            icon = if (active) Icons.Rounded.CheckCircle else Icons.Rounded.Collections,
            title = stringResource(if (active) R.string.screensaver_active else R.string.screensaver_inactive),
            description = stringResource(if (active) R.string.screensaver_active_desc else R.string.screensaver_inactive_desc),
            onClick = { active = vm.isScreensaverActive() },
        )
        if (!active) {
            InfoBlock(
                title = stringResource(R.string.screensaver_how_title),
                body = stringResource(R.string.screensaver_how_body),
                code = screensaverCommand(context.packageName),
            )
        }
    }
    item { SectionHeader(stringResource(R.string.section_app)) }
    item {
        ActionRow(
            icon = Icons.Rounded.Collections,
            title = stringResource(R.string.app_name),
            description = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
            onClick = {},
        )
    }
    item {
        InfoBlock(stringResource(R.string.about_privacy_title), stringResource(R.string.about_privacy_body))
    }
}

fun screensaverCommand(packageName: String) =
    "adb shell settings put secure screensaver_components \\\n  $packageName/com.roylet.immichcast.dream.PhotoDreamService"

