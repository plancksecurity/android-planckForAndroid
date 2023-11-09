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
import security.planck.provisioning.ProvisioningSettings
import security.planck.provisioning.findAccountsToRemove
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.exitProcess

@Singleton
class ConfigurationManager @Inject constructor(
    private val preferences: Preferences,
    private val restrictionsManager: RestrictionsProvider,
    private val settingsUpdater: ConfiguredSettingsUpdater,
    private val provisioningSettings: ProvisioningSettings,
    private val k9: K9,
    private val dispatcherProvider: DispatcherProvider,
) {

    private val restrictionsUpdatedMF: MutableStateFlow<Int> = MutableStateFlow(0)
    val restrictionsUpdatedFlow = restrictionsUpdatedMF.asStateFlow()

    private val accountRemovedMF: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val accountRemovedFlow = accountRemovedMF.asStateFlow()

    private val wrongAccountSettingsMF: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val wrongAccountSettingsFlow = wrongAccountSettingsMF.asStateFlow()

    fun loadConfigurations() {
        CoroutineScope(Dispatchers.Main).launch {
            loadConfigurationsSuspend()
                .onSuccess {
                    sendRemoteConfig()
                }.onFailure {
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
            var allowModifyAccountProvisioningSettings = true
            when (provisioningScope) {
                ProvisioningScope.FirstStartup -> {
                    if (!isProvisionAvailable(restrictions)) {
                        throw ProvisioningFailedException("Provisioning data is missing")
                    }
                    entries = provisioningManifestEntries
                    allowModifyAccountProvisioningSettings = false
                }

                ProvisioningScope.Startup -> {
                    entries = provisioningManifestEntries
                    allowModifyAccountProvisioningSettings = false
                }

                ProvisioningScope.InitializedEngine -> {
                    entries = initializedEngineManifestEntries
                }

                ProvisioningScope.AllAccountSettings -> {
                    entries = accountsManifestEntries
                }

                ProvisioningScope.AllSettings -> {
                    entries = restrictionsManager.manifestRestrictions
                }

                is ProvisioningScope.SingleAccountSettings -> {
                    entries = accountsManifestEntries
                    filterAccountsRestrictionsToSingleAccount(restrictions, provisioningScope.email)
                }
            }

            mapRestrictions(entries, restrictions, allowModifyAccountProvisioningSettings)
            saveAppSettings()
            saveAccounts()
        }.onSuccess {
            if (shouldActOnAccountsRemoved(provisioningScope)) {
                if (k9.isRunningInForeground) {
                    accountRemovedMF.value = true
                } else {
                    exitProcess(0)
                }
            }
            if (provisioningSettings.hasAnyAccountWithWrongSettings()
                && !wrongAccountSettingsMF.value
            ) {
                wrongAccountSettingsMF.value = true
            }
        }
    }

    private fun shouldActOnAccountsRemoved(provisioningScope: ProvisioningScope) =
        provisioningSettings.findAccountsToRemove(preferences).isNotEmpty()
                && provisioningScope != ProvisioningScope.FirstStartup
                && provisioningScope != ProvisioningScope.Startup

    private val initializedEngineManifestEntries: List<RestrictionEntry>
        get() = restrictionsManager.manifestRestrictions
            .filter { it.key in INITIALIZED_ENGINE_RESTRICTIONS }

    private val accountsManifestEntries: List<RestrictionEntry>
        get() = restrictionsManager.manifestRestrictions.filter {
            it.key == RESTRICTION_PLANCK_ACCOUNTS_SETTINGS
        }

    private fun filterAccountsRestrictionsToSingleAccount(
        restrictions: Bundle,
        accountEmail: String,
    ) {
        restrictions.putParcelableArray(
            RESTRICTION_PLANCK_ACCOUNTS_SETTINGS,
            restrictions.getParcelableArray(
                RESTRICTION_PLANCK_ACCOUNTS_SETTINGS
            )?.filter {
                (it as Bundle).getBundle(RESTRICTION_ACCOUNT_MAIL_SETTINGS)?.getString(
                    RESTRICTION_ACCOUNT_EMAIL_ADDRESS
                ) == accountEmail
            }?.toTypedArray()
        )
    }

    private val provisioningManifestEntries: List<RestrictionEntry>
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
        allowModifyAccountProvisioningSettings: Boolean,
    ) {
        entries.forEach { entry ->
            settingsUpdater.update(restrictions, entry, allowModifyAccountProvisioningSettings)
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