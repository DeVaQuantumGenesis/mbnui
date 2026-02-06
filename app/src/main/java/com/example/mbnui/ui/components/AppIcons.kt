package com.example.mbnui.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.geometry.Rect
import com.example.mbnui.data.AppInfo

@Composable
fun AppItem(
    app: AppInfo,
    badgeCount: Int = 0,
    onClick: (Rect) -> Unit
) {
    // badgeCount shows active notifications for the app
    fun Int.coerceBadgeText(): String = if (this <= 0) "" else if (this > 99) "99+" else this.toString()
    
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
             Box(modifier = Modifier.fillMaxSize()) {
                 Image(
                    bitmap = app.icon,
                    contentDescription = app.name,
                    modifier = Modifier.fillMaxSize()
                )
                if (badgeCount > 0) {
                    Box(modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .background(Color(0xFFE53935), CircleShape), contentAlignment = Alignment.Center) {
                        Text(text = badgeCount.coerceBadgeText(), color = Color.White, fontSize = 10.sp)
                    }
                }
            }
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
    badgeCount: Int = 0,
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
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                bitmap = app.icon,
                contentDescription = app.name,
                modifier = Modifier.fillMaxSize().padding(4.dp)
            )
            if (badgeCount > 0) {
                Box(modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .background(Color(0xFFE53935), CircleShape), contentAlignment = Alignment.Center) {
                    Text(text = badgeCount.coerceBadgeText(), color = Color.White, fontSize = 9.sp)
                }
            }
        }
    }
}
