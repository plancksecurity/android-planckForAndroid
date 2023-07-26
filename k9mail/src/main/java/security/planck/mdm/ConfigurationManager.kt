package security.planck.mdm

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.RestrictionEntry
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.planck.infrastructure.threading.PlanckDispatcher
import kotlinx.coroutines.*
import security.planck.provisioning.ProvisioningFailedException
import security.planck.provisioning.ProvisioningStage
import timber.log.Timber
import javax.inject.Inject

class ConfigurationManager @Inject constructor(
    private val k9: K9,
    private val preferences: Preferences,
    private val restrictionsManager: RestrictionsProvider,
    private val settingsUpdater: ConfiguredSettingsUpdater,
) {

    private var listener: RestrictionsListener? = null
    private var restrictionsReceiver: RestrictionsReceiver? = null

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

    fun loadConfigurationsBlocking() {
        runBlocking {
            loadConfigurationsSuspend()
                .onFailure {
                    Timber.e(
                        it,
                        "Could not load configurations"
                    )
                }
        }
    }

    suspend fun loadConfigurationsSuspend(
        provisioningStage: ProvisioningStage = ProvisioningStage.ProvisioningDone,
    ): Result<Unit> = withContext(PlanckDispatcher) {
        kotlin.runCatching {
            val restrictions = restrictionsManager.applicationRestrictions
            val entries: List<RestrictionEntry>
            when (provisioningStage) {
                is ProvisioningStage.Startup -> {
                    if (provisioningStage.firstStartup && !isProvisionAvailable(restrictions)) {
                        throw ProvisioningFailedException("Provisioning data is missing")
                    }
                    entries = restrictionsManager.manifestRestrictions
                        // ignore media keys from MDM before PEpProvider has been initialized
                        .filterNot { it.key in INITIALIZED_ENGINE_RESTRICTIONS }
                }
                is ProvisioningStage.InitializedEngine -> {
                    entries = restrictionsManager.manifestRestrictions
                        .filter{ it.key in INITIALIZED_ENGINE_RESTRICTIONS }
                }
                is ProvisioningStage.ProvisioningDone -> {
                    entries = restrictionsManager.manifestRestrictions
                }
            }

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
            k9.unregisterReceiver(restrictionsReceiver)
            restrictionsReceiver = null
        }
    }

    fun registerReceiver() {
        if (restrictionsReceiver == null) {
            restrictionsReceiver = RestrictionsReceiver(this)
        }
        k9.registerReceiver(
            restrictionsReceiver, IntentFilter(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED)
        )
    }

    fun setListener(listener: RestrictionsListener) {
        this.listener = listener
    }
}