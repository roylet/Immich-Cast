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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text

val BrutalShape = RoundedCornerShape(10.dp)
val BrutalBorder = 1.5.dp

/** Black outline on orange elements (buttons, tags, number badges). */
val InkOutline = 2.dp

/**
 * Shadow for inked (black-outlined) elements: a deep shade of their own colour, so the black
 * edge reads against the dark page the way the banner's tag does against the photo scenes.
 */
fun inkShadow(color: Color): Color = lerp(color, Color.Black, 0.6f)

/** A solid, unblurred copy of [shape] drawn down-right of the content: the theme's offset shadow. */
fun Modifier.hardShadow(offset: Dp = 4.dp, color: Color = Palette.Shadow, shape: Shape = BrutalShape): Modifier =
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
    borderColor: Color = Palette.Line,
    shadow: Dp = 4.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .hardShadow(shadow)
            .background(container, BrutalShape)
            .border(BrutalBorder, borderColor, BrutalShape),
        content = content,
    )
}

enum class BrutalStyle { PRIMARY, OUTLINE, GHOST }

/**
 * Focusable surface. Focus is what a TV remote "hovers": a focused surface gets a warm tint and an
 * orange outline (primary buttons brighten) and lifts slightly off its shadow, so it reads from
 * the sofa without a glaring block of colour.
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
    val primary = style == BrutalStyle.PRIMARY
    val focusedContainer = if (primary) Palette.PrimaryFocused else Palette.FocusFill
    val contentColor = if (primary) Palette.OnPrimary else Palette.Text
    val outlined = style != BrutalStyle.GHOST || selected
    val lift = if (focused && outlined) 2.dp else 0.dp
    Surface(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        modifier = modifier
            // Lift only the drawing: moving the bounds would shift the focus rect, and D-pad search
            // would then treat neighbours in a column as being "to the right" of the focused item.
            .drawWithContent { translate(-lift.toPx(), -lift.toPx()) { this@drawWithContent.drawContent() } }
            .hardShadow(if (outlined) shadow + lift else 0.dp, if (primary) inkShadow(Palette.Primary) else Palette.Shadow),
        shape = ClickableSurfaceDefaults.shape(BrutalShape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f, pressedScale = 1f),
        border = ClickableSurfaceDefaults.border(
            border = when {
                // Orange buttons keep the black inked edge from the banner's tag.
                primary -> Border(BorderStroke(InkOutline, Palette.OnPrimary), shape = BrutalShape)
                outlined && !selected -> Border(BorderStroke(BrutalBorder, Palette.Line), shape = BrutalShape)
                else -> Border.None
            },
            focusedBorder = if (primary) {
                Border(BorderStroke(InkOutline, Palette.OnPrimary), shape = BrutalShape)
            } else {
                Border(BorderStroke(2.dp, Palette.Primary), shape = BrutalShape)
            },
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = container,
            contentColor = contentColor,
            focusedContainerColor = focusedContainer,
            focusedContentColor = contentColor,
            pressedContainerColor = focusedContainer,
            pressedContentColor = contentColor,
            disabledContainerColor = container.copy(alpha = 0.4f),
            disabledContentColor = Palette.TextMuted,
        ),
        glow = ClickableSurfaceDefaults.glow(),
    ) {
        content(focused)
    }
}

/** Uppercase button in the theme's display type. */
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

/** Small uppercase tag with a black outline, e.g. a season or "Excluded". */
@Composable
fun BrutalBadge(text: String, container: Color, modifier: Modifier = Modifier, content: Color = Palette.OnPrimary) {
    Box(
        modifier
            .hardShadow(2.dp, inkShadow(container), RoundedCornerShape(4.dp))
            .background(container, RoundedCornerShape(4.dp))
            .border(InkOutline, Palette.OnPrimary, RoundedCornerShape(4.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = content)
    }
}

/** Toggle: orange track when on, outline in the current content colour when off. */
@Composable
fun BrutalToggle(checked: Boolean, focused: Boolean, modifier: Modifier = Modifier) {
    val ink = LocalContentColor.current
    val track = if (checked) Palette.Primary else Color.Transparent
    Box(
        modifier
            .size(46.dp, 26.dp)
            .background(track, RoundedCornerShape(13.dp))
            .border(BrutalBorder, if (checked) track else ink.copy(alpha = 0.6f), RoundedCornerShape(13.dp))
            .padding(4.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier.size(16.dp).background(
                if (checked) Palette.OnPrimary else ink.copy(alpha = if (focused) 1f else 0.8f),
                RoundedCornerShape(8.dp),
            ),
        )
    }
}

/** Check/radio indicator, filled when selected. */
@Composable
fun BrutalCheck(checked: Boolean, modifier: Modifier = Modifier) {
    val ink = LocalContentColor.current
    Box(
        modifier
            .size(22.dp)
            .background(if (checked) Palette.Primary else Color.Transparent, RoundedCornerShape(6.dp))
            .border(BrutalBorder, if (checked) Palette.Primary else ink.copy(alpha = 0.6f), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Palette.OnPrimary)
        }
    }
}

/** Thin rule used between rows inside a card. */
@Composable
fun BrutalDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Palette.Line))
}
