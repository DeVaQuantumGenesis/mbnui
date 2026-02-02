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
    val appInfo: AppInfo,
    override val x: Int,
    override val y: Int
) : HomeItem()

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
    val type: WidgetType,
    val label: String
)

enum class WidgetType {
    CLOCK, CALENDAR, WEATHER, STORAGE
}
