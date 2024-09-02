package security.planck.ui.passphrase.unlock

import androidx.lifecycle.LiveData
import com.fsck.k9.planck.infrastructure.livedata.Event
import com.fsck.k9.planck.testutils.CoroutineTestRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import security.planck.common.LiveDataEventTest
import security.planck.passphrase.PassphraseRepository

@OptIn(ExperimentalCoroutinesApi::class)
class PassphraseUnlockNotificationViewModelTest : LiveDataEventTest<Boolean>() {

    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()

    private val repository: PassphraseRepository = mockk()

    private lateinit var viewModel: PassphraseUnlockNotificationViewModel

    private val testFlow = MutableStateFlow(PassphraseRepository.UnlockState.LOADING)
    override val testLivedata: LiveData<Event<Boolean>>
        get() = viewModel.needsPassphraseUnlock

    override fun initialize() {
        every { repository.lockedState }.returns(testFlow)
        viewModel = PassphraseUnlockNotificationViewModel(repository)
    }

    @Test
    fun `initially repository has UnlockState_LOADING and ViewModel has false`() {
        assertObservedValues(false)
    }

    @Test
    fun `when repository state becomes LOCKED, ViewModel value is true`() = runTest {
        testFlow.value = PassphraseRepository.UnlockState.LOCKED
        advanceUntilIdle()

        assertObservedValues(false, false, true)
    }

    @Test
    fun `when repository state becomes UNLOCKED, ViewModel value is false`() = runTest {
        testFlow.value = PassphraseRepository.UnlockState.UNLOCKED
        advanceUntilIdle()

        assertObservedValues(false, false, false)
    }
}