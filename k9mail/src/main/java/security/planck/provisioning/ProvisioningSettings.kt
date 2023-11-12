package security.planck.provisioning

import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.auth.OAuthProviderType
import security.planck.network.UrlChecker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProvisioningSettings @Inject constructor(
    private val preferences: Preferences,
    private val urlChecker: UrlChecker,
) {
    var provisioningUrl: String? = null
    private val accountsProvisionMutableList = mutableListOf<AccountProvisioningSettings>()
    val accountsProvisionList: List<AccountProvisioningSettings> = accountsProvisionMutableList

    fun hasValidMailSettings(): Boolean =
        accountsProvisionList.firstOrNull()?.isValid(urlChecker) ?: false

    fun hasAnyAccountWithWrongSettings(): Boolean =
        accountsProvisionList.any { !it.isValid(urlChecker) }

    fun getAccountSettingsByAddress(address: String): AccountProvisioningSettings? =
        accountsProvisionList.find { it.email == address }

    fun removeAccountSettingsByAddress(address: String) {
        accountsProvisionMutableList.removeIf { it.email == address }
    }

    fun modifyOrAddAccountSettingsByAddress(
        address: String,
        change: (AccountProvisioningSettings) -> Unit
    ) {
        accountsProvisionList.find { it.email == address }?.let(change)
            ?: let {
                accountsProvisionMutableList.add(
                    AccountProvisioningSettings(email = address).also(
                        change
                    )
                )
            }
    }

    fun findNextAccountToInstall(): AccountProvisioningSettings? =
        accountsProvisionList.filter { it.isValid(urlChecker) }.firstOrNull {
            it.email !in preferences.accounts.map { account -> account.email }
        }

    fun findAccountsToRemove(): List<Account> =
        preferences.accountsAllowingIncomplete.filter { account ->
            account.email != null && accountsProvisionList.none { it.email == account.email }
        }

    fun purgeAccountsNotInstalledOrInstalling() {
        accountsProvisionMutableList.removeIf { setting ->
            preferences.accountsAllowingIncomplete.none { it.email == setting.email }
        }
    }

    fun purgeAccountsNotInRestrictions(newEmailAddresses: List<String>) {
        accountsProvisionMutableList.removeIf {
            it.email !in newEmailAddresses
        }
    }
}

data class AccountProvisioningSettings(
    val email: String,
    var senderName: String? = null,
    var accountDescription: String? = null,
    var oAuthType: OAuthProviderType? = null,
    var provisionedMailSettings: AccountMailSettingsProvision? = null
) {
    /**
     * isValid
     *
     * Check for validity of these settings.
     * Note that the email address regex is **not** checked because no settings are
     * created with an invalid address.
     *
     * @return true if valid, false otherwise.
     */
    fun isValid(urlChecker: UrlChecker): Boolean =
        provisionedMailSettings?.isValidForProvision(urlChecker) ?: false
}
