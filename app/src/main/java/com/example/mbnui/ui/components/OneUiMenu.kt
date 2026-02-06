package com.example.mbnui.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Rect
import kotlin.math.roundToInt

@Composable
fun OneUiMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    anchorBounds: Rect? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (expanded) {
        val density = LocalDensity.current
        val popupOffset = if (anchorBounds != null) {
            val menuWidthPx = with(density) { 220.dp.toPx() }
            val x = (anchorBounds.left + (anchorBounds.width - menuWidthPx) / 2f).roundToInt()
            val y = anchorBounds.bottom.roundToInt()
            IntOffset(x, y)
        } else IntOffset(0, 0)

        Popup(
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = true),
            alignment = Alignment.TopStart,
            offset = popupOffset
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .clickable { onDismissRequest() }
            ) {
                Box(
                    modifier = modifier
                        .width(220.dp)
                        .padding(16.dp)
                ) {
                    GlassBox(
                        cornerRadius = 24.dp,
                        isDark = true,
                        modifier = Modifier
                            .animateContentSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            content()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OneUiMenuItem(
    text: String,
    icon: Int? = null,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    tint = if (isDestructive) Color(0xFFFF5252) else Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = text,
                color = if (isDestructive) Color(0xFFFF5252) else Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
