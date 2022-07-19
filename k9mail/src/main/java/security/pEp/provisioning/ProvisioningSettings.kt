package security.pEp.provisioning

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProvisioningSettings @Inject constructor() {
    var provisioningUrl: String? = null
    var senderName: String? = null
    var accountDescription: String? = null
    var email: String? = null
    var provisionedMailSettings: AccountMailSettingsProvision? = null
}
