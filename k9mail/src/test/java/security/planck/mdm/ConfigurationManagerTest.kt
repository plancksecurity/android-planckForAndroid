package security.planck.mdm

import android.content.RestrictionEntry
import android.os.Bundle
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.RobolectricTest
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.planck.testutils.CoroutineTestRule
import com.fsck.k9.preferences.Storage
import com.fsck.k9.preferences.StorageEditor
import io.mockk.Called
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import security.planck.provisioning.CONNECTION_SECURITY_NONE
import security.planck.provisioning.CONNECTION_SECURITY_SSL_TLS
import security.planck.provisioning.CONNECTION_SECURITY_STARTTLS
import security.planck.provisioning.ProvisioningFailedException
import security.planck.provisioning.ProvisioningScope
import security.planck.provisioning.ProvisioningSettings

@ExperimentalCoroutinesApi
class ConfigurationManagerTest : RobolectricTest() {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())
    private val testRestrictions = INITIALIZED_ENGINE_RESTRICTIONS + RESTRICTION_PLANCK_DEBUG_LOG + PROVISIONING_RESTRICTIONS
    private val applicationRestrictionsBundle = Bundle().apply {
        putString(RESTRICTION_PLANCK_EXTRA_KEYS, "extra keys")
        putString(RESTRICTION_PLANCK_MEDIA_KEYS, "media keys")
        putBoolean(RESTRICTION_PLANCK_DEBUG_LOG, true)
        putParcelableArray(
            RESTRICTION_PLANCK_ACCOUNTS_SETTINGS,
            arrayOf(
                Bundle().apply {
                    putMailSettingsBundle()
                }
            )
        )
    }
    private val applicationManifestEntries = listOf<RestrictionEntry>(
        RestrictionEntry(RESTRICTION_PLANCK_EXTRA_KEYS, "extra keys"),
        RestrictionEntry(RESTRICTION_PLANCK_MEDIA_KEYS, "media keys"),
        RestrictionEntry(RESTRICTION_PLANCK_DEBUG_LOG, false),
        RestrictionEntry.createBundleArrayEntry(
            RESTRICTION_PLANCK_ACCOUNTS_SETTINGS,
            arrayOf(
                RestrictionEntry.createBundleEntry(
                    RESTRICTION_PLANCK_ACCOUNT_SETTINGS,
                    arrayOf(
                        getMailRestrictionEntry(),
                    )
                )
            )
        ),
    )
    private val restrictionsProvider: RestrictionsProvider = mockk {
        every { applicationRestrictions }.returns(applicationRestrictionsBundle)
        every { manifestRestrictions }.returns(
            applicationManifestEntries
        )
    }
    private val updater: ConfiguredSettingsUpdater = mockk(relaxed = true)
    private val editor: StorageEditor = mockk(relaxed = true)
    private val mockStorage: Storage = mockk {
        every { edit() }.returns(editor)
    }
    private val account: Account = mockk(relaxed = true)
    private val preferences: Preferences = mockk {
        every { accounts }.answers { listOf(account) }
        every { storage }.returns(mockStorage)
    }
    private val provisioningSettings: ProvisioningSettings = mockk()
    private val k9: K9 = mockk()

    private val manager = ConfigurationManager(
        preferences,
        restrictionsProvider,
        updater,
        provisioningSettings,
        k9,
        coroutineTestRule.testDispatcherProvider
    )
    private val restrictionsUpdateValues = mutableListOf<Int>()

    @Before
    fun setUp() {
        mockkStatic(K9::class)
        every { K9.save(any()) }.just(Runs)
        every { provisioningSettings.findAccountsToRemove() }.returns(emptyList())
        every { provisioningSettings.hasAnyAccountWithWrongSettings() }.returns(false)
        restrictionsUpdateValues.clear()
        observeRestrictionsUpdateValues()
    }

    @After
    fun tearDown() {
        unmockkStatic(K9::class)
    }

    @Test
    fun `loadConfigurationsSuspend() loads restrictions using RestrictionsProvider`() = runTest {
        manager.loadConfigurationsSuspend()


        coVerify { restrictionsProvider.manifestRestrictions }
        coVerify { restrictionsProvider.applicationRestrictions }
    }

    @Test
    fun `loadConfigurationsSuspend() saves settings using K9`() = runTest {
        manager.loadConfigurationsSuspend()


        verifySettingsSaved()
    }

    private fun verifySettingsSaved() {
        coVerify { preferences.storage }
        coVerify { mockStorage.edit() }
        coVerify { K9.save(editor) }
        coVerify { editor.commit() }
        coVerify { account.save(preferences) }
    }

    @Test
    fun `loadConfigurationsSuspend() with FirstStartup updates settings with all manifest entries except the initialized engine entries`() =
        runTest {
            val result = manager.loadConfigurationsSuspend(ProvisioningScope.FirstStartup).also {
                it.exceptionOrNull()
                    ?.let { throwable -> println("ERROR: ${throwable.stackTraceToString()}") }
            }


            assertTrue(result.isSuccess)
            val slot = mutableListOf<RestrictionEntry>()
            coVerify {
                updater.update(
                    applicationRestrictionsBundle,
                    capture(slot),
                    allowModifyAccountProvisioningSettings = true,
                    true
                )
            }
            assertTrue(slot.containsAll(applicationManifestEntries.filter { it.key !in INITIALIZED_ENGINE_RESTRICTIONS }))
            assertTrue(slot.none { it.key in INITIALIZED_ENGINE_RESTRICTIONS })
            verifySettingsSaved()
        }

    @Test
    fun `loadConfigurationsSuspend() with InitializedEngine updates settings with initialized engine restrictions`() =
        runTest {
            val result =
                manager.loadConfigurationsSuspend(ProvisioningScope.InitializedEngine).also {
                    it.exceptionOrNull()
                        ?.let { throwable -> println("ERROR: ${throwable.stackTraceToString()}") }
                }


            assertTrue(result.isSuccess)
            val slot = mutableListOf<RestrictionEntry>()
            coVerify {
                updater.update(
                    applicationRestrictionsBundle,
                    capture(slot),
                    allowModifyAccountProvisioningSettings = true,
                    true
                )
            }
            slot.forEachIndexed { index, restrictionEntry ->
                assertEquals(INITIALIZED_ENGINE_RESTRICTIONS[index], restrictionEntry.key)
            }
            verifySettingsSaved()
        }

    @Test
    fun `loadConfigurationsSuspend() with AllAccountsSettings updates settings with account restrictions for all accounts provided in restrictions`() =
        runTest {
            val result =
                manager.loadConfigurationsSuspend(ProvisioningScope.AllAccountsSettings).also {
                    it.exceptionOrNull()
                        ?.let { throwable -> println("ERROR: ${throwable.stackTraceToString()}") }
                }


            assertTrue(result.isSuccess)
            val slot = mutableListOf<RestrictionEntry>()
            coVerify {
                updater.update(
                    applicationRestrictionsBundle,
                    capture(slot),
                    allowModifyAccountProvisioningSettings = true,
                    true
                )
            }
            slot.forEach { restrictionEntry ->
                assertTrue(restrictionEntry.key == RESTRICTION_PLANCK_ACCOUNTS_SETTINGS)
            }
            verifySettingsSaved()
        }

    @Test
    fun `loadConfigurationsSuspend() with SingleAccountSettings updates settings with account restrictions for the single account provided in restrictions`() =
        runTest {
            val result = manager.loadConfigurationsSuspend(
                ProvisioningScope.SingleAccountSettings(ACCOUNT_EMAIL)
            ).also {
                it.exceptionOrNull()
                    ?.let { throwable -> println("ERROR: ${throwable.stackTraceToString()}") }
            }


            assertTrue(result.isSuccess)
            val restrictionsSlot = mutableListOf<Bundle>()
            val entriesSlot = slot<RestrictionEntry>()
            coVerify {
                updater.update(
                    capture(restrictionsSlot),
                    capture(entriesSlot),
                    allowModifyAccountProvisioningSettings = true,
                    false
                )
            }
            restrictionsSlot.forEach { restrictions ->
                assertTrue(restrictions.containsKey(RESTRICTION_PLANCK_ACCOUNTS_SETTINGS))
                assertEquals(
                    ACCOUNT_EMAIL,
                    restrictions.assertSingleAccountAndGetEmail()
                )
            }
            entriesSlot.captured.also { restrictionEntry ->
                assertTrue(restrictionEntry.key == RESTRICTION_PLANCK_ACCOUNTS_SETTINGS)
            }
            verifySettingsSaved()
        }

    private fun Bundle.assertSingleAccountAndGetEmail(): String? =
        (getParcelableArray(RESTRICTION_PLANCK_ACCOUNTS_SETTINGS).also {
            assertTrue(it!!.size == 1)
        }!!.first() as Bundle).getBundle(RESTRICTION_ACCOUNT_MAIL_SETTINGS)!!
            .getString(RESTRICTION_ACCOUNT_EMAIL_ADDRESS)

    @Test
    fun `loadConfigurationsSuspend() with AllSettings updates all managed settings`() =
        runTest {
            val result = manager.loadConfigurationsSuspend(ProvisioningScope.AllSettings).also {
                it.exceptionOrNull()
                    ?.let { throwable -> println("ERROR: ${throwable.stackTraceToString()}") }
            }


            assertTrue(result.isSuccess)
            val slot = mutableListOf<RestrictionEntry>()
            coVerify {
                updater.update(
                    applicationRestrictionsBundle,
                    capture(slot),
                    allowModifyAccountProvisioningSettings = true,
                    true
                )
            }
            slot.forEachIndexed { index, restrictionEntry ->
                assertEquals(testRestrictions[index], restrictionEntry.key)
            }
            verifySettingsSaved()
        }

    @Test
    fun `loadConfigurationsSuspend() on first startup fails if account mail settings are not provided`() =
        runTest {
            coEvery { restrictionsProvider.applicationRestrictions }.returns(
                Bundle().apply {
                    INITIALIZED_ENGINE_RESTRICTIONS.forEach { putString(it, "a") }
                }
            )


            val result = manager.loadConfigurationsSuspend(ProvisioningScope.FirstStartup)


            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is ProvisioningFailedException)
            coVerify { updater.wasNot(Called) }
            coVerify(exactly = 0) { K9.save(any()) }
        }

    @Test
    fun `loadConfigurationsSuspend() returns failure if an exception is thrown`() =
        runTest {
            coEvery { restrictionsProvider.applicationRestrictions }.throws(RuntimeException("test"))


            val result = manager.loadConfigurationsSuspend()


            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is RuntimeException)
            coVerify { updater.wasNot(Called) }
            coVerify(exactly = 0) { K9.save(any()) }
        }

    @Test
    fun `loadConfigurationsSuspend() sets account removed flow if it detects an account was removed and app is running in foreground`() =
        runTest {
            coEvery { provisioningSettings.findAccountsToRemove() }.returns(listOf(mockk()))
            coEvery { k9.isRunningInForeground }.returns(true)
            assertFalse(manager.accountRemovedFlow.first())


            val result = manager.loadConfigurationsSuspend()


            assertTrue(result.isSuccess)
            verify { provisioningSettings.findAccountsToRemove() }
            assertTrue(manager.accountRemovedFlow.first())
        }

    @Test
    fun `loadConfigurationsSuspend() does not set account removed flow when called on startup`() =
        runTest {
            coEvery { provisioningSettings.findAccountsToRemove() }.returns(listOf(mockk()))
            coEvery { k9.isRunningInForeground }.returns(true)
            assertFalse(manager.accountRemovedFlow.first())


            val result = manager.loadConfigurationsSuspend(ProvisioningScope.Startup)


            assertTrue(result.isSuccess)
            verify(exactly = 0) { provisioningSettings.findAccountsToRemove() }
            assertFalse(manager.accountRemovedFlow.first())
        }

    @Test
    fun `loadConfigurationsSuspend() sets wrong account settings flow if it detects some accounts in provisioning have wrong settings`() =
        runTest {
            coEvery { provisioningSettings.hasAnyAccountWithWrongSettings() }.returns(true)
            assertFalse(manager.wrongAccountSettingsFlow.first())


            val result = manager.loadConfigurationsSuspend()


            assertTrue(result.isSuccess)
            verify { provisioningSettings.hasAnyAccountWithWrongSettings() }
            assertTrue(manager.wrongAccountSettingsFlow.first())
        }

    @Test
    fun `resetWrongAccountSettingsWarning() unsets wrong account settings flow`() = runTest {
        coEvery { provisioningSettings.hasAnyAccountWithWrongSettings() }.returns(true)
        assertFalse(manager.wrongAccountSettingsFlow.first())
        manager.loadConfigurationsSuspend()
        assertTrue(manager.wrongAccountSettingsFlow.first())


        manager.resetWrongAccountSettingsWarning()
        advanceUntilIdle()


        assertFalse(manager.wrongAccountSettingsFlow.first())
    }

    @Test
    fun `loadConfigurationsSuspend() sets wrong account settings flow if it detects some incoming settings with badly formatted or missing mail address`() =
        runTest {
            coEvery { provisioningSettings.hasAnyAccountWithWrongSettings() }.returns(false)
            assertFalse(manager.wrongAccountSettingsFlow.first())
            stubBadEmailAddress("ljsfj")


            val result = manager.loadConfigurationsSuspend()


            assertTrue(result.isSuccess)
            verify(exactly = 0) { provisioningSettings.hasAnyAccountWithWrongSettings() }
            assertTrue(manager.wrongAccountSettingsFlow.first())
        }

    @Suppress("SameParameterValue")
    private fun stubBadEmailAddress(newEmailAddress: String?) {
        (applicationRestrictionsBundle.getParcelableArray(RESTRICTION_PLANCK_ACCOUNTS_SETTINGS)!!
            .first() as Bundle).getBundle(RESTRICTION_ACCOUNT_MAIL_SETTINGS)!!
            .putString(RESTRICTION_ACCOUNT_EMAIL_ADDRESS, newEmailAddress)
    }

    @Test
    fun `loadConfigurationsBlocking() calls loadConfigurationsSuspend()`() {
        manager.loadConfigurationsBlocking(ProvisioningScope.AllSettings)


        coVerify { restrictionsProvider.manifestRestrictions }
        coVerify { restrictionsProvider.applicationRestrictions }
        val slot = mutableListOf<RestrictionEntry>()
        coVerify {
            updater.update(
                applicationRestrictionsBundle,
                capture(slot),
                allowModifyAccountProvisioningSettings = true,
                true
            )
        }
        slot.forEach { restrictionEntry ->
            assertTrue(restrictionEntry.key in testRestrictions)
        }
        verifySettingsSaved()
    }

    @Test
    fun `loadConfigurations() calls loadConfigurationsSuspend()`() = runTest {
        manager.loadConfigurations()
        advanceUntilIdle()


        coVerify { restrictionsProvider.manifestRestrictions }
        coVerify { restrictionsProvider.applicationRestrictions }
        val slot = mutableListOf<RestrictionEntry>()
        coVerify {
            updater.update(
                applicationRestrictionsBundle,
                capture(slot),
                allowModifyAccountProvisioningSettings = true,
                true
            )
        }
        slot.forEach { restrictionEntry ->
            assertTrue(restrictionEntry.key in testRestrictions)
        }
        verifySettingsSaved()
    }

    @Test
    fun `initially restrictions flow value is 0`() {
        assertUpdateValues(0)
    }

    @Test
    fun `loadConfigurations() updates restrictions flow when settings are successfully updated`() =
        runTest {
            manager.loadConfigurations()
            advanceUntilIdle()


            assertUpdateValues(0, 1)
        }

    @Test
    fun `loadConfigurations() does not update restrictions flow when settings update fails`() =
        runTest {
            coEvery { restrictionsProvider.applicationRestrictions }.throws(RuntimeException("test"))


            manager.loadConfigurations()
            advanceUntilIdle()


            assertUpdateValues(0)
        }

    private fun observeRestrictionsUpdateValues() {
        CoroutineScope(UnconfinedTestDispatcher()).launch {
            manager.restrictionsUpdatedFlow.collect {
                restrictionsUpdateValues.add(it)
            }
        }
    }

    private fun assertUpdateValues(vararg states: Int) {
        assertEquals(states.toList(), restrictionsUpdateValues)
    }

    private fun Bundle.putMailSettingsBundle(
        email: String? = ACCOUNT_EMAIL,
    ) = apply {
        putBundle(
            RESTRICTION_ACCOUNT_MAIL_SETTINGS,
            Bundle().apply {
                putString(RESTRICTION_ACCOUNT_EMAIL_ADDRESS, email)
            }
        )
    }

    private fun getMailRestrictionEntry(): RestrictionEntry = RestrictionEntry.createBundleEntry(
        RESTRICTION_ACCOUNT_MAIL_SETTINGS,
        arrayOf(
            RestrictionEntry(
                RESTRICTION_ACCOUNT_EMAIL_ADDRESS,
                DEFAULT_EMAIL
            ),
        )
    )

    private fun ConnectionSecurity.toMdmName(): String = when (this) {
        ConnectionSecurity.NONE -> CONNECTION_SECURITY_NONE
        ConnectionSecurity.STARTTLS_REQUIRED -> CONNECTION_SECURITY_STARTTLS
        ConnectionSecurity.SSL_TLS_REQUIRED -> CONNECTION_SECURITY_SSL_TLS
    }

    companion object {
        private const val ACCOUNT_EMAIL = "account.email@example.ch"
        private const val DEFAULT_EMAIL = "email@default.ch"
    }
}