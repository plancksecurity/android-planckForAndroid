package security.planck.mdm

import android.content.RestrictionEntry
import android.os.Bundle
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.planck.DispatcherProvider
import com.fsck.k9.planck.infrastructure.extensions.modifyItems
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val dispatcherProvider: DispatcherProvider,
) {

    private val restrictionsUpdatedMF: MutableStateFlow<Int> = MutableStateFlow(0)
    val restrictionsUpdatedFlow = restrictionsUpdatedMF.asStateFlow()

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
    ): Result<Unit> = withContext(dispatcherProvider.planckDispatcher()) {
        kotlin.runCatching {
            val restrictions = restrictionsManager.applicationRestrictions
            val entries: List<RestrictionEntry>
            when (provisioningScope) {
                ProvisioningScope.FirstStartup -> {
                    if (!isProvisionAvailable(restrictions)) {
                        throw ProvisioningFailedException("Provisioning data is missing")
                    }
                    entries = provisioningEntries
                    restrictions.putBoolean(ACCOUNT_SETTINGS_ONLY_PROVISION, true)
                }

                ProvisioningScope.Startup -> {
                    entries = provisioningEntries
                    restrictions.putBoolean(ACCOUNT_SETTINGS_ONLY_PROVISION, true)
                }

                ProvisioningScope.InitializedEngine -> {
                    entries = restrictionsManager.manifestRestrictions
                        .filter { it.key in INITIALIZED_ENGINE_RESTRICTIONS }
                }

                ProvisioningScope.AllAccountSettings -> {
                    entries = restrictionsManager.manifestRestrictions.filter {
                        it.key == RESTRICTION_PLANCK_ACCOUNTS_SETTINGS
                    }
                }

                ProvisioningScope.AllSettings -> {
                    entries = restrictionsManager.manifestRestrictions
                }

                is ProvisioningScope.SingleAccountSettings -> {
                    entries = restrictionsManager.manifestRestrictions.filter {
                        it.key == RESTRICTION_PLANCK_ACCOUNTS_SETTINGS
                    }
                    restrictions.getParcelableArray(
                        RESTRICTION_PLANCK_ACCOUNTS_SETTINGS
                    )?.filter {
                        (it as Bundle).getBundle(RESTRICTION_ACCOUNT_MAIL_SETTINGS)?.getString(
                            RESTRICTION_ACCOUNT_EMAIL_ADDRESS) == provisioningScope.email
                    }.also { restrictions.putParcelableArray(RESTRICTION_PLANCK_ACCOUNTS_SETTINGS, it?.toTypedArray()) }
                }
            }

            mapRestrictions(entries, restrictions)
            saveAppSettings()
            saveAccounts()
        }
    }

    private val provisioningEntries
        get() = restrictionsManager.manifestRestrictions
            // ignore media keys from MDM before PlanckProvider has been initialized
            .filter { it.key in PROVISIONING_RESTRICTIONS }
            .modifyItems( // filter account settings needed
                findItem = { it.key == RESTRICTION_PLANCK_ACCOUNTS_SETTINGS }
            ) { item ->
                item.apply {
                    this.restrictions.first().apply {
                        this.restrictions = this.restrictions.filter {
                            it.key in ACCOUNT_PROVISIONING_RESTRICTIONS
                        }.toTypedArray()
                    }
                }
            }

    private fun isProvisionAvailable(restrictions: Bundle): Boolean {
        return restrictions.keySet().containsAll(
            setOf(
                RESTRICTION_PLANCK_ACCOUNTS_SETTINGS,
            )
        ) && (restrictions.getParcelableArray(RESTRICTION_PLANCK_ACCOUNTS_SETTINGS)
            ?.firstOrNull() as? Bundle)
            ?.containsKey(RESTRICTION_ACCOUNT_MAIL_SETTINGS) ?: false
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

    private fun sendRemoteConfig() {
        restrictionsUpdatedMF.value = restrictionsUpdatedMF.value + 1
    }
}