package security.pEp.provisioning

sealed class ProvisionState {
    object WaitingForProvisioning: ProvisionState()
    object InProvisioning: ProvisionState()
    data class Initializing(val provisioned: Boolean = false): ProvisionState()
    object Initialized: ProvisionState()
    data class Error(val throwable: Throwable): ProvisionState()
}
