package security.planck.mdm

import android.content.RestrictionEntry
import android.os.Bundle
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.RobolectricTest
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
import io.mockk.unmockkStatic
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import security.planck.provisioning.ProvisioningFailedException
import security.planck.provisioning.ProvisioningScope

@ExperimentalCoroutinesApi
class ConfigurationManagerTest : RobolectricTest() {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())
    private val testRestrictions = INITIALIZED_ENGINE_RESTRICTIONS + ALL_ACCOUNT_RESTRICTIONS
    private val applicationRestrictionsBundle = Bundle().apply {
        testRestrictions.forEach { putString(it, "a") }
    }
    private val restrictionsProvider: RestrictionsProvider = mockk {
        every { applicationRestrictions }.returns(applicationRestrictionsBundle)
        every { manifestRestrictions }.returns(
            testRestrictions.map { RestrictionEntry(it, "a") }
        )
    }
    private val updater: ConfiguredSettingsUpdater = mockk(relaxed = true)
    private val editor: StorageEditor = mockk(relaxed = true)
    private val mockStorage: Storage = mockk {
        every { edit() }.returns(editor)
    }
    private val account: Account = mockk(relaxed = true)
    private val preferences: Preferences = mockk {
        every { accounts }.returns(listOf(account))
        every { storage }.returns(mockStorage)
    }

    private val manager = ConfigurationManager(
        preferences,
        restrictionsProvider,
        updater,
        coroutineTestRule.testDispatcherProvider
    )

    @Before
    fun setUp() {
        mockkStatic(K9::class)
        every { K9.save(any()) }.just(Runs)
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
    fun `loadConfigurationsSuspend() on first startup updates settings with provisioning restrictions`() =
        runTest {
            val result = manager.loadConfigurationsSuspend(ProvisioningScope.FirstStartup)


            assertTrue(result.isSuccess)
            val slot = mutableListOf<RestrictionEntry>()
            coVerify { updater.update(applicationRestrictionsBundle, capture(slot)) }
            slot.forEach { restrictionEntry ->
                assertTrue(restrictionEntry.key in PROVISIONING_RESTRICTIONS)
            }
            verifySettingsSaved()
        }

    @Test
    fun `loadConfigurationsSuspend() with InitializedEngine updates settings with intialized engine restrictions`() =
        runTest {
            val result = manager.loadConfigurationsSuspend(ProvisioningScope.InitializedEngine)


            assertTrue(result.isSuccess)
            val slot = mutableListOf<RestrictionEntry>()
            coVerify { updater.update(applicationRestrictionsBundle, capture(slot)) }
            slot.forEachIndexed { index, restrictionEntry ->
                assertEquals(INITIALIZED_ENGINE_RESTRICTIONS[index], restrictionEntry.key)
            }
            verifySettingsSaved()
        }

    @Test
    fun `loadConfigurationsSuspend() with AllAccountSettings updates settings with account restrictions`() =
        runTest {
            val result = manager.loadConfigurationsSuspend(ProvisioningScope.AllAccountSettings)


            assertTrue(result.isSuccess)
            val slot = mutableListOf<RestrictionEntry>()
            coVerify { updater.update(applicationRestrictionsBundle, capture(slot)) }
            slot.forEachIndexed { index, restrictionEntry ->
                assertEquals(ALL_ACCOUNT_RESTRICTIONS[index], restrictionEntry.key)
            }
            verifySettingsSaved()
        }

    @Test
    fun `loadConfigurationsSuspend() with AllSettings updates all managed settings`() =
        runTest {
            val result = manager.loadConfigurationsSuspend(ProvisioningScope.AllSettings)


            assertTrue(result.isSuccess)
            val slot = mutableListOf<RestrictionEntry>()
            coVerify { updater.update(applicationRestrictionsBundle, capture(slot)) }
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
    fun `loadConfigurationsBlocking() calls loadConfigurationsSuspend()`() {
        manager.loadConfigurationsBlocking(ProvisioningScope.AllSettings)


        coVerify { restrictionsProvider.manifestRestrictions }
        coVerify { restrictionsProvider.applicationRestrictions }
        val slot = mutableListOf<RestrictionEntry>()
        coVerify { updater.update(applicationRestrictionsBundle, capture(slot)) }
        slot.forEachIndexed { index, restrictionEntry ->
            assertEquals(testRestrictions[index], restrictionEntry.key)
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
        coVerify { updater.update(applicationRestrictionsBundle, capture(slot)) }
        slot.forEachIndexed { index, restrictionEntry ->
            assertEquals(testRestrictions[index], restrictionEntry.key)
        }
        verifySettingsSaved()
    }

    @Test
    fun `loadConfigurations() calls restrictions listeners when settings are successfully updated`() =
        runTest {
            val listener = mockk<RestrictionsListener>()


            manager.addListener(listener)
            manager.loadConfigurations()
            advanceUntilIdle()


            coVerify { listener.updatedRestrictions() }
        }

    @Test
    fun `loadConfigurations() does not call restrictions listeners when settings update fails`() =
        runTest {
            coEvery { restrictionsProvider.applicationRestrictions }.throws(RuntimeException("test"))
            val listener = mockk<RestrictionsListener>()


            manager.addListener(listener)
            manager.loadConfigurations()
            advanceUntilIdle()


            coVerify { listener.wasNot(Called) }
        }
}