package security.planck.mdm

import android.content.RestrictionEntry
import android.os.Bundle
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.planck.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import security.planck.provisioning.ProvisioningScope
import security.planck.provisioning.ProvisioningSettings
import security.planck.provisioning.findAccountsToRemove
import security.planck.provisioning.isValidEmailAddress
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
    private var wrongAccountSettingsWarningDone = false

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
            mapRestrictions(
                provisioningScope.manifestEntryFilter(restrictionsManager.manifestRestrictions),
                restrictionsManager.applicationRestrictions
                    .apply { provisioningScope.restrictionFilter(this) },
                provisioningScope.allowModifyAccountProvisioningSettings,
                provisioningScope.purgeAccountSettings
            )
            saveAppSettings()
            saveAccounts()
        }.onSuccess {
            if (shouldActOnAccountsRemoved(provisioningScope)) {
                if (k9.isRunningInForeground) {
                    accountRemovedMF.value = true
                } else {
                    exitProcess(0) // if running in background just exit the app to remove accounts.
                }
            }

            if (!wrongAccountSettingsWarningDone && shouldWarnWrongAccountSettings()) {
                wrongAccountSettingsWarningDone = true
                wrongAccountSettingsMF.value = true // only set once and seen if app in foreground.
            }
        }
    }

    private fun shouldWarnWrongAccountSettings(): Boolean =
    // Inform that there are some MDM accounts that cannot be setup, since they dont have right email.
        // This is done mainly because we don't have yet feedback to MDM implemented.
        newMailAddressesIncludingFailures.any { it == null } ||
                // Here we are checking for wrong settings that are not fatally wrong, since they
                // arrived to the provisioning settings.
                // This means the case of an account not yet set on the device with wrong settings,
                // which is actually a stopper for this account setup.
                provisioningSettings.hasAnyAccountWithWrongSettings()

    private val newMailAddressesIncludingFailures: List<String?> =
        restrictionsManager.applicationRestrictions.getParcelableArray(
            RESTRICTION_PLANCK_ACCOUNTS_SETTINGS
        )?.map {
            (it as Bundle).getBundle(RESTRICTION_ACCOUNT_MAIL_SETTINGS)
                ?.getString(RESTRICTION_ACCOUNT_EMAIL_ADDRESS)
                ?.takeIf { email -> email.isValidEmailAddress() }
        }.orEmpty()

    private fun shouldActOnAccountsRemoved(provisioningScope: ProvisioningScope) =
        provisioningSettings.findAccountsToRemove(preferences).isNotEmpty()
                && !provisioningScope.isStartup

    private fun mapRestrictions(
        entries: List<RestrictionEntry>,
        restrictions: Bundle,
        allowModifyAccountProvisioningSettings: Boolean,
        purgeAccountSettings: Boolean,
    ) {
        entries.forEach { entry ->
            settingsUpdater.update(
                restrictions,
                entry,
                allowModifyAccountProvisioningSettings,
                purgeAccountSettings
            )
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

    fun resetWrongAccountSettingsWarning() {
        wrongAccountSettingsMF.value = false
    }
}