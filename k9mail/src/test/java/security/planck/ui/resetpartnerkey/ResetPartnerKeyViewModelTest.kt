package security.planck.ui.resetpartnerkey

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fsck.k9.mail.Address
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.PlanckUtils
import com.fsck.k9.planck.infrastructure.ResultCompat
import com.fsck.k9.planck.testutils.CoroutineTestRule
import foundation.pEp.jniadapter.Identity
import foundation.pEp.jniadapter.Rating
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import junit.framework.TestCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import security.planck.dialog.BackgroundTaskDialogView.State

private const val PARTNER_MAIL = "test1@test.ch"

@ExperimentalCoroutinesApi
class ResetPartnerKeyViewModelTest {
    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val planckProvider: PlanckProvider = mockk {
        coEvery { getRating(any<Address>()) }.returns(ResultCompat.success(Rating.pEpRatingReliable))
        every { keyResetIdentity(any(), any()) }.just(runs)
    }
    private val app: Application = mockk()
    private val partnerAddress: Address = mockk {
        every { address }.returns(PARTNER_MAIL)
        every { personal }.returns(null)
    }
    private val partnerIdentity: Identity = mockk()

    private val viewModel = ResetPartnerKeyViewModel(
        app,
        planckProvider,
        coroutinesTestRule.testDispatcherProvider
    )

    private val receivedResetStates = mutableListOf<State>()

    @Before
    fun setUp() {
        mockkStatic(PlanckUtils::class)
        every { PlanckUtils.createIdentity(any(), any()) }.returns(partnerIdentity)
        mockkStatic(Address::class)
        every { Address.create(any()) }.returns(partnerAddress)
        receivedResetStates.clear()
        observeViewModel()
    }

    @After
    fun tearDown() {
        unmockkStatic(Address::class)
        unmockkStatic(PlanckUtils::class)
    }

    @Test
    fun `initial value for resetPartnerKeyState is Confirmation`() {
        assertResetStates(State.CONFIRMATION)
    }

    @Test
    fun `initialize() creates the partner identity`() = runTest {
        viewModel.initialize(PARTNER_MAIL)
        advanceUntilIdle()


        verify { Address.create(PARTNER_MAIL) }
        verify { PlanckUtils.createIdentity(partnerAddress, app) }
    }

    @Test
    fun `initialize() sets state to error if provided address is null`() = runTest {
        viewModel.initialize(null)
        advanceUntilIdle()


        verify(exactly = 0) { Address.create(any()) }
        verify(exactly = 0) { PlanckUtils.createIdentity(any(), any()) }
        assertResetStates(State.CONFIRMATION, State.ERROR)
    }

    @Test
    fun `initialize() sets state to error if PlanckUtils_createIdentity() fails`() = runTest {
        every { PlanckUtils.createIdentity(any(), any()) }.throws(RuntimeException("test"))


        viewModel.initialize(PARTNER_MAIL)
        advanceUntilIdle()


        verify { Address.create(any()) }
        verify { PlanckUtils.createIdentity(any(), any()) }
        assertResetStates(State.CONFIRMATION, State.ERROR)
    }

    @Test
    fun `resetPlanckData sets resetPartnerKeyState to Loading`() = runTest {
        viewModel.initialize(PARTNER_MAIL)
        advanceUntilIdle()
        viewModel.resetPlanckData()
        advanceUntilIdle()


        assertResetStates(
            State.CONFIRMATION,
            State.LOADING,
            full = false
        )
    }

    @Test
    fun `resetPlanckData uses PlanckProvider to reset sender identity`() = runTest {
        viewModel.initialize(PARTNER_MAIL)
        advanceUntilIdle()
        viewModel.resetPlanckData()
        advanceUntilIdle()


        verify { PlanckUtils.createIdentity(partnerAddress, any()) }
        verify { planckProvider.keyResetIdentity(partnerIdentity, null) }
    }

    @Test
    fun `resetPlanckData sets resetPartnerKeyState to Success if operation was successful`() =
        runTest {
            viewModel.initialize(PARTNER_MAIL)
            advanceUntilIdle()
            viewModel.resetPlanckData()
            advanceUntilIdle()


            assertResetStates(
                State.CONFIRMATION,
                State.LOADING,
                State.SUCCESS
            )
        }

    @Test
    fun `resetPlanckData sets resetPartnerKeyState to Error if operation was not successful`() =
        runTest {
            coEvery {
                planckProvider.keyResetIdentity(
                    any(),
                    null
                )
            }.throws(RuntimeException("test"))


            viewModel.initialize(PARTNER_MAIL)
            advanceUntilIdle()
            viewModel.resetPlanckData()
            advanceUntilIdle()


            assertResetStates(
                State.CONFIRMATION,
                State.LOADING,
                State.ERROR
            )
        }

    private fun assertResetStates(
        vararg states: State,
        full: Boolean = true
    ) {
        if (full) TestCase.assertEquals(states.size, receivedResetStates.size)
        states.forEachIndexed { index, resetState ->
            TestCase.assertEquals(resetState, receivedResetStates[index])
        }
    }

    private fun observeViewModel() {
        viewModel.resetPartnerKeyState.observeForever { value ->
            println("received: $value")
            receivedResetStates.add(value)
        }
    }
}