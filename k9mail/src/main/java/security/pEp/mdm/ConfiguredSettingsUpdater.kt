package security.pEp.mdm

import android.content.RestrictionEntry
import android.os.Bundle
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences

class ConfiguredSettingsUpdater(
    private val k9: K9,
    private val preferences: Preferences,
) {
    var accountSettingsChanged = false
        private set
    var appSettingsChanged = false
        private set

    fun update(restrictions: Bundle, entry: RestrictionEntry) {

    }

    fun saveUseTrustwords(restrictions: Bundle, entry: RestrictionEntry) {
        saveConfiguredSetting(restrictions, entry, K9.isUsingTrustwords()) {
            K9.setpEpUseTrustwords(it)
        }
    }

    fun saveUnsecureDeliveryWarning(restrictions: Bundle, entry: RestrictionEntry) {
        saveConfiguredSetting(restrictions, entry, K9.ispEpForwardWarningEnabled()) {
            k9.setpEpForwardWarningEnabled(it.value)
        }
    }

    fun savepEpSyncFolder(restrictions: Bundle, entry: RestrictionEntry) {
        saveConfiguredSetting(restrictions, entry, K9.isUsingpEpSyncFolder()) {
            K9.setUsingpEpSyncFolder(it.value)
        }
    }

    fun savepEpDebugLog(restrictions: Bundle, entry: RestrictionEntry) {
        saveConfiguredSetting(restrictions, entry, K9.isDebug()) {
            K9.setDebug(it.value)
        }
    }

    fun saveExtrasKeys(restrictions: Bundle, entry: RestrictionEntry) {
        saveConfiguredSetting(
            restrictions,
            entry,
            K9.getMasterKeys().map { ExtraKey(it) }
        ) { newExtraKeys ->
            val currentKeys = K9.getMasterKeys().toSet()
            newExtraKeys.value.forEach { extraKey ->
                currentKeys.plus(extraKey.fpr)
            }
            K9.setMasterKeys(currentKeys)
        }
    }

    fun savePrivacyProtection(restrictions: Bundle, entry: RestrictionEntry) {
        saveAccountConfiguredSetting(
            restrictions,
            entry,
            { account -> account.ispEpPrivacyProtected() }
        ) { account, config ->
            account.setpEpPrivacyProtection(config)
        }
    }

    private inline fun <reified T> saveConfiguredSetting(
        restrictions: Bundle,
        entry: RestrictionEntry,
        oldValue: T,
        block: (config: ManageableSetting<T>) -> Unit
    ) {
        val value = restrictions.getString(entry.key)
        value?.let {
            val config = AppConfigEntry(entry.key, value).getValue<T>()?.toManageableSetting()
            config?.let {
                block(it)
                if (oldValue != it.value) {
                    appSettingsChanged = true
                }
            }
        }
    }

    private inline fun <reified T> saveAccountConfiguredSetting(
        restrictions: Bundle,
        entry: RestrictionEntry,
        oldValue: (Account) -> T,
        block: (account: Account, config: ManageableSetting<T>) -> Unit
    ) {
        val value = restrictions.getString(entry.key)
        value?.let {
            val config = AppConfigEntry(entry.key, value).getValue<T>()?.toManageableSetting()
            config?.let {
                preferences.accounts.forEach { account ->
                    block(account, config)
                    if (oldValue(account) != config.value) {
                        accountSettingsChanged = true
                    }
                }
            }
        }
    }
}
