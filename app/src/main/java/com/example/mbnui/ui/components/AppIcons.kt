package com.example.mbnui.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import com.example.mbnui.data.AppInfo
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.geometry.Rect

@Composable
fun AppItem(
    app: AppInfo,
    onClick: (Rect) -> Unit
) {
    var itemRect by remember { mutableStateOf(Rect.Zero) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .onGloballyPositioned { itemRect = it.boundsInWindow() }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onClick(itemRect) }
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Expressive Squircle Icon
        Box(
            modifier = Modifier
                .size(64.dp)
                .padding(4.dp)
                .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(24.dp))
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
        ) {
             Image(
                bitmap = app.icon,
                contentDescription = app.name,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = app.name,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            color = Color.White,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
fun DockItem(
    app: AppInfo,
    onClick: (Rect) -> Unit
) {
    var itemRect by remember { mutableStateOf(Rect.Zero) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        label = "dock_scale"
    )

    Box(
        modifier = Modifier
            .size(56.dp)
            .scale(scale)
            .onGloballyPositioned { itemRect = it.boundsInWindow() }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onClick(itemRect) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = app.icon,
            contentDescription = app.name,
            modifier = Modifier.fillMaxSize().padding(4.dp)
        )
    }
}
