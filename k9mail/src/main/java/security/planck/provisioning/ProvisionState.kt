package security.planck.provisioning

sealed class ProvisionState {
    object InProvisioning: ProvisionState()
    data class WaitingToInitialize(val offerRestore: Boolean): ProvisionState()
    data class DbImportFailed(val throwable: Throwable): ProvisionState()
    data class Initializing(val provisioned: Boolean = false): ProvisionState()
    object Initialized: ProvisionState()
    data class Error(val throwable: Throwable): ProvisionState()
}
