package com.example.mbnui.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

import androidx.compose.runtime.Immutable

@Immutable
data class AppInfo(
    val packageName: String,
    val name: String,
    val icon: ImageBitmap,
    val launchIntent: Intent?
) {
    // Unique key for LazyVerticalGrid items
    val key: String get() = packageName
}

@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val packageManager: PackageManager = context.packageManager

    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        
        packageManager.queryIntentActivities(intent, 0).map { resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName
            val name = resolveInfo.loadLabel(packageManager).toString()
            val icon = resolveInfo.loadIcon(packageManager).toBitmap().asImageBitmap()
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            
            AppInfo(packageName, name, icon, launchIntent)
        }.sortedBy { it.name.lowercase() }
    }
}
