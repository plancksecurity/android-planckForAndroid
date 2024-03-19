package com.fsck.k9.helper

import android.content.Context
import android.content.pm.PackageInfo
import com.fsck.k9.BuildConfig
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.preferences.Storage
import com.fsck.k9.preferences.StorageEditor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import security.planck.mdm.ManageableSetting
import java.io.File

class AppUpdater(private val context: Context, private val cacheDir: File) {

    fun performOperationsOnUpdate() {
        if (appWasJustUpdated(context) == NO_APP_VERSION) {
            clearBodyCacheOnAppUpgrade()
        }
    }

    private fun clearBodyCacheOnAppUpgrade() {
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

    @Suppress("DEPRECATION")
    private fun appWasJustUpdated(context: Context): Int {
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
            val storage = Preferences.getPreferences(context).storage
            val editor = storage.edit()
            updateAppSettings(oldVersion, newVersion, storage, editor)
            K9.setAppVersionCode(newVersion) // version code updated every time
            K9.save(editor)
            editor.commit()
        }
    }

    private fun updateAppSettings(
        oldVersion: Long,
        newVersion: Long,
        storage: Storage,
        editor: StorageEditor
    ) {
        fun needsUpdateFor(target: Long) = needsUpdate(target, oldVersion, newVersion)

        if (needsUpdateFor(v3_1_7)) {
            v317Update(editor)
        }
        if (needsUpdateFor(v3_1_11)) {
            v3111Update(storage, editor)
        }
    }

    private fun v3111Update(storage: Storage, editor: StorageEditor) {
        // setting became lockable so storage changed from Boolean to String
        val previousValue = storage.getBoolean(
            "pEpUsePassphraseForNewKeys",
            BuildConfig.USE_PASSPHRASE_FOR_NEW_KEYS
        )
        editor.remove("pEpUsePassphraseForNewKeys")
        K9.setPlanckUsePassphraseForNewKeys(ManageableSetting(previousValue))
    }

    private fun v317Update(editor: StorageEditor) {
        // cleanup removed settings
        editor.remove("messageViewArchiveActionVisible")
        editor.remove("messageViewDeleteActionVisible")
        editor.remove("messageViewMoveActionVisible")
        editor.remove("messageViewCopyActionVisible")
        editor.remove("messageViewSpamActionVisible")
    }

    private fun needsUpdate(target: Long, oldVersion: Long, newVersion: Long): Boolean {
        return target in oldVersion..newVersion
    }

    companion object {
        const val NO_APP_VERSION = 1
        const val APP_UPDATED = 1
        const val APP_NOT_UPDATED = 2
        private const val v3_1_7 = 526L
        private const val v3_1_11 = 529L
        private const val VERSION_OFFSET = 10000
    }


}