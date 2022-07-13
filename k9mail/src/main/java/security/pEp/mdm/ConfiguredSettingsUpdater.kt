package security.pEp.mdm

import android.content.RestrictionEntry
import android.os.Bundle
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import timber.log.Timber
import java.util.*

class ConfiguredSettingsUpdater(
    private val k9: K9,
    private val preferences: Preferences? = null
) {

    fun update(restrictions: Bundle, entry: RestrictionEntry) {
        when (val key = entry.key) {
            RESTRICTION_PROVISIONING_URL ->
                saveProvisioningUrl(restrictions, key)
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
            RESTRICTION_ACCOUNT_LOCAL_FOLDER_SIZE ->
                saveAccountLocalFolderSize(restrictions, key)
            RESTRICTION_ACCOUNT_MAX_PUSH_FOLDERS ->
                saveAccountMaxPushFolders(restrictions, key)
            RESTRICTION_ACCOUNT_COMPOSITION_DEFAULTS ->
                saveAccountCompositionDefaults(restrictions, key)
            RESTRICTION_ACCOUNT_QUOTE_MESSAGES_REPLY ->
                saveAccountQuoteMessagesWhenReply(restrictions, key)
            RESTRICTION_ACCOUNT_DEFAULT_FOLDERS ->
                saveAccountDefaultFolders(restrictions, key)
            RESTRICTION_ACCOUNT_ENABLE_SERVER_SEARCH ->
                saveAccountEnableServerSearch(restrictions, key)
            RESTRICTION_ACCOUNT_SERVER_SEARCH_LIMIT ->
                saveAccountSeverSearchLimit(restrictions, key)
            RESTRICTION_ACCOUNT_STORE_MESSAGES_SECURELY ->
                saveAccountSaveMessagesSecurely(restrictions, key)
            RESTRICTION_ACCOUNT_ENABLE_SYNC ->
                saveAccountEnableSync(restrictions, key)
        }
    }

    private fun saveProvisioningUrl(restrictions: Bundle, key: String) {
        updateString(restrictions, key) {
            k9.provisioningUrl = it
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
                ?.toSet()?.also { newExtraKeys ->
                    if (newExtraKeys.isEmpty() || newExtraKeys.all { it.isBlank() }) {
                        K9.setMasterKeys(Collections.emptySet())
                    } else {
                        newExtraKeys.filter { it.isNotBlank() }
                            .also { K9.setMasterKeys(it.toSet()) }
                    }
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

    private fun saveAccountLocalFolderSize(restrictions: Bundle, key: String) {
        updateAccountString(
            restrictions,
            key
        ) { account, newValue ->
            account.displayCount = newValue.toInt()
        }
    }

    private fun saveAccountMaxPushFolders(restrictions: Bundle, key: String) {
        updateAccountString(
            restrictions,
            key
        ) { account, newValue ->
            account.maxPushFolders = newValue.toInt()
        }
    }

    private fun saveAccountQuoteMessagesWhenReply(restrictions: Bundle, key: String) {
        updateAccountBoolean(
            restrictions,
            key
        ) { account, newValue ->
            account.isDefaultQuotedTextShown = newValue
        }
    }

    private fun saveAccountCompositionDefaults(restrictions: Bundle, key: String) {
        val bundle = restrictions.getBundle(key)
        bundle?.let {
            bundle.keySet().forEach { key ->
                when (key) {
                    RESTRICTION_ACCOUNT_COMPOSITION_USE_SIGNATURE ->
                        saveAccountUseSignature(bundle, key)
                    RESTRICTION_ACCOUNT_COMPOSITION_SIGNATURE ->
                        saveAccountSignature(bundle, key)
                    RESTRICTION_ACCOUNT_COMPOSITION_SIGNATURE_BEFORE_QUOTED_MESSAGE ->
                        saveAccountSignatureBeforeQuotedMessage(bundle, key)
                }
            }
        }
    }

    private fun saveAccountSignatureBeforeQuotedMessage(bundle: Bundle, key: String) {
        updateAccountBoolean(bundle, key) { account, newValue ->
            account.isSignatureBeforeQuotedText = newValue
        }
    }

    private fun saveAccountSignature(bundle: Bundle, key: String) {
        updateAccountString(bundle, key) { account, newValue ->
            account.signature = newValue
        }
    }

    private fun saveAccountUseSignature(bundle: Bundle, key: String) {
        updateAccountBoolean(bundle, key) { account, newValue ->
            account.signatureUse = newValue
        }
    }

    private fun saveAccountDefaultFolders(restrictions: Bundle, key: String) {
        val bundle = restrictions.getBundle(key)
        bundle?.let {
            bundle.keySet().forEach { key ->
                when (key) {
                    RESTRICTION_ACCOUNT_ARCHIVE_FOLDER ->
                        saveAccountArchiveFolder(bundle, key)
                    RESTRICTION_ACCOUNT_DRAFTS_FOLDER ->
                        saveAccountDraftsFolder(bundle, key)
                    RESTRICTION_ACCOUNT_SENT_FOLDER ->
                        saveAccountSentFolder(bundle, key)
                    RESTRICTION_ACCOUNT_SPAM_FOLDER ->
                        saveAccountSpamFolder(bundle, key)
                    RESTRICTION_ACCOUNT_TRASH_FOLDER ->
                        saveAccountTrashFolder(bundle, key)
                }
            }
        }
    }

    private fun saveAccountArchiveFolder(restrictions: Bundle, key: String) {
        updateAccountString(
            restrictions,
            key,
        ) { account, newValue ->
            account.archiveFolderName = newValue
        }
    }

    private fun saveAccountDraftsFolder(restrictions: Bundle, key: String) {
        updateAccountString(
            restrictions,
            key
        ) { account, newValue ->
            account.draftsFolderName = newValue
        }
    }

    private fun saveAccountSentFolder(restrictions: Bundle, key: String) {
        updateAccountString(
            restrictions,
            key,
        ) { account, newValue ->
            account.sentFolderName = newValue
        }
    }

    private fun saveAccountSpamFolder(restrictions: Bundle, key: String) {
        updateAccountString(
            restrictions,
            key,
        ) { account, newValue ->
            account.spamFolderName = newValue
        }
    }

    private fun saveAccountTrashFolder(restrictions: Bundle, key: String) {
        updateAccountString(
            restrictions,
            key,
        ) { account, newValue ->
            account.trashFolderName = newValue
        }
    }

    private fun saveAccountEnableServerSearch(restrictions: Bundle, key: String) {
        updateAccountBoolean(
            restrictions,
            key,
        ) { account, newValue ->
            account.setAllowRemoteSearch(newValue)
        }
    }

    private fun saveAccountSeverSearchLimit(restrictions: Bundle, key: String) {
        updateAccountString(
            restrictions,
            key,
        ) { account, newValue ->
            account.remoteSearchNumResults = newValue.toInt()
        }
    }

    private fun saveAccountSaveMessagesSecurely(restrictions: Bundle, key: String) {
        updateAccountBoolean(
            restrictions,
            key,
        ) { account, newValue ->
            account.setPEpStoreEncryptedOnServer(newValue)
        }
    }

    private fun saveAccountEnableSync(restrictions: Bundle, key: String) {
        updateAccountBoolean(
            restrictions,
            key,
        ) { account, newValue ->
            account.isDefaultQuotedTextShown = newValue
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

    private inline fun updateString(
        restrictions: Bundle,
        key: String,
        crossinline block: (newValue: String) -> Unit
    ) {
        kotlin.runCatching {
            val newValue = restrictions.getString(key)
            if (!newValue.isNullOrBlank()) {
                block(newValue)
            }
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
                preferences?.accounts?.forEach { account ->
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
            preferences?.accounts?.forEach { account ->
                block(account, newValue)
            }
        }.onFailure { Timber.e(it) }
    }
}
