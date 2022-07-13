package com.fsck.k9.pEp.ui.activities.provisioning

interface ProvisioningView {
    fun waitingForProvisioning()
    fun provisioningProgress()
    fun initializing()
    fun initializingAfterSuccessfulProvision()
    fun initialized()
    fun displayError(message: String)
    fun displayUnknownError(trace: String)
}