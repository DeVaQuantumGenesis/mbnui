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
import com.example.mbnui.ui.components.AppLaunchOverlay

import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.zIndex
import com.example.mbnui.ui.components.AppItem
import com.example.mbnui.ui.components.DockItem
import com.example.mbnui.ui.components.OneUiMenu
import com.example.mbnui.ui.components.OneUiMenuItem
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import com.example.mbnui.data.FolderShape
import com.example.mbnui.data.HomeFolder
import kotlin.math.roundToInt
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.geometry.Rect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: LauncherViewModel = viewModel(),
    appWidgetHost: android.appwidget.AppWidgetHost
) {
    val apps by viewModel.filteredApps.collectAsState()
    val homeItems by viewModel.homeItems.collectAsState()
    val isCustomizing by viewModel.isCustomizing.collectAsState()
    val draggingAppInfo by viewModel.draggingItem.collectAsState()
    val draggingHomeItemId by viewModel.draggingItemId.collectAsState()
    val dragOffset by viewModel.dragOffset.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val widgetProviders by viewModel.widgetProviders.collectAsState()
    val gridCols by viewModel.gridCols.collectAsState()
    val gridRows by viewModel.gridRows.collectAsState()
    val launchingApp by viewModel.launchingApp.collectAsState()
    
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    val density = LocalDensity.current

    var drawerProgress by remember { mutableFloatStateOf(1f) }
    val animatedProgress by animateFloatAsState(
        targetValue = drawerProgress,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "drawer_animation"
    )

    val drawerOffsetY = animatedProgress * screenHeight.value
    var showWidgetPicker by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    var activeItem by remember { mutableStateOf<HomeItem?>(null) }
    var activeApp by remember { mutableStateOf<AppInfo?>(null) }
    var activeFolderId by remember { mutableStateOf<String?>(null) }
    var renamingFolderId by remember { mutableStateOf<String?>(null) }
    
    var resizingWidgetId by remember { mutableStateOf<String?>(null) }
    
    var pendingWidgetId by remember { mutableStateOf<Int?>(null) }
    var pendingProviderInfo by remember { mutableStateOf<android.appwidget.AppWidgetProviderInfo?>(null) }
    var pendingTargetStackId by remember { mutableStateOf<String?>(null) }

    val bindWidgetLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val id = pendingWidgetId
            val info = pendingProviderInfo
            if (id != null && info != null) {
                if (pendingTargetStackId != null) {
                    viewModel.addWidgetToStack(pendingTargetStackId!!, id, info.provider.className, info.loadLabel(context.packageManager))
                    pendingTargetStackId = null
                } else {
                    viewModel.addWidget(id, info.provider.className, info.loadLabel(context.packageManager), 0, 0)
                }
            }
        }
        pendingWidgetId = null
        pendingProviderInfo = null
    }

    fun requestAddWidget(provider: android.appwidget.AppWidgetProviderInfo) {
        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
        val id = appWidgetHost.allocateAppWidgetId()
        val allowed = appWidgetManager.bindAppWidgetIdIfAllowed(id, provider.provider)
        if (allowed) {
            if (pendingTargetStackId != null) {
                viewModel.addWidgetToStack(pendingTargetStackId!!, id, provider.provider.className, provider.loadLabel(context.packageManager))
                pendingTargetStackId = null
            } else {
                viewModel.addWidget(id, provider.provider.className, provider.loadLabel(context.packageManager), 0, 0)
            }
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
                    onTap = {
                         activeItem = null
                         resizingWidgetId = null
                    },
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showSettingsSheet = true 
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
                         try {
                            context.startActivity(Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS))
                        } catch (e: Exception) {
                            try {
                                context.startActivity(Intent(Settings.ACTION_DATE_SETTINGS))
                            } catch (e: Exception) {}
                        }
                    }
            )
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val containerWidth = maxWidth
                    val containerHeight = maxHeight
                    val calculatedCellWidth = containerWidth / gridCols
                    val calculatedCellHeight = containerHeight / gridRows
                    
                    if (draggingHomeItemId != null || draggingAppInfo != null) {
                         val currentDrag = dragOffset ?: Pair(0f, 0f)
                         val draggedItem = homeItems.find { it.id == draggingHomeItemId }
                         val startX = (draggedItem?.x ?: 0) * calculatedCellWidth.value
                         val startY = (draggedItem?.y ?: 0) * calculatedCellHeight.value
                         val rawTargetX = if (draggedItem != null) startX + currentDrag.first / density.density else currentDrag.first / density.density
                         val rawTargetY = if (draggedItem != null) startY + currentDrag.second / density.density else currentDrag.second / density.density
                         val gridX = (rawTargetX / calculatedCellWidth.value).roundToInt().coerceIn(0, gridCols - 1)
                         val gridY = (rawTargetY / calculatedCellHeight.value).roundToInt().coerceIn(0, gridRows - 1)
                         val itemW = if (draggedItem is HomeWidgetStack) draggedItem.sizeX else 1
                         val itemH = if (draggedItem is HomeWidgetStack) draggedItem.sizeY else 1
                         val safeGridX = gridX.coerceAtMost(gridCols - itemW)
                         val safeGridY = gridY.coerceAtMost(gridRows - itemH)

                         Box(
                             modifier = Modifier
                                 .offset(x = calculatedCellWidth * safeGridX, y = calculatedCellHeight * safeGridY)
                                 .width(calculatedCellWidth * itemW)
                                 .height(calculatedCellHeight * itemH)
                                 .background(Color.White.copy(0.2f), RoundedCornerShape(12.dp))
                                 .border(1.dp, Color.White.copy(0.5f), RoundedCornerShape(12.dp))
                         )
                    }

                    homeItems.forEach { item ->
                        val isBeingDragged = item.id == draggingHomeItemId
                        val isResizing = item.id == resizingWidgetId
                        
                        Box(
                            modifier = Modifier
                                .offset(x = calculatedCellWidth * item.x, y = calculatedCellHeight * item.y)
                                .width(calculatedCellWidth * (if (item is HomeWidgetStack) item.sizeX else 1))
                                .height(calculatedCellHeight * (if (item is HomeWidgetStack) item.sizeY else 1))
                                .alpha(if (isBeingDragged) 0f else 1f)
                                .zIndex(if (isResizing) 10f else 1f)
                                .pointerInput(item, isResizing) {
                                    if (!isResizing) {
                                        var totalDragDistance = 0f
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { offset ->
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                totalDragDistance = 0f
                                                // Pre-emptively show menu, it will be hidden if drag distance exceeds threshold
                                                activeItem = item
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                totalDragDistance += dragAmount.getDistance()
                                                if (totalDragDistance > 15f) {
                                                    activeItem = null // Hide menu if we move too much
                                                    resizingWidgetId = null 
                                                    if (viewModel.draggingItemId.value != item.id) {
                                                        viewModel.onDragStart(item, Pair(0f, 0f))
                                                    }
                                                    val current = viewModel.dragOffset.value ?: Pair(0f, 0f)
                                                    viewModel.onDrag(Pair(current.first + dragAmount.x, current.second + dragAmount.y))
                                                }
                                            },
                                            onDragEnd = {
                                                if (totalDragDistance <= 15f) {
                                                    // Menu remains visible
                                                } else {
                                                    val currentOffset = viewModel.dragOffset.value ?: Pair(0f, 0f)
                                                    val startPxX = with(density) { calculatedCellWidth.toPx() } * item.x
                                                    val startPxY = with(density) { calculatedCellHeight.toPx() } * item.y
                                                    val dropPxX = startPxX + currentOffset.first
                                                    val dropPxY = startPxY + currentOffset.second
                                                    val targetX = (dropPxX / with(density) { calculatedCellWidth.toPx() }).roundToInt().coerceIn(0, gridCols - 1)
                                                    val targetY = (dropPxY / with(density) { calculatedCellHeight.toPx() }).roundToInt().coerceIn(0, gridRows - 1)
                                                    viewModel.onItemDrop(item.id, targetX, targetY)
                                                }
                                                viewModel.onHomeItemDragEnd(0, 0, 0f) 
                                            },
                                            onDragCancel = { 
                                                viewModel.onHomeItemDragEnd(0, 0, 0f)
                                                totalDragDistance = 0f
                                            }
                                        )
                                    }
                                }
                        ) {
                            var itemRect by remember { mutableStateOf(Rect.Zero) }
                            Box(modifier = Modifier
                                .fillMaxSize()
                                .onGloballyPositioned { itemRect = it.boundsInWindow() }
                                .clickable(enabled = draggingHomeItemId == null && !isResizing && item is HomeFolder) {
                                    if (item is HomeFolder) activeFolderId = item.id
                                }
                            ) {
                                 when (item) {
                                    is HomeApp -> AppItem(app = item.appInfo, onClick = { rect -> viewModel.launchApp(item.appInfo, rect) })
                                    is HomeWidgetStack -> WidgetStack(item, appWidgetHost)
                                    is HomeFolder -> FolderIcon(folder = item)
                                }
                            }
                            
                            if (isResizing && item is HomeWidgetStack) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .border(2.dp, Color(0xFF3D5AFE), RoundedCornerShape(12.dp))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(32.dp)
                                            .background(Color(0xFF3D5AFE), RoundedCornerShape(topStart = 12.dp))
                                            .pointerInput(Unit) {
                                                detectDragGestures(onDragEnd = {}) { change, dragAmount ->
                                                    change.consume()
                                                    if (dragAmount.x > 20) viewModel.resizeWidget(item.id, (item.sizeX + 1).coerceAtMost(gridCols - item.x), item.sizeY)
                                                    if (dragAmount.x < -20) viewModel.resizeWidget(item.id, (item.sizeX - 1).coerceAtLeast(1), item.sizeY)
                                                    if (dragAmount.y > 20) viewModel.resizeWidget(item.id, item.sizeX, (item.sizeY + 1).coerceAtMost(gridRows - item.y))
                                                    if (dragAmount.y < -20) viewModel.resizeWidget(item.id, item.sizeX, (item.sizeY - 1).coerceAtLeast(1))
                                                }
                                            }
                                    ) {
                                        Icon(painterResource(android.R.drawable.ic_menu_crop), null, tint = Color.White, modifier = Modifier.padding(4.dp))
                                    }
                                }
                            }

                                if (activeItem?.id == item.id) {
                                OneUiMenu(
                                    expanded = true,
                                    onDismissRequest = { activeItem = null },
                                    anchorBounds = itemRect
                                ) {
                                    OneUiMenuItem(
                                        text = "Remove",
                                        icon = android.R.drawable.ic_menu_delete,
                                        onClick = { viewModel.removeItem(item); activeItem = null }
                                    )
                                    if (item is HomeWidgetStack) {
                                        OneUiMenuItem(
                                            text = "Resize",
                                            icon = android.R.drawable.ic_menu_crop,
                                            onClick = { resizingWidgetId = item.id; activeItem = null }
                                        )
                                        OneUiMenuItem(
                                            text = "Manage Stack",
                                            icon = android.R.drawable.ic_menu_manage,
                                            onClick = { pendingTargetStackId = item.id; showWidgetPicker = true; activeItem = null }
                                        )
                                    }
                                    if (item is HomeFolder) {
                                        OneUiMenuItem(
                                            text = "Rename",
                                            icon = android.R.drawable.ic_menu_edit,
                                            onClick = { renamingFolderId = item.id; activeItem = null }
                                        )
                                        // Simplified shape picker for OneUI style
                                        FolderShape.values().forEach { shape ->
                                            OneUiMenuItem(
                                                text = "Shape: ${shape.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                                onClick = { viewModel.setFolderShape(item.id, shape); activeItem = null }
                                            )
                                        }
                                    }
                                    if (item is HomeApp) {
                                        OneUiMenuItem(
                                            text = "App Info",
                                            icon = android.R.drawable.ic_menu_info_details,
                                            onClick = { viewModel.openAppInfo(item.appInfo); activeItem = null }
                                        )
                                        OneUiMenuItem(
                                            text = "Uninstall",
                                            icon = android.R.drawable.ic_notification_clear_all,
                                            onClick = { viewModel.uninstallApp(item.appInfo); activeItem = null },
                                            isDestructive = true
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth().padding(bottom = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                GlassBox(modifier = Modifier.width(320.dp).height(96.dp), cornerRadius = 28.dp, isDark = true) {
                    Row(modifier = Modifier.fillMaxSize(), Arrangement.SpaceEvenly, Alignment.CenterVertically) {
                        apps.take(4).forEach { app ->
                            var totalDragDistance = 0f
                            Box(
                                modifier = Modifier.pointerInput(app) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { offset ->
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            totalDragDistance = 0f
                                            activeApp = app
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            totalDragDistance += dragAmount.getDistance()
                                            if (totalDragDistance > 15f) {
                                                activeApp = null
                                                if (viewModel.draggingItem.value != app) {
                                                    viewModel.setDraggingItem(app)
                                                }
                                                val current = viewModel.dragOffset.value ?: Pair(0f, 0f)
                                                viewModel.onDrag(Pair(current.first + dragAmount.x, current.second + dragAmount.y))
                                            }
                                        },
                                        onDragEnd = {
                                            if (totalDragDistance > 15f) {
                                                val currentOffset = viewModel.dragOffset.value ?: Pair(0f, 0f)
                                                val screenWidthPx = with(density) { screenWidth.toPx() }
                                                val screenHeightPx = with(density) { screenHeight.toPx() }
                                                val targetX = (currentOffset.first / (screenWidthPx / gridCols)).roundToInt().coerceIn(0, gridCols - 1)
                                                val targetY = (currentOffset.second / (screenHeightPx / gridRows)).roundToInt().coerceIn(0, gridRows - 1)
                                                
                                                viewModel.onDragEnd(targetX, targetY)
                                            } else {
                                                viewModel.setDraggingItem(null)
                                                viewModel.onDrag(null)
                                            }
                                        },
                                        onDragCancel = {
                                            viewModel.setDraggingItem(null)
                                            viewModel.onDrag(null)
                                        }
                                    )
                                }
                            ) {
                                var dockRect by remember { mutableStateOf(Rect.Zero) }
                                Box(modifier = Modifier.onGloballyPositioned { dockRect = it.boundsInWindow() }) {
                                    DockItem(app) { rect ->
                                        viewModel.launchApp(app, rect)
                                    }
                                }

                                if (activeApp?.key == app.key) {
                                    OneUiMenu(
                                        expanded = true,
                                        onDismissRequest = { activeApp = null },
                                        anchorBounds = dockRect
                                    ) {
                                        OneUiMenuItem(
                                            text = "App Info",
                                            icon = android.R.drawable.ic_menu_info_details,
                                            onClick = { viewModel.openAppInfo(app); activeApp = null }
                                        )
                                        OneUiMenuItem(
                                            text = "Uninstall",
                                            icon = android.R.drawable.ic_notification_clear_all,
                                            onClick = { viewModel.uninstallApp(app); activeApp = null },
                                            isDestructive = true
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        AppDrawer(
            apps = apps,
            offsetY = drawerOffsetY,
            onClose = { drawerProgress = if (drawerProgress < 0.5f) 0f else 1f },
            onDrag = { dragAmount ->
                val delta = (dragAmount / density.density) / screenHeight.value
                drawerProgress = (drawerProgress + delta * 1.5f).coerceIn(0f, 1f)
            },
            onAppClick = { app, rect -> viewModel.launchApp(app, rect); drawerProgress = 1f },
            onAppDragStart = { app, offset ->
                 viewModel.setDraggingItem(app)
                 viewModel.onDrag(Pair(offset.x, offset.y)) 
                 drawerProgress = 1f 
            },
            onAppDrag = { offset -> 
                val current = viewModel.dragOffset.value ?: Pair(0f, 0f)
                viewModel.onDrag(Pair(current.first + offset.x, current.second + offset.y)) 
            },
            onAppDragEnd = { 
                val currentOffset = viewModel.dragOffset.value ?: Pair(0f, 0f)
                val screenWidthPx = with(density) { screenWidth.toPx() }
                val screenHeightPx = with(density) { screenHeight.toPx() }
                val targetX = (currentOffset.first / (screenWidthPx / gridCols)).roundToInt().coerceIn(0, gridCols - 1)
                val targetY = (currentOffset.second / (screenHeightPx / gridRows)).roundToInt().coerceIn(0, gridRows - 1)
                viewModel.onDragEnd(targetX, targetY) 
            },
            searchQuery = searchQuery,
            onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
            onAppReorder = { from, to -> viewModel.onAppDrawerReorder(from, to) },
            onAppInfo = { viewModel.openAppInfo(it) },
            onUninstall = { viewModel.uninstallApp(it) }
        )

        if (draggingHomeItemId != null || draggingAppInfo != null) {
            Box(modifier = Modifier.fillMaxSize().zIndex(100f)) {
                 dragOffset?.let { (x, y) ->
                     val item = if (draggingHomeItemId != null) homeItems.find { it.id == draggingHomeItemId } else null
                     val finalX = if (draggingAppInfo != null) x / density.density else {
                         val cellW = (screenWidth - 32.dp) / gridCols
                         val originalX = (item?.x ?: 0) * cellW.value
                         originalX + (x / density.density)
                     }
                      
                     val finalY = if (draggingAppInfo != null) y / density.density else {
                         val cellH = (screenHeight - 192.dp) / gridRows 
                         val originalY = (item?.y ?: 0) * cellH.value
                         originalY + 100 + (y / density.density)
                     }

                     Box(
                         modifier = Modifier
                            .offset(x = finalX.dp, y = finalY.dp)
                            .size(64.dp) 
                     ) {
                         if (draggingAppInfo != null) {
                             Image(bitmap = draggingAppInfo!!.icon, contentDescription = null, modifier = Modifier.fillMaxSize())
                         } else if (item is HomeApp) {
                             Image(bitmap = item.appInfo.icon, contentDescription = null, modifier = Modifier.fillMaxSize())
                         } else if (item is HomeFolder) {
                             GlassBox(modifier = Modifier.fillMaxSize(), cornerRadius = 12.dp, isDark = true) { }
                         } else if (item is HomeWidgetStack) {
                             Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(0.5f), RoundedCornerShape(12.dp)))
                         }
                     }
                 }
            }
        }
        
        val activeFolder = homeItems.find { it.id == activeFolderId } as? HomeFolder
        if (activeFolder != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(200f)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { activeFolderId = null },
                contentAlignment = Alignment.Center
            ) {
                 GlassBox(modifier = Modifier.size(320.dp), cornerRadius = 28.dp, isDark = true) {
                     Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                         Text(
                             text = activeFolder.title,
                             color = Color.White,
                             fontSize = 20.sp,
                             textAlign = TextAlign.Center,
                             modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).clickable { renamingFolderId = activeFolder.id }
                         )
                         
                         LazyVerticalGrid(
                             columns = GridCells.Adaptive(minSize = 64.dp),
                             modifier = Modifier.weight(1f),
                             verticalArrangement = Arrangement.spacedBy(16.dp),
                             horizontalArrangement = Arrangement.spacedBy(16.dp)
                         ) {
                              items(activeFolder.items.size) { index ->
                                 val app = activeFolder.items[index]
                                 Box(
                                     modifier = Modifier
                                         .pointerInput(activeFolder.id, index) {
                                             detectDragGesturesAfterLongPress(
                                                 onDragStart = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                                                 onDrag = { change, dragAmount -> 
                                                     change.consume()
                                                     if (dragAmount.x > 30) viewModel.reorderFolderItems(activeFolder.id, index, (index + 1).coerceAtMost(activeFolder.items.size - 1))
                                                     if (dragAmount.x < -30) viewModel.reorderFolderItems(activeFolder.id, index, (index - 1).coerceAtLeast(0))
                                                 }
                                             )
                                         }
                                 ) {
                                     AppItem(app = app.appInfo, onClick = { rect ->
                                         viewModel.launchApp(app.appInfo, rect)
                                         activeFolderId = null
                                     })
                                 }
                             }
                         }
                     }
                 }
            }
        }

        if (renamingFolderId != null) {
            val folder = homeItems.find { it.id == renamingFolderId } as? HomeFolder
            if (folder != null) {
                var text by remember { mutableStateOf(folder.title) }
                AlertDialog(
                    onDismissRequest = { renamingFolderId = null },
                    title = { Text("Rename Folder") },
                    text = { OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth()) },
                    confirmButton = { Button(onClick = { viewModel.renameFolder(folder.id, text); renamingFolderId = null }) { Text("OK") } },
                    dismissButton = { TextButton(onClick = { renamingFolderId = null }) { Text("Cancel") } }
                )
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
                onOpenWidgets = { showSettingsSheet = false; showWidgetPicker = true },
                onDismiss = { showSettingsSheet = false }
            )
        }

        launchingApp?.let { state ->
            AppLaunchOverlay(
                app = state.app,
                sourceRect = state.rect,
                onAnimationEnd = { /* ViewModel handles the actual launch delay */ }
            )
        }
    }
}

@Composable
fun FolderIcon(folder: HomeFolder) {
    val shape: Shape = when (folder.shape) {
        FolderShape.SQUARE -> RoundedCornerShape(0.dp)
        FolderShape.CIRCLE -> CircleShape
        FolderShape.SQUIRCLE -> RoundedCornerShape(20.dp)
        FolderShape.ROUNDED_SQUARE -> RoundedCornerShape(12.dp)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f)
                .background(Color.White.copy(0.2f), shape)
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val previewApps = folder.items.take(4)
                repeat(2) { rowIndex ->
                    Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                         repeat(2) { colIndex ->
                             val index = rowIndex * 2 + colIndex
                             if (index < previewApps.size) {
                                 Image(bitmap = previewApps[index].appInfo.icon, contentDescription = null, modifier = Modifier.weight(1f).fillMaxHeight())
                             } else {
                                 Spacer(modifier = Modifier.weight(1f))
                             }
                         }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = folder.title, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
fun WidgetStack(
    stack: HomeWidgetStack,
    appWidgetHost: android.appwidget.AppWidgetHost
) {
    var currentIndex by remember { mutableIntStateOf(stack.currentIndex.coerceAtLeast(0)) }
    val context = LocalContext.current
    var userInteracting by remember { mutableStateOf(false) }

    // Auto-rotate similar to iOS Smart Stack (simple rotation)
    LaunchedEffect(stack.id, stack.widgets.size) {
        while (true) {
            if (!userInteracting && stack.widgets.size > 1) {
                kotlinx.coroutines.delay(8000)
                currentIndex = (currentIndex + 1) % stack.widgets.size
            } else {
                kotlinx.coroutines.delay(500)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .pointerInput(stack.id) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        userInteracting = true
                        if (dragAmount > 20) currentIndex = (currentIndex + 1) % stack.widgets.size
                        else if (dragAmount < -20) currentIndex = (currentIndex - 1 + stack.widgets.size) % stack.widgets.size
                    },
                    onDragEnd = { userInteracting = false },
                    onDragCancel = { userInteracting = false }
                )
            }
    ) {
        val currentWidget = stack.widgets.getOrNull(currentIndex)
        androidx.compose.animation.Crossfade(targetState = currentIndex) { _ ->
            if (currentWidget != null) {
                val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
                val appWidgetInfo = appWidgetManager.getAppWidgetInfo(currentWidget.appWidgetId)
                if (appWidgetInfo != null) {
                    AndroidView(
                        factory = { ctx -> appWidgetHost.createView(ctx, currentWidget.appWidgetId, appWidgetInfo).apply { setPadding(0, 0, 0, 0) } },
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
}
