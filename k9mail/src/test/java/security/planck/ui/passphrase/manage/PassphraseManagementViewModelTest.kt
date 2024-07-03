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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import security.planck.common.LiveDataTest
import security.planck.passphrase.PassphraseFormatValidator
import security.planck.ui.passphrase.assertPairArrayList
import security.planck.ui.passphrase.models.AccountTextFieldState
import security.planck.ui.passphrase.models.PassphraseMgmtState
import security.planck.ui.passphrase.models.PassphraseState
import security.planck.ui.passphrase.models.PassphraseVerificationStatus
import security.planck.ui.passphrase.models.SelectableItem
import security.planck.ui.passphrase.models.TextFieldState
import security.planck.ui.passphrase.models.TextFieldStateContract
import java.util.Vector

private const val EMAIL = "test@mail.ch"
private const val EMAIL2 = "test2@mail.ch"
private const val TEST_PASSPHRASE = "HelloPassphrase!!1"
private const val TEST_OLD_PASSPHRASE = "HelloPassphrase!!111"

@OptIn(ExperimentalCoroutinesApi::class)
class PassphraseManagementViewModelTest : LiveDataTest<PassphraseState>() {
    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()

    private val account: Account = mockk {
        every { email }.returns(EMAIL)
    }
    private val account2: Account = mockk {
        every { email }.returns(EMAIL2)
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
    fun `initial state is Processing`() {
        assertObservedValues(PassphraseState.Processing)
    }

    @Test
    fun `start() loads accounts with or without passphrase from PlanckProvider`() = runTest {
        viewModel.start()
        advanceUntilIdle()


        coVerify { planckProvider.hasPassphrase(EMAIL) }
        assertObservedValues(
            PassphraseState.Processing,
            PassphraseState.Processing,
            PassphraseMgmtState.ChoosingAccountsToManage(listOf(SelectableItem(EMAIL))),
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
            PassphraseState.Processing,
            PassphraseState.Processing,
            PassphraseState.CoreError(TestException())
        )
    }

    @Test
    fun `accountClicked() sets state to ManagingAccounts`() = runTest {
        viewModel.start()
        advanceUntilIdle()
        coVerify { planckProvider.hasPassphrase(EMAIL) }


        viewModel.accountClicked(0)


        assertObservedValues(
            PassphraseState.Processing,
            PassphraseState.Processing,
            PassphraseMgmtState.ChoosingAccountsToManage(listOf(SelectableItem(EMAIL))),
            PassphraseMgmtState.ManagingAccounts(
                accountsWithNoPassphrase = emptyList(),
                oldPasswordStates = listOf(AccountTextFieldState(EMAIL)),
                newPasswordState = TextFieldState(),
                newPasswordVerificationState = TextFieldState(),
            ),
        )
    }

    @Test
    fun `accountLongClicked() selects accounts and goToManagePassphrase() sets state to ManagingAccounts`() =
        runTest {
            viewModel.start()
            advanceUntilIdle()
            coVerify { planckProvider.hasPassphrase(EMAIL) }


            viewModel.accountLongClicked(0)
            viewModel.goToManagePassphrase()


            assertObservedValues(
                PassphraseState.Processing,
                PassphraseState.Processing,
                PassphraseMgmtState.ChoosingAccountsToManage(listOf(SelectableItem(EMAIL))),
                PassphraseMgmtState.ChoosingAccountsToManage(
                    listOf(
                        SelectableItem(
                            EMAIL,
                            selected = true
                        )
                    )
                ),
                PassphraseMgmtState.ManagingAccounts(
                    accountsWithNoPassphrase = emptyList(),
                    oldPasswordStates = listOf(AccountTextFieldState(EMAIL)),
                    newPasswordState = TextFieldState(),
                    newPasswordVerificationState = TextFieldState(),
                ),
            )
        }

    @Test
    fun `accountLongClicked() on an account in action mode just selects it`() =
        runTest {
            every { preferences.availableAccounts }.answers { listOf(account, account2) }

            viewModel.start()
            advanceUntilIdle()
            coVerify { planckProvider.hasPassphrase(EMAIL) }


            viewModel.accountLongClicked(0)
            viewModel.accountLongClicked(1)
            viewModel.goToManagePassphrase()


            assertObservedValues(
                PassphraseState.Processing,
                PassphraseState.Processing,
                PassphraseMgmtState.ChoosingAccountsToManage(
                    listOf(
                        SelectableItem(EMAIL),
                        SelectableItem(EMAIL2)
                    )
                ),
                PassphraseMgmtState.ChoosingAccountsToManage(
                    listOf(
                        SelectableItem(
                            EMAIL,
                            selected = true
                        ),
                        SelectableItem(
                            EMAIL2,
                            selected = false
                        )
                    )
                ),
                PassphraseMgmtState.ChoosingAccountsToManage(
                    listOf(
                        SelectableItem(
                            EMAIL,
                            selected = true
                        ),
                        SelectableItem(
                            EMAIL2,
                            selected = true
                        )
                    )
                ),
                PassphraseMgmtState.ManagingAccounts(
                    accountsWithNoPassphrase = emptyList(),
                    oldPasswordStates = listOf(
                        AccountTextFieldState(EMAIL),
                        AccountTextFieldState(EMAIL2)
                    ),
                    newPasswordState = TextFieldState(),
                    newPasswordVerificationState = TextFieldState(),
                ),
            )
        }

    @Test
    fun `accountClicked() on an account in action mode just selects it`() =
        runTest {
            every { preferences.availableAccounts }.answers { listOf(account, account2) }

            viewModel.start()
            advanceUntilIdle()
            coVerify { planckProvider.hasPassphrase(EMAIL) }


            viewModel.accountLongClicked(0)
            viewModel.accountClicked(1)
            viewModel.goToManagePassphrase()


            assertObservedValues(
                PassphraseState.Processing,
                PassphraseState.Processing,
                PassphraseMgmtState.ChoosingAccountsToManage(
                    listOf(
                        SelectableItem(EMAIL),
                        SelectableItem(EMAIL2)
                    )
                ),
                PassphraseMgmtState.ChoosingAccountsToManage(
                    listOf(
                        SelectableItem(
                            EMAIL,
                            selected = true
                        ),
                        SelectableItem(
                            EMAIL2,
                            selected = false
                        )
                    )
                ),
                PassphraseMgmtState.ChoosingAccountsToManage(
                    listOf(
                        SelectableItem(
                            EMAIL,
                            selected = true
                        ),
                        SelectableItem(
                            EMAIL2,
                            selected = true
                        )
                    )
                ),
                PassphraseMgmtState.ManagingAccounts(
                    accountsWithNoPassphrase = emptyList(),
                    oldPasswordStates = listOf(
                        AccountTextFieldState(EMAIL),
                        AccountTextFieldState(EMAIL2)
                    ),
                    newPasswordState = TextFieldState(),
                    newPasswordVerificationState = TextFieldState(),
                ),
            )
        }

    @Test
    fun `accountLongClicked() on a selected account just deselects it`() =
        runTest {
            viewModel.start()
            advanceUntilIdle()
            coVerify { planckProvider.hasPassphrase(EMAIL) }


            viewModel.accountLongClicked(0)
            viewModel.accountLongClicked(0)


            assertObservedValues(
                PassphraseState.Processing,
                PassphraseState.Processing,
                PassphraseMgmtState.ChoosingAccountsToManage(listOf(SelectableItem(EMAIL))),
                PassphraseMgmtState.ChoosingAccountsToManage(
                    listOf(
                        SelectableItem(
                            EMAIL,
                            selected = true
                        )
                    )
                ),
                PassphraseMgmtState.ChoosingAccountsToManage(listOf(SelectableItem(EMAIL))),
            )
        }

    @Test
    fun `accountClicked() does not take into account if accounts use passphrase, but viewmodel does`() =
        runTest {
            coEvery { planckProvider.hasPassphrase(any()) }.returns(Result.success(false))
            viewModel.start()
            advanceUntilIdle()
            coVerify { planckProvider.hasPassphrase(EMAIL) }


            viewModel.accountClicked(0)


            assertObservedValues(
                PassphraseState.Processing,
                PassphraseState.Processing,
                PassphraseMgmtState.ChoosingAccountsToManage(listOf(SelectableItem(EMAIL))),
                PassphraseMgmtState.ManagingAccounts(
                    accountsWithNoPassphrase = listOf(EMAIL),
                    oldPasswordStates = emptyList(),
                    newPasswordState = TextFieldState(),
                    newPasswordVerificationState = TextFieldState(),
                ),
            )
        }

    @Test
    fun `account with old passphrase allowing passphrase removal`() = runTest {
        viewModel.start()
        advanceUntilIdle()
        viewModel.accountClicked(0)
        viewModel.updateAndValidateText(0, TEST_OLD_PASSPHRASE)


        assertObservedValues(
            *happyPathOldPassphraseAllowRemoval
        )
    }

    @Test
    fun `account with old passphrase, old passphrase format error`() = runTest {
        viewModel.start()
        advanceUntilIdle()
        viewModel.accountClicked(0)
        viewModel.updateAndValidateText(0, "wrong")


        assertObservedValues(
            PassphraseState.Processing, //0
            PassphraseState.Processing, //1
            PassphraseMgmtState.ChoosingAccountsToManage(listOf(SelectableItem(EMAIL))), //2
            PassphraseMgmtState.ManagingAccounts(
                //3
                accountsWithNoPassphrase = emptyList(),
                oldPasswordStates = listOf(AccountTextFieldState(EMAIL)),
                newPasswordState = TextFieldState(),
                newPasswordVerificationState = TextFieldState(),
            ),
            PassphraseMgmtState.ManagingAccounts(
                //4
                accountsWithNoPassphrase = emptyList(),
                oldPasswordStates = listOf(
                    AccountTextFieldState(
                        EMAIL,
                        text = "wrong",
                        errorStatus = TextFieldStateContract.ErrorStatus.ERROR
                    )
                ),
                newPasswordState = TextFieldState(),
                newPasswordVerificationState = TextFieldState(),
            ),
            PassphraseMgmtState.ManagingAccounts(
                //5
                accountsWithNoPassphrase = emptyList(),
                oldPasswordStates = listOf(
                    AccountTextFieldState(
                        EMAIL,
                        text = "wrong",
                        errorStatus = TextFieldStateContract.ErrorStatus.ERROR
                    )
                ),
                newPasswordState = TextFieldState(),
                newPasswordVerificationState = TextFieldState(),
                status = PassphraseVerificationStatus.WRONG_FORMAT
            ),
        )
    }

    @Test
    fun `account with old passphrase allowing passphrase change`() = runTest {
        viewModel.start()
        advanceUntilIdle()
        viewModel.accountClicked(0)
        viewModel.updateAndValidateText(0, TEST_OLD_PASSPHRASE)
        viewModel.updateAndValidateText(1, TEST_PASSPHRASE)
        viewModel.updateAndValidateText(2, TEST_PASSPHRASE)


        assertObservedValues(
            *happyPathOldPassphraseAllowChange
        )
    }

    @Test
    fun `account with no old passphrase allowing passphrase change`() = runTest {
        coEvery { planckProvider.hasPassphrase(any()) }.returns(Result.success(false))
        viewModel.start()
        advanceUntilIdle()
        viewModel.accountClicked(0)
        viewModel.updateAndValidateText(0, TEST_PASSPHRASE)
        viewModel.updateAndValidateText(1, TEST_PASSPHRASE)


        assertObservedValues(
            PassphraseState.Processing, //0
            PassphraseState.Processing, //1
            PassphraseMgmtState.ChoosingAccountsToManage(listOf(SelectableItem(EMAIL))), //2
            PassphraseMgmtState.ManagingAccounts(
                //3
                accountsWithNoPassphrase = listOf(EMAIL),
                oldPasswordStates = emptyList(),
                newPasswordState = TextFieldState(),
                newPasswordVerificationState = TextFieldState(),
            ),
            PassphraseMgmtState.ManagingAccounts(
                //4
                accountsWithNoPassphrase = listOf(EMAIL),
                oldPasswordStates = emptyList(),
                newPasswordState = TextFieldState(
                    text = TEST_PASSPHRASE,
                    errorStatus = TextFieldStateContract.ErrorStatus.SUCCESS
                ),
                newPasswordVerificationState = TextFieldState(),
            ),
            PassphraseMgmtState.ManagingAccounts(
                //5
                accountsWithNoPassphrase = listOf(EMAIL),
                oldPasswordStates = emptyList(),
                newPasswordState = TextFieldState(
                    text = TEST_PASSPHRASE,
                    errorStatus = TextFieldStateContract.ErrorStatus.SUCCESS
                ),
                newPasswordVerificationState = TextFieldState(
                    text = TEST_PASSPHRASE,
                    errorStatus = TextFieldStateContract.ErrorStatus.SUCCESS
                ),
            ),
            PassphraseMgmtState.ManagingAccounts( //6
                accountsWithNoPassphrase = listOf(EMAIL),
                oldPasswordStates = emptyList(),
                newPasswordState = TextFieldState(
                    text = TEST_PASSPHRASE,
                    errorStatus = TextFieldStateContract.ErrorStatus.SUCCESS
                ),
                newPasswordVerificationState = TextFieldState(
                    text = TEST_PASSPHRASE,
                    errorStatus = TextFieldStateContract.ErrorStatus.SUCCESS
                ),
                status = PassphraseVerificationStatus.SUCCESS
            ),
        )
    }

    @Test
    fun `new passphrase validation format error`() = runTest {
        coEvery { planckProvider.hasPassphrase(any()) }.returns(Result.success(false))
        viewModel.start()
        advanceUntilIdle()
        viewModel.accountClicked(0)
        viewModel.updateAndValidateText(0, "wrong")


        assertObservedValues(
            PassphraseState.Processing, //0
            PassphraseState.Processing, //1
            PassphraseMgmtState.ChoosingAccountsToManage(listOf(SelectableItem(EMAIL))), //2
            PassphraseMgmtState.ManagingAccounts(
                //3
                accountsWithNoPassphrase = listOf(EMAIL),
                oldPasswordStates = emptyList(),
                newPasswordState = TextFieldState(),
                newPasswordVerificationState = TextFieldState(),
            ),
            PassphraseMgmtState.ManagingAccounts(
                //4
                accountsWithNoPassphrase = listOf(EMAIL),
                oldPasswordStates = emptyList(),
                newPasswordState = TextFieldState(
                    text = "wrong",
                    errorStatus = TextFieldStateContract.ErrorStatus.ERROR
                ),
                newPasswordVerificationState = TextFieldState(),
            ),
            PassphraseMgmtState.ManagingAccounts(
                //5
                accountsWithNoPassphrase = listOf(EMAIL),
                oldPasswordStates = emptyList(),
                newPasswordState = TextFieldState(
                    text = "wrong",
                    errorStatus = TextFieldStateContract.ErrorStatus.ERROR
                ),
                newPasswordVerificationState = TextFieldState(),
                status = PassphraseVerificationStatus.WRONG_FORMAT
            ),
        )
    }

    @Test
    fun `new passphrase verification error`() = runTest {
        coEvery { planckProvider.hasPassphrase(any()) }.returns(Result.success(false))
        viewModel.start()
        advanceUntilIdle()
        viewModel.accountClicked(0)
        viewModel.updateAndValidateText(0, TEST_PASSPHRASE)
        viewModel.updateAndValidateText(1, "wrong")


        assertObservedValues(
            PassphraseState.Processing, //0
            PassphraseState.Processing, //1
            PassphraseMgmtState.ChoosingAccountsToManage(listOf(SelectableItem(EMAIL))), //2
            PassphraseMgmtState.ManagingAccounts(
                //3
                accountsWithNoPassphrase = listOf(EMAIL),
                oldPasswordStates = emptyList(),
                newPasswordState = TextFieldState(),
                newPasswordVerificationState = TextFieldState(),
            ),
            PassphraseMgmtState.ManagingAccounts(
                //4
                accountsWithNoPassphrase = listOf(EMAIL),
                oldPasswordStates = emptyList(),
                newPasswordState = TextFieldState(
                    text = TEST_PASSPHRASE,
                    errorStatus = TextFieldStateContract.ErrorStatus.SUCCESS
                ),
                newPasswordVerificationState = TextFieldState(),
            ),
            PassphraseMgmtState.ManagingAccounts(
                //5
                accountsWithNoPassphrase = listOf(EMAIL),
                oldPasswordStates = emptyList(),
                newPasswordState = TextFieldState(
                    text = TEST_PASSPHRASE,
                    errorStatus = TextFieldStateContract.ErrorStatus.SUCCESS
                ),
                newPasswordVerificationState = TextFieldState(
                    text = "wrong",
                    errorStatus = TextFieldStateContract.ErrorStatus.ERROR
                ),
            ),
            PassphraseMgmtState.ManagingAccounts(
                //6
                accountsWithNoPassphrase = listOf(EMAIL),
                oldPasswordStates = emptyList(),
                newPasswordState = TextFieldState(
                    text = TEST_PASSPHRASE,
                    errorStatus = TextFieldStateContract.ErrorStatus.SUCCESS
                ),
                newPasswordVerificationState = TextFieldState(
                    text = "wrong",
                    errorStatus = TextFieldStateContract.ErrorStatus.ERROR
                ),
                status = PassphraseVerificationStatus.NEW_PASSPHRASE_DOES_NOT_MATCH
            ),
        )
    }

    @Test
    fun `setNewPassphrase() uses PlanckProvider to set new passphrase`() = runTest {
        viewModel.start()
        advanceUntilIdle()
        viewModel.accountClicked(0)
        viewModel.updateAndValidateText(0, TEST_OLD_PASSPHRASE)
        viewModel.updateAndValidateText(1, TEST_PASSPHRASE)
        viewModel.updateAndValidateText(2, TEST_PASSPHRASE)

        viewModel.setNewPassphrase()
        advanceUntilIdle()


        val arrayListSlot = slot<ArrayList<Pair<String, String>>>()
        coVerify { planckProvider.managePassphrase(capture(arrayListSlot), TEST_PASSPHRASE) }
        assertPairArrayList(
            listOf(Pair(EMAIL, TEST_OLD_PASSPHRASE)),
            arrayListSlot.captured
        )

        assertObservedValues(
            *happyPathOldPassphraseAllowChange,
            PassphraseState.Processing,
            PassphraseState.Success,
        )
    }

    @Test
    fun `setNewPassphrase() uses PlanckProvider to remove passphrase`() = runTest {
        viewModel.start()
        advanceUntilIdle()
        viewModel.accountClicked(0)
        viewModel.updateAndValidateText(0, TEST_OLD_PASSPHRASE)

        viewModel.setNewPassphrase()
        advanceUntilIdle()


        val arrayListSlot = slot<ArrayList<Pair<String, String>>>()
        coVerify { planckProvider.managePassphrase(capture(arrayListSlot), "") }
        assertPairArrayList(
            listOf(Pair(EMAIL, TEST_OLD_PASSPHRASE)),
            arrayListSlot.captured
        )

        assertObservedValues(
            *happyPathOldPassphraseAllowRemoval,
            PassphraseState.Processing,
            PassphraseState.Success,
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
        viewModel.accountClicked(0)
        viewModel.updateAndValidateText(0, TEST_OLD_PASSPHRASE)

        viewModel.setNewPassphrase()
        advanceUntilIdle()


        assertObservedValues(
            *happyPathOldPassphraseAllowRemoval,
            PassphraseState.Processing,
            PassphraseMgmtState.ManagingAccounts( //5
                accountsWithNoPassphrase = emptyList(),
                oldPasswordStates = listOf(
                    AccountTextFieldState(
                        EMAIL,
                        text = TEST_OLD_PASSPHRASE,
                        errorStatus = TextFieldStateContract.ErrorStatus.SUCCESS
                    )
                ),
                newPasswordState = TextFieldState(),
                newPasswordVerificationState = TextFieldState(),
                status = PassphraseVerificationStatus.CORE_ERROR
            ),
        )
    }

    @Test
    fun `setNewPassphrase() sets state to WrongPassphrase if old password does not match`() =
        runTest {
            coEvery { planckProvider.managePassphrase(any(), any()) }.returns(
                Result.success(
                    Vector(
                        listOf(EMAIL)
                    )
                )
            )
            viewModel.start()
            advanceUntilIdle()
            viewModel.accountClicked(0)
            viewModel.updateAndValidateText(0, TEST_OLD_PASSPHRASE)

            viewModel.setNewPassphrase()
            advanceUntilIdle()


            assertObservedValues(
                *happyPathOldPassphraseAllowRemoval,
                PassphraseState.Processing,
                PassphraseMgmtState.ManagingAccounts( //5
                    accountsWithNoPassphrase = emptyList(),
                    oldPasswordStates = listOf(
                        AccountTextFieldState(
                            EMAIL,
                            text = TEST_OLD_PASSPHRASE,
                            errorStatus = TextFieldStateContract.ErrorStatus.ERROR
                        )
                    ),
                    newPasswordState = TextFieldState(),
                    newPasswordVerificationState = TextFieldState(),
                    status = PassphraseVerificationStatus.WRONG_PASSPHRASE
                ),
            )
        }

    private val happyPathOldPassphraseAllowChange = arrayOf(
        PassphraseState.Processing, //0
        PassphraseState.Processing, //1
        PassphraseMgmtState.ChoosingAccountsToManage(listOf(SelectableItem(EMAIL))), //2
        PassphraseMgmtState.ManagingAccounts(
            //3
            accountsWithNoPassphrase = emptyList(),
            oldPasswordStates = listOf(AccountTextFieldState(EMAIL)),
            newPasswordState = TextFieldState(),
            newPasswordVerificationState = TextFieldState(),
        ),
        PassphraseMgmtState.ManagingAccounts(
            //4
            accountsWithNoPassphrase = emptyList(),
            oldPasswordStates = listOf(
                AccountTextFieldState(
                    EMAIL,
                    text = TEST_OLD_PASSPHRASE,
                    errorStatus = TextFieldStateContract.ErrorStatus.SUCCESS
                )
            ),
            newPasswordState = TextFieldState(),
            newPasswordVerificationState = TextFieldState(),
        ),
        PassphraseMgmtState.ManagingAccounts( //5
            accountsWithNoPassphrase = emptyList(),
            oldPasswordStates = listOf(
                AccountTextFieldState(
                    EMAIL,
                    text = TEST_OLD_PASSPHRASE,
                    errorStatus = TextFieldStateContract.ErrorStatus.SUCCESS
                )
            ),
            newPasswordState = TextFieldState(),
            newPasswordVerificationState = TextFieldState(),
            status = PassphraseVerificationStatus.SUCCESS_EMPTY
        ),
        PassphraseMgmtState.ManagingAccounts( //6
            accountsWithNoPassphrase = emptyList(),
            oldPasswordStates = listOf(
                AccountTextFieldState(
                    EMAIL,
                    text = TEST_OLD_PASSPHRASE,
                    errorStatus = TextFieldStateContract.ErrorStatus.SUCCESS
                )
            ),
            newPasswordState = TextFieldState(
                text = TEST_PASSPHRASE,
                errorStatus = TextFieldStateContract.ErrorStatus.SUCCESS
            ),
            newPasswordVerificationState = TextFieldState(),
            status = PassphraseVerificationStatus.SUCCESS_EMPTY
        ),
        PassphraseMgmtState.ManagingAccounts( //7
            accountsWithNoPassphrase = emptyList(),
            oldPasswordStates = listOf(
                AccountTextFieldState(
                    EMAIL,
                    text = TEST_OLD_PASSPHRASE,
                    errorStatus = TextFieldStateContract.ErrorStatus.SUCCESS
                )
            ),
            newPasswordState = TextFieldState(
                text = TEST_PASSPHRASE,
                errorStatus = TextFieldStateContract.ErrorStatus.SUCCESS
            ),
            newPasswordVerificationState = TextFieldState(),
            status = PassphraseVerificationStatus.NONE
        ),
        PassphraseMgmtState.ManagingAccounts(
            //8
            accountsWithNoPassphrase = emptyList(),
            oldPasswordStates = listOf(
                AccountTextFieldState(
                    EMAIL,
                    text = TEST_OLD_PASSPHRASE,
                    errorStatus = TextFieldStateContract.ErrorStatus.SUCCESS
                )
            ),
            newPasswordState = TextFieldState(
                text = TEST_PASSPHRASE,
                errorStatus = TextFieldStateContract.ErrorStatus.SUCCESS
            ),
            newPasswordVerificationState = TextFieldState(
                text = TEST_PASSPHRASE,
                errorStatus = TextFieldStateContract.ErrorStatus.SUCCESS
            ),
        ),
        PassphraseMgmtState.ManagingAccounts( //9
            accountsWithNoPassphrase = emptyList(),
            oldPasswordStates = listOf(
                AccountTextFieldState(
                    EMAIL,
                    text = TEST_OLD_PASSPHRASE,
                    errorStatus = TextFieldStateContract.ErrorStatus.SUCCESS
                )
            ),
            newPasswordState = TextFieldState(
                text = TEST_PASSPHRASE,
                errorStatus = TextFieldStateContract.ErrorStatus.SUCCESS
            ),
            newPasswordVerificationState = TextFieldState(
                text = TEST_PASSPHRASE,
                errorStatus = TextFieldStateContract.ErrorStatus.SUCCESS
            ),
            status = PassphraseVerificationStatus.SUCCESS
        ),
    )

    private val happyPathOldPassphraseAllowRemoval = arrayOf(
        PassphraseState.Processing, //0
        PassphraseState.Processing, //1
        PassphraseMgmtState.ChoosingAccountsToManage(listOf(SelectableItem(EMAIL))), //2
        PassphraseMgmtState.ManagingAccounts(
            //3
            accountsWithNoPassphrase = emptyList(),
            oldPasswordStates = listOf(AccountTextFieldState(EMAIL)),
            newPasswordState = TextFieldState(),
            newPasswordVerificationState = TextFieldState(),
        ),
        PassphraseMgmtState.ManagingAccounts(
            //4
            accountsWithNoPassphrase = emptyList(),
            oldPasswordStates = listOf(
                AccountTextFieldState(
                    EMAIL,
                    text = TEST_OLD_PASSPHRASE,
                    errorStatus = TextFieldStateContract.ErrorStatus.SUCCESS
                )
            ),
            newPasswordState = TextFieldState(),
            newPasswordVerificationState = TextFieldState(),
        ),
        PassphraseMgmtState.ManagingAccounts( //5
            accountsWithNoPassphrase = emptyList(),
            oldPasswordStates = listOf(
                AccountTextFieldState(
                    EMAIL,
                    text = TEST_OLD_PASSPHRASE,
                    errorStatus = TextFieldStateContract.ErrorStatus.SUCCESS
                )
            ),
            newPasswordState = TextFieldState(),
            newPasswordVerificationState = TextFieldState(),
            status = PassphraseVerificationStatus.SUCCESS_EMPTY
        ),
    )

    private data class TestException(override val message: String = "test") : Throwable()
}
