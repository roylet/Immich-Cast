package com.roylet.immichcast.dream

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import com.roylet.immichcast.core.ImageFit

/** One full-screen slide: an optional blurred backdrop with the photo on top. */
internal class SlideLayer(context: Context) : FrameLayout(context) {

    val backdrop = ImageView(context).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        setRenderEffect(RenderEffect.createBlurEffect(BLUR_RADIUS, BLUR_RADIUS, Shader.TileMode.CLAMP))
        setColorFilter(Color.rgb(110, 110, 110), PorterDuff.Mode.MULTIPLY)
        // Slightly oversized so the blur's soft edges stay off-screen.
        scaleX = 1.1f
        scaleY = 1.1f
    }

    val image = ImageView(context)

    init {
        setBackgroundColor(Color.BLACK)
        addView(backdrop, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(image, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        alpha = 0f
    }

    fun bind(drawable: Drawable?, fit: ImageFit) {
        image.animate().cancel()
        image.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        image.scaleX = 1f
        image.scaleY = 1f
        image.translationX = 0f
        image.translationY = 0f
        image.scaleType = if (fit == ImageFit.FILL) ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.FIT_CENTER
        image.setImageDrawable(drawable)
        val blurred = fit == ImageFit.FIT_BLURRED && drawable != null
        backdrop.visibility = if (blurred) View.VISIBLE else View.GONE
        // A separate drawable instance: the backdrop's dimming filter must not touch the photo.
        backdrop.setImageDrawable(if (blurred) drawable?.constantState?.newDrawable(resources)?.mutate() else null)
    }

    /** A panorama at full screen height, wider than the screen, ready to pan across. */
    fun bindWide(drawable: Drawable, screenWidth: Int, screenHeight: Int) {
        bind(drawable, ImageFit.FILL)
        val width = (drawable.intrinsicWidth.toFloat() * screenHeight / drawable.intrinsicHeight.coerceAtLeast(1)).toInt()
        image.layoutParams = LayoutParams(width.coerceAtLeast(screenWidth), LayoutParams.MATCH_PARENT)
        image.scaleType = ImageView.ScaleType.FIT_XY
        backdrop.visibility = View.GONE
        backdrop.setImageDrawable(null)
    }

    fun clear() {
        image.animate().cancel()
        image.setImageDrawable(null)
        backdrop.setImageDrawable(null)
    }

    private companion object {
        const val BLUR_RADIUS = 90f
    }
}
