package com.example.mbnui.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mbnui.data.AppInfo
import com.example.mbnui.ui.components.AppItem
import com.example.mbnui.ui.components.GlassBox
import com.example.mbnui.ui.components.GlassSearchBar

@Composable
fun AppDrawer(
    apps: List<AppInfo>,
    offsetY: Float,
    onClose: () -> Unit,
    onDrag: (Float) -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onAppDragStart: (AppInfo, androidx.compose.ui.geometry.Offset) -> Unit,
    onAppDrag: (androidx.compose.ui.geometry.Offset) -> Unit,
    onAppDragEnd: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .offset(y = offsetY.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        onDrag(dragAmount)
                    },
                    onDragEnd = {
                        onClose()
                    }
                )
            }
    ) {
        GlassBox(
            modifier = Modifier.fillMaxSize(),
            cornerRadius = 32.dp,
            isDark = true,
            blurRadius = 32.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp)
            ) {
                // Drawer handle
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color.White.copy(alpha = 0.3f), androidx.compose.foundation.shape.CircleShape)
                        .align(Alignment.CenterHorizontally)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                GlassSearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    placeholder = "Search all apps..."
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        items = apps,
                        key = { it.key }
                    ) { app ->
                        var isDragging by remember { mutableStateOf(false) }
                        
                        Box(
                            modifier = Modifier.pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { offset ->
                                        isDragging = true
                                        onAppDragStart(app, offset)
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        onAppDrag(change.position)
                                    },
                                    onDragEnd = {
                                        isDragging = false
                                        onAppDragEnd()
                                    },
                                    onDragCancel = {
                                        isDragging = false
                                        onAppDragEnd()
                                    }
                                )
                            }
                        ) {
                            AppItem(app) {
                                onAppClick(app)
                            }
                        }
                    }
                }
            }
        }
    }
}
