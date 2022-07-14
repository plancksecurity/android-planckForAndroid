package security.pEp.provisioning

import android.webkit.URLUtil
import com.fsck.k9.BuildConfig
import com.fsck.k9.K9
import com.fsck.k9.pEp.DispatcherProvider
import com.fsck.k9.pEp.PEpProviderImplKotlin
import com.fsck.k9.pEp.infrastructure.extensions.flatMapSuspend
import com.fsck.k9.pEp.infrastructure.extensions.mapError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import security.pEp.file.PEpSystemFileLocator
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
        CoroutineScope(dispatcherProvider.main()).launch {
            if (!BuildConfig.IS_ENTERPRISE) {
                finalizeSetup()
            } else {
                val provisioningUrl = k9.provisioningUrl
                if (provisioningUrl != null && !systemFileLocator.keysDbFile.exists()) {
                    if (!urlChecker.isValidUrl(provisioningUrl)) {
                        Result.failure(
                            IllegalStateException("Url has bad format: $provisioningUrl")
                        )
                    } else {
                        setProvisionState(ProvisionState.InProvisioning)
                        PEpProviderImplKotlin.provision(
                            dispatcherProvider.io(),
                            provisioningUrl
                        ).flatMapSuspend {
                            finalizeSetup(true)
                        }
                    }
                } else {
                    finalizeSetup()
                }
            }.onFailure {
                setProvisionState(ProvisionState.Error(it))
            }.onSuccess { setProvisionState(ProvisionState.Initialized) }
        }
    }

    private suspend fun finalizeSetup(provisionDone: Boolean = false): Result<Unit> {
        setProvisionState(ProvisionState.Initializing(provisionDone))
        val result: Result<Unit> = withContext(dispatcherProvider.io()) {
            kotlin.runCatching {
                k9.finalizeSetup()
            }.mapError {
                InitializationFailedException(it.message, it)
            }
        }
        return result
    }

    private fun setProvisionState(newState: ProvisionState) {
        provisionState = newState
        listeners.forEach { it.provisionStateChanged(newState) }
    }

    interface ProvisioningStateListener {
        fun provisionStateChanged(state: ProvisionState)
    }
}

class UrlChecker @Inject constructor() {
    fun isValidUrl(url: String?): Boolean = URLUtil.isValidUrl(url)
}
