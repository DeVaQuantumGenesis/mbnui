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

    fun onDragStart(app: AppInfo, offset: Pair<Float, Float>) {
        _draggingItem.value = app
        _dragOffset.value = offset
    }
    
    fun onDrag(offset: Pair<Float, Float>) {
        _dragOffset.value = offset
    }

    private val GRID_COLS = 4
    private val GRID_ROWS = 6

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
        // Find empty slot for widget - assume 2x2
        // Simplified: just put it at 0,0 or next available
        val (targetX, targetY) = findFirstEmptySlot() ?: (0 to 0)

        val stack = HomeWidgetStack(widgets = listOf(widget), x = targetX, y = targetY)
        _homeItems.value = _homeItems.value + stack
    }

    fun removeItem(item: HomeItem) {
        _homeItems.value = _homeItems.value.filter { it.id != item.id }
    }
    
    fun uninstallApp(app: AppInfo) {
        repository.uninstallApp(app)
        // Note: The app list update is handled by simple reload or observing package changes,
        // but for now we rely on explicit reload or system broadcast (not implemented here)
    }

    fun openAppInfo(app: AppInfo) {
        repository.openAppInfo(app)
    }

    fun moveItem(itemId: String, x: Int, y: Int) {
        _homeItems.value = _homeItems.value.map {
            if (it.id == itemId) {
                when(it) {
                    is HomeApp -> it.copy(x = x, y = y)
                    is HomeWidgetStack -> it.copy(x = x, y = y)
                }
            } else it
        }
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
