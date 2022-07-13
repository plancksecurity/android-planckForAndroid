package security.pEp.enterprise.provisioning

import android.content.Context
import android.util.Log
import com.fsck.k9.BuildConfig
import com.fsck.k9.K9
import com.fsck.k9.pEp.PEpProviderImplKotlin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ProvisioningManager(
    context: Context
) {
    private val k9 = context as K9
    var provisionState: ProvisionState =
        if (BuildConfig.IS_ENTERPRISE) ProvisionState.WaitingForProvisioning
        else ProvisionState.Initializing()
        private set

    private val listeners = mutableListOf<ProvisioningStateListener>()

    fun addListener(listener: ProvisioningStateListener) {
        listeners.add(listener)
        listener.provisionStateChanged(provisionState)
    }

    fun removeListener(listener: ProvisioningStateListener) {
        listeners.remove(listener)
    }

    fun startProvisioning() {
        if (provisionState != ProvisionState.WaitingForProvisioning) return
        val provisioningUrl = k9.provisioningUrl
        CoroutineScope(Dispatchers.Main).launch {
            if (!BuildConfig.IS_ENTERPRISE) {
                finalizeSetup()
            } else {
                val homeDir: File = k9.getDir("home", Context.MODE_PRIVATE)
                val keysDb = File(homeDir, ".pEp" + File.separator + "keys.db")
                if (provisioningUrl != null && !keysDb.exists()) {
                    setProvisionState(ProvisionState.InProvisioning)
                    PEpProviderImplKotlin.provision(provisioningUrl).fold(
                        onSuccess = {
                            finalizeSetup(true)
                        },
                        onFailure = {
                            setProvisionState(ProvisionState.Error(it))
                        }
                    )
                } else {
                    finalizeSetup()
                }
            }
        }
    }

    private suspend fun finalizeSetup(provisionDone: Boolean = false) {
        setProvisionState(ProvisionState.Initializing(provisionDone))
        withContext(Dispatchers.IO) {
            k9.finalizeSetup()
        }
        setProvisionState(ProvisionState.Initialized)
    }

    private fun setProvisionState(newState: ProvisionState) {
        provisionState = newState
        listeners.forEach { it.provisionStateChanged(newState) }
    }

    interface ProvisioningStateListener {
        fun provisionStateChanged(state: ProvisionState)
    }
}
