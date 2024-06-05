package com.fsck.k9.helper

import android.content.Context
import android.content.pm.PackageInfo
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.planck.DefaultDispatcherProvider
import com.fsck.k9.planck.DispatcherProvider
import com.fsck.k9.preferences.Storage
import com.fsck.k9.preferences.StorageEditor
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import security.planck.mdm.ManageableSetting
import security.planck.mdm.serializeBooleanManageableSetting
import java.io.File

class AppUpdater
@JvmOverloads
constructor(
    private val context: Context,
    private val cacheDir: File,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider()
) {

    private val storage: Storage
        get() = Preferences.getPreferences(context).storage

    fun performOperationsOnUpdate() {
        if (appWasJustUpdated(context) == NO_APP_VERSION) {
            clearBodyCacheOnAppUpgrade()
        }
    }

    private fun clearBodyCacheOnAppUpgrade() {
        if (cacheDir.exists()) {
            cacheDir.listFiles()?.forEach { file ->
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
        val oldVersionCode = storage.getLong("appVersionCode", -1) % VERSION_OFFSET
        return when {
            oldVersionCode == -1L -> {
                updateAppSettingsAndVersion(oldVersionCode, appVersionCode, storage)
                NO_APP_VERSION
            }

            oldVersionCode < appVersionCode -> {
                updateAppSettingsAndVersion(oldVersionCode, appVersionCode, storage)
                APP_UPDATED
            }

            else -> APP_NOT_UPDATED
        }

    }

    private fun updateAppSettingsAndVersion(oldVersion: Long, newVersion: Long, storage: Storage) = runBlocking {
        withContext(dispatcherProvider.io()) {
            val editor = storage.edit()
            updateAppSettings(oldVersion, newVersion, storage, editor)
            K9.setAppVersionCode(newVersion) // version code updated every time
            editor.putLong("appVersionCode", newVersion)
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
        if (needsUpdateFor(v3_1_14)) {
            v3114Update(editor)
        }
    }

    private fun v3111Update(storage: Storage, editor: StorageEditor) {
        // setting became lockable so storage changed from Boolean to String
        val previousValue = storage.getBoolean(
            "pEpUsePassphraseForNewKeys",
            false
        )
        editor.remove("pEpUsePassphraseForNewKeys")
        val newSetting = ManageableSetting(previousValue)
        editor.putString("pEpUsePassphraseForNewKeys", serializeBooleanManageableSetting(newSetting))
    }

    private fun v317Update(editor: StorageEditor) {
        // cleanup removed settings
        editor.remove("messageViewArchiveActionVisible")
        editor.remove("messageViewDeleteActionVisible")
        editor.remove("messageViewMoveActionVisible")
        editor.remove("messageViewCopyActionVisible")
        editor.remove("messageViewSpamActionVisible")
    }
    private fun v3114Update(editor: StorageEditor) { // name is the version just before the change, so last version that needs it
        // cleanup removed settings
        editor.remove("pEpUsePassphraseForNewKeys")
    }

    private fun needsUpdate(target: Long, oldVersion: Long, newVersion: Long): Boolean {
        return target in oldVersion until newVersion
    }

    companion object {
        const val NO_APP_VERSION = 1
        const val APP_UPDATED = 1
        const val APP_NOT_UPDATED = 2
        private const val v3_1_7 = 526L // always the version that has the old code, that needs to be changed
        private const val v3_1_11 = 529L
        private const val v3_1_14 = 534L
        private const val VERSION_OFFSET = 10000
    }


}