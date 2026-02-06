package com.example.mbnui.notification

import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class LauncherNotificationListener : NotificationListenerService() {

    private fun prefs() = applicationContext.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            val pkg = sbn.packageName ?: return
            val prefs = prefs()
            val key = "notif_count_$pkg"
            val current = prefs.getInt(key, 0)
            prefs.edit().putInt(key, current + 1).apply()
            // Broadcast update
            val intent = Intent("com.example.mbnui.ACTION_NOTIFICATION_COUNT_CHANGED").apply {
                putExtra("package", pkg)
                putExtra("count", current + 1)
            }
            sendBroadcast(intent)
        } catch (e: Exception) {
            // swallow
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        try {
            val pkg = sbn.packageName ?: return
            val prefs = prefs()
            val key = "notif_count_$pkg"
            val current = prefs.getInt(key, 0)
            val next = (current - 1).coerceAtLeast(0)
            prefs.edit().putInt(key, next).apply()
            val intent = Intent("com.example.mbnui.ACTION_NOTIFICATION_COUNT_CHANGED").apply {
                putExtra("package", pkg)
                putExtra("count", next)
            }
            sendBroadcast(intent)
        } catch (e: Exception) {
            // swallow
        }
    }
}
