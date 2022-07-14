package security.pEp.provisioning

import com.fsck.k9.K9
import com.fsck.k9.pEp.PEpProviderImplKotlin
import com.fsck.k9.pEp.testutils.CoroutineTestRule
import io.mockk.*
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import security.pEp.file.PEpSystemFileLocator

@ExperimentalCoroutinesApi
class ProvisioningManagerTestEndUser {
    @get:Rule
    val coroutinesTestRule = CoroutineTestRule()

    private val k9: K9 = mockk(relaxed = true)
    private val systemFileLocator: PEpSystemFileLocator = mockk()
    private val urlChecker: UrlChecker = mockk()
    private val listener: ProvisioningManager.ProvisioningStateListener = mockk(relaxed = true)
    private val manager = ProvisioningManager(
        k9,
        systemFileLocator,
        urlChecker,
        coroutinesTestRule.testDispatcherProvider
    )

    @Before
    fun setUp() {
        mockkObject(PEpProviderImplKotlin)
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
        assertEquals(ProvisionState.Initialized, manager.provisionState)
    }

    @Test
    fun `when K9 initialization fails, resulting state is error`() {
        coEvery { k9.finalizeSetup() }.coAnswers { throw RuntimeException("fail") }


        manager.startProvisioning()


        coVerify { k9.finalizeSetup() }

        val slot = slot<ProvisionState>()
        coVerify { listener.provisionStateChanged(capture(slot)) }
        val state = slot.captured
        assertTrue(state is ProvisionState.Error)
        val throwable = (state as ProvisionState.Error).throwable
        assertTrue(throwable is InitializationFailedException)
    }
}
