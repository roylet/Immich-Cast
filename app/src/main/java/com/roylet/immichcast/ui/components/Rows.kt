package com.roylet.immichcast.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.roylet.immichcast.ui.theme.BrutalCard
import com.roylet.immichcast.ui.theme.BrutalCheck
import com.roylet.immichcast.ui.theme.BrutalDivider
import com.roylet.immichcast.ui.theme.BrutalStyle
import com.roylet.immichcast.ui.theme.BrutalSurface
import com.roylet.immichcast.ui.theme.BrutalToggle
import com.roylet.immichcast.ui.theme.Palette

/** A titled card holding a group of setting rows, like ImmichUp's step cards. */
@Composable
fun SettingsSection(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().padding(top = 22.dp, end = 6.dp, bottom = 4.dp)) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = Palette.TextMuted,
            modifier = Modifier.padding(start = 2.dp, bottom = 10.dp),
        )
        BrutalCard(Modifier.fillMaxWidth(), content = content)
    }
}

/** Row inside a [SettingsSection]; turns orange when focused. */
@Composable
private fun SettingItem(
    icon: ImageVector?,
    title: String,
    description: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    danger: Boolean = false,
    trailing: (@Composable (focused: Boolean) -> Unit)? = null,
) {
    BrutalSurface(onClick, modifier.fillMaxWidth(), style = BrutalStyle.GHOST, enabled = enabled, shadow = 0.dp) { focused ->
        val tint = if (danger && !focused) Palette.Destructive else LocalContentColor.current
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let {
                Icon(it, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(16.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
                description?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (focused) Palette.OnPrimary.copy(alpha = 0.75f) else Palette.TextMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            trailing?.let {
                Spacer(Modifier.width(12.dp))
                it(focused)
            }
        }
    }
}

/** Separator between rows of a [SettingsSection]. */
@Composable
fun RowDivider() = BrutalDivider()

@Composable
fun ValueRow(
    icon: ImageVector?,
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
) = SettingItem(icon, title, description, onClick, modifier) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(value, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.width(4.dp))
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun SwitchRow(
    icon: ImageVector?,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
) = SettingItem(icon, title, description, onClick = { onCheckedChange(!checked) }, modifier) { focused ->
    BrutalToggle(checked, focused)
}

@Composable
fun CheckRow(title: String, checked: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, description: String? = null) =
    SettingItem(null, title, description, onClick, modifier) { BrutalCheck(checked) }

@Composable
fun ActionRow(
    icon: ImageVector?,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    danger: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) = SettingItem(icon, title, description, onClick, modifier, enabled, danger, trailing?.let { t -> { _ -> t() } })

/** A block of read-only text inside a section, e.g. setup instructions. */
@Composable
fun InfoBlock(title: String, body: String, modifier: Modifier = Modifier, code: String? = null) {
    Column(modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(body, style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted)
        code?.let { CodeBlock(it) }
    }
}
