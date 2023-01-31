package security.pEp.provisioning

import com.fsck.k9.BuildConfig
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.RobolectricTest
import com.fsck.k9.pEp.PEpProviderImplKotlin
import com.fsck.k9.pEp.testutils.CoroutineTestRule
import io.mockk.*
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import security.pEp.mdm.ConfigurationManager
import security.pEp.network.UrlChecker

@ExperimentalCoroutinesApi
class NonProvisioningManagerTest : RobolectricTest() {
    @get:Rule
    val coroutinesTestRule = CoroutineTestRule()

    private val k9: K9 = mockk(relaxed = true)
    private val urlChecker: UrlChecker = mockk()
    private val listener: ProvisioningManager.ProvisioningStateListener = mockk(relaxed = true)
    private val configurationManagerFactory: ConfigurationManager.Factory = mockk()
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
        assumeFalse(BuildConfig.IS_ENTERPRISE)
        mockkObject(PEpProviderImplKotlin)

        manager.addListener(listener)
        verify { listener.provisionStateChanged(any()) }
        clearMocks(listener)
    }

    @After
    fun tearDown() {
        unmockkObject(PEpProviderImplKotlin)
    }

    @Test
    fun `startProvisioning() does not provision app in endUser variant`() {
        manager.startProvisioning()


        coVerify { PEpProviderImplKotlin.wasNot(called) }
        coVerify { k9.finalizeSetup() }
        assertListenerProvisionChangedWithState { state ->
            assertEquals(ProvisionState.Initialized, state)
        }
    }

    @Test
    fun `when K9 initialization fails, resulting state is error`() {
        coEvery { k9.finalizeSetup() }.coAnswers { throw RuntimeException("fail") }


        manager.startProvisioning()


        coVerify { k9.finalizeSetup() }
        assertListenerProvisionChangedWithState { state ->
            assertTrue(state is ProvisionState.Error)
            val throwable = (state as ProvisionState.Error).throwable
            assertTrue(throwable is InitializationFailedException)
        }
    }

    @Test
    fun `performInitializedEngineProvisioning() never calls configurationManager_loadConfigurationsSuspend`() {
        val configurationManager: ConfigurationManager = mockk()
        every { configurationManagerFactory.create(any()) }.returns(configurationManager)


        manager.performInitializedEngineProvisioning()


        coVerify { configurationManager.wasNot(called) }
    }

    private fun assertListenerProvisionChangedWithState(block: (state: ProvisionState) -> Unit) {
        val slot = mutableListOf<ProvisionState>()
        coVerify { listener.provisionStateChanged(capture(slot)) }
        block(slot.last())
    }
}
