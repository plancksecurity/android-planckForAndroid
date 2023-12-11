package security.planck.mdm

import android.content.RestrictionEntry
import android.content.res.Resources
import android.os.Bundle
import androidx.annotation.ArrayRes
import androidx.core.os.bundleOf
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.R
import com.fsck.k9.RobolectricTest
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
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.testutils.ReturnBehavior
import foundation.pEp.jniadapter.Identity
import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import security.planck.network.UrlChecker
import security.planck.provisioning.AccountMailSettingsProvision
import security.planck.provisioning.CONNECTION_SECURITY_NONE
import security.planck.provisioning.CONNECTION_SECURITY_SSL_TLS
import security.planck.provisioning.CONNECTION_SECURITY_STARTTLS
import security.planck.provisioning.ProvisioningSettings
import security.planck.provisioning.toSimpleMailSettings
import java.util.Vector
import javax.inject.Provider

class ConfiguredSettingsUpdaterTest : RobolectricTest() {
    private val k9: K9 = mockk(relaxed = true)
    private val preferences: Preferences = mockk()
    private val urlChecker: UrlChecker = spyk(UrlChecker())
    private val account: Account = mockk(relaxed = true)
    private val provisioningSettings: ProvisioningSettings =
        spyk(ProvisioningSettings(preferences, urlChecker))
    private val folderRepositoryManager: FolderRepositoryManager = mockk()
    private val folderRepository: FolderRepository = mockk()
    private val planck: PlanckProvider = mockk()
    private val planckProviderProvider: Provider<PlanckProvider> = mockk {
        every { get() }.returns(planck)
    }
    private var updater = ConfiguredSettingsUpdater(
        k9,
        preferences,
        planckProviderProvider,
        urlChecker,
        folderRepositoryManager,
        provisioningSettings
    )

    private val defaultImportMediaKeyBehaviors: MutableMap<String, ReturnBehavior<Vector<Identity>>> =
        mutableMapOf(
            KEY_MATERIAL_1 to ReturnBehavior.Return(
                Vector<Identity>(2).apply {
                    add(
                        Identity().apply {
                            this.fpr = KEY_FPR_1
                            this.address = MEDIA_KEY_PATTERN_1
                        }
                    )
                    add(
                        Identity().apply {
                            this.fpr = KEY_FPR_1
                            this.address = MEDIA_KEY_PATTERN_1
                        }
                    )
                }
            ),
            KEY_MATERIAL_2 to ReturnBehavior.Return(
                Vector<Identity>(2).apply {
                    add(
                        Identity().apply {
                            this.fpr = KEY_FPR_2
                            this.address = MEDIA_KEY_PATTERN_2
                        }
                    )
                    add(
                        Identity().apply {
                            this.fpr = KEY_FPR_2
                            this.address = MEDIA_KEY_PATTERN_2
                        }
                    )
                }
            ),
        )

    private val defaultImportExtraKeyBehaviors: MutableMap<String, ReturnBehavior<Vector<String>>> =
        mutableMapOf(
            KEY_MATERIAL_1 to ReturnBehavior.Return(
                Vector<String>(2).apply {
                    add(KEY_FPR_1)
                }
            ),
            KEY_MATERIAL_2 to ReturnBehavior.Return(
                Vector<String>(2).apply {
                    add(KEY_FPR_2)
                }
            ),
        )

    @Before
    fun setUp() {
        every { preferences.accounts }.answers { listOf(account) }
        every { preferences.accountsAllowingIncomplete }.answers { listOf(account) }
        every { account.email }.returns(ACCOUNT_EMAIL)
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


        callUpdate(restrictions, entry)


        verify { provisioningSettings.provisioningUrl = "url" }
    }

    @Test
    fun `update() takes the value for provisioning url from the restrictions entry if not provided in restrictions`() {

        val restrictions = Bundle()
        val entry = RestrictionEntry(RESTRICTION_PROVISIONING_URL, "defaultUrl")


        callUpdate(restrictions, entry)


        verify { provisioningSettings.provisioningUrl = "defaultUrl" }
    }

    @Test
    fun `update() does not set provisioning url if provided value is blank`() {

        val restrictions = Bundle().apply { putString(RESTRICTION_PROVISIONING_URL, "     ") }
        val entry = RestrictionEntry(RESTRICTION_PROVISIONING_URL, "defaultUrl")


        callUpdate(restrictions, entry)


        verify(exactly = 0) { provisioningSettings.provisioningUrl = any() }
    }

    @Test
    fun `update() takes the value for unsecure delivery warning from the provided restrictions`() {
        val restrictions = getUnsecureDeliveryWarningBundle(value = false)
        val entry = getUnsecureDeliveryWarningEntry()


        callUpdate(restrictions, entry)


        verify {
            k9.setPlanckForwardWarningEnabled(
                ManageableSetting(
                    value = false,
                    locked = true
                )
            )
        }
    }

    @Test
    fun `update() takes the value for unsecure delivery warning from the restriction entry if not provided in bundle`() {
        val restrictions = Bundle()
        val entry = getUnsecureDeliveryWarningEntry()


        callUpdate(restrictions, entry)


        verify { k9.setPlanckForwardWarningEnabled(ManageableSetting(value = true, locked = true)) }
    }

    private fun getUnsecureDeliveryWarningBundle(
        value: Boolean = true,
        locked: Boolean = true
    ): Bundle = getSimpleBooleanLockedSettingBundle(
        mainKey = RESTRICTION_PLANCK_UNSECURE_DELIVERY_WARNING,
        valueKey = RESTRICTION_PLANCK_UNSECURE_DELIVERY_WARNING_VALUE,
        lockedKey = RESTRICTION_PLANCK_UNSECURE_DELIVERY_WARNING_LOCKED,
        value = value,
        locked = locked
    )

    private fun getUnsecureDeliveryWarningEntry(
        value: Boolean = true,
        locked: Boolean = true
    ): RestrictionEntry = getSimpleBooleanLockedSettingEntry(
        mainKey = RESTRICTION_PLANCK_UNSECURE_DELIVERY_WARNING,
        valueKey = RESTRICTION_PLANCK_UNSECURE_DELIVERY_WARNING_VALUE,
        lockedKey = RESTRICTION_PLANCK_UNSECURE_DELIVERY_WARNING_LOCKED,
        value = value,
        locked = locked
    )

    @Test
    fun `update() takes the value for enable planck privacy protection from the provided restrictions`() {
        stubInitialServerSettings()
        val restrictions = getEnablePlanckProtectionBundle(value = false)
        val entry = getEnablePlanckProtectionEntry()


        callUpdate(restrictions, entry)


        verify {
            account.setPlanckPrivacyProtection(
                ManageableSetting(
                    value = false,
                    locked = true
                )
            )
        }
    }

    @Test
    fun `update() takes the value for enable planck privacy protection from the restriction entry if not provided in bundle`() {
        stubInitialServerSettings()
        val restrictions = getSingleAccountBundle { putMailSettingsBundle() }
        val entry = getEnablePlanckProtectionEntry(value = true)


        callUpdate(restrictions, entry)


        verify {
            account.setPlanckPrivacyProtection(
                ManageableSetting(
                    value = true,
                    locked = true
                )
            )
        }
    }

    private fun getEnablePlanckProtectionBundle(
        value: Boolean = true,
        locked: Boolean = true
    ): Bundle = getSimpleAccountBooleanLockedSettingBundle(
        mainKey = RESTRICTION_PLANCK_ENABLE_PRIVACY_PROTECTION,
        valueKey = RESTRICTION_PLANCK_ENABLE_PRIVACY_PROTECTION_VALUE,
        lockedKey = RESTRICTION_PLANCK_ENABLE_PRIVACY_PROTECTION_LOCKED,
        value = value,
        locked = locked
    )

    private fun getEnablePlanckProtectionEntry(
        value: Boolean = true,
        locked: Boolean = true
    ): RestrictionEntry = getSimpleAccountBooleanLockedSettingEntry(
        mainKey = RESTRICTION_PLANCK_ENABLE_PRIVACY_PROTECTION,
        valueKey = RESTRICTION_PLANCK_ENABLE_PRIVACY_PROTECTION_VALUE,
        lockedKey = RESTRICTION_PLANCK_ENABLE_PRIVACY_PROTECTION_LOCKED,
        value = value,
        locked = locked
    )

    @Suppress("sameParameterValue")
    private fun getSimpleBooleanLockedSettingBundle(
        mainKey: String,
        valueKey: String,
        lockedKey: String,
        value: Boolean,
        locked: Boolean,
    ): Bundle = Bundle().apply {
        putBundle(
            mainKey,
            createSimpleBooleanLockedSettingBundle(valueKey, value, lockedKey, locked)
        )
    }

    @Suppress("sameParameterValue")
    private fun getSimpleAccountBooleanLockedSettingBundle(
        mainKey: String,
        valueKey: String,
        lockedKey: String,
        value: Boolean,
        locked: Boolean,
    ): Bundle = getAccountsBundle(
        {
            putMailSettingsBundle()
            putSimpleBooleanLockedSettingBundle(mainKey, valueKey, value, lockedKey, locked)
        },
    )

    private fun getAccountsBundle(vararg accountBundles: Bundle.() -> Unit): Bundle =
        Bundle().apply {
            putParcelableArray(
                RESTRICTION_PLANCK_ACCOUNTS_SETTINGS,
                accountBundles.map {
                    Bundle().apply(it)
                }.toTypedArray()
            )
        }

    private fun getSingleAccountBundle(accountBlock: Bundle.() -> Unit): Bundle =
        getAccountsBundle(accountBlock)

    private fun Bundle.putSimpleBooleanLockedSettingBundle(
        mainKey: String,
        valueKey: String,
        value: Boolean,
        lockedKey: String,
        locked: Boolean
    ) {
        putBundle(
            mainKey,
            createSimpleBooleanLockedSettingBundle(valueKey, value, lockedKey, locked)
        )
    }

    private fun createSimpleBooleanLockedSettingBundle(
        valueKey: String,
        value: Boolean,
        lockedKey: String,
        locked: Boolean
    ) = Bundle().apply {
        putBoolean(valueKey, value)
        putBoolean(lockedKey, locked)
    }

    private fun getSimpleBooleanLockedSettingEntry(
        mainKey: String,
        valueKey: String,
        lockedKey: String,
        value: Boolean,
        locked: Boolean,
    ): RestrictionEntry =
        RestrictionEntry.createBundleEntry(
            mainKey,
            arrayOf(
                RestrictionEntry(valueKey, value),
                RestrictionEntry(lockedKey, locked)
            )
        )

    @Suppress("sameParameterValue")
    private fun getSimpleAccountBooleanLockedSettingEntry(
        mainKey: String,
        valueKey: String,
        lockedKey: String,
        value: Boolean,
        locked: Boolean,
    ): RestrictionEntry = getAccountsManifestEntry(
        getMailRestrictionEntry(),
        getSimpleBooleanLockedSettingEntry(mainKey, valueKey, lockedKey, value, locked)
    )

    private fun getAccountsManifestEntry(
        vararg accountDefaultContents: RestrictionEntry
    ): RestrictionEntry = RestrictionEntry.createBundleArrayEntry(
        RESTRICTION_PLANCK_ACCOUNTS_SETTINGS,
        arrayOf(
            RestrictionEntry.createBundleEntry(
                RESTRICTION_PLANCK_ACCOUNT_SETTINGS,
                accountDefaultContents
            )
        )
    )


    @Test
    fun `update() takes the value for extra keys from the provided restrictions`() {
        stubImportExtraKeyBehavior(planck, defaultImportExtraKeyBehaviors)
        val restrictions = getExtraKeysBundle()
        val entry = getExtraKeysRestrictionEntry()


        callUpdate(restrictions, entry)


        verify {
            planck.importExtraKey(KEY_MATERIAL_1.toByteArray())
            planck.importExtraKey(KEY_MATERIAL_2.toByteArray())
            K9.setMasterKeys(
                setOf(
                    KEY_FPR_1,
                    KEY_FPR_2
                )
            )
        }
    }

    @Test
    fun `update() adds linebreaks before the key material body start if key material matches criteria`() {
        val restrictions = getExtraKeysBundle(
            material1 = REAL_KEY_MATERIAL_START
        )
        val entry = getExtraKeysRestrictionEntry()


        callUpdate(restrictions, entry)


        verify {
            planck.importExtraKey(REAL_KEY_MATERIAL_START_UPDATED.trim().toByteArray())
            planck.importExtraKey(KEY_MATERIAL_2.toByteArray())
        }
    }

    @Test
    fun `update() does not add linebreaks before the key material body start if key material does not match criteria`() {
        val restrictions = getExtraKeysBundle(
            material1 = FAKE_KEY_MATERIAL_START
        )
        val entry = getExtraKeysRestrictionEntry()


        callUpdate(restrictions, entry)


        verify {
            planck.importExtraKey(FAKE_KEY_MATERIAL_START.trim().toByteArray())
            planck.importExtraKey(KEY_MATERIAL_2.toByteArray())
        }
    }

    @Test
    fun `update() set extra keys to empty set if not provided in bundle`() {
        val restrictions = Bundle()
        val entry = getExtraKeysRestrictionEntry()


        callUpdate(restrictions, entry)


        verify {
            K9.setMasterKeys(emptySet())
        }
    }

    @Test
    fun `update() sets extra keys with empty set if no extra keys are provided`() {
        val restrictions = Bundle()
        val entry = getExtraKeysRestrictionEntry()


        callUpdate(restrictions, entry)


        verify { K9.setMasterKeys(emptySet()) }
    }

    @Test
    fun `update() ignores extra keys with blank or missing fields`() {
        stubImportExtraKeyBehavior(planck, defaultImportExtraKeyBehaviors)
        val restrictions = getExtraKeysBundle(fpr1 = " ")
        val entry = getExtraKeysRestrictionEntry()


        callUpdate(restrictions, entry)


        verify {
            planck.importExtraKey(KEY_MATERIAL_2.toByteArray())
            K9.setMasterKeys(
                setOf(
                    KEY_FPR_2
                )
            )
        }
    }

    @Test
    fun `update() ignores extra keys with badly formatted fingerprints`() {
        stubImportExtraKeyBehavior(planck, defaultImportExtraKeyBehaviors)
        val restrictions = getExtraKeysBundle(fpr1 = WRONG_FPR)
        val entry = getExtraKeysRestrictionEntry()


        callUpdate(restrictions, entry)


        verify {
            planck.importExtraKey(KEY_MATERIAL_2.toByteArray())
            K9.setMasterKeys(
                setOf(
                    KEY_FPR_2
                )
            )
        }
    }

    @Test
    fun `update() does not set extra keys if all keys are blank or have errors`() {
        stubImportExtraKeyBehavior(planck, defaultImportExtraKeyBehaviors)
        val restrictions = getExtraKeysBundle(
            fpr1 = " ",
            fpr2 = WRONG_FPR
        )
        val entry = getExtraKeysRestrictionEntry()


        callUpdate(restrictions, entry)


        verify(exactly = 0) {
            planck.importExtraKey(any())
            K9.setMasterKeys(any())
        }
    }

    @Test
    fun `update() ignores extra keys for which PlanckProvider returns bad key import result`() {
        stubImportExtraKeyBehavior(
            planck,
            defaultImportExtraKeyBehaviors.apply {
                this[KEY_MATERIAL_1] = ReturnBehavior.Return(null)
            }
        )
        val restrictions = getExtraKeysBundle()
        val entry = getExtraKeysRestrictionEntry()


        callUpdate(restrictions, entry)


        verify {
            planck.importExtraKey(KEY_MATERIAL_2.toByteArray())
            K9.setMasterKeys(
                setOf(
                    KEY_FPR_2
                )
            )
        }
    }

    @Test
    fun `update() ignores extra keys for which PlanckProvider throws an exception`() {
        stubImportExtraKeyBehavior(
            planck,
            defaultImportExtraKeyBehaviors.apply {
                this[KEY_MATERIAL_1] = ReturnBehavior.Throw(RuntimeException())
            }
        )
        val restrictions = getExtraKeysBundle()
        val entry = getExtraKeysRestrictionEntry()


        callUpdate(restrictions, entry)


        verify {
            planck.importExtraKey(KEY_MATERIAL_2.toByteArray())
            K9.setMasterKeys(
                setOf(
                    KEY_FPR_2
                )
            )
        }
    }

    @Test
    fun `update() takes the value for media keys from the provided restrictions`() {
        stubImportKeyBehavior(planck)
        val restrictions = getMediaKeysBundle()
        val entry = getMediaKeysRestrictionEntry()


        callUpdate(restrictions, entry)


        verify {
            planck.importKey(KEY_MATERIAL_1.toByteArray())
            planck.importKey(KEY_MATERIAL_2.toByteArray())
            K9.setMediaKeys(
                setOf(
                    MediaKey(
                        MEDIA_KEY_PATTERN_1,
                        KEY_FPR_1
                    ),
                    MediaKey(
                        MEDIA_KEY_PATTERN_2,
                        KEY_FPR_2
                    )
                )
            )
        }
    }

    @Test
    fun `update() set media keys to null if not provided in bundle`() {

        val restrictions = Bundle()
        val entry = getMediaKeysRestrictionEntry()


        callUpdate(restrictions, entry)


        verify {
            K9.setMediaKeys(null)
        }
    }

    @Test
    fun `update() sets media keys with null if no media keys are provided`() {
        val restrictions = Bundle()
        val entry = getMediaKeysRestrictionEntry()


        callUpdate(restrictions, entry)


        verify { K9.setMediaKeys(null) }
    }

    @Test
    fun `update() ignores media keys with blank or missing fields`() {
        stubImportKeyBehavior(planck)
        val restrictions = getMediaKeysBundle(pattern1 = " ")
        val entry = getMediaKeysRestrictionEntry()


        callUpdate(restrictions, entry)


        verify {
            planck.importKey(KEY_MATERIAL_2.toByteArray())
            K9.setMediaKeys(
                setOf(
                    MediaKey(
                        MEDIA_KEY_PATTERN_2,
                        KEY_FPR_2
                    )
                )
            )
        }
    }

    @Test
    fun `update() ignores media keys with badly formated fingerprints`() {
        stubImportKeyBehavior(planck)
        val restrictions = getMediaKeysBundle(fpr1 = WRONG_FPR)
        val entry = getMediaKeysRestrictionEntry()


        callUpdate(restrictions, entry)


        verify {
            planck.importKey(KEY_MATERIAL_2.toByteArray())
            K9.setMediaKeys(
                setOf(
                    MediaKey(
                        MEDIA_KEY_PATTERN_2,
                        KEY_FPR_2
                    )
                )
            )
        }
    }

    @Test
    fun `update() does not set media keys if all keys are blank or have errors`() {
        stubImportKeyBehavior(planck)
        val restrictions = getMediaKeysBundle(
            pattern1 = " ",
            fpr2 = WRONG_FPR
        )
        val entry = getMediaKeysRestrictionEntry()


        callUpdate(restrictions, entry)


        verify(exactly = 0) {
            planck.importKey(any())
            K9.setMediaKeys(any())
        }
    }

    @Test
    fun `update() ignores media keys for which PlanckProvider returns bad key import result`() {
        stubImportKeyBehavior(
            planck,
            defaultImportMediaKeyBehaviors.apply {
                this[KEY_MATERIAL_1] = ReturnBehavior.Return(null)
            }
        )
        val restrictions = getMediaKeysBundle()
        val entry = getMediaKeysRestrictionEntry()


        callUpdate(restrictions, entry)


        verify {
            planck.importKey(KEY_MATERIAL_2.toByteArray())
            K9.setMediaKeys(
                setOf(
                    MediaKey(
                        MEDIA_KEY_PATTERN_2,
                        KEY_FPR_2
                    )
                )
            )
        }
    }

    @Test
    fun `update() ignores media keys for which PlanckProvider throws an exception`() {
        stubImportKeyBehavior(
            planck,
            defaultImportMediaKeyBehaviors.apply {
                this[KEY_MATERIAL_1] = ReturnBehavior.Throw(RuntimeException())
            }
        )
        val restrictions = getMediaKeysBundle()
        val entry = getMediaKeysRestrictionEntry()


        callUpdate(restrictions, entry)


        verify {
            planck.importKey(KEY_MATERIAL_2.toByteArray())
            K9.setMediaKeys(
                setOf(
                    MediaKey(
                        MEDIA_KEY_PATTERN_2,
                        KEY_FPR_2
                    )
                )
            )
        }
    }

    private fun getExtraKeysRestrictionEntry() = RestrictionEntry.createBundleArrayEntry(
        RESTRICTION_PLANCK_EXTRA_KEYS,
        arrayOf(
            RestrictionEntry.createBundleEntry(
                RESTRICTION_PLANCK_EXTRA_KEY,
                arrayOf(
                    RestrictionEntry(
                        RESTRICTION_PLANCK_EXTRA_KEY_FINGERPRINT,
                        ""
                    ),
                    RestrictionEntry(
                        RESTRICTION_PLANCK_EXTRA_KEY_MATERIAL,
                        ""
                    )
                )
            )
        )
    )

    private fun getExtraKeysBundle(
        fpr1: String = KEY_FPR_1,
        fpr2: String = KEY_FPR_2,
        material1: String = KEY_MATERIAL_1,
        material2: String = KEY_MATERIAL_2,
    ) = Bundle().apply {
        putParcelableArray(
            RESTRICTION_PLANCK_EXTRA_KEYS,
            arrayOf(
                bundleOf(
                    RESTRICTION_PLANCK_EXTRA_KEY_FINGERPRINT to fpr1,
                    RESTRICTION_PLANCK_EXTRA_KEY_MATERIAL to material1
                ),
                bundleOf(
                    RESTRICTION_PLANCK_EXTRA_KEY_FINGERPRINT to fpr2,
                    RESTRICTION_PLANCK_EXTRA_KEY_MATERIAL to material2
                ),
            )
        )
    }

    private fun getMediaKeysRestrictionEntry() = RestrictionEntry.createBundleArrayEntry(
        RESTRICTION_PLANCK_MEDIA_KEYS,
        arrayOf(
            RestrictionEntry.createBundleEntry(
                RESTRICTION_PLANCK_MEDIA_KEY,
                arrayOf(
                    RestrictionEntry(
                        RESTRICTION_PLANCK_MEDIA_KEY_ADDRESS_PATTERN,
                        ""
                    ),
                    RestrictionEntry(
                        RESTRICTION_PLANCK_MEDIA_KEY_FINGERPRINT,
                        ""
                    ),
                    RestrictionEntry(
                        RESTRICTION_PLANCK_MEDIA_KEY_MATERIAL,
                        ""
                    )
                )
            )
        )
    )

    private fun getMediaKeysBundle(
        pattern1: String = MEDIA_KEY_PATTERN_1,
        pattern2: String = MEDIA_KEY_PATTERN_2,
        fpr1: String = KEY_FPR_1,
        fpr2: String = KEY_FPR_2,
    ) = Bundle().apply {
        putParcelableArray(
            RESTRICTION_PLANCK_MEDIA_KEYS,
            arrayOf(
                bundleOf(
                    RESTRICTION_PLANCK_MEDIA_KEY_ADDRESS_PATTERN to pattern1,
                    RESTRICTION_PLANCK_MEDIA_KEY_FINGERPRINT to fpr1,
                    RESTRICTION_PLANCK_MEDIA_KEY_MATERIAL to KEY_MATERIAL_1
                ),
                bundleOf(
                    RESTRICTION_PLANCK_MEDIA_KEY_ADDRESS_PATTERN to pattern2,
                    RESTRICTION_PLANCK_MEDIA_KEY_FINGERPRINT to fpr2,
                    RESTRICTION_PLANCK_MEDIA_KEY_MATERIAL to KEY_MATERIAL_2
                ),
            )
        )
    }

    fun stubImportKeyBehavior(
        planck: PlanckProvider,
        material: String,
        behavior: ReturnBehavior<Vector<Identity>>
    ) {
        every { planck.importKey(material.toByteArray()) }.answers {
            when (behavior) {
                is ReturnBehavior.Return -> behavior.value!!
                is ReturnBehavior.Throw -> throw behavior.e
            }
        }
    }

    private fun stubImportKeyBehavior(
        planck: PlanckProvider,
        behaviors: MutableMap<String, ReturnBehavior<Vector<Identity>>> = defaultImportMediaKeyBehaviors
    ) {
        val keySlot = mutableListOf<ByteArray>()
        every { planck.importKey(capture(keySlot)) }.answers {
            val material = keySlot.last()
            behaviors[material.decodeToString()]!!.let { behavior ->
                when (behavior) {
                    is ReturnBehavior.Return -> behavior.value!!
                    is ReturnBehavior.Throw -> throw behavior.e
                }
            }
        }
    }

    private fun stubImportExtraKeyBehavior(
        planck: PlanckProvider,
        behaviors: Map<String, ReturnBehavior<Vector<String>>> = defaultImportExtraKeyBehaviors
    ) {
        val keySlot = mutableListOf<ByteArray>()
        every { planck.importExtraKey(capture(keySlot)) }.answers {
            val material = keySlot.last()
            behaviors[material.decodeToString()]!!.let { behavior ->
                when (behavior) {
                    is ReturnBehavior.Return -> behavior.value!!
                    is ReturnBehavior.Throw -> throw behavior.e
                }
            }
        }
    }

    @Test
    fun `update() takes the value for account composition defaults from the provided restrictions`() {
        val restrictions = getAccountsBundle(
            {
                putMailSettingsBundle()
                putCompositionDefaultsBundle()
            }
        )
        val entry = getAccountsManifestEntry(
            getCompositionDefaultsEntry()
        )


        callUpdate(restrictions, entry)


        verify {
            account.name = NEW_SENDER_NAME
            account.signatureUse = false
            account.signature = NEW_SIGNATURE
            account.isSignatureBeforeQuotedText = true
        }
    }

    @Test
    fun `update() takes the value for account composition defaults for all accounts in the provided restrictions`() {
        val secondAccount = stubSecondAccount()
        val restrictions = getAccountsBundle(
            {
                putMailSettingsBundle(email = ACCOUNT_EMAIL)
                putCompositionDefaultsBundle()
            },
            {
                putMailSettingsBundle(email = SECOND_EMAIL)
                putCompositionDefaultsBundle()
            },
        )
        val entry = getAccountsManifestEntry(
            getCompositionDefaultsEntry()
        )


        callUpdate(restrictions, entry)


        verify {
            account.name = NEW_SENDER_NAME
            account.signatureUse = false
            account.signature = NEW_SIGNATURE
            account.isSignatureBeforeQuotedText = true
        }
        verify {
            secondAccount.name = NEW_SENDER_NAME
            secondAccount.signatureUse = false
            secondAccount.signature = NEW_SIGNATURE
            secondAccount.isSignatureBeforeQuotedText = true
        }
    }

    @Test
    fun `update() does not update account composition defaults sender if false is passed for allowModifyAccountProvisioningSettings`() {
        val restrictions = getAccountsBundle(
            {
                putMailSettingsBundle()
                putCompositionDefaultsBundle()
            }
        )
        val entry = getAccountsManifestEntry(
            getCompositionDefaultsEntry()
        )


        callUpdate(restrictions, entry, allowModifyAccountProvisioningSettings = false)


        verify {
            account.signatureUse = false
            account.signature = NEW_SIGNATURE
            account.isSignatureBeforeQuotedText = true
        }
        verify(exactly = 0) { account.name = any() }
    }

    @Test
    fun `update() takes the value for provisioning sender name from the provided restrictions`() {
        val restrictions = getAccountsBundle(
            {
                putMailSettingsBundle()
                putCompositionDefaultsBundle()
            }
        )
        val entry = getAccountsManifestEntry(
            getCompositionDefaultsEntry()
        )


        callUpdate(restrictions, entry)


        verifyProvisioningSenderName(NEW_SENDER_NAME)
    }

    @Test
    fun `update() sets or updates account provisioning sender name for all accounts in the provided restrictions`() {
        stubNoAccounts()

        val restrictions = getAccountsBundle(
            {
                putMailSettingsBundle(email = ACCOUNT_EMAIL)
                putCompositionDefaultsBundle()
            },
            {
                putMailSettingsBundle(email = SECOND_EMAIL)
                putCompositionDefaultsBundle()
            },
        )
        val entry = getAccountsManifestEntry(
            getCompositionDefaultsEntry()
        )


        callUpdate(restrictions, entry)


        verifyProvisioningSenderName(NEW_SENDER_NAME, NEW_SENDER_NAME)
    }

    @Test
    fun `update() takes the value for provisioning sender name even if false is passed for allowModifyAccountProvisioningSettings`() {
        val restrictions = getAccountsBundle(
            {
                putMailSettingsBundle()
                putCompositionDefaultsBundle()
            }
        )
        val entry = getAccountsManifestEntry(
            getCompositionDefaultsEntry()
        )


        callUpdate(restrictions, entry, allowModifyAccountProvisioningSettings = false)


        verifyProvisioningSenderName(NEW_SENDER_NAME)
    }

    @Test
    fun `update() takes the value for account composition defaults from the restriction entry if not provided in bundle, and sender name defaults to email`() {
        val restrictions = getAccountsBundle(
            {
                putMailSettingsBundle()
            }
        )
        val entry = getAccountsManifestEntry(
            getCompositionDefaultsEntry()
        )


        callUpdate(restrictions, entry)


        verify {
            account.name = ACCOUNT_EMAIL
            account.signatureUse = true
            account.signature = DEFAULT_SIGNATURE
            account.isSignatureBeforeQuotedText = false
        }
    }

    @Test
    fun `update() uses null as default for provisioning sender name if not provided in bundle - later it will be set to the email as default so there is no hardcoded default`() {
        val restrictions = getAccountsBundle(
            {
                putMailSettingsBundle()
            }
        )
        val entry = getAccountsManifestEntry(
            getCompositionDefaultsEntry()
        )


        callUpdate(restrictions, entry)


        verifyProvisioningSenderName(null)
    }

    @Test
    fun `update() keeps previous composition settings values that are not valid`() {
        val restrictions = getAccountsBundle(
            {
                putMailSettingsBundle()
                putCompositionDefaultsBundle(
                    senderName = "",
                    signature = "    "
                )
            }
        )

        val entry = getAccountsManifestEntry(
            getCompositionDefaultsEntry()
        )


        callUpdate(restrictions, entry)


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
    fun `update() accepts nearly any string value for provisioning sender name`() { // TODO: Verify later on it is defaulted to email in this case
        val restrictions = getAccountsBundle(
            {
                putMailSettingsBundle()
                putCompositionDefaultsBundle(
                    senderName = "",
                    signature = "    "
                )
            }
        )

        val entry = getAccountsManifestEntry(
            getCompositionDefaultsEntry()
        )


        callUpdate(restrictions, entry)


        verifyProvisioningSenderName("")
    }

    private fun Bundle.putCompositionDefaultsBundle(
        senderName: String? = NEW_SENDER_NAME,
        useSignature: Boolean = false,
        signature: String? = NEW_SIGNATURE,
        signatureBeforeQuotedMessage: Boolean = true,
    ) {
        putBundle(
            RESTRICTION_ACCOUNT_COMPOSITION_DEFAULTS,
            Bundle().apply {
                putString(RESTRICTION_ACCOUNT_COMPOSITION_SENDER_NAME, senderName)
                putBoolean(RESTRICTION_ACCOUNT_COMPOSITION_USE_SIGNATURE, useSignature)
                putString(RESTRICTION_ACCOUNT_COMPOSITION_SIGNATURE, signature)
                putBoolean(
                    RESTRICTION_ACCOUNT_COMPOSITION_SIGNATURE_BEFORE_QUOTED_MESSAGE,
                    signatureBeforeQuotedMessage
                )
            }
        )
    }

    private fun getCompositionDefaultsEntry(): RestrictionEntry =
        RestrictionEntry.createBundleEntry(
            RESTRICTION_ACCOUNT_COMPOSITION_DEFAULTS,
            arrayOf(
                RestrictionEntry(
                    RESTRICTION_ACCOUNT_COMPOSITION_SENDER_NAME,
                    DEFAULT_SENDER_NAME
                ),
                RestrictionEntry(
                    RESTRICTION_ACCOUNT_COMPOSITION_USE_SIGNATURE,
                    true
                ),
                RestrictionEntry(
                    RESTRICTION_ACCOUNT_COMPOSITION_SIGNATURE,
                    DEFAULT_SIGNATURE
                ),
                RestrictionEntry(
                    RESTRICTION_ACCOUNT_COMPOSITION_SIGNATURE_BEFORE_QUOTED_MESSAGE,
                    false
                ),
            )
        )

    @Test
    fun `update() takes the value for default folders from the provided restrictions`() {
        stubInitialServerSettings()
        stubAccountSettersAndGetters()
        val restrictions = getAccountsBundle(
            {
                putMailSettingsBundle()
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
        )

        val entry = getAccountsManifestEntry(
            RestrictionEntry.createBundleEntry(
                RESTRICTION_ACCOUNT_DEFAULT_FOLDERS,
                arrayOf(
                    RestrictionEntry(RESTRICTION_ACCOUNT_ARCHIVE_FOLDER, "archiveDefault"),
                    RestrictionEntry(RESTRICTION_ACCOUNT_DRAFTS_FOLDER, "draftsDefault"),
                    RestrictionEntry(RESTRICTION_ACCOUNT_SENT_FOLDER, "sentDefault"),
                    RestrictionEntry(RESTRICTION_ACCOUNT_SPAM_FOLDER, "spamDefault"),
                    RestrictionEntry(RESTRICTION_ACCOUNT_TRASH_FOLDER, "trashDefault")
                )
            )
        )


        callUpdate(restrictions, entry)


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
        stubInitialServerSettings()
        stubAccountSettersAndGetters()
        val restrictions = getAccountsBundle(
            {
                putMailSettingsBundle()
            }
        )

        val entry = getAccountsManifestEntry(
            RestrictionEntry.createBundleEntry(
                RESTRICTION_ACCOUNT_DEFAULT_FOLDERS,
                arrayOf(
                    RestrictionEntry(RESTRICTION_ACCOUNT_ARCHIVE_FOLDER, "archiveDefault"),
                    RestrictionEntry(RESTRICTION_ACCOUNT_DRAFTS_FOLDER, "draftsDefault"),
                    RestrictionEntry(RESTRICTION_ACCOUNT_SENT_FOLDER, "sentDefault"),
                    RestrictionEntry(RESTRICTION_ACCOUNT_SPAM_FOLDER, "spamDefault"),
                    RestrictionEntry(RESTRICTION_ACCOUNT_TRASH_FOLDER, "trashDefault")
                )
            )
        )


        callUpdate(restrictions, entry)


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
        stubInitialServerSettings()
        stubAccountSettersAndGetters()
        val restrictions = getAccountsBundle(
            {
                putMailSettingsBundle()
            }
        )

        val entry = getAccountsManifestEntry(
            RestrictionEntry.createBundleEntry(
                RESTRICTION_ACCOUNT_DEFAULT_FOLDERS,
                arrayOf(
                    RestrictionEntry(RESTRICTION_ACCOUNT_ARCHIVE_FOLDER, "   "),
                )
            )
        )


        callUpdate(restrictions, entry)


        verify(exactly = 0) {
            account.archiveFolderName = any()
        }
    }

    @Test
    fun `update() keeps previous folder name if folder does not exist in server`() {
        stubInitialServerSettings()
        stubAccountSettersAndGetters()
        val restrictions = getAccountsBundle(
            {
                putMailSettingsBundle()
                putBundle(
                    RESTRICTION_ACCOUNT_DEFAULT_FOLDERS,
                    Bundle().apply {
                        putString(RESTRICTION_ACCOUNT_ARCHIVE_FOLDER, "unknown folder")
                    }
                )
            }
        )

        val entry = getAccountsManifestEntry(
            RestrictionEntry.createBundleEntry(
                RESTRICTION_ACCOUNT_DEFAULT_FOLDERS,
                arrayOf(
                    RestrictionEntry(RESTRICTION_ACCOUNT_ARCHIVE_FOLDER, "   "),
                )
            )
        )


        callUpdate(restrictions, entry)


        verify(exactly = 0) {
            account.archiveFolderName = any()
        }
    }

    @Test
    fun `update() takes the value for provisioning mail settings from the provided restrictions`() {
        stubInitialServerSettings()
        stubAccountSettersAndGetters()

        val restrictions = getAccountsBundle(
            { putMailSettingsBundle() }
        )
        val entry = getAccountsManifestEntry(
            getMailRestrictionEntry()
        )


        callUpdate(restrictions, entry)


        verifyProvisioningMailSettings(
            expectedEmail = ACCOUNT_EMAIL,
            expectedIncomingPort = NEW_PORT,
            expectedIncomingServer = NEW_SERVER,
            expectedConnectionSecurity = NEW_SECURITY_TYPE,
            expectedUserName = NEW_USER_NAME,
            expectedAuthType = security.planck.mdm.AuthType.PLAIN,
            expectedOAuthProvider = OAuthProviderType.GOOGLE
        )
    }

    @Test
    fun `update() takes the value for account mail settings from the provided restrictions`() {
        stubInitialServerSettings()
        stubAccountSettersAndGetters()

        val restrictions = getAccountsBundle(
            { putMailSettingsBundle() }
        )
        val entry = getAccountsManifestEntry(
            getMailRestrictionEntry()
        )


        callUpdate(restrictions, entry)


        verifyAccountMailSettings(
            expectedEmail = ACCOUNT_EMAIL,
            expectedIncomingPort = NEW_PORT,
            expectedIncomingServer = NEW_SERVER,
            expectedConnectionSecurity = NEW_SECURITY_TYPE,
            expectedUserName = NEW_USER_NAME,
            expectedAuthType = AuthType.PLAIN,
            expectedOAuthProvider = OAuthProviderType.GOOGLE,
        )
    }

    @Test
    fun `update() updates all accounts mail settings from the provided restrictions`() {
        stubInitialServerSettings(account = account)
        stubAccountSettersAndGetters(account)
        val newAccount: Account = stubSecondAccount()

        val restrictions = getAccountsBundle(
            { putMailSettingsBundle(email = ACCOUNT_EMAIL) },
            { putMailSettingsBundle(email = SECOND_EMAIL) }
        )
        val entry = getAccountsManifestEntry(
            getMailRestrictionEntry()
        )


        callUpdate(restrictions, entry)


        verifyAccountMailSettings(
            AccountMailSettingsVerification(
                expectedEmail = ACCOUNT_EMAIL,
                expectedIncomingPort = NEW_PORT,
                expectedIncomingServer = NEW_SERVER,
                expectedConnectionSecurity = NEW_SECURITY_TYPE,
                expectedUserName = NEW_USER_NAME,
                expectedAuthType = AuthType.PLAIN,
                expectedOAuthProvider = OAuthProviderType.GOOGLE,
                account = account
            ),
            AccountMailSettingsVerification(
                expectedEmail = SECOND_EMAIL,
                expectedIncomingPort = NEW_PORT,
                expectedIncomingServer = NEW_SERVER,
                expectedConnectionSecurity = NEW_SECURITY_TYPE,
                expectedUserName = NEW_USER_NAME,
                expectedAuthType = AuthType.PLAIN,
                expectedOAuthProvider = OAuthProviderType.GOOGLE,
                account = newAccount
            ),
        )
    }

    private fun stubSecondAccount(): Account {
        val newAccount: Account = mockk(relaxed = true) {
            every { email }.returns(SECOND_EMAIL)
        }
        stubInitialServerSettings(account = newAccount)
        stubAccountSettersAndGetters(newAccount)

        every { preferences.accounts }.answers { listOf(account, newAccount) }
        every { preferences.accountsAllowingIncomplete }.answers { listOf(account, newAccount) }
        return newAccount
    }

    @Test
    fun `update() does not update the mail settings for account if false is passed for allowModifyAccountProvisioningSettings`() {
        stubInitialServerSettings()
        stubAccountSettersAndGetters()

        val restrictions = getAccountsBundle(
            { putMailSettingsBundle() }
        )
        val entry = getAccountsManifestEntry(
            getMailRestrictionEntry()
        )


        callUpdate(restrictions, entry, allowModifyAccountProvisioningSettings = false)


        verify(exactly = 0) {
            RemoteStore.createStoreUri(any())
            Transport.createTransportUri(any())
            account.storeUri = any()
            account.transportUri = any()
            account.mandatoryOAuthProviderType = any()
        }
        verify { urlChecker.isValidUrl(NEW_SERVER) }
    }

    @Test
    fun `update() still takes the value for provisioning mail settings from the provided restrictions even if false is passed for allowModifyAccountProvisioningSettings`() {
        stubInitialServerSettings()
        stubAccountSettersAndGetters()

        val restrictions = getAccountsBundle(
            { putMailSettingsBundle() }
        )
        val entry = getAccountsManifestEntry(
            getMailRestrictionEntry()
        )


        callUpdate(restrictions, entry, allowModifyAccountProvisioningSettings = false)


        verifyProvisioningMailSettings(
            expectedEmail = ACCOUNT_EMAIL,
            expectedIncomingPort = NEW_PORT,
            expectedIncomingServer = NEW_SERVER,
            expectedConnectionSecurity = NEW_SECURITY_TYPE,
            expectedUserName = NEW_USER_NAME,
            expectedAuthType = security.planck.mdm.AuthType.PLAIN,
            expectedOAuthProvider = OAuthProviderType.GOOGLE
        )
    }

    @Test
    fun `update() takes the default value for provisioning mail settings from current accounts even if false is passed for allowModifyAccountProvisioningSettings`() {
        stubInitialServerSettings()
        stubAccountSettersAndGetters()

        val restrictions = getAccountsBundle(
            { putMailSettingsBundle() }
        )
        val entry = getAccountsManifestEntry(
            getMailRestrictionEntry()
        )


        callUpdate(restrictions, entry, allowModifyAccountProvisioningSettings = false)



        verify {
            RemoteStore.decodeStoreUri("storeUri")
            Transport.decodeTransportUri("transportUri")
        }
        verifyProvisioningMailSettings(
            expectedEmail = ACCOUNT_EMAIL,
            expectedIncomingPort = NEW_PORT,
            expectedIncomingServer = NEW_SERVER,
            expectedConnectionSecurity = NEW_SECURITY_TYPE,
            expectedUserName = NEW_USER_NAME,
            expectedAuthType = security.planck.mdm.AuthType.PLAIN,
            expectedOAuthProvider = OAuthProviderType.GOOGLE
        )
    }

    @Test
    fun `update() uses specific values for provisioning mail settings when using a Gmail account with OAuth auth type`() {
        stubInitialServerSettings()
        stubAccountSettersAndGetters()

        val restrictions = getAccountsBundle(
            {
                putMailSettingsBundle(
                    oAuthProvider = OAuthProviderType.GOOGLE,
                    authType = AuthType.XOAUTH2
                )
            }
        )
        val entry = getAccountsManifestEntry(
            getMailRestrictionEntry()
        )


        callUpdate(restrictions, entry)


        verifyProvisioningMailSettings(
            expectedEmail = ACCOUNT_EMAIL,
            expectedIncomingPort = GMAIL_INCOMING_PORT,
            expectedOutgoingPort = GMAIL_OUTGOING_PORT,
            expectedIncomingServer = GMAIL_INCOMING_SERVER,
            expectedOutgoingServer = GMAIL_OUTGOING_SERVER,
            expectedConnectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            expectedUserName = ACCOUNT_EMAIL,
            expectedAuthType = security.planck.mdm.AuthType.XOAUTH2,
            expectedOAuthProvider = OAuthProviderType.GOOGLE
        )
    }

    @Test
    fun `update() uses specific values for account mail settings when using a Gmail account with OAuth auth type`() {
        stubInitialServerSettings()
        stubAccountSettersAndGetters()

        val restrictions = getAccountsBundle(
            {
                putMailSettingsBundle(
                    oAuthProvider = OAuthProviderType.GOOGLE,
                    authType = AuthType.XOAUTH2
                )
            }
        )

        val entry = getAccountsManifestEntry(
            getMailRestrictionEntry()
        )


        callUpdate(restrictions, entry)


        verifyAccountMailSettings(
            expectedEmail = ACCOUNT_EMAIL,
            expectedIncomingPort = GMAIL_INCOMING_PORT,
            expectedOutgoingPort = GMAIL_OUTGOING_PORT,
            expectedIncomingServer = GMAIL_INCOMING_SERVER,
            expectedOutgoingServer = GMAIL_OUTGOING_SERVER,
            expectedConnectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            expectedUserName = ACCOUNT_EMAIL,
            expectedAuthType = AuthType.XOAUTH2,
            expectedOAuthProvider = OAuthProviderType.GOOGLE,
        )
    }

    @Test
    fun `update() uses given values for provisioning mail settings when using certificate auth`() {
        stubInitialServerSettings()

        val restrictions = getAccountsBundle(
            {
                putMailSettingsBundle(
                    authType = AuthType.EXTERNAL
                )
            }
        )

        val entry = getAccountsManifestEntry(
            getMailRestrictionEntry()
        )


        callUpdate(restrictions, entry)


        verifyProvisioningMailSettings(
            expectedEmail = ACCOUNT_EMAIL,
            expectedIncomingPort = NEW_PORT,
            expectedIncomingServer = NEW_SERVER,
            expectedConnectionSecurity = NEW_SECURITY_TYPE,
            expectedUserName = NEW_USER_NAME,
            expectedAuthType = security.planck.mdm.AuthType.EXTERNAL,
            expectedOAuthProvider = OAuthProviderType.GOOGLE
        )
    }

    @Test
    fun `update() uses given values for account mail settings when using certificate auth`() {
        stubInitialServerSettings()
        stubAccountSettersAndGetters()
        val restrictions = getAccountsBundle(
            {
                putMailSettingsBundle(
                    authType = AuthType.EXTERNAL
                )
            }
        )

        val entry = getAccountsManifestEntry(
            getMailRestrictionEntry()
        )


        callUpdate(restrictions, entry)


        verifyAccountMailSettings(
            expectedEmail = ACCOUNT_EMAIL,
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
        val restrictions =
            getAccountsBundle({ putMailSettingsBundle(authType = AuthType.CRAM_MD5) })
        val entry = getAccountsManifestEntry(getMailRestrictionEntry())


        callUpdate(restrictions, entry)


        verifyProvisioningMailSettings(
            expectedEmail = ACCOUNT_EMAIL,
            expectedIncomingPort = NEW_PORT,
            expectedIncomingServer = NEW_SERVER,
            expectedConnectionSecurity = NEW_SECURITY_TYPE,
            expectedUserName = NEW_USER_NAME,
            expectedAuthType = security.planck.mdm.AuthType.CRAM_MD5,
            expectedOAuthProvider = OAuthProviderType.GOOGLE
        )
    }

    @Test
    fun `update() uses given values for account mail settings when using encrypted password auth`() {
        stubInitialServerSettings()
        stubAccountSettersAndGetters()

        val restrictions = getAccountsBundle(
            {
                putMailSettingsBundle(
                    authType = AuthType.CRAM_MD5
                )
            }
        )

        val entry = getAccountsManifestEntry(
            getMailRestrictionEntry()
        )


        callUpdate(restrictions, entry)


        verifyAccountMailSettings(
            expectedEmail = ACCOUNT_EMAIL,
            expectedIncomingPort = NEW_PORT,
            expectedIncomingServer = NEW_SERVER,
            expectedConnectionSecurity = NEW_SECURITY_TYPE,
            expectedUserName = NEW_USER_NAME,
            expectedAuthType = AuthType.CRAM_MD5,
            expectedOAuthProvider = OAuthProviderType.GOOGLE,
        )
    }

    @Test
    fun `update() does not process nor add an account provisioning settings entry if account settings or email address are missing - this means removing that account if it's setup`() {
        stubInitialServerSettings()

        val restrictions = getAccountsBundle({ putMailSettingsBundle(email = null) })
        val entry = getAccountsManifestEntry(getMailRestrictionEntry())


        callUpdate(restrictions, entry)


        verify { urlChecker.wasNot(called) }
        assertTrue(provisioningSettings.accountsProvisionList.isEmpty())
    }

    @Test
    fun `update() does not process nor add an account if account settings or email address are missing`() {
        stubInitialServerSettings()
        stubAccountSettersAndGetters()

        val restrictions = getAccountsBundle({ putMailSettingsBundle(email = null) })
        val entry = getAccountsManifestEntry(getMailRestrictionEntry())


        callUpdate(restrictions, entry)


        verify(exactly = 0) {
            RemoteStore.decodeStoreUri(any())
            RemoteStore.createStoreUri(any())
            Transport.decodeTransportUri(any())
            Transport.createTransportUri(any())
        }
        verify { urlChecker.wasNot(called) }
        verify { account.wasNot(called) }
    }

    @Test
    fun `update() keeps the provisioning mail setting values from current accounts for each new value that is not valid`() {
        stubInitialServerSettings()

        val restrictions = getAccountsBundle(
            {
                putMailSettingsBundle(
                    email = ACCOUNT_EMAIL,
                    server = "",
                    username = " ",
                    security = "wrong security",
                    port = -4
                )
            }
        )
        val entry = getAccountsManifestEntry(getMailRestrictionEntry())


        callUpdate(restrictions, entry)


        verifyProvisioningMailSettings(
            expectedEmail = ACCOUNT_EMAIL,
            expectedIncomingPort = OLD_PORT,
            expectedIncomingServer = OLD_SERVER,
            expectedConnectionSecurity = OLD_SECURITY_TYPE,
            expectedUserName = OLD_USER_NAME,
            expectedAuthType = OLD_AUTH_TYPE.toMdmAuthType(),
            expectedOAuthProvider = OAuthProviderType.GOOGLE
        )
    }

    @Test
    fun `update() keeps the old account mail setting values for each new value that is not valid`() {
        stubInitialServerSettings()
        stubAccountSettersAndGetters()

        val restrictions = getAccountsBundle(
            {
                putMailSettingsBundle(
                    email = ACCOUNT_EMAIL,
                    server = "",
                    username = " ",
                    security = "wrong security",
                    port = -4
                )
            }
        )
        val entry = getAccountsManifestEntry(getMailRestrictionEntry())


        callUpdate(restrictions, entry)


        verifyAccountMailSettings(
            expectedEmail = ACCOUNT_EMAIL,
            expectedIncomingPort = OLD_PORT,
            expectedIncomingServer = OLD_SERVER,
            expectedConnectionSecurity = OLD_SECURITY_TYPE,
            expectedUserName = OLD_USER_NAME,
            expectedAuthType = OLD_AUTH_TYPE,
            expectedOAuthProvider = OAuthProviderType.GOOGLE,
        )
    }

    @Test
    fun `update() keeps the mail provisioning settings server from current accounts if it is badly formatted`() {
        stubInitialServerSettings()

        val restrictions = getAccountsBundle({ putMailSettingsBundle(server = WRONG_SERVER) })
        val entry = getAccountsManifestEntry(getMailRestrictionEntry())


        callUpdate(restrictions, entry)


        verifyProvisioningMailSettings(
            expectedEmail = ACCOUNT_EMAIL,
            expectedIncomingPort = NEW_PORT,
            expectedIncomingServer = OLD_SERVER,
            expectedConnectionSecurity = NEW_SECURITY_TYPE,
            expectedUserName = NEW_USER_NAME,
            expectedAuthType = security.planck.mdm.AuthType.PLAIN,
            expectedOAuthProvider = OAuthProviderType.GOOGLE
        )
    }

    @Test
    fun `update() clears previous provisioning data for accounts that are not setup in the app, so wrong values mean default provision mail values which means null server`() {
        stubNoAccounts()

        provisioningSettings.modifyOrAddAccountSettingsByAddress(ACCOUNT_EMAIL) {
            it.provisionedMailSettings = AccountMailSettingsProvision(
                getInitialIncomingSettings().toSimpleMailSettings(),
                getInitialOutgoingSettings().toSimpleMailSettings()
            )
            it.oAuthType = OAuthProviderType.MICROSOFT
        }

        val restrictions = getAccountsBundle({ putMailSettingsBundle(server = WRONG_SERVER) })
        val entry = getAccountsManifestEntry(getMailRestrictionEntry())


        callUpdate(restrictions, entry)


        verifyProvisioningMailSettings(
            expectedEmail = ACCOUNT_EMAIL,
            expectedIncomingPort = NEW_PORT,
            expectedIncomingServer = null,
            expectedConnectionSecurity = NEW_SECURITY_TYPE,
            expectedUserName = NEW_USER_NAME,
            expectedAuthType = security.planck.mdm.AuthType.PLAIN,
            expectedOAuthProvider = OAuthProviderType.GOOGLE
        )
    }

    @Test
    fun `update() clears provisioning data for accounts that do not come in restrictions bundle`() {
        stubNoAccounts()

        provisioningSettings.modifyOrAddAccountSettingsByAddress(ACCOUNT_EMAIL) {
            it.provisionedMailSettings = AccountMailSettingsProvision(
                getInitialIncomingSettings().toSimpleMailSettings(),
                getInitialOutgoingSettings().toSimpleMailSettings()
            )
            it.oAuthType = OAuthProviderType.MICROSOFT
        }

        val restrictions = getAccountsBundle({ putMailSettingsBundle(email = NEW_EMAIL) })
        val entry = getAccountsManifestEntry(getMailRestrictionEntry())


        callUpdate(restrictions, entry)


        verifyProvisioningMailSettings(
            expectedEmail = NEW_EMAIL,
            expectedIncomingPort = NEW_PORT,
            expectedIncomingServer = NEW_SERVER,
            expectedConnectionSecurity = NEW_SECURITY_TYPE,
            expectedUserName = NEW_USER_NAME,
            expectedAuthType = security.planck.mdm.AuthType.PLAIN,
            expectedOAuthProvider = OAuthProviderType.GOOGLE
        )
    }

    @Test
    fun `update() does not clear previous provisioning data for accounts that are not setup in the app nor accounts that do not come in restrictions bundle if false is passed for purgeAccountSettings`() {
        stubNoAccounts()

        provisioningSettings.modifyOrAddAccountSettingsByAddress(ACCOUNT_EMAIL) {
            it.provisionedMailSettings = AccountMailSettingsProvision(
                getInitialIncomingSettings().toSimpleMailSettings(),
                getInitialOutgoingSettings().toSimpleMailSettings()
            )
            it.oAuthType = OAuthProviderType.MICROSOFT
        }

        val restrictions = getAccountsBundle({ putMailSettingsBundle(email = NEW_EMAIL) })
        val entry = getAccountsManifestEntry(getMailRestrictionEntry())


        callUpdate(restrictions, entry, purgeAccountSettings = false)


        verifyProvisioningMailSettings(
            ProvisioningMailSettingsVerification(
                expectedEmail = ACCOUNT_EMAIL,
                expectedIncomingPort = OLD_PORT,
                expectedIncomingServer = OLD_SERVER,
                expectedConnectionSecurity = OLD_SECURITY_TYPE,
                expectedUserName = OLD_USER_NAME,
                expectedAuthType = OLD_AUTH_TYPE.toMdmAuthType(),
                expectedOAuthProvider = OAuthProviderType.MICROSOFT
            ),
            ProvisioningMailSettingsVerification(
                expectedEmail = NEW_EMAIL,
                expectedIncomingPort = NEW_PORT,
                expectedIncomingServer = NEW_SERVER,
                expectedConnectionSecurity = NEW_SECURITY_TYPE,
                expectedUserName = NEW_USER_NAME,
                expectedAuthType = security.planck.mdm.AuthType.PLAIN,
                expectedOAuthProvider = OAuthProviderType.GOOGLE
            ),
        )
    }

    @Test
    fun `update() sets or updates provisioning mail settings for all accounts in restrictions bundle`() {
        stubNoAccounts()

        val restrictions = getAccountsBundle(
            { putMailSettingsBundle(email = ACCOUNT_EMAIL) },
            { putMailSettingsBundle(email = NEW_EMAIL) }
        )
        val entry = getAccountsManifestEntry(getMailRestrictionEntry())


        callUpdate(restrictions, entry)


        verifyProvisioningMailSettings(
            ProvisioningMailSettingsVerification(
                expectedEmail = ACCOUNT_EMAIL,
                expectedIncomingPort = NEW_PORT,
                expectedIncomingServer = NEW_SERVER,
                expectedConnectionSecurity = NEW_SECURITY_TYPE,
                expectedUserName = NEW_USER_NAME,
                expectedAuthType = security.planck.mdm.AuthType.PLAIN,
                expectedOAuthProvider = OAuthProviderType.GOOGLE
            ),
            ProvisioningMailSettingsVerification(
                expectedEmail = NEW_EMAIL,
                expectedIncomingPort = NEW_PORT,
                expectedIncomingServer = NEW_SERVER,
                expectedConnectionSecurity = NEW_SECURITY_TYPE,
                expectedUserName = NEW_USER_NAME,
                expectedAuthType = security.planck.mdm.AuthType.PLAIN,
                expectedOAuthProvider = OAuthProviderType.GOOGLE
            ),
        )
    }

    @Test
    fun `update() keeps the old account mail settings server if if its badly formatted`() {
        stubInitialServerSettings()
        stubAccountSettersAndGetters()

        val restrictions = getAccountsBundle({ putMailSettingsBundle(server = WRONG_SERVER) })
        val entry = getAccountsManifestEntry(getMailRestrictionEntry())


        callUpdate(restrictions, entry)


        verifyAccountMailSettings(
            expectedEmail = ACCOUNT_EMAIL,
            expectedIncomingPort = NEW_PORT,
            expectedIncomingServer = OLD_SERVER,
            expectedConnectionSecurity = NEW_SECURITY_TYPE,
            expectedUserName = NEW_USER_NAME,
            expectedAuthType = AuthType.PLAIN,
            expectedOAuthProvider = OAuthProviderType.GOOGLE,
        )
    }

    @Test
    fun `update() does not process nor add an account provisioning settings entry if provided email address has no right format`() {
        stubInitialServerSettings()

        val restrictions = getAccountsBundle({ putMailSettingsBundle(email = "malformedEmail") })
        val entry = getAccountsManifestEntry(getMailRestrictionEntry())


        callUpdate(restrictions, entry)


        verify { urlChecker.wasNot(called) }
        assertTrue(provisioningSettings.accountsProvisionList.isEmpty())
    }

    @Test
    fun `update() does not process nor add an account if provided email address has no right format`() {
        stubInitialServerSettings()
        stubAccountSettersAndGetters()

        val restrictions = getAccountsBundle({ putMailSettingsBundle(email = "malformedEmail") })
        val entry = getAccountsManifestEntry(getMailRestrictionEntry())


        callUpdate(restrictions, entry)


        verify(exactly = 0) {
            RemoteStore.decodeStoreUri(any())
            RemoteStore.createStoreUri(any())
            Transport.decodeTransportUri(any())
            Transport.createTransportUri(any())
        }
        verify { account.wasNot(called) }
    }

    @Test
    fun `update() takes the value for local folder size from the provided restrictions`() {
        stubResArray(R.array.display_count_values, arrayOf("10", "20", "250"))
        every { account.lockableDisplayCount }.returns(ManageableSetting(20, false))
        val restrictions = getLocalFolderSizeBundle(value = "10")
        val entry = getLocalFolderSizeEntry()


        callUpdate(restrictions, entry)


        verify { account.setDisplayCount(ManageableSetting(10, true)) }
    }

    @Test
    fun `update() takes the value for local folder size from the restrictions entry if not provided in restrictions`() {
        stubResArray(R.array.display_count_values, arrayOf("10", "20", "250"))
        every { account.lockableDisplayCount }.returns(ManageableSetting(20, false))
        val restrictions = getAccountsBundle(
            {
                putMailSettingsBundle()
            }
        )
        val entry = getLocalFolderSizeEntry()


        callUpdate(restrictions, entry)


        verify { account.setDisplayCount(ManageableSetting(250, true)) }
    }

    @Test
    fun `update() keeps last value for local folder size if provided value is not valid`() {
        stubResArray(R.array.display_count_values, arrayOf("10", "20", "250"))
        every { account.lockableDisplayCount }.returns(ManageableSetting(20, false))
        val restrictions = getLocalFolderSizeBundle(value = "hello")
        val entry = getLocalFolderSizeEntry()


        callUpdate(restrictions, entry)


        verify { account.setDisplayCount(ManageableSetting(20, true)) }
    }

    private fun getLocalFolderSizeBundle(
        value: String = "250",
        locked: Boolean = true
    ) = getSimpleAccountStringLockedSettingBundle(
        mainKey = RESTRICTION_ACCOUNT_LOCAL_FOLDER_SIZE,
        valueKey = RESTRICTION_ACCOUNT_LOCAL_FOLDER_SIZE_VALUE,
        lockedKey = RESTRICTION_ACCOUNT_LOCAL_FOLDER_SIZE_LOCKED,
        value = value,
        locked = locked
    )

    private fun getLocalFolderSizeEntry(
        value: String = "250",
        locked: Boolean = true
    ): RestrictionEntry =
        getSimpleAccountStringLockedSettingEntry(
            mainKey = RESTRICTION_ACCOUNT_LOCAL_FOLDER_SIZE,
            valueKey = RESTRICTION_ACCOUNT_LOCAL_FOLDER_SIZE_VALUE,
            lockedKey = RESTRICTION_ACCOUNT_LOCAL_FOLDER_SIZE_LOCKED,
            value = value,
            locked = locked
        )

    @Test
    fun `update() takes the value for audit logs retention time from the provided restrictions`() {
        stubResArray(R.array.audit_log_data_time_retention_values, arrayOf("30", "60", "90"))
        every { k9.auditLogDataTimeRetention }.returns(ManageableSetting(30L, false))
        val restrictions = getAuditLogRetentionBundle()
        val entry = getAuditLogRetentionEntry()


        callUpdate(restrictions, entry)


        verify { k9.auditLogDataTimeRetention = ManageableSetting(90L, true) }
    }

    @Test
    fun `update() takes the value for audit logs retention time from the restrictions entry if not provided in restrictions`() {
        stubResArray(R.array.audit_log_data_time_retention_values, arrayOf("30", "60", "90"))
        every { k9.auditLogDataTimeRetention }.returns(ManageableSetting(60L, true))
        val restrictions = Bundle()
        val entry = getAuditLogRetentionEntry()


        callUpdate(restrictions, entry)


        verify { k9.auditLogDataTimeRetention = ManageableSetting(30L, true) }
    }

    @Test
    fun `update() keeps last value for audit logs retention time if provided value is not valid`() {
        stubResArray(R.array.audit_log_data_time_retention_values, arrayOf("30", "60", "90"))
        every { k9.auditLogDataTimeRetention }.returns(ManageableSetting(30L, false))
        val restrictions = getAuditLogRetentionBundle(value = "hello")
        val entry = getAuditLogRetentionEntry()


        callUpdate(restrictions, entry)


        verify { k9.auditLogDataTimeRetention = ManageableSetting(30L, true) }
    }

    private fun stubResArray(
        @ArrayRes arrayRes: Int,
        values: Array<String>
    ) {
        val resources: Resources = mockk()
        every { resources.getStringArray(arrayRes) }
            .returns(values)
        every { k9.resources }.returns(resources)
    }

    private fun getAuditLogRetentionBundle(
        value: String = "90",
        locked: Boolean = true
    ): Bundle = getSimpleStringLockedSettingBundle(
        mainKey = RESTRICTION_AUDIT_LOG_DATA_TIME_RETENTION,
        valueKey = RESTRICTION_AUDIT_LOG_DATA_TIME_RETENTION_VALUE,
        lockedKey = RESTRICTION_AUDIT_LOG_DATA_TIME_RETENTION_LOCKED,
        value = value,
        locked = locked
    )

    private fun getAuditLogRetentionEntry(
        value: String = "30",
        locked: Boolean = true
    ): RestrictionEntry =
        getSimpleStringLockedSettingEntry(
            mainKey = RESTRICTION_AUDIT_LOG_DATA_TIME_RETENTION,
            valueKey = RESTRICTION_AUDIT_LOG_DATA_TIME_RETENTION_VALUE,
            lockedKey = RESTRICTION_AUDIT_LOG_DATA_TIME_RETENTION_LOCKED,
            value = value,
            locked = locked
        )

    @Test
    fun `update() takes the value for account description from the provided restrictions`() {
        every { account.lockableDescription }.returns(ManageableSetting(null, false))
        val restrictions = getAccountDescriptionBundleWithMailSettings()
        val entry = getAccountDescriptionEntry()


        callUpdate(restrictions, entry)


        verify { account.setDescription(ManageableSetting(NEW_ACCOUNT_DESCRIPTION, true)) }
    }

    @Test
    fun `update() takes the value for account description for all accounts in the provided restrictions`() {
        val secondAccount = stubSecondAccount()
        every { account.lockableDescription }.returns(ManageableSetting(null, false))
        every { secondAccount.lockableDescription }.returns(ManageableSetting(null, false))

        val restrictions = getAccountsBundle(
            {
                putMailSettingsBundle(email = ACCOUNT_EMAIL)
                putAccountDescriptionBundle()
            },
            {
                putMailSettingsBundle(email = SECOND_EMAIL)
                putAccountDescriptionBundle()
            },
        )
        val entry = getAccountDescriptionEntry()


        callUpdate(restrictions, entry)


        verify { account.setDescription(ManageableSetting(NEW_ACCOUNT_DESCRIPTION, true)) }
        verify { secondAccount.setDescription(ManageableSetting(NEW_ACCOUNT_DESCRIPTION, true)) }
    }

    @Test
    fun `update() does not take the value for account description from the provided restrictions if false is passed for allowModifyAccountProvisioningSettings`() {
        every { account.lockableDescription }.returns(ManageableSetting(null, false))
        val restrictions = getAccountDescriptionBundleWithMailSettings()
        val entry = getAccountDescriptionEntry()


        callUpdate(restrictions, entry, allowModifyAccountProvisioningSettings = false)


        verify(exactly = 0) { account.setDescription(any<ManageableSetting<String?>>()) }
    }

    @Test
    fun `update() takes the value for account description in provisioning settings from the provided restrictions`() {
        every { account.lockableDescription }.returns(ManageableSetting(null, false))
        val restrictions = getAccountDescriptionBundleWithMailSettings()
        val entry = getAccountDescriptionEntry()


        callUpdate(restrictions, entry)


        verifyProvisioningDescription(NEW_ACCOUNT_DESCRIPTION)
    }

    @Test
    fun `update() sets or updates account provisioning description for all accounts in the provided restrictions`() {
        stubNoAccounts()

        val restrictions = getAccountsBundle(
            {
                putMailSettingsBundle(email = ACCOUNT_EMAIL)
                putAccountDescriptionBundle()
            },
            {
                putMailSettingsBundle(email = SECOND_EMAIL)
                putAccountDescriptionBundle()
            },
        )
        val entry = getAccountDescriptionEntry()


        callUpdate(restrictions, entry)


        verifyProvisioningDescription(NEW_ACCOUNT_DESCRIPTION, NEW_ACCOUNT_DESCRIPTION)
    }

    @Test
    fun `update() takes the value for account description in provisioning settings from the provided restrictions even if false is passed for allowModifyAccountProvisioningSettings`() {
        every { account.lockableDescription }.returns(ManageableSetting(null, false))
        val restrictions = getAccountDescriptionBundleWithMailSettings()
        val entry = getAccountDescriptionEntry()


        callUpdate(restrictions, entry, allowModifyAccountProvisioningSettings = false)


        verifyProvisioningDescription(NEW_ACCOUNT_DESCRIPTION)
    }

    @Test
    fun `update() takes the value for account description from the account email if not provided in restrictions`() {
        every { account.lockableDescription }.returns(ManageableSetting(null, false))
        val restrictions = getAccountsBundle({ putMailSettingsBundle() })
        val entry = getAccountDescriptionEntry()


        callUpdate(restrictions, entry)


        verify { account.setDescription(ManageableSetting(ACCOUNT_EMAIL, true)) }
    }

    @Test
    fun `update() sets null as default account description in provisioning settings if not provided in restrictions - do not have a default provisioning description because later on it is taken from email`() {
        every { account.lockableDescription }.returns(ManageableSetting(null, false))
        val restrictions = getAccountsBundle({ putMailSettingsBundle() })
        val entry = getAccountDescriptionEntry()


        callUpdate(restrictions, entry)


        verifyProvisioningDescription(null)
    }

    @Test
    fun `update() accepts nearly any value for account description`() { // TODO: check this behavior in the app and contrast with sender name
        every { account.lockableDescription }.returns(ManageableSetting(null, false))
        val restrictions = getAccountDescriptionBundleWithMailSettings("")
        val entry = getAccountDescriptionEntry()


        callUpdate(restrictions, entry)


        verify { account.setDescription(ManageableSetting("", true)) }
    }

    @Test
    fun `update() accepts nearly any value for account description in provisioning settings`() {
        every { account.lockableDescription }.returns(ManageableSetting(null, false))
        val restrictions = getAccountDescriptionBundleWithMailSettings("")
        val entry = getAccountDescriptionEntry()


        callUpdate(restrictions, entry)


        verifyProvisioningDescription("")
    }

    private fun stubNoAccounts() {
        every { preferences.accounts }.answers { emptyList() }
        every { preferences.accountsAllowingIncomplete }.answers { emptyList() }
    }

    private fun callUpdate(
        restrictions: Bundle,
        entry: RestrictionEntry,
        allowModifyAccountProvisioningSettings: Boolean = true,
        purgeAccountSettings: Boolean = true,
    ) {
        updater.update(
            restrictions,
            entry,
            allowModifyAccountProvisioningSettings,
            purgeAccountSettings
        )
    }

    private fun getAccountDescriptionBundleWithMailSettings(
        value: String = NEW_ACCOUNT_DESCRIPTION,
        locked: Boolean = true
    ) = getSimpleAccountStringLockedSettingBundle(
        mainKey = RESTRICTION_ACCOUNT_DESCRIPTION,
        valueKey = RESTRICTION_ACCOUNT_DESCRIPTION_VALUE,
        lockedKey = RESTRICTION_ACCOUNT_DESCRIPTION_LOCKED,
        value = value,
        locked = locked
    )

    private fun Bundle.putAccountDescriptionBundle(
        value: String = NEW_ACCOUNT_DESCRIPTION,
        locked: Boolean = true
    ) {
        putSimpleStringLockerSettingBundle(
            mainKey = RESTRICTION_ACCOUNT_DESCRIPTION,
            valueKey = RESTRICTION_ACCOUNT_DESCRIPTION_VALUE,
            lockedKey = RESTRICTION_ACCOUNT_DESCRIPTION_LOCKED,
            value = value,
            locked = locked
        )
    }

    private fun getAccountDescriptionEntry(
        value: String = "",
        locked: Boolean = true
    ): RestrictionEntry =
        getSimpleAccountStringLockedSettingEntry(
            mainKey = RESTRICTION_ACCOUNT_DESCRIPTION,
            valueKey = RESTRICTION_ACCOUNT_DESCRIPTION_VALUE,
            lockedKey = RESTRICTION_ACCOUNT_DESCRIPTION_LOCKED,
            value = value,
            locked = locked
        )

    @Suppress("sameParameterValue")
    private fun getSimpleStringLockedSettingBundle(
        mainKey: String,
        valueKey: String,
        lockedKey: String,
        value: String,
        locked: Boolean,
    ): Bundle = Bundle().apply {
        putBundle(
            mainKey,
            Bundle().apply {
                putString(valueKey, value)
                putBoolean(lockedKey, locked)
            }
        )
    }

    private fun getSimpleAccountStringLockedSettingBundle(
        mainKey: String,
        valueKey: String,
        lockedKey: String,
        value: String,
        locked: Boolean,
    ): Bundle = getAccountsBundle(
        {
            putMailSettingsBundle()
            putSimpleStringLockerSettingBundle(mainKey, valueKey, value, lockedKey, locked)
        }
    )

    private fun Bundle.putSimpleStringLockerSettingBundle(
        mainKey: String,
        valueKey: String,
        value: String,
        lockedKey: String,
        locked: Boolean
    ) {
        putBundle(
            mainKey,
            Bundle().apply {
                putString(valueKey, value)
                putBoolean(lockedKey, locked)
            }
        )
    }

    @Suppress("sameParameterValue")
    private fun getSimpleStringLockedSettingEntry(
        mainKey: String,
        valueKey: String,
        lockedKey: String,
        value: String,
        locked: Boolean,
    ): RestrictionEntry =
        RestrictionEntry.createBundleEntry(
            mainKey,
            arrayOf(
                RestrictionEntry(valueKey, value),
                RestrictionEntry(lockedKey, locked)
            )
        )

    private fun getSimpleAccountStringLockedSettingEntry(
        mainKey: String,
        valueKey: String,
        lockedKey: String,
        value: String,
        locked: Boolean,
    ): RestrictionEntry =
        getAccountsManifestEntry(
            RestrictionEntry.createBundleEntry(
                mainKey,
                arrayOf(
                    RestrictionEntry(valueKey, value),
                    RestrictionEntry(lockedKey, locked)
                )
            )
        )

    private fun stubAccountSettersAndGetters(
        account: Account = this.account
    ) {
        val oAuthProviderSlot = slot<OAuthProviderType>()
        every { account.mandatoryOAuthProviderType = capture(oAuthProviderSlot) }.answers {
            every { account.mandatoryOAuthProviderType }.returns(oAuthProviderSlot.captured)
        }
        val emailSlot = slot<String>()
        every { account.email = capture(emailSlot) }.answers {
            every { account.email }.returns(emailSlot.captured)
        }
    }

    private fun verifyProvisioningDescription(
        vararg expectedDescription: String?
    ) {
        assertEquals(expectedDescription.size, provisioningSettings.accountsProvisionList.size)
        expectedDescription.forEachIndexed { index, description ->
            assertEquals(
                description,
                provisioningSettings.accountsProvisionList[index].accountDescription
            )
        }
    }

    private fun verifyProvisioningSenderName(
        vararg expectedSenderName: String?
    ) {
        assertEquals(expectedSenderName.size, provisioningSettings.accountsProvisionList.size)
        expectedSenderName.forEachIndexed { index, senderName ->
            assertEquals(
                senderName,
                provisioningSettings.accountsProvisionList[index].senderName
            )
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
        expectedAuthType: security.planck.mdm.AuthType,
        expectedOAuthProvider: OAuthProviderType?
    ) {
        verifyProvisioningMailSettings(
            ProvisioningMailSettingsVerification(
                expectedEmail, expectedIncomingPort, expectedOutgoingPort, expectedIncomingServer,
                expectedOutgoingServer, expectedConnectionSecurity, expectedUserName,
                expectedAuthType, expectedOAuthProvider
            )
        )
    }

    private fun verifyProvisioningMailSettings(
        vararg verifications: ProvisioningMailSettingsVerification,
    ) {
        assertEquals(provisioningSettings.accountsProvisionList.size, verifications.size)
        verifications.forEachIndexed { index, verification ->
            with(verification) {
                with(provisioningSettings.accountsProvisionList[index]) {
                    assertEquals(expectedEmail, email)
                    assertEquals(expectedIncomingPort, provisionedMailSettings?.incoming?.port)
                    assertEquals(expectedOutgoingPort, provisionedMailSettings?.outgoing?.port)
                    assertEquals(expectedIncomingServer, provisionedMailSettings?.incoming?.server)
                    assertEquals(expectedOutgoingServer, provisionedMailSettings?.outgoing?.server)
                    assertEquals(
                        expectedConnectionSecurity,
                        provisionedMailSettings?.incoming?.connectionSecurity
                    )
                    assertEquals(
                        expectedConnectionSecurity,
                        provisionedMailSettings?.outgoing?.connectionSecurity
                    )
                    assertEquals(expectedUserName, provisionedMailSettings?.incoming?.userName)
                    assertEquals(expectedUserName, provisionedMailSettings?.outgoing?.userName)
                    assertEquals(expectedAuthType, provisionedMailSettings?.incoming?.authType)
                    assertEquals(expectedAuthType, provisionedMailSettings?.outgoing?.authType)
                    assertEquals(expectedOAuthProvider, oAuthType)
                }
            }
        }
    }

    private data class ProvisioningMailSettingsVerification(
        val expectedEmail: String?,
        val expectedIncomingPort: Int,
        val expectedOutgoingPort: Int = expectedIncomingPort,
        val expectedIncomingServer: String?,
        val expectedOutgoingServer: String? = expectedIncomingServer,
        val expectedConnectionSecurity: ConnectionSecurity?,
        val expectedUserName: String?,
        val expectedAuthType: security.planck.mdm.AuthType,
        val expectedOAuthProvider: OAuthProviderType?
    )

    private fun verifyAccountMailSettings(
        expectedEmail: String?,
        expectedIncomingPort: Int,
        expectedOutgoingPort: Int = expectedIncomingPort,
        expectedIncomingServer: String?,
        expectedOutgoingServer: String? = expectedIncomingServer,
        expectedConnectionSecurity: ConnectionSecurity,
        expectedUserName: String?,
        expectedAuthType: AuthType,
        expectedOAuthProvider: OAuthProviderType?,
        account: Account = this.account
    ) {
        verifyAccountMailSettings(
            AccountMailSettingsVerification(
                expectedEmail, expectedIncomingPort, expectedOutgoingPort, expectedIncomingServer,
                expectedOutgoingServer, expectedConnectionSecurity, expectedUserName,
                expectedAuthType, expectedOAuthProvider, account
            )
        )
    }

    private fun verifyAccountMailSettings(
        vararg verifications: AccountMailSettingsVerification
    ) {
        assertEquals(verifications.size, preferences.accounts.size)
        val incomingSettingsSlot = mutableListOf<ServerSettings>()
        val outgoingSettingsSlot = mutableListOf<ServerSettings>()
        verifications.forEachIndexed { index, verification ->
            with(verification) {
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
                    account.mandatoryOAuthProviderType = expectedOAuthProvider
                }

                assertEquals(expectedEmail, account.email)

                val newIncoming = incomingSettingsSlot[index]

                assertEquals(expectedIncomingServer, newIncoming.host)
                assertEquals(expectedIncomingPort, newIncoming.port)
                assertEquals(expectedConnectionSecurity, newIncoming.connectionSecurity)
                assertEquals(expectedUserName, newIncoming.username)
                assertEquals(expectedAuthType, newIncoming.authenticationType)

                val newOutgoing = outgoingSettingsSlot[index]

                assertEquals(expectedOutgoingServer, newOutgoing.host)
                assertEquals(expectedOutgoingPort, newOutgoing.port)
                assertEquals(expectedConnectionSecurity, newOutgoing.connectionSecurity)
                assertEquals(expectedUserName, newOutgoing.username)
                assertEquals(expectedAuthType, newOutgoing.authenticationType)
            }
        }
    }

    private data class AccountMailSettingsVerification(
        val expectedEmail: String?,
        val expectedIncomingPort: Int,
        val expectedOutgoingPort: Int = expectedIncomingPort,
        val expectedIncomingServer: String?,
        val expectedOutgoingServer: String? = expectedIncomingServer,
        val expectedConnectionSecurity: ConnectionSecurity?,
        val expectedUserName: String?,
        val expectedAuthType: AuthType,
        val expectedOAuthProvider: OAuthProviderType?,
        val account: Account
    )

    private fun Bundle.putMailSettingsBundle(
        email: String? = ACCOUNT_EMAIL,
        authType: AuthType = AuthType.PLAIN,
        oAuthProvider: OAuthProviderType = OAuthProviderType.GOOGLE,
        server: String? = NEW_SERVER,
        username: String? = NEW_USER_NAME,
        security: String? = NEW_SECURITY_TYPE_STRING,
        port: Int = NEW_PORT,
    ) = apply {
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

    private fun stubInitialServerSettings(
        previousOAuthProviderType: OAuthProviderType? = null,
        account: Account = this.account
    ) {
        val incomingSettings = getInitialIncomingSettings()
        val outgoingSettings = getInitialOutgoingSettings()

        every { RemoteStore.decodeStoreUri(any()) }.returns(incomingSettings)
        every { RemoteStore.createStoreUri(any()) }.returns("incomingUri")
        every { Transport.decodeTransportUri(any()) }.returns(outgoingSettings)
        every { Transport.createTransportUri(any()) }.returns("outgoingUri")
        every { account.storeUri }.returns("storeUri")
        every { account.transportUri }.returns("transportUri")
        every { account.mandatoryOAuthProviderType }.returns(previousOAuthProviderType)
    }

    private fun ConnectionSecurity.toMdmName(): String = when (this) {
        ConnectionSecurity.NONE -> CONNECTION_SECURITY_NONE
        ConnectionSecurity.STARTTLS_REQUIRED -> CONNECTION_SECURITY_STARTTLS
        ConnectionSecurity.SSL_TLS_REQUIRED -> CONNECTION_SECURITY_SSL_TLS
    }

    companion object {
        private const val OLD_SERVER = "old.valid.server"
        private const val OLD_PORT = 333
        private val OLD_SECURITY_TYPE = ConnectionSecurity.NONE
        private val OLD_AUTH_TYPE = AuthType.PLAIN
        private const val OLD_USER_NAME = "oldUsername"
        private const val OLD_PASSWORD = "oldPassword"
        private const val OLD_CERTIFICATE_ALIAS = "cert"
        private const val ACCOUNT_EMAIL = "account.email@example.ch"

        private const val DEFAULT_SERVER = "serverDefault"
        private const val DEFAULT_PORT = 888
        private val DEFAULT_SECURITY_TYPE = ConnectionSecurity.STARTTLS_REQUIRED
        private const val DEFAULT_USER_NAME = "usernameDefault"
        private const val DEFAULT_EMAIL = "email@default.ch"
        private val DEFAULT_OAUTH_PROVIDER = OAuthProviderType.MICROSOFT
        private val DEFAULT_AUTH_TYPE = AuthType.PLAIN

        private const val NEW_EMAIL = "new.email@mail.ch"
        private const val SECOND_EMAIL = "second.email@mail.ch"
        private const val NEW_SERVER = "mail.server.host"
        private const val WRONG_SERVER = "wrongserver"
        private const val NEW_USER_NAME = "username"
        private const val NEW_SECURITY_TYPE_STRING = "SSL/TLS"
        private val NEW_SECURITY_TYPE = ConnectionSecurity.SSL_TLS_REQUIRED
        private const val NEW_PORT = 999

        private const val MEDIA_KEY_PATTERN_1 = "*@test1.test"
        private const val MEDIA_KEY_PATTERN_2 = "*@test2.test"
        private const val KEY_FPR_1 = "1111111111111111111111111111111111111111"
        private const val KEY_FPR_2 = "2222222222222222222222222222222222222222"
        private const val KEY_MATERIAL_1 = "keymaterial1"
        private const val KEY_MATERIAL_2 = "keymaterial2"
        private const val REAL_KEY_MATERIAL_START =
            "comment\n" +
                    "xjMEY9OobxYJKwYBBAHaRw8BAQdADRQay4tPfxu2PIJ6sw6Z4gZDXESaX1KZZ1nE\n" +
                    "KkGTpXbCwBEEHxYKAIMFgmPTqG8FiQWkj70DCwkHCRBum59KMDX840cUAAAAAAAe\n" +
                    "ACBzYWx0QG5vdGF0aW9ucy5zZXF1b2lhLXBncC5vcmemVc1Oks1c7Q20Bi9S4uqY\n" +
                    "/aMiGCiOOBLVlaO8A6crqAMVCggCmwECHgEWIQQ/i189pVs58d9t43tum59KMDX8\n" +
                    "4wAA0RUBAL72eYxNQsb870U4/FPYy09JJjPoP3S/B3d9ukESJsj9AQDSq4ZTkVlV\n"
        private const val REAL_KEY_MATERIAL_START_UPDATED =
            "comment\n\n\n" +
                    "xjMEY9OobxYJKwYBBAHaRw8BAQdADRQay4tPfxu2PIJ6sw6Z4gZDXESaX1KZZ1nE\n" +
                    "KkGTpXbCwBEEHxYKAIMFgmPTqG8FiQWkj70DCwkHCRBum59KMDX840cUAAAAAAAe\n" +
                    "ACBzYWx0QG5vdGF0aW9ucy5zZXF1b2lhLXBncC5vcmemVc1Oks1c7Q20Bi9S4uqY\n" +
                    "/aMiGCiOOBLVlaO8A6crqAMVCggCmwECHgEWIQQ/i189pVs58d9t43tum59KMDX8\n" +
                    "4wAA0RUBAL72eYxNQsb870U4/FPYy09JJjPoP3S/B3d9ukESJsj9AQDSq4ZTkVlV\n"
        private const val FAKE_KEY_MATERIAL_START =
            "xjMEY9OobxYJKwYBBAHaRw8BAQdADR Qay4tPfxu2PIJ6sw6Z4gZDXESaX1KZZ1nE\n" +
                    "KkGTpXbCwBEEHxYKAIMFgmPTqG8FiQWkj70DCwkHCRBum59KMDX840cUAAAAAAAeu\n" +
                    "ACBzYWx0QG5vdGF0aW9ucy5zZXF1b2lhLXBncC5vcmemVc1Oks1c7Q20Bi9S4uqY\n" +
                    "/aMiGCiOOBLVlaO8A6crqAMVCggCmwECHgEWIQQ/i189pVs58d9t43tum59KMDX8\n" +
                    "4wAA0RUBAL72eYxNQsb870U4/FPYy09JJjPoP3S/B3d9ukESJsj9AQDSq4ZTkVlV\n"
        private const val WRONG_FPR = "WRONG_FPR"
        private const val NEW_ACCOUNT_DESCRIPTION = "newAccountDescription"
        private const val NEW_SENDER_NAME = "new sender name"
        private const val NEW_SIGNATURE = "new signature"
        private const val DEFAULT_SENDER_NAME = "default sender name"
        private const val DEFAULT_SIGNATURE = "default signature"
    }
}
