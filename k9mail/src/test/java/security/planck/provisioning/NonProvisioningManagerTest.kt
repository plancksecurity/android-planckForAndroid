package security.planck.provisioning

import com.fsck.k9.BuildConfig
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.RobolectricTest
import com.fsck.k9.planck.PlanckProviderImplKotlin
import com.fsck.k9.planck.testutils.CoroutineTestRule
import io.mockk.called
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import security.planck.mdm.ConfigurationManager
import security.planck.network.UrlChecker

@ExperimentalCoroutinesApi
class NonProvisioningManagerTest: RobolectricTest() {
    @get:Rule
    val coroutinesTestRule = CoroutineTestRule(testDispatcher = UnconfinedTestDispatcher())

    private val k9: K9 = mockk(relaxed = true)
    private val urlChecker: UrlChecker = mockk()
    private val listener: ProvisioningManager.ProvisioningStateListener = mockk(relaxed = true)
    private val configurationManager: ConfigurationManager = mockk()
    private val provisioningSettings: ProvisioningSettings = mockk()
    private val preferences: Preferences = mockk()
    private val manager = ProvisioningManager(
        k9,
        preferences,
        configurationManager,
        provisioningSettings,
        coroutinesTestRule.testDispatcherProvider,
    )

    @Before
    fun setUp() {
        assumeFalse(BuildConfig.IS_OFFICIAL)
        mockkObject(PlanckProviderImplKotlin)

        manager.addListener(listener)
        verify { listener.provisionStateChanged(any()) }
        clearMocks(listener)
    }

    @After
    fun tearDown() {
        unmockkObject(PlanckProviderImplKotlin)
    }

    @Test
    fun `startProvisioning() does not provision app in dev variant`() {
        manager.startProvisioning()


        coVerify { PlanckProviderImplKotlin.wasNot(called) }
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
        manager.performInitializedEngineProvisioning()


        coVerify { configurationManager.wasNot(called) }
    }

    private fun assertListenerProvisionChangedWithState(block: (state: ProvisionState) -> Unit) {
        val slot = mutableListOf<ProvisionState>()
        coVerify { listener.provisionStateChanged(capture(slot)) }
        block(slot.last())
    }
}
