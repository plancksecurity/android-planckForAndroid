package com.fsck.k9.planck.manualsync

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fsck.k9.K9
import com.fsck.k9.RobolectricTest
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.PlanckUtils
import com.fsck.k9.planck.infrastructure.ResultCompat
import com.fsck.k9.planck.testutils.CoroutineTestRule
import foundation.pEp.jniadapter.Identity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

private const val TEST_TRUSTWORDS = "TEST TRUSTWORDS"
private const val GERMAN_TRUSTWORDS = "GERMAN TRUSTWORDS"
private const val LONG_TRUSTWORDS = "LONG TRUSTWORDS"
private const val MYSELF_FPR = "MY_FPR"
private const val PARTNER_FPR = "PARTNER_FPR"
private const val ENGLISH_LANGUAGE = "en"
private const val GERMAN_LANGUAGE = "de"
private const val GERMAN_POSITION = 1

@ExperimentalCoroutinesApi
class PlanckSyncWizardViewModelTest : RobolectricTest() {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()

    private val k9: K9 = mockk(relaxed = true)
    private val planckProvider: PlanckProvider = mockk(relaxed = true)
    private lateinit var viewModel: PlanckSyncWizardViewModel
    private val receivedSyncStates = mutableListOf<SyncScreenState>()
    private val myself: Identity = Identity().apply { fpr = MYSELF_FPR }
    private val partner: Identity = Identity().apply { fpr = PARTNER_FPR }

    @Before
    fun setUp() {
        mockkStatic(K9::class)
        mockkStatic(PlanckUtils::class)
        every { K9.getK9CurrentLanguage() }.returns(ENGLISH_LANGUAGE)
        val slot = slot<String>()
        every { PlanckUtils.formatFpr(capture(slot)) }.answers { slot.captured }
        every { PlanckUtils.trustWordsAvailableForLang(any()) }.returns(true)
        every { k9.syncState }.returns(SyncState.Idle)
        coEvery {
            planckProvider.trustwords(
                myself,
                partner,
                any(),
                any()
            )
        }.returns(ResultCompat.success(TEST_TRUSTWORDS))

        receivedSyncStates.clear()
        viewModel = PlanckSyncWizardViewModel(
            k9,
            planckProvider,
            coroutinesTestRule.testDispatcherProvider
        )
        observeViewModel()
        assertionsOnViewModelCreation()
    }

    @After
    fun tearDown() {
        unmockkStatic(K9::class)
        unmockkStatic(PlanckUtils::class)
    }

    @Test
    fun `syncStateChanged() sets syncState`() {
        viewModel.syncStateChanged(SyncState.Done)


        assertEquals(
            listOf(
                SyncState.AwaitingOtherDevice,
                SyncState.Done,
            ),
            receivedSyncStates
        )
    }

    @Test
    fun `if screen finishes and sync was done, sync is not cancelled`() {
        viewModel.syncStateChanged(SyncState.Done)
        viewModel.cancelIfNotDone()


        assertEquals(
            listOf(
                SyncState.AwaitingOtherDevice,
                SyncState.Done,
            ),
            receivedSyncStates
        )
        verify(exactly = 0) { planckProvider.cancelSync() }
        verify(exactly = 0) { k9.cancelSync() }
    }

    @Test
    fun `if screen finishes and sync was not done, sync is cancelled`() {
        viewModel.cancelIfNotDone()


        assertEquals(
            listOf(
                SyncState.AwaitingOtherDevice,
            ),
            receivedSyncStates
        )
        verify { planckProvider.cancelSync() }
        verify { k9.cancelSync() }
    }

    @Test
    fun `initial call to syncStateChanged() sets state and formingGroup in ViewModel`() {
        viewModel.syncStateChanged(
            SyncState.HandshakeReadyAwaitingUser, myself, partner, true
        )


        assertEquals(
            listOf(
                SyncState.AwaitingOtherDevice,
                SyncState.HandshakeReadyAwaitingUser
            ),
            receivedSyncStates
        )
        assertEquals(true, viewModel.formingGroup)
    }

    @Test
    fun `next() sets state to UserHandshaking with identities received from syncStateChanged() and trustwords from PlanckProvider`() =
        runTest {
            viewModel.syncStateChanged(
                SyncState.HandshakeReadyAwaitingUser, myself, partner, true
            )
            viewModel.next()
            advanceUntilIdle()


            coVerify { planckProvider.trustwords(myself, partner, ENGLISH_LANGUAGE, true) }
            assertEquals(
                listOf(
                    SyncState.AwaitingOtherDevice,
                    SyncState.HandshakeReadyAwaitingUser,
                    SyncState.UserHandshaking(MYSELF_FPR, PARTNER_FPR, TEST_TRUSTWORDS)
                ),
                receivedSyncStates
            )
        }

    @Test
    fun `acceptHandshake() calls PlanckProvider acceptSync and sets state AwaitingHandshakeCompletion`() =
        runTest {
            viewModel.syncStateChanged(
                SyncState.HandshakeReadyAwaitingUser, myself, partner, true
            )
            viewModel.next()
            advanceUntilIdle()
            viewModel.acceptHandshake()


            coVerify { planckProvider.acceptSync() }
            assertEquals(
                listOf(
                    SyncState.AwaitingOtherDevice,
                    SyncState.HandshakeReadyAwaitingUser,
                    SyncState.UserHandshaking(MYSELF_FPR, PARTNER_FPR, TEST_TRUSTWORDS),
                    SyncState.AwaitingHandshakeCompletion(MYSELF_FPR, PARTNER_FPR)
                ),
                receivedSyncStates
            )
            coVerify { k9.syncState = SyncState.PerformingHandshake }
        }

    @Test
    fun `cancelHandshake() uses PlanckProvider and K9 to cancel handshake`() {
        viewModel.cancelHandshake()


        verify { planckProvider.cancelSync() }
        verify { k9.cancelSync() }
    }

    @Test
    fun `rejectHandshake() uses PlanckProvider to reject handshake and K9 to cancel sync`() {
        viewModel.rejectHandshake()


        verify { planckProvider.rejectSync() }
        verify { k9.cancelSync() }
    }

    @Test
    fun `changeTrustwordsLanguage() resets syncState with new trustwords`() = runTest {
        every { PlanckUtils.getPlanckLocales() }.returns(listOf(ENGLISH_LANGUAGE, GERMAN_LANGUAGE))
        coEvery { planckProvider.trustwords(myself, partner, ENGLISH_LANGUAGE, any()) }
            .returns(ResultCompat.success(TEST_TRUSTWORDS))
        coEvery { planckProvider.trustwords(myself, partner, GERMAN_LANGUAGE, any()) }
            .returns(ResultCompat.success(GERMAN_TRUSTWORDS))


        viewModel.syncStateChanged(
            SyncState.HandshakeReadyAwaitingUser, myself, partner, true
        )
        viewModel.next()
        advanceUntilIdle()
        viewModel.changeTrustwordsLanguage(GERMAN_POSITION)
        advanceUntilIdle()


        verify { PlanckUtils.getPlanckLocales() }
        coVerify { planckProvider.trustwords(myself, partner, ENGLISH_LANGUAGE, true) }
        coVerify { planckProvider.trustwords(myself, partner, GERMAN_LANGUAGE, true) }
        assertEquals(
            listOf(
                SyncState.AwaitingOtherDevice,
                SyncState.HandshakeReadyAwaitingUser,
                SyncState.UserHandshaking(MYSELF_FPR, PARTNER_FPR, TEST_TRUSTWORDS),
                SyncState.UserHandshaking(MYSELF_FPR, PARTNER_FPR, GERMAN_TRUSTWORDS)
            ),
            receivedSyncStates
        )
    }

    @Test
    fun `switchTrustwordsLength() resets syncState with new trustwords`() = runTest {
        coEvery { planckProvider.trustwords(myself, partner, any(), true) }
            .returns(ResultCompat.success(TEST_TRUSTWORDS))
        coEvery { planckProvider.trustwords(myself, partner, any(), false) }
            .returns(ResultCompat.success(LONG_TRUSTWORDS))


        viewModel.syncStateChanged(
            SyncState.HandshakeReadyAwaitingUser, myself, partner, true
        )
        viewModel.next()
        advanceUntilIdle()
        viewModel.switchTrustwordsLength()
        advanceUntilIdle()


        coVerify { planckProvider.trustwords(myself, partner, any(), true) }
        coVerify { planckProvider.trustwords(myself, partner, any(), false) }
        assertEquals(
            listOf(
                SyncState.AwaitingOtherDevice,
                SyncState.HandshakeReadyAwaitingUser,
                SyncState.UserHandshaking(MYSELF_FPR, PARTNER_FPR, TEST_TRUSTWORDS),
                SyncState.UserHandshaking(MYSELF_FPR, PARTNER_FPR, LONG_TRUSTWORDS)
            ),
            receivedSyncStates
        )
        assertFalse(viewModel.shortTrustWords)
    }

    @Test
    fun `if PlanckProvider_trustwords returns Failure, syncState is set to Error`() = runTest {
        val expectedError = RuntimeException("Test")
        every { planckProvider.trustwords(any(), any(), any(), any()) }
            .returns(ResultCompat.failure(expectedError))


        viewModel.syncStateChanged(
            SyncState.HandshakeReadyAwaitingUser, myself, partner, false
        )
        viewModel.next()
        advanceUntilIdle()


        coVerify { planckProvider.trustwords(myself, partner, ENGLISH_LANGUAGE, true) }
        assertEquals(
            listOf(
                SyncState.AwaitingOtherDevice,
                SyncState.HandshakeReadyAwaitingUser,
                SyncState.Error(expectedError)
            ),
            receivedSyncStates
        )
    }

    @Test
    fun `when screen finishes, K9 has syncState set to Idle and SyncStateChangeListener set to null`() {
        val method = viewModel.javaClass.getDeclaredMethod("onCleared")
        method.isAccessible = true
        method.invoke(viewModel)


        verify { k9.syncState = SyncState.Idle }
        verify { k9.setSyncStateChangeListener(null) }
    }

    private fun assertionsOnViewModelCreation() {
        verify { k9.syncState = SyncState.AwaitingOtherDevice }
        verify { k9.setSyncStateChangeListener(viewModel) }
        assertStates(listOf(SyncState.AwaitingOtherDevice))
    }

    private fun assertStates(states: List<SyncScreenState>) {
        assertEquals(states, receivedSyncStates)
    }

    private fun observeViewModel() {
        viewModel.getSyncState().observeForever { value ->
            receivedSyncStates.add(value)
        }
    }
}