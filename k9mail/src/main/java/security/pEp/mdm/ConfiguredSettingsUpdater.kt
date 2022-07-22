package security.pEp.mdm

import android.content.RestrictionEntry
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.Transport
import com.fsck.k9.mail.store.RemoteStore
import security.pEp.provisioning.AccountMailSettingsProvision
import security.pEp.provisioning.ProvisioningSettings
import security.pEp.provisioning.SimpleMailSettings
import timber.log.Timber

class ConfiguredSettingsUpdater(
    private val k9: K9,
    private val preferences: Preferences,
    private val provisioningSettings: ProvisioningSettings = k9.component.provisioningSettings(),
) {

    fun update(
        restrictions: Bundle,
        entry: RestrictionEntry,
    ) {
        when (entry.key) {
            RESTRICTION_PROVISIONING_URL ->
                updateString(restrictions, entry) {
                    provisioningSettings.provisioningUrl = it
                }
            RESTRICTION_PEP_EXTRA_KEYS ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    saveExtrasKeys(restrictions, entry)
                }
            RESTRICTION_PEP_USE_TRUSTWORDS ->
                K9.setpEpUseTrustwords(getBooleanOrDefault(restrictions, entry))
            RESTRICTION_PEP_UNSECURE_DELIVERY_WARNING ->
                k9.setpEpForwardWarningEnabled(getBooleanOrDefault(restrictions, entry))
            RESTRICTION_PEP_SYNC_FOLDER ->
                K9.setUsingpEpSyncFolder(getBooleanOrDefault(restrictions, entry))
            RESTRICTION_PEP_DEBUG_LOG ->
                K9.setDebug(getBooleanOrDefault(restrictions, entry))

            RESTRICTION_ACCOUNT_DESCRIPTION ->
                saveAccountDescription(restrictions, entry)
            RESTRICTION_PEP_ENABLE_PRIVACY_PROTECTION ->
                savePrivacyProtection(restrictions, entry)
            RESTRICTION_ACCOUNT_LOCAL_FOLDER_SIZE ->
                saveAccountLocalFolderSize(restrictions, entry)
            RESTRICTION_ACCOUNT_MAX_PUSH_FOLDERS ->
                saveAccountMaxPushFolders(restrictions, entry)
            RESTRICTION_ACCOUNT_COMPOSITION_DEFAULTS ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    saveAccountCompositionDefaults(restrictions, entry)
                }
            RESTRICTION_ACCOUNT_QUOTE_MESSAGES_REPLY ->
                saveAccountQuoteMessagesWhenReply(restrictions, entry)
            RESTRICTION_ACCOUNT_DEFAULT_FOLDERS ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    saveAccountDefaultFolders(restrictions, entry)
                }
            RESTRICTION_ACCOUNT_ENABLE_SERVER_SEARCH ->
                saveAccountEnableServerSearch(restrictions, entry)
            RESTRICTION_ACCOUNT_SERVER_SEARCH_LIMIT ->
                saveAccountSeverSearchLimit(restrictions, entry)
            RESTRICTION_ACCOUNT_STORE_MESSAGES_SECURELY ->
                saveAccountSaveMessagesSecurely(restrictions, entry)
            RESTRICTION_ACCOUNT_ENABLE_SYNC ->
                saveAccountEnableSync(restrictions, entry)

            RESTRICTION_ACCOUNT_MAIL_SETTINGS ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    saveAccountMailSettings(restrictions, entry)
                }
        }
    }

    private fun saveAccountDescription(restrictions: Bundle, entry: RestrictionEntry) {
        updateString(restrictions, entry) {
            provisioningSettings.accountDescription = it
        }
        updateAccountString(restrictions, entry) { account, newValue ->
            account.description = newValue
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun saveAccountMailSettings(restrictions: Bundle, entry: RestrictionEntry) {
        val bundle = restrictions.getBundle(entry.key)
        var incoming = SimpleMailSettings()
        var outgoing = SimpleMailSettings()
        entry.restrictions.forEach { restriction ->
            when (restriction.key) {
                RESTRICTION_ACCOUNT_EMAIL_ADDRESS ->
                    saveAccountEmailAddress(bundle, restriction)
                RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS -> {
                    incoming = getAccountIncomingMailSettings(bundle, restriction)
                    if (incoming.isValid()) { // TODO: 22/7/22 give feedback of invalid settings for operations
                        saveAccountIncomingSettings(incoming)
                    }
                }
                RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS -> {
                    outgoing = getAccountOutgoingMailSettings(bundle, restriction)
                    if (outgoing.isValid()) { // TODO: 22/7/22 give feedback of invalid settings for operations
                        saveAccountOutgoingSettings(outgoing)
                    }
                }
            }
        }
        provisioningSettings.provisionedMailSettings = AccountMailSettingsProvision(
            incoming, outgoing
        )
    }

    private fun saveAccountEmailAddress(restrictions: Bundle?, entry: RestrictionEntry) {
        updateString(restrictions, entry) {
            provisioningSettings.email = it
        }
        updateAccountString(restrictions, entry) { account, newValue ->
            account.email = newValue
        }
    }

    private fun saveAccountIncomingSettings(incoming: SimpleMailSettings) {
        preferences.accounts?.forEach { account ->
            val currentSettings = RemoteStore.decodeStoreUri(account.storeUri)
            val newSettings = currentSettings.newFromProvisionValues(
                incoming.server,
                incoming.connectionSecurity,
                incoming.port,
                incoming.userName
            )
            account.storeUri = RemoteStore.createStoreUri(newSettings)
        }
    }

    private fun saveAccountOutgoingSettings(outgoing: SimpleMailSettings) {
        preferences.accounts?.forEach { account ->
            val currentSettings = Transport.decodeTransportUri(account.transportUri)
            val newSettings = currentSettings.newFromProvisionValues(
                outgoing.server,
                outgoing.connectionSecurity,
                outgoing.port,
                outgoing.userName
            )
            account.transportUri = Transport.createTransportUri(newSettings)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getAccountOutgoingMailSettings(
        restrictions: Bundle?,
        entry: RestrictionEntry
    ): SimpleMailSettings {
        val bundle = restrictions?.getBundle(entry.key)
        var port = -1
        var server = ""
        var security = ""
        var username = ""
        entry.restrictions.forEach { restriction ->
            when(restriction.key) {
                RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_PORT ->
                    port = getIntOrDefault(bundle, restriction)
                RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_SERVER ->
                    server = getStringOrDefault(bundle, restriction)
                RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_SECURITY_TYPE ->
                    security = getStringOrDefault(bundle, restriction)
                RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_USER_NAME ->
                    username = getStringOrDefault(bundle, restriction)

            }
        }
        return SimpleMailSettings(
            port,
            server,
            security.toConnectionSecurity(),
            username
        )
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getAccountIncomingMailSettings(
        restrictions: Bundle?,
        entry: RestrictionEntry
    ): SimpleMailSettings {
        val bundle = restrictions?.getBundle(entry.key)
        var port = -1
        var server = ""
        var security = ""
        var username = ""
        entry.restrictions.forEach { restriction ->
            when(restriction.key) {
                RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_PORT ->
                    port = getIntOrDefault(bundle, restriction)
                RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_SERVER ->
                    server = getStringOrDefault(bundle, restriction)
                RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_SECURITY_TYPE ->
                    security = getStringOrDefault(bundle, restriction)
                RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_USER_NAME ->
                    username = getStringOrDefault(bundle, restriction)
                
            }
        }
        return SimpleMailSettings(
            port,
            server,
            security.toConnectionSecurity(),
            username
        )
    }

    private fun String.toConnectionSecurity(): ConnectionSecurity? = when {
        this.equals(CONNECTION_SECURITY_NONE, true) ->
            ConnectionSecurity.NONE
        this.equals(CONNECTION_SECURITY_STARTTLS, true) ->
            ConnectionSecurity.STARTTLS_REQUIRED
        this.equals(CONNECTION_SECURITY_SSL_TLS, true) ->
            ConnectionSecurity.SSL_TLS_REQUIRED
        else -> null
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun saveExtrasKeys(restrictions: Bundle, entry: RestrictionEntry) {
        kotlin.runCatching {
            val newExtraKeys = restrictions.getParcelableArray(entry.key)
            ?.mapNotNull { (it as Bundle).getString(RESTRICTION_PEP_FINGERPRINT) }
                ?: entry.restrictions.map { bundleRestriction ->
                    bundleRestriction.restrictions.first()
                }.map {
                    it.selectedString
                }

            newExtraKeys.filter {
                it.isNotBlank()
            }.toSet().also { newKeys ->
                if (newKeys.isEmpty()) {
                    K9.setMasterKeys(emptySet())
                } else {
                    newKeys.filter { it.isNotBlank() }
                        .also { K9.setMasterKeys(it.toSet()) }
                }
            }
        }
    }

    private fun savePrivacyProtection(restrictions: Bundle, entry: RestrictionEntry) {
        updateAccountBoolean(
            restrictions,
            entry
        ) { account, newValue ->
            account.setpEpPrivacyProtection(newValue)
        }
    }

    private fun saveAccountLocalFolderSize(restrictions: Bundle, entry: RestrictionEntry) {
        updateAccountString(
            restrictions,
            entry
        ) { account, newValue ->
            account.displayCount = newValue.toInt()
        }
    }

    private fun saveAccountMaxPushFolders(restrictions: Bundle, entry: RestrictionEntry) {
        updateAccountString(
            restrictions,
            entry
        ) { account, newValue ->
            account.maxPushFolders = newValue.toInt()
        }
    }

    private fun saveAccountQuoteMessagesWhenReply(restrictions: Bundle, entry: RestrictionEntry) {
        updateAccountBoolean(
            restrictions,
            entry
        ) { account, newValue ->
            account.isDefaultQuotedTextShown = newValue
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun saveAccountCompositionDefaults(restrictions: Bundle, entry: RestrictionEntry) {
        val bundle = restrictions.getBundle(entry.key)
        entry.restrictions.forEach { restriction ->
            when (restriction.key) {
                RESTRICTION_ACCOUNT_COMPOSITION_SENDER_NAME ->
                    saveAccountSenderName(bundle, restriction)
                RESTRICTION_ACCOUNT_COMPOSITION_USE_SIGNATURE ->
                    saveAccountUseSignature(bundle, restriction)
                RESTRICTION_ACCOUNT_COMPOSITION_SIGNATURE ->
                    saveAccountSignature(bundle, restriction)
                RESTRICTION_ACCOUNT_COMPOSITION_SIGNATURE_BEFORE_QUOTED_MESSAGE ->
                    saveAccountSignatureBeforeQuotedMessage(bundle, restriction)
            }
        }
    }

    private fun saveAccountSenderName(bundle: Bundle?, entry: RestrictionEntry) {
        updateString(bundle, entry) {
            provisioningSettings.senderName = it
        }
        updateAccountString(
            bundle,
            entry
        ) { account, newValue ->
            account.name = newValue
        }
    }

    private fun saveAccountSignatureBeforeQuotedMessage(bundle: Bundle?, entry: RestrictionEntry) {
        updateAccountBoolean(bundle, entry) { account, newValue ->
            account.isSignatureBeforeQuotedText = newValue
        }
    }

    private fun saveAccountSignature(bundle: Bundle?, entry: RestrictionEntry) {
        updateAccountString(bundle, entry) { account, newValue ->
            account.signature = newValue
        }
    }

    private fun saveAccountUseSignature(bundle: Bundle?, entry: RestrictionEntry) {
        updateAccountBoolean(bundle, entry) { account, newValue ->
            account.signatureUse = newValue
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun saveAccountDefaultFolders(restrictions: Bundle, entry: RestrictionEntry) {
        val bundle = restrictions.getBundle(entry.key)
        entry.restrictions.forEach { restriction ->
            when (restriction.key) {
                RESTRICTION_ACCOUNT_ARCHIVE_FOLDER ->
                    saveAccountArchiveFolder(bundle, restriction)
                RESTRICTION_ACCOUNT_DRAFTS_FOLDER ->
                    saveAccountDraftsFolder(bundle, restriction)
                RESTRICTION_ACCOUNT_SENT_FOLDER ->
                    saveAccountSentFolder(bundle, restriction)
                RESTRICTION_ACCOUNT_SPAM_FOLDER ->
                    saveAccountSpamFolder(bundle, restriction)
                RESTRICTION_ACCOUNT_TRASH_FOLDER ->
                    saveAccountTrashFolder(bundle, restriction)
            }
        }
    }

    private fun saveAccountArchiveFolder(restrictions: Bundle?, entry: RestrictionEntry) {
        updateAccountString(
            restrictions,
            entry,
        ) { account, newValue ->
            account.archiveFolderName = newValue
        }
    }

    private fun saveAccountDraftsFolder(restrictions: Bundle?, entry: RestrictionEntry) {
        updateAccountString(
            restrictions,
            entry
        ) { account, newValue ->
            account.draftsFolderName = newValue
        }
    }

    private fun saveAccountSentFolder(restrictions: Bundle?, entry: RestrictionEntry) {
        updateAccountString(
            restrictions,
            entry,
        ) { account, newValue ->
            account.sentFolderName = newValue
        }
    }

    private fun saveAccountSpamFolder(restrictions: Bundle?, entry: RestrictionEntry) {
        updateAccountString(
            restrictions,
            entry,
        ) { account, newValue ->
            account.spamFolderName = newValue
        }
    }

    private fun saveAccountTrashFolder(restrictions: Bundle?, entry: RestrictionEntry) {
        updateAccountString(
            restrictions,
            entry,
        ) { account, newValue ->
            account.trashFolderName = newValue
        }
    }

    private fun saveAccountEnableServerSearch(restrictions: Bundle, entry: RestrictionEntry) {
        updateAccountBoolean(
            restrictions,
            entry,
        ) { account, newValue ->
            account.setAllowRemoteSearch(newValue)
        }
    }

    private fun saveAccountSeverSearchLimit(restrictions: Bundle, entry: RestrictionEntry) {
        updateAccountString(
            restrictions,
            entry,
        ) { account, newValue ->
            account.remoteSearchNumResults = newValue.toInt()
        }
    }

    private fun saveAccountSaveMessagesSecurely(restrictions: Bundle, entry: RestrictionEntry) {
        updateAccountBoolean(
            restrictions,
            entry,
        ) { account, newValue ->
            account.setPEpStoreEncryptedOnServer(newValue)
        }
    }

    private fun saveAccountEnableSync(restrictions: Bundle, entry: RestrictionEntry) {
        updateAccountBoolean(
            restrictions,
            entry,
        ) { account, newValue ->
            account.setPEpSyncAccount(newValue)
        }
    }

    private inline fun updateString(
        restrictions: Bundle?,
        entry: RestrictionEntry,
        crossinline block: (newValue: String) -> Unit
    ) {
        kotlin.runCatching {
            val newValue = getStringOrDefault(restrictions, entry)
            if (newValue.isNotBlank()) {
                block(newValue)
            }
        }.onFailure { Timber.e(it) }
    }

    private inline fun updateAccountString(
        restrictions: Bundle?,
        entry: RestrictionEntry,
        crossinline block: (account: Account, newValue: String) -> Unit
    ) {
        kotlin.runCatching {
            val newValue = getStringOrDefault(restrictions, entry)
            if (newValue.isNotBlank()) {
                preferences.accounts?.forEach { account ->
                    block(account, newValue)
                }
            }
        }.onFailure { Timber.e(it) }
    }

    private inline fun updateAccountBoolean(
        restrictions: Bundle?,
        entry: RestrictionEntry,
        crossinline block: (account: Account, newValue: Boolean) -> Unit
    ) {
        kotlin.runCatching {
            val newValue = restrictions?.getBoolean(entry.key, entry.selectedState)
                ?: entry.selectedState
            preferences.accounts?.forEach { account ->
                block(account, newValue)
            }
        }.onFailure { Timber.e(it) }
    }

    private fun getBooleanOrDefault(
        restrictions: Bundle?,
        entry: RestrictionEntry,
    ): Boolean {
        return restrictions?.getBoolean(entry.key, entry.selectedState) ?: entry.selectedState
    }

    private fun getIntOrDefault(
        restrictions: Bundle?,
        entry: RestrictionEntry,
    ): Int {
        return restrictions?.getInt(entry.key, entry.intValue) ?: entry.intValue
    }

    private fun getStringOrDefault(
        restrictions: Bundle?,
        entry: RestrictionEntry,
    ): String {
        val provided = restrictions?.getString(entry.key)
        return if (!provided.isNullOrBlank()) provided else entry.selectedString
    }

    companion object {
        private const val CONNECTION_SECURITY_NONE = "NONE"
        private const val CONNECTION_SECURITY_STARTTLS = "STARTTLS"
        private const val CONNECTION_SECURITY_SSL_TLS = "SSL/TLS"
    }
}
