package security.planck.provisioning

import com.fsck.k9.auth.OAuthProviderType
import security.planck.network.UrlChecker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProvisioningSettings @Inject constructor() {
    var provisioningUrl: String? = null
    var accountsProvisionList = mutableListOf<AccountProvisioningSettings>()

    fun hasValidMailSettings(urlChecker: UrlChecker): Boolean =
        accountsProvisionList.firstOrNull()?.let { accountProvision ->
            accountProvision.email?.isValidEmailAddress() == true &&
                    accountProvision.provisionedMailSettings?.isValidForProvision(urlChecker) == true
        } ?: false

    fun getAccountSettingsByAddress(address: String): AccountProvisioningSettings? =
        accountsProvisionList.find { it.email == address }

    fun modifyOrAddAccountSettingsByAddress(address: String, change: (AccountProvisioningSettings) -> Unit) {
        accountsProvisionList.find { it.email == address }?.let(change)
            ?:let { accountsProvisionList.add(AccountProvisioningSettings().also(change)) }
    }
}

data class AccountProvisioningSettings(
    var senderName: String? = null,
    var accountDescription: String? = null,
    var email: String? = null,
    var oAuthType: OAuthProviderType? = null,
    var provisionedMailSettings: AccountMailSettingsProvision? = null
)
