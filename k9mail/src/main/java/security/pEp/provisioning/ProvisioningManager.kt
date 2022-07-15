package security.pEp.provisioning

import com.fsck.k9.BuildConfig
import com.fsck.k9.K9
import com.fsck.k9.helper.Utility
import com.fsck.k9.pEp.DispatcherProvider
import com.fsck.k9.pEp.PEpProviderImplKotlin
import com.fsck.k9.pEp.infrastructure.extensions.flatMapSuspend
import com.fsck.k9.pEp.infrastructure.extensions.mapError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import security.pEp.file.PEpSystemFileLocator
import security.pEp.network.UrlChecker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProvisioningManager @Inject constructor(
    private val k9: K9,
    private val systemFileLocator: PEpSystemFileLocator,
    private val urlChecker: UrlChecker,
    private val dispatcherProvider: DispatcherProvider,
) {
    private var provisionState: ProvisionState =
        if (BuildConfig.IS_ENTERPRISE) ProvisionState.WaitingForProvisioning
        else ProvisionState.Initializing()

    private val listeners = mutableListOf<ProvisioningStateListener>()

    fun addListener(listener: ProvisioningStateListener) {
        listeners.add(listener)
        listener.provisionStateChanged(provisionState)
    }

    fun removeListener(listener: ProvisioningStateListener) {
        listeners.remove(listener)
    }

    fun startProvisioning() {
        CoroutineScope(dispatcherProvider.io()).launch {
            if (!BuildConfig.IS_ENTERPRISE) {
                finalizeSetup()
            } else {
                val provisioningUrl = k9.provisioningUrl
                if (provisioningUrl != null && !systemFileLocator.keysDbFile.exists()) {
                    performProvisioningAfterChecks(provisioningUrl)
                } else {
                    finalizeSetup()
                }
            }.onFailure {
                setProvisionState(ProvisionState.Error(it))
            }.onSuccess { setProvisionState(ProvisionState.Initialized) }
        }
    }

    private suspend fun performProvisioningAfterChecks(provisioningUrl: String): Result<Unit> =
        when {
            !isDeviceOnline() -> {
                Result.failure(ProvisioningFailedException("Device is offline"))
            }
            !urlChecker.isUrlReachable(provisioningUrl) -> {
                Result.failure(
                    ProvisioningFailedException(
                        "Provisioning url $provisioningUrl is not reachable"
                    )
                )
            }
            !urlChecker.isValidUrl(provisioningUrl) -> {
                Result.failure(
                    IllegalStateException("Url has bad format: $provisioningUrl")
                )
            }
            else -> {
                setProvisionState(ProvisionState.InProvisioning)
                PEpProviderImplKotlin.provision(
                    dispatcherProvider.io(),
                    provisioningUrl
                ).flatMapSuspend {
                    finalizeSetup(true)
                }
            }
        }

    private fun isDeviceOnline(): Boolean =
        kotlin.runCatching { Utility.hasConnectivity(k9) }.getOrDefault(false)

    private suspend fun finalizeSetup(provisionDone: Boolean = false): Result<Unit> {
        setProvisionState(ProvisionState.Initializing(provisionDone))
        val result: Result<Unit> = kotlin.runCatching {
            k9.finalizeSetup()
        }.mapError {
            InitializationFailedException(it.message, it)
        }
        return result
    }

    private suspend fun setProvisionState(newState: ProvisionState) {
        provisionState = newState
        withContext(dispatcherProvider.main()) {
            listeners.forEach { it.provisionStateChanged(newState) }
        }
    }

    interface ProvisioningStateListener {
        fun provisionStateChanged(state: ProvisionState)
    }
}

