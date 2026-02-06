package com.example.mbnui.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.mbnui.ui.theme.GlassBlack
import com.example.mbnui.ui.theme.GlassBlackBorder
import com.example.mbnui.ui.theme.GlassWhite
import com.example.mbnui.ui.theme.GlassWhiteBorder

@Composable
fun GlassBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    blurRadius: Dp = 12.dp,
    isDark: Boolean = false,
    content: @Composable () -> Unit
) {
    val backgroundColor = if (isDark) GlassBlack.copy(alpha = 0.3f) else GlassWhite.copy(alpha = 0.15f)
    val borderColor = if (isDark) GlassBlackBorder.copy(alpha = 0.4f) else GlassWhiteBorder.copy(alpha = 0.5f)

    Box(modifier = modifier) {
        // Blurred background layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    if (blurRadius > 0.dp) {
                        renderEffect = BlurEffect(
                            blurRadius.toPx(),
                            blurRadius.toPx(),
                            TileMode.Clamp
                        )
                    }
                    clip = true
                    shape = RoundedCornerShape(cornerRadius)
                }
                .background(backgroundColor)
                .border(1.dp, borderColor, RoundedCornerShape(cornerRadius))
        )
        
        // Crisp content layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}
