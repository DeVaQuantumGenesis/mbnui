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
import com.example.mbnui.data.HomeItem
import com.example.mbnui.data.HomeWidgetStack
import com.example.mbnui.data.LauncherWidget
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted

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

    init {
        loadApps()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun setCustomizing(value: Boolean) {
        _isCustomizing.value = value
    }

    private val GRID_COLS = 4
    private val GRID_ROWS = 6

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

    fun onDrag(offset: Pair<Float, Float>) {
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
        if (targetX < 0 || targetX >= GRID_COLS || targetY < 0 || targetY >= GRID_ROWS) return
        
        val item = _homeItems.value.find { it.id == itemId } ?: return
        val width = if (item is HomeWidgetStack) item.sizeX else 1
        val height = if (item is HomeWidgetStack) item.sizeY else 1
        
        // Check for collision at target
        val collidingItem = findItemAt(targetX, targetY, width, height, excludeId = itemId)
        
        if (collidingItem != null) {
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
    fun onDragEnd(x: Int, y: Int) {
        val app = _draggingItem.value ?: return
        
        _draggingItem.value = null
        _dragOffset.value = null
        
        // Add to home at first available position
        val (targetX, targetY) = findFirstEmptySlot() ?: return
        
        addAppToHome(app, targetX, targetY)
    }

    private fun findFirstEmptySlot(): Pair<Int, Int>? {
        val occupied = _homeItems.value.map { it.x to it.y }.toSet()
        for (y in 0 until GRID_ROWS) {
            for (x in 0 until GRID_COLS) {
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
            _apps.value = repository.getInstalledApps()
        }
    }

    fun launchApp(app: AppInfo) {
        repository.launchApp(app)
    }
}
