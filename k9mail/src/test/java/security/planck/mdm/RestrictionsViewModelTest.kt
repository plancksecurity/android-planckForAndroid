package security.planck.mdm

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fsck.k9.planck.testutils.CoroutineTestRule
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import security.planck.provisioning.ProvisioningSettings

@ExperimentalCoroutinesApi
class RestrictionsViewModelTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()

    private val testFlow = MutableStateFlow(0)
    private val configurationManager: ConfigurationManager = mockk {
        every { restrictionsUpdatedFlow }.returns(testFlow)
    }
    private val provisioningSettings: ProvisioningSettings = mockk {
    }
    private lateinit var viewModel: RestrictionsViewModel
    private val observedStates = mutableListOf<Boolean>()

    @Before
    fun setUp() {
        observedStates.clear()
        viewModel = RestrictionsViewModel(configurationManager, provisioningSettings)
        observeViewModel()
    }

    @Test
    fun `initially restrictionsUpdated LiveData is false`() {
        assertRestrictionsUpdates(false)
    }

    @Test
    fun `on restrictions updates the restrictionsUpdated LiveData is updated`() = runTest {
        testFlow.value = 1
        advanceUntilIdle()

        assertRestrictionsUpdates(false, true)
    }

    private fun assertRestrictionsUpdates(vararg states: Boolean) {
        TestCase.assertEquals(states.toList(), observedStates)
    }

    private fun observeViewModel() {
        viewModel.restrictionsUpdated.observeForever { event ->
            event.getContentIfNotHandled()?.let { observedStates.add(it) }
        }
    }
}