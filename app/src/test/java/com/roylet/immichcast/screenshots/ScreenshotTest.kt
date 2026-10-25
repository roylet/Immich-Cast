package com.roylet.immichcast.screenshots

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.widget.FrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.roylet.immichcast.R
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import com.android.resources.UiMode
import com.roylet.immichcast.api.ImmichApi
import com.roylet.immichcast.core.ImageFit
import com.roylet.immichcast.core.OverlayItem
import com.roylet.immichcast.core.SlideshowConfig
import com.roylet.immichcast.data.AppSettings
import com.roylet.immichcast.data.PlaylistRepository
import com.roylet.immichcast.dream.OverlayView
import com.roylet.immichcast.dream.SlideCaption
import com.roylet.immichcast.dream.SlideLayer
import com.roylet.immichcast.ui.AlbumPickerScreen
import com.roylet.immichcast.ui.AlbumsState
import com.roylet.immichcast.ui.SettingsViewModel
import com.roylet.immichcast.ui.settings.Category
import com.roylet.immichcast.ui.settings.SettingsScreen
import com.roylet.immichcast.ui.setup.SetupWizard
import com.roylet.immichcast.ui.theme.ImmichCastTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate

/**
 * Renders the TV UI at 1080p so it can be reviewed without a device.
 * `./gradlew recordPaparazziDebug` writes the images to app/src/test/snapshots.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig(
            screenWidth = 1920,
            screenHeight = 1080,
            xdpi = 320,
            ydpi = 320,
            density = Density.XHIGH,
            orientation = ScreenOrientation.LANDSCAPE,
            uiMode = UiMode.TELEVISION,
        ),
        theme = "android:Theme.Material.NoActionBar",
        maxPercentDifference = 1.0,
    )

    private val server = MockWebServer()

    private class TestApp(base: Context) : Application() {
        init { attachBaseContext(base) }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        server.dispatcher = FakeImmich
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
        Dispatchers.resetMain()
    }

    private fun viewModel(configured: Boolean = true, edit: (SlideshowConfig) -> SlideshowConfig = { it }): SettingsViewModel {
        val app = TestApp(paparazzi.context)
        val settings = AppSettings(app).apply { reset() }
        if (configured) {
            settings.update {
                edit(it.copy(serverUrl = server.url("/").toString().trimEnd('/'), apiKey = "demo", excludedAlbumIds = setOf("a2", "a5")))
            }
        }
        val apiFor = { c: SlideshowConfig -> ImmichApi(c.serverUrl, OkHttpClient(), c.apiKey) }
        val repo = PlaylistRepository(
            File.createTempFile("playlist", ".json"), apiFor,
            today = { LocalDate.of(2026, 10, 25) },
            log = { _, _ -> },
        )
        if (configured) runBlocking { repo.refresh(settings.current) }
        return SettingsViewModel(app, settings, repo, OkHttpClient(), apiFor)
    }

    @Test
    fun settingsPhotos() {
        val vm = viewModel()
        paparazzi.snapshot { ImmichCastTheme { SettingsScreen(vm, {}, {}) } }
    }

    @Test
    fun settingsDisplay() {
        val vm = viewModel()
        paparazzi.snapshot { ImmichCastTheme { SettingsScreen(vm, {}, {}, initialCategory = Category.DISPLAY) } }
    }

    @Test
    fun settingsOverlay() {
        val vm = viewModel { it.copy(overlayItems = setOf(OverlayItem.CLOCK, OverlayItem.DATE)) }
        paparazzi.snapshot { ImmichCastTheme { SettingsScreen(vm, {}, {}, initialCategory = Category.OVERLAY) } }
    }

    @Test
    fun albumPicker() {
        val vm = viewModel()
        vm.loadAlbums()
        val deadline = System.currentTimeMillis() + 5_000
        while (vm.albums.value !is AlbumsState.Loaded && System.currentTimeMillis() < deadline) Thread.sleep(20)
        paparazzi.snapshot { ImmichCastTheme { AlbumPickerScreen(vm) } }
    }

    @Test
    fun setupWelcome() {
        val vm = viewModel(configured = false)
        paparazzi.snapshot { ImmichCastTheme { SetupWizard(vm, onFinished = {}) } }
    }

    @Test
    fun screensaverWithOverlay() {
        val context = paparazzi.context
        val photo = BitmapDrawable(context.resources, samplePhoto())
        val root = FrameLayout(context)
        root.addView(SlideLayer(context).apply { bind(photo, ImageFit.FIT_BLURRED); alpha = 1f })
        root.addView(OverlayView(context).apply {
            configure(SlideshowConfig(overlayItems = setOf(OverlayItem.CLOCK, OverlayItem.DATE, OverlayItem.LOCATION, OverlayItem.SEASON)))
            setCaption(SlideCaption(date = "5 October 2022", location = "Hobart, Australia", season = "Spring 2022"), animate = false)
        })
        paparazzi.snapshot(root)
    }

    /** The home-screen banner in a mock Google TV apps row, plus the launcher icon under common masks. */
    @Test
    fun appArt() {
        paparazzi.snapshot {
            ImmichCastTheme {
                Column(
                    Modifier.fillMaxSize().background(Color(0xFF101318)).padding(48.dp),
                    verticalArrangement = Arrangement.spacedBy(32.dp),
                ) {
                    Text("Your apps", style = MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(196.dp, 110.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF2B3140)))
                        Image(
                            painterResource(R.drawable.banner), contentDescription = null,
                            modifier = Modifier.size(216.dp, 121.5.dp).border(3.dp, Color.White, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp)),
                        )
                        Image(
                            painterResource(R.drawable.banner), contentDescription = null,
                            modifier = Modifier.size(196.dp, 110.dp).clip(RoundedCornerShape(10.dp)),
                        )
                        Box(Modifier.size(196.dp, 110.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF2B3140)))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                        // Adaptive icons show the middle 72 of 108 units, so draw the layers at 1.5x and mask.
                        listOf(CircleShape, RoundedCornerShape(28.dp), RoundedCornerShape(12.dp)).forEach { shape ->
                            Box(Modifier.size(96.dp).clip(shape), contentAlignment = Alignment.Center) {
                                Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.requiredSize(144.dp))
                                Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.requiredSize(144.dp))
                            }
                        }
                        Box(Modifier.size(96.dp).clip(CircleShape).background(Color(0xFF3A4256)), contentAlignment = Alignment.Center) {
                            Image(painterResource(R.drawable.ic_launcher_monochrome), null, Modifier.requiredSize(144.dp))
                        }
                    }
                }
            }
        }
    }

    /** A 4:3 landscape "photo" so the blurred letterbox is visible on a 16:9 screen. */
    private fun samplePhoto(): Bitmap {
        val w = 1440; val h = 1080
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(0f, 0f, 0f, h * 0.7f, intArrayOf(0xFF3B6FB6.toInt(), 0xFFF4B98A.toInt()), null, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        p.shader = null
        p.color = 0xFFFFE1A8.toInt(); c.drawCircle(w * 0.72f, h * 0.38f, 70f, p)
        p.color = 0xFF2F4858.toInt()
        c.drawPath(Path().apply { moveTo(0f, h * 0.72f); lineTo(w * 0.3f, h * 0.45f); lineTo(w * 0.55f, h * 0.68f); lineTo(w * 0.75f, h * 0.52f); lineTo(w.toFloat(), h * 0.7f); lineTo(w.toFloat(), h.toFloat()); lineTo(0f, h.toFloat()); close() }, p)
        p.color = 0xFF5E8C61.toInt()
        c.drawPath(Path().apply { moveTo(0f, h * 0.85f); quadTo(w * 0.5f, h * 0.72f, w.toFloat(), h * 0.86f); lineTo(w.toFloat(), h.toFloat()); lineTo(0f, h.toFloat()); close() }, p)
        return bmp
    }

    private object FakeImmich : Dispatcher() {
        private val albums = listOf(
            "a1" to "Family holidays" to 412, "a2" to "Receipts" to 88, "a3" to "Garden" to 156,
            "a4" to "Tasmania 2022" to 640, "a5" to "Screenshots" to 1203, "a6" to "Wedding" to 952,
            "a7" to "Pets" to 318, "a8" to "Beach days" to 204,
        )

        override fun dispatch(request: RecordedRequest): MockResponse {
            val path = request.path.orEmpty()
            return when {
                path.startsWith("/api/albums") -> MockResponse().setBody(
                    albums.joinToString(",", "[", "]") { (idName, count) ->
                        """{"id":"${idName.first}","albumName":"${idName.second}","assetCount":$count}"""
                    },
                )
                path.startsWith("/api/timeline/buckets") -> MockResponse().setBody(
                    (2016..2025).joinToString(",", "[", "]") { """{"timeBucket":"$it-10-01","count":10}""" },
                )
                path == "/api/search/metadata" -> {
                    val body = request.body.readUtf8()
                    val items = if ("albumIds" in body) {
                        (1..37).joinToString(",") { """{"id":"x$it"}""" }
                    } else {
                        val year = Regex("\"takenAfter\":\"(\\d{4})").find(body)?.groupValues?.get(1) ?: "2020"
                        (1..128).joinToString(",") { """{"id":"$year-$it","localDateTime":"$year-10-${(it % 28 + 1).toString().padStart(2, '0')}T10:00:00"}""" }
                    }
                    MockResponse().setBody("""{"assets":{"items":[$items],"nextPage":null}}""")
                }
                else -> MockResponse().setResponseCode(404)
            }
        }
    }
}
