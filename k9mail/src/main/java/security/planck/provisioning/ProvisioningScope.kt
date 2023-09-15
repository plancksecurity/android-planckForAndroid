package security.planck.provisioning

sealed interface ProvisioningScope {
    object FirstStartup: ProvisioningScope
    object InitializedEngine: ProvisioningScope
    object AllSettings: ProvisioningScope
    object AllAccountSettings: ProvisioningScope
}
