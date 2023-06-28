package com.fsck.k9.preferences

import android.content.Context
import android.content.SharedPreferences

class AppAliveMonitorStorage(context: Context) {
    private val preferences: SharedPreferences = context.getSharedPreferences(
        APP_ALIVE_MONITOR_STORAGE_PREFS,
        Context.MODE_PRIVATE
    )

    val lastAppAliveMonitoredTime: Long
        get() = preferences.getLong(LAST_ALIVE_MONITORED_TIME, 0L)

    fun setLastAppAliveMonitoredTime(lastTime: Long) {
        preferences.edit().putLong(LAST_ALIVE_MONITORED_TIME, lastTime).apply()
    }

    companion object {
        private const val APP_ALIVE_MONITOR_STORAGE_PREFS = "app_alive_monitor_preferences"
        private const val LAST_ALIVE_MONITORED_TIME = "lastAppAliveMonitoredTime"
    }
}