package security.planck.mdm

import android.content.RestrictionEntry
import android.os.Bundle
import android.util.Log
import androidx.annotation.ArrayRes
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
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.infrastructure.NEW_LINE
import com.fsck.k9.planck.infrastructure.extensions.mapSuccess
import security.planck.network.UrlChecker
import security.planck.provisioning.AccountMailSettingsProvision
import security.planck.provisioning.ProvisioningSettings
import security.planck.provisioning.SimpleMailSettings
import security.planck.provisioning.isValidEmailAddress
import security.planck.provisioning.isValidPort
import security.planck.provisioning.isValidServer
import security.planck.provisioning.toConnectionSecurity
import security.planck.provisioning.toSimpleMailSettings
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider

const val GMAIL_INCOMING_PORT = 993
const val GMAIL_OUTGOING_PORT = 465
const val GMAIL_INCOMING_SERVER = "imap.gmail.com"
const val GMAIL_OUTGOING_SERVER = "smtp.gmail.com"
private val GMAIL_SECURITY_TYPE = ConnectionSecurity.SSL_TLS_REQUIRED
private const val KEY_MATERIAL_BODY_LINE_LENGTH = 64
private const val KEY_BODY_START_PROOF_MIN_LINES = 5
private const val KEY_BODY_START_REGEX =
    """(\S{${KEY_MATERIAL_BODY_LINE_LENGTH}}\s+){${KEY_BODY_START_PROOF_MIN_LINES}}"""

class ConfiguredSettingsUpdater @Inject constructor(
    private val k9: K9,
    private val preferences: Preferences,
    private val planck: Provider<PlanckProvider>,
    private val urlChecker: UrlChecker = UrlChecker(),
    private val folderRepositoryManager: FolderRepositoryManager = FolderRepositoryManager(),
    private val provisioningSettings: ProvisioningSettings,
) {

    fun update(
        restrictions: Bundle,
        entry: RestrictionEntry,
        allowModifyAccountProvisioningSettings: Boolean,
        purgeAccountSettings: Boolean,
    ) {
        when (entry.key) {
            RESTRICTION_PROVISIONING_URL ->
                updateString(restrictions, entry, accepted = { it.isNotBlank() }) {
                    provisioningSettings.provisioningUrl = it
                }

            RESTRICTION_PLANCK_EXTRA_KEYS ->
                saveExtraKeys(restrictions, entry)

            RESTRICTION_PLANCK_MEDIA_KEYS ->
                saveMediaKeys(restrictions, entry)

            RESTRICTION_PLANCK_UNSECURE_DELIVERY_WARNING ->
                saveUnsecureDeliveryWarning(restrictions, entry)

            RESTRICTION_ENABLE_PLANCK_SYNC ->
                saveEnablePlanckSync(restrictions, entry)

            RESTRICTION_PLANCK_SYNC_FOLDER ->
                K9.setUsingpEpSyncFolder(getBooleanOrDefault(restrictions, entry))

            RESTRICTION_PLANCK_DEBUG_LOG ->
                saveDebugLogging(restrictions, entry)

            RESTRICTION_ENABLE_ECHO_PROTOCOL ->
                K9.setEchoProtocolEnabled(getBooleanOrDefault(restrictions, entry))

            RESTRICTION_AUDIT_LOG_DATA_TIME_RETENTION ->
                saveAuditLogDataTimeRetention(restrictions, entry)

            RESTRICTION_PLANCK_ACCOUNTS_SETTINGS ->
                saveAccountsSettings(
                    restrictions,
                    entry,
                    allowModifyAccountProvisioningSettings,
                    purgeAccountSettings
                )
        }
    }

    private fun saveAccountsSettings(
        restrictions: Bundle,
        entry: RestrictionEntry,
        allowModifyAccountProvisioningSettings: Boolean,
        purgeAccountSettings: Boolean,
    ) {
        if (purgeAccountSettings) {
            resetProvisioningAccountSettingsInitially()
        }
        val newMailAddresses = mutableListOf<String>()
        restrictions.getParcelableArray(entry.key)
            ?.forEach { // get the parcelable array for accounts settings
                val accountBundle = it as Bundle
                getAccountEmail(accountBundle)?.let { accountEmail ->
                    newMailAddresses.add(accountEmail)
                    saveAccountSettings(
                        accountEmail,
                        entry.restrictions.first(),
                        accountBundle,
                        allowModifyAccountProvisioningSettings
                    )
                }
            }
        if (purgeAccountSettings) {
            purgeProvisioningAccountSettings(newMailAddresses)
        }
    }

    private fun resetProvisioningAccountSettingsInitially() {
        provisioningSettings.purgeAccountsNotInstalledOrInstalling()
    }

    private fun purgeProvisioningAccountSettings(newEmailAddresses: List<String>) {
        provisioningSettings.purgeAccountsNotInRestrictions(newEmailAddresses)
    }

    private fun saveAccountSettings(
        accountEmail: String,
        accountEntry: RestrictionEntry,
        accountBundle: Bundle,
        allowModifyAccountProvisioningSettings: Boolean
    ) {
        accountEntry.restrictions.forEach { accountSettingEntry ->
            saveAccountSetting(
                accountEmail,
                accountSettingEntry,
                accountBundle,
                allowModifyAccountProvisioningSettings
            )
        }
    }

    private fun saveAccountSetting(
        accountEmail: String,
        accountSettingEntry: RestrictionEntry,
        accountBundle: Bundle,
        allowModifyAccountProvisioningSettings: Boolean
    ) {
        val account = preferences.accounts.find { it.email == accountEmail }
        when (accountSettingEntry.key) {
            RESTRICTION_ACCOUNT_DESCRIPTION ->
                saveAccountDescription(
                    accountBundle,
                    accountSettingEntry,
                    accountEmail,
                    allowModifyAccountProvisioningSettings
                )

            RESTRICTION_PLANCK_ENABLE_PRIVACY_PROTECTION ->
                account?.let { savePrivacyProtection(accountBundle, accountSettingEntry, account) }

            RESTRICTION_ACCOUNT_LOCAL_FOLDER_SIZE ->
                account?.let {
                    saveAccountLocalFolderSize(
                        accountBundle,
                        accountSettingEntry,
                        account
                    )
                }

            RESTRICTION_ACCOUNT_MAX_PUSH_FOLDERS ->
                account?.let {
                    saveAccountMaxPushFolders(
                        accountBundle,
                        accountSettingEntry,
                        account
                    )
                }

            RESTRICTION_ACCOUNT_COMPOSITION_DEFAULTS ->
                saveAccountCompositionDefaults(
                    accountBundle,
                    accountSettingEntry,
                    accountEmail,
                    allowModifyAccountProvisioningSettings
                )

            RESTRICTION_ACCOUNT_QUOTE_MESSAGES_REPLY ->
                account?.let {
                    saveAccountQuoteMessagesWhenReply(
                        accountBundle,
                        accountSettingEntry,
                        account
                    )
                }

            RESTRICTION_ACCOUNT_DEFAULT_FOLDERS ->
                account?.let {
                    saveAccountDefaultFolders(
                        accountBundle,
                        accountSettingEntry,
                        account
                    )
                }

            RESTRICTION_ACCOUNT_ENABLE_SERVER_SEARCH ->
                account?.let {
                    saveAccountEnableServerSearch(
                        accountBundle,
                        accountSettingEntry,
                        account
                    )
                }

            RESTRICTION_ACCOUNT_SERVER_SEARCH_LIMIT ->
                account?.let {
                    saveAccountSeverSearchLimit(
                        accountBundle,
                        accountSettingEntry,
                        account
                    )
                }

            RESTRICTION_ACCOUNT_ENABLE_SYNC ->
                account?.let { saveAccountEnableSync(accountBundle, accountSettingEntry, account) }

            RESTRICTION_ACCOUNT_MAIL_SETTINGS ->
                saveAccountMailSettings(
                    accountBundle,
                    accountSettingEntry,
                    accountEmail,
                    allowModifyAccountProvisioningSettings
                )
        }
    }

    private fun saveEnablePlanckSync(restrictions: Bundle, entry: RestrictionEntry) {
        saveBooleanLockableSetting(
            restrictions = restrictions,
            entry = entry,
            valueKey = RESTRICTION_ENABLE_PLANCK_SYNC_VALUE,
            lockedKey = RESTRICTION_ENABLE_PLANCK_SYNC_LOCKED
        ) {
            k9.setPlanckSyncEnabled(it)
        }
    }

    private fun saveDebugLogging(restrictions: Bundle, entry: RestrictionEntry) {
        saveBooleanLockableSetting(
            restrictions = restrictions,
            entry = entry,
            valueKey = RESTRICTION_PLANCK_DEBUG_LOG_VALUE,
            lockedKey = RESTRICTION_PLANCK_DEBUG_LOG_LOCKED
        ) {
            K9.setDebug(it)
        }
    }

    private fun saveUnsecureDeliveryWarning(restrictions: Bundle, entry: RestrictionEntry) {
        saveBooleanLockableSetting(
            restrictions = restrictions,
            entry = entry,
            valueKey = RESTRICTION_PLANCK_UNSECURE_DELIVERY_WARNING_VALUE,
            lockedKey = RESTRICTION_PLANCK_UNSECURE_DELIVERY_WARNING_LOCKED
        ) {
            k9.setPlanckForwardWarningEnabled(it)
        }
    }

    private fun saveBooleanLockableSetting(
        restrictions: Bundle,
        entry: RestrictionEntry,
        valueKey: String,
        lockedKey: String,
        updateSetting: (ManageableSetting<Boolean>) -> Unit,
    ) {
        val bundle = restrictions.getBundle(entry.key)
        var value = false
        var locked = true
        entry.restrictions.forEach { restriction ->
            when (restriction.key) {
                valueKey ->
                    value = getBooleanOrDefault(bundle, restriction)

                lockedKey ->
                    locked = getBooleanOrDefault(bundle, restriction)
            }
        }
        updateSetting(ManageableSetting(value = value, locked = locked))
    }

    private fun saveAccountDescription(
        restrictions: Bundle,
        entry: RestrictionEntry,
        accountEmail: String,
        allowModifyAccountProvisioningSettings: Boolean,
    ) {
        val bundle = restrictions.getBundle(entry.key)
        entry.restrictions.find {
            it.key == RESTRICTION_ACCOUNT_DESCRIPTION_VALUE
        }?.let { valueEntry ->
            updateNullableString(
                bundle,
                valueEntry,
                default = { null }
            ) { newDescription ->
                provisioningSettings.modifyOrAddAccountSettingsByAddress(accountEmail) {
                    it.accountDescription = newDescription
                }
            }
        }

        if (allowModifyAccountProvisioningSettings) {
            preferences.accounts.find { it.email == accountEmail }?.let { account ->
                var (value: String?, locked: Boolean) = account.lockableDescription // take current value as default

                entry.restrictions.forEach { restriction ->
                    when (restriction.key) {
                        RESTRICTION_ACCOUNT_DESCRIPTION_VALUE ->
                            updateString(
                                bundle,
                                restriction,
                                default = { accountEmail }
                            ) {
                                value = it
                            }

                        RESTRICTION_ACCOUNT_DESCRIPTION_LOCKED ->
                            locked = getBooleanOrDefault(bundle, restriction)
                    }
                }
                account.setDescription(ManageableSetting(value = value, locked = locked))
            }
        }
    }

    private fun getAccountEmail(
        accountBundle: Bundle
    ): String? = accountBundle.getBundle(RESTRICTION_ACCOUNT_MAIL_SETTINGS)
        ?.getString(RESTRICTION_ACCOUNT_EMAIL_ADDRESS)
        ?.takeIf { it.isValidEmailAddress() } // missing or wrong email means account is ignored

    private fun saveAccountMailSettings(
        restrictions: Bundle,
        entry: RestrictionEntry,
        accountEmail: String,
        allowModifyAccountProvisioningSettings: Boolean,
    ) {
        val bundle = restrictions.getBundle(entry.key)
        var incoming = SimpleMailSettings()
        var outgoing = SimpleMailSettings()
        saveOAuthProviderType(entry, bundle, accountEmail, allowModifyAccountProvisioningSettings)
        val oAuthProviderType = getCurrentOAuthProvider(accountEmail)

        entry.restrictions.forEach { restriction ->
            when (restriction.key) {
                RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS -> {
                    incoming = saveAccountMailSettings(
                        bundle,
                        restriction,
                        oAuthProviderType,
                        accountEmail,
                        true,
                        allowModifyAccountProvisioningSettings
                    ) // TODO: 22/7/22 give feedback of invalid settings for operations
                }

                RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS -> {
                    outgoing = saveAccountMailSettings(
                        bundle,
                        restriction,
                        oAuthProviderType,
                        accountEmail,
                        false,
                        allowModifyAccountProvisioningSettings
                    ) // TODO: 22/7/22 give feedback of invalid settings for operations
                }
            }
        }
        provisioningSettings.modifyOrAddAccountSettingsByAddress(accountEmail) {
            it.provisionedMailSettings = AccountMailSettingsProvision(
                incoming,
                outgoing,
            )
        }
    }

    private fun updateAuthType(
        entry: RestrictionEntry,
        bundle: Bundle?,
        simpleSettings: SimpleMailSettings,
        incoming: Boolean
    ) {
        entry.restrictions
            .firstOrNull {
                it.key ==
                        if (incoming) RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_AUTH_TYPE
                        else RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_AUTH_TYPE
            }?.let { restriction ->
                updateString(
                    bundle,
                    restriction,
                    accepted = { newValue ->
                        newValue.isNotBlank() &&
                                newValue in AuthType.values().map { it.toString() }
                    }
                ) {
                    simpleSettings.authType = AuthType.valueOf(it)
                }
            }
    }

    private fun saveOAuthProviderType(
        entry: RestrictionEntry,
        bundle: Bundle?,
        accountEmail: String,
        allowModifyAccountProvisioningSettings: Boolean,
    ) {
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
                ) { newOAuthType ->
                    provisioningSettings.modifyOrAddAccountSettingsByAddress(accountEmail) {
                        it.oAuthType = OAuthProviderType.valueOf(newOAuthType)
                    }
                }

                if (allowModifyAccountProvisioningSettings) {
                    preferences.accounts.find { it.email == accountEmail }?.let { account ->
                        updateAccountString(
                            account = account,
                            bundle,
                            restriction,
                            accepted = { newValue ->
                                !newValue.isNullOrBlank() &&
                                        newValue in OAuthProviderType.values().map { it.toString() }
                            },
                        ) { newValue ->
                            newValue?.let {
                                mandatoryOAuthProviderType = OAuthProviderType.valueOf(newValue)
                            }
                        }
                    }
                }
            }
    }

    private fun getCurrentOAuthProvider(accountEmail: String): OAuthProviderType? =
        preferences.accounts.find { it.email == accountEmail }?.mandatoryOAuthProviderType
            ?: provisioningSettings.getAccountSettingsByAddress(accountEmail)?.oAuthType

    private fun saveAccountMailSettings(
        restrictions: Bundle?,
        entry: RestrictionEntry,
        oAuthProviderType: OAuthProviderType?,
        email: String?,
        incoming: Boolean,
        allowModifyAccountProvisioningSettings: Boolean,
    ): SimpleMailSettings {
        val account = preferences.accounts.find { it.email == email }
        val currentSettings: ServerSettings? =
            if (incoming) getCurrentIncomingSettings(account)
            else getCurrentOutgoingSettings(account)
        var simpleSettings = currentSettings?.toSimpleMailSettings() ?: SimpleMailSettings()
        val bundle = restrictions?.getBundle(entry.key)
        updateAuthType(entry, bundle, simpleSettings, incoming)
        if (simpleSettings.authType == AuthType.XOAUTH2
            && oAuthProviderType == OAuthProviderType.GOOGLE
            && email != null
        ) {
            simpleSettings =
                if (incoming) getGmailOAuthIncomingServerSettings(email)
                else getGmailOAuthOutgoingServerSettings(email)
        } else {
            entry.restrictions.forEach { restriction ->
                when (restriction.key) {
                    RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_PORT,
                    RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_PORT ->
                        updatePort(bundle, restriction, simpleSettings)

                    RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_SERVER,
                    RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_SERVER ->
                        updateServer(bundle, restriction, simpleSettings)

                    RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_SECURITY_TYPE,
                    RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_SECURITY_TYPE ->
                        updateSecurityType(bundle, restriction, simpleSettings)

                    RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_USER_NAME,
                    RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_USER_NAME ->
                        updateUsername(bundle, restriction, simpleSettings)
                }
            }
        }
        if (allowModifyAccountProvisioningSettings && account != null) {
            if (incoming) applyNewIncomingMailSettings(account, currentSettings, simpleSettings)
            else applyNewOutgoingMailSettings(account, currentSettings, simpleSettings)
        }
        return simpleSettings
    }

    private fun updateUsername(
        bundle: Bundle?,
        restriction: RestrictionEntry,
        simpleSettings: SimpleMailSettings
    ) {
        updateString(
            bundle,
            restriction,
            accepted = { it.isNotBlank() }
        ) {
            simpleSettings.userName = it
        }
    }

    private fun updateSecurityType(
        bundle: Bundle?,
        restriction: RestrictionEntry,
        simpleSettings: SimpleMailSettings
    ) {
        updateString(
            bundle,
            restriction,
            accepted = { newValue ->
                newValue.toConnectionSecurity() != null
            }
        ) {
            simpleSettings.connectionSecurity = it.toConnectionSecurity()
        }
    }

    private fun updateServer(
        bundle: Bundle?,
        restriction: RestrictionEntry,
        simpleSettings: SimpleMailSettings
    ) {
        updateString(
            bundle,
            restriction,
            accepted = { it.isValidServer(urlChecker) }
        ) {
            simpleSettings.server = it
        }
    }

    private fun updatePort(
        bundle: Bundle?,
        restriction: RestrictionEntry,
        simpleSettings: SimpleMailSettings
    ) {
        updateInt(
            bundle,
            restriction,
            accepted = { it.isValidPort() }
        ) {
            simpleSettings.port = it
        }
    }

    private fun getGmailOAuthIncomingServerSettings(email: String): SimpleMailSettings {
        return SimpleMailSettings(
            GMAIL_INCOMING_PORT,
            GMAIL_INCOMING_SERVER,
            GMAIL_SECURITY_TYPE,
            email,
            AuthType.XOAUTH2
        )
    }

    private fun getGmailOAuthOutgoingServerSettings(email: String): SimpleMailSettings {
        return SimpleMailSettings(
            GMAIL_OUTGOING_PORT,
            GMAIL_OUTGOING_SERVER,
            GMAIL_SECURITY_TYPE,
            email,
            AuthType.XOAUTH2
        )
    }

    private fun applyNewIncomingMailSettings(
        account: Account,
        currentSettings: ServerSettings?,
        simpleSettings: SimpleMailSettings,
    ) {
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

    private fun getCurrentIncomingSettings(account: Account?): ServerSettings? {
        return account?.let {
            kotlin.runCatching { RemoteStore.decodeStoreUri(it.storeUri) }
                .onFailure { Timber.e(it) }
                .getOrNull()
        }
    }

    private fun getCurrentOutgoingSettings(account: Account?): ServerSettings? {
        return account?.let {
            kotlin.runCatching { Transport.decodeTransportUri(it.transportUri) }
                .onFailure {
                    Timber.e(it)
                }.getOrNull()
        }
    }

    private fun applyNewOutgoingMailSettings(
        account: Account,
        currentSettings: ServerSettings?,
        simpleSettings: SimpleMailSettings
    ) {
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

    private fun saveExtraKeys(restrictions: Bundle, entry: RestrictionEntry) {
        kotlin.runCatching {
            val newMdmExtraKeys = restrictions.getParcelableArray(entry.key)
                ?.mapNotNull {
                    val bundle = it as Bundle
                    val fingerprint = bundle.getString(RESTRICTION_PLANCK_EXTRA_KEY_FINGERPRINT)
                    val keyMaterial = bundle.getString(RESTRICTION_PLANCK_EXTRA_KEY_MATERIAL)
                    if (fingerprint != null && keyMaterial != null) {
                        MdmExtraKey(
                            fingerprint.formatPgpFingerprint(),
                            keyMaterial.fixKeyMaterialHeader()
                        )
                    } else null
                }?.filter {
                    with(it) {
                        fpr.isPgpFingerprint() && material.isNotBlank()
                    }
                }
            if (newMdmExtraKeys == null) {
                K9.setMasterKeys(emptySet())
            } else if (newMdmExtraKeys.isNotEmpty()) {
                saveFilteredExtraKeys(newMdmExtraKeys)
            }
        }.onFailure { if (K9.isDebug()) Log.e("MDM", "error saving extra keys: ", it) }
    }

    private fun String.fixKeyMaterialHeader(): String {
        return KEY_BODY_START_REGEX.toRegex().find(this)?.value
            ?.substring(0, KEY_MATERIAL_BODY_LINE_LENGTH)
            ?.let { firstKeyBodyLine ->
                replace(firstKeyBodyLine, "$NEW_LINE$NEW_LINE$firstKeyBodyLine")
            } ?: this
    }

    private fun saveFilteredExtraKeys(newMdmExtraKeys: List<MdmExtraKey>) {
        val newExtraKeys = newMdmExtraKeys.mapSuccess { mdmExtraKey ->
            kotlin.runCatching {
                val fprs = planck.get().importExtraKey(mdmExtraKey.material.trim().toByteArray())
                val errorMsg = when {
                    fprs == null ->
                        "Error: got null from extra key import"

                    fprs.isEmpty() ->
                        "Error: got empty fpr vector from extra key import"

                    fprs.size != 1 ->
                        "Error: got too many or too few fprs from extra key import: " +
                                "${fprs.size}, expected: 1"

                    fprs.first() != mdmExtraKey.fpr ->
                        "Error: got an unexpected fpr from extra key import: " +
                                "${fprs.first()}, expected: ${mdmExtraKey.fpr}"

                    else -> null
                }
                errorMsg?.let { error(it) }
                mdmExtraKey.fpr
            }.onFailure {
                if (K9.isDebug()) {
                    Log.e("MDM", "error importing extra key:\n$mdmExtraKey", it)
                }
            }
        }

        if (newExtraKeys.isNotEmpty()) {
            K9.setMasterKeys(newExtraKeys.toSet())
        }
    }

    private fun saveMediaKeys(restrictions: Bundle, entry: RestrictionEntry) {
        kotlin.runCatching {
            val newMdmMediaKeys = restrictions.getParcelableArray(entry.key)
                ?.mapNotNull {
                    val bundle = it as Bundle
                    val addressPattern =
                        bundle.getString(RESTRICTION_PLANCK_MEDIA_KEY_ADDRESS_PATTERN)
                    val fingerprint = bundle.getString(RESTRICTION_PLANCK_MEDIA_KEY_FINGERPRINT)
                    val keyMaterial = bundle.getString(RESTRICTION_PLANCK_MEDIA_KEY_MATERIAL)
                    if (addressPattern != null && fingerprint != null && keyMaterial != null) {
                        MdmMediaKey(
                            addressPattern,
                            fingerprint.formatPgpFingerprint(),
                            keyMaterial.fixKeyMaterialHeader()
                        )
                    } else null
                }?.filter {
                    with(it) {
                        addressPattern.isNotBlank()
                                && fpr.isPgpFingerprint()
                                && material.isNotBlank()
                    }
                }
            if (newMdmMediaKeys == null) {
                K9.setMediaKeys(null)
            } else if (newMdmMediaKeys.isNotEmpty()) {
                saveFilteredMediaKeys(newMdmMediaKeys)
            }
        }.onFailure { if (K9.isDebug()) Log.e("MDM", "error saving media keys: ", it) }
    }

    private fun saveFilteredMediaKeys(newMdmMediaKeys: List<MdmMediaKey>) {
        val newMediaKeys = newMdmMediaKeys.mapSuccess { mdmMediaKey ->
            kotlin.runCatching {
                val ids = planck.get().importKey(mdmMediaKey.material.toByteArray())
                val errorMsg = when {
                    ids == null ->
                        "Error: got null from media key import"

                    ids.isEmpty() ->
                        "Error: got empty identity vector from media key import"

                    ids.size != 2 ->
                        "Error: got too many or too few identities from media key import: " +
                                "${ids.size}, expected: 2"

                    ids.first().fpr != mdmMediaKey.fpr ->
                        "Error: got an unexpected fpr from media key import: " +
                                "${ids.first().fpr}, expected: ${mdmMediaKey.fpr}"

                    else -> null
                }
                errorMsg?.let { error(it) }
                mdmMediaKey.toMediaKey()
            }.onFailure {
                if (K9.isDebug()) {
                    Log.e("MDM", "error importing media key:\n$mdmMediaKey", it)
                }
            }
        }

        if (newMediaKeys.isNotEmpty()) {
            K9.setMediaKeys(newMediaKeys.toSet())
        }
    }

    private fun String.isPgpFingerprint(): Boolean = matches("[A-F0-9]{40}".toRegex())

    private fun String.formatPgpFingerprint(): String = this
        .replace("\\s".toRegex(), "")
        .uppercase()

    private fun saveAuditLogDataTimeRetention(restrictions: Bundle, entry: RestrictionEntry) {
        val bundle = restrictions.getBundle(entry.key)
        var (value, locked) = k9.auditLogDataTimeRetention
        entry.restrictions.forEach { restriction ->
            when (restriction.key) {
                RESTRICTION_AUDIT_LOG_DATA_TIME_RETENTION_VALUE ->
                    updateString(
                        bundle,
                        restriction,
                        accepted = { newValue ->
                            val acceptedValues = k9.resources.getStringArray(
                                R.array.audit_log_data_time_retention_values
                            )
                            acceptedValues.contains(newValue)
                        }
                    ) {
                        try {
                            value = it.toLong()
                        } catch (nfe: NumberFormatException) {
                            Timber.e(nfe)
                        }
                    }

                RESTRICTION_AUDIT_LOG_DATA_TIME_RETENTION_LOCKED ->
                    locked = getBooleanOrDefault(bundle, restriction)
            }
        }
        k9.auditLogDataTimeRetention = ManageableSetting(value = value, locked = locked)
    }

    private fun savePrivacyProtection(
        restrictions: Bundle,
        entry: RestrictionEntry,
        account: Account
    ) {
        saveAccountBooleanLockableSetting(
            account,
            restrictions = restrictions,
            entry = entry,
            valueKey = RESTRICTION_PLANCK_ENABLE_PRIVACY_PROTECTION_VALUE,
            lockedKey = RESTRICTION_PLANCK_ENABLE_PRIVACY_PROTECTION_LOCKED,
        ) {
            setPlanckPrivacyProtection(it)
        }
    }

    private fun saveAccountLocalFolderSize(
        restrictions: Bundle,
        entry: RestrictionEntry,
        account: Account
    ) {
        saveAccountIntChoiceLockableSetting(
            account,
            restrictions = restrictions,
            entry = entry,
            valueKey = RESTRICTION_ACCOUNT_LOCAL_FOLDER_SIZE_VALUE,
            lockedKey = RESTRICTION_ACCOUNT_LOCAL_FOLDER_SIZE_LOCKED,
            acceptedValues = R.array.display_count_values,
            initialSettingValue = { lockableDisplayCount }
        ) {
            setDisplayCount(it)
        }
    }

    private fun saveAccountMaxPushFolders(
        restrictions: Bundle,
        entry: RestrictionEntry,
        account: Account
    ) {
        updateAccountString(
            account,
            restrictions,
            entry,
            accepted = { newValue ->
                val acceptedValues = k9.resources.getStringArray(R.array.push_limit_values)
                acceptedValues.contains(newValue)
            }
        ) { newValue ->
            try {
                newValue?.let { maxPushFolders = newValue.toInt() }
            } catch (nfe: NumberFormatException) {
                Timber.e(nfe)
            }
        }
    }

    private fun saveAccountQuoteMessagesWhenReply(
        restrictions: Bundle,
        entry: RestrictionEntry,
        account: Account
    ) {
        saveAccountBooleanLockableSetting(
            account,
            restrictions = restrictions,
            entry = entry,
            valueKey = RESTRICTION_ACCOUNT_QUOTE_MESSAGES_REPLY_VALUE,
            lockedKey = RESTRICTION_ACCOUNT_QUOTE_MESSAGES_REPLY_LOCKED,
        ) {
            defaultQuotedTextShown = it
        }
    }

    private fun saveAccountCompositionDefaults(
        restrictions: Bundle,
        entry: RestrictionEntry,
        accountEmail: String,
        allowModifyAccountProvisioningSettings: Boolean,
    ) {
        val bundle = restrictions.getBundle(entry.key)
        val account = preferences.accounts.find { it.email == accountEmail }
        entry.restrictions.forEach { restriction ->
            when (restriction.key) {
                RESTRICTION_ACCOUNT_COMPOSITION_SENDER_NAME ->
                    saveAccountSenderName(
                        bundle,
                        restriction,
                        accountEmail,
                        allowModifyAccountProvisioningSettings
                    )

                RESTRICTION_ACCOUNT_COMPOSITION_USE_SIGNATURE ->
                    account?.let { saveAccountUseSignature(bundle, restriction, account) }

                RESTRICTION_ACCOUNT_COMPOSITION_SIGNATURE ->
                    account?.let { saveAccountSignature(bundle, restriction, account) }

                RESTRICTION_ACCOUNT_COMPOSITION_SIGNATURE_BEFORE_QUOTED_MESSAGE ->
                    account?.let {
                        saveAccountSignatureBeforeQuotedMessage(
                            bundle,
                            restriction,
                            account
                        )
                    }
            }
        }
    }

    private fun saveAccountSenderName(
        bundle: Bundle?,
        entry: RestrictionEntry,
        accountEmail: String,
        allowModifyAccountProvisioningSettings: Boolean,
    ) {
        updateNullableString(
            bundle,
            entry,
            default = { null }
        ) { newSenderName ->
            provisioningSettings.modifyOrAddAccountSettingsByAddress(accountEmail) {
                it.senderName = newSenderName
            }
        }

        if (allowModifyAccountProvisioningSettings) {
            preferences.accounts.find { it.email == accountEmail }?.let { account ->
                updateAccountString(
                    account = account,
                    bundle,
                    entry,
                    default = { it.email },
                    accepted = { !it.isNullOrBlank() }
                ) { newValue ->
                    account.name = newValue
                }
            }
        }
    }

    private fun saveAccountSignatureBeforeQuotedMessage(
        bundle: Bundle?,
        entry: RestrictionEntry,
        account: Account
    ) {
        updateAccountBoolean(account, bundle, entry) { newValue ->
            isSignatureBeforeQuotedText = newValue
        }
    }

    private fun saveAccountSignature(bundle: Bundle?, entry: RestrictionEntry, account: Account) {
        updateAccountString(
            account,
            bundle,
            entry,
            accepted = { !it.isNullOrBlank() }
        ) { newValue ->
            signature = newValue
        }
    }

    private fun saveAccountUseSignature(
        bundle: Bundle?,
        entry: RestrictionEntry,
        account: Account
    ) {
        updateAccountBoolean(account, bundle, entry) { newValue ->
            signatureUse = newValue
        }
    }

    private fun saveAccountDefaultFolders(
        restrictions: Bundle,
        entry: RestrictionEntry,
        account: Account
    ) {
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
                            account,
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

    private fun saveAccountEnableServerSearch(
        restrictions: Bundle,
        entry: RestrictionEntry,
        account: Account
    ) {
        saveAccountBooleanLockableSetting(
            account,
            restrictions = restrictions,
            entry = entry,
            valueKey = RESTRICTION_ACCOUNT_ENABLE_SERVER_SEARCH_VALUE,
            lockedKey = RESTRICTION_ACCOUNT_ENABLE_SERVER_SEARCH_LOCKED,
        ) {
            allowRemoteSearch = it
        }
    }

    private fun saveAccountSeverSearchLimit(
        restrictions: Bundle,
        entry: RestrictionEntry,
        account: Account
    ) {
        saveAccountIntChoiceLockableSetting(
            account,
            restrictions = restrictions,
            entry = entry,
            valueKey = RESTRICTION_ACCOUNT_SERVER_SEARCH_LIMIT_VALUE,
            lockedKey = RESTRICTION_ACCOUNT_SERVER_SEARCH_LIMIT_LOCKED,
            acceptedValues = R.array.remote_search_num_results_values,
            initialSettingValue = { lockableRemoteSearchNumResults }
        ) {
            setRemoteSearchNumResults(it)
        }
    }

    private fun saveAccountIntChoiceLockableSetting(
        account: Account,
        restrictions: Bundle,
        entry: RestrictionEntry,
        valueKey: String,
        lockedKey: String,
        @ArrayRes acceptedValues: Int,
        initialSettingValue: Account.() -> ManageableSetting<Int>,
        updateSetting: Account.(ManageableSetting<Int>) -> Unit,
    ) {
        val bundle = restrictions.getBundle(entry.key)
        val firstAccount = preferences.accounts.firstOrNull() ?: return

        var (value, locked) = firstAccount.initialSettingValue()
        entry.restrictions.forEach { restriction ->
            when (restriction.key) {
                valueKey ->
                    updateString(
                        bundle,
                        restriction,
                        accepted = { newValue ->
                            k9.resources.getStringArray(acceptedValues).contains(newValue)
                        }
                    ) {
                        try {
                            value = it.toInt()
                        } catch (nfe: NumberFormatException) {
                            Timber.e(nfe)
                        }
                    }

                lockedKey ->
                    locked = getBooleanOrDefault(bundle, restriction)
            }
        }
        account.updateSetting(ManageableSetting(value = value, locked = locked))
    }

    private fun saveAccountEnableSync(
        restrictions: Bundle,
        entry: RestrictionEntry,
        account: Account
    ) {
        saveAccountBooleanLockableSetting(
            account,
            restrictions = restrictions,
            entry = entry,
            valueKey = RESTRICTION_ACCOUNT_ENABLE_SYNC_VALUE,
            lockedKey = RESTRICTION_ACCOUNT_ENABLE_SYNC_LOCKED,
        ) {
            setPlanckSyncAccount(it)
        }
    }

    private fun saveAccountBooleanLockableSetting(
        account: Account,
        restrictions: Bundle,
        entry: RestrictionEntry,
        valueKey: String,
        lockedKey: String,
        updateSetting: Account.(ManageableSetting<Boolean>) -> Unit,
    ) {
        val bundle = restrictions.getBundle(entry.key)
        var value = false
        var locked = true

        entry.restrictions.forEach { restriction ->
            when (restriction.key) {
                valueKey ->
                    value = getBooleanOrDefault(bundle, restriction)

                lockedKey ->
                    locked = getBooleanOrDefault(bundle, restriction)
            }
        }

        account.updateSetting(ManageableSetting(value = value, locked = locked))
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
        account: Account,
        restrictions: Bundle?,
        entry: RestrictionEntry,
        crossinline default: (Account) -> String? = { entry.selectedString },
        crossinline accepted: (String?) -> Boolean = { true },
        crossinline block: Account.(newValue: String?) -> Unit
    ) {
        kotlin.runCatching {
            val newValue = restrictions?.getString(entry.key) ?: default(account)
            if (accepted(newValue)) {
                account.block(newValue)
            }
        }.onFailure { Timber.e(it) }
    }

    private inline fun updateAccountBoolean(
        account: Account,
        restrictions: Bundle?,
        entry: RestrictionEntry,
        crossinline block: Account.(newValue: Boolean) -> Unit
    ) {
        kotlin.runCatching {
            val newValue = getBooleanOrDefault(restrictions, entry)
            account.block(newValue)
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
