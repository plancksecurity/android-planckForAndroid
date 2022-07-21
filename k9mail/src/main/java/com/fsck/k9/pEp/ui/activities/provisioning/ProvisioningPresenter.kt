package com.fsck.k9.pEp.ui.activities.provisioning

import security.pEp.provisioning.InitializationFailedException
import security.pEp.provisioning.ProvisionState
import security.pEp.provisioning.ProvisioningFailedException
import security.pEp.provisioning.ProvisioningManager
import javax.inject.Inject

class ProvisioningPresenter @Inject constructor(
    private val provisioningManager: ProvisioningManager
) : ProvisioningManager.ProvisioningStateListener {
    private var view: ProvisioningView? = null

    fun attach(view: ProvisioningView) {
        this.view = view
        provisioningManager.addListener(this)
    }

    fun detach() {
        this.view = null
        provisioningManager.removeListener(this)
    }

    private fun displayProvisionState(state: ProvisionState) {
        when(state) {
            is ProvisionState.WaitingForProvisioning ->
                view?.waitingForProvisioning()
            is ProvisionState.InProvisioning ->
                view?.provisioningProgress()
            is ProvisionState.Initializing ->
                if (state.provisioned) {
                    view?.initializingAfterSuccessfulProvision()
                } else {
                    view?.initializing()
                }
            is ProvisionState.Initialized ->
                view?.initialized()
            is ProvisionState.Error -> {
                val throwableMessage = state.throwable.message
                val message =
                    if (throwableMessage.isNullOrBlank())
                        state.throwable.stackTraceToString()
                    else throwableMessage
                if (state.throwable is ProvisioningFailedException) {
                    view?.displayProvisioningError(message)
                } else {
                    view?.displayInitializationError(message)
                }
            }
        }
    }

    override fun provisionStateChanged(state: ProvisionState) {
        displayProvisionState(state)
    }
}
