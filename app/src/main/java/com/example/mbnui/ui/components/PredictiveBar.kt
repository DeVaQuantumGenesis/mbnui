package com.example.mbnui.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.mbnui.data.AppInfo

@Composable
fun PredictiveBar(apps: List<AppInfo>, onClick: (AppInfo) -> Unit) {
    if (apps.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val scope = rememberCoroutineScope()
        apps.forEach { app ->
            val scale = remember { Animatable(1f) }
            Surface(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(8.dp, CircleShape)
                    .scale(scale.value)
                    .clickable {
                        // perform press animation in coroutine then trigger click
                        scope.launch {
                            scale.animateTo(0.9f, animationSpec = tween(120))
                            scale.animateTo(1.06f, animationSpec = tween(220, easing = EaseOutBack))
                            scale.animateTo(1f, animationSpec = tween(160))
                        }
                        onClick(app)
                    },
                shape = CircleShape,
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.02f)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(bitmap = app.icon, contentDescription = app.name, modifier = Modifier.size(44.dp))
                }
            }
        }
    }
}

private val EaseOutBack = CubicBezierEasing(0.175f, 0.885f, 0.32f, 1.275f)
