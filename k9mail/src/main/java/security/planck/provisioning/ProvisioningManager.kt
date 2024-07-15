package security.planck.provisioning

import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.fsck.k9.BuildConfig
import com.fsck.k9.Globals
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.helper.Utility
import com.fsck.k9.planck.DispatcherProvider
import com.fsck.k9.planck.infrastructure.extensions.flatMapSuspend
import com.fsck.k9.planck.infrastructure.extensions.mapError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import security.planck.file.PlanckSystemFileLocator
import security.planck.mdm.ConfigurationManager
import java.io.File
import java.io.InputStream
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
    private val stateMutableFlow: MutableStateFlow<ProvisionState> = MutableStateFlow(
        ProvisionState.WaitingToInitialize(shouldOfferRestore)
    )
    val state = stateMutableFlow.asStateFlow()

    private val areCoreDbsClear: Boolean
        get() = !fileLocator.keysDbFile.exists()
    private val shouldOfferRestore: Boolean
        get() = areCoreDbsClear

    fun startProvisioningBlockingIfPossible() {
        runBlocking(dispatcherProvider.planckDispatcher()) {
            // performPresetProvisioning() -> If Engine preset provisioning needed, do it here or at the beginning of next method.
            if (!shouldOfferRestore) {
                performProvisioningIfNeeded()
                    .onFailure {
                        Log.e("Provisioning Manager", "Error", it)
                        setProvisionState(ProvisionState.Error(it))
                    }
                    .onSuccess { setProvisionState(ProvisionState.Initialized) }
            }
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

    fun initializeApp() {
        startProvisioning()
    }

    suspend fun restoreData(documentFile: DocumentFile?) {
        withContext(dispatcherProvider.io()) {
            kotlin.runCatching {
                logInDebug("EFA-625", "SELECTED DOCUMENT FILE: ${documentFile?.uri}")
                documentFile?.listFiles()?.forEach { file ->
                    copyFileToInternalStorage(file.uri, file.name ?: "unknown")
                }
            }
        }.onSuccess {
            initializeApp()
        }.onFailure {
            stateMutableFlow.value = ProvisionState.DbImportFailed(it)
        }
    }

    private fun copyFileToInternalStorage(fileUri: Uri, fileName: String) {
        var outputFolder: File? = null
        if (fileName.startsWith(PlanckSystemFileLocator.KEYS_DB_FILE)
            || fileName.startsWith(PlanckSystemFileLocator.MANAGEMENT_DB_FILE)
            || fileName.startsWith(PlanckSystemFileLocator.LOG_DB_FILE)
        ) {
            outputFolder = fileLocator.pEpFolder
        } else if (fileName.startsWith(PlanckSystemFileLocator.SYSTEM_DB_FILE)) {
            outputFolder = fileLocator.trustwordsFolder
        } else {
            logInDebug("EFA-625", "IGNORED FILE: $fileName")
        }
        outputFolder?.let {
            val inputStream: InputStream? =
                Globals.getContext().contentResolver.openInputStream(fileUri)
            outputFolder.mkdirs()
            val outputFile = File(outputFolder, fileName)
            inputStream?.use { input ->
                outputFile.outputStream().use { output ->
                    logInDebug("EFA-625", "COPYING FILE $fileName TO ${outputFile.absolutePath}")
                    input.copyTo(output)
                }
            }
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

    private fun finalizeSetup(provisionDone: Boolean = false): Result<Unit> {
        setProvisionState(ProvisionState.Initializing(provisionDone))
        return kotlin.runCatching {
            k9.finalizeSetup()
        }.mapError {
            InitializationFailedException(it.message, it)
        }
    }

    private fun setProvisionState(newState: ProvisionState) {
        stateMutableFlow.value = newState
    }

    @Suppress("SameParameterValue")
    private fun logInDebug(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }
}
