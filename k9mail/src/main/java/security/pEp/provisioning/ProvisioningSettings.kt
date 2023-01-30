package security.pEp.provisioning

import security.pEp.network.UrlChecker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProvisioningSettings @Inject constructor() {
    var provisioningUrl: String? = null
    var senderName: String? = null
    var accountDescription: String? = null
    var email: String? = null
    var oAuthType: com.fsck.k9.auth.OAuthProviderType? = null
    var provisionedMailSettings: AccountMailSettingsProvision? = null

    fun hasValidMailSettings(urlChecker: UrlChecker): Boolean =
        email?.isValidEmailAddress() == true &&
                provisionedMailSettings?.isValidForProvision(urlChecker) == true
}
