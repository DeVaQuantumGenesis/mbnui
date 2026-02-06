package com.example.mbnui.data

import androidx.compose.runtime.Immutable
import java.util.UUID

@Immutable
sealed class HomeItem {
    abstract val id: String
    abstract val x: Int
    abstract val y: Int
}

@Immutable
data class HomeApp(
    override val id: String = UUID.randomUUID().toString(),
    val packageName: String,
    val className: String,
    override val x: Int,
    override val y: Int
) : HomeItem() {
    // Non-serializable field, populated at runtime
    var appInfo: AppInfo? = null
}

@Immutable
data class HomeFolder(
    override val id: String = UUID.randomUUID().toString(),
    val title: String = "Folder",
    val items: List<HomeApp>,
    override val x: Int,
    override val y: Int,
    val shape: FolderShape = FolderShape.SQUIRCLE
) : HomeItem()

enum class FolderShape {
    SQUARE, CIRCLE, SQUIRCLE, ROUNDED_SQUARE
}

@Immutable
data class HomeWidgetStack(
    override val id: String = UUID.randomUUID().toString(),
    val widgets: List<LauncherWidget>,
    val currentIndex: Int = 0,
    override val x: Int,
    override val y: Int,
    val sizeX: Int = 2,
    val sizeY: Int = 2
) : HomeItem()

@Immutable
data class LauncherWidget(
    val id: String = UUID.randomUUID().toString(),
    val appWidgetId: Int,
    val providerName: String,
    val label: String
)
