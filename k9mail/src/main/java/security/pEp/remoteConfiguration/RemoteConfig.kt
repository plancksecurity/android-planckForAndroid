package security.pEp.remoteConfiguration

data class RemoteConfig(val pepEnablePrivacyProtection: Boolean, val isPepEnablePrivacyProtectionManaged: Boolean)

class RemoteConfigBuilder {
    private var pepEnablePrivacyProtection: Boolean = true
    private var isPepEnablePrivacyProtectionManaged: Boolean = false

    fun pepEnablePrivacyProtection(pepEnablePrivacyProtection: Boolean) = apply {
        this.pepEnablePrivacyProtection = pepEnablePrivacyProtection
        this.isPepEnablePrivacyProtectionManaged = true

    }

    fun build() = RemoteConfig(pepEnablePrivacyProtection, isPepEnablePrivacyProtectionManaged)
}