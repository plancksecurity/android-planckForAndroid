package security.planck.provisioning

import android.content.RestrictionEntry
import android.os.Bundle
import com.fsck.k9.planck.infrastructure.extensions.modifyItems
import security.planck.mdm.ACCOUNT_PROVISIONING_RESTRICTIONS
import security.planck.mdm.INITIALIZED_ENGINE_RESTRICTIONS
import security.planck.mdm.PROVISIONING_RESTRICTIONS
import security.planck.mdm.RESTRICTION_ACCOUNT_EMAIL_ADDRESS
import security.planck.mdm.RESTRICTION_ACCOUNT_MAIL_SETTINGS
import security.planck.mdm.RESTRICTION_PLANCK_ACCOUNTS_SETTINGS

sealed class ProvisioningScope {
    open val manifestEntryFilter: (List<RestrictionEntry>) -> List<RestrictionEntry> = { it }
    open val restrictionFilter: Bundle.() -> Unit = {}
    open val allowModifyAccountProvisioningSettings = true
    open val purgeAccountSettings = true

    val isStartup
        get() = this == FirstStartup || this == Startup

    object FirstStartup : ProvisioningScope() {
        override val manifestEntryFilter = ::getProvisioningManifestEntries
        override val restrictionFilter: Bundle.() -> Unit = {
            if (!isProvisionAvailable())
                throw ProvisioningFailedException("Provisioning data is missing")
        }
        override val allowModifyAccountProvisioningSettings: Boolean = false
    }

    object Startup : ProvisioningScope() {
        override val manifestEntryFilter = ::getProvisioningManifestEntries
        override val allowModifyAccountProvisioningSettings: Boolean = false
    }

    object InitializedEngine : ProvisioningScope() {
        override val manifestEntryFilter = ::getInitializedEngineManifestEntries
    }

    object AllSettings : ProvisioningScope()
    object AllAccountsSettings : ProvisioningScope() {
        override val manifestEntryFilter = ::getAccountsManifestEntries
    }

    data class SingleAccountSettings(val email: String) : ProvisioningScope() {
        override val manifestEntryFilter = ::getAccountsManifestEntries
        override val restrictionFilter: Bundle.() -> Unit =
            { filterAccountsRestrictionsToSingleAccount(email) }
        override val purgeAccountSettings: Boolean = false
    }

    protected fun getProvisioningManifestEntries(
        manifestEntries: List<RestrictionEntry>
    ): List<RestrictionEntry> = manifestEntries
        // ignore media keys from MDM before PlanckProvider has been initialized
        .filter { it.key in PROVISIONING_RESTRICTIONS }
        .modifyItems( // filter account settings needed
            findItem = { it.key == RESTRICTION_PLANCK_ACCOUNTS_SETTINGS }
        ) { item ->
            item.apply {
                this.restrictions.first().apply {
                    this.restrictions = this.restrictions.filter {
                        it.key in ACCOUNT_PROVISIONING_RESTRICTIONS
                    }.toTypedArray()
                }
            }
        }

    protected fun getInitializedEngineManifestEntries(
        manifestEntries: List<RestrictionEntry>
    ): List<RestrictionEntry> = manifestEntries
        .filter { it.key in INITIALIZED_ENGINE_RESTRICTIONS }

    protected fun getAccountsManifestEntries(
        manifestEntries: List<RestrictionEntry>
    ): List<RestrictionEntry> = manifestEntries.filter {
        it.key == RESTRICTION_PLANCK_ACCOUNTS_SETTINGS
    }

    protected fun Bundle.filterAccountsRestrictionsToSingleAccount(
        accountEmail: String,
    ) {
        putParcelableArray(
            RESTRICTION_PLANCK_ACCOUNTS_SETTINGS,
            getParcelableArray(
                RESTRICTION_PLANCK_ACCOUNTS_SETTINGS
            )?.filter {
                (it as Bundle).getBundle(RESTRICTION_ACCOUNT_MAIL_SETTINGS)?.getString(
                    RESTRICTION_ACCOUNT_EMAIL_ADDRESS
                ) == accountEmail
            }?.toTypedArray()
        )
    }

    protected fun Bundle.isProvisionAvailable(): Boolean {
        return keySet().containsAll(
            setOf(
                RESTRICTION_PLANCK_ACCOUNTS_SETTINGS,
            )
        ) && (getParcelableArray(RESTRICTION_PLANCK_ACCOUNTS_SETTINGS)
            ?.firstOrNull() as? Bundle)
            ?.containsKey(RESTRICTION_ACCOUNT_MAIL_SETTINGS) ?: false
    }
}
