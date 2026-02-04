package com.example.mbnui.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
                durationMillis = 500,
                easing = FastOutSlowInEasing
            )
        )
        onAnimationEnd()
    }

    val progress = animProgress.value
    
    // Interpolate rect
    val left = lerp(sourceRect.left / density.density, 0f, progress)
    val top = lerp(sourceRect.top / density.density, 0f, progress)
    val width = lerp(sourceRect.width / density.density, screenWidth.value, progress)
    val height = lerp(sourceRect.height / density.density, screenHeight.value, progress)
    val cornerRadius = lerp(24f, 28f, progress.coerceAtMost(0.5f)) // Keep some radius until nearly full
    val finalCornerRadius = lerp(cornerRadius, 0f, (progress - 0.8f).coerceAtLeast(0f) * 5f)
    
    val alpha = if (progress < 0.1f) progress * 10f else 1f

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
                .background(Color.White)
                .alpha(alpha)
        ) {
            Image(
                bitmap = app.icon,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(lerp(8f, screenWidth.value / 4, progress).dp) // Icon remains centered and scales
                    .alpha(1f - progress),
                contentScale = ContentScale.Fit
            )
            
            // App content mockup
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = progress))
            )
        }
    }
}

private fun lerp(start: Float, end: Float, fraction: Float): Float {
    return start + fraction * (end - start)
}
