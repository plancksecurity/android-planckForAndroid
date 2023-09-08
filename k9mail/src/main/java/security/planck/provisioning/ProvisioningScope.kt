package security.planck.provisioning

sealed interface ProvisioningScope {
    data class Startup(val firstStartup: Boolean): ProvisioningScope
    object InitializedEngine: ProvisioningScope
    object AllSettings: ProvisioningScope
    object AllAccountSettings: ProvisioningScope
}
