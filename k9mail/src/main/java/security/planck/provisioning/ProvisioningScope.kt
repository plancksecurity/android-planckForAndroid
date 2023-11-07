package security.planck.provisioning

sealed interface ProvisioningScope {
    object FirstStartup: ProvisioningScope
    object Startup: ProvisioningScope
    object InitializedEngine: ProvisioningScope
    object AllSettings: ProvisioningScope
    object AllAccountSettings: ProvisioningScope
    data class SingleAccountSettings(val email: String): ProvisioningScope
}
