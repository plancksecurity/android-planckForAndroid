package security.pEp.mdm

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.RestrictionEntry
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import security.pEp.provisioning.ProvisioningFailedException
import timber.log.Timber
import javax.inject.Inject

class ConfigurationManager(
    private val context: Context,
    private val preferences: Preferences,
    private val restrictionsManager: RestrictionsProvider
) {

    private var listener: RestrictionsListener? = null
    private var restrictionsReceiver: RestrictionsReceiver? = null
    private val k9: K9 = context.applicationContext as K9
    private val settingsUpdater = ConfiguredSettingsUpdater(k9, preferences)

    fun loadConfigurations() {
        CoroutineScope(Dispatchers.Main).launch {
            loadConfigurationsSuspend()
                .onSuccess { sendRemoteConfig() }
                .onFailure {
                    Timber.e(
                        it,
                        "Could not load configurations after registering the receiver"
                    )
                }
        }
    }

    suspend fun loadConfigurationsSuspend(
        firstTimeStartup: Boolean = false,
        startup: Boolean = false,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        kotlin.runCatching {
            val restrictions = restrictionsManager.applicationRestrictions
            if (firstTimeStartup && !isProvisionAvailable(restrictions)) {
                throw ProvisioningFailedException("Provisioning data is missing")
            }
            val entries = restrictionsManager.manifestRestrictions.let { entries ->
                if (startup) {
                    // ignore media keys from MDM before PEpProvider has been initialized
                    entries.toMutableList().filterNot { it.key == RESTRICTION_PEP_MEDIA_KEYS }
                } else entries
            }
            settingsUpdater.pEp = k9.component.backgroundpEpProvider()
            mapRestrictions(entries, restrictions)
            saveAppSettings()
            saveAccounts()
        }
    }

    private fun isProvisionAvailable(restrictions: Bundle): Boolean {
        return restrictions.keySet().containsAll(
            setOf(
                RESTRICTION_ACCOUNT_MAIL_SETTINGS,
            )
        )
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
        val editor = preferences.storage.edit()
        K9.save(editor)
        editor.commit()
    }

    private fun saveAccounts() {
        preferences.accounts.forEach { account ->
            account.save(preferences)
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

    class Factory @Inject constructor(
        private val preferences: Preferences,
        private val restrictionsManager: RestrictionsProvider,
    ) {
        fun create(
            context: Context,
        ): ConfigurationManager = ConfigurationManager(context, preferences, restrictionsManager)
    }
}