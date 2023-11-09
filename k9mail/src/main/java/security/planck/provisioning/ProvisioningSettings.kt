package security.planck.provisioning

import com.fsck.k9.auth.OAuthProviderType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProvisioningSettings @Inject constructor() {
    var provisioningUrl: String? = null
    var accountsProvisionList = mutableListOf<AccountProvisioningSettings>()

    fun hasValidMailSettings(): Boolean =
        accountsProvisionList.firstOrNull()?.isValid() ?: false

    fun hasAnyAccountWithWrongSettings(): Boolean =
        accountsProvisionList.any { !it.isValid() }

    fun getAccountSettingsByAddress(address: String): AccountProvisioningSettings? =
        accountsProvisionList.find { it.email == address }

    fun removeAccountSettingsByAddress(address: String) {
        accountsProvisionList.removeIf { it.email == address }
    }

    fun modifyOrAddAccountSettingsByAddress(
        address: String,
        change: (AccountProvisioningSettings) -> Unit
    ) {
        accountsProvisionList.find { it.email == address }?.let(change)
            ?: let {
                accountsProvisionList.add(
                    AccountProvisioningSettings(email = address).also(
                        change
                    )
                )
            }
    }
}

data class AccountProvisioningSettings(
    var senderName: String? = null,
    var accountDescription: String? = null,
    var email: String? = null,
    var oAuthType: OAuthProviderType? = null,
    var provisionedMailSettings: AccountMailSettingsProvision? = null
) {
    fun isValid(): Boolean = email?.isValidEmailAddress() == true &&
            provisionedMailSettings?.isValidForProvision() == true
}
