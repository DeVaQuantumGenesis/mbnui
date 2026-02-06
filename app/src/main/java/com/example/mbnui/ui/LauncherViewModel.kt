package com.example.mbnui.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mbnui.data.AppInfo
import com.example.mbnui.data.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.example.mbnui.data.HomeApp
import com.example.mbnui.data.HomeFolder
import com.example.mbnui.data.HomeItem
import com.example.mbnui.data.HomeWidgetStack
import com.example.mbnui.data.LauncherWidget
import com.example.mbnui.data.FolderShape
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import androidx.compose.ui.geometry.Rect
import kotlinx.coroutines.delay

@HiltViewModel
class LauncherViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    
    val filteredApps: StateFlow<List<AppInfo>> = combine(_apps, _searchQuery) { apps, query ->
        if (query.isBlank()) {
            apps
        } else {
            apps.filter { it.name.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _widgetProviders = MutableStateFlow<List<android.appwidget.AppWidgetProviderInfo>>(emptyList())
    val widgetProviders: StateFlow<List<android.appwidget.AppWidgetProviderInfo>> = _widgetProviders.asStateFlow()

    // Home items (Grid 4x6)
    private val _homeItems = MutableStateFlow<List<HomeItem>>(emptyList())
    val homeItems: StateFlow<List<HomeItem>> = _homeItems.asStateFlow()

    // Drag and Drop state
    private val _draggingItem = MutableStateFlow<AppInfo?>(null)
    val draggingItem: StateFlow<AppInfo?> = _draggingItem.asStateFlow()
    
    private val _dragOffset = MutableStateFlow<Pair<Float, Float>?>(null)
    val dragOffset: StateFlow<Pair<Float, Float>?> = _dragOffset.asStateFlow()

    private val _isCustomizing = MutableStateFlow(false)
    val isCustomizing: StateFlow<Boolean> = _isCustomizing.asStateFlow()

    data class LaunchingState(val app: AppInfo, val rect: Rect)
    private val _launchingApp = MutableStateFlow<LaunchingState?>(null)
    val launchingApp: StateFlow<LaunchingState?> = _launchingApp.asStateFlow()
    
    fun setDraggingItem(app: AppInfo?) {
        _draggingItem.value = app
    }

    init {
        loadApps()
        loadWidgets()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun setCustomizing(value: Boolean) {
        _isCustomizing.value = value
    }

    private val _gridCols = MutableStateFlow(4)
    val gridCols: StateFlow<Int> = _gridCols.asStateFlow()

    private val _gridRows = MutableStateFlow(6)
    val gridRows: StateFlow<Int> = _gridRows.asStateFlow()

    fun setGridSize(cols: Int, rows: Int) {
        _gridCols.value = cols
        _gridRows.value = rows
        
        // Sanitize items that are now out of bounds
        _homeItems.value = _homeItems.value.map { item ->
            val maxX = cols - (if (item is HomeWidgetStack) item.sizeX else 1)
            val maxY = rows - (if (item is HomeWidgetStack) item.sizeY else 1)
            
            val newX = item.x.coerceAtMost(maxX.coerceAtLeast(0))
            val newY = item.y.coerceAtMost(maxY.coerceAtLeast(0))
            
            if (newX != item.x || newY != item.y) {
                updateItemPos(item, newX, newY)
            } else {
                item
            }
        }
    }

    // Default constants used for initial fallback or calculation if needed, 
    // but UI should observe flows.


    fun onDragStart(item: HomeItem, offset: Pair<Float, Float>) {
        // Find app info if it's an app, primarily for icon display
        val appInfo = (item as? HomeApp)?.appInfo ?: (item as? HomeWidgetStack)?.widgets?.firstOrNull()?.let { 
            // Dummy AppInfo for widget drag visual (simplified)
            null 
        }
        
        // We store the ID of the item being dragged to hide it from grid
        _draggingItemId.value = item.id
        _dragOffset.value = offset
        
        // Store original position in case of cancel
        _dragStartPos.value = item.x to item.y
    }
    
    // Track ID of dragged item separately from the generic draggingItem (which was AppInfo)
    private val _draggingItemId = MutableStateFlow<String?>(null)
    val draggingItemId: StateFlow<String?> = _draggingItemId.asStateFlow()
    
    private val _dragStartPos = MutableStateFlow<Pair<Int, Int>?>(null)

    fun onDrag(offset: Pair<Float, Float>?) {
        _dragOffset.value = offset
    }

    fun onHomeItemDragEnd(screenWidthPx: Int, screenHeightPx: Int, density: Float) {
        val itemId = _draggingItemId.value ?: return
        val offset = _dragOffset.value ?: return
        
        _draggingItemId.value = null
        _dragOffset.value = null
        _dragStartPos.value = null

        // Calculate target grid position
        // This requires knowledge of cell size, which ideally should be passed or calculated
        // Simplified: assuming screen mapping
        // Grid 4x6
        
        // We need to implement a more robust coordinate to grid mapper relative to the Grid Box
        // For now, we rely on the UI passing the drop coordinates or we do a best effort mapping if logic is in VM
    }
    
    // New method called by UI with Grid coordinates
    fun onItemDrop(itemId: String, targetX: Int, targetY: Int) {
        // Check bounds
        if (targetX < 0 || targetX >= _gridCols.value || targetY < 0 || targetY >= _gridRows.value) return
        
        val item = _homeItems.value.find { it.id == itemId } ?: return
        val width = if (item is HomeWidgetStack) item.sizeX else 1
        val height = if (item is HomeWidgetStack) item.sizeY else 1
        
        // Check for collision at target
        val collidingItem = findItemAt(targetX, targetY, width, height, excludeId = itemId)
        
        if (collidingItem != null) {
            // Folder Creation Logic
            if (item is HomeApp && width == 1 && height == 1) {
                if (collidingItem is HomeApp) {
                     // Create Folder
                     createFolder(item, collidingItem)
                     return
                } else if (collidingItem is HomeFolder) {
                     // Add to Folder
                     addToFolder(item, collidingItem)
                     return
                }
            }
            
            // Swap logic: simple 1-to-1 swap if sizes match, otherwise find nearest empty
            if (width == 1 && height == 1 && 
                (collidingItem is HomeApp || (collidingItem is HomeWidgetStack && collidingItem.sizeX == 1 && collidingItem.sizeY == 1))) {
                    
                // Swap positions
                val originalX = _dragStartPos.value?.first ?: item.x
                val originalY = _dragStartPos.value?.second ?: item.y
                
                _homeItems.value = _homeItems.value.map {
                    when (it.id) {
                        itemId -> updateItemPos(it, targetX, targetY)
                        collidingItem.id -> updateItemPos(it, originalX, originalY)
                        else -> it
                    }
                }
            } else {
                // Return to start if complex collision
                 _homeItems.value = _homeItems.value // No change (snap back)
            }
        } else {
            // Move freely
             _homeItems.value = _homeItems.value.map {
                if (it.id == itemId) updateItemPos(it, targetX, targetY) else it
            }
        }
        
        _draggingItemId.value = null
        _dragStartPos.value = null
    }

    private fun createFolder(item1: HomeApp, item2: HomeApp) {
        val newFolder = HomeFolder(
            title = "Folder",
            items = listOf(item2, item1), // Order collision first then dropped
            x = item2.x,
            y = item2.y
        )
        _homeItems.value = _homeItems.value.filter { it.id != item1.id && it.id != item2.id } + newFolder
        _draggingItemId.value = null
        _dragStartPos.value = null
    }

    private fun addToFolder(item: HomeApp, folder: HomeFolder) {
        val updatedFolder = folder.copy(items = folder.items + item)
        _homeItems.value = _homeItems.value.filter { it.id != item.id }.map { 
             if (it.id == folder.id) updatedFolder else it
         }
        _draggingItemId.value = null
        _dragStartPos.value = null
    }

    fun renameFolder(folderId: String, newName: String) {
        _homeItems.value = _homeItems.value.map {
            if (it is HomeFolder && it.id == folderId) it.copy(title = newName) else it
        }
    }

    fun reorderFolderItems(folderId: String, fromIndex: Int, toIndex: Int) {
        _homeItems.value = _homeItems.value.map {
            if (it is HomeFolder && it.id == folderId) {
                val newItems = it.items.toMutableList()
                if (fromIndex in newItems.indices && toIndex in newItems.indices) {
                    val movedItem = newItems.removeAt(fromIndex)
                    newItems.add(toIndex, movedItem)
                    it.copy(items = newItems)
                } else it
            } else it
        }
    }

    fun setFolderShape(folderId: String, shape: FolderShape) {
        _homeItems.value = _homeItems.value.map {
            if (it is HomeFolder && it.id == folderId) it.copy(shape = shape) else it
        }
    }

    private fun findItemAt(x: Int, y: Int, w: Int, h: Int, excludeId: String): HomeItem? {
        return _homeItems.value.find { item ->
            if (item.id == excludeId) return@find false
            val itemW = if (item is HomeWidgetStack) item.sizeX else 1
            val itemH = if (item is HomeWidgetStack) item.sizeY else 1
            
            // Rect intersection
            x < item.x + itemW && x + w > item.x &&
            y < item.y + itemH && y + h > item.y
        }
    }
    
    private fun updateItemPos(item: HomeItem, x: Int, y: Int): HomeItem {
        return when(item) {
            is HomeApp -> item.copy(x = x, y = y)
            is HomeWidgetStack -> item.copy(x = x, y = y)
            is HomeFolder -> item.copy(x = x, y = y)
        }
    }
    
    fun resizeWidget(widgetId: String, newX: Int, newY: Int) {
        _homeItems.value = _homeItems.value.map {
             if (it.id == widgetId && it is HomeWidgetStack) {
                 it.copy(sizeX = newX, sizeY = newY)
             } else it
        }
    }

    // App addition from drawer
    fun onDragEnd(targetX: Int, targetY: Int) {
        val app = _draggingItem.value ?: return
        
        _draggingItem.value = null
        _dragOffset.value = null
        
        // Check if there is already an item at target
        val collidingItem = findItemAt(targetX, targetY, 1, 1, excludeId = "")
        if (collidingItem != null) {
             // Fallback to first empty slot if target is occupied
             val (emptyX, emptyY) = findFirstEmptySlot() ?: return
             addAppToHome(app, emptyX, emptyY)
        } else {
             addAppToHome(app, targetX, targetY)
        }
    }

    private fun findFirstEmptySlot(): Pair<Int, Int>? {
        val occupied = _homeItems.value.map { it.x to it.y }.toSet()
        val rows = _gridRows.value
        val cols = _gridCols.value
        for (y in 0 until rows) {
            for (x in 0 until cols) {
                if (!occupied.contains(x to y)) {
                    return x to y
                }
            }
        }
        return null // No space
    }

    private fun addAppToHome(app: AppInfo, x: Int, y: Int) {
        val newItem = HomeApp(appInfo = app, x = x, y = y)
        _homeItems.value = _homeItems.value + newItem
    }

    fun addWidget(appWidgetId: Int, providerName: String, label: String, x: Int, y: Int) {
        val widget = LauncherWidget(appWidgetId = appWidgetId, providerName = providerName, label = label)
        val (targetX, targetY) = findFirstEmptySlot() ?: (0 to 0)

        val stack = HomeWidgetStack(widgets = listOf(widget), x = targetX, y = targetY)
        _homeItems.value = _homeItems.value + stack
    }

    fun addWidgetToStack(stackId: String, appWidgetId: Int, providerName: String, label: String) {
        val widget = LauncherWidget(appWidgetId = appWidgetId, providerName = providerName, label = label)
        _homeItems.value = _homeItems.value.map {
            if (it is HomeWidgetStack && it.id == stackId) {
                it.copy(widgets = it.widgets + widget)
            } else it
        }
    }

    fun removeWidgetFromStack(stackId: String, widgetId: String) {
        _homeItems.value = _homeItems.value.mapNotNull {
            if (it is HomeWidgetStack && it.id == stackId) {
                val newWidgets = it.widgets.filter { w -> w.id != widgetId }
                if (newWidgets.isEmpty()) null else it.copy(widgets = newWidgets)
            } else it
        }
    }

    fun removeItem(item: HomeItem) {
        _homeItems.value = _homeItems.value.filter { it.id != item.id }
    }
    
    fun uninstallApp(app: AppInfo) {
        repository.uninstallApp(app)
    }

    fun openAppInfo(app: AppInfo) {
        repository.openAppInfo(app)
    }

    fun loadApps() {
        viewModelScope.launch {
            val installed = repository.getInstalledApps()
            // Only update if empty to preserve order (if we were persisting, we'd load persistence here)
            // For now, always load but try to keep generic sort or just load once
             if (_apps.value.isEmpty()) {
                 _apps.value = installed
             } else {
                 // Update list but try to keep existing items? simpler to just reload for now
                 // Ideally we reconcile. For this task, we just set it.
                 // If the user expects persistence across reboots, we need DB.
                 // For "session" persistence of order:
                 val currentMap = _apps.value.associateBy { it.key }
                 val newApps = installed.map { currentMap[it.key] ?: it }
                 _apps.value = newApps
             }
        }
    }

    fun loadWidgets() {
        viewModelScope.launch {
            _widgetProviders.value = repository.getWidgetProviders()
        }
    }

    fun onAppDrawerReorder(fromIndex: Int, toIndex: Int) {
        val currentList = _apps.value.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            _apps.value = currentList
        }
    }

    fun launchApp(app: AppInfo, sourceRect: Rect? = null) {
        if (sourceRect != null) {
            viewModelScope.launch {
                _launchingApp.value = LaunchingState(app, sourceRect)
                delay(400) // Animation duration
                repository.launchApp(app)
                delay(500) // Keep overlay for a bit while app loads
                _launchingApp.value = null
            }
        } else {
            repository.launchApp(app)
        }
    }
}
