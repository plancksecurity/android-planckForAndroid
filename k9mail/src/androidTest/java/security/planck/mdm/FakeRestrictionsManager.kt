package security.planck.mdm

import android.app.Activity
import android.content.RestrictionEntry
import android.os.Bundle
import androidx.core.os.bundleOf
import com.fsck.k9.BuildConfig
import com.fsck.k9.activity.K9Activity
import com.fsck.k9.activity.K9ActivityCommon
import kotlinx.coroutines.runBlocking
import java.lang.reflect.Field
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeRestrictionsManager @Inject constructor() : RestrictionsProvider {
    override var applicationRestrictions: Bundle = getProvisioningRestrictions()
        private set
    override val manifestRestrictions: List<RestrictionEntry> = getDefaultManifestRestrictions()

    fun updateTestRestrictions(activity: Activity) = when (activity) {
        is K9Activity -> K9Activity::class.java
        else -> null
    }?.let { activityClass ->
        val commonField: Field = activityClass.getDeclaredField("mBase")
        commonField.isAccessible = true
        val common = commonField.get(activity) as K9ActivityCommon
        val configurationManagerField: Field =
            K9ActivityCommon::class.java.getDeclaredField("configurationManager")
        configurationManagerField.isAccessible = true
        val configurationManager = configurationManagerField.get(common) as ConfigurationManager
        runBlocking {
            configurationManager.loadConfigurationsSuspend().onSuccess {
                //if (activity is RestrictionsListener) {
                //    withContext(Dispatchers.Main) {
                //        activity.updatedRestrictions()
                //    }
                //}
            }
        }.onFailure { throw it }
    }

    fun getManifestBoolean(key: String) = manifestRestrictions.first { it.key == key }.selectedState

    fun getBoolean(key: String): Boolean? = applicationRestrictions.getBooleanOrNull(key)

    fun setBoolean(key: String, value: Boolean) = applicationRestrictions.putBoolean(key, value)

    fun getManifestString(key: String) = manifestRestrictions.first { it.key == key }.selectedString

    fun getString(key: String): String? = applicationRestrictions.getString(key)

    fun setString(key: String, value: String?) = applicationRestrictions.putString(key, value)

    fun getBundle(key: String): Bundle? = applicationRestrictions.getBundle(key)

    fun setBundle(key: String, value: Bundle?) = applicationRestrictions.putBundle(key, value)

    fun containsSetting(key: String): Boolean = applicationRestrictions.containsKey(key)

    fun removeSetting(key: String) = applicationRestrictions.remove(key)

    fun clearSettings() = applicationRestrictions.clear()

    fun resetSettings() {
        applicationRestrictions = getProvisioningRestrictions()
    }

    fun getManifestExtraKeys(): Set<String> =
        manifestRestrictions.first {
            it.key == RESTRICTION_PLANCK_EXTRA_KEYS
        }.restrictions.map { bundleRestriction ->
            bundleRestriction.restrictions.first().selectedString
        }.toSet()

    fun getExtraKeys(): Set<TestMdmExtraKey>? = applicationRestrictions.getParcelableArray(
        RESTRICTION_PLANCK_EXTRA_KEYS
    )?.map {
        val bundle = it as Bundle
        val fpr = bundle.getString(RESTRICTION_PLANCK_EXTRA_KEY_FINGERPRINT)
        val material = bundle.getString(RESTRICTION_PLANCK_EXTRA_KEY_MATERIAL)
        TestMdmExtraKey(fpr, material)
    }?.toSet()

    fun getMediaKeys(): Set<TestMdmMediaKey>? = applicationRestrictions.getParcelableArray(
        RESTRICTION_PLANCK_MEDIA_KEYS
    )?.map {
        val bundle = it as Bundle
        val pattern = bundle.getString(RESTRICTION_PLANCK_MEDIA_KEY_ADDRESS_PATTERN)
        val fpr = bundle.getString(RESTRICTION_PLANCK_MEDIA_KEY_FINGERPRINT)
        val material = bundle.getString(RESTRICTION_PLANCK_MEDIA_KEY_MATERIAL)
        TestMdmMediaKey(pattern, fpr, material)
    }?.toSet()

    fun setExtraKeys(keys: Set<TestMdmExtraKey>?) = if (keys == null) {
        applicationRestrictions.remove(RESTRICTION_PLANCK_EXTRA_KEYS)
    } else {
        applicationRestrictions.putParcelableArray(
            RESTRICTION_PLANCK_EXTRA_KEYS,
            keys.map {
                Bundle().apply {
                    putString(RESTRICTION_PLANCK_EXTRA_KEY_FINGERPRINT, it.fpr)
                    putString(RESTRICTION_PLANCK_EXTRA_KEY_MATERIAL, it.material)
                }
            }.toTypedArray()
        )
    }

    fun setMediaKeys(keys: Set<TestMdmMediaKey>?) = if (keys == null) {
        applicationRestrictions.remove(RESTRICTION_PLANCK_EXTRA_KEYS)
    } else {
        applicationRestrictions.putParcelableArray(
            RESTRICTION_PLANCK_MEDIA_KEYS,
            keys.map {
                Bundle().apply {
                    putString(RESTRICTION_PLANCK_MEDIA_KEY_ADDRESS_PATTERN, it.pattern)
                    putString(RESTRICTION_PLANCK_MEDIA_KEY_FINGERPRINT, it.fpr)
                    putString(RESTRICTION_PLANCK_MEDIA_KEY_MATERIAL, it.material)
                }
            }.toTypedArray()
        )
    }

    fun getManifestCompositionSettings(): CompositionSettings = CompositionSettings(
        senderName = DEFAULT_ACCOUNT_COMPOSITION_SENDER_NAME,
        signature = DEFAULT_ACCOUNT_COMPOSITION_SIGNATURE,
        useSignature = DEFAULT_ACCOUNT_COMPOSITION_USE_SIGNATURE,
        signatureBefore = DEFAULT_ACCOUNT_COMPOSITION_SIGNATURE_BEFORE_QUOTED_MESSAGE,
    )

    fun getCompositionSettings(): CompositionSettings? = applicationRestrictions.getBundle(
        RESTRICTION_ACCOUNT_COMPOSITION_DEFAULTS
    )?.let { bundle ->
        with(bundle) {
            CompositionSettings(
                senderName = getString(RESTRICTION_ACCOUNT_COMPOSITION_SENDER_NAME),
                signature = getString(RESTRICTION_ACCOUNT_COMPOSITION_SIGNATURE),
                useSignature = getBooleanOrNull(RESTRICTION_ACCOUNT_COMPOSITION_USE_SIGNATURE),
                signatureBefore = getBooleanOrNull(
                    RESTRICTION_ACCOUNT_COMPOSITION_SIGNATURE_BEFORE_QUOTED_MESSAGE
                )
            )
        }
    }

    fun setCompositionSettings(compositionSettings: CompositionSettings?) = with(
        applicationRestrictions
    ) {
        if (compositionSettings == null) {
            remove(RESTRICTION_ACCOUNT_COMPOSITION_DEFAULTS)
        } else {
            with(compositionSettings) {
                putBundle(
                    RESTRICTION_ACCOUNT_DEFAULT_FOLDERS,
                    Bundle().apply {
                        putStringOrRemove(
                            RESTRICTION_ACCOUNT_COMPOSITION_SENDER_NAME, senderName
                        )
                        putStringOrRemove(
                            RESTRICTION_ACCOUNT_COMPOSITION_SIGNATURE, signature
                        )
                        putBooleanOrRemove(
                            RESTRICTION_ACCOUNT_COMPOSITION_USE_SIGNATURE, useSignature
                        )
                        putBooleanOrRemove(
                            RESTRICTION_ACCOUNT_COMPOSITION_SIGNATURE_BEFORE_QUOTED_MESSAGE,
                            signatureBefore
                        )
                    }
                )
            }
        }
    }

    fun getManifestFolders(): FolderSettings = FolderSettings(
        archiveFolder = DEFAULT_FOLDER,
        draftsFolder = DEFAULT_FOLDER,
        sentFolder = DEFAULT_FOLDER,
        spamFolder = DEFAULT_FOLDER,
        trashFolder = DEFAULT_FOLDER,
    )

    fun getDefaultFolders(): FolderSettings? = applicationRestrictions.getBundle(
        RESTRICTION_ACCOUNT_DEFAULT_FOLDERS
    )?.let { bundle ->
        with(bundle) {
            FolderSettings(
                archiveFolder = getString(RESTRICTION_ACCOUNT_ARCHIVE_FOLDER),
                draftsFolder = getString(RESTRICTION_ACCOUNT_DRAFTS_FOLDER),
                sentFolder = getString(RESTRICTION_ACCOUNT_SENT_FOLDER),
                spamFolder = getString(RESTRICTION_ACCOUNT_SPAM_FOLDER),
                trashFolder = getString(RESTRICTION_ACCOUNT_TRASH_FOLDER),
            )
        }
    }

    fun setDefaultFolders(folderSettings: FolderSettings?) = with(applicationRestrictions) {
        if (folderSettings == null) {
            remove(RESTRICTION_ACCOUNT_DEFAULT_FOLDERS)
        } else {
            with(folderSettings) {
                putBundle(
                    RESTRICTION_ACCOUNT_DEFAULT_FOLDERS,
                    Bundle().apply {
                        putStringOrRemove(RESTRICTION_ACCOUNT_ARCHIVE_FOLDER, archiveFolder)
                        putStringOrRemove(RESTRICTION_ACCOUNT_DRAFTS_FOLDER, draftsFolder)
                        putStringOrRemove(RESTRICTION_ACCOUNT_SENT_FOLDER, sentFolder)
                        putStringOrRemove(RESTRICTION_ACCOUNT_SPAM_FOLDER, spamFolder)
                        putStringOrRemove(RESTRICTION_ACCOUNT_TRASH_FOLDER, trashFolder)
                    }
                )
            }
        }
    }

    fun getManifestMailSettings(): MailSettings = MailSettings(
        email = DEFAULT_ACCOUNT_EMAIL_ADDRESS,
        oAuthProvider = DEFAULT_ACCOUNT_OAUTH_PROVIDER,
        incoming = MailIncomingOutgoingSettings(
            server = DEFAULT_ACCOUNT_MAIL_SETTINGS_SERVER,
            securityType = DEFAULT_ACCOUNT_INCOMING_MAIL_SETTINGS_SECURITY,
            port = DEFAULT_ACCOUNT_INCOMING_MAIL_SETTINGS_PORT,
            userName = DEFAULT_ACCOUNT_MAIL_SETTINGS_USER_NAME,
            authType = DEFAULT_ACCOUNT_MAIL_SETTINGS_AUTH_TYPE,
        ),
        outgoing = MailIncomingOutgoingSettings(
            server = DEFAULT_ACCOUNT_MAIL_SETTINGS_SERVER,
            securityType = DEFAULT_ACCOUNT_OUTGOING_MAIL_SETTINGS_SECURITY,
            port = DEFAULT_ACCOUNT_OUTGOING_MAIL_SETTINGS_PORT,
            userName = DEFAULT_ACCOUNT_MAIL_SETTINGS_USER_NAME,
            authType = DEFAULT_ACCOUNT_MAIL_SETTINGS_AUTH_TYPE,
        )
    )

    fun getMailSettings(): MailSettings? = applicationRestrictions.getBundle(
        RESTRICTION_ACCOUNT_MAIL_SETTINGS
    )?.let { bundle ->
        with(bundle) {
            val incoming = getBundle(RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS)
            val outgoing = getBundle(RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS)
            MailSettings(
                email = getString(RESTRICTION_ACCOUNT_EMAIL_ADDRESS),
                oAuthProvider = getString(RESTRICTION_ACCOUNT_OAUTH_PROVIDER),
                incoming = incoming?.let {
                    with(incoming) {
                        MailIncomingOutgoingSettings(
                            server = getString(
                                RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_SERVER
                            ),
                            securityType = getString(
                                RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_SECURITY_TYPE
                            ),
                            port = getIntOrNull(
                                RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_PORT
                            ),
                            userName = getString(
                                RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_USER_NAME
                            ),
                            authType = getString(
                                RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_AUTH_TYPE
                            ),
                        )
                    }
                },
                outgoing = outgoing?.let {
                    with(outgoing) {
                        MailIncomingOutgoingSettings(
                            server = getString(
                                RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_SERVER
                            ),
                            securityType = getString(
                                RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_SECURITY_TYPE
                            ),
                            port = getIntOrNull(
                                RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_PORT
                            ),
                            userName = getString(
                                RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_USER_NAME
                            ),
                            authType = getString(
                                RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_AUTH_TYPE
                            ),
                        )
                    }
                }
            )
        }
    }

    fun setMailSettings(mailSettings: MailSettings?) = with(applicationRestrictions) {
        if (mailSettings == null) {
            remove(RESTRICTION_ACCOUNT_MAIL_SETTINGS)
        } else {
            val incomingBundle = mailSettings.incoming?.let { incoming ->
                Bundle().apply {
                    with(incoming) {
                        putStringOrRemove(
                            RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_SERVER,
                            server
                        )
                        putStringOrRemove(
                            RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_SECURITY_TYPE,
                            securityType
                        )
                        putIntOrRemove(
                            RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_PORT,
                            port
                        )
                        putStringOrRemove(
                            RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_USER_NAME,
                            userName
                        )
                        putStringOrRemove(
                            RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_AUTH_TYPE,
                            authType
                        )
                    }
                }
            }
            val outgoingBundle = mailSettings.outgoing?.let { outgoing ->
                Bundle().apply {
                    with(outgoing) {
                        putStringOrRemove(
                            RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_SERVER,
                            server
                        )
                        putStringOrRemove(
                            RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_SECURITY_TYPE,
                            securityType
                        )
                        putIntOrRemove(
                            RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_PORT,
                            port
                        )
                        putStringOrRemove(
                            RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_USER_NAME,
                            userName
                        )
                        putStringOrRemove(
                            RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_AUTH_TYPE,
                            authType
                        )
                    }
                }
            }
            putBundle(
                RESTRICTION_ACCOUNT_MAIL_SETTINGS,
                Bundle().apply {
                    putStringOrRemove(
                        RESTRICTION_ACCOUNT_EMAIL_ADDRESS,
                        mailSettings.email
                    )
                    putStringOrRemove(
                        RESTRICTION_ACCOUNT_OAUTH_PROVIDER,
                        mailSettings.oAuthProvider
                    )
                    incomingBundle?.let {
                        putBundle(
                            RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS,
                            it
                        )
                    }
                    outgoingBundle?.let {
                        putBundle(
                            RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS,
                            it
                        )
                    }
                }
            )
        }
    }

    private fun Bundle.getIntOrNull(key: String): Int? = get(key) as? Int
    private fun Bundle.getBooleanOrNull(key: String): Boolean? = get(key) as? Boolean
    private fun Bundle.putBooleanOrRemove(key: String, value: Boolean?) = if (value == null) {
        remove(key)
    } else {
        putBoolean(key, value)
    }

    private fun Bundle.putIntOrRemove(key: String, value: Int?) =
        if (value == null) {
            remove(key)
        } else {
            putInt(key, value)
        }

    private fun Bundle.putStringOrRemove(key: String, value: String?) =
        if (value == null) {
            remove(key)
        } else {
            putString(key, value)
        }

    companion object {
        fun getProvisioningRestrictions(): Bundle = Bundle().apply {
            putBundle(
                RESTRICTION_ACCOUNT_MAIL_SETTINGS,
                getMailSettingsBundle()
            )
        }

        fun getDefaultManifestRestrictions(): List<RestrictionEntry> =
            listOf(
                RestrictionEntry(
                    RESTRICTION_PLANCK_ENABLE_PRIVACY_PROTECTION,
                    DEFAULT_PRIVACY_PROTECTION
                ),
                getExtraKeysRestrictionEntry(),
                getMediaKeysRestrictionEntry(),
                RestrictionEntry(
                    RESTRICTION_ENABLE_ECHO_PROTOCOL,
                    DEFAULT_ENABLE_ECHO_PROTOCOL
                ),
                RestrictionEntry(
                    RESTRICTION_PLANCK_UNSECURE_DELIVERY_WARNING,
                    DEFAULT_UNSECURE_DELIVERY_WARNING
                ),
                RestrictionEntry(RESTRICTION_PLANCK_SYNC_FOLDER, DEFAULT_PLANCK_SYNC_FOLDER),
                RestrictionEntry(RESTRICTION_PLANCK_DEBUG_LOG, DEFAULT_PLANCK_DEBUG_LOG),
                RestrictionEntry(RESTRICTION_ACCOUNT_DESCRIPTION, DEFAULT_ACCOUNT_DESCRIPTION),
                RestrictionEntry(
                    RESTRICTION_ACCOUNT_LOCAL_FOLDER_SIZE,
                    DEFAULT_ACCOUNT_LOCAL_FOLDER_SIZE
                ),
                RestrictionEntry(
                    RESTRICTION_ACCOUNT_MAX_PUSH_FOLDERS,
                    DEFAULT_ACCOUNT_MAX_PUSH_FOLDERS
                ),
                getCompositionRestrictionEntry(),
                RestrictionEntry(
                    RESTRICTION_ACCOUNT_QUOTE_MESSAGES_REPLY,
                    DEFAULT_ACCOUNT_QUOTE_MESSAGES_REPLY
                ),
                getFoldersRestrictionEntry(),
                RestrictionEntry(
                    RESTRICTION_ACCOUNT_ENABLE_SERVER_SEARCH,
                    DEFAULT_ACCOUNT_ENABLE_SERVER_SEARCH
                ),
                RestrictionEntry(
                    RESTRICTION_ACCOUNT_SERVER_SEARCH_LIMIT,
                    DEFAULT_ACCOUNT_SERVER_SEARCH_LIMIT
                ),
                RestrictionEntry(RESTRICTION_ACCOUNT_ENABLE_SYNC, DEFAULT_ACCOUNT_ENABLE_SYNC),

                getMailSettingsRestrictionEntry(),
                RestrictionEntry(
                    RESTRICTION_AUDIT_LOG_DATA_TIME_RETENTION,
                    DEFAULT_AUDIT_LOG_DATA_TIME_RETENTION
                )
            )

        private fun getFoldersRestrictionEntry(): RestrictionEntry =
            RestrictionEntry.createBundleEntry(
                RESTRICTION_ACCOUNT_DEFAULT_FOLDERS,
                arrayOf(
                    RestrictionEntry(RESTRICTION_ACCOUNT_ARCHIVE_FOLDER, DEFAULT_FOLDER),
                    RestrictionEntry(RESTRICTION_ACCOUNT_DRAFTS_FOLDER, DEFAULT_FOLDER),
                    RestrictionEntry(RESTRICTION_ACCOUNT_SENT_FOLDER, DEFAULT_FOLDER),
                    RestrictionEntry(RESTRICTION_ACCOUNT_SPAM_FOLDER, DEFAULT_FOLDER),
                    RestrictionEntry(RESTRICTION_ACCOUNT_TRASH_FOLDER, DEFAULT_FOLDER)
                )
            )

        private fun getCompositionRestrictionEntry(): RestrictionEntry =
            RestrictionEntry.createBundleEntry(
                RESTRICTION_ACCOUNT_COMPOSITION_DEFAULTS,
                arrayOf(
                    RestrictionEntry(
                        RESTRICTION_ACCOUNT_COMPOSITION_SENDER_NAME,
                        DEFAULT_ACCOUNT_COMPOSITION_SENDER_NAME
                    ),
                    RestrictionEntry(
                        RESTRICTION_ACCOUNT_COMPOSITION_USE_SIGNATURE,
                        DEFAULT_ACCOUNT_COMPOSITION_USE_SIGNATURE
                    ),
                    RestrictionEntry(
                        RESTRICTION_ACCOUNT_COMPOSITION_SIGNATURE,
                        DEFAULT_ACCOUNT_COMPOSITION_SIGNATURE
                    ),
                    RestrictionEntry(
                        RESTRICTION_ACCOUNT_COMPOSITION_SIGNATURE_BEFORE_QUOTED_MESSAGE,
                        DEFAULT_ACCOUNT_COMPOSITION_SIGNATURE_BEFORE_QUOTED_MESSAGE
                    ),
                )
            )

        private fun getExtraKeysRestrictionEntry(): RestrictionEntry =
            RestrictionEntry.createBundleArrayEntry(
                RESTRICTION_PLANCK_EXTRA_KEYS,
                arrayOf(
                    RestrictionEntry.createBundleEntry(
                        RESTRICTION_PLANCK_MEDIA_KEY,
                        arrayOf(
                            RestrictionEntry(
                                RESTRICTION_PLANCK_EXTRA_KEY_FINGERPRINT,
                                DEFAULT_MEDIA_KEY_FPR
                            ),
                            RestrictionEntry(
                                RESTRICTION_PLANCK_EXTRA_KEY_MATERIAL,
                                DEFAULT_MEDIA_KEY_MATERIAL
                            ),
                        )
                    )
                )
            )

        private fun getMediaKeysRestrictionEntry(): RestrictionEntry =
            RestrictionEntry.createBundleArrayEntry(
                RESTRICTION_PLANCK_MEDIA_KEYS,
                arrayOf(
                    RestrictionEntry.createBundleEntry(
                        RESTRICTION_PLANCK_MEDIA_KEY,
                        arrayOf(
                            RestrictionEntry(
                                RESTRICTION_PLANCK_MEDIA_KEY_ADDRESS_PATTERN,
                                DEFAULT_MEDIA_KEY_PATTERN
                            ),
                            RestrictionEntry(
                                RESTRICTION_PLANCK_MEDIA_KEY_FINGERPRINT,
                                DEFAULT_MEDIA_KEY_FPR
                            ),
                            RestrictionEntry(
                                RESTRICTION_PLANCK_MEDIA_KEY_MATERIAL,
                                DEFAULT_MEDIA_KEY_MATERIAL
                            ),
                        )
                    )
                )
            )

        private fun getMailSettingsRestrictionEntry(): RestrictionEntry =
            RestrictionEntry.createBundleEntry(
                RESTRICTION_ACCOUNT_MAIL_SETTINGS,
                arrayOf(
                    RestrictionEntry(
                        RESTRICTION_ACCOUNT_EMAIL_ADDRESS,
                        DEFAULT_ACCOUNT_EMAIL_ADDRESS
                    ),
                    RestrictionEntry(
                        RESTRICTION_ACCOUNT_OAUTH_PROVIDER,
                        DEFAULT_ACCOUNT_OAUTH_PROVIDER
                    ),
                    RestrictionEntry.createBundleEntry(
                        RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS,
                        arrayOf(
                            RestrictionEntry(
                                RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_SERVER,
                                DEFAULT_ACCOUNT_MAIL_SETTINGS_SERVER
                            ),
                            RestrictionEntry(
                                RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_PORT,
                                DEFAULT_ACCOUNT_INCOMING_MAIL_SETTINGS_PORT
                            ),
                            RestrictionEntry(
                                RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_SECURITY_TYPE,
                                DEFAULT_ACCOUNT_INCOMING_MAIL_SETTINGS_SECURITY
                            ),
                            RestrictionEntry(
                                RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_USER_NAME,
                                DEFAULT_ACCOUNT_MAIL_SETTINGS_USER_NAME
                            ),
                            RestrictionEntry(
                                RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_AUTH_TYPE,
                                DEFAULT_ACCOUNT_MAIL_SETTINGS_AUTH_TYPE
                            ),
                        )
                    ),
                    RestrictionEntry.createBundleEntry(
                        RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS,
                        arrayOf(
                            RestrictionEntry(
                                RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_SERVER,
                                DEFAULT_ACCOUNT_MAIL_SETTINGS_SERVER
                            ),
                            RestrictionEntry(
                                RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_PORT,
                                DEFAULT_ACCOUNT_OUTGOING_MAIL_SETTINGS_PORT
                            ),
                            RestrictionEntry(
                                RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_SECURITY_TYPE,
                                DEFAULT_ACCOUNT_OUTGOING_MAIL_SETTINGS_SECURITY
                            ),
                            RestrictionEntry(
                                RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_USER_NAME,
                                DEFAULT_ACCOUNT_MAIL_SETTINGS_USER_NAME
                            ),
                            RestrictionEntry(
                                RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_AUTH_TYPE,
                                DEFAULT_ACCOUNT_MAIL_SETTINGS_AUTH_TYPE
                            ),
                        )
                    ),
                )
            )

        private fun getMailSettingsBundle(): Bundle = Bundle().apply {
            putString(RESTRICTION_ACCOUNT_EMAIL_ADDRESS, BuildConfig.PLANCK_TEST_EMAIL_ADDRESS)
            putString(RESTRICTION_ACCOUNT_OAUTH_PROVIDER, DEFAULT_ACCOUNT_OAUTH_PROVIDER)
            putBundle(
                RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS,
                bundleOf(
                    RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_SERVER to
                            BuildConfig.PLANCK_TEST_EMAIL_SERVER,
                    RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_PORT to
                            DEFAULT_ACCOUNT_INCOMING_MAIL_SETTINGS_PORT,
                    RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_SECURITY_TYPE to
                            DEFAULT_ACCOUNT_INCOMING_MAIL_SETTINGS_SECURITY,
                    RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_USER_NAME to
                            BuildConfig.PLANCK_TEST_EMAIL_ADDRESS,
                    RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_AUTH_TYPE to
                            DEFAULT_ACCOUNT_MAIL_SETTINGS_AUTH_TYPE,
                )
            )
            putBundle(
                RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS,
                bundleOf(
                    RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_SERVER to
                            BuildConfig.PLANCK_TEST_EMAIL_SERVER,
                    RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_PORT to
                            DEFAULT_ACCOUNT_OUTGOING_MAIL_SETTINGS_PORT,
                    RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_SECURITY_TYPE to
                            DEFAULT_ACCOUNT_OUTGOING_MAIL_SETTINGS_SECURITY,
                    RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_USER_NAME to
                            BuildConfig.PLANCK_TEST_EMAIL_ADDRESS,
                    RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_AUTH_TYPE to
                            DEFAULT_ACCOUNT_MAIL_SETTINGS_AUTH_TYPE,
                )
            )
        }
    }
}
