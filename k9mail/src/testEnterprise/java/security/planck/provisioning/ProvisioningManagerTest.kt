package security.planck.provisioning

import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.RobolectricTest
import com.fsck.k9.helper.Utility
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.planck.PEpProviderImplKotlin
import com.fsck.k9.planck.testutils.CoroutineTestRule
import io.mockk.*
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.*
import security.planck.mdm.ConfigurationManager
import security.planck.network.UrlChecker
import security.planck.provisioning.*

private const val TEST_PROVISIONING_URL = "https://test/url"

@ExperimentalCoroutinesApi
class ProvisioningManagerTest: RobolectricTest() {
    @get:Rule
    val coroutinesTestRule = CoroutineTestRule(testDispatcher = UnconfinedTestDispatcher())

    private val k9: K9 = mockk(relaxed = true)
    private val urlChecker: UrlChecker = mockk()
    private val listener: ProvisioningManager.ProvisioningStateListener = mockk(relaxed = true)
    private val configurationManagerFactory: ConfigurationManager.Factory = mockk()
    private val configurationManager: ConfigurationManager = mockk(relaxed = true)
    private val provisioningSettings: ProvisioningSettings = mockk()
    private val preferences: Preferences = mockk()
    private val manager = ProvisioningManager(
        k9,
        preferences,
        urlChecker,
        configurationManagerFactory,
        provisioningSettings,
        coroutinesTestRule.testDispatcherProvider,
    )

    @Before
    fun setUp() {
        coEvery { provisioningSettings.provisioningUrl }.returns(TEST_PROVISIONING_URL)
        coEvery { provisioningSettings.hasValidMailSettings(any()) }.returns(true)
        coEvery { provisioningSettings.provisionedMailSettings }.returns(null)
        coEvery { urlChecker.isValidUrl(any()) }.returns(true)
        coEvery { urlChecker.isUrlReachable(any()) }.returns(true)
        coEvery { preferences.accounts }.returns(emptyList())
        coEvery { configurationManagerFactory.create(k9) }.returns(configurationManager)
        coEvery { configurationManager.loadConfigurationsSuspend(any()) }
            .returns(Result.success(Unit))
        mockkObject(PEpProviderImplKotlin)
        coEvery { PEpProviderImplKotlin.provision(any(), TEST_PROVISIONING_URL) }
            .returns(Result.success(Unit))

        mockkStatic(Utility::class)
        coEvery { Utility.hasConnectivity(any()) }.returns(true)

        manager.addListener(listener)
        verify { listener.provisionStateChanged(any()) }
        clearMocks(listener)
    }

    @After
    fun tearDown() {
        unmockkObject(PEpProviderImplKotlin)
        unmockkStatic(Utility::class)
    }

    @Test
    fun `startProvisioning() starts provisioning app if running on work profile`() {
        coEvery { k9.isRunningOnWorkProfile }.returns(true)


        manager.startProvisioning()


        coVerify { configurationManager.loadConfigurationsSuspend(ProvisioningStage.Startup(true)) }
    }

    @Test
    @Ignore("provisioning url disabled")
    fun `startProvisioning() provisions app using PEpProviderImplKotlin`() {
        manager.startProvisioning()


        coVerify { PEpProviderImplKotlin.provision(any(), TEST_PROVISIONING_URL) }
        coVerify { k9.finalizeSetup() }
        coVerify { listener.provisionStateChanged(ProvisionState.Initialized) }
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
    @Ignore("remove ignore when we know the url for provisioning")
    fun `when provisioning url is not reachable, resulting state is error`() {
        coEvery { urlChecker.isUrlReachable(any()) }.returns(false)


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
        coEvery { urlChecker.isValidUrl(any()) }.returns(false)


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
        coEvery { PEpProviderImplKotlin.provision(any(), any()) }
            .returns(Result.failure(ProvisioningFailedException("fail", RuntimeException())))


        manager.startProvisioning()


        coVerify { PEpProviderImplKotlin.provision(any(), TEST_PROVISIONING_URL) }
        assertListenerProvisionChangedWithState { state ->
            assertTrue(state is ProvisionState.Error)
            val throwable = (state as ProvisionState.Error).throwable
            assertTrue(throwable is ProvisioningFailedException)
        }
        unmockkObject(PEpProviderImplKotlin)
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
        coEvery { provisioningSettings.provisioningUrl }.returns(null)


        manager.startProvisioning()


        coVerify(exactly = 0) { PEpProviderImplKotlin.provision(any(), TEST_PROVISIONING_URL) }

        assertListenerProvisionChangedWithState { state ->
            assertEquals(ProvisionState.Initialized, state)
        }
    }

    @Test
    fun `if there are no accounts setup, configurationManager_loadConfigurationsSuspend is called with parameter Startup and firsStartup true`() {
        coEvery { k9.isRunningOnWorkProfile }.returns(true)


        manager.startProvisioning()


        coVerify { configurationManager.loadConfigurationsSuspend(ProvisioningStage.Startup(true)) }
        assertListenerProvisionChangedWithState { state ->
            assertEquals(ProvisionState.Initialized, state)
        }
    }

    @Test
    fun `if there are any accounts setup, configurationManager_loadConfigurationsSuspend is called with parameter Startup and firsStartup false`() {
        coEvery { k9.isRunningOnWorkProfile }.returns(true)


        coEvery { preferences.accounts }.returns(listOf(mockk()))


        manager.startProvisioning()


        coVerify { configurationManager.loadConfigurationsSuspend(ProvisioningStage.Startup(false)) }
        assertListenerProvisionChangedWithState { state ->
            assertEquals(ProvisionState.Initialized, state)
        }
    }

    @Test
    fun `performInitializedEngineProvisioning() calls configurationManager_loadConfigurationsSuspend with parameter InitializedEngine`() {
        coEvery { k9.isRunningOnWorkProfile }.returns(true)


        manager.performInitializedEngineProvisioning()


        coVerify { configurationManager.loadConfigurationsSuspend(ProvisioningStage.InitializedEngine) }
    }

    @Test
    fun `startProvisioning() does not provision app if not running on work profile`() {
        coEvery { k9.isRunningOnWorkProfile }.returns(false)


        manager.startProvisioning()


        coVerify { PEpProviderImplKotlin.wasNot(called) }
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
        coEvery { provisioningSettings.hasValidMailSettings(any()) }.returns(false)


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
        coEvery { provisioningSettings.provisionedMailSettings }.returns(
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

    @Test
    fun `manager calls provisionStateChanged() on added listener`() {
        manager.addListener(listener)


        verify { listener.provisionStateChanged(any()) }
    }

    private fun assertListenerProvisionChangedWithState(block: (state: ProvisionState) -> Unit) {
        val slot = mutableListOf<ProvisionState>()
        coVerify { listener.provisionStateChanged(capture(slot)) }
        block(slot.last())
    }
}
