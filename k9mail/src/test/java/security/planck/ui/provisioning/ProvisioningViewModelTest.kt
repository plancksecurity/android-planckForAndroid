package security.planck.ui.provisioning

import androidx.lifecycle.LiveData
import com.fsck.k9.planck.testutils.CoroutineTestRule
import com.fsck.k9.planck.ui.activities.provisioning.ProvisioningViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import security.planck.common.LiveDataTest
import security.planck.provisioning.ProvisionState
import security.planck.provisioning.ProvisioningManager

@OptIn(ExperimentalCoroutinesApi::class)
class ProvisioningViewModelTest : LiveDataTest<ProvisionState>() {
    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()

    private val testFlow =
        MutableStateFlow<ProvisionState>(ProvisionState.WaitingToInitialize(false))
    private val provisioningManager: ProvisioningManager = mockk(relaxed = true) {
        every { state }.returns(testFlow)
    }
    private lateinit var viewModel: ProvisioningViewModel
    override val testLivedata: LiveData<ProvisionState>
        get() = viewModel.state

    override fun initialize() {
        viewModel = ProvisioningViewModel(
            provisioningManager,
        )
    }

    @Test
    fun `view emits state from ProvisioningManager`() = runTest {
        assertObservedValues(
            ProvisionState.WaitingToInitialize(false),// initial state
            ProvisionState.WaitingToInitialize(false),
        )


        testFlow.value = ProvisionState.Initialized
        advanceUntilIdle()


        assertObservedValues(
            ProvisionState.WaitingToInitialize(false),
            ProvisionState.WaitingToInitialize(false),
            ProvisionState.Initialized,
        )
    }
}
