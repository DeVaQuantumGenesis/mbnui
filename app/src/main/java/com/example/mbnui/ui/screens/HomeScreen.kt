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
import androidx.compose.material3.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.viewinterop.AndroidView
import java.util.ArrayList
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.zIndex
import com.example.mbnui.ui.components.AppItem
import com.example.mbnui.ui.components.DockItem
import androidx.compose.ui.res.painterResource

@Composable
fun HomeScreen(
    viewModel: LauncherViewModel = viewModel(),
    appWidgetHost: android.appwidget.AppWidgetHost
) {
    val apps by viewModel.filteredApps.collectAsState()
    val homeItems by viewModel.homeItems.collectAsState()
    val isCustomizing by viewModel.isCustomizing.collectAsState()
    val draggingAppInfo by viewModel.draggingItem.collectAsState() // From Drawer
    val draggingHomeItemId by viewModel.draggingItemId.collectAsState() // From Home
    val dragOffset by viewModel.dragOffset.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    val density = LocalDensity.current

    // Drawer state
    var drawerProgress by remember { mutableFloatStateOf(1f) }
    val animatedProgress by animateFloatAsState(
        targetValue = drawerProgress,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "drawer_animation"
    )

    val drawerOffsetY = animatedProgress * screenHeight.value
    var showCustomizationSheet by remember { mutableStateOf(false) }

    // Widget Picking
    val widgetPickerLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val appWidgetId = result.data?.extras?.getInt(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID)
            if (appWidgetId != null) {
                 val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
                 val appWidgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
                 val label = appWidgetInfo?.loadLabel(context.packageManager) ?: "Widget"
                 val provider = appWidgetInfo?.provider?.className ?: "Unknown"
                viewModel.addWidget(appWidgetId, provider, label, 0, 0)
            }
        }
    }

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
                        val delta = (dragAmount / density.density) / screenHeight.value
                        drawerProgress = (drawerProgress + delta * 1.5f).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        drawerProgress = if (drawerProgress < 0.5f) 0f else 1f
                    }
                )
            }
    ) {
        // Workspace Content
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
                                    context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                                } catch (e: Exception) {
                                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                }
                            }
                        )
                    }
            )
            
            // Grid
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                val gridCols = 4
                val cellWidth = (screenWidth - 32.dp) / gridCols
                val cellHeight = 110.dp
                var activeItem by remember { mutableStateOf<HomeItem?>(null) }

                homeItems.forEach { item ->
                    val isBeingDragged = item.id == draggingHomeItemId
                    Box(
                        modifier = Modifier
                            .offset(x = cellWidth * item.x, y = cellHeight * item.y)
                            .width(if (item is HomeWidgetStack) cellWidth * item.sizeX.dp else cellWidth)
                            .height(if (item is HomeWidgetStack) cellHeight * item.sizeY.dp else cellHeight)
                            .alpha(if (isBeingDragged) 0f else 1f)
                            .pointerInput(item) {
                                var isDragging = false
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { offset ->
                                        isDragging = false
                                        viewModel.onDragStart(item, Pair(offset.x, offset.y))
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        isDragging = true
                                        val current = viewModel.dragOffset.value ?: Pair(0f, 0f)
                                        viewModel.onDrag(Pair(current.first + dragAmount.x, current.second + dragAmount.y))
                                    },
                                    onDragEnd = {
                                        if (isDragging) {
                                            val currentOffset = viewModel.dragOffset.value ?: Pair(0f, 0f)
                                            val startPxX = (cellWidth * item.x).toPx()
                                            val startPxY = (cellHeight * item.y).toPx()
                                            val dropPxX = startPxX + currentOffset.first
                                            val dropPxY = startPxY + currentOffset.second
                                            
                                            val targetX = (dropPxX / cellWidth.toPx()).toInt().coerceIn(0, 3)
                                            val targetY = (dropPxY / cellHeight.toPx()).toInt().coerceIn(0, 5)
                                            viewModel.onItemDrop(item.id, targetX, targetY)
                                        } else {
                                            activeItem = item
                                            viewModel.onHomeItemDragEnd(0, 0, 0f)
                                        }
                                    },
                                    onDragCancel = { viewModel.onHomeItemDragEnd(0, 0, 0f) }
                                )
                            }
                            .clickable(enabled = draggingHomeItemId == null) {
                                if (item is HomeApp) viewModel.launchApp(item.appInfo)
                            }
                    ) {
                        when (item) {
                            is HomeApp -> AppItem(app = item.appInfo, onClick = {})
                            is HomeWidgetStack -> WidgetStack(item, appWidgetHost)
                        }

                        // Context Menu
                        if (activeItem == item) {
                            DropdownMenu(
                                expanded = true,
                                onDismissRequest = { activeItem = null },
                                modifier = Modifier.background(Color(0xFF1A1A2E).copy(alpha = 0.95f), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Remove", color = Color.White) },
                                    leadingIcon = { Icon(painterResource(android.R.drawable.ic_menu_delete), null, tint = Color.White) },
                                    onClick = { viewModel.removeItem(item); activeItem = null }
                                )
                                if (item is HomeApp) {
                                    DropdownMenuItem(
                                        text = { Text("App Info", color = Color.White) },
                                        leadingIcon = { Icon(painterResource(android.R.drawable.ic_menu_info_details), null, tint = Color.White) },
                                        onClick = { viewModel.openAppInfo(item.appInfo); activeItem = null }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Uninstall", color = Color.Red.copy(alpha = 0.8f)) },
                                        leadingIcon = { Icon(painterResource(android.R.drawable.ic_notification_clear_all), null, tint = Color.Red) },
                                        onClick = { viewModel.uninstallApp(item.appInfo); activeItem = null }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Dock
            Box(
                modifier = Modifier.fillMaxWidth().padding(bottom = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                GlassBox(modifier = Modifier.width(320.dp).height(96.dp), cornerRadius = 28.dp, isDark = true) {
                    Row(modifier = Modifier.fillMaxSize(), Arrangement.SpaceEvenly, Alignment.CenterVertically) {
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
            onAppClick = { app -> viewModel.launchApp(app); drawerProgress = 1f },
            onAppDragStart = { app, offset ->
                 viewModel.onDragStart(HomeApp(app, 0, 0), Pair(offset.x, offset.y)) // Dummy HomeApp for reuse
                 drawerProgress = 1f 
            },
            onAppDrag = { offset -> viewModel.onDrag(Pair(offset.x, offset.y)) },
            onAppDragEnd = { viewModel.onDragEnd(0, 0) },
            searchQuery = searchQuery,
            onSearchQueryChange = { viewModel.onSearchQueryChange(it) }
        )

        // Unified Drag Overlay
        if (draggingHomeItemId != null || draggingAppInfo != null) {
            Box(modifier = Modifier.fillMaxSize().zIndex(100f)) {
                 dragOffset?.let { (x, y) ->
                     val item = if (draggingHomeItemId != null) homeItems.find { it.id == draggingHomeItemId } else null
                     Box(
                         modifier = Modifier
                            .offset(x = (x / density.density).dp, y = (y / density.density).dp)
                            .size(64.dp) 
                     ) {
                         if (draggingAppInfo != null) {
                             Image(bitmap = draggingAppInfo!!.icon, contentDescription = null, modifier = Modifier.fillMaxSize())
                         } else if (item is HomeApp) {
                             Image(bitmap = item.appInfo.icon, contentDescription = null, modifier = Modifier.fillMaxSize())
                         } else if (item is HomeWidgetStack) {
                             Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(0.5f), RoundedCornerShape(12.dp)))
                         }
                     }
                 }
            }
        }

        if (showCustomizationSheet) {
            CustomizationSheet(
                onDismiss = { showCustomizationSheet = false },
                onAddWidget = { 
                    val appWidgetId = appWidgetHost.allocateAppWidgetId()
                    val pickIntent = Intent(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
                        putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    }
                    widgetPickerLauncher.launch(pickIntent)
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
fun WidgetStack(
    stack: HomeWidgetStack,
    appWidgetHost: android.appwidget.AppWidgetHost
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    
    Box(
        modifier = Modifier.fillMaxSize().padding(8.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, dragAmount ->
                    change.consume()
                    if (dragAmount > 20) currentIndex = (currentIndex + 1) % stack.widgets.size
                    else if (dragAmount < -20) currentIndex = (currentIndex - 1 + stack.widgets.size) % stack.widgets.size
                }
            }
    ) {
        val currentWidget = stack.widgets.getOrNull(currentIndex)
        
        if (currentWidget != null) {
            val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
            val appWidgetInfo = appWidgetManager.getAppWidgetInfo(currentWidget.appWidgetId)
            
            if (appWidgetInfo != null) {
                AndroidView(
                    factory = { ctx ->
                        appWidgetHost.createView(ctx, currentWidget.appWidgetId, appWidgetInfo).apply { setPadding(0, 0, 0, 0) }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                 GlassBox(modifier = Modifier.fillMaxSize(), cornerRadius = 24.dp, isDark = true) {
                    Text("Widget Error", color = Color.White, modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizationSheet(
    onDismiss: () -> Unit,
    onAddWidget: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A2E),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Text("Customize Home", fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(24.dp))
            
            Box(
                modifier = Modifier.fillMaxWidth().height(64.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .clickable { onAddWidget() },
                contentAlignment = Alignment.Center
            ) {
                 Row(verticalAlignment = Alignment.CenterVertically) {
                     Icon(painterResource(android.R.drawable.ic_menu_add), null, tint = Color.White)
                     Spacer(modifier = Modifier.width(8.dp))
                     Text("Add System Widget", color = Color.White, fontSize = 16.sp)
                 }
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
