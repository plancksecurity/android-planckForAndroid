package security.pEp.provisioning

import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.RobolectricTest
import com.fsck.k9.helper.Utility
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.pEp.PEpProviderImplKotlin
import com.fsck.k9.pEp.testutils.CoroutineTestRule
import io.mockk.*
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.*
import security.pEp.mdm.ConfigurationManager
import security.pEp.network.UrlChecker

private const val TEST_PROVISIONING_URL = "https://test/url"

@ExperimentalCoroutinesApi
class ProvisioningManagerTest: RobolectricTest() {
    @get:Rule
    val coroutinesTestRule = CoroutineTestRule()

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
    @Ignore("provisioning url disabled")
    fun `startProvisioning() provisions app using PEpProviderImplKotlin`() {
        manager.startProvisioning()


        coVerify { PEpProviderImplKotlin.provision(any(), TEST_PROVISIONING_URL) }
        coVerify { k9.finalizeSetup() }
        coVerify { listener.provisionStateChanged(ProvisionState.Initialized) }
    }

    @Test
    fun `when device has no network connectivity, resulting state is error`() {
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
        manager.startProvisioning()


        coVerify { configurationManager.loadConfigurationsSuspend(ProvisioningStage.Startup(true)) }
        assertListenerProvisionChangedWithState { state ->
            assertEquals(ProvisionState.Initialized, state)
        }
    }

    @Test
    fun `if there are any accounts setup, configurationManager_loadConfigurationsSuspend is called with parameter Startup and firsStartup false`() {
        coEvery { preferences.accounts }.returns(listOf(mockk()))


        manager.startProvisioning()


        coVerify { configurationManager.loadConfigurationsSuspend(ProvisioningStage.Startup(false)) }
        assertListenerProvisionChangedWithState { state ->
            assertEquals(ProvisionState.Initialized, state)
        }
    }

    @Test
    fun `performInitializedEngineProvisioning() calls configurationManager_loadConfigurationsSuspend with parameter InitializedEngine`() {
        manager.performInitializedEngineProvisioning()


        coVerify { configurationManager.loadConfigurationsSuspend(ProvisioningStage.InitializedEngine) }
    }

    @Test
    fun `if ConfigurationManager_loadConfigurationSuspend fails, resulting state is error`() {
        coEvery { configurationManager.loadConfigurationsSuspend(any()) }
            .returns(Result.failure(RuntimeException("fail")))


        manager.startProvisioning()


        assertListenerProvisionChangedWithState { state ->
            coVerify { k9.wasNot(called) }
            assertTrue(state is ProvisionState.Error)
            val throwable = (state as ProvisionState.Error).throwable
            assertTrue(throwable is RuntimeException)
            assertTrue(throwable.message!!.contains("fail"))
        }
    }

    @Test
    fun `if mail settings are not valid, resulting state is error`() {
        coEvery { provisioningSettings.hasValidMailSettings(any()) }.returns(false)


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
