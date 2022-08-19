package security.pEp.mdm

import android.content.RestrictionEntry
import android.os.Build
import android.os.Bundle
import android.util.Patterns
import androidx.annotation.RequiresApi
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.R
import com.fsck.k9.auth.OAuthProviderType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.Transport
import com.fsck.k9.mail.store.RemoteStore
import com.fsck.k9.mailstore.FolderRepositoryManager
import security.pEp.network.UrlChecker
import security.pEp.provisioning.*
import timber.log.Timber

private const val CONNECTION_SECURITY_NONE = "NONE"
private const val CONNECTION_SECURITY_STARTTLS = "STARTTLS"
private const val CONNECTION_SECURITY_SSL_TLS = "SSL/TLS"

class ConfiguredSettingsUpdater(
    private val k9: K9,
    private val preferences: Preferences,
    private val urlChecker: UrlChecker = UrlChecker(),
    private val folderRepositoryManager: FolderRepositoryManager = FolderRepositoryManager(),
    private val provisioningSettings: ProvisioningSettings = k9.component.provisioningSettings(),
) {

    fun update(
        restrictions: Bundle,
        entry: RestrictionEntry,
    ) {
        when (entry.key) {
            RESTRICTION_PROVISIONING_URL ->
                updateString(restrictions, entry, accepted = { it.isNotBlank() }) {
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
            RESTRICTION_ALLOW_PEP_SYNC_NEW_DEVICES ->
                k9.setAllowpEpSyncNewDevices(getBooleanOrDefault(restrictions, entry))

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
        updateNullableString(
            restrictions,
            entry,
            default = { null }
        ) {
            provisioningSettings.accountDescription = it
        }
        updateAccountString(
            restrictions,
            entry,
            default = { it.email }
        ) { account, newValue ->
            account.description = newValue
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun saveAccountMailSettings(restrictions: Bundle, entry: RestrictionEntry) {
        val bundle = restrictions.getBundle(entry.key)
        var incoming = SimpleMailSettings()
        var outgoing = SimpleMailSettings()
        var oAuthProviderType = OAuthProviderType.NONE
        oAuthProviderType = getNewOAuthProviderType(entry, bundle, oAuthProviderType)
        entry.restrictions.forEach { restriction ->
            when (restriction.key) {
                RESTRICTION_ACCOUNT_EMAIL_ADDRESS ->
                    saveAccountEmailAddress(bundle, restriction)
                RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS -> {
                    incoming = saveAccountIncomingSettings(
                        bundle,
                        restriction
                    ) // TODO: 22/7/22 give feedback of invalid settings for operations
                }
                RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS -> {
                    outgoing = saveAccountOutgoingSettings(
                        bundle,
                        restriction
                    ) // TODO: 22/7/22 give feedback of invalid settings for operations
                }
            }
        }
        provisioningSettings.provisionedMailSettings = AccountMailSettingsProvision(
            incoming,
            outgoing,
            oAuthProviderType
        )
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getNewOAuthProviderType(
        entry: RestrictionEntry,
        bundle: Bundle?,
        previous: OAuthProviderType
    ): OAuthProviderType {
        var oAuthProvider = previous
        entry.restrictions
            .firstOrNull { it.key == RESTRICTION_ACCOUNT_OAUTH_PROVIDER }
            ?.let { restriction ->
                updateString(
                    bundle,
                    restriction,
                    accepted = { newValue ->
                        newValue.isNotBlank() &&
                                newValue in OAuthProviderType.values().map { it.toString() }
                    },
                ) {
                    oAuthProvider = OAuthProviderType.valueOf(it)
                }
            }
        return oAuthProvider
    }

    private fun saveAccountEmailAddress(restrictions: Bundle?, entry: RestrictionEntry) {
        updateNullableString(
            restrictions,
            entry,
            default = { null }
        ) {
            provisioningSettings.email = it
        }
        updateAccountString(
            restrictions,
            entry,
            accepted = { newValue ->
                !newValue.isNullOrBlank() && Patterns.EMAIL_ADDRESS.matcher(newValue).matches()
            }
        ) { account, newValue ->
            account.email = newValue
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun saveAccountIncomingSettings(
        restrictions: Bundle?,
        entry: RestrictionEntry
    ): SimpleMailSettings {
        val currentSettings: ServerSettings? = getCurrentIncomingSettings()
        var simpleSettings = currentSettings?.toSimpleMailSettings() ?: SimpleMailSettings()
        val bundle = restrictions?.getBundle(entry.key)
        entry.restrictions.forEach { restriction ->
            when (restriction.key) {
                RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_PORT ->
                    updateInt(
                        bundle,
                        restriction,
                        accepted = { it.isValidPort() }
                    ) {
                        simpleSettings = simpleSettings.copy(port = it)
                    }
                RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_SERVER ->
                    updateString(
                        bundle,
                        restriction,
                        accepted = { server ->
                            server.isValidServer(urlChecker)
                        }
                    ) {
                        simpleSettings = simpleSettings.copy(server = it)
                    }
                RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_SECURITY_TYPE ->
                    updateString(
                        bundle,
                        restriction,
                        accepted = { newValue ->
                            newValue.toConnectionSecurity() != null
                        }
                    ) {
                        simpleSettings = simpleSettings.copy(
                            connectionSecurity = it.toConnectionSecurity()
                        )
                    }
                RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_USER_NAME ->
                    updateString(
                        bundle,
                        restriction,
                        accepted = { it.isNotBlank() && !it.contains("{{") }
                    ) {
                        simpleSettings = simpleSettings.copy(userName = it)
                    }
                RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_AUTH_TYPE ->
                    updateString(
                        bundle,
                        restriction,
                        accepted = { newValue ->
                            newValue.isNotBlank() &&
                                    newValue in AuthType.values().map { it.toString() }
                        }
                    ) {
                        simpleSettings = simpleSettings.copy(authType = AuthType.valueOf(it))
                    }
            }
        }

        preferences.accounts.forEach { account ->
            val currentStoreUri = account.storeUri
            val settings = currentSettings ?: RemoteStore.decodeStoreUri(currentStoreUri)
            val newSettings = settings.newFromProvisionValues(
                simpleSettings.server,
                simpleSettings.connectionSecurity,
                simpleSettings.port,
                simpleSettings.userName,
                simpleSettings.authType?.toAppAuthType()
            )
            account.storeUri = try {
                RemoteStore.createStoreUri(newSettings)
            } catch (ex: Throwable) { // TODO: 28/7/22 notify back to MDM incoming server settings could not be applied, if possible 
                currentStoreUri
            }
        }
        return simpleSettings
    }

    private fun getCurrentIncomingSettings(): ServerSettings? {
        return preferences.accounts.firstOrNull()?.let {
            kotlin.runCatching { RemoteStore.decodeStoreUri(it.storeUri) }
                .onFailure { Timber.e(it) }
                .getOrNull()
        }
    }

    private fun getCurrentOutgoingSettings(): ServerSettings? {
        return preferences.accounts.firstOrNull()?.let {
            kotlin.runCatching { Transport.decodeTransportUri(it.transportUri) }
                .onFailure {
                    Timber.e(it)
                }.getOrNull()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun saveAccountOutgoingSettings(
        restrictions: Bundle?,
        entry: RestrictionEntry
    ): SimpleMailSettings {
        val currentSettings: ServerSettings? = getCurrentOutgoingSettings()
        var simpleSettings = currentSettings?.toSimpleMailSettings() ?: SimpleMailSettings()
        val bundle = restrictions?.getBundle(entry.key)
        entry.restrictions.forEach { restriction ->
            when (restriction.key) {
                RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_PORT ->
                    updateInt(
                        bundle,
                        restriction,
                        accepted = { it.isValidPort() }
                    ) {
                        simpleSettings = simpleSettings.copy(port = it)
                    }
                RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_SERVER ->
                    updateString(
                        bundle,
                        restriction,
                        accepted = { server ->
                            server.isValidServer(urlChecker)
                        }
                    ) {
                        simpleSettings = simpleSettings.copy(server = it)
                    }
                RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_SECURITY_TYPE ->
                    updateString(
                        bundle,
                        restriction,
                        accepted = { newValue ->
                            newValue.toConnectionSecurity() != null
                        }
                    ) {
                        simpleSettings = simpleSettings.copy(
                            connectionSecurity = it.toConnectionSecurity()
                        )
                    }
                RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_USER_NAME ->
                    updateString(
                        bundle,
                        restriction,
                        accepted = { it.isNotBlank() && !it.contains("{{") }
                    ) {
                        simpleSettings = simpleSettings.copy(userName = it)
                    }
                RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_AUTH_TYPE ->
                    updateString(
                        bundle,
                        restriction,
                        accepted = { newValue ->
                            newValue.isNotBlank() &&
                                    newValue in AuthType.values().map { it.toString() }
                        }
                    ) {
                        simpleSettings = simpleSettings.copy(authType = AuthType.valueOf(it))
                    }
            }
        }

        preferences.accounts.forEach { account ->
            val currentTransportUri = account.transportUri
            val settings = currentSettings ?: Transport.decodeTransportUri(currentTransportUri)
            val newSettings = settings.newFromProvisionValues(
                simpleSettings.server,
                simpleSettings.connectionSecurity,
                simpleSettings.port,
                simpleSettings.userName,
                simpleSettings.authType?.toAppAuthType()
            )
            account.transportUri = try {
                Transport.createTransportUri(newSettings)
            } catch (ex: Throwable) { // TODO: 28/7/22 notify back to MDM outgoing server settings could not be applied, if possible
                currentTransportUri
            }
        }
        return simpleSettings
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
            entry,
            accepted = { newValue ->
                val acceptedValues = k9.resources.getStringArray(R.array.display_count_values)
                acceptedValues.contains(newValue)
            }
        ) { account, newValue ->
            try {
                newValue?.let { account.displayCount = newValue.toInt() }
            } catch (nfe: NumberFormatException) {
                Timber.e(nfe)
            }
        }
    }

    private fun saveAccountMaxPushFolders(restrictions: Bundle, entry: RestrictionEntry) {
        updateAccountString(
            restrictions,
            entry,
            accepted = { newValue ->
                val acceptedValues = k9.resources.getStringArray(R.array.push_limit_values)
                acceptedValues.contains(newValue)
            }
        ) { account, newValue ->
            try {
                newValue?.let { account.maxPushFolders = newValue.toInt() }
            } catch (nfe: NumberFormatException) {
                Timber.e(nfe)
            }
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
        updateNullableString(
            bundle,
            entry,
            default = { null }
        ) {
            provisioningSettings.senderName = it
        }
        updateAccountString(
            bundle,
            entry,
            default = { it.email },
            accepted = { !it.isNullOrBlank() }
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
        updateAccountString(
            bundle,
            entry,
            accepted = { !it.isNullOrBlank() }
        ) { account, newValue ->
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
        val firstAccount = preferences.accounts.firstOrNull()
        if (firstAccount != null) {
            kotlin.runCatching {
                val currentFolders: List<String>
                val acceptable: (String?) -> Boolean
                val bundle = restrictions.getBundle(entry.key)
                if (bundle == null
                    || bundle.keySet().map { bundle.getString(it) }.all { it.isNullOrBlank() }
                ) {
                    currentFolders = emptyList()
                    acceptable = { !it.isNullOrBlank() }
                } else {
                    currentFolders = listOf(K9.FOLDER_NONE) +
                            folderRepositoryManager.getFolderRepository(firstAccount)
                                .getRemoteFolders().map { it.name }
                    acceptable = { currentFolders.contains(it) }
                }

                entry.restrictions.forEach { restriction ->

                    fun saveFolder(
                        block: (Account, String?) -> Unit
                    ) {
                        updateAccountString(
                            bundle,
                            restriction,
                            accepted = acceptable,
                            block = block
                        )
                    }

                    when (restriction.key) {
                        RESTRICTION_ACCOUNT_ARCHIVE_FOLDER ->
                            saveFolder { account, newValue -> account.archiveFolderName = newValue }
                        RESTRICTION_ACCOUNT_DRAFTS_FOLDER ->
                            saveFolder { account, newValue -> account.draftsFolderName = newValue }
                        RESTRICTION_ACCOUNT_SENT_FOLDER ->
                            saveFolder { account, newValue -> account.sentFolderName = newValue }
                        RESTRICTION_ACCOUNT_SPAM_FOLDER ->
                            saveFolder { account, newValue -> account.spamFolderName = newValue }
                        RESTRICTION_ACCOUNT_TRASH_FOLDER ->
                            saveFolder { account, newValue -> account.trashFolderName = newValue }
                    }
                }
            }.onFailure { Timber.e(it) }
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
            accepted = { newValue ->
                val acceptedValues = k9.resources.getStringArray(
                    R.array.remote_search_num_results_values
                )
                acceptedValues.contains(newValue)
            }
        ) { account, newValue ->
            try {
                newValue?.let { account.remoteSearchNumResults = newValue.toInt() }
            } catch (nfe: NumberFormatException) {
                Timber.e(nfe)
            }
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

    private inline fun updateNullableString(
        restrictions: Bundle?,
        entry: RestrictionEntry,
        crossinline default: () -> String? = { entry.selectedString },
        crossinline accepted: (String?) -> Boolean = { true },
        block: (newValue: String?) -> Unit
    ) {
        kotlin.runCatching {
            val newValue = restrictions?.getString(entry.key) ?: default()
            if (accepted(newValue)) {
                block(newValue)
            }
        }.onFailure { Timber.e(it) }
    }

    private inline fun updateString(
        restrictions: Bundle?,
        entry: RestrictionEntry,
        crossinline default: () -> String = { entry.selectedString },
        crossinline accepted: (String) -> Boolean = { true },
        block: (newValue: String) -> Unit
    ) {
        kotlin.runCatching {
            val newValue = restrictions?.getString(entry.key) ?: default()
            if (accepted(newValue)) {
                block(newValue)
            }
        }.onFailure { Timber.e(it) }
    }

    private inline fun updateInt(
        restrictions: Bundle?,
        entry: RestrictionEntry,
        crossinline accepted: (Int) -> Boolean = { true },
        crossinline block: (newValue: Int) -> Unit
    ) {
        kotlin.runCatching {
            val newValue = getIntOrDefault(restrictions, entry)
            if (accepted(newValue)) {
                block(newValue)
            }
        }.onFailure { Timber.e(it) }
    }

    private inline fun updateAccountString(
        restrictions: Bundle?,
        entry: RestrictionEntry,
        crossinline default: (Account) -> String? = { entry.selectedString },
        crossinline accepted: (String?) -> Boolean = { true },
        crossinline block: (account: Account, newValue: String?) -> Unit
    ) {
        kotlin.runCatching {
            preferences.accounts.forEach { account ->
                val newValue = restrictions?.getString(entry.key) ?: default(account)
                if (accepted(newValue)) {
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
            val newValue = getBooleanOrDefault(restrictions, entry)
            preferences.accounts.forEach { account ->
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
}
