package com.roylet.immichcast.dream

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.core.content.res.ResourcesCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.roylet.immichcast.ImmichCastApp
import com.roylet.immichcast.R
import com.roylet.immichcast.core.ImageFit
import com.roylet.immichcast.core.Panorama
import com.roylet.immichcast.core.SlideshowConfig
import com.roylet.immichcast.core.Transition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.random.Random

/**
 * Full-screen slideshow surface shared by the screensaver and the in-app preview.
 * Call [start] when visible and [stop] when hidden; all work is tied to that window.
 */
@OptIn(UnstableApi::class)
class SlideshowView(context: Context) : FrameLayout(context) {

    private val app = context.applicationContext as ImmichCastApp

    private val layers = arrayOf(SlideLayer(context), SlideLayer(context))
    private var front = 0
    private var hasShownSlide = false

    private val videoView = PlayerView(context).apply {
        useController = false
        setShutterBackgroundColor(Color.TRANSPARENT)
        setKeepContentOnPlayerReset(true)
        visibility = View.GONE
        alpha = 0f
    }
    private var player: ExoPlayer? = null

    // Kept behind the photo layers until a 360° slide brings it forward.
    private val sphere = SphereView(context).apply { alpha = 0f }
    private var sphereShowing = false

    private val overlay = OverlayView(context)
    private val dim = View(context).apply { setBackgroundColor(Color.BLACK); alpha = 0f }

    private val messageTitle = text(28f, ResourcesCompat.getFont(context, R.font.archivo_black), 1f)
    private val messageBody = text(17f, null, 0.72f).apply { maxWidth = dp(560) }
    private val messageSpinner = ProgressBar(context).apply {
        isIndeterminate = true
        indeterminateTintList = android.content.res.ColorStateList.valueOf(Color.rgb(255, 90, 31))
    }
    private val message = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        addView(ImageView(context).apply { setImageResource(R.drawable.ic_logo) }, LinearLayout.LayoutParams(dp(88), dp(88)))
        addView(messageTitle, spaced(dp(20)))
        addView(messageBody, spaced(dp(10)))
        addView(messageSpinner, LinearLayout.LayoutParams(dp(32), dp(32)).apply { topMargin = dp(28) })
        alpha = 0f
        visibility = View.GONE
    }

    // Brutalist tag, matching the settings UI: outlined, square-ish, uppercase Archivo.
    private val notice = text(13f, ResourcesCompat.getFont(context, R.font.archivo_extrabold), 1f).apply {
        background = GradientDrawable().apply {
            cornerRadius = dp(4).toFloat()
            setColor(Color.argb(230, 31, 29, 26))
            setStroke(dp(2), Color.rgb(243, 235, 221))
        }
        isAllCaps = true
        letterSpacing = 0.08f
        setPadding(dp(18), dp(10), dp(18), dp(10))
        alpha = 0f
    }

    private var scope: CoroutineScope? = null
    private var controller: SlideshowController? = null

    init {
        setBackgroundColor(Color.BLACK)
        addView(sphere, match())
        layers.forEach { addView(it, match()) }
        addView(videoView, match())
        addView(overlay, match())
        addView(dim, match())
        addView(message, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        addView(notice, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
            topMargin = dp(36)
        })
    }

    fun start() {
        if (scope != null) return
        val newScope = MainScope()
        scope = newScope
        controller = SlideshowController(app, this, newScope).also { it.start() }
    }

    fun stop() {
        controller = null
        scope?.cancel()
        scope = null
        player?.release()
        player = null
        layers.forEach { it.animate().cancel() }
        sphere.idle()
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }

    /** Shows a short hint at the top of the screen, e.g. how to leave the preview. */
    fun showNoticeText(text: String, durationMs: Long) = showNotice(text, durationMs)

    internal fun applyConfig(config: SlideshowConfig) {
        overlay.configure(config)
        dim.alpha = config.dimPercent / 100f
    }

    /** Size images are decoded to: the screen, so full-quality originals are not decoded at 50 MP. */
    internal val targetWidth: Int get() = width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
    internal val targetHeight: Int get() = height.takeIf { it > 0 } ?: resources.displayMetrics.heightPixels

    /** Shows [slide] for [durationMs]; panoramas pan and 360° photos turn over that time. */
    internal fun showSlide(slide: LoadedSlide, config: SlideshowConfig, durationMs: Long) {
        val fadeMs = if (config.transition == Transition.NONE || !hasShownSlide) 0L else FADE_MS
        val bitmap = (slide.drawable as? BitmapDrawable)?.bitmap
        if (slide.kind == Panorama.SPHERE && bitmap != null) {
            showSphere(bitmap, config.turnSeconds * 1000L, fadeMs)
        } else {
            showFlat(slide, config, durationMs, fadeMs)
        }
        overlay.setCaption(slide.caption, animate = hasShownSlide)
        hasShownSlide = true
        hideMessage()
    }

    private fun showFlat(slide: LoadedSlide, config: SlideshowConfig, durationMs: Long, fadeMs: Long) {
        val incoming = layers[1 - front]
        val outgoing = layers[front]
        front = 1 - front

        if (slide.kind == Panorama.WIDE && slide.drawable != null) {
            incoming.bindWide(slide.drawable, targetWidth, targetHeight)
        } else {
            incoming.bind(slide.drawable, config.fit)
        }
        incoming.bringToFront()
        bringChromeToFront()

        val leavingSphere = sphereShowing
        sphereShowing = false
        val done = {
            outgoing.alpha = 0f
            outgoing.clear()
            if (leavingSphere) {
                sphere.alpha = 0f
                sphere.idle()
            }
        }
        if (fadeMs == 0L) {
            incoming.alpha = 1f
            done()
        } else {
            incoming.alpha = 0f
            incoming.animate().alpha(1f).setDuration(fadeMs).setInterpolator(DecelerateInterpolator())
                .withLayer()
                .withEndAction(done)
                .start()
        }
        when {
            slide.kind == Panorama.WIDE -> pan(incoming, durationMs + fadeMs)
            config.transition == Transition.KEN_BURNS -> kenBurns(incoming, durationMs + fadeMs)
        }
    }

    private fun showSphere(bitmap: android.graphics.Bitmap, turnMs: Long, fadeMs: Long) {
        val outgoing = layers[front]
        sphere.showImage(bitmap, turnMs)
        if (sphereShowing) return // already on screen: cut straight to the next one
        sphereShowing = true
        sphere.bringToFront()
        bringChromeToFront()
        sphere.animate().cancel()
        sphere.alpha = if (fadeMs == 0L) 1f else 0f
        sphere.animate().alpha(1f).setDuration(fadeMs).setInterpolator(DecelerateInterpolator())
            .withEndAction { outgoing.alpha = 0f; outgoing.clear() }
            .start()
    }

    private fun bringChromeToFront() {
        videoView.bringToFront()
        overlay.bringToFront()
        dim.bringToFront()
        message.bringToFront()
        notice.bringToFront()
    }

    /** Slides a panorama from one end to the other, in a random direction. */
    private fun pan(layer: SlideLayer, durationMs: Long) {
        val travel = (layer.image.layoutParams.width - targetWidth).coerceAtLeast(0).toFloat()
        val leftToRight = Random.nextBoolean()
        layer.image.apply {
            translationX = if (leftToRight) 0f else -travel
            animate().translationX(if (leftToRight) -travel else 0f)
                .setDuration(durationMs).setInterpolator(AccelerateDecelerateInterpolator()).start()
        }
    }

    private fun kenBurns(layer: SlideLayer, durationMs: Long) {
        val zoomIn = Random.nextBoolean()
        val from = if (zoomIn) 1f else KEN_BURNS_SCALE
        val to = if (zoomIn) KEN_BURNS_SCALE else 1f
        val drift = (KEN_BURNS_SCALE - 1f) / 2f
        val dx = (Random.nextFloat() * 2 - 1) * drift * targetWidth
        val dy = (Random.nextFloat() * 2 - 1) * drift * targetHeight
        layer.image.apply {
            scaleX = from; scaleY = from
            translationX = if (zoomIn) 0f else dx; translationY = if (zoomIn) 0f else dy
            animate().scaleX(to).scaleY(to)
                .translationX(if (zoomIn) dx else 0f).translationY(if (zoomIn) dy else 0f)
                .setDuration(durationMs).setInterpolator(LinearInterpolator()).start()
        }
    }

    /**
     * Plays [url] muted over the current slide; returns when it ends, fails, or after [maxMs].
     * With [limitMs], it also stops that long after the first frame, so loading time doesn't count.
     * With [inSphere], a 360° video plays inside the turning sphere view instead of flat.
     */
    internal suspend fun playVideo(url: String, fit: ImageFit, maxMs: Long, limitMs: Long? = null, inSphere: Boolean = false) {
        val exo = player ?: ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(OkHttpDataSource.Factory(app.httpClient)))
            .build().also {
                it.volume = 0f
                it.repeatMode = Player.REPEAT_MODE_OFF
                player = it
            }
        val sphereSurface = if (inSphere && sphereShowing) sphere.videoSurface() else null
        if (sphereSurface != null) {
            videoView.player = null
            exo.setVideoSurface(sphereSurface)
        } else {
            videoView.player = exo
            videoView.resizeMode = if (fit == ImageFit.FILL) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT
            videoView.visibility = View.VISIBLE
            bringChromeToFront()
        }

        exo.setMediaItem(MediaItem.fromUri(url))
        exo.prepare()
        exo.play()
        var stopAtLimit: Runnable? = null
        try {
            kotlinx.coroutines.withTimeoutOrNull(maxMs) {
                suspendCancellableCoroutine { cont ->
                    val listener = object : Player.Listener {
                        override fun onRenderedFirstFrame() {
                            if (sphereSurface == null) videoView.animate().alpha(1f).setDuration(FADE_MS).start()
                            if (limitMs != null && stopAtLimit == null) {
                                stopAtLimit = Runnable { if (cont.isActive) cont.resume(Unit) }
                                    .also { videoView.postDelayed(it, limitMs) }
                            }
                        }
                        override fun onPlaybackStateChanged(state: Int) {
                            if (state == Player.STATE_ENDED && cont.isActive) cont.resume(Unit)
                        }
                        override fun onPlayerError(error: PlaybackException) {
                            if (cont.isActive) cont.resume(Unit)
                        }
                    }
                    exo.addListener(listener)
                    cont.invokeOnCancellation { exo.removeListener(listener) }
                }
            }
        } finally {
            stopAtLimit?.let(videoView::removeCallbacks)
            if (sphereSurface != null) {
                exo.stop()
                exo.clearVideoSurface(sphereSurface)
                sphere.stopVideo()
            } else {
                videoView.animate().alpha(0f).setDuration(FADE_MS / 2).withEndAction {
                    videoView.visibility = View.GONE
                }.start()
                exo.stop()
            }
            exo.clearMediaItems()
        }
    }

    internal fun showMessage(title: String, body: String?, busy: Boolean) {
        messageTitle.text = title
        messageBody.text = body
        messageBody.visibility = if (body.isNullOrBlank()) View.GONE else View.VISIBLE
        messageSpinner.visibility = if (busy) View.VISIBLE else View.GONE
        message.bringToFront()
        if (message.visibility != View.VISIBLE) {
            message.visibility = View.VISIBLE
            message.animate().alpha(1f).setDuration(400).start()
        }
        // Keep the last photo visible behind the message, dimmed.
        layers.forEach { if (it.alpha > 0f) it.animate().alpha(0.25f).setDuration(400).start() }
    }

    internal fun hideMessage() {
        if (message.visibility != View.VISIBLE) return
        message.animate().alpha(0f).setDuration(300).withEndAction { message.visibility = View.GONE }.start()
    }

    internal fun showNotice(text: String, durationMs: Long = 8_000) {
        notice.text = text
        notice.bringToFront()
        notice.animate().cancel()
        notice.animate().setStartDelay(0).alpha(1f).setDuration(400).withEndAction {
            notice.animate().alpha(0f).setStartDelay(durationMs).setDuration(600).start()
        }.start()
    }

    private fun text(sizeSp: Float, typeface: Typeface?, alpha: Float) = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setTextColor(Color.WHITE)
        this.alpha = alpha
        gravity = Gravity.CENTER
        typeface?.let { this.typeface = it }
    }

    private fun spaced(top: Int) = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { topMargin = top }

    private fun match() = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val FADE_MS = 1_200L
        const val KEN_BURNS_SCALE = 1.12f
    }
}

