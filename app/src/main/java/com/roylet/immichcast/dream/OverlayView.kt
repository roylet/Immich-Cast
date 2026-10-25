package com.roylet.immichcast.dream

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import com.roylet.immichcast.core.ClockFormat
import com.roylet.immichcast.core.OverlayItem
import com.roylet.immichcast.core.OverlayPosition
import com.roylet.immichcast.core.SlideshowConfig

/** Text shown over a slide; each line is optional and hidden when empty. */
internal data class SlideCaption(
    val date: String? = null,
    val location: String? = null,
    val album: String? = null,
    val description: String? = null,
    val season: String? = null,
)

/** Clock and photo details, drawn over a soft gradient inside the TV safe area. */
internal class OverlayView(context: Context) : FrameLayout(context) {

    private val scrim = View(context)
    private val column = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

    private val clock = TextClock(context).apply {
        style(sizeSp = 56f, weight = Typeface.create("sans-serif-light", Typeface.NORMAL))
    }
    private val dateLine = TextView(context).apply { style(22f, Typeface.create("sans-serif-medium", Typeface.NORMAL)) }
    private val locationLine = TextView(context).apply { style(18f, alpha = 0.85f) }
    private val albumLine = TextView(context).apply { style(18f, alpha = 0.85f) }
    private val descriptionLine = TextView(context).apply { style(18f, alpha = 0.85f); maxLines = 2 }
    private val seasonLine = TextView(context).apply {
        style(13f, Typeface.create("sans-serif-medium", Typeface.NORMAL), alpha = 0.75f)
        letterSpacing = 0.12f
        isAllCaps = true
    }
    private val details = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        listOf(seasonLine, dateLine, locationLine, albumLine, descriptionLine).forEach { addView(it) }
    }

    private var items: Set<OverlayItem> = emptySet()

    init {
        column.addView(clock)
        column.addView(details)
        addView(scrim)
        addView(column)
        isClickable = false
        isFocusable = false
    }

    fun configure(config: SlideshowConfig) {
        items = config.overlayItems
        visibility = if (items.isEmpty()) View.GONE else View.VISIBLE

        clock.visibility = if (OverlayItem.CLOCK in items) View.VISIBLE else View.GONE
        when (config.clockFormat) {
            ClockFormat.SYSTEM -> {
                clock.format12Hour = "h:mm"
                clock.format24Hour = "H:mm"
            }
            ClockFormat.H12 -> {
                clock.format12Hour = "h:mm"
                clock.format24Hour = "h:mm"
            }
            ClockFormat.H24 -> {
                clock.format12Hour = "HH:mm"
                clock.format24Hour = "HH:mm"
            }
        }

        val top = config.overlayPosition == OverlayPosition.TOP_START || config.overlayPosition == OverlayPosition.TOP_END
        val end = config.overlayPosition == OverlayPosition.BOTTOM_END || config.overlayPosition == OverlayPosition.TOP_END
        val vertical = if (top) Gravity.TOP else Gravity.BOTTOM
        val horizontal = if (end) Gravity.END else Gravity.START

        scrim.background = GradientDrawable(
            if (top) GradientDrawable.Orientation.TOP_BOTTOM else GradientDrawable.Orientation.BOTTOM_TOP,
            intArrayOf(Color.argb(150, 0, 0, 0), Color.argb(60, 0, 0, 0), Color.TRANSPARENT),
        )
        scrim.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, (resources.displayMetrics.heightPixels * 0.42f).toInt(), vertical)

        // Overscan-safe margins: 5% of a 960x540dp TV canvas.
        column.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, vertical or horizontal).apply {
            val h = dp(56)
            val v = dp(36)
            setMargins(h, v, h, v)
        }
        column.gravity = horizontal
        details.gravity = horizontal
        listOf(clock, dateLine, locationLine, albumLine, descriptionLine, seasonLine).forEach { it.gravity = horizontal }
    }

    fun setCaption(caption: SlideCaption, animate: Boolean) {
        if (visibility != View.VISIBLE) return
        val apply = {
            seasonLine.show(caption.season, OverlayItem.SEASON)
            dateLine.show(caption.date, OverlayItem.DATE)
            locationLine.show(caption.location, OverlayItem.LOCATION)
            albumLine.show(caption.album, OverlayItem.ALBUM)
            descriptionLine.show(caption.description, OverlayItem.DESCRIPTION)
        }
        if (!animate) {
            apply()
            return
        }
        details.animate().alpha(0f).setDuration(350).withEndAction {
            apply()
            details.animate().alpha(1f).setDuration(650).start()
        }.start()
    }

    private fun TextView.show(text: String?, item: OverlayItem) {
        val visible = item in items && !text.isNullOrBlank()
        this.text = if (visible) text else null
        visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun TextView.style(sizeSp: Float, weight: Typeface? = null, alpha: Float = 1f) {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setTextColor(Color.WHITE)
        this.alpha = alpha
        weight?.let { typeface = it }
        setShadowLayer(dp(6).toFloat(), 0f, dp(1).toFloat(), Color.argb(160, 0, 0, 0))
        includeFontPadding = false
        setPadding(0, dp(2), 0, dp(2))
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
