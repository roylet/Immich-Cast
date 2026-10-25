package com.roylet.immichcast.ui.setup

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.roylet.immichcast.R
import com.roylet.immichcast.ui.AlbumPicker
import com.roylet.immichcast.ui.ConnectionState
import com.roylet.immichcast.ui.PreviewActivity
import com.roylet.immichcast.ui.SettingsViewModel
import com.roylet.immichcast.ui.components.CodeBlock
import com.roylet.immichcast.ui.components.Spinner
import com.roylet.immichcast.ui.components.TvTextField
import com.roylet.immichcast.ui.settings.screensaverCommand
import com.roylet.immichcast.ui.theme.BrutalBadge
import com.roylet.immichcast.ui.theme.BrutalButton
import com.roylet.immichcast.ui.theme.BrutalCard
import com.roylet.immichcast.ui.theme.BrutalStyle
import com.roylet.immichcast.ui.theme.Palette
import com.roylet.immichcast.ui.theme.dotGrid
import com.roylet.immichcast.ui.theme.hardShadow

private enum class Step { WELCOME, SERVER, KEY, CONNECT, ALBUMS, DONE }

@Composable
fun SetupWizard(vm: SettingsViewModel, onFinished: () -> Unit, onCancel: (() -> Unit)? = null) {
    val config by vm.config.collectAsStateWithLifecycle()
    var step by rememberSaveable { mutableStateOf(Step.WELCOME) }
    var url by rememberSaveable { mutableStateOf(config.serverUrl) }
    var key by rememberSaveable { mutableStateOf(config.apiKey) }

    BackHandler(enabled = step != Step.WELCOME || onCancel != null) {
        if (step == Step.WELCOME) {
            onCancel?.invoke()
            return@BackHandler
        }
        step = when (step) {
            Step.SERVER -> Step.WELCOME
            Step.KEY -> Step.SERVER
            Step.CONNECT -> Step.KEY
            Step.ALBUMS -> Step.KEY
            Step.DONE -> Step.ALBUMS
            Step.WELCOME -> Step.WELCOME
        }
    }

    Row(Modifier.fillMaxSize().background(Palette.Background).dotGrid()) {
        // Hero panel in ImmichUp orange
        Column(
            Modifier
                .width(380.dp)
                .fillMaxHeight()
                .background(Palette.Primary)
                .dotGrid(Palette.OnPrimary.copy(alpha = 0.12f))
                .padding(48.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Image(painterResource(R.drawable.ic_logo_ink), contentDescription = null, modifier = Modifier.size(88.dp))
            Spacer(Modifier.height(28.dp))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displayMedium, color = Palette.OnPrimary)
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.setup_tagline), style = MaterialTheme.typography.titleSmall,
                color = Palette.OnPrimary.copy(alpha = 0.8f))
            Spacer(Modifier.height(40.dp))
            StepIndicator(step.ordinal, Step.entries.size)
        }
        Box(Modifier.width(2.dp).fillMaxHeight().background(Palette.OnPrimary))

        Box(Modifier.weight(1f).fillMaxHeight().padding(horizontal = 64.dp, vertical = 48.dp), contentAlignment = Alignment.CenterStart) {
            AnimatedContent(targetState = step, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "step") { current ->
                when (current) {
                    Step.WELCOME -> Welcome { step = Step.SERVER }
                    Step.SERVER -> TextStep(
                        title = stringResource(R.string.setup_server_title),
                        body = stringResource(R.string.setup_server_hint),
                        number = 1,
                        initial = url,
                        placeholder = stringResource(R.string.setup_server_placeholder),
                        keyboardType = KeyboardType.Uri,
                        onNext = { url = it; step = Step.KEY },
                    )
                    Step.KEY -> TextStep(
                        title = stringResource(R.string.setup_key_title),
                        body = stringResource(R.string.setup_key_body),
                        number = 2,
                        initial = key,
                        placeholder = stringResource(R.string.setup_key_placeholder),
                        keyboardType = KeyboardType.Password,
                        onNext = {
                            key = it
                            step = Step.CONNECT
                            vm.connect(url, key) { step = Step.ALBUMS }
                        },
                    )
                    Step.CONNECT -> ConnectStep(vm, onRetry = { vm.connect(url, key) { step = Step.ALBUMS } }, onBack = {
                        vm.resetConnectionState()
                        step = Step.SERVER
                    })
                    Step.ALBUMS -> AlbumPicker(
                        vm,
                        header = {
                            StepHeader(stringResource(R.string.setup_albums_title), stringResource(R.string.setup_albums_body), number = 3)
                        },
                        footer = {
                            val count = config.excludedAlbumIds.size
                            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                BrutalButton(
                                    stringResource(if (count == 0) R.string.action_skip else R.string.action_continue),
                                    onClick = { step = Step.DONE },
                                )
                                Spacer(Modifier.width(20.dp))
                                BrutalBadge(
                                    if (count == 0) stringResource(R.string.albums_none_excluded)
                                    else pluralStringResource(R.plurals.albums_excluded_count, count, count),
                                    if (count == 0) Palette.Ink else Palette.Destructive,
                                )
                            }
                        },
                    )
                    Step.DONE -> Done(onFinished)
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(current: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(total) { i ->
            Box(
                Modifier
                    .size(if (i == current) 32.dp else 14.dp, 14.dp)
                    .background(if (i <= current) Palette.OnPrimary else Color.Transparent, RoundedCornerShape(2.dp))
                    .border(2.dp, Palette.OnPrimary, RoundedCornerShape(2.dp)),
            )
        }
    }
}

/** Title with ImmichUp's numbered square, plus a description. */
@Composable
private fun StepHeader(title: String, body: String, number: Int? = null) {
    Column(Modifier.padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            number?.let {
                Box(
                    Modifier.size(34.dp).hardShadow(3.dp).background(Palette.Primary, RoundedCornerShape(3.dp))
                        .border(2.dp, Palette.Ink, RoundedCornerShape(3.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(it.toString(), style = MaterialTheme.typography.titleMedium, color = Palette.OnPrimary)
                }
                Spacer(Modifier.width(16.dp))
            }
            Text(title, style = MaterialTheme.typography.headlineMedium)
        }
        Text(body, style = MaterialTheme.typography.bodyLarge, color = Palette.TextMuted, modifier = Modifier.widthIn(max = 640.dp))
    }
}

@Composable
private fun Welcome(onStart: () -> Unit) {
    val focus = remember { FocusRequester() }
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        StepHeader(stringResource(R.string.setup_welcome_title), stringResource(R.string.setup_welcome_body))
        Bullet(stringResource(R.string.setup_welcome_point_seasons))
        Bullet(stringResource(R.string.setup_welcome_point_exclude))
        Bullet(stringResource(R.string.setup_welcome_point_private))
        Spacer(Modifier.height(10.dp))
        BrutalButton(stringResource(R.string.action_get_started), onClick = onStart, modifier = Modifier.focusRequester(focus))
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}

@Composable
private fun Bullet(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(24.dp).background(Palette.Primary, RoundedCornerShape(3.dp)).border(2.dp, Palette.Ink, RoundedCornerShape(3.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = Palette.OnPrimary, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(text, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun TextStep(
    title: String,
    body: String,
    number: Int,
    initial: String,
    placeholder: String,
    keyboardType: KeyboardType,
    onNext: (String) -> Unit,
) {
    var value by remember { mutableStateOf(TextFieldValue(initial, TextRange(initial.length))) }
    val focus = remember { FocusRequester() }
    val submit = { if (value.text.isNotBlank()) onNext(value.text.trim()) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        StepHeader(title, body, number)
        TvTextField(value, { value = it }, placeholder, keyboardType, focus, Modifier.width(580.dp)) { submit() }
        BrutalButton(stringResource(R.string.action_continue), onClick = submit, enabled = value.text.isNotBlank())
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}

@Composable
private fun ConnectStep(vm: SettingsViewModel, onRetry: () -> Unit, onBack: () -> Unit) {
    val connection by vm.connection.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        when (val c = connection) {
            ConnectionState.Idle, ConnectionState.Testing, is ConnectionState.Connected -> {
                StepHeader(stringResource(R.string.setup_connecting_title), stringResource(R.string.setup_connecting_body))
                Spinner(40.dp)
            }
            is ConnectionState.Failed -> {
                BrutalCard(Modifier.widthIn(max = 640.dp), borderColor = Palette.Destructive) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = Palette.Destructive, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(R.string.setup_failed_title), style = MaterialTheme.typography.titleLarge, color = Palette.Destructive)
                            Text(c.message, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    BrutalButton(stringResource(R.string.action_retry), onClick = onRetry, modifier = Modifier.focusRequester(focus))
                    BrutalButton(stringResource(R.string.action_edit_details), onClick = onBack, style = BrutalStyle.OUTLINE)
                }
                LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
            }
        }
    }
}

@Composable
private fun Done(onFinished: () -> Unit) {
    val context = LocalContext.current
    val focus = remember { FocusRequester() }
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        StepHeader(stringResource(R.string.setup_done_title), stringResource(R.string.setup_done_body), number = 4)
        BrutalCard(Modifier.width(660.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.screensaver_how_title).uppercase(), style = MaterialTheme.typography.labelLarge)
                Text(stringResource(R.string.screensaver_how_body), style = MaterialTheme.typography.bodyMedium, color = Palette.TextMuted)
                CodeBlock(screensaverCommand(context.packageName))
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            BrutalButton(
                stringResource(R.string.action_preview),
                onClick = {
                    onFinished()
                    context.startActivity(Intent(context, PreviewActivity::class.java))
                },
                modifier = Modifier.focusRequester(focus),
            )
            BrutalButton(stringResource(R.string.action_open_settings), onClick = onFinished, style = BrutalStyle.OUTLINE)
        }
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}
