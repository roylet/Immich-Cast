package com.roylet.immichcast.ui

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.roylet.immichcast.R
import com.roylet.immichcast.dream.SlideshowView

/** Runs the screensaver on demand. Like the real screensaver, any button exits. */
class PreviewActivity : ComponentActivity() {

    private lateinit var slideshow: SlideshowView
    private var startedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        slideshow = SlideshowView(this)
        setContentView(slideshow)
    }

    override fun onStart() {
        super.onStart()
        startedAt = System.currentTimeMillis()
        slideshow.start()
        slideshow.post { slideshow.showNoticeText(getString(R.string.preview_hint), 4_000) }
    }

    override fun onStop() {
        slideshow.stop()
        super.onStop()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // Ignore the key-repeat of the button that launched the preview.
        if (System.currentTimeMillis() - startedAt > 600) finish()
        return true
    }
}
