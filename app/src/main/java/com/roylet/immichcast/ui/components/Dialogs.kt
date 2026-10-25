package com.roylet.immichcast.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.roylet.immichcast.R
import com.roylet.immichcast.ui.theme.BrutalBorder
import com.roylet.immichcast.ui.theme.BrutalButton
import com.roylet.immichcast.ui.theme.BrutalCard
import com.roylet.immichcast.ui.theme.BrutalShape
import com.roylet.immichcast.ui.theme.BrutalStyle
import com.roylet.immichcast.ui.theme.Palette
import com.roylet.immichcast.ui.theme.hardShadow

@Composable
private fun DialogFrame(title: String, subtitle: String?, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        BrutalCard(Modifier.width(540.dp), shadow = 6.dp) {
            Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(title, style = MaterialTheme.typography.headlineSmall)
                    subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Palette.TextMuted) }
                }
                content()
            }
        }
    }
}

@Composable
fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    subtitle: String? = null,
    description: (@Composable (T) -> String?)? = null,
) {
    val focus = remember { FocusRequester() }
    DialogFrame(title, subtitle, onDismiss) {
        Box(Modifier.border(BrutalBorder, Palette.Ink, BrutalShape)) {
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                itemsIndexed(options) { index, option ->
                    if (index > 0) RowDivider()
                    val isSelected = option == selected
                    CheckRow(
                        title = label(option),
                        description = description?.invoke(option),
                        checked = isSelected,
                        onClick = { onSelect(option); onDismiss() },
                        modifier = if (isSelected) Modifier.focusRequester(focus) else Modifier,
                    )
                }
            }
        }
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}

@Composable
fun TextInputDialog(
    title: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    subtitle: String? = null,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    var value by remember { mutableStateOf(TextFieldValue(initial, TextRange(initial.length))) }
    val focus = remember { FocusRequester() }
    DialogFrame(title, subtitle, onDismiss) {
        TvTextField(value, { value = it }, placeholder, keyboardType, focus) { onConfirm(value.text.trim()); onDismiss() }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BrutalButton(stringResource(R.string.action_save), onClick = { onConfirm(value.text.trim()); onDismiss() })
            BrutalButton(stringResource(R.string.action_cancel), onClick = onDismiss, style = BrutalStyle.OUTLINE)
        }
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}

/** ImmichUp's input: outlined with a small hard shadow; when focused it turns orange and presses into the shadow. */
@Composable
fun TvTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    placeholder: String?,
    keyboardType: KeyboardType,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    onDone: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val press = if (focused) 2.dp else 0.dp
    val textStyle = MaterialTheme.typography.titleMedium.copy(color = Palette.Text)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(Palette.Primary),
        visualTransformation = if (keyboardType == KeyboardType.Password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done, autoCorrectEnabled = false),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused },
        decorationBox = { inner ->
            Box(
                Modifier
                    .offset(press, press)
                    .hardShadow(3.dp - press)
                    .background(Palette.Card, BrutalShape)
                    .border(BrutalBorder, if (focused) Palette.Primary else Palette.Ink, BrutalShape)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                if (value.text.isEmpty() && placeholder != null) {
                    Text(placeholder, style = textStyle, color = Palette.TextMuted)
                }
                inner()
            }
        },
    )
}
