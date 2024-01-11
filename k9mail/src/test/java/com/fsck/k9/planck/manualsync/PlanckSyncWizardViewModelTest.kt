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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import security.planck.sync.SyncRepository

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

    private val planckProvider: PlanckProvider = mockk(relaxed = true)
    private val syncRepository: SyncRepository = mockk(relaxed = true)
    private lateinit var viewModel: PlanckSyncWizardViewModel
    private val receivedSyncStates = mutableListOf<SyncScreenState>()
    private val myself: Identity = Identity().apply { fpr = MYSELF_FPR }
    private val partner: Identity = Identity().apply { fpr = PARTNER_FPR }
    private val syncStateFlow: MutableStateFlow<SyncAppState> = MutableStateFlow(SyncState.Idle)

    @Before
    fun setUp() = runTest {
        mockkStatic(K9::class)
        mockkStatic(PlanckUtils::class)
        every { K9.getK9CurrentLanguage() }.returns(ENGLISH_LANGUAGE)
        val slot = slot<String>()
        every { PlanckUtils.formatFpr(capture(slot)) }.answers { slot.captured }
        every { PlanckUtils.trustWordsAvailableForLang(any()) }.returns(true)
        every { syncRepository.syncStateFlow }.returns(syncStateFlow)
        val stateSlot = slot<SyncAppState>()
        coEvery { syncRepository.setCurrentState(capture(stateSlot)) }.coAnswers {
            syncStateFlow.value = stateSlot.captured
        }
        coEvery { planckProvider.isSyncRunning() }.returns(true)
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
            syncRepository,
            planckProvider,
            coroutinesTestRule.testDispatcherProvider
        )
        advanceUntilIdle()
        observeViewModel()
        assertionsOnViewModelCreation()
    }

    @After
    fun tearDown() {
        unmockkStatic(K9::class)
        unmockkStatic(PlanckUtils::class)
    }

    @Test
    fun `setting a value in incoming StateFlow sets syncState`() = runTest {
        syncStateFlow.value = SyncState.Done
        advanceUntilIdle()


        assertStates(
            SyncState.Idle,
            SyncState.Done,
        )
    }

    @Test
    fun `if screen finishes and sync was done, sync is not cancelled`() = runTest {
        syncStateFlow.value = SyncState.Done
        advanceUntilIdle()
        viewModel.cancelIfNotDone()


        assertStates(
            SyncState.Idle,
            SyncState.Done,
        )
        coVerify(exactly = 0) { planckProvider.cancelSync() }
        verify(exactly = 0) { syncRepository.cancelSync() }
    }

    @Test
    fun `if screen finishes and sync was not done, sync is cancelled`() = runTest {
        viewModel.cancelIfNotDone()
        advanceUntilIdle()


        assertStates(SyncState.Idle)
        coVerify { planckProvider.cancelSync() }
        verify { syncRepository.cancelSync() }
    }


    @Test
    fun `repository flow with value of HandshakeReadyAwaitingUser sets state and formingGroup in ViewModel`() =
        runTest {
            syncStateFlow.value = SyncState.HandshakeReadyAwaitingUser(myself, partner, true)
            advanceUntilIdle()


            assertStates(
                SyncState.Idle,
                SyncState.HandshakeReadyAwaitingUser(myself, partner, true)
            )
            assertEquals(true, viewModel.formingGroup)
        }

    @Test
    fun `next() sets state to UserHandshaking with identities received from SyncRepository's flow and trustwords from PlanckProvider`() =
        runTest {
            syncStateFlow.value = SyncState.HandshakeReadyAwaitingUser(myself, partner, true)
            advanceUntilIdle()
            viewModel.next()
            advanceUntilIdle()


            coVerify { planckProvider.trustwords(myself, partner, ENGLISH_LANGUAGE, true) }
            assertStates(
                SyncState.Idle,
                SyncState.HandshakeReadyAwaitingUser(myself, partner, true),
                SyncState.UserHandshaking(MYSELF_FPR, PARTNER_FPR, TEST_TRUSTWORDS)
            )
        }

    @Test
    fun `acceptHandshake() calls PlanckProvider acceptSync and sets state AwaitingHandshakeCompletion`() =
        runTest {
            syncStateFlow.value = SyncState.HandshakeReadyAwaitingUser(myself, partner, true)
            advanceUntilIdle()
            viewModel.next()
            advanceUntilIdle()
            viewModel.acceptHandshake()
            advanceUntilIdle()


            coVerify { planckProvider.acceptSync() }
            assertStates(
                SyncState.Idle,
                SyncState.HandshakeReadyAwaitingUser(myself, partner, true),
                SyncState.UserHandshaking(MYSELF_FPR, PARTNER_FPR, TEST_TRUSTWORDS),
                SyncState.AwaitingHandshakeCompletion(MYSELF_FPR, PARTNER_FPR)
            )
            coVerify { syncRepository.setCurrentState(SyncState.PerformingHandshake) }
        }

    @Test
    fun `cancelHandshake() uses PlanckProvider and SyncRepository to cancel handshake`() = runTest {
        viewModel.cancelHandshake()
        advanceUntilIdle()


        coVerify { planckProvider.cancelSync() }
        verify { syncRepository.cancelSync() }
    }

    @Test
    fun `rejectHandshake() uses PlanckProvider to reject handshake and K9 to cancel sync`() = runTest {
        viewModel.rejectHandshake()
        advanceUntilIdle()


        coVerify { planckProvider.rejectSync() }
        verify { syncRepository.cancelSync() }
    }

    @Test
    fun `changeTrustwordsLanguage() resets syncState with new trustwords`() = runTest {
        every { PlanckUtils.getPlanckLocales() }.returns(listOf(ENGLISH_LANGUAGE, GERMAN_LANGUAGE))
        coEvery { planckProvider.trustwords(myself, partner, ENGLISH_LANGUAGE, any()) }
            .returns(ResultCompat.success(TEST_TRUSTWORDS))
        coEvery { planckProvider.trustwords(myself, partner, GERMAN_LANGUAGE, any()) }
            .returns(ResultCompat.success(GERMAN_TRUSTWORDS))


        syncStateFlow.value = SyncState.HandshakeReadyAwaitingUser(myself, partner, true)
        advanceUntilIdle()
        viewModel.next()
        advanceUntilIdle()
        viewModel.changeTrustwordsLanguage(GERMAN_POSITION)
        advanceUntilIdle()


        verify { PlanckUtils.getPlanckLocales() }
        coVerify { planckProvider.trustwords(myself, partner, ENGLISH_LANGUAGE, true) }
        coVerify { planckProvider.trustwords(myself, partner, GERMAN_LANGUAGE, true) }
        assertStates(
            SyncState.Idle,
            SyncState.HandshakeReadyAwaitingUser(myself, partner, true),
            SyncState.UserHandshaking(MYSELF_FPR, PARTNER_FPR, TEST_TRUSTWORDS),
            SyncState.UserHandshaking(MYSELF_FPR, PARTNER_FPR, GERMAN_TRUSTWORDS)
        )
    }

    @Test
    fun `switchTrustwordsLength() resets syncState with new trustwords`() = runTest {
        coEvery { planckProvider.trustwords(myself, partner, any(), true) }
            .returns(ResultCompat.success(TEST_TRUSTWORDS))
        coEvery { planckProvider.trustwords(myself, partner, any(), false) }
            .returns(ResultCompat.success(LONG_TRUSTWORDS))


        syncStateFlow.value = SyncState.HandshakeReadyAwaitingUser(myself, partner, true)
        advanceUntilIdle()
        viewModel.next()
        advanceUntilIdle()
        viewModel.switchTrustwordsLength()
        advanceUntilIdle()


        coVerify { planckProvider.trustwords(myself, partner, any(), true) }
        coVerify { planckProvider.trustwords(myself, partner, any(), false) }
        assertEquals(
            listOf(
                SyncState.Idle,
                SyncState.HandshakeReadyAwaitingUser(myself, partner, true),
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


        syncStateFlow.value = SyncState.HandshakeReadyAwaitingUser(myself, partner, false)
        advanceUntilIdle()
        viewModel.next()
        advanceUntilIdle()


        coVerify { planckProvider.trustwords(myself, partner, ENGLISH_LANGUAGE, true) }
        assertStates(
            SyncState.Idle,
            SyncState.HandshakeReadyAwaitingUser(myself, partner, false),
            SyncState.Error(expectedError)
        )
    }

    @Test
    fun `when screen finishes, SyncRepository_userDisconnected() is called`() {
        val method = viewModel.javaClass.getDeclaredMethod("onCleared")
        method.isAccessible = true
        method.invoke(viewModel)


        verify { syncRepository.userDisconnected() }
    }

    private fun assertionsOnViewModelCreation() {
        coVerify { syncRepository.userConnected() }
    }

    private fun assertStates(vararg states: SyncScreenState) {
        assertEquals(states.toList(), receivedSyncStates)
    }

    private fun observeViewModel() {
        viewModel.getSyncState().observeForever { value ->
            receivedSyncStates.add(value)
        }
    }
}