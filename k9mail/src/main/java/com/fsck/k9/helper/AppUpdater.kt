package com.fsck.k9.helper

import android.content.Context
import android.content.pm.PackageInfo
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.preferences.StorageEditor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

class AppUpdater(private val context: Context, private val cacheDir: File) {

    fun clearBodyCacheIfAppUpgrade() {
        if (isAppUpdated(context) == NO_APP_VERSION) {
            val dir = File(cacheDir.absolutePath)
            if (dir.exists()) {
                dir.listFiles()?.forEach { file ->
                    val isBody = file.name.contains("body")
                    if (isBody) {
                        file.delete()
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun isAppUpdated(context: Context): Int {
        val packageInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val appVersionCode = packageInfo.longVersionCode % VERSION_OFFSET

        val oldVersionCode = K9.getAppVersionCode() % VERSION_OFFSET
        return when {
            oldVersionCode == -1L -> {
                updateAppSettingsAndVersion(oldVersionCode, appVersionCode, context)
                NO_APP_VERSION
            }
            oldVersionCode < appVersionCode -> {
                updateAppSettingsAndVersion(oldVersionCode, appVersionCode, context)
                APP_UPDATED
            }
            else -> APP_NOT_UPDATED
        }

    }

    private fun updateAppSettingsAndVersion(oldVersion: Long, newVersion: Long, context: Context) {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            val editor = Preferences.getPreferences(context).storage.edit()
            updateAppSettings(oldVersion, editor)
            K9.setAppVersionCode(newVersion) // version code updated every time
            K9.save(editor)
            editor.commit()
        }
    }

    private fun updateAppSettings(oldVersion: Long, editor: StorageEditor) {
        when (oldVersion) {
            v317 -> {
                // cleanup removed settings
                editor.remove("messageViewArchiveActionVisible")
                editor.remove("messageViewDeleteActionVisible")
                editor.remove("messageViewMoveActionVisible")
                editor.remove("messageViewCopyActionVisible")
                editor.remove("messageViewSpamActionVisible")
            }
            else -> {}
        }
    }

    companion object {
        const val NO_APP_VERSION = 1
        const val APP_UPDATED = 1
        const val APP_NOT_UPDATED = 2
        private const val v317 = 526L
        private const val VERSION_OFFSET = 10000
    }


}