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
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.jsoup.helper.Validate.fail
import org.junit.Rule
import org.junit.Test
import security.planck.common.LiveDataTest
import security.planck.passphrase.PassphraseFormatValidator
import security.planck.passphrase.PassphraseRepository
import security.planck.ui.passphrase.assertPairArrayList
import security.planck.ui.passphrase.models.AccountTextFieldState
import security.planck.ui.passphrase.models.PassphraseLoading
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
    fun `initial state is Loading`() {
        assertObservedValues(PassphraseState.Loading)
    }

    @Test
    fun `start() loads accounts with passphrase from PassphraseRepository`() = runTest {
        viewModel.start()
        advanceUntilIdle()


        coVerify { passphraseRepository.getAccountsWithPassPhrase() }
        customAssertObservedValues(
            { assertEquals(PassphraseState.Loading, it) },
            { assertUnlockingPassphrasesState(it) },
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
        assertObservedValues(PassphraseState.Loading, PassphraseState.CoreError(TestException()))
    }

    @Test
    fun `unlockKeysWithPassphrase() uses PlanckProvider and PassphraseRepository`() = runTest {
        viewModel.start()
        advanceUntilIdle()


        viewModel.unlockKeysWithPassphrase(
            listOf(
                AccountTextFieldState(
                    EMAIL,
                    text = TEST_PASSPHRASE
                )
            )
        )
        advanceUntilIdle()


        val arrayListSlot = slot<ArrayList<Pair<String, String>>>()
        coVerify { planckProvider.unlockKeysWithPassphrase(capture(arrayListSlot)) }

        assertPairArrayList(
            listOf(Pair(EMAIL, TEST_PASSPHRASE)),
            arrayListSlot.captured
        )
        verify { passphraseRepository.unlockPassphrase() }

        customAssertObservedValues(
            { assertEquals(PassphraseState.Loading, it) },
            { assertUnlockingPassphrasesState(it, expectedLoading = PassphraseLoading.Processing) },
            { assertEquals(PassphraseState.Success, it) },
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


            viewModel.unlockKeysWithPassphrase(
                listOf(
                    AccountTextFieldState(
                        EMAIL,
                        text = TEST_PASSPHRASE
                    )
                )
            )
            advanceUntilIdle()


            coVerify { planckProvider.unlockKeysWithPassphrase(any()) }
            verify(exactly = 0) { passphraseRepository.unlockPassphrase() }
            customAssertObservedValues(
                { assertEquals(PassphraseState.Loading, it) },
                {
                    assertUnlockingPassphrasesState(
                        it,
                        expectedStatus = PassphraseVerificationStatus.CORE_ERROR
                    )
                },
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


        viewModel.unlockKeysWithPassphrase(
            listOf(
                AccountTextFieldState(
                    EMAIL,
                    text = TEST_PASSPHRASE
                )
            )
        )
        advanceUntilIdle()


        coVerify { planckProvider.unlockKeysWithPassphrase(any()) }
        verify(exactly = 0) { passphraseRepository.unlockPassphrase() }
        customAssertObservedValues(
            { assertEquals(PassphraseState.Loading, it) },
            {
                assertUnlockingPassphrasesState(
                    it,
                    expectedStatus = PassphraseVerificationStatus.WRONG_PASSPHRASE
                )
            },
        )
    }

    @Test
    fun `validateInput() clears errors`() = runTest {
        viewModel.start()
        advanceUntilIdle()
        val passwordState =
            (viewModel.state.value as PassphraseUnlockState.UnlockingPassphrases).passwordStates[0]

        customAssertObservedValues(
            { assertEquals(PassphraseState.Loading, it) },
            { assertUnlockingPassphrasesState(it) },
        )

        passwordState.text = ""
        viewModel.updateAndValidateInput(passwordState)

        customAssertObservedValues(
            { assertEquals(PassphraseState.Loading, it) },
            { assertUnlockingPassphrasesState(it) }, // empty passphrase is not considered error
        )


        passwordState.text = "wrong"
        viewModel.updateAndValidateInput(passwordState)


        customAssertObservedValues(
            { assertEquals(PassphraseState.Loading, it) },
            {
                assertUnlockingPassphrasesState(
                    it,
                    expectedStatus = PassphraseVerificationStatus.WRONG_FORMAT
                )
            },
        )


        passwordState.text = TEST_PASSPHRASE
        viewModel.updateAndValidateInput(passwordState)

        customAssertObservedValues(
            { assertEquals(PassphraseState.Loading, it) },
            {
                assertUnlockingPassphrasesState(
                    it,
                    expectedStatus = PassphraseVerificationStatus.SUCCESS
                )
            },
        )
    }

    private fun assertUnlockingPassphrasesState(
        actual: PassphraseState,
        expectedStates: List<TextFieldStateContract> = listOf(AccountTextFieldState(EMAIL)),
        expectedLoading: PassphraseLoading? = null,
        expectedStatus: PassphraseVerificationStatus = PassphraseVerificationStatus.NONE,
    ) {
        if (actual is PassphraseUnlockState.UnlockingPassphrases) {
            assertEquals(expectedStates, actual.passwordStates.toList())
            assertEquals(expectedLoading, actual.loading.value)
            assertEquals(expectedStatus, actual.status.value)
        } else fail("Wrong type. Expected UnlockingPassphrases but got ${actual.javaClass.simpleName}")
    }

    private data class TestException(override val message: String = "test") : Throwable()
}