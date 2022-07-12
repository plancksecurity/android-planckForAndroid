package security.pEp.mdm

import android.content.RestrictionEntry
import android.os.Bundle
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import timber.log.Timber

class ConfiguredSettingsUpdater(
    private val k9: K9,
    private val preferences: Preferences,
) {

    fun update(restrictions: Bundle, entry: RestrictionEntry) {
        when (val key = entry.key) {
            RESTRICTION_PEP_EXTRA_KEYS ->
                saveExtrasKeys(restrictions, key)
            RESTRICTION_PEP_USE_TRUSTWORDS ->
                saveUseTrustwords(restrictions, key)
            RESTRICTION_PEP_UNSECURE_DELIVERY_WARNING ->
                saveUnsecureDeliveryWarning(restrictions, key)
            RESTRICTION_PEP_SYNC_FOLDER ->
                savepEpSyncFolder(restrictions, entry.key)
            RESTRICTION_PEP_DEBUG_LOG ->
                savepEpDebugLog(restrictions, entry.key)
            RESTRICTION_PEP_ENABLE_PRIVACY_PROTECTION ->
                savePrivacyProtection(restrictions, key)
        }
    }

    private fun saveUseTrustwords(restrictions: Bundle, key: String) {
        updateBoolean(restrictions, key) {
            K9.setpEpUseTrustwords(it)
        }
    }

    private fun saveUnsecureDeliveryWarning(restrictions: Bundle, key: String) {
        updateBoolean(restrictions, key) {
            k9.setpEpForwardWarningEnabled(it)
        }
    }

    private fun savepEpSyncFolder(restrictions: Bundle, key: String) {
        updateBoolean(restrictions, key) {
            K9.setUsingpEpSyncFolder(it)
        }
    }

    private fun savepEpDebugLog(restrictions: Bundle, key: String) {
        updateBoolean(restrictions, key) {
            K9.setDebug(it)
        }
    }

    private fun saveExtrasKeys(restrictions: Bundle, key: String) {
        kotlin.runCatching {
            val parcelableArray = restrictions.getParcelableArray(key)
            parcelableArray?.mapNotNull { (it as Bundle).getString(RESTRICTION_PEP_FINGERPRINT) }
                ?.filter { it.isNotBlank() }?.toSet()?.also {
                    K9.setMasterKeys(it)
                }
        }
    }

    private fun savePrivacyProtection(restrictions: Bundle, key: String) {
        updateAccountBoolean(
            restrictions,
            key
        ) { account, newValue ->
            account.setpEpPrivacyProtection(newValue)
        }
    }

    private inline fun updateBoolean(
        restrictions: Bundle,
        key: String,
        crossinline block: (newValue: Boolean) -> Unit
    ) {
        kotlin.runCatching {
            val newValue = restrictions.getBoolean(key)
            block(newValue)
        }.onFailure { Timber.e(it) }
    }

    private inline fun updateAccountString(
        restrictions: Bundle,
        key: String,
        crossinline block: (account: Account, newValue: String) -> Unit
    ) {
        kotlin.runCatching {
            val newValue = restrictions.getString(key)
            if (!newValue.isNullOrBlank()) {
                preferences.accounts.forEach { account ->
                    block(account, newValue)
                }
            }
        }.onFailure { Timber.e(it) }
    }

    private inline fun updateAccountBoolean(
        restrictions: Bundle,
        key: String,
        crossinline block: (account: Account, newValue: Boolean) -> Unit
    ) {
        kotlin.runCatching {
            val newValue = restrictions.getBoolean(key)
            preferences.accounts.forEach { account ->
                block(account, newValue)
            }
        }.onFailure { Timber.e(it) }
    }
}
