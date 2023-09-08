package security.planck.mdm

import android.content.RestrictionEntry
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.planck.infrastructure.threading.PlanckDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import security.planck.provisioning.ProvisioningFailedException
import security.planck.provisioning.ProvisioningScope
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigurationManager @Inject constructor(
    private val preferences: Preferences,
    private val restrictionsManager: RestrictionsProvider,
    private val settingsUpdater: ConfiguredSettingsUpdater,
) {

    private val listeners = mutableListOf<RestrictionsListener>()

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

    fun loadConfigurationsBlocking(provisioningScope: ProvisioningScope) {
        runBlocking {
            loadConfigurationsSuspend(provisioningScope)
                .onFailure {
                    Timber.e(
                        it,
                        "Could not load configurations"
                    )
                }
        }
    }

    suspend fun loadConfigurationsSuspend(
        provisioningScope: ProvisioningScope = ProvisioningScope.AllSettings,
    ): Result<Unit> = withContext(PlanckDispatcher) {
        kotlin.runCatching {
            val restrictions = restrictionsManager.applicationRestrictions
            val entries: List<RestrictionEntry>
            when (provisioningScope) {
                is ProvisioningScope.Startup -> {
                    if (provisioningScope.firstStartup && !isProvisionAvailable(restrictions)) {
                        throw ProvisioningFailedException("Provisioning data is missing")
                    }
                    entries = restrictionsManager.manifestRestrictions
                        // ignore media keys from MDM before PlanckProvider has been initialized
                        .filter { it.key in PROVISIONING_RESTRICTIONS }
                }
                ProvisioningScope.InitializedEngine -> {
                    entries = restrictionsManager.manifestRestrictions
                        .filter{ it.key in INITIALIZED_ENGINE_RESTRICTIONS }
                }
                ProvisioningScope.AllAccountSettings -> {
                    entries = restrictionsManager.manifestRestrictions.filter {
                        it.key in ALL_ACCOUNT_RESTRICTIONS
                    }
                }
                ProvisioningScope.AllSettings -> {
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
        listeners.forEach {
            it.updatedRestrictions()
        }
    }

    fun addListener(listener: RestrictionsListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: RestrictionsListener) {
        listeners.remove(listener)
    }
}