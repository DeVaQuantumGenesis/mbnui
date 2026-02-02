package com.example.mbnui.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mbnui.data.AppInfo
import com.example.mbnui.ui.LauncherViewModel
import com.example.mbnui.ui.components.GlassBox
import com.example.mbnui.ui.components.GlassClock
import com.example.mbnui.ui.components.GlassSearchBar

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.*
import com.example.mbnui.data.HomeApp
import com.example.mbnui.data.HomeItem
import com.example.mbnui.data.HomeWidgetStack
import com.example.mbnui.data.WidgetType
import androidx.compose.material3.*
import androidx.compose.foundation.gestures.detectTapGestures
import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.zIndex
import com.example.mbnui.ui.components.AppItem
import com.example.mbnui.ui.components.DockItem

@Composable
fun HomeScreen(
    viewModel: LauncherViewModel = viewModel()
) {
    val apps by viewModel.filteredApps.collectAsState()
    val homeItems by viewModel.homeItems.collectAsState()
    val isCustomizing by viewModel.isCustomizing.collectAsState()
    val draggingItem by viewModel.draggingItem.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    val density = LocalDensity.current

    // Drawer state
    var drawerProgress by remember { mutableStateOf(1f) }
    val animatedProgress by animateFloatAsState(
        targetValue = drawerProgress,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "drawer_animation"
    )

    val drawerOffsetY = animatedProgress * screenHeight.value

    var showCustomizationSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.setCustomizing(true)
                        showCustomizationSheet = true
                    }
                )
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        val sensitivity = 1.5f
                        val delta = (dragAmount / density.density) / screenHeight.value
                        drawerProgress = (drawerProgress + delta * sensitivity).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        drawerProgress = if (drawerProgress < 0.5f) 0f else 1f
                        if (drawerProgress == 0f) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }
                )
            }
    ) {
        // Aesthetic Background Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A1A2E),
                            Color(0xFF16213E),
                            Color(0xFF0F3460)
                        )
                    )
                )
        )
        
        // Home Screen Content (Workspace)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .alpha(1f - (1f - animatedProgress) * 0.8f)
                .scale(1f - (1f - animatedProgress) * 0.05f)
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            GlassClock(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                try {
                                    val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val intent = Intent(Settings.ACTION_SETTINGS)
                                    context.startActivity(intent)
                                }
                            }
                        )
                    }
            )
            
            // Grid Workspace
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                val gridCols = 4
                val cellWidth = (screenWidth - 32.dp) / gridCols
                val cellHeight = 110.dp

                homeItems.forEach { item ->
                    Box(
                        modifier = Modifier
                            .offset(
                                x = cellWidth * item.x,
                                y = cellHeight * item.y
                            )
                            .width(if (item is HomeWidgetStack) cellWidth * 2 else cellWidth)
                            .height(if (item is HomeWidgetStack) cellHeight * 2 else cellHeight)
                    ) {
                        when (item) {
                            is HomeApp -> {
                                AppItem(item.appInfo) {
                                    viewModel.launchApp(item.appInfo)
                                }
                            }
                            is HomeWidgetStack -> {
                                WidgetStack(item)
                            }
                        }
                    }
                }
            }

            // Stationary Dock
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                GlassBox(
                    modifier = Modifier
                        .width(320.dp)
                        .height(96.dp),
                    cornerRadius = 28.dp,
                    isDark = true
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        apps.take(4).forEach { app ->
                            DockItem(app) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.launchApp(app)
                            }
                        }
                    }
                }
            }
        }

        // App Drawer
        AppDrawer(
            apps = apps,
            offsetY = drawerOffsetY,
            onClose = { drawerProgress = if (drawerProgress < 0.5f) 0f else 1f },
            onDrag = { dragAmount ->
                val delta = (dragAmount / density.density) / screenHeight.value
                drawerProgress = (drawerProgress + delta * 1.5f).coerceIn(0f, 1f)
            },
            onAppClick = { app ->
                if (isCustomizing) {
                    // In customizing mode, clicking in drawer adds to home
                    viewModel.onDragStart(app)
                    viewModel.onDragEnd(0, 0) // Simplified: add to first available or 0,0
                } else {
                    viewModel.launchApp(app)
                }
                drawerProgress = 1f
            },
            searchQuery = searchQuery,
            onSearchQueryChange = { viewModel.onSearchQueryChange(it) }
        )

        // Dragging Overlay (Simplified)
        draggingItem?.let {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.Center)
                    .zIndex(100f)
            ) {
                Image(bitmap = it.icon, contentDescription = null, modifier = Modifier.fillMaxSize())
            }
        }

        if (showCustomizationSheet) {
            CustomizationSheet(
                onDismiss = { showCustomizationSheet = false },
                onAddWidget = { type ->
                    viewModel.addWidget(type, 0, 0)
                    showCustomizationSheet = false
                }
            )
        }

        // Swipe Indicator
        if (drawerProgress > 0.9f && !showCustomizationSheet) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
fun WidgetStack(stack: HomeWidgetStack) {
    var currentIndex by remember { mutableIntStateOf(0) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, dragAmount ->
                    change.consume()
                    if (dragAmount > 20) {
                        currentIndex = (currentIndex + 1) % stack.widgets.size
                    } else if (dragAmount < -20) {
                        currentIndex = (currentIndex - 1 + stack.widgets.size) % stack.widgets.size
                    }
                }
            }
    ) {
        GlassBox(
            modifier = Modifier.fillMaxSize(),
            cornerRadius = 24.dp,
            isDark = true
        ) {
            val currentWidget = stack.widgets[currentIndex]
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = currentWidget.type.name,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Text(
                    text = "Widget",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizationSheet(
    onDismiss: () -> Unit,
    onAddWidget: (WidgetType) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A2E),
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                "Customize Home",
                fontSize = 20.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Add Widgets", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                WidgetType.values().forEach { type ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onAddWidget(type) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        GlassBox(
                            modifier = Modifier.aspectRatio(1f),
                            cornerRadius = 16.dp,
                            isDark = true
                        ) {
                            Text(type.name.take(1), fontSize = 24.sp, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(type.name.lowercase().capitalize(), fontSize = 12.sp, color = Color.White)
                    }
                }
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
