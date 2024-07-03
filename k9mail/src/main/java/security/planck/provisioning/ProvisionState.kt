package security.planck.provisioning

sealed class ProvisionState {
    object WaitingForProvisioning: ProvisionState()
    object InProvisioning: ProvisionState()
    object WaitingToInitialize: ProvisionState()
    data class Initializing(val provisioned: Boolean = false): ProvisionState()
    object Initialized: ProvisionState()
    data class Error(val throwable: Throwable): ProvisionState()
}
