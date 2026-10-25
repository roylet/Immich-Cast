package com.roylet.immichcast.dream

import android.service.dreams.DreamService

/** The system screensaver entry point. */
class PhotoDreamService : DreamService() {

    private var slideshow: SlideshowView? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        isScreenBright = true
        slideshow = SlideshowView(this).also { setContentView(it) }
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        slideshow?.start()
    }

    override fun onDreamingStopped() {
        slideshow?.stop()
        super.onDreamingStopped()
    }

    override fun onDetachedFromWindow() {
        slideshow?.stop()
        slideshow = null
        super.onDetachedFromWindow()
    }
}
