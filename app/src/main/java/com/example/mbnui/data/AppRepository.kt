package com.example.mbnui.data

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserManager
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
    val key: String get() = packageName
}

@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager

    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val user = Process.myUserHandle()
        val profiles = userManager.userProfiles
        
        val allApps = mutableListOf<AppInfo>()
        
        profiles.forEach { profile ->
            launcherApps.getActivityList(null, profile).forEach { activityInfo ->
                val packageName = activityInfo.applicationInfo.packageName
                val name = activityInfo.label.toString()
                
                // Get icon (LauncherApps takes care of badging for work profiles)
                val icon = activityInfo.getIcon(0).toBitmap().asImageBitmap()
                
                // Create launch intent for this specific activity
                val launchIntent = launcherApps.getLaunchIntentForPackage(packageName, profile)
                
                allApps.add(AppInfo(packageName, name, icon, launchIntent))
            }
        }
        
        allApps.distinctBy { it.packageName }.sortedBy { it.name.lowercase() }
    }
}
