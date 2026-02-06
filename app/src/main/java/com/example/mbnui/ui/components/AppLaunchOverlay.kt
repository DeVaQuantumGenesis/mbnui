package com.example.mbnui.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.mbnui.data.AppInfo

@Composable
fun AppLaunchOverlay(
    app: AppInfo,
    sourceRect: Rect,
    onAnimationEnd: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 700,
                easing = { t ->
                    // custom ease: slow start, quick middle, gentle overshoot
                    val eased = FastOutSlowInEasing.transform(t)
                    when {
                        t < 0.85f -> eased
                        else -> eased + (t - 0.85f) * 0.6f
                    }
                }
            )
        )
        onAnimationEnd()
    }

    val progress = animProgress.value
    
    // Interpolate rect
    val left = lerp(sourceRect.left / density.density, 0f, smoothStep(progress))
    val top = lerp(sourceRect.top / density.density, 0f, smoothStep(progress))
    val width = lerp(sourceRect.width / density.density, screenWidth.value, smoothStep(progress))
    val height = lerp(sourceRect.height / density.density, screenHeight.value, smoothStep(progress))
    val cornerRadius = lerp(24f, 20f, (progress * 1.2f).coerceAtMost(1f))
    val finalCornerRadius = lerp(cornerRadius, 0f, ((progress - 0.75f) / 0.25f).coerceIn(0f, 1f))
    
    val alpha = lerp(0.2f, 1f, progress)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = progress * 0.3f))
    ) {
        Box(
            modifier = Modifier
                .offset(x = left.dp, y = top.dp)
                .size(width.dp, height.dp)
                .clip(RoundedCornerShape(finalCornerRadius.dp))
                .background(
                    Brush.verticalGradient(listOf(Color.White, Color(0xFFF7F7F8))),
                )
        ) {
            // subtle parallax icon
            val iconOffset = lerp(0f, -screenWidth.value * 0.08f, progress)
            Image(
                bitmap = app.icon,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(lerp(8f, screenWidth.value / 4, progress).dp)
                    .offset(x = iconOffset.dp)
                    .alpha(1f - progress * 0.6f),
                contentScale = ContentScale.Fit
            )

            // App content mockup with shimmer-like gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = progress))
            ) {
                // decorative gloss
                Box(modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.08f), Color.Transparent)),
                    )
                )
            }
        }
    }
}

private fun lerp(start: Float, end: Float, fraction: Float): Float {
    return start + fraction * (end - start)
}

private fun smoothStep(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return x * x * (3 - 2 * x)
}
