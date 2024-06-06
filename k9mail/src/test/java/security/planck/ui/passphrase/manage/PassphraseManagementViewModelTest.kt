package security.planck.ui.passphrase.manage

import androidx.lifecycle.LiveData
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.testutils.CoroutineTestRule
import foundation.pEp.jniadapter.Pair
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.jsoup.helper.Validate
import org.junit.Rule
import org.junit.Test
import security.planck.common.LiveDataTest
import security.planck.passphrase.PassphraseFormatValidator
import security.planck.ui.passphrase.assertPairArrayList
import security.planck.ui.passphrase.models.AccountTextFieldState
import security.planck.ui.passphrase.models.AccountUsesPassphrase
import security.planck.ui.passphrase.models.PassphraseLoading
import security.planck.ui.passphrase.models.PassphraseMgmtState
import security.planck.ui.passphrase.models.PassphraseState
import security.planck.ui.passphrase.models.PassphraseVerificationStatus
import security.planck.ui.passphrase.models.SelectableItem
import security.planck.ui.passphrase.models.TextFieldState
import security.planck.ui.passphrase.models.TextFieldStateContract
import java.util.Vector

private const val EMAIL = "test@mail.ch"
private const val TEST_PASSPHRASE = "HelloPassphrase!!1"
private const val TEST_OLD_PASSPHRASE = "HelloPassphrase!!111"

@OptIn(ExperimentalCoroutinesApi::class)
class PassphraseManagementViewModelTest : LiveDataTest<PassphraseState>() {
    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()

    private val account: Account = mockk {
        every { email }.returns(EMAIL)
    }
    private val preferences: Preferences = mockk {
        every { availableAccounts }.answers { listOf(account) }
    }
    private val planckProvider: PlanckProvider = mockk {
        coEvery { hasPassphrase(any()) }.returns(Result.success(true))
        coEvery { managePassphrase(any(), any()) }.returns(Result.success(Vector()))
    }
    private val passphraseValidator: PassphraseFormatValidator = PassphraseFormatValidator()
    private val viewModel =
        PassphraseManagementViewModel(planckProvider, preferences, passphraseValidator)
    override val testLivedata: LiveData<PassphraseState>
        get() = viewModel.state

    @Test
    fun `initial state is Loading`() {
        assertObservedValues(PassphraseState.Loading)
    }

    @Test
    fun `start() loads accounts with or without passphrase from PlanckProvider`() = runTest {
        viewModel.start()
        advanceUntilIdle()


        coVerify { planckProvider.hasPassphrase(EMAIL) }
        customAssertObservedValues(
            { assertEquals(PassphraseState.Loading, it) },
            { assertEquals(PassphraseState.Loading, it) },
            { assertChoosingAccountsToManage(it) },
        )
    }

    @Test
    fun `start() sets CoreError status if PlanckProvider call fails`() = runTest {
        coEvery { planckProvider.hasPassphrase(any()) }.returns(
            Result.failure(
                TestException()
            )
        )


        viewModel.start()
        advanceUntilIdle()


        coVerify { planckProvider.hasPassphrase(EMAIL) }
        assertObservedValues(
            PassphraseState.Loading,
            PassphraseState.Loading,
            PassphraseState.CoreError(TestException())
        )
    }

    @Test
    fun `selectAccountsToManagePassphrase() sets state to ManagingAccounts`() = runTest {
        viewModel.start()
        advanceUntilIdle()
        coVerify { planckProvider.hasPassphrase(EMAIL) }


        viewModel.selectAccountsToManagePassphrase(listOf(AccountUsesPassphrase(EMAIL, true)))


        customAssertObservedValues(
            { assertEquals(PassphraseState.Loading, it) },
            { assertEquals(PassphraseState.Loading, it) },
            { assertChoosingAccountsToManage(it) },
            { assertManagingAccounts(it) },
        )
    }

    @Test
    fun `selectAccountsToManagePassphrase() takes into account if accounts use passphrase`() =
        runTest {
            coEvery { planckProvider.hasPassphrase(any()) }.returns(Result.success(false))
            viewModel.start()
            advanceUntilIdle()
            coVerify { planckProvider.hasPassphrase(EMAIL) }


            viewModel.selectAccountsToManagePassphrase(listOf(AccountUsesPassphrase(EMAIL, false)))


            customAssertObservedValues(
                { assertEquals(PassphraseState.Loading, it) },
                { assertEquals(PassphraseState.Loading, it) },
                { assertChoosingAccountsToManage(it, usesPassphrase = false) },
                { assertManagingAccounts(it, usesPassphrase = false) },
            )
        }

    @Test
    fun `setNewPassphrase() uses PlanckProvider to set new passphrases`() = runTest {
        viewModel.start()
        advanceUntilIdle()
        viewModel.selectAccountsToManagePassphrase(listOf(AccountUsesPassphrase(EMAIL, true)))


        val state = viewModel.state.value as PassphraseMgmtState.ManagingAccounts
        state.oldPasswordStates.first().textState = TEST_OLD_PASSPHRASE
        state.newPasswordState.textState = TEST_PASSPHRASE
        state.newPasswordVerificationState.textState = TEST_PASSPHRASE
        viewModel.setNewPassphrase(state)
        advanceUntilIdle()


        val arrayListSlot = slot<ArrayList<Pair<String, String>>>()
        coVerify { planckProvider.managePassphrase(capture(arrayListSlot), TEST_PASSPHRASE) }
        assertPairArrayList(
            listOf(Pair(EMAIL, TEST_OLD_PASSPHRASE)),
            arrayListSlot.captured
        )

        customAssertObservedValues(
            { assertEquals(PassphraseState.Loading, it) },
            { assertEquals(PassphraseState.Loading, it) },
            { assertChoosingAccountsToManage(it) },
            { assertManagingAccounts(it, expectedLoading = PassphraseLoading.Processing) },
            { assertEquals(PassphraseState.Success, it) },
        )
    }

    @Test
    fun `setNewPassphrase() sets state to CoreError if PlanckProvider call fails`() = runTest {
        coEvery { planckProvider.managePassphrase(any(), any()) }.returns(
            Result.failure(
                TestException()
            )
        )
        viewModel.start()
        advanceUntilIdle()
        viewModel.selectAccountsToManagePassphrase(listOf(AccountUsesPassphrase(EMAIL, true)))


        val state = viewModel.state.value as PassphraseMgmtState.ManagingAccounts
        state.oldPasswordStates.first().textState = TEST_OLD_PASSPHRASE
        state.newPasswordState.textState = TEST_PASSPHRASE
        state.newPasswordVerificationState.textState = TEST_PASSPHRASE
        viewModel.setNewPassphrase(state)
        advanceUntilIdle()


        customAssertObservedValues(
            { assertEquals(PassphraseState.Loading, it) },
            { assertEquals(PassphraseState.Loading, it) },
            { assertChoosingAccountsToManage(it) },
            {
                assertManagingAccounts(
                    it,
                    expectedStatus = PassphraseVerificationStatus.CORE_ERROR
                )
            },
        )
    }

    @Test
    fun `setNewPassphrase() offers retry to user`() = runTest {
        coEvery { planckProvider.managePassphrase(any(), any()) }.returns(
            Result.success(
                Vector(
                    listOf(EMAIL)
                )
            )
        )
        viewModel.start()
        advanceUntilIdle()
        viewModel.selectAccountsToManagePassphrase(listOf(AccountUsesPassphrase(EMAIL, true)))


        val state = viewModel.state.value as PassphraseMgmtState.ManagingAccounts
        state.oldPasswordStates.first().textState = TEST_OLD_PASSPHRASE
        state.newPasswordState.textState = TEST_PASSPHRASE
        state.newPasswordVerificationState.textState = TEST_PASSPHRASE
        viewModel.setNewPassphrase(state)
        advanceUntilIdle()


        customAssertObservedValues(
            { assertEquals(PassphraseState.Loading, it) },
            { assertEquals(PassphraseState.Loading, it) },
            { assertChoosingAccountsToManage(it) },
            {
                assertManagingAccounts(
                    it,
                    expectedStatus = PassphraseVerificationStatus.WRONG_PASSPHRASE
                )
            },
        )
    }

    private fun assertManagingAccounts(
        actual: PassphraseState,
        usesPassphrase: Boolean = true,
        expectedAccounts: List<AccountUsesPassphrase> = listOf(
            AccountUsesPassphrase(
                EMAIL,
                usesPassphrase
            )
        ),
        expectedOldStates: List<TextFieldStateContract> = if (usesPassphrase) listOf(
            AccountTextFieldState(EMAIL)
        ) else emptyList(),
        expectedNewPasswordState: TextFieldState = TextFieldState(errorStatus = TextFieldStateContract.ErrorStatus.SUCCESS),
        expectedNewPasswordVerificationState: TextFieldState = TextFieldState(errorStatus = TextFieldStateContract.ErrorStatus.SUCCESS),
        expectedLoading: PassphraseLoading? = null,
        expectedStatus: PassphraseVerificationStatus = PassphraseVerificationStatus.NONE,
    ) {
        if (actual is PassphraseMgmtState.ManagingAccounts) {
            assertEquals(expectedAccounts, actual.accounts)
            assertEquals(expectedOldStates, actual.oldPasswordStates.toList())
            assertEquals(expectedNewPasswordState, actual.newPasswordState)
            assertEquals(expectedNewPasswordVerificationState, actual.newPasswordVerificationState)
            assertEquals(expectedLoading, actual.loading.value)
            assertEquals(expectedStatus, actual.status.value)
        } else Validate.fail("Wrong type. Expected ManagingAccounts but got ${actual.javaClass.simpleName}")
    }

    private fun assertChoosingAccountsToManage(
        actual: PassphraseState,
        usesPassphrase: Boolean = true,
        expectedStates: List<SelectableItem<AccountUsesPassphrase>> = listOf(
            SelectableItem(
                AccountUsesPassphrase(EMAIL, usesPassphrase)
            )
        ),
    ) {
        if (actual is PassphraseMgmtState.ChoosingAccountsToManage) {
            assertEquals(expectedStates, actual.accountsUsingPassphrase.toList())
        } else Validate.fail("Wrong type. Expected ChoosingAccountsToManage but got ${actual.javaClass.simpleName}")
    }

    private data class TestException(override val message: String = "test") : Throwable()
}
