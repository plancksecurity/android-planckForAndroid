package com.fsck.k9.helper

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import com.fsck.k9.K9
import com.fsck.k9.Preferences
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
        val appVersionCode =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    packageInfo.versionCode.toLong()
                }

        val oldVersionCode = K9.getAppVersionCode()
        return when {
            oldVersionCode == -1L -> {
                saveVersionNameAndCode(appVersionCode, context)
                NO_APP_VERSION
            }
            oldVersionCode < appVersionCode -> {
                saveVersionNameAndCode(appVersionCode, context)
                APP_UPDATED
            }
            else -> APP_NOT_UPDATED
        }

    }

    private fun saveVersionNameAndCode(appVersion: Long, context: Context) {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            K9.setAppVersionCode(appVersion)
            val editor = Preferences.getPreferences(context).storage.edit()
            K9.save(editor)
            editor.commit()
        }
    }

    companion object {
        const val NO_APP_VERSION = 1
        const val APP_UPDATED = 1
        const val APP_NOT_UPDATED = 2
    }


}