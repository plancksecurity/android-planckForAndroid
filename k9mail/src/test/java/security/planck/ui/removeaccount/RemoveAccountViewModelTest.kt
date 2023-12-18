package security.planck.ui.removeaccount

import android.app.Application
import androidx.lifecycle.LiveData
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.mailstore.LocalStore
import com.fsck.k9.planck.testutils.CoroutineTestRule
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import security.planck.common.LiveDataTest

private const val ACCOUNT_DESCRIPTION = "description"
private const val ACCOUNT_EMAIL = "email"
private const val ACCOUNT_UUID = "uuid"

@ExperimentalCoroutinesApi
class RemoveAccountViewModelTest : LiveDataTest<RemoveAccountState>() {
    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()

    private val app: Application = mockk()
    private val localStore: LocalStore = mockk()
    private val account: Account = mockk {
        every { description }.answers { ACCOUNT_DESCRIPTION }
        every { email }.returns(ACCOUNT_EMAIL)
        every { localStore }.returns(this@RemoveAccountViewModelTest.localStore)
    }
    private val preferences: Preferences = mockk {
        every { getAccount(any()) }.returns(account)
        every { deleteAccount(any()) }.just(runs)
    }
    private val controller: MessagingController = mockk {
        every { deleteAccount(any()) }.just(runs)
    }
    private val viewModel = RemoveAccountViewModel(
        app,
        preferences,
        controller,
        coroutinesTestRule.testDispatcherProvider
    )

    override fun initialize() {
        mockkStatic(K9::class)
        every { K9.setServicesEnabled(any()) }.just(runs)
    }

    @After
    fun tearDown() {
        unmockkStatic(K9::class)
    }

    @Test
    fun `state is initially Idle`() {
        assertObservedValues(RemoveAccountState.Idle)
    }

    @Test
    fun `initialize() uses Preferences to retrieve account from uuid`() = runTest {
        viewModel.initialize(ACCOUNT_UUID)
        advanceUntilIdle()


        verify { preferences.getAccount(ACCOUNT_UUID) }
    }

    @Test
    fun `initialize() sets state to RemoveAccountConfirmation if account is retrieved correctly`() =
        runTest {
            viewModel.initialize(ACCOUNT_UUID)
            advanceUntilIdle()


            verify { preferences.getAccount(ACCOUNT_UUID) }
            assertObservedValues(
                RemoveAccountState.Idle,
                RemoveAccountState.RemoveAccountConfirmation(ACCOUNT_DESCRIPTION)
            )
        }

    @Test
    fun `initialize() sets state to AccountNotAvailable if account was already deleted`() =
        runTest {
            every { preferences.getAccount(any()) }.returns(null)


            viewModel.initialize(ACCOUNT_UUID)
            advanceUntilIdle()


            verify { preferences.getAccount(ACCOUNT_UUID) }
            assertObservedValues(
                RemoveAccountState.Idle,
                RemoveAccountState.AccountNotAvailable(ACCOUNT_UUID)
            )
        }

    @Test
    fun `negativeAction() sets state to Finish`() = runTest {
        viewModel.initialize(ACCOUNT_UUID)
        advanceUntilIdle()
        viewModel.negativeAction()


        assertObservedValues(
            RemoveAccountState.Idle,
            RemoveAccountState.RemoveAccountConfirmation(ACCOUNT_DESCRIPTION),
            RemoveAccountState.Finish(false)
        )
    }

    @Test
    fun `positiveAction() in state RemoveAccountConfirmation removes account`() = runTest {
        viewModel.initialize(ACCOUNT_UUID)
        advanceUntilIdle()
        viewModel.positiveAction()
        advanceUntilIdle()


        verify { account.localStore }
        verify { localStore.delete() }
        verify { controller.deleteAccount(account) }
        verify { preferences.deleteAccount(account) }
        verify { K9.setServicesEnabled(app) }

        assertObservedValues(
            RemoveAccountState.Idle,
            RemoveAccountState.RemoveAccountConfirmation(ACCOUNT_DESCRIPTION),
            RemoveAccountState.RemovingAccount,
            RemoveAccountState.Done(ACCOUNT_DESCRIPTION)
        )
    }

    @Test
    fun `positiveAction() in state Done sets state to Finish`() = runTest {
        viewModel.initialize(ACCOUNT_UUID)
        advanceUntilIdle()
        viewModel.positiveAction()
        advanceUntilIdle()
        viewModel.positiveAction()


        assertObservedValues(
            RemoveAccountState.Idle,
            RemoveAccountState.RemoveAccountConfirmation(ACCOUNT_DESCRIPTION),
            RemoveAccountState.RemovingAccount,
            RemoveAccountState.Done(ACCOUNT_DESCRIPTION),
            RemoveAccountState.Finish(true)
        )
    }

    @Test
    fun `positiveAction() in state AccountNotAvailable sets state to Finish`() =
        runTest {
            every { preferences.getAccount(any()) }.returns(null)


            viewModel.initialize(ACCOUNT_UUID)
            advanceUntilIdle()
            viewModel.positiveAction()


            verify { preferences.getAccount(ACCOUNT_UUID) }
            assertObservedValues(
                RemoveAccountState.Idle,
                RemoveAccountState.AccountNotAvailable(ACCOUNT_UUID),
                RemoveAccountState.Finish(true)
            )
        }

    override val testLivedata: LiveData<RemoveAccountState>
        get() = viewModel.state
}