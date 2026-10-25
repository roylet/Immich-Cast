package com.roylet.immichcast.ui.settings

import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.roylet.immichcast.R
import com.roylet.immichcast.core.Season
import com.roylet.immichcast.ui.StatusState
import com.roylet.immichcast.ui.accent
import com.roylet.immichcast.ui.components.Pill
import com.roylet.immichcast.ui.components.Spinner
import com.roylet.immichcast.ui.labelRes
import com.roylet.immichcast.ui.monthsRes
import java.text.NumberFormat

/** Summary of what the screensaver will show right now. */
@Composable
fun StatusCard(status: StatusState, season: Season?, modifier: Modifier = Modifier) {
    val accent = season?.accent ?: MaterialTheme.colorScheme.primary
    val stats = status.playlist?.stats
    val numbers = NumberFormat.getIntegerInstance()
    Column(
        modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(listOf(accent.copy(alpha = 0.22f), MaterialTheme.colorScheme.surfaceVariant)),
                RoundedCornerShape(18.dp),
            )
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Pill(stringResource(season?.labelRes ?: R.string.season_all), accent)
            if (status.refreshing) Spinner(14.dp, accent)
        }
        if (season != null) {
            Text(stringResource(season.monthsRes), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        when {
            stats != null -> {
                Text(
                    pluralStringResource(R.plurals.status_photos, stats.total, numbers.format(stats.total)),
                    style = MaterialTheme.typography.headlineSmall,
                )
                val details = buildList {
                    if (stats.seasonYears > 0) add(pluralStringResource(R.plurals.status_years, stats.seasonYears, stats.seasonYears))
                    if (stats.excludedCount > 0) add(stringResource(R.string.status_excluded, numbers.format(stats.excludedCount)))
                }.joinToString(" · ")
                if (details.isNotEmpty()) {
                    Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (stats.fellBackToAll) {
                    Text(stringResource(R.string.status_fallback), style = MaterialTheme.typography.bodySmall, color = accent)
                }
                Text(
                    stringResource(R.string.status_updated, formatTime(stats.builtAtMillis)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            status.refreshing -> Text(stringResource(R.string.status_building), style = MaterialTheme.typography.bodyMedium)
            else -> Text(stringResource(R.string.status_not_built), style = MaterialTheme.typography.bodyMedium)
        }
        status.error?.let {
            Text(stringResource(R.string.status_error, it), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun formatTime(millis: Long): String {
    val context = LocalContext.current
    return if (DateUtils.isToday(millis)) DateFormat.getTimeFormat(context).format(millis)
    else DateUtils.formatDateTime(context, millis, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH)
}
