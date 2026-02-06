package com.example.mbnui.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.scale
 
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.mbnui.data.AppInfo
import kotlinx.coroutines.launch

@Composable
fun AnimatedDock(
    apps: List<AppInfo>,
    modifier: Modifier = Modifier,
    maxScale: Float = 0.42f,
    spreadDp: Float = 88f,
    onAppClick: (AppInfo, Rect?) -> Unit
) {
    val density = LocalDensity.current
    val spreadPx = with(density) { spreadDp.dp.toPx() }

    // Track current pointer X in window coordinates (px)
    var pointerX by remember { mutableStateOf<Float?>(null) }
    val boundsList = remember { mutableStateListOf<Rect>() }

    // Ensure boundsList size matches apps
    LaunchedEffect(apps.size) {
        boundsList.clear()
        repeat(apps.size) { boundsList.add(Rect.Zero) }
    }

    var rowLeft by remember { mutableStateOf(0f) }
    Row(
        modifier = modifier
            .padding(bottom = 8.dp)
            .onGloballyPositioned { coords -> rowLeft = coords.boundsInWindow().left }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val p = event.changes.firstOrNull()
                        if (p != null && p.pressed) {
                            // position is local to the Row; add rowLeft to get window x
                            pointerX = rowLeft + p.position.x
                        } else {
                            pointerX = null
                        }
                    }
                }
            }
            .background(Color.Transparent),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        apps.forEachIndexed { index, app ->
            var centerX by remember { mutableStateOf(0f) }
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .onGloballyPositioned { coords ->
                        val b = coords.boundsInWindow()
                        boundsList[index] = b
                        centerX = (b.left + b.right) / 2f
                    }
                    .pointerInput(app) {
                        detectTapGestures(onTap = {
                            // send bounds as Rect for launch overlay
                            val rect = boundsList.getOrNull(index)
                            onAppClick(app, rect)
                        })
                    },
                contentAlignment = Alignment.Center
            ) {
                val targetScale = remember(pointerX, centerX) {
                    derivedStateOf {
                        val px = pointerX
                        if (px == null) 1f else {
                            val d = kotlin.math.abs(px - centerX)
                            val influence = kotlin.math.max(0f, 1f - (d / spreadPx))
                            1f + (maxScale * influence)
                        }
                    }
                }

                val scale by animateFloatAsState(targetValue = targetScale.value)

                Box(modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                    .scale(scale)
                ) {
                    Image(bitmap = app.icon, contentDescription = app.name, modifier = Modifier.fillMaxSize().padding(6.dp))
                }
            }
        }
    }
}
