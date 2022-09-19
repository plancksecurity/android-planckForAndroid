package security.pEp.provisioning

sealed class ProvisioningStage {
    class Startup(val firstStartup: Boolean): ProvisioningStage()
    object InitializedEngine: ProvisioningStage()
    object ProvisioningDone: ProvisioningStage()
}
