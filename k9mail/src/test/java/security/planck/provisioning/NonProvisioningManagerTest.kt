package security.planck.provisioning

import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.RobolectricTest
import com.fsck.k9.planck.PlanckProviderImplKotlin
import com.fsck.k9.planck.testutils.CoroutineTestRule
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import security.planck.file.PlanckSystemFileLocator
import security.planck.mdm.ConfigurationManager
import java.io.File

@ExperimentalCoroutinesApi
class NonProvisioningManagerTest : RobolectricTest() {
    @get:Rule
    val coroutinesTestRule = CoroutineTestRule(testDispatcher = UnconfinedTestDispatcher())

    private val k9: K9 = mockk(relaxed = true)
    private val configurationManager: ConfigurationManager = mockk()
    private val provisioningSettings: ProvisioningSettings = mockk()
    private val preferences: Preferences = mockk()
    private val keysDb: File = mockk {
        every { exists() }.returns(true)
    }
    private val fileLocator: PlanckSystemFileLocator = mockk {
        every { keysDbFile }.returns(keysDb)
    }
    private val observedValues = mutableListOf<ProvisionState>()
    private lateinit var manager: ProvisioningManager

    @Before
    fun setUp() {
        mockkObject(PlanckProviderImplKotlin)
        initializeManager()
    }

    @After
    fun tearDown() {
        unmockkObject(PlanckProviderImplKotlin)
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

    private fun initializeManagerAndObserveFlow() {
        initializeManager()
        observeFlow()
    }

    @Test
    fun `startProvisioning() does not provision app in dev variant`() = runTest {
        initializeManagerAndObserveFlow()


        manager.startProvisioning()
        advanceUntilIdle()


        coVerify { PlanckProviderImplKotlin.wasNot(called) }
        coVerify { k9.finalizeSetup() }
        assertObservedValues(
            ProvisionState.WaitingToInitialize(false),
            ProvisionState.Initialized
        )
    }

    @Test
    fun `Manager offers restore initially if keys db does not exist`() {
        every { keysDb.exists() }.returns(false)
        initializeManagerAndObserveFlow()
        assertObservedValues(ProvisionState.WaitingToInitialize(true))
    }

    @Test
    fun `when K9 initialization fails, resulting state is error`() = runTest {
        coEvery { k9.finalizeSetup() }.coAnswers { throw RuntimeException("fail") }
        initializeManagerAndObserveFlow()


        manager.startProvisioning()
        advanceUntilIdle()


        assertFirstObservedValues(
            ProvisionState.WaitingToInitialize(false),
            ProvisionState.Initializing(false)
        )
        val throwable = (observedValues[2] as ProvisionState.Error).throwable
        assertTrue(throwable is InitializationFailedException)
    }

    @Test
    fun `performInitializedEngineProvisioning() never calls configurationManager_loadConfigurationsSuspend`() {
        initializeManagerAndObserveFlow()
        manager.performInitializedEngineProvisioning()


        coVerify { configurationManager.wasNot(called) }
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

    private fun assertFirstObservedValues(vararg values: ProvisionState) {
        println("########################################")
        println("observed values: \n${
            observedValues
                .mapIndexed { index, value -> "$index: $value" }
                .joinToString("\n\n")
        }")
        println("########################################")
        values.forEachIndexed { index, value ->
            assertEquals(
                "FAILURE AT POSITION $index:",
                value, observedValues[index]
            )
        }
    }

    private fun observeFlow() {
        CoroutineScope(UnconfinedTestDispatcher()).launch {
            manager.state.collect {
                observedValues.add(it)
            }
        }
    }
}
