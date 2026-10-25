package com.roylet.immichcast.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import com.roylet.immichcast.ui.settings.SettingsScreen
import com.roylet.immichcast.ui.setup.SetupWizard
import com.roylet.immichcast.ui.theme.ImmichCastTheme

private enum class Screen { SETUP, SETTINGS, ALBUMS }

/**
 * Launcher entry and the screensaver's settings activity.
 *
 * Supports one-shot configuration over adb so nobody has to type an API key with a remote:
 * `adb shell am start -n <package>/com.roylet.immichcast.ui.SettingsActivity --es url http://host:2283 --es apiKey KEY`
 */
class SettingsActivity : ComponentActivity() {

    private val vm: SettingsViewModel by viewModels { SettingsViewModel.Factory }

    /** Set when adb-provided credentials connect, so the wizard is skipped. */
    private var configuredExternally by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyIntentExtras(intent)
        setContent {
            ImmichCastTheme {
                val config by vm.config.collectAsStateWithLifecycle()
                var screen by rememberSaveable { mutableStateOf(if (config.isConfigured) Screen.SETTINGS else Screen.SETUP) }
                if (!config.isConfigured && screen != Screen.SETUP) screen = Screen.SETUP
                if (configuredExternally) {
                    screen = Screen.SETTINGS
                    configuredExternally = false
                }

                BackHandler(enabled = screen == Screen.ALBUMS) { screen = Screen.SETTINGS }

                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    Crossfade(targetState = screen, label = "screen") { current ->
                        when (current) {
                            Screen.SETUP -> SetupWizard(
                                vm,
                                onFinished = { screen = Screen.SETTINGS },
                                onCancel = if (config.isConfigured) ({ screen = Screen.SETTINGS }) else null,
                            )
                            Screen.SETTINGS -> SettingsScreen(
                                vm,
                                onOpenAlbums = { screen = Screen.ALBUMS },
                                onRunSetup = { screen = Screen.SETUP },
                            )
                            Screen.ALBUMS -> AlbumPickerScreen(vm)
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        applyIntentExtras(intent)
    }

    private fun applyIntentExtras(intent: Intent?) {
        val url = intent?.getStringExtra(EXTRA_URL)?.takeIf { it.isNotBlank() } ?: return
        val key = intent.getStringExtra(EXTRA_API_KEY)?.takeIf { it.isNotBlank() } ?: return
        vm.connect(url, key) { configuredExternally = true }
    }

    companion object {
        const val EXTRA_URL = "url"
        const val EXTRA_API_KEY = "apiKey"
    }
}
