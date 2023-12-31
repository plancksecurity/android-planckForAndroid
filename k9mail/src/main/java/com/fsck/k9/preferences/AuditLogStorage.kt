package com.fsck.k9.preferences

import android.content.Context
import android.content.SharedPreferences

class AuditLogStorage(context: Context) {
    private val preferences: SharedPreferences = context.getSharedPreferences(
        AUDIT_LOG_STORAGE_PREFS,
        Context.MODE_PRIVATE
    )

    val lastTamperingDetectedTime: Long
        get() = preferences.getLong(LAST_TAMPERING_DETECTED_TIME, 0L)
    val auditLogFileExists: Boolean
        get() = preferences.getBoolean(AUDIT_LOG_FILE_EXISTS, false)
    val persistentWarningOnStartup: Boolean
        get() = preferences.getBoolean(PERSISTENT_WARNING_ON_STARTUP, false)

    fun setLastTamperingDetectedTime(lastTime: Long) {
        preferences.edit().putLong(LAST_TAMPERING_DETECTED_TIME, lastTime).apply()
    }

    fun setAuditLogFileExists(exists: Boolean) {
        preferences.edit().putBoolean(AUDIT_LOG_FILE_EXISTS, exists).apply()
    }

    fun setPersistentWarningOnStartup(warning: Boolean) {
        preferences.edit().putBoolean(PERSISTENT_WARNING_ON_STARTUP, warning).apply()
    }

    companion object {
        private const val AUDIT_LOG_STORAGE_PREFS = "audit_log_preferences"
        private const val AUDIT_LOG_FILE_EXISTS = "auditLogFileExists"
        private const val PERSISTENT_WARNING_ON_STARTUP = "persistentWarningOnStartup"
        private const val LAST_TAMPERING_DETECTED_TIME = "lastTamperingDetectedTime"
    }
}