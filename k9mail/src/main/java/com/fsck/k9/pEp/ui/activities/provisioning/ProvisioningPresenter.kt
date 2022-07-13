package com.fsck.k9.pEp.ui.activities.provisioning

import security.pEp.enterprise.provisioning.ProvisionState
import security.pEp.enterprise.provisioning.ProvisioningManager
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

    private fun displayProvisionState() {
        when(val state = provisioningManager.provisionState) {
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
            is ProvisionState.Error ->
                view?.displayError(state.throwable.message ?: state.throwable.stackTraceToString())
        }
    }

    override fun provisionStateChanged(state: ProvisionState) {
        displayProvisionState()
    }
}