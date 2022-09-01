package security.pEp.mdm

import android.content.RestrictionEntry
import android.content.res.Resources
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.R
import com.fsck.k9.auth.OAuthProviderType
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.Transport
import com.fsck.k9.mail.store.RemoteStore
import com.fsck.k9.mailstore.Folder
import com.fsck.k9.mailstore.FolderRepository
import com.fsck.k9.mailstore.FolderRepositoryManager
import com.fsck.k9.mailstore.FolderType
import io.mockk.*
import junit.framework.TestCase.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import security.pEp.network.UrlChecker
import security.pEp.provisioning.*

@RunWith(AndroidJUnit4::class)
class ConfiguredSettingsUpdaterTest {
    private val k9: K9 = mockk(relaxed = true)
    private val preferences: Preferences = mockk()
    private val account: Account = mockk(relaxed = true)
    private val provisioningSettings: ProvisioningSettings = spyk(ProvisioningSettings())
    private val folderRepositoryManager: FolderRepositoryManager = mockk()
    private val folderRepository: FolderRepository = mockk()
    private val urlChecker: UrlChecker = mockk()
    private var updater = ConfiguredSettingsUpdater(
        k9,
        preferences,
        urlChecker,
        folderRepositoryManager,
        provisioningSettings
    )

    @Before
    fun setUp() {
        every { preferences.accounts }.returns(listOf(account))
        every { urlChecker.isValidUrl(any()) }.returns(true)
        every { folderRepositoryManager.getFolderRepository(account) }.returns(folderRepository)
        every { folderRepository.getRemoteFolders() }.returns(
            listOf(
                Folder(0, "", "archive", FolderType.ARCHIVE),
                Folder(0, "", "drafts", FolderType.DRAFTS),
                Folder(0, "", "sent", FolderType.SENT),
                Folder(0, "", "spam", FolderType.SPAM),
                Folder(0, "", "trash", FolderType.TRASH),
            )
        )
        mockkStatic(K9::class)
        mockkStatic(RemoteStore::class)
        mockkStatic(Transport::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(K9::class)
        unmockkStatic(RemoteStore::class)
        unmockkStatic(Transport::class)
    }

    @Test
    fun `update() takes the value for provisioning url from the provided restrictions`() {

        val restrictions = Bundle().apply { putString(RESTRICTION_PROVISIONING_URL, "url") }
        val entry = RestrictionEntry(RESTRICTION_PROVISIONING_URL, "defaultUrl")


        updater.update(restrictions, entry)


        verify { provisioningSettings.provisioningUrl = "url" }
    }

    @Test
    fun `update() takes the value for provisioning url from the restrictions entry if not provided in restrictions`() {

        val restrictions = Bundle()
        val entry = RestrictionEntry(RESTRICTION_PROVISIONING_URL, "defaultUrl")


        updater.update(restrictions, entry)


        verify { provisioningSettings.provisioningUrl = "defaultUrl" }
    }

    @Test
    fun `update() does not set provisioning url if provided value is blank`() {

        val restrictions = Bundle().apply { putString(RESTRICTION_PROVISIONING_URL, "     ") }
        val entry = RestrictionEntry(RESTRICTION_PROVISIONING_URL, "defaultUrl")


        updater.update(restrictions, entry)


        verify(exactly = 0) { provisioningSettings.provisioningUrl = any() }
    }

    @Test
    fun `update() takes the value for unsecure delivery warning from the provided restrictions`() {

        val restrictions =
            Bundle().apply { putBoolean(RESTRICTION_PEP_UNSECURE_DELIVERY_WARNING, false) }
        val entry = RestrictionEntry(RESTRICTION_PEP_UNSECURE_DELIVERY_WARNING, true)


        updater.update(restrictions, entry)


        verify { k9.setpEpForwardWarningEnabled(false) }
    }

    @Test
    fun `update() takes the value for unsecure delivery warning from the restriction entry if not provided in bundle`() {

        val restrictions = Bundle()
        val entry = RestrictionEntry(RESTRICTION_PEP_UNSECURE_DELIVERY_WARNING, true)


        updater.update(restrictions, entry)


        verify { k9.setpEpForwardWarningEnabled(true) }
    }

    @Test
    fun `update() takes the value for enable pEp privacy protection from the provided restrictions`() {

        val restrictions =
            Bundle().apply { putBoolean(RESTRICTION_PEP_ENABLE_PRIVACY_PROTECTION, false) }
        val entry = RestrictionEntry(RESTRICTION_PEP_ENABLE_PRIVACY_PROTECTION, true)


        updater.update(restrictions, entry)


        verify { account.setpEpPrivacyProtection(false) }
    }

    @Test
    fun `update() takes the value for enable pEp privacy protection from the restriction entry if not provided in bundle`() {

        val restrictions = Bundle()
        val entry = RestrictionEntry(RESTRICTION_PEP_ENABLE_PRIVACY_PROTECTION, true)


        updater.update(restrictions, entry)


        verify { account.setpEpPrivacyProtection(true) }
    }

    @Test
    fun `update() takes the value for extra keys from the provided restrictions`() {

        val restrictions = Bundle().apply {
            putParcelableArray(
                RESTRICTION_PEP_EXTRA_KEYS,
                arrayOf(Bundle().apply { putString(RESTRICTION_PEP_EXTRA_KEY_FINGERPRINT, "fpr") })
            )
        }
        val entry = RestrictionEntry.createBundleArrayEntry(
            RESTRICTION_PEP_EXTRA_KEYS,
            arrayOf(
                RestrictionEntry.createBundleEntry(
                    RESTRICTION_PEP_EXTRA_KEY,
                    arrayOf(RestrictionEntry(RESTRICTION_PEP_EXTRA_KEY_FINGERPRINT, "defaultFpr"))
                )
            )
        )


        updater.update(restrictions, entry)


        verify { K9.setMasterKeys(setOf("fpr")) }
    }

    @Test
    fun `update() takes the value for extra keys from the restriction entry if not provided in bundle`() {

        val restrictions = Bundle()
        val entry = RestrictionEntry.createBundleArrayEntry(
            RESTRICTION_PEP_EXTRA_KEYS,
            arrayOf(
                RestrictionEntry.createBundleEntry(
                    RESTRICTION_PEP_EXTRA_KEY,
                    arrayOf(RestrictionEntry(RESTRICTION_PEP_EXTRA_KEY_FINGERPRINT, "defaultFpr"))
                )
            )
        )


        updater.update(restrictions, entry)


        verify { K9.setMasterKeys(setOf("defaultFpr")) }
    }

    @Test
    fun `update() sets extra keys with empty set if all provided extra keys are blank`() {

        val restrictions = Bundle()
        val entry = RestrictionEntry.createBundleArrayEntry(
            RESTRICTION_PEP_EXTRA_KEYS,
            arrayOf(
                RestrictionEntry.createBundleEntry(
                    RESTRICTION_PEP_EXTRA_KEY,
                    arrayOf(RestrictionEntry(RESTRICTION_PEP_EXTRA_KEY_FINGERPRINT, "  "))
                )
            )
        )


        updater.update(restrictions, entry)


        verify { K9.setMasterKeys(emptySet()) }
    }

    @Test
    fun `update() takes the value for composition defaults from the provided restrictions`() {

        val restrictions = Bundle().apply {
            putBundle(
                RESTRICTION_ACCOUNT_COMPOSITION_DEFAULTS,
                Bundle().apply {
                    putString(RESTRICTION_ACCOUNT_COMPOSITION_SENDER_NAME, "sender name")
                    putBoolean(RESTRICTION_ACCOUNT_COMPOSITION_USE_SIGNATURE, false)
                    putString(RESTRICTION_ACCOUNT_COMPOSITION_SIGNATURE, "signature")
                    putBoolean(
                        RESTRICTION_ACCOUNT_COMPOSITION_SIGNATURE_BEFORE_QUOTED_MESSAGE,
                        true
                    )
                }
            )
        }
        val entry = RestrictionEntry.createBundleEntry(
            RESTRICTION_ACCOUNT_COMPOSITION_DEFAULTS,
            arrayOf(
                RestrictionEntry(
                    RESTRICTION_ACCOUNT_COMPOSITION_SENDER_NAME,
                    "default sender name"
                ),
                RestrictionEntry(
                    RESTRICTION_ACCOUNT_COMPOSITION_USE_SIGNATURE,
                    true
                ),
                RestrictionEntry(
                    RESTRICTION_ACCOUNT_COMPOSITION_SIGNATURE,
                    "default signature"
                ),
                RestrictionEntry(
                    RESTRICTION_ACCOUNT_COMPOSITION_SIGNATURE_BEFORE_QUOTED_MESSAGE,
                    false
                ),
            )
        )


        updater.update(restrictions, entry)


        verify {
            account.name = "sender name"
            account.signatureUse = false
            account.signature = "signature"
            account.isSignatureBeforeQuotedText = true
        }
    }

    @Test
    fun `update() takes the value for composition defaults from the restriction entry if not provided in bundle, and sender name defaults to email`() {
        every { account.email }.returns("email")
        val restrictions = Bundle()
        val entry = RestrictionEntry.createBundleEntry(
            RESTRICTION_ACCOUNT_COMPOSITION_DEFAULTS,
            arrayOf(
                RestrictionEntry(
                    RESTRICTION_ACCOUNT_COMPOSITION_SENDER_NAME,
                    "default sender name"
                ),
                RestrictionEntry(
                    RESTRICTION_ACCOUNT_COMPOSITION_USE_SIGNATURE,
                    true
                ),
                RestrictionEntry(
                    RESTRICTION_ACCOUNT_COMPOSITION_SIGNATURE,
                    "default signature"
                ),
                RestrictionEntry(
                    RESTRICTION_ACCOUNT_COMPOSITION_SIGNATURE_BEFORE_QUOTED_MESSAGE,
                    false
                ),
            )
        )


        updater.update(restrictions, entry)


        verify {
            account.name = "email"
            account.signatureUse = true
            account.signature = "default signature"
            account.isSignatureBeforeQuotedText = false
            provisioningSettings.senderName = null
        }
    }

    @Test
    fun `update() keeps previous composition settings values that are not valid`() {
        every { account.email }.returns("email")
        val restrictions = Bundle().apply {
            putBundle(
                RESTRICTION_ACCOUNT_COMPOSITION_DEFAULTS,
                Bundle().apply {
                    putString(RESTRICTION_ACCOUNT_COMPOSITION_SENDER_NAME, "")
                    putBoolean(RESTRICTION_ACCOUNT_COMPOSITION_USE_SIGNATURE, false)
                    putString(RESTRICTION_ACCOUNT_COMPOSITION_SIGNATURE, "    ")
                    putBoolean(
                        RESTRICTION_ACCOUNT_COMPOSITION_SIGNATURE_BEFORE_QUOTED_MESSAGE,
                        true
                    )
                }
            )
        }
        val entry = RestrictionEntry.createBundleEntry(
            RESTRICTION_ACCOUNT_COMPOSITION_DEFAULTS,
            arrayOf(
                RestrictionEntry(
                    RESTRICTION_ACCOUNT_COMPOSITION_SENDER_NAME,
                    "default sender name"
                ),
                RestrictionEntry(
                    RESTRICTION_ACCOUNT_COMPOSITION_USE_SIGNATURE,
                    true
                ),
                RestrictionEntry(
                    RESTRICTION_ACCOUNT_COMPOSITION_SIGNATURE,
                    "default signature"
                ),
                RestrictionEntry(
                    RESTRICTION_ACCOUNT_COMPOSITION_SIGNATURE_BEFORE_QUOTED_MESSAGE,
                    false
                ),
            )
        )


        updater.update(restrictions, entry)


        verify {
            account.signatureUse = false
            account.isSignatureBeforeQuotedText = true
        }

        verify(exactly = 0) {
            account.name = any()
            account.signature = any()
        }
    }

    @Test
    fun `update() takes the value for default folders from the provided restrictions`() {
        val restrictions = Bundle().apply {
            putBundle(
                RESTRICTION_ACCOUNT_DEFAULT_FOLDERS,
                Bundle().apply {
                    putString(RESTRICTION_ACCOUNT_ARCHIVE_FOLDER, "archive")
                    putString(RESTRICTION_ACCOUNT_DRAFTS_FOLDER, "drafts")
                    putString(RESTRICTION_ACCOUNT_SENT_FOLDER, "sent")
                    putString(RESTRICTION_ACCOUNT_SPAM_FOLDER, "spam")
                    putString(RESTRICTION_ACCOUNT_TRASH_FOLDER, "trash")
                }
            )
        }
        val entry = RestrictionEntry.createBundleEntry(
            RESTRICTION_ACCOUNT_DEFAULT_FOLDERS,
            arrayOf(
                RestrictionEntry(RESTRICTION_ACCOUNT_ARCHIVE_FOLDER, "archiveDefault"),
                RestrictionEntry(RESTRICTION_ACCOUNT_DRAFTS_FOLDER, "draftsDefault"),
                RestrictionEntry(RESTRICTION_ACCOUNT_SENT_FOLDER, "sentDefault"),
                RestrictionEntry(RESTRICTION_ACCOUNT_SPAM_FOLDER, "spamDefault"),
                RestrictionEntry(RESTRICTION_ACCOUNT_TRASH_FOLDER, "trashDefault")
            )
        )


        updater.update(restrictions, entry)


        verify {
            account.archiveFolderName = "archive"
            account.draftsFolderName = "drafts"
            account.sentFolderName = "sent"
            account.spamFolderName = "spam"
            account.trashFolderName = "trash"
        }
    }

    @Test
    fun `update() does not change default folders from the restriction entry if not provided in bundle`() {

        val restrictions = Bundle()
        val entry = RestrictionEntry.createBundleEntry(
            RESTRICTION_ACCOUNT_DEFAULT_FOLDERS,
            arrayOf(
                RestrictionEntry(RESTRICTION_ACCOUNT_ARCHIVE_FOLDER, "archiveDefault"),
                RestrictionEntry(RESTRICTION_ACCOUNT_DRAFTS_FOLDER, "draftsDefault"),
                RestrictionEntry(RESTRICTION_ACCOUNT_SENT_FOLDER, "sentDefault"),
                RestrictionEntry(RESTRICTION_ACCOUNT_SPAM_FOLDER, "spamDefault"),
                RestrictionEntry(RESTRICTION_ACCOUNT_TRASH_FOLDER, "trashDefault")
            )
        )


        updater.update(restrictions, entry)


        verify {
            account.archiveFolderName = "archiveDefault"
            account.draftsFolderName = "draftsDefault"
            account.sentFolderName = "sentDefault"
            account.spamFolderName = "spamDefault"
            account.trashFolderName = "trashDefault"
        }
    }

    @Test
    fun `update() does not set folder name if not provided in bundle and default value in restriction entry is null or blank`() {

        val restrictions = Bundle()
        val entry = RestrictionEntry.createBundleEntry(
            RESTRICTION_ACCOUNT_DEFAULT_FOLDERS,
            arrayOf(
                RestrictionEntry(RESTRICTION_ACCOUNT_ARCHIVE_FOLDER, "   "),
            )
        )


        updater.update(restrictions, entry)


        verify {
            account.wasNot(called)
        }
    }

    @Test
    fun `update() keeps previous folder name if folder does not exist in server`() {

        val restrictions = Bundle().apply {
            putBundle(
                RESTRICTION_ACCOUNT_DEFAULT_FOLDERS,
                Bundle().apply {
                    putString(RESTRICTION_ACCOUNT_ARCHIVE_FOLDER, "unknown folder")
                }
            )
        }
        val entry = RestrictionEntry.createBundleEntry(
            RESTRICTION_ACCOUNT_DEFAULT_FOLDERS,
            arrayOf(
                RestrictionEntry(RESTRICTION_ACCOUNT_ARCHIVE_FOLDER, "   "),
            )
        )


        updater.update(restrictions, entry)


        verify {
            account.wasNot(called)
        }
    }

    @Test
    fun `update() takes the value for provisioning mail settings from the provided restrictions`() {
        stubInitialServerSettings()
        every { preferences.accounts }.returns(emptyList())

        val restrictions = getMailSettingsBundle()
        val entry = getMailRestrictionEntry()


        updater.update(restrictions, entry)

        
        verifyProvisioningMailSettings(
            expectedEmail = NEW_EMAIL,
            expectedIncomingPort = NEW_PORT,
            expectedIncomingServer = NEW_SERVER,
            expectedConnectionSecurity = NEW_SECURITY_TYPE,
            expectedUserName = NEW_USER_NAME,
            expectedAuthType = security.pEp.mdm.AuthType.PLAIN,
            expectedOAuthProvider = OAuthProviderType.GOOGLE
        )
    }

    @Test
    fun `update() takes the value for account mail settings from the provided restrictions`() {
        stubInitialServerSettings()
        stubAccountSettersAndGetters()

        val restrictions = getMailSettingsBundle()
        val entry = getMailRestrictionEntry()


        updater.update(restrictions, entry)


        verifyAccountMailSettings(
            expectedEmail = NEW_EMAIL,
            expectedIncomingPort = NEW_PORT,
            expectedIncomingServer = NEW_SERVER,
            expectedConnectionSecurity = NEW_SECURITY_TYPE,
            expectedUserName = NEW_USER_NAME,
            expectedAuthType = AuthType.PLAIN,
            expectedOAuthProvider = OAuthProviderType.GOOGLE,
        )
    }

    @Test
    fun `update() uses specific values for provisioning mail settings when using a Gmail account with OAuth auth type`() {
        stubInitialServerSettings()
        every { preferences.accounts }.returns(emptyList())

        val restrictions = getMailSettingsBundle(
            oAuthProvider = OAuthProviderType.GOOGLE,
            authType = AuthType.XOAUTH2
        )
        val entry = getMailRestrictionEntry()


        updater.update(restrictions, entry)


        verifyProvisioningMailSettings(
            expectedEmail = NEW_EMAIL,
            expectedIncomingPort = GMAIL_INCOMING_PORT,
            expectedOutgoingPort = GMAIL_OUTGOING_PORT,
            expectedIncomingServer = GMAIL_INCOMING_SERVER,
            expectedOutgoingServer = GMAIL_OUTGOING_SERVER,
            expectedConnectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            expectedUserName = NEW_EMAIL,
            expectedAuthType = security.pEp.mdm.AuthType.XOAUTH2,
            expectedOAuthProvider = OAuthProviderType.GOOGLE
        )
    }

    @Test
    fun `update() uses specific values for account mail settings when using a Gmail account with OAuth auth type`() {
        stubInitialServerSettings()
        stubAccountSettersAndGetters()

        val restrictions = getMailSettingsBundle(
            oAuthProvider = OAuthProviderType.GOOGLE,
            authType = AuthType.XOAUTH2
        )
        val entry = getMailRestrictionEntry()


        updater.update(restrictions, entry)


        verifyAccountMailSettings(
            expectedEmail = NEW_EMAIL,
            expectedIncomingPort = GMAIL_INCOMING_PORT,
            expectedOutgoingPort = GMAIL_OUTGOING_PORT,
            expectedIncomingServer = GMAIL_INCOMING_SERVER,
            expectedOutgoingServer = GMAIL_OUTGOING_SERVER,
            expectedConnectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            expectedUserName = NEW_EMAIL,
            expectedAuthType = AuthType.XOAUTH2,
            expectedOAuthProvider = OAuthProviderType.GOOGLE,
        )
    }

    @Test
    fun `update() uses given values for provisioning mail settings when using certificate auth`() {
        stubInitialServerSettings()
        every { preferences.accounts }.returns(emptyList())

        val restrictions = getMailSettingsBundle(authType = AuthType.EXTERNAL)
        val entry = getMailRestrictionEntry()


        updater.update(restrictions, entry)


        verifyProvisioningMailSettings(
            expectedEmail = NEW_EMAIL,
            expectedIncomingPort = NEW_PORT,
            expectedIncomingServer = NEW_SERVER,
            expectedConnectionSecurity = NEW_SECURITY_TYPE,
            expectedUserName = NEW_USER_NAME,
            expectedAuthType = security.pEp.mdm.AuthType.EXTERNAL,
            expectedOAuthProvider = OAuthProviderType.GOOGLE
        )
    }

    @Test
    fun `update() uses given values for account mail settings when using certificate auth`() {
        stubInitialServerSettings()
        stubAccountSettersAndGetters()

        val restrictions = getMailSettingsBundle(authType = AuthType.EXTERNAL)
        val entry = getMailRestrictionEntry()


        updater.update(restrictions, entry)


        verifyAccountMailSettings(
            expectedEmail = NEW_EMAIL,
            expectedIncomingPort = NEW_PORT,
            expectedIncomingServer = NEW_SERVER,
            expectedConnectionSecurity = NEW_SECURITY_TYPE,
            expectedUserName = NEW_USER_NAME,
            expectedAuthType = AuthType.EXTERNAL,
            expectedOAuthProvider = OAuthProviderType.GOOGLE,
        )
    }

    @Test
    fun `update() uses given values for provisioning mail settings when using encrypted password auth`() {
        stubInitialServerSettings()
        every { preferences.accounts }.returns(emptyList())

        val restrictions = getMailSettingsBundle(authType = AuthType.CRAM_MD5)
        val entry = getMailRestrictionEntry()


        updater.update(restrictions, entry)


        verifyProvisioningMailSettings(
            expectedEmail = NEW_EMAIL,
            expectedIncomingPort = NEW_PORT,
            expectedIncomingServer = NEW_SERVER,
            expectedConnectionSecurity = NEW_SECURITY_TYPE,
            expectedUserName = NEW_USER_NAME,
            expectedAuthType = security.pEp.mdm.AuthType.CRAM_MD5,
            expectedOAuthProvider = OAuthProviderType.GOOGLE
        )
    }

    @Test
    fun `update() uses given values for account mail settings when using encrypted password auth`() {
        stubInitialServerSettings()
        stubAccountSettersAndGetters()

        val restrictions = getMailSettingsBundle(authType = AuthType.CRAM_MD5)
        val entry = getMailRestrictionEntry()


        updater.update(restrictions, entry)


        verifyAccountMailSettings(
            expectedEmail = NEW_EMAIL,
            expectedIncomingPort = NEW_PORT,
            expectedIncomingServer = NEW_SERVER,
            expectedConnectionSecurity = NEW_SECURITY_TYPE,
            expectedUserName = NEW_USER_NAME,
            expectedAuthType = AuthType.CRAM_MD5,
            expectedOAuthProvider = OAuthProviderType.GOOGLE,
        )
    }

    @Test
    fun `update() takes the value for provisioning mail settings from the restriction entry if not provided in bundle`() {
        stubInitialServerSettings()
        every { preferences.accounts }.returns(emptyList())

        val restrictions = Bundle()
        val entry = getMailRestrictionEntry()


        updater.update(restrictions, entry)


        verifyProvisioningMailSettings(
            expectedEmail = null,
            expectedIncomingPort = DEFAULT_PORT,
            expectedIncomingServer = DEFAULT_SERVER,
            expectedConnectionSecurity = DEFAULT_SECURITY_TYPE,
            expectedUserName = DEFAULT_USER_NAME,
            expectedAuthType = security.pEp.mdm.AuthType.PLAIN,
            expectedOAuthProvider = DEFAULT_OAUTH_PROVIDER
        )
    }

    @Test
    fun `update() takes the value for account mail settings from the restriction entry if not provided in bundle`() {
        stubInitialServerSettings()
        stubAccountSettersAndGetters()

        val restrictions = Bundle()
        val entry = getMailRestrictionEntry()


        updater.update(restrictions, entry)


        verifyAccountMailSettings(
            expectedEmail = DEFAULT_EMAIL,
            expectedIncomingPort = DEFAULT_PORT,
            expectedIncomingServer = DEFAULT_SERVER,
            expectedConnectionSecurity = DEFAULT_SECURITY_TYPE,
            expectedUserName = DEFAULT_USER_NAME,
            expectedAuthType = AuthType.PLAIN,
            expectedOAuthProvider = DEFAULT_OAUTH_PROVIDER,
        )
    }

    @Test
    fun `update() keeps the old provisioning mail setting values for each new value that is not valid`() {
        stubInitialServerSettings()
        every { preferences.accounts }.returns(emptyList())

        val restrictions = getMailSettingsBundle(
            email = "malformedEmail",
            server = "",
            username = "{{}}",
            security = "wrong security",
            port = -4
        )
        val entry = getMailRestrictionEntry()


        updater.update(restrictions, entry)


        verifyProvisioningMailSettings(
            expectedEmail = null,
            expectedIncomingPort = -1,
            expectedIncomingServer = null,
            expectedConnectionSecurity = null,
            expectedUserName = null,
            expectedAuthType = security.pEp.mdm.AuthType.PLAIN,
            expectedOAuthProvider = OAuthProviderType.GOOGLE
        )
    }

    @Test
    fun `update() keeps the old account mail setting values for each new value that is not valid`() {
        stubInitialServerSettings()
        stubAccountSettersAndGetters()

        val restrictions = getMailSettingsBundle(
            email = "malformedEmail",
            server = "",
            username = "{{}}",
            security = "wrong security",
            port = -4
        )
        val entry = getMailRestrictionEntry()


        updater.update(restrictions, entry)


        verifyAccountMailSettings(
            expectedEmail = OLD_EMAIL,
            expectedIncomingPort = OLD_PORT,
            expectedIncomingServer = OLD_SERVER,
            expectedConnectionSecurity = OLD_SECURITY_TYPE,
            expectedUserName = OLD_USER_NAME,
            expectedAuthType = AuthType.PLAIN,
            expectedOAuthProvider = OAuthProviderType.GOOGLE,
        )
    }

    @Test
    fun `update() keeps the old provisioning mail settings server if UrlChecker fails to check it`() {
        stubInitialServerSettings()
        every { preferences.accounts }.returns(emptyList())
        every { urlChecker.isValidUrl(any()) }.returns(false)

        val restrictions = getMailSettingsBundle()
        val entry = getMailRestrictionEntry()


        updater.update(restrictions, entry)


        verifyProvisioningMailSettings(
            expectedEmail = NEW_EMAIL,
            expectedIncomingPort = NEW_PORT,
            expectedIncomingServer = null,
            expectedConnectionSecurity = NEW_SECURITY_TYPE,
            expectedUserName = NEW_USER_NAME,
            expectedAuthType = security.pEp.mdm.AuthType.PLAIN,
            expectedOAuthProvider = OAuthProviderType.GOOGLE
        )
    }

    @Test
    fun `update() keeps the old account mail settings server if UrlChecker fails to check it`() {
        stubInitialServerSettings()
        stubAccountSettersAndGetters()
        every { urlChecker.isValidUrl(any()) }.returns(false)

        val restrictions = getMailSettingsBundle()
        val entry = getMailRestrictionEntry()


        updater.update(restrictions, entry)


        verifyAccountMailSettings(
            expectedEmail = NEW_EMAIL,
            expectedIncomingPort = NEW_PORT,
            expectedIncomingServer = OLD_SERVER,
            expectedConnectionSecurity = NEW_SECURITY_TYPE,
            expectedUserName = NEW_USER_NAME,
            expectedAuthType = AuthType.PLAIN,
            expectedOAuthProvider = OAuthProviderType.GOOGLE,
        )
    }

    @Test
    fun `update() keeps the old provisioning email address if provided email address has no right format`() {
        stubInitialServerSettings()
        every { preferences.accounts }.returns(emptyList())

        val restrictions = getMailSettingsBundle(email = "malformedEmail")
        val entry = getMailRestrictionEntry()


        updater.update(restrictions, entry)


        verifyProvisioningMailSettings(
            expectedEmail = null,
            expectedIncomingPort = NEW_PORT,
            expectedIncomingServer = NEW_SERVER,
            expectedConnectionSecurity = NEW_SECURITY_TYPE,
            expectedUserName = NEW_USER_NAME,
            expectedAuthType = security.pEp.mdm.AuthType.PLAIN,
            expectedOAuthProvider = OAuthProviderType.GOOGLE
        )
    }

    @Test
    fun `update() keeps the old account email address if provided email address has no right format`() {
        stubInitialServerSettings()
        stubAccountSettersAndGetters()

        val restrictions = getMailSettingsBundle(email = "malformedEmail")
        val entry = getMailRestrictionEntry()


        updater.update(restrictions, entry)


        verifyAccountMailSettings(
            expectedEmail = OLD_EMAIL,
            expectedIncomingServer = NEW_SERVER,
            expectedIncomingPort = NEW_PORT,
            expectedConnectionSecurity = NEW_SECURITY_TYPE,
            expectedUserName = NEW_USER_NAME,
            expectedAuthType = AuthType.PLAIN,
            expectedOAuthProvider = OAuthProviderType.GOOGLE,
        )
    }

    @Test
    fun `update() takes the value for local folder size from the provided restrictions`() {
        val resources: Resources = mockk()
        every { resources.getStringArray(R.array.display_count_values) }
            .returns(arrayOf("10", "250"))
        every { k9.resources }.returns(resources)
        val restrictions = Bundle().apply {
            putString(RESTRICTION_ACCOUNT_LOCAL_FOLDER_SIZE, "10")
        }
        val entry = RestrictionEntry(RESTRICTION_ACCOUNT_LOCAL_FOLDER_SIZE, "250")


        updater.update(restrictions, entry)


        verify { account.displayCount = 10 }
    }

    @Test
    fun `update() takes the value for local folder size from the restrictions entry if not provided in restrictions`() {
        val resources: Resources = mockk()
        every { resources.getStringArray(R.array.display_count_values) }
            .returns(arrayOf("10", "250"))
        every { k9.resources }.returns(resources)
        val restrictions = Bundle()
        val entry = RestrictionEntry(RESTRICTION_ACCOUNT_LOCAL_FOLDER_SIZE, "250")


        updater.update(restrictions, entry)


        verify { account.displayCount = 250 }
    }

    @Test
    fun `update() keeps last value if provided value is not valid`() {
        val resources: Resources = mockk()
        every { resources.getStringArray(R.array.display_count_values) }
            .returns(arrayOf("10", "250"))
        every { k9.resources }.returns(resources)
        val restrictions = Bundle().apply {
            putString(RESTRICTION_ACCOUNT_LOCAL_FOLDER_SIZE, "hello")
        }
        val entry = RestrictionEntry(RESTRICTION_ACCOUNT_LOCAL_FOLDER_SIZE, "250")


        updater.update(restrictions, entry)


        verify { account.wasNot(called) }
    }

    private fun stubAccountSettersAndGetters() {
        val oAuthProviderSlot = slot<OAuthProviderType>()
        every { account.oAuthProviderType = capture(oAuthProviderSlot) }.answers {
            every { account.oAuthProviderType }.returns(oAuthProviderSlot.captured)
        }
        val emailSlot = slot<String>()
        every { account.email = capture(emailSlot) }.answers {
            every { account.email }.returns(emailSlot.captured)
        }
    }

    private fun verifyProvisioningMailSettings(
        expectedEmail: String?,
        expectedIncomingPort: Int,
        expectedOutgoingPort: Int = expectedIncomingPort,
        expectedIncomingServer: String?,
        expectedOutgoingServer: String? = expectedIncomingServer,
        expectedConnectionSecurity: ConnectionSecurity?,
        expectedUserName: String?,
        expectedAuthType: security.pEp.mdm.AuthType,
        expectedOAuthProvider: OAuthProviderType?
    ) {
        val slot = slot<AccountMailSettingsProvision>()
        verify {
            provisioningSettings.provisionedMailSettings = capture(slot)
        }

        val provision = slot.captured
        assertEquals(expectedIncomingPort, provision.incoming.port)
        assertEquals(expectedIncomingServer, provision.incoming.server)
        assertEquals(expectedConnectionSecurity, provision.incoming.connectionSecurity)
        assertEquals(expectedUserName, provision.incoming.userName)
        assertEquals(expectedAuthType, provision.incoming.authType)

        assertEquals(expectedOutgoingPort, provision.outgoing.port)
        assertEquals(expectedOutgoingServer, provision.outgoing.server)
        assertEquals(expectedConnectionSecurity, provision.outgoing.connectionSecurity)
        assertEquals(expectedUserName, provision.outgoing.userName)
        assertEquals(expectedAuthType, provision.outgoing.authType)

        assertEquals(expectedOAuthProvider, provisioningSettings.oAuthType)
        assertEquals(expectedEmail, provisioningSettings.email)
    }

    private fun verifyAccountMailSettings(
        expectedEmail: String?,
        expectedIncomingPort: Int,
        expectedOutgoingPort: Int = expectedIncomingPort,
        expectedIncomingServer: String?,
        expectedOutgoingServer: String? = expectedIncomingServer,
        expectedConnectionSecurity: ConnectionSecurity,
        expectedUserName: String?,
        expectedAuthType: AuthType,
        expectedOAuthProvider: OAuthProviderType?
    ) {
        val incomingSettingsSlot = slot<ServerSettings>()
        val outgoingSettingsSlot = slot<ServerSettings>()
        verify {
            RemoteStore.decodeStoreUri("storeUri")
            RemoteStore.createStoreUri(
                capture(incomingSettingsSlot)
            )
            Transport.decodeTransportUri("transportUri")
            Transport.createTransportUri(
                capture(outgoingSettingsSlot)
            )

            account.storeUri = "incomingUri"
            account.transportUri = "outgoingUri"
            account.oAuthProviderType = expectedOAuthProvider
        }

        assertEquals(expectedEmail, account.email)

        val newIncoming = incomingSettingsSlot.captured

        assertEquals(expectedIncomingServer, newIncoming.host)
        assertEquals(expectedIncomingPort, newIncoming.port)
        assertEquals(expectedConnectionSecurity, newIncoming.connectionSecurity)
        assertEquals(expectedUserName, newIncoming.username)
        assertEquals(expectedAuthType, newIncoming.authenticationType)

        val newOutgoing = outgoingSettingsSlot.captured

        assertEquals(expectedOutgoingServer, newOutgoing.host)
        assertEquals(expectedOutgoingPort, newOutgoing.port)
        assertEquals(expectedConnectionSecurity, newOutgoing.connectionSecurity)
        assertEquals(expectedUserName, newOutgoing.username)
        assertEquals(expectedAuthType, newOutgoing.authenticationType)
    }

    private fun getMailSettingsBundle(
        email: String? = NEW_EMAIL,
        authType: AuthType = AuthType.PLAIN,
        oAuthProvider: OAuthProviderType = OAuthProviderType.GOOGLE,
        server: String? = NEW_SERVER,
        username: String? = NEW_USER_NAME,
        security: String? = NEW_SECURITY_TYPE_STRING,
        port: Int = NEW_PORT,
    ): Bundle = Bundle().apply {
        putBundle(
            RESTRICTION_ACCOUNT_MAIL_SETTINGS,
            Bundle().apply {
                putString(RESTRICTION_ACCOUNT_EMAIL_ADDRESS, email)
                putString(RESTRICTION_ACCOUNT_OAUTH_PROVIDER, oAuthProvider.toString())
                putBundle(
                    RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS,
                    bundleOf(
                        RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_SERVER to server,
                        RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_PORT to port,
                        RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_SECURITY_TYPE to security,
                        RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_USER_NAME to username,
                        RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_AUTH_TYPE to authType.toString()
                    )
                )
                putBundle(
                    RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS,
                    bundleOf(
                        RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_SERVER to server,
                        RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_PORT to port,
                        RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_SECURITY_TYPE to security,
                        RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_USER_NAME to username,
                        RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_AUTH_TYPE to authType.toString()
                    )
                )
            }
        )
    }

    private fun getMailRestrictionEntry(): RestrictionEntry = RestrictionEntry.createBundleEntry(
        RESTRICTION_ACCOUNT_MAIL_SETTINGS,
        arrayOf(
            RestrictionEntry(RESTRICTION_ACCOUNT_EMAIL_ADDRESS, DEFAULT_EMAIL),
            RestrictionEntry(
                RESTRICTION_ACCOUNT_OAUTH_PROVIDER,
                DEFAULT_OAUTH_PROVIDER.toString()
            ),
            RestrictionEntry.createBundleEntry(
                RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS,
                arrayOf(
                    RestrictionEntry(
                        RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_SERVER,
                        DEFAULT_SERVER
                    ),
                    RestrictionEntry(
                        RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_PORT,
                        DEFAULT_PORT
                    ),
                    RestrictionEntry(
                        RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_SECURITY_TYPE,
                        DEFAULT_SECURITY_TYPE.toMdmName()
                    ),
                    RestrictionEntry(
                        RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_USER_NAME,
                        DEFAULT_USER_NAME
                    ),
                    RestrictionEntry(
                        RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_AUTH_TYPE,
                        DEFAULT_AUTH_TYPE.toString()
                    )
                )
            ),
            RestrictionEntry.createBundleEntry(
                RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS,
                arrayOf(
                    RestrictionEntry(
                        RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_SERVER,
                        DEFAULT_SERVER
                    ),
                    RestrictionEntry(
                        RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_PORT,
                        DEFAULT_PORT
                    ),
                    RestrictionEntry(
                        RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_SECURITY_TYPE,
                        DEFAULT_SECURITY_TYPE.toMdmName()
                    ),
                    RestrictionEntry(
                        RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_USER_NAME,
                        DEFAULT_USER_NAME
                    ),
                    RestrictionEntry(
                        RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_AUTH_TYPE,
                        DEFAULT_AUTH_TYPE.toString()
                    )
                )
            ),
        )
    )

    private fun getInitialIncomingSettings(): ServerSettings = ServerSettings(
        ServerSettings.Type.IMAP,
        OLD_SERVER,
        OLD_PORT,
        OLD_SECURITY_TYPE,
        OLD_AUTH_TYPE,
        OLD_USER_NAME,
        OLD_PASSWORD,
        OLD_CERTIFICATE_ALIAS
    )

    private fun getInitialOutgoingSettings(): ServerSettings = ServerSettings(
        ServerSettings.Type.SMTP,
        OLD_SERVER,
        OLD_PORT,
        OLD_SECURITY_TYPE,
        OLD_AUTH_TYPE,
        OLD_USER_NAME,
        OLD_PASSWORD,
        OLD_CERTIFICATE_ALIAS
    )

    private fun stubInitialServerSettings(previousOAuthProviderType: OAuthProviderType? = null) {
        val incomingSettings = getInitialIncomingSettings()
        val outgoingSettings = getInitialOutgoingSettings()

        every { RemoteStore.decodeStoreUri(any()) }.returns(incomingSettings)
        every { RemoteStore.createStoreUri(any()) }.returns("incomingUri")
        every { Transport.decodeTransportUri(any()) }.returns(outgoingSettings)
        every { Transport.createTransportUri(any()) }.returns("outgoingUri")
        every { account.storeUri }.returns("storeUri")
        every { account.transportUri }.returns("transportUri")
        every { account.email }.returns("old.email@example.ch")
        every { account.oAuthProviderType }.returns(previousOAuthProviderType)
    }

    private fun ConnectionSecurity.toMdmName(): String = when(this) {
        ConnectionSecurity.NONE -> CONNECTION_SECURITY_NONE
        ConnectionSecurity.STARTTLS_REQUIRED -> CONNECTION_SECURITY_STARTTLS
        ConnectionSecurity.SSL_TLS_REQUIRED -> CONNECTION_SECURITY_SSL_TLS
    }

    companion object {
        private const val OLD_SERVER = "oldServer"
        private const val OLD_PORT = 333
        private val OLD_SECURITY_TYPE = ConnectionSecurity.NONE
        private val OLD_AUTH_TYPE = AuthType.PLAIN
        private const val OLD_USER_NAME = "oldUsername"
        private const val OLD_PASSWORD = "oldPassword"
        private const val OLD_CERTIFICATE_ALIAS = "cert"
        private const val OLD_EMAIL = "old.email@example.ch"

        private const val DEFAULT_SERVER = "serverDefault"
        private const val DEFAULT_PORT = 888
        private val DEFAULT_SECURITY_TYPE = ConnectionSecurity.STARTTLS_REQUIRED
        private const val DEFAULT_USER_NAME = "usernameDefault"
        private const val DEFAULT_EMAIL = "email@default.ch"
        private val DEFAULT_OAUTH_PROVIDER = OAuthProviderType.MICROSOFT
        private val DEFAULT_AUTH_TYPE = AuthType.PLAIN

        private const val NEW_EMAIL = "email@mail.ch"
        private const val NEW_SERVER = "mail.server.host"
        private const val NEW_USER_NAME = "username"
        private const val NEW_SECURITY_TYPE_STRING = "SSL/TLS"
        private val NEW_SECURITY_TYPE = ConnectionSecurity.SSL_TLS_REQUIRED
        private const val NEW_PORT = 999
    }
}
