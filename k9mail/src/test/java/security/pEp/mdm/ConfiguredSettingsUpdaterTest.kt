package security.pEp.mdm

import android.content.RestrictionEntry
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.Transport
import com.fsck.k9.mail.store.RemoteStore
import io.mockk.*
import junit.framework.TestCase.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import security.pEp.provisioning.AccountMailSettingsProvision
import security.pEp.provisioning.ProvisioningSettings
import security.pEp.provisioning.SimpleMailSettings

@RunWith(AndroidJUnit4::class)
class ConfiguredSettingsUpdaterTest {
    private val k9: K9 = mockk(relaxed = true)
    private val preferences: Preferences = mockk()
    private val account: Account = mockk(relaxed = true)
    private val provisioningSettings: ProvisioningSettings = mockk(relaxed = true)
    private var updater = ConfiguredSettingsUpdater(k9, preferences, provisioningSettings)

    @Before
    fun setUp() {
        every { preferences.accounts }.returns(listOf(account))
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
    fun `update() takes the value for provisioning url from the restrictions entry if provided value is blank`() {

        val restrictions = Bundle().apply { putString(RESTRICTION_PROVISIONING_URL, "     ") }
        val entry = RestrictionEntry(RESTRICTION_PROVISIONING_URL, "defaultUrl")


        updater.update(restrictions, entry)


        verify { provisioningSettings.provisioningUrl = "defaultUrl" }
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
                arrayOf(Bundle().apply { putString(RESTRICTION_PEP_FINGERPRINT, "fpr") })
            )
        }
        val entry = RestrictionEntry.createBundleArrayEntry(
            RESTRICTION_PEP_EXTRA_KEYS,
            arrayOf(
                RestrictionEntry.createBundleEntry(
                    RESTRICTION_PEP_EXTRA_KEY,
                    arrayOf(RestrictionEntry(RESTRICTION_PEP_FINGERPRINT, "defaultFpr"))
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
                    arrayOf(RestrictionEntry(RESTRICTION_PEP_FINGERPRINT, "defaultFpr"))
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
                    arrayOf(RestrictionEntry(RESTRICTION_PEP_FINGERPRINT, "  "))
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
    fun `update() takes the value for composition defaults from the restriction entry if not provided in bundle`() {

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
            account.name = "default sender name"
            account.signatureUse = true
            account.signature = "default signature"
            account.isSignatureBeforeQuotedText = false
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
    fun `update() takes the value for default folders from the restriction entry if not provided in bundle`() {

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
    fun `update() takes the value for mail settings from the provided restrictions`() {
        val incomingSettings = ServerSettings(
            ServerSettings.Type.IMAP,
            "oldServer",
            333,
            ConnectionSecurity.NONE,
            AuthType.PLAIN,
            "oldUsername",
            "oldPassword",
            "cert"
        )
        val outgoingSettings = ServerSettings(
            ServerSettings.Type.SMTP,
            "oldServer",
            333,
            ConnectionSecurity.NONE,
            AuthType.PLAIN,
            "oldUsername",
            "oldPassword",
            "cert"
        )

        every { RemoteStore.decodeStoreUri(any()) }.returns(incomingSettings)
        every { RemoteStore.createStoreUri(any()) }.returns("incomingUri")
        every { Transport.decodeTransportUri(any()) }.returns(outgoingSettings)
        every { Transport.createTransportUri(any()) }.returns("outgoingUri")
        every { account.storeUri }.returns("storeUri")
        every { account.transportUri }.returns("transportUri")

        val restrictions = Bundle().apply {
            putBundle(
                RESTRICTION_ACCOUNT_MAIL_SETTINGS,
                Bundle().apply {
                    putString(RESTRICTION_ACCOUNT_EMAIL_ADDRESS, "email")
                    putBundle(
                        RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS,
                        bundleOf(
                            RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_SERVER to "server",
                            RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_PORT to 999,
                            RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_SECURITY_TYPE to "SSL/TLS",
                            RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_USER_NAME to "username"
                        )
                    )
                    putBundle(
                        RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS,
                        bundleOf(
                            RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_SERVER to "server",
                            RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_PORT to 999,
                            RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_SECURITY_TYPE to "SSL/TLS",
                            RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_USER_NAME to "username"
                        )
                    )
                }
            )
        }
        val entry = RestrictionEntry.createBundleEntry(
            RESTRICTION_ACCOUNT_MAIL_SETTINGS,
            arrayOf(
                RestrictionEntry(RESTRICTION_ACCOUNT_EMAIL_ADDRESS, "emailDefault"),
                RestrictionEntry.createBundleEntry(
                    RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS,
                    arrayOf(
                        RestrictionEntry(
                            RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_SERVER,
                            "serverDefault"
                        ),
                        RestrictionEntry(
                            RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_PORT,
                            888
                        ),
                        RestrictionEntry(
                            RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_SECURITY_TYPE,
                            "STARTTLS"
                        ),
                        RestrictionEntry(
                            RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_USER_NAME,
                            "usernameDefault"
                        ),
                    )
                ),
                RestrictionEntry.createBundleEntry(
                    RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS,
                    arrayOf(
                        RestrictionEntry(
                            RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_SERVER,
                            "serverDefault"
                        ),
                        RestrictionEntry(
                            RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_PORT,
                            888
                        ),
                        RestrictionEntry(
                            RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_SECURITY_TYPE,
                            "STARTTLS"
                        ),
                        RestrictionEntry(
                            RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_USER_NAME,
                            "usernameDefault"
                        ),
                    )
                ),
            )
        )


        updater.update(restrictions, entry)


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
            provisioningSettings.email = "email"
            account.email = "email"
            provisioningSettings.provisionedMailSettings = AccountMailSettingsProvision(
                incoming = SimpleMailSettings(
                    999,
                    "server",
                    ConnectionSecurity.SSL_TLS_REQUIRED,
                    "username"
                ),
                outgoing = SimpleMailSettings(
                    999,
                    "server",
                    ConnectionSecurity.SSL_TLS_REQUIRED,
                    "username"
                )
            )
            account.storeUri = "incomingUri"
            account.transportUri = "outgoingUri"
        }

        val newIncoming = incomingSettingsSlot.captured

        assertEquals("server", newIncoming.host)
        assertEquals(999, newIncoming.port)
        assertEquals(ConnectionSecurity.SSL_TLS_REQUIRED, newIncoming.connectionSecurity)
        assertEquals("username", newIncoming.username)

        val newOutgoing = outgoingSettingsSlot.captured

        assertEquals("server", newOutgoing.host)
        assertEquals(999, newOutgoing.port)
        assertEquals(ConnectionSecurity.SSL_TLS_REQUIRED, newOutgoing.connectionSecurity)
        assertEquals("username", newOutgoing.username)
    }

    @Test
    fun `update() takes the value for mail settings from the restriction entry if not provided in bundle`() {
        val incomingSettings = ServerSettings(
            ServerSettings.Type.IMAP,
            "oldServer",
            333,
            ConnectionSecurity.NONE,
            AuthType.PLAIN,
            "oldUsername",
            "oldPassword",
            "cert"
        )
        val outgoingSettings = ServerSettings(
            ServerSettings.Type.SMTP,
            "oldServer",
            333,
            ConnectionSecurity.NONE,
            AuthType.PLAIN,
            "oldUsername",
            "oldPassword",
            "cert"
        )

        every { RemoteStore.decodeStoreUri(any()) }.returns(incomingSettings)
        every { RemoteStore.createStoreUri(any()) }.returns("incomingUri")
        every { Transport.decodeTransportUri(any()) }.returns(outgoingSettings)
        every { Transport.createTransportUri(any()) }.returns("outgoingUri")
        every { account.storeUri }.returns("storeUri")
        every { account.transportUri }.returns("transportUri")

        val restrictions = Bundle()
        val entry = RestrictionEntry.createBundleEntry(
            RESTRICTION_ACCOUNT_MAIL_SETTINGS,
            arrayOf(
                RestrictionEntry(RESTRICTION_ACCOUNT_EMAIL_ADDRESS, "emailDefault"),
                RestrictionEntry.createBundleEntry(
                    RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS,
                    arrayOf(
                        RestrictionEntry(
                            RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_SERVER,
                            "serverDefault"
                        ),
                        RestrictionEntry(
                            RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_PORT,
                            888
                        ),
                        RestrictionEntry(
                            RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_SECURITY_TYPE,
                            "STARTTLS"
                        ),
                        RestrictionEntry(
                            RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_USER_NAME,
                            "usernameDefault"
                        ),
                    )
                ),
                RestrictionEntry.createBundleEntry(
                    RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS,
                    arrayOf(
                        RestrictionEntry(
                            RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_SERVER,
                            "serverDefault"
                        ),
                        RestrictionEntry(
                            RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_PORT,
                            888
                        ),
                        RestrictionEntry(
                            RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_SECURITY_TYPE,
                            "STARTTLS"
                        ),
                        RestrictionEntry(
                            RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_USER_NAME,
                            "usernameDefault"
                        ),
                    )
                ),
            )
        )


        updater.update(restrictions, entry)


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
            provisioningSettings.email = "emailDefault"
            account.email = "emailDefault"
            provisioningSettings.provisionedMailSettings = AccountMailSettingsProvision(
                incoming = SimpleMailSettings(
                    888,
                    "serverDefault",
                    ConnectionSecurity.STARTTLS_REQUIRED,
                    "usernameDefault"
                ),
                outgoing = SimpleMailSettings(
                    888,
                    "serverDefault",
                    ConnectionSecurity.STARTTLS_REQUIRED,
                    "usernameDefault"
                )
            )
            account.storeUri = "incomingUri"
            account.transportUri = "outgoingUri"
        }

        val newIncoming = incomingSettingsSlot.captured

        assertEquals("serverDefault", newIncoming.host)
        assertEquals(888, newIncoming.port)
        assertEquals(ConnectionSecurity.STARTTLS_REQUIRED, newIncoming.connectionSecurity)
        assertEquals("usernameDefault", newIncoming.username)

        val newOutgoing = outgoingSettingsSlot.captured

        assertEquals("serverDefault", newOutgoing.host)
        assertEquals(888, newOutgoing.port)
        assertEquals(ConnectionSecurity.STARTTLS_REQUIRED, newOutgoing.connectionSecurity)
        assertEquals("usernameDefault", newOutgoing.username)
    }

    @Test
    fun `update() does not assign the new settings if they are not valid`() {
        val incomingSettings = ServerSettings(
            ServerSettings.Type.IMAP,
            "oldServer",
            333,
            ConnectionSecurity.NONE,
            AuthType.PLAIN,
            "oldUsername",
            "oldPassword",
            "cert"
        )

        every { RemoteStore.decodeStoreUri(any()) }.returns(incomingSettings)
        every { RemoteStore.createStoreUri(any()) }.returns("incomingUri")
        every { account.storeUri }.returns("storeUri")

        val restrictions = Bundle()
        val entry = RestrictionEntry.createBundleEntry(
            RESTRICTION_ACCOUNT_MAIL_SETTINGS,
            arrayOf(
                RestrictionEntry(RESTRICTION_ACCOUNT_EMAIL_ADDRESS, "emailDefault"),
                RestrictionEntry.createBundleEntry(
                    RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS,
                    arrayOf(
                        RestrictionEntry(
                            RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_SERVER,
                            ""
                        ),
                        RestrictionEntry(
                            RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_PORT,
                            888
                        ),
                        RestrictionEntry(
                            RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_SECURITY_TYPE,
                            "STARTTLS"
                        ),
                    )
                ),
            )
        )


        updater.update(restrictions, entry)


        verify {
            provisioningSettings.email = "emailDefault"
            account.email = "emailDefault"
            provisioningSettings.provisionedMailSettings = AccountMailSettingsProvision(
                incoming = SimpleMailSettings(
                    888,
                    "",
                    ConnectionSecurity.STARTTLS_REQUIRED,
                    ""
                ),
                outgoing = SimpleMailSettings()
            )
        }
        verify(exactly = 0) {
            account.storeUri = any()
            account.transportUri = any()
            RemoteStore.decodeStoreUri(any())
            RemoteStore.createStoreUri(any())
            Transport.decodeTransportUri(any())
            Transport.createTransportUri(any())
        }
    }
}
