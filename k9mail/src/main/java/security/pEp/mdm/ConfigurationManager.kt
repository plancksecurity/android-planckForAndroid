package security.pEp.mdm

import android.content.*
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject

class ConfigurationManager(
    private val context: Context,
    private val preferences: Preferences?
) {

    private var listener: RestrictionsListener? = null
    private var restrictionsReceiver: RestrictionsReceiver? = null
    private val k9: K9 = context.applicationContext as K9
    private val settingsUpdater = ConfiguredSettingsUpdater(k9, preferences)

    fun loadConfigurations() {
        CoroutineScope(Dispatchers.Main).launch {
            loadConfigurationsInBackground()
        }
    }

    suspend fun loadConfigurationsInBackground() {
        loadConfigurationsSuspend()
            .onSuccess { sendRemoteConfig() }
            .onFailure {
                Timber.e(
                    it,
                    "Could not load configurations after registering the receiver"
                )
            }
    }

    private suspend fun loadConfigurationsSuspend(): Result<Unit> = withContext(Dispatchers.IO) {
        kotlin.runCatching {
            val manager = context.getSystemService(Context.RESTRICTIONS_SERVICE)
                    as RestrictionsManager
            val restrictions = manager.applicationRestrictions
            val entries = manager.getManifestRestrictions(context.applicationContext?.packageName)
            mapRestrictions(entries, restrictions)
            saveAppSettings()
            saveAccounts()
        }
    }

    private fun mapRestrictions(
        entries: List<RestrictionEntry>,
        restrictions: Bundle,
    ) {
        entries.forEach { entry ->
            settingsUpdater.update(restrictions, entry)
        }
    }

    private fun saveAppSettings() {
        preferences?.let {
            val editor = preferences.storage.edit()
            K9.save(editor)
            editor.commit()
        }
    }

    private fun saveAccounts() {
        preferences?.let {
            preferences.accounts.forEach { account ->
                account.save(preferences)
            }
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun sendRemoteConfig() {
        listener?.updatedRestrictions()
    }

    fun unregisterReceiver() {
        if (restrictionsReceiver != null) {
            context.applicationContext.unregisterReceiver(restrictionsReceiver)
            restrictionsReceiver = null
        }
    }

    fun registerReceiver() {
        if (restrictionsReceiver == null) {
            restrictionsReceiver = RestrictionsReceiver(this)
        }
        context.applicationContext.registerReceiver(
            restrictionsReceiver, IntentFilter(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED)
        )
    }

    fun setListener(listener: RestrictionsListener) {
        this.listener = listener
    }

    class Factory @Inject constructor() {
        fun getInstance(
            context: Context,
            preferences: Preferences? = null
        ): ConfigurationManager = ConfigurationManager(context, preferences)
    }
}