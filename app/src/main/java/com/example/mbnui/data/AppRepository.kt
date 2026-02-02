package com.example.mbnui.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle
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
    val className: String,
    val userHandle: UserHandle,
    val name: String,
    val icon: ImageBitmap
) {
    val key: String get() = "${packageName}_${userHandle.hashCode()}"
    val componentName: ComponentName get() = ComponentName(packageName, className)
}

@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager

    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val profiles = userManager.userProfiles
        val allApps = mutableListOf<AppInfo>()
        
        profiles.forEach { profile ->
            launcherApps.getActivityList(null, profile).forEach { activityInfo ->
                val packageName = activityInfo.applicationInfo.packageName
                val className = activityInfo.name
                val name = activityInfo.label.toString()
                val icon = activityInfo.getIcon(0).toBitmap().asImageBitmap()
                
                allApps.add(AppInfo(packageName, className, profile, name, icon))
            }
        }
        
        allApps.sortedBy { it.name.lowercase() }
    }

    fun launchApp(app: AppInfo) {
        launcherApps.startMainActivity(app.componentName, app.userHandle, null, null)
    }

    fun openAppInfo(app: AppInfo) {
        try {
            launcherApps.startAppDetailsActivity(app.componentName, app.userHandle, null, null)
        } catch (e: Exception) {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.fromParts("package", app.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun uninstallApp(app: AppInfo) {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = android.net.Uri.fromParts("package", app.packageName, null)
            putExtra(Intent.EXTRA_USER, app.userHandle)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun getWidgetProviders(): List<android.appwidget.AppWidgetProviderInfo> {
        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
        return appWidgetManager.installedProviders
    }
}
