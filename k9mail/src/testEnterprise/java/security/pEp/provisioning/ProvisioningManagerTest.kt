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
import java.io.File

private const val TEST_PROVISIONING_URL = "https://test/url"

@ExperimentalCoroutinesApi
class ProvisioningManagerTest {
    @get:Rule
    val coroutinesTestRule = CoroutineTestRule()

    private val k9: K9 = mockk(relaxed = true)
    private val systemFileLocator: PEpSystemFileLocator = mockk()
    private val urlChecker: UrlChecker = mockk()
    private val keysDbFile: File = mockk()
    private val listener: ProvisioningManager.ProvisioningStateListener = mockk(relaxed = true)
    private val manager = ProvisioningManager(
        k9,
        systemFileLocator,
        urlChecker,
        coroutinesTestRule.testDispatcherProvider
    )

    @Before
    fun setUp() {
        coEvery { k9.provisioningUrl }.returns(TEST_PROVISIONING_URL)
        coEvery { urlChecker.isValidUrl(any()) }.returns(true)
        coEvery { systemFileLocator.keysDbFile }.returns(keysDbFile)
        coEvery { keysDbFile.exists() }.returns(false)
        mockkObject(PEpProviderImplKotlin)
        coEvery { PEpProviderImplKotlin.provision(any(), TEST_PROVISIONING_URL) }
            .returns(Result.success(Unit))

        manager.addListener(listener)
        verify { listener.provisionStateChanged(any()) }
        clearMocks(listener)
    }

    @After
    fun tearDown() {
        unmockkObject(PEpProviderImplKotlin)
    }

    @Test
    fun `startProvisioning() provisions app using PEpProviderImplKotlin`() {
        manager.startProvisioning()


        coVerify { PEpProviderImplKotlin.provision(any(), TEST_PROVISIONING_URL) }
        coVerify { k9.finalizeSetup() }
        coVerify { listener.provisionStateChanged(ProvisionState.Initialized) }
    }

    @Test
    fun `when url has bad format, resulting state is error`() {
        coEvery { urlChecker.isValidUrl(any()) }.returns(false)


        manager.startProvisioning()


        assertListenerProvisionChangedWithState { state ->
            assertTrue(state is ProvisionState.Error)
            val throwable = (state as ProvisionState.Error).throwable
            assertTrue(throwable is IllegalStateException)
            assertTrue(throwable.message!!.contains("Url has bad format"))
        }
    }

    @Test
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
    fun `if provisioning url was not provided, provisioning does not happen`() {
        coEvery { k9.provisioningUrl }.returns(null)


        manager.startProvisioning()


        coVerify(exactly = 0) { PEpProviderImplKotlin.provision(any(), TEST_PROVISIONING_URL) }

        assertListenerProvisionChangedWithState { state ->
            assertEquals(ProvisionState.Initialized, state)
        }
    }

    @Test
    fun `if pEp databases already exist, provisioning does not happen`() {
        coEvery { keysDbFile.exists() }.returns(true)


        manager.startProvisioning()


        coVerify(exactly = 0) { PEpProviderImplKotlin.provision(any(), any()) }
        assertListenerProvisionChangedWithState { state ->
            assertEquals(ProvisionState.Initialized, state)
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
