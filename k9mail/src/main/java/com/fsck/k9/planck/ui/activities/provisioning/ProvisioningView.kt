package com.fsck.k9.planck.ui.activities.provisioning

interface ProvisioningView {
    fun provisioningProgress()
    fun initializing()
    fun initializingAfterSuccessfulProvision()
    fun initialized()
    fun displayProvisioningError(message: String)
    fun displayInitializationError(message: String)
    fun displayUnknownError(trace: String)
    fun displayDbImportFailed(message: String)
    fun offerRestorePlanckData()
}