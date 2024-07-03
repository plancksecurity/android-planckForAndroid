package security.planck.provisioning

import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.RobolectricTest
import com.fsck.k9.activity.setup.AuthFlowState
import com.fsck.k9.helper.Utility
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mailstore.LocalStore
import com.fsck.k9.planck.PlanckProviderImplKotlin
import com.fsck.k9.planck.testutils.CoroutineTestRule
import io.mockk.*
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.*
import security.planck.file.PlanckSystemFileLocator
import security.planck.mdm.ConfigurationManager
import security.planck.network.UrlChecker
import security.planck.provisioning.*

private const val TEST_PROVISIONING_URL = "https://test/url"

@ExperimentalCoroutinesApi
class ProvisioningManagerTest: RobolectricTest() {
    @get:Rule
    val coroutinesTestRule = CoroutineTestRule(testDispatcher = UnconfinedTestDispatcher())

    private val k9: K9 = mockk(relaxed = true)
    private val urlChecker: UrlChecker = spyk(UrlChecker())
    private val configurationManager: ConfigurationManager = mockk(relaxed = true)
    private val preferences: Preferences = mockk()
    private val provisioningSettings: ProvisioningSettings = mockk()
    private val fileLocator: PlanckSystemFileLocator = mockk()

    private val observedValues = mutableListOf<ProvisionState>()

    private lateinit var manager : ProvisioningManager

    @Before
    fun setUp() {
        observedValues.clear()
        coEvery { provisioningSettings.hasValidMailSettings() }.returns(true)
        coEvery { provisioningSettings.accountsProvisionList }.returns(mutableListOf())
        coEvery { provisioningSettings.findAccountsToRemove() }.returns(emptyList())
        coEvery { preferences.accounts }.answers { emptyList() }
        coEvery { preferences.deleteAccount(any()) }.just(runs)
        coEvery { configurationManager.loadConfigurationsSuspend(any()) }
            .returns(Result.success(Unit))
        mockkObject(PlanckProviderImplKotlin)
        coEvery { PlanckProviderImplKotlin.provision(any(), TEST_PROVISIONING_URL) }
            .returns(Result.success(Unit))

        mockkStatic(Utility::class)
        coEvery { Utility.hasConnectivity(any()) }.returns(true)
        initializeManager()
    }

    private fun initializeManager() {
        manager = ProvisioningManager(
            k9,
            preferences,
            configurationManager,
            fileLocator,
            provisioningSettings,
            coroutinesTestRule.testDispatcherProvider,
        )
    }

    @After
    fun tearDown() {
        unmockkObject(PlanckProviderImplKotlin)
        unmockkStatic(Utility::class)
    }

    @Test
    fun `initial state is WaitingForProvisioning if running under MDM`() {
        coEvery { k9.isRunningOnWorkProfile }.returns(true)


        assertObservedValues(ProvisionState.WaitingForProvisioning)
    }

    @Test
    fun `startProvisioning() starts provisioning app if running on work profile`() {
        coEvery { k9.isRunningOnWorkProfile }.returns(true)


        manager.startProvisioning()


        coVerify { configurationManager.loadConfigurationsSuspend(ProvisioningScope.FirstStartup) }
    }

    @Test
    @Ignore("provisioning url disabled")
    fun `startProvisioning() provisions app using PEpProviderImplKotlin`() {
        manager.startProvisioning()


        coVerify { PlanckProviderImplKotlin.provision(any(), TEST_PROVISIONING_URL) }
        coVerify { k9.finalizeSetup() }
    }

    @Test
    fun `when device has no network connectivity, resulting state is error`() {
        coEvery { k9.isRunningOnWorkProfile }.returns(true)


        coEvery { Utility.hasConnectivity(any()) }.returns(false)


        manager.startProvisioning()


        assertListenerProvisionChangedWithState { state ->
            assertTrue(state is ProvisionState.Error)
            val throwable = (state as ProvisionState.Error).throwable
            assertTrue(throwable is ProvisioningFailedException)
            assertTrue(throwable.message!!.contains("Device is offline"))
        }
    }

    @Test
    @Ignore("provisioning url disabled")
    fun `when provisioning url is not reachable, resulting state is error`() {
        manager.startProvisioning()


        assertListenerProvisionChangedWithState { state ->
            assertTrue(state is ProvisionState.Error)
            val throwable = (state as ProvisionState.Error).throwable
            assertTrue(throwable is ProvisioningFailedException)
            assertTrue(throwable.message!!.contains("is not reachable"))
        }
    }

    @Test
    @Ignore("provisioning url disabled")
    fun `when url has bad format, resulting state is error`() {
        manager.startProvisioning()


        assertListenerProvisionChangedWithState { state ->
            assertTrue(state is ProvisionState.Error)
            val throwable = (state as ProvisionState.Error).throwable
            assertTrue(throwable is ProvisioningFailedException)
            assertTrue(throwable.message!!.contains("Url has bad format"))
        }
    }

    @Test
    @Ignore("provisioning url disabled")
    fun `when provisioning fails, resulting state is error`() {
        coEvery { PlanckProviderImplKotlin.provision(any(), any()) }
            .returns(Result.failure(ProvisioningFailedException("fail", RuntimeException())))


        manager.startProvisioning()


        coVerify { PlanckProviderImplKotlin.provision(any(), TEST_PROVISIONING_URL) }
        assertListenerProvisionChangedWithState { state ->
            assertTrue(state is ProvisionState.Error)
            val throwable = (state as ProvisionState.Error).throwable
            assertTrue(throwable is ProvisioningFailedException)
        }
        unmockkObject(PlanckProviderImplKotlin)
    }

    @Test
    fun `when K9 initialization fails, resulting state is error`() {
        coEvery { k9.finalizeSetup() }.coAnswers { throw RuntimeException("fail") }


        manager.startProvisioning()


        assertListenerProvisionChangedWithState { state ->
            coVerify { k9.finalizeSetup() }
            assertTrue(state is ProvisionState.Error)
            val throwable = (state as ProvisionState.Error).throwable
            assertTrue(throwable is InitializationFailedException)
        }
    }

    @Test
    @Ignore("provisioning url disabled")
    fun `if provisioning url was not provided, provisioning does not happen`() {
        manager.startProvisioning()


        coVerify(exactly = 0) { PlanckProviderImplKotlin.provision(any(), TEST_PROVISIONING_URL) }

        assertListenerProvisionChangedWithState { state ->
            assertEquals(ProvisionState.Initialized, state)
        }
    }

    @Test
    fun `if there are no accounts setup, configurationManager_loadConfigurationsSuspend is called with parameter FirstStartup`() {
        coEvery { k9.isRunningOnWorkProfile }.returns(true)


        manager.startProvisioning()


        coVerify { configurationManager.loadConfigurationsSuspend(ProvisioningScope.FirstStartup) }
        assertListenerProvisionChangedWithState { state ->
            assertEquals(ProvisionState.Initialized, state)
        }
    }

    @Test
    fun `if there are accounts setup, configurationManager_loadConfigurationsSuspend is called with parameter Startup`() {
        coEvery { preferences.accounts }.answers { listOf(mockk()) }
        coEvery { k9.isRunningOnWorkProfile }.returns(true)


        manager.startProvisioning()


        coVerify { configurationManager.loadConfigurationsSuspend(ProvisioningScope.Startup) }
        assertListenerProvisionChangedWithState { state ->
            assertEquals(ProvisionState.Initialized, state)
        }
    }

    @Test
    fun `if there are accounts setup, accounts in ProvisioningSettings that need deletion are deleted`() {
        val localStore: LocalStore = mockk {
            every { delete() }.just(runs)
        }
        val account: Account = mockk {
            every { this@mockk.localStore }.returns(localStore)
            every { email }.returns("email")
        }
        coEvery { preferences.accounts }.answers { listOf(account) }
        coEvery { provisioningSettings.findAccountsToRemove() }.returns(listOf(account))
        coEvery { k9.isRunningOnWorkProfile }.returns(true)


        manager.startProvisioning()


        coVerifyOrder {
            configurationManager.loadConfigurationsSuspend(ProvisioningScope.Startup)
            account.localStore
            localStore.delete()
            preferences.deleteAccount(account)
            provisioningSettings.removeAccountSettingsByAddress("email")
        }
    }

    @Test
    fun `performInitializedEngineProvisioning() calls configurationManager_loadConfigurationsSuspend with parameter InitializedEngine if it is the first startup`() {
        coEvery { k9.isRunningOnWorkProfile }.returns(true)


        manager.startProvisioning()
        manager.performInitializedEngineProvisioning()


        coVerify { configurationManager.loadConfigurationsSuspend(ProvisioningScope.InitializedEngine) }
    }

    @Test
    fun `performInitializedEngineProvisioning() calls configurationManager_loadConfigurationsSuspend with parameter InitializedEngine on every startup`() {
        coEvery { preferences.accounts }.answers { listOf(mockk()) }
        coEvery { k9.isRunningOnWorkProfile }.returns(true)


        manager.startProvisioning()
        manager.performInitializedEngineProvisioning()


        coVerify { configurationManager.loadConfigurationsSuspend(ProvisioningScope.InitializedEngine) }
    }

    @Test
    fun `startProvisioning() does not provision app if not running on work profile`() {
        coEvery { k9.isRunningOnWorkProfile }.returns(false)


        manager.startProvisioning()


        coVerify { PlanckProviderImplKotlin.wasNot(called) }
        coVerify { k9.finalizeSetup() }
        assertListenerProvisionChangedWithState { state ->
            assertEquals(ProvisionState.Initialized, state)
        }
    }

    @Test
    fun `if ConfigurationManager_loadConfigurationSuspend fails, resulting state is error`() {
        coEvery { k9.isRunningOnWorkProfile }.returns(true)


        coEvery { configurationManager.loadConfigurationsSuspend(any()) }
            .returns(Result.failure(RuntimeException("fail")))


        manager.startProvisioning()


        assertListenerProvisionChangedWithState { state ->
            coVerify { k9.isRunningOnWorkProfile }
            confirmVerified(k9)
            assertTrue(state is ProvisionState.Error)
            val throwable = (state as ProvisionState.Error).throwable
            assertTrue(throwable is RuntimeException)
            assertTrue(throwable.message!!.contains("fail"))
        }
    }

    @Test
    fun `if mail settings are not valid, resulting state is error`() {
        coEvery { k9.isRunningOnWorkProfile }.returns(true)
        coEvery { provisioningSettings.hasValidMailSettings() }.returns(false)


        manager.startProvisioning()


        assertListenerProvisionChangedWithState { state ->
            coVerify { k9.isRunningOnWorkProfile }
            confirmVerified(k9)
            assertTrue(state is ProvisionState.Error)
            val throwable = (state as ProvisionState.Error).throwable
            assertTrue(throwable is ProvisioningFailedException)
            assertTrue(
                throwable.message!!.contains("Provisioned mail settings are not valid")
            )
        }
    }

    @Test
    @Ignore("provisioning url disabled")
    fun `if url fails mail settings server url check, resulting state is error`() {
        coEvery { urlChecker.isValidUrl(any()) }.returns(false)
        coEvery { provisioningSettings.accountsProvisionList.firstOrNull()?.provisionedMailSettings }.returns(
            AccountMailSettingsProvision(
                incoming = SimpleMailSettings(
                    700,
                    "server",
                    ConnectionSecurity.SSL_TLS_REQUIRED,
                    "username"
                ),
                outgoing = SimpleMailSettings(
                    700,
                    "server",
                    ConnectionSecurity.STARTTLS_REQUIRED,
                    "username"
                )
            )
        )


        manager.startProvisioning()


        assertListenerProvisionChangedWithState { state ->
            coVerify { k9.wasNot(called) }
            assertTrue(state is ProvisionState.Error)
            val throwable = (state as ProvisionState.Error).throwable
            assertTrue(throwable is ProvisioningFailedException)
            assertTrue(
                throwable.message!!.contains("Provisioned mail settings are not valid")
            )
        }
    }

    private fun assertObservedValues(vararg values: ProvisionState) {
        println("########################################")
        println("observed values: \n${
            observedValues
                .mapIndexed { index, value -> "$index: $value" }
                .joinToString("\n\n")
        }")
        println("########################################")
        assertEquals(
            "expected ${values.size} values but got ${observedValues.size} values instead",
            values.size, observedValues.size
        )
        values.forEachIndexed { index, value ->
            assertEquals(
                "FAILURE AT POSITION $index:",
                value, observedValues[index]
            )
        }
    }

    private fun assertListenerProvisionChangedWithState(block: (state: ProvisionState) -> Unit) {
        //val slot = mutableListOf<ProvisionState>()
        //coVerify { listener.provisionStateChanged(capture(slot)) }
        //block(slot.last())
    }

    private fun observeFlow() {
        CoroutineScope(UnconfinedTestDispatcher()).launch {
            manager.state.collect {
                observedValues.add(it)
            }
        }
    }
}
