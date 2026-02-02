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
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress

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
    val widgetProviders by viewModel.widgetProviders.collectAsState()
    val gridCols by viewModel.gridCols.collectAsState()
    val gridRows by viewModel.gridRows.collectAsState()
    
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
    var showWidgetPicker by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    // Context Menu State
    var activeItem by remember { mutableStateOf<HomeItem?>(null) }
    
    // Widget Binding State
    var pendingWidgetId by remember { mutableStateOf<Int?>(null) }
    var pendingProviderInfo by remember { mutableStateOf<android.appwidget.AppWidgetProviderInfo?>(null) }

    val bindWidgetLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val id = pendingWidgetId
            val info = pendingProviderInfo
            if (id != null && info != null) {
                viewModel.addWidget(id, info.provider.className, info.loadLabel(context.packageManager), 0, 0)
            }
        }
        pendingWidgetId = null
        pendingProviderInfo = null
    }

    // Helper to add widget
    fun requestAddWidget(provider: android.appwidget.AppWidgetProviderInfo) {
        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
        val id = appWidgetHost.allocateAppWidgetId()
        
        val allowed = appWidgetManager.bindAppWidgetIdIfAllowed(id, provider.provider)
        if (allowed) {
            viewModel.addWidget(id, provider.provider.className, provider.loadLabel(context.packageManager), 0, 0)
            showWidgetPicker = false
        } else {
            pendingWidgetId = id
            pendingProviderInfo = provider
            val intent = Intent(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider)
            }
            bindWidgetLauncher.launch(intent)
            showWidgetPicker = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showSettingsSheet = true // Long press on empty space -> Settings (was widget picker)
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
        // Workspace
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
                    .clickable {
                        // Launch Widget Picker on Clock Tap as a shortcut? 
                        // Or just calendar/clock app. For now standard clock.
                         try {
                            context.startActivity(Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS))
                        } catch (e: Exception) {
                            try {
                                context.startActivity(Intent(Settings.ACTION_DATE_SETTINGS))
                            } catch (e: Exception) {}
                        }
                    }
            )
            
            // Re-use logic for Grid Layout
             Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Dynamic Grid
                val cellWidth = (screenWidth - 32.dp) / gridCols
                val availableHeight = 1.dp // We need real height or just use approximate. 
                // For dynamic height, we usually use BoxWithConstraints, but strictly mapping 
                // screen height chunks is okay for this stage.
                // Or simplified: Fixed height per row approx 90-110dp
                val cellHeight = 100.dp 

                homeItems.forEach { item ->
                    // Determine if this specific item is being dragged
                    val isBeingDragged = item.id == draggingHomeItemId
                    
                    // Box for each item
                    Box(
                        modifier = Modifier
                            .offset(x = cellWidth * item.x, y = cellHeight * item.y)
                            .width(if (item is HomeWidgetStack) cellWidth * item.sizeX else cellWidth)
                            .height(if (item is HomeWidgetStack) cellHeight * item.sizeY else cellHeight)
                            .alpha(if (isBeingDragged) 0f else 1f)
                            .pointerInput(item) {
                                var totalDragDistance = 0f
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { offset ->
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        totalDragDistance = 0f
                                        // Do NOT start VM drag yet, wait for move or release
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        totalDragDistance += dragAmount.getDistance()
                                        
                                        // Threshold to convert from "Long Press" to "Drag"
                                        if (totalDragDistance > 10f) {
                                            activeItem = null // Dismiss menu if showing
                                            // Start VM drag if not already started
                                            if (viewModel.draggingItemId.value != item.id) {
                                                viewModel.onDragStart(item, Pair(0f, 0f)) // Reset offset logic might be needed
                                            }
                                            // Pass separate move event
                                            val current = viewModel.dragOffset.value ?: Pair(0f, 0f)
                                            viewModel.onDrag(Pair(current.first + dragAmount.x, current.second + dragAmount.y))
                                        }
                                    },
                                    onDragEnd = {
                                        if (totalDragDistance < 10f) {
                                            // It was a long press without drag -> Show Menu
                                            activeItem = item
                                        } else {
                                            // It was a drag -> Drop
                                            val currentOffset = viewModel.dragOffset.value ?: Pair(0f, 0f)
                                            val startPxX = (cellWidth * item.x).toPx()
                                            val startPxY = (cellHeight * item.y).toPx()
                                            val dropPxX = startPxX + currentOffset.first
                                            val dropPxY = startPxY + currentOffset.second
                                            
                                            val targetX = (dropPxX / cellWidth.toPx()).toInt().coerceIn(0, gridCols - 1)
                                            val targetY = (dropPxY / cellHeight.toPx()).toInt().coerceIn(0, gridRows - 1)
                                            viewModel.onItemDrop(item.id, targetX, targetY)
                                        }
                                        // Ensure cleanup
                                        viewModel.onHomeItemDragEnd(0, 0, 0f) 
                                    },
                                    onDragCancel = { 
                                        viewModel.onHomeItemDragEnd(0, 0, 0f)
                                        totalDragDistance = 0f
                                    }
                                )
                            }
                    ) {
                        // Content
                        Box(modifier = Modifier.fillMaxSize()
                            .clickable(enabled = draggingHomeItemId == null) {
                                if (item is HomeApp) viewModel.launchApp(item.appInfo)
                            }
                        ) {
                             when (item) {
                                is HomeApp -> AppItem(app = item.appInfo, onClick = { viewModel.launchApp(item.appInfo) })
                                is HomeWidgetStack -> WidgetStack(item, appWidgetHost)
                            }
                        }

                        // Context Menu Overlay
                        if (activeItem?.id == item.id) {
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

        // App Drawer with improved Drag
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
                 viewModel.onDragStart(HomeApp(appInfo = app, x = 0, y = 0), Pair(offset.x, offset.y)) 
                 drawerProgress = 1f 
            },
            onAppDrag = { offset -> viewModel.onDrag(Pair(offset.x, offset.y)) },
            onAppDragEnd = { viewModel.onDragEnd(0, 0) },
            searchQuery = searchQuery,
            onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
            // Add callback for Reorder if feasible, or just keep generic Drag to Home
            onAppReorder = { from, to -> viewModel.onAppDrawerReorder(from, to) } 
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

        if (showWidgetPicker) {
            com.example.mbnui.ui.components.WidgetPickerSheet(
                providers = widgetProviders,
                onDismiss = { showWidgetPicker = false },
                onProviderSelected = { requestAddWidget(it) }
            )
        }

        if (showSettingsSheet) {
            com.example.mbnui.ui.components.HomeSettingsSheet(
                currentGridRows = gridRows,
                currentGridCols = gridCols,
                onGridSizeChange = { cols, rows ->
                    viewModel.setGridSize(cols, rows)
                    showSettingsSheet = false
                },
                onOpenWidgets = {
                    showSettingsSheet = false
                    showWidgetPicker = true
                },
                onDismiss = { showSettingsSheet = false }
            )
        }
    }
}
