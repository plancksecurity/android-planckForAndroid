package security.planck.ui.leavedevicegroup

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.infrastructure.ResultCompat
import com.fsck.k9.planck.testutils.CoroutineTestRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import junit.framework.TestCase
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import security.planck.dialog.BackgroundTaskDialogView
import security.planck.sync.SyncRepository

@ExperimentalCoroutinesApi
class LeaveDeviceGroupViewModelTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()

    private val planckProvider: PlanckProvider = mockk()
    private val syncRepository: SyncRepository = mockk(relaxed = true)

    private val receivedStates = mutableListOf<BackgroundTaskDialogView.State>()
    private val viewModel = LeaveDeviceGroupViewModel(planckProvider, syncRepository)

    @Before
    fun setUp() {
        coEvery { planckProvider.isDeviceGrouped() }.returns(false)
        coEvery { planckProvider.leaveDeviceGroup() }.returns(ResultCompat.success(Unit))
        receivedStates.clear()
        observeViewModel()
    }

    @Test
    fun `initial state is confirmation`() = runTest {
        assertStates(BackgroundTaskDialogView.State.CONFIRMATION)
    }

    @Test
    fun `leaveDeviceGroup() initially sets state to loading`() = runTest {
        viewModel.leaveDeviceGroup()
        advanceUntilIdle()


        assertEquals(BackgroundTaskDialogView.State.CONFIRMATION, receivedStates.first())
        assertEquals(BackgroundTaskDialogView.State.LOADING, receivedStates[1])
    }

    @Test
    fun `leaveDeviceGroup() uses PlanckProvider to leave device group`() = runTest {
        viewModel.leaveDeviceGroup()
        advanceUntilIdle()


        coVerify { planckProvider.leaveDeviceGroup() }
    }

    @Test
    fun `leaveDeviceGroup() on success sets SyncDelegate_isDeviceGrouped to false and state to success`() =
        runTest {
            viewModel.leaveDeviceGroup()
            advanceUntilIdle()


            coVerify { planckProvider.isDeviceGrouped() }
            coVerify { syncRepository.isGrouped = false }
            assertStates(
                BackgroundTaskDialogView.State.CONFIRMATION,
                BackgroundTaskDialogView.State.LOADING,
                BackgroundTaskDialogView.State.SUCCESS
            )
        }

    @Test
    fun `leaveDeviceGroup() on failure sets state to error`() = runTest {
        coEvery { planckProvider.leaveDeviceGroup() }.returns(
            ResultCompat.failure(
                RuntimeException(
                    "test"
                )
            )
        )
        viewModel.leaveDeviceGroup()
        advanceUntilIdle()


        assertStates(
            BackgroundTaskDialogView.State.CONFIRMATION,
            BackgroundTaskDialogView.State.LOADING,
            BackgroundTaskDialogView.State.ERROR
        )
    }

    @Test
    fun `leaveDeviceGroup() on success waits in loop if PlanckProvider_isDeviceGrouped returns true and it sets state to Error in Debug`() =
        runTest {
            coEvery { planckProvider.isDeviceGrouped() }.returns(true)
            viewModel.leaveDeviceGroup()
            advanceUntilIdle()


            coVerify(exactly = 6) { planckProvider.isDeviceGrouped() }
            coVerify { syncRepository.isGrouped = false }
            assertStates(
                BackgroundTaskDialogView.State.CONFIRMATION,
                BackgroundTaskDialogView.State.LOADING,
                BackgroundTaskDialogView.State.ERROR
            )
        }

    private fun assertStates(vararg states: BackgroundTaskDialogView.State) {
        TestCase.assertEquals(states.toList(), receivedStates)
    }

    private fun observeViewModel() {
        viewModel.state.observeForever { value ->
            receivedStates.add(value)
        }
    }
}