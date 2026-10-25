package com.roylet.immichcast.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.roylet.immichcast.ui.theme.BrutalBorder
import com.roylet.immichcast.ui.theme.BrutalShape
import com.roylet.immichcast.ui.theme.Palette

@Composable
fun CodeBlock(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .background(Color(0xFF0B0A09), BrutalShape)
            .border(BrutalBorder, Palette.Line, BrutalShape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(text, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, color = Palette.Warning)
    }
}

@Composable
fun Spinner(size: Dp = 18.dp, color: Color = Palette.Primary) {
    CircularProgressIndicator(modifier = Modifier.size(size), color = color, strokeWidth = 3.dp, strokeCap = StrokeCap.Square)
}
