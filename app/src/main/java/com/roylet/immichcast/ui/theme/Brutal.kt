package com.roylet.immichcast.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text

val BrutalShape = RoundedCornerShape(4.dp)
val BrutalBorder = 2.dp

/** A solid, unblurred copy of [shape] drawn down-right of the content: the signature brutalist shadow. */
fun Modifier.hardShadow(offset: Dp = 4.dp, color: Color = Palette.Ink, shape: Shape = BrutalShape): Modifier =
    if (offset <= 0.dp) this else drawBehind {
        val outline = shape.createOutline(size, layoutDirection, this)
        translate(offset.toPx(), offset.toPx()) { drawOutline(outline, color) }
    }

/** ImmichUp's dotted page background. */
fun Modifier.dotGrid(color: Color = Palette.Dot, spacing: Dp = 22.dp, radius: Dp = 1.5.dp): Modifier = drawBehind {
    val step = spacing.toPx()
    val r = radius.toPx()
    var y = step / 2
    while (y < size.height) {
        var x = step / 2
        while (x < size.width) {
            drawCircle(color, r, Offset(x, y))
            x += step
        }
        y += step
    }
}

/** Bordered card with a hard shadow. */
@Composable
fun BrutalCard(
    modifier: Modifier = Modifier,
    container: Color = Palette.Card,
    borderColor: Color = Palette.Ink,
    shadow: Dp = 4.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .hardShadow(shadow, borderColor)
            .background(container, BrutalShape)
            .border(BrutalBorder, borderColor, BrutalShape),
        content = content,
    )
}

enum class BrutalStyle { PRIMARY, OUTLINE, GHOST }

/**
 * Focusable surface with the brutalist treatment. Focus is what a TV remote "hovers", so a
 * focused surface turns orange and lifts up-left off its shadow; that reads clearly from the sofa.
 */
@Composable
fun BrutalSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: BrutalStyle = BrutalStyle.OUTLINE,
    selected: Boolean = false,
    enabled: Boolean = true,
    shadow: Dp = 4.dp,
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val container = when {
        style == BrutalStyle.PRIMARY -> Palette.Primary
        selected -> Palette.Secondary
        style == BrutalStyle.GHOST -> Color.Transparent
        else -> Palette.Card
    }
    val focusedContainer = if (style == BrutalStyle.PRIMARY) Palette.Ink else Palette.Primary
    val outlined = style != BrutalStyle.GHOST || selected
    val lift = if (focused && outlined) 2.dp else 0.dp
    Surface(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        modifier = modifier
            .offset(-lift, -lift)
            .hardShadow(if (outlined) shadow + lift else 0.dp),
        shape = ClickableSurfaceDefaults.shape(BrutalShape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f, pressedScale = 1f),
        border = ClickableSurfaceDefaults.border(
            border = if (outlined) Border(BorderStroke(BrutalBorder, Palette.Ink), shape = BrutalShape) else Border.None,
            focusedBorder = Border(BorderStroke(BrutalBorder, Palette.Ink), shape = BrutalShape),
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = container,
            contentColor = if (style == BrutalStyle.PRIMARY) Palette.OnPrimary else Palette.Text,
            focusedContainerColor = focusedContainer,
            focusedContentColor = Palette.OnPrimary,
            pressedContainerColor = focusedContainer,
            pressedContentColor = Palette.OnPrimary,
            disabledContainerColor = container.copy(alpha = 0.4f),
            disabledContentColor = Palette.TextMuted,
        ),
        glow = ClickableSurfaceDefaults.glow(),
    ) {
        content(focused)
    }
}

/** ImmichUp's chunky uppercase button. */
@Composable
fun BrutalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: BrutalStyle = BrutalStyle.PRIMARY,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
) {
    BrutalSurface(onClick, modifier.height(44.dp), style = style, enabled = enabled) {
        Row(
            Modifier.align(Alignment.Center).padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let {
                Box(Modifier.size(18.dp)) { it() }
                Spacer(Modifier.size(10.dp))
            }
            Text(text.uppercase(), style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Small uppercase tag with an outline, e.g. a season or "Excluded". */
@Composable
fun BrutalBadge(text: String, container: Color, modifier: Modifier = Modifier, content: Color = Palette.OnPrimary) {
    Box(
        modifier
            .background(container, RoundedCornerShape(3.dp))
            .border(BrutalBorder, Palette.OnPrimary, RoundedCornerShape(3.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = content)
    }
}

/** Square toggle drawn in the current content colour, so it works on focused (orange) rows too. */
@Composable
fun BrutalToggle(checked: Boolean, focused: Boolean, modifier: Modifier = Modifier) {
    val ink = LocalContentColor.current
    val track = when {
        checked && focused -> Palette.OnPrimary
        checked -> Palette.Primary
        else -> Color.Transparent
    }
    Box(
        modifier
            .size(46.dp, 26.dp)
            .background(track, RoundedCornerShape(3.dp))
            .border(BrutalBorder, ink, RoundedCornerShape(3.dp))
            .padding(4.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier.size(16.dp).background(
                if (checked) (if (focused) Palette.Primary else Palette.OnPrimary) else ink,
                RoundedCornerShape(2.dp),
            ),
        )
    }
}

/** Square check/radio indicator, filled when selected. */
@Composable
fun BrutalCheck(checked: Boolean, modifier: Modifier = Modifier) {
    val ink = LocalContentColor.current
    Box(
        modifier
            .size(22.dp)
            .background(if (checked) ink else Color.Transparent, RoundedCornerShape(3.dp))
            .border(BrutalBorder, ink, RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp),
                tint = if (ink == Palette.OnPrimary) Palette.Primary else Palette.OnPrimary)
        }
    }
}

/** 2dp rule used between rows inside a card. */
@Composable
fun BrutalDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(BrutalBorder).background(Palette.Ink))
}
