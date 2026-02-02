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

@Composable
fun HomeScreen(
    viewModel: LauncherViewModel = viewModel(),
    appWidgetHost: android.appwidget.AppWidgetHost
) {
    val apps by viewModel.filteredApps.collectAsState()
    val homeItems by viewModel.homeItems.collectAsState()
    val isCustomizing by viewModel.isCustomizing.collectAsState()
    val draggingItem by viewModel.draggingItem.collectAsState()
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
                // Get provider info for label
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
        // App Drawer (moved to be behind content if we want transparent home, but typically drawer overlays)
        // Wait, standard launcher has wallpaper.
        // We removed the Box with Gradient.
        
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
                
                var activeItem by remember { mutableStateOf<HomeItem?>(null) }
                val draggingItemId by viewModel.draggingItemId.collectAsState()
                
                homeItems.forEach { item ->
                    val isBeingDragged = item.id == draggingItemId
                    
                    Box(
                        modifier = Modifier
                            .offset(
                                x = cellWidth * item.x,
                                y = cellHeight * item.y
                            )
                            .width(if (item is HomeWidgetStack) cellWidth * 2 else cellWidth)
                            .height(if (item is HomeWidgetStack) cellHeight * 2 else cellHeight)
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
                                        // Update drag offset (global) - logic needs refinement to accumulate
                                        // keeping simplified for this step
                                        val current = dragOffset ?: Pair(0f, 0f)
                                        viewModel.onDrag(Pair(current.first + dragAmount.x, current.second + dragAmount.y))
                                    },
                                    onDragEnd = {
                                        if (isDragging) {
                                            // Calculate Drop Grid Position
                                            val currentDragX = (dragOffset?.first ?: 0f)
                                            val currentDragY = (dragOffset?.second ?: 0f)
                                            
                                            // Approximation of grid relative position
                                            // Since we track relative movement, we need absolute start position
                                            // This is tricky without absolute coordinates. 
                                            // Simplifying: assuming dragOffset is relative to screen for now (needs proper touch tracking)
                                             
                                            // Let's assume onDragStart gives relative to Item.
                                            // We need screen coordinates for accurate drop. 
                                            // For this prototype, we'll iterate simplistically.
                                            
                                            // Better approach:
                                            // Pass simple mock drop for now as re-implementing full Coordinate system is complex in one go.
                                            // We will try to map loosely based on item start X/Y + drag amount.
                                            val startPxX = (cellWidth * item.x).toPx()
                                            val startPxY = (cellHeight * item.y).toPx()
                                            
                                            val dropPxX = startPxX + (dragOffset?.first ?: 0f) // This offset logic in VM needs check
                                            val dropPxY = startPxY + (dragOffset?.second ?: 0f)
                                            
                                            val cellW = cellWidth.toPx()
                                            val cellH = cellHeight.toPx()
                                            
                                            val targetX = (dropPxX / cellW).toInt().coerceIn(0, 3)
                                            val targetY = (dropPxY / cellH).toInt().coerceIn(0, 5)
                                            
                                            viewModel.onItemDrop(item.id, targetX, targetY)
                                        } else {
                                            // Long press without drag -> Context Menu
                                            activeItem = item
                                            // Reset drag state in VM
                                            viewModel.onHomeItemDragEnd(0, 0, 0f)
                                        }
                                    },
                                    onDragCancel = {
                                        viewModel.onHomeItemDragEnd(0, 0, 0f)
                                    }
                                )
                            }
                            .pointerInput(item) {
                                detectTapGestures(
                                    onTap = {
                                        if (item is HomeApp) {
                                            viewModel.launchApp(item.appInfo)
                                        }
                                    }
                                )
                            }
                    ) {
                        when (item) {
                            is HomeApp -> {
                                AppItem(
                                    app = item.appInfo,
                                    onClick = {} 
                                )
                            }
                            is HomeWidgetStack -> {
                                WidgetStack(item, appWidgetHost)
                            }
                        }
                        
                        // Context Menu (Existing code...)
                        if (activeItem == item) {
                            DropdownMenu(
                                expanded = true,
                                onDismissRequest = { activeItem = null },
                                modifier = Modifier.background(
                                    Color(0xFF1A1A2E).copy(alpha = 0.95f),
                                    RoundedCornerShape(12.dp)
                                ).border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Remove", color = Color.White) },
                                    onClick = {
                                        viewModel.removeItem(item)
                                        activeItem = null
                                    },
                                     leadingIcon = {
                                        Icon(
                                            painter = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_delete), 
                                            contentDescription = null,
                                            tint = Color.White
                                        )
                                    }
                                )
                                // ... (Rest of menu items)
                                if (item is HomeApp) {
                                    DropdownMenuItem(
                                        text = { Text("App Info", color = Color.White) },
                                        onClick = {
                                            viewModel.openAppInfo(item.appInfo)
                                            activeItem = null
                                        },
                                        leadingIcon = {
                                            Icon(
                                                painter = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_info_details), 
                                                contentDescription = null, 
                                                tint = Color.White
                                            )
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Uninstall", color = Color.Red.copy(alpha = 0.8f)) },
                                        onClick = {
                                            viewModel.uninstallApp(item.appInfo)
                                            activeItem = null
                                        },
                                         leadingIcon = {
                                            Icon(
                                                painter = androidx.compose.ui.res.painterResource(android.R.drawable.ic_notification_clear_all), 
                                                contentDescription = null,
                                                tint = Color.Red.copy(alpha = 0.8f)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

        // Expanded Dragging Overlay handles both AppDrawer Drag and Home Reorder Drag
        val draggingHomeItemId by viewModel.draggingItemId.collectAsState()
        val draggingAppInfo by viewModel.draggingItem.collectAsState() // From Drawer
        
        if (draggingHomeItemId != null || draggingAppInfo != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(100f)
            ) {
                 dragOffset?.let { (x, y) ->
                     // Need better absolute positioning logic here. 
                     // Using rough offset for visual feedback
                     Box(
                         modifier = Modifier
                            .offset(x = (x).dp, y = (y).dp) // Units need adjustment based on source
                            .size(64.dp) 
                     ) {
                         // Decide what to show
                         if (draggingAppInfo != null) {
                             Image(bitmap = draggingAppInfo!!.icon, contentDescription = null, modifier = Modifier.fillMaxSize())
                         } else if (draggingHomeItemId != null) {
                             val item = homeItems.find { it.id == draggingHomeItemId }
                             if (item is HomeApp) {
                                 Image(bitmap = item.appInfo.icon, contentDescription = null, modifier = Modifier.fillMaxSize())
                             } else {
                                 // Widget drag placeholder
                                 Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(0.5f)))
                             }
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
                 viewModel.launchApp(app)
                 drawerProgress = 1f
            },
            onAppDragStart = { app, offset ->
                 viewModel.onDragStart(app, Pair(offset.x, offset.y))
                 // Close drawer when drag starts, or handle visually
                 drawerProgress = 1f 
            },
            onAppDrag = { offset ->
                viewModel.onDrag(Pair(offset.x, offset.y))
            },
            onAppDragEnd = {
                // Determine drop position based on current drag offset
                // Simplified: default to 0,0 or find empty slot
                viewModel.onDragEnd(0, 0) 
            },
            searchQuery = searchQuery,
            onSearchQueryChange = { viewModel.onSearchQueryChange(it) }
        )

        // Dragging Overlay
        if (draggingItem != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(100f)
            ) {
                 dragOffset?.let { (x, y) ->
                     // This is simplified. Real implementation needs proper coordinate mapping
                     // from touch screen to local coordinates
                     Box(
                         modifier = Modifier
                            .offset(x = x.dp / density.density , y = y.dp / density.density) // Very rough approximation
                            .size(64.dp)
                     ) {
                         Image(bitmap = draggingItem!!.icon, contentDescription = null, modifier = Modifier.fillMaxSize())
                     }
                 }
            }
        }

        if (showCustomizationSheet) {
            CustomizationSheet(
                onDismiss = { showCustomizationSheet = false },
                onAddWidget = { 
                    // Launch system widget picker
                    val appWidgetId = appWidgetHost.allocateAppWidgetId()
                    val pickIntent = Intent(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
                        putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        putParcelableArrayListExtra(android.appwidget.AppWidgetManager.EXTRA_CUSTOM_INFO, ArrayList())
                        putParcelableArrayListExtra(android.appwidget.AppWidgetManager.EXTRA_CUSTOM_EXTRAS, ArrayList())
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
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .pointerInput(Unit) {
                // Simplified gesture detection that might conflict less, or handled by parent
                // For now, we keep it simple for stack swiping. 
                // In a real launcher, we'd need complex touch event handling (intercepting touch)
                detectVerticalDragGestures { change, dragAmount ->
                    // change.consume() // Do NOT consume if we want parent to see it? 
                    // Actually, standard widgets consume touch. 
                    // We only want vertical swipes for stack.
                    change.consume()
                    if (dragAmount > 20) {
                        currentIndex = (currentIndex + 1) % stack.widgets.size
                    } else if (dragAmount < -20) {
                        currentIndex = (currentIndex - 1 + stack.widgets.size) % stack.widgets.size
                    }
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
                        appWidgetHost.createView(ctx, currentWidget.appWidgetId, appWidgetInfo).apply {
                            setPadding(0, 0, 0, 0)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                 GlassBox(
                    modifier = Modifier.fillMaxSize(),
                    cornerRadius = 24.dp,
                    isDark = true
                ) {
                    Text("Widget Error", color = Color.White)
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
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .clickable { onAddWidget() },
                contentAlignment = Alignment.Center
            ) {
                 Row(verticalAlignment = Alignment.CenterVertically) {
                     Icon(
                         painter = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_add),
                         contentDescription = null,
                         tint = Color.White
                     )
                     Spacer(modifier = Modifier.width(8.dp))
                     Text("Add System Widget", color = Color.White, fontSize = 16.sp)
                 }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
