package security.planck.ui.passphrase.unlock

import androidx.lifecycle.LiveData
import com.fsck.k9.Account
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.testutils.CoroutineTestRule
import foundation.pEp.jniadapter.Pair
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import security.planck.common.LiveDataTest
import security.planck.passphrase.PassphraseFormatValidator
import security.planck.passphrase.PassphraseRepository
import security.planck.ui.passphrase.assertPairArrayList
import security.planck.ui.passphrase.models.AccountTextFieldState
import security.planck.ui.passphrase.models.PassphraseState
import security.planck.ui.passphrase.models.PassphraseUnlockState
import security.planck.ui.passphrase.models.PassphraseVerificationStatus
import security.planck.ui.passphrase.models.TextFieldStateContract
import java.util.Vector

private const val EMAIL = "test@mail.ch"
private const val TEST_PASSPHRASE = "HelloPassphrase!!1"

@OptIn(ExperimentalCoroutinesApi::class)
class PassphraseUnlockViewModelTest : LiveDataTest<PassphraseState>() {
    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()
    override val testLivedata: LiveData<PassphraseState>
        get() = viewModel.state

    private val account: Account = mockk {
        every { email }.returns(EMAIL)
    }
    private val planckProvider: PlanckProvider = mockk {
        coEvery { unlockKeysWithPassphrase(any()) }.returns(Result.success(Vector()))
    }
    private val passphraseRepository: PassphraseRepository = mockk {
        coEvery { getAccountsWithPassPhrase() }.returns(Result.success(listOf(account)))
        every { unlockPassphrase() }.just(runs)
    }
    private val passphraseValidator: PassphraseFormatValidator = PassphraseFormatValidator()
    private val viewModel =
        PassphraseUnlockViewModel(planckProvider, passphraseRepository, passphraseValidator)

    @Test
    fun `initial state is Processing`() {
        assertObservedValues(PassphraseState.Processing)
    }

    @Test
    fun `start() loads accounts with passphrase from PassphraseRepository`() = runTest {
        viewModel.start()
        advanceUntilIdle()


        coVerify { passphraseRepository.getAccountsWithPassPhrase() }
        assertObservedValues(
            PassphraseState.Processing,
            PassphraseUnlockState.UnlockingPassphrases(listOf(AccountTextFieldState(EMAIL)))
        )
    }

    @Test
    fun `start() sets CoreError status if PlanckRepository call fails`() = runTest {
        coEvery { passphraseRepository.getAccountsWithPassPhrase() }.returns(
            Result.failure(
                TestException()
            )
        )


        viewModel.start()
        advanceUntilIdle()


        coVerify { passphraseRepository.getAccountsWithPassPhrase() }
        assertObservedValues(PassphraseState.Processing, PassphraseState.CoreError(TestException()))
    }

    @Test
    fun `unlockKeysWithPassphrase() uses PlanckProvider and PassphraseRepository`() = runTest {
        viewModel.start()
        advanceUntilIdle()

        viewModel.updateAndValidateText(0, TEST_PASSPHRASE)
        viewModel.unlockKeysWithPassphrase()
        advanceUntilIdle()


        val arrayListSlot = slot<ArrayList<Pair<String, String>>>()
        coVerify { planckProvider.unlockKeysWithPassphrase(capture(arrayListSlot)) }

        assertPairArrayList(
            listOf(Pair(EMAIL, TEST_PASSPHRASE)),
            arrayListSlot.captured
        )
        verify { passphraseRepository.unlockPassphrase() }

        assertObservedValues(
            // initial state
            PassphraseState.Processing,
            // initialized
            PassphraseUnlockState.UnlockingPassphrases(listOf(AccountTextFieldState(EMAIL))),
            // text updated
            PassphraseUnlockState.UnlockingPassphrases(
                listOf(
                    AccountTextFieldState(
                        EMAIL, text = TEST_PASSPHRASE,
                        errorStatus = TextFieldStateContract.ErrorStatus.SUCCESS
                    )
                ),
            ),
            // overall status updated according to text status
            PassphraseUnlockState.UnlockingPassphrases(
                listOf(
                    AccountTextFieldState(
                        EMAIL, text = TEST_PASSPHRASE,
                        errorStatus = TextFieldStateContract.ErrorStatus.SUCCESS
                    )
                ),
                status = PassphraseVerificationStatus.SUCCESS
            ),
            // core operation
            PassphraseState.Processing,
            // success
            PassphraseState.Success,
        )
    }

    @Test
    fun `unlockKeysWithPassphrase() sets state to CoreError if PlanckProvider call fails`() =
        runTest {
            coEvery { planckProvider.unlockKeysWithPassphrase(any()) }.returns(
                Result.failure(
                    TestException()
                )
            )
            viewModel.start()
            advanceUntilIdle()

            viewModel.updateAndValidateText(0, TEST_PASSPHRASE)
            viewModel.unlockKeysWithPassphrase()
            advanceUntilIdle()


            coVerify { planckProvider.unlockKeysWithPassphrase(any()) }
            verify(exactly = 0) { passphraseRepository.unlockPassphrase() }
            assertObservedValues(
                // initial state
                PassphraseState.Processing,
                // initialized
                PassphraseUnlockState.UnlockingPassphrases(listOf(AccountTextFieldState(EMAIL))),
                // text updated
                PassphraseUnlockState.UnlockingPassphrases(
                    listOf(
                        AccountTextFieldState(
                            EMAIL, text = TEST_PASSPHRASE,
                            errorStatus = TextFieldStateContract.ErrorStatus.SUCCESS
                        )
                    ),
                ),
                // overall status updated according to text status
                PassphraseUnlockState.UnlockingPassphrases(
                    listOf(
                        AccountTextFieldState(
                            EMAIL, text = TEST_PASSPHRASE,
                            errorStatus = TextFieldStateContract.ErrorStatus.SUCCESS
                        )
                    ),
                    status = PassphraseVerificationStatus.SUCCESS
                ),
                // core operation
                PassphraseState.Processing,
                // core error
                PassphraseUnlockState.UnlockingPassphrases(
                    listOf(
                        AccountTextFieldState(
                            EMAIL, text = TEST_PASSPHRASE,
                            errorStatus = TextFieldStateContract.ErrorStatus.SUCCESS
                        )
                    ),
                    status = PassphraseVerificationStatus.CORE_ERROR
                ),
            )
        }

    @Test
    fun `unlockKeysWithPassphrase() offers retry to user`() = runTest {
        coEvery { planckProvider.unlockKeysWithPassphrase(any()) }.returns(
            Result.success(
                Vector(
                    listOf(EMAIL)
                )
            )
        )
        viewModel.start()
        advanceUntilIdle()

        viewModel.updateAndValidateText(0, TEST_PASSPHRASE)
        viewModel.unlockKeysWithPassphrase()
        advanceUntilIdle()


        coVerify { planckProvider.unlockKeysWithPassphrase(any()) }
        verify(exactly = 0) { passphraseRepository.unlockPassphrase() }
        assertObservedValues(
            // initial state
            PassphraseState.Processing,
            // initialized
            PassphraseUnlockState.UnlockingPassphrases(listOf(AccountTextFieldState(EMAIL))),
            // text updated
            PassphraseUnlockState.UnlockingPassphrases(
                listOf(
                    AccountTextFieldState(
                        EMAIL, text = TEST_PASSPHRASE,
                        errorStatus = TextFieldStateContract.ErrorStatus.SUCCESS
                    )
                ),
            ),
            // overall status updated according to text status
            PassphraseUnlockState.UnlockingPassphrases(
                listOf(
                    AccountTextFieldState(
                        EMAIL, text = TEST_PASSPHRASE,
                        errorStatus = TextFieldStateContract.ErrorStatus.SUCCESS
                    )
                ),
                status = PassphraseVerificationStatus.SUCCESS
            ),
            // core operation
            PassphraseState.Processing,
            // wrong passphrase
            PassphraseUnlockState.UnlockingPassphrases(
                listOf(
                    AccountTextFieldState(
                        EMAIL, text = TEST_PASSPHRASE,
                        errorStatus = TextFieldStateContract.ErrorStatus.ERROR
                    )
                ),
                status = PassphraseVerificationStatus.WRONG_PASSPHRASE
            ),
        )
    }

    @Test
    fun `updateAndValidateText() clears errors`() = runTest {
        viewModel.start()
        advanceUntilIdle()

        assertObservedValues(
            PassphraseState.Processing,
            PassphraseUnlockState.UnlockingPassphrases(listOf(AccountTextFieldState(EMAIL)))
        )

        viewModel.updateAndValidateText(0, "")

        assertObservedValues(
            // initial state
            PassphraseState.Processing,
            // initialized
            PassphraseUnlockState.UnlockingPassphrases(listOf(AccountTextFieldState(EMAIL))),
            // text updated
            PassphraseUnlockState.UnlockingPassphrases(
                listOf(
                    AccountTextFieldState(
                        EMAIL,
                        errorStatus = TextFieldStateContract.ErrorStatus.NONE
                    )
                ),
            ),
            // overall status not updated according to text status since it did not change
        )


        viewModel.updateAndValidateText(0, "wrong")


        assertObservedValues(
            // initial state
            PassphraseState.Processing,
            // initialized
            PassphraseUnlockState.UnlockingPassphrases(listOf(AccountTextFieldState(EMAIL))),
            PassphraseUnlockState.UnlockingPassphrases(listOf(AccountTextFieldState(EMAIL))),
            // text updated
            PassphraseUnlockState.UnlockingPassphrases(
                listOf(
                    AccountTextFieldState(
                        EMAIL, text = "wrong",
                        errorStatus = TextFieldStateContract.ErrorStatus.ERROR
                    )
                ),
            ),
            // overall status updated according to text status
            PassphraseUnlockState.UnlockingPassphrases(
                listOf(
                    AccountTextFieldState(
                        EMAIL, text = "wrong",
                        errorStatus = TextFieldStateContract.ErrorStatus.ERROR
                    )
                ),
                status = PassphraseVerificationStatus.WRONG_FORMAT
            ),
        )
        //[security.planck.ui.passphrase.models.PassphraseState$Processing@5da57a64,
        //UnlockingPassphrases(passwordStates=[AccountTextFieldState(email=test@mail.ch, text=, errorStatus=NONE)], status=NONE),
        //UnlockingPassphrases(passwordStates=[AccountTextFieldState(email=test@mail.ch, text=, errorStatus=NONE)], status=NONE),
        //UnlockingPassphrases(passwordStates=[AccountTextFieldState(email=test@mail.ch, text=wrong, errorStatus=ERROR)], status=NONE),
        //UnlockingPassphrases(passwordStates=[AccountTextFieldState(email=test@mail.ch, text=wrong, errorStatus=ERROR)], status=WRONG_FORMAT)]

        viewModel.updateAndValidateText(0, TEST_PASSPHRASE)

        assertObservedValues(
            // initial state
            PassphraseState.Processing,
            // initialized
            PassphraseUnlockState.UnlockingPassphrases(listOf(AccountTextFieldState(EMAIL))),
            PassphraseUnlockState.UnlockingPassphrases(listOf(AccountTextFieldState(EMAIL))),
            // text updated
            PassphraseUnlockState.UnlockingPassphrases(
                listOf(
                    AccountTextFieldState(
                        EMAIL, text = "wrong",
                        errorStatus = TextFieldStateContract.ErrorStatus.ERROR
                    )
                ),
            ),
            // overall status updated according to text status
            PassphraseUnlockState.UnlockingPassphrases(
                listOf(
                    AccountTextFieldState(
                        EMAIL, text = "wrong",
                        errorStatus = TextFieldStateContract.ErrorStatus.ERROR
                    )
                ),
                status = PassphraseVerificationStatus.WRONG_FORMAT
            ),

            // text updated
            PassphraseUnlockState.UnlockingPassphrases(
                listOf(
                    AccountTextFieldState(
                        EMAIL, text = TEST_PASSPHRASE,
                        errorStatus = TextFieldStateContract.ErrorStatus.SUCCESS
                    ),
                ),
                status = PassphraseVerificationStatus.WRONG_FORMAT
            ),
            // overall status updated according to text status
            PassphraseUnlockState.UnlockingPassphrases(
                listOf(
                    AccountTextFieldState(
                        EMAIL, text = TEST_PASSPHRASE,
                        errorStatus = TextFieldStateContract.ErrorStatus.SUCCESS
                    )
                ),
                status = PassphraseVerificationStatus.SUCCESS
            ),
        )
    }

    private data class TestException(override val message: String = "test") : Throwable()
}