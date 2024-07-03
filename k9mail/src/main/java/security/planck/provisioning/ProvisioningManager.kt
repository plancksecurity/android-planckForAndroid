package security.planck.provisioning

import android.util.Log
import com.fsck.k9.BuildConfig
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.helper.Utility
import com.fsck.k9.planck.DispatcherProvider
import com.fsck.k9.planck.infrastructure.extensions.flatMapSuspend
import com.fsck.k9.planck.infrastructure.extensions.mapError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import security.planck.file.PlanckSystemFileLocator
import security.planck.mdm.ConfigurationManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProvisioningManager @Inject constructor(
    private val k9: K9,
    private val preferences: Preferences,
    private val configurationManager: ConfigurationManager,
    private val fileLocator: PlanckSystemFileLocator,
    private val provisioningSettings: ProvisioningSettings,
    private val dispatcherProvider: DispatcherProvider,
) {
    private var provisionState: ProvisionState =
        if (k9.isRunningOnWorkProfile) ProvisionState.WaitingForProvisioning
        else ProvisionState.WaitingToInitialize(shouldOfferRestore)

    private val listeners = mutableListOf<ProvisioningStateListener>()

    private val areCoreDbsClear: Boolean
        get() = !fileLocator.keysDbFile.exists()
    private val shouldOfferRestore: Boolean
        get() = BuildConfig.IS_ENTERPRISE && !k9.isRunningOnWorkProfile && areCoreDbsClear

    fun addListener(listener: ProvisioningStateListener) {
        listeners.add(listener)
        listener.provisionStateChanged(provisionState)
    }

    fun removeListener(listener: ProvisioningStateListener) {
        listeners.remove(listener)
    }

    fun startProvisioningBlocking() {
        runBlocking(dispatcherProvider.planckDispatcher()) {
            // performPresetProvisioning() -> If Engine preset provisioning needed, do it here or at the beginning of next method.
            performProvisioningIfNeeded()
                .onFailure {
                    Log.e("Provisioning Manager", "Error", it)
                    setProvisionState(ProvisionState.Error(it))
                }
                .onSuccess { setProvisionState(ProvisionState.Initialized) }
        }
    }

    fun startProvisioning() {
        CoroutineScope(dispatcherProvider.planckDispatcher()).launch {
            // performPresetProvisioning() -> If Engine preset provisioning needed, do it here or at the beginning of next method.
            performProvisioningIfNeeded()
                .onFailure {
                    Log.e("Provisioning Manager", "Error", it)
                    setProvisionState(ProvisionState.Error(it))
                }
                .onSuccess { setProvisionState(ProvisionState.Initialized) }
        }
    }

    private suspend fun performProvisioningIfNeeded(): Result<Unit> {
        return when {
            !k9.isRunningOnWorkProfile -> {
                finalizeSetup()
            }

            else -> {
                configurationManager.loadConfigurationsSuspend( // TODO: Should we check if the device is online on every startup? If we are online the app is supposed to keep last restrictions it has so it should not be needed.
                    if (preferences.accounts.isEmpty()) ProvisioningScope.FirstStartup
                    else ProvisioningScope.Startup
                ).mapCatching {
                    removeAccountsRemovedFromMDM()
                }.flatMapSuspend {
                    finalizeSetupAfterChecks()
                }
            }
        }
    }

    private fun removeAccountsRemovedFromMDM() {
        provisioningSettings.findAccountsToRemove().forEach { account ->
            account.localStore.delete()
            preferences.deleteAccount(account)
            provisioningSettings.removeAccountSettingsByAddress(account.email)
        }
    }

    fun performInitializedEngineProvisioning() = runBlocking<Unit> {
        if (k9.isRunningOnWorkProfile) {
            configurationManager
                .loadConfigurationsSuspend(
                    ProvisioningScope.InitializedEngine
                )
                .onFailure { throw it }
        }
    }

    private suspend fun finalizeSetupAfterChecks(): Result<Unit> {
        return performChecksAfterProvisioning().flatMapSuspend {
            finalizeSetup(true)
        }
    }

    private fun performChecksAfterProvisioning(): Result<Unit> = when {
        !isDeviceOnline() -> {
            Result.failure(ProvisioningFailedException("Device is offline"))
        }

        areProvisionedMailSettingsInvalid() -> {
            Log.e(
                "MDM", "mail settings not valid: " +
                        "${provisioningSettings.accountsProvisionList.firstOrNull()?.provisionedMailSettings}"
            )
            Result.failure(
                ProvisioningFailedException(
                    "Provisioned mail settings are not valid"
                )
            )
        }

        else -> Result.success(Unit)
    }

    private fun areProvisionedMailSettingsInvalid(): Boolean {
        return !provisioningSettings.hasValidMailSettings()
    }

    private fun isDeviceOnline(): Boolean =
        kotlin.runCatching { Utility.hasConnectivity(k9) }.getOrDefault(false)

    private suspend fun finalizeSetup(provisionDone: Boolean = false): Result<Unit> {
        setProvisionState(ProvisionState.Initializing(provisionDone))
        return kotlin.runCatching {
            k9.finalizeSetup()
        }.mapError {
            InitializationFailedException(it.message, it)
        }
    }

    private suspend fun setProvisionState(newState: ProvisionState) {
        provisionState = newState
        listeners.forEach { it.provisionStateChanged(newState) }
    }

    interface ProvisioningStateListener {
        fun provisionStateChanged(state: ProvisionState)
    }
}
