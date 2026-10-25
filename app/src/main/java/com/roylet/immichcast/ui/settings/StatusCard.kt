package com.roylet.immichcast.ui.settings

import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.roylet.immichcast.R
import com.roylet.immichcast.core.Season
import com.roylet.immichcast.core.SlideshowConfig
import com.roylet.immichcast.core.SlideshowMode
import com.roylet.immichcast.ui.StatusState
import com.roylet.immichcast.ui.accent
import com.roylet.immichcast.ui.components.Spinner
import com.roylet.immichcast.ui.labelRes
import com.roylet.immichcast.ui.monthsRes
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.roylet.immichcast.ui.theme.BrutalBadge
import com.roylet.immichcast.ui.theme.BrutalCard
import com.roylet.immichcast.ui.theme.Palette
import java.text.NumberFormat

/** Summary of what the screensaver will show right now. */
@Composable
fun StatusCard(status: StatusState, config: SlideshowConfig, season: Season?, modifier: Modifier = Modifier) {
    val accent = season?.accent ?: config.mode.accent
    val stats = status.playlist?.stats
    val numbers = NumberFormat.getIntegerInstance()
    BrutalCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BrutalBadge(
                    when {
                        season != null -> stringResource(season.labelRes)
                        config.mode == SlideshowMode.RECENT -> recentLabel(config.recentDays)
                        else -> stringResource(config.mode.labelRes)
                    },
                    accent,
                )
                if (status.refreshing) Spinner(14.dp, accent)
            }
            val subtitle = when (config.mode) {
                SlideshowMode.SEASONAL -> season?.let { stringResource(it.monthsRes) }
                SlideshowMode.ON_THIS_DAY -> stringResource(
                    R.string.on_this_day_window,
                    LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM")),
                )
                SlideshowMode.RANDOM -> stringResource(R.string.mode_random_desc)
                SlideshowMode.RECENT -> null
            }
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted) }
            when {
                stats != null -> {
                    Text(
                        pluralStringResource(R.plurals.status_photos, stats.total, numbers.format(stats.total)),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    val details = buildList {
                        if (stats.years > 0) add(pluralStringResource(R.plurals.status_years, stats.years, stats.years))
                        if (stats.excludedCount > 0) add(stringResource(R.string.status_excluded, numbers.format(stats.excludedCount)))
                    }.joinToString(" · ")
                    if (details.isNotEmpty()) {
                        Text(details, style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted)
                    }
                    if (stats.fellBackToAll) {
                        Text(stringResource(R.string.status_fallback), style = MaterialTheme.typography.bodySmall, color = Palette.Warning)
                    }
                    Text(
                        stringResource(R.string.status_updated, formatTime(stats.builtAtMillis)).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Palette.TextMuted,
                    )
                }
                status.refreshing -> Text(stringResource(R.string.status_building), style = MaterialTheme.typography.bodyMedium)
                else -> Text(stringResource(R.string.status_not_built), style = MaterialTheme.typography.bodyMedium)
            }
            status.error?.let {
                Text(stringResource(R.string.status_error, it), style = MaterialTheme.typography.bodySmall, color = Palette.Destructive)
            }
        }
    }
}

@Composable
private fun formatTime(millis: Long): String {
    val context = LocalContext.current
    return if (DateUtils.isToday(millis)) DateFormat.getTimeFormat(context).format(millis)
    else DateUtils.formatDateTime(context, millis, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH)
}
