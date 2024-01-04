package security.planck.sync

import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.RobolectricTest
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.infrastructure.Poller
import com.fsck.k9.planck.infrastructure.PollerFactory
import com.fsck.k9.planck.infrastructure.ResultCompat
import com.fsck.k9.planck.manualsync.SyncAppState
import com.fsck.k9.planck.manualsync.SyncState
import com.fsck.k9.planck.testutils.CoroutineTestRule
import foundation.pEp.jniadapter.Identity
import foundation.pEp.jniadapter.SyncHandshakeSignal
import io.mockk.called
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import security.planck.notification.GroupMailSignal
import security.planck.timer.Timer
import javax.inject.Provider

private const val UUID = "uuid1"

@ExperimentalCoroutinesApi
class SyncRepositoryTest : RobolectricTest() {
    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()

    private val k9: K9 = mockk(relaxed = true)
    private val account: Account = mockk {
        every { uuid }.returns(UUID)
    }
    private val preferences: Preferences = mockk {
        every { accounts }.answers { listOf(account) }
    }
    private val planckProvider: PlanckProvider = mockk(relaxed = true)
    private val messagingController: MessagingController = mockk(relaxed = true)
    private val messagingControllerProvider: Provider<MessagingController> = mockk {
        every { get() }.returns(messagingController)
    }
    private val timer: Timer = mockk(relaxed = true)
    private val poller: Poller = mockk(relaxed = true)
    private val pollerFactory: PollerFactory = mockk {
        every { createPoller() }.returns(poller)
    }

    private val myself: Identity = Identity()
    private val partner: Identity = Identity()

    private val syncRepository: SyncRepository = PlanckSyncRepository(
        k9,
        preferences,
        planckProvider,
        messagingControllerProvider,
        timer,
        pollerFactory,
        coroutinesTestRule.testDispatcherProvider
    )

    private val syncStates = mutableListOf<SyncAppState>()

    @Before
    fun setUp() {
        mockkStatic(K9::class)
        every { K9.isPlanckSyncEnabled() }.returns(true)
        mockkObject(KeySyncCleaner.Companion)
        every { KeySyncCleaner.queueAutoConsumeMessages() }.just(runs)
        stubTimer()
        syncStates.clear()
        observeSyncDelegateStates()
    }

    @After
    fun tearDown() {
        unmockkStatic(K9::class)
        unmockkObject(KeySyncCleaner.Companion)
    }

    @Test
    fun `SyncDelegate default initial state is Idle`() {
        assertStates(SyncState.Idle)
    }

    @Test
    fun `setCurrentState changes SyncDelegate state`() {
        syncRepository.setCurrentState(SyncState.Done)


        assertStates(SyncState.Idle, SyncState.Done)
    }

    @Test
    fun `setupFastPoller() creates, initializes and starts poller if poller is not initialized`() {
        syncRepository.setupFastPoller()


        verify { pollerFactory.createPoller() }
        verify { poller.init(any(), any()) }
        verify { poller.startPolling() }
    }

    @Test
    fun `setupFastPoller() stops and restarts poller if poller is already initialized`() {
        syncRepository.setupFastPoller()
        clearMocks(pollerFactory, poller, answers = false)
        syncRepository.setupFastPoller()


        verify { pollerFactory.wasNot(called) }
        verify(exactly = 0) { poller.init(any(), any()) }
        verify { poller.stopPolling() }
        verify { poller.startPolling() }
    }

    @Test
    fun `Poller checks planck sync mail if SyncDelegate's current state needs fast polling`() {
        syncRepository.setCurrentState(SyncState.AwaitingOtherDevice())
        val slot = slot<Runnable>()
        every { poller.init(any(), capture(slot)) }.just(runs)
        every { poller.startPolling() }.answers { slot.captured.run() }
        syncRepository.setupFastPoller()


        verify { messagingController.checkpEpSyncMail(k9, any()) }
    }

    @Test
    fun `Poller does not check planck sync mail if SyncDelegate's current state does not need fast polling`() {
        val slot = slot<Runnable>()
        every { poller.init(any(), capture(slot)) }.just(runs)
        every { poller.startPolling() }.answers { slot.captured.run() }
        syncRepository.setupFastPoller()


        verify { messagingController.wasNot(called) }
    }

    @Test
    fun `setPlanckSyncEnabled(true) schedules KeySyncCleaner work`() {
        syncRepository.setPlanckSyncEnabled(true)


        verify { KeySyncCleaner.queueAutoConsumeMessages() }
    }

    @Test
    fun `setPlanckSyncEnabled(true) initializes sync if there any accounts, sync is enabled in app settings and not running`() = runTest {
        coEvery { planckProvider.isSyncRunning() }.returns(false)


        syncRepository.setPlanckSyncEnabled(true)
        advanceUntilIdle()


        coVerify { planckProvider.updateSyncAccountsConfig() }
        coVerify { planckProvider.startSync() }
    }

    @Test
    fun `setPlanckSyncEnabled(true) does not start sync if sync is already running`() = runTest {
        coEvery { planckProvider.isSyncRunning() }.returns(true)


        syncRepository.setPlanckSyncEnabled(true)
        advanceUntilIdle()


        coVerify { planckProvider.updateSyncAccountsConfig() }
        coVerify(exactly = 0) { planckProvider.startSync() }
    }

    @Test
    fun `setPlanckSyncEnabled(true) does not initialize sync if there are no accounts setup in device`() {
        coEvery { planckProvider.isSyncRunning() }.returns(false)
        every { preferences.accounts }.returns(emptyList())


        syncRepository.setPlanckSyncEnabled(true)


        verify { planckProvider.wasNot(called) }
    }

    @Test
    fun `setPlanckSyncEnabled(true) does not initialize sync if sync is disabled in app settings`() {
        every { K9.isPlanckSyncEnabled() }.returns(false)
        coEvery { planckProvider.isSyncRunning() }.returns(false)


        syncRepository.setPlanckSyncEnabled(true)


        verify { planckProvider.wasNot(called) }
    }

    @Test
    fun `setPlanckSyncEnabled(false) shuts down sync if device is not grouped and sync is running`() {
        coEvery { planckProvider.isSyncRunning() }.returns(true)
        syncRepository.isGrouped = false


        syncRepository.setPlanckSyncEnabled(false)


        coVerify { planckProvider.stopSync() }
        verify { k9.markSyncEnabled(false) }
    }

    @Test
    fun `setPlanckSyncEnabled(false) does not shut down sync if device is not grouped and sync is not running`() {
        coEvery { planckProvider.isSyncRunning() }.returns(false)
        syncRepository.isGrouped = false


        syncRepository.setPlanckSyncEnabled(false)


        coVerify(exactly = 0) { planckProvider.stopSync() }
        verify(exactly = 0) { k9.markSyncEnabled(false) }
    }

    @Test
    fun `setPlanckSyncEnabled(false) leaves device group if device is grouped and sync is running`() {
        coEvery { planckProvider.isSyncRunning() }.returns(true)
        syncRepository.isGrouped = true
        coEvery { planckProvider.leaveDeviceGroup() }.returns(ResultCompat.success(Unit))

        syncRepository.setPlanckSyncEnabled(false)


        coVerify { planckProvider.leaveDeviceGroup() }
        assertEquals(false, syncRepository.isGrouped)
    }

    @Test
    fun `setPlanckSyncEnabled(false) does not leave device group if sync is not running`() {
        coEvery { planckProvider.isSyncRunning() }.returns(false)
        syncRepository.isGrouped = true
        coEvery { planckProvider.leaveDeviceGroup() }.returns(ResultCompat.success(Unit))

        syncRepository.setPlanckSyncEnabled(false)


        coVerify(exactly = 0) { planckProvider.leaveDeviceGroup() }
        assertEquals(true, syncRepository.isGrouped)
    }

    @Test
    fun `setPlanckSyncEnabled(false) does not leave device group if PlanckProvider_leaveDeviceGroup() fails`() {
        coEvery { planckProvider.isSyncRunning() }.returns(true)
        syncRepository.isGrouped = true
        coEvery { planckProvider.leaveDeviceGroup() }.returns(ResultCompat.failure(RuntimeException("test")))

        syncRepository.setPlanckSyncEnabled(false)


        assertEquals(true, syncRepository.isGrouped)
    }

    @Test
    fun `allowManualSync() starts or resets timer`() = runTest {
        syncRepository.allowTimedManualSync()
        advanceUntilIdle()


        verify { timer.startOrReset(any(), any()) }
    }

    @Test
    fun `cancelSync() cancels countdown timer and sets state to Cancelled`() {
        syncRepository.cancelSync()


        verify { timer.cancel() }
        assertStates(SyncState.Idle, SyncState.Cancelled)
    }

    @Test
    fun `syncStartTimeout() sets state to SyncStartTimeout`() {
        syncRepository.syncStartTimeout()


        assertStates(SyncState.Idle, SyncState.SyncStartTimeout)
    }

    @Test
    fun `userConnected() does nothing if current state is HandshakeReadyAwaitingUser`() = runTest {
        syncRepository.setCurrentState(SyncState.HandshakeReadyAwaitingUser(myself, partner, false))


        syncRepository.userConnected()


        verify { timer.wasNot(called) }
        assertStates(SyncState.Idle, SyncState.HandshakeReadyAwaitingUser(myself, partner, false))
    }

    @Test
    fun `userConnected() sets state to AwaitingOtherDevice in catchup allowance period and starts timer if current state is not HandshakeReadyAwaitingUser`() = runTest {
        syncRepository.userConnected()


        assertFirstStates(
            SyncState.Idle,
            SyncState.AwaitingOtherDevice(inCatchupAllowancePeriod = true)
        )
        verify { timer.startOrReset(INITIAL_HANDSHAKE_AVAILABLE_WAIT, any()) }
    }

    @Test
    fun `userConnected() stops and restarts sync if current state is not HandshakeReadyAwaitingUser and device just left a device group`() = runTest {
        every { k9.deviceJustLeftGroup() }.returns(true)
        coEvery { planckProvider.isSyncRunning() }.returns(false)


        syncRepository.userConnected()
        advanceUntilIdle()


        coVerify { planckProvider.stopSync() }
        verify { k9.markDeviceJustLeftGroup(false) }
        coVerify { planckProvider.updateSyncAccountsConfig() }
        coVerify { planckProvider.isSyncRunning() }
        coVerify { planckProvider.startSync() }
    }

    @Test
    fun `userConnected() sets state to AwaitingOtherDevice and allows timed handshake if state is not HandshakeReadyAwaitingUser after time up`() = runTest {
        coEvery { planckProvider.isSyncRunning() }.returns(true)


        syncRepository.userConnected()
        advanceUntilIdle()


        assertFirstStates(
            SyncState.Idle,
            SyncState.AwaitingOtherDevice(inCatchupAllowancePeriod = true),
            SyncState.AwaitingOtherDevice(inCatchupAllowancePeriod = false)
        )
        verify { timer.startOrReset(INITIAL_HANDSHAKE_AVAILABLE_WAIT, any()) }
        coVerify { planckProvider.isSyncRunning() }
        coVerify { planckProvider.syncReset() }
        verify { timer.startOrReset(MANUAL_SYNC_TIME_LIMIT, any()) }
    }

    @Test
    fun `userConnected() does nothing if state is HandshakeReadyAwaitingUser after time up`() = runTest {
        coEvery { planckProvider.isSyncRunning() }.returns(true)
        stubTimer {
            syncRepository.setCurrentState(
                SyncState.HandshakeReadyAwaitingUser(
                    myself,
                    partner,
                    false
                )
            )
        }


        syncRepository.userConnected()


        assertStates(
            SyncState.Idle,
            SyncState.AwaitingOtherDevice(inCatchupAllowancePeriod = true),
            SyncState.HandshakeReadyAwaitingUser(myself, partner, false)
        )
        verify { timer.startOrReset(INITIAL_HANDSHAKE_AVAILABLE_WAIT, any()) }
        verify { planckProvider.wasNot(called) }
        verify(exactly = 0) { timer.startOrReset(MANUAL_SYNC_TIME_LIMIT, any()) }
    }

    private fun stubTimer(doBeforeTimeUp: () -> Unit = {}) {
        val slot = slot<() -> Unit>()
        every { timer.startOrReset(any(), capture(slot)) }.answers {
            doBeforeTimeUp()
            slot.captured()
        }
    }

    @Test
    fun `userDisconnected() cancels timer and sets state to Idle`() {
        syncRepository.setCurrentState(SyncState.HandshakeReadyAwaitingUser(myself, partner, false))
        syncRepository.userDisconnected()


        verify { timer.cancel() }
        assertStates(
            SyncState.Idle,
            SyncState.HandshakeReadyAwaitingUser(myself, partner, false),
            SyncState.Idle,
        )
    }

    /***************************
     * SYNC CALLBACK RELATED TESTS
     ***************************/

    @Test
    fun `notifyHandshake(SyncNotifyTimeout) sets state to TimeoutError`() {
        notifyHandshake(myself, partner, SyncHandshakeSignal.SyncNotifyTimeout)

        assertStates(SyncState.Idle, SyncState.TimeoutError)
    }

    @Test
    fun `notifyHandshake(SyncPassphraseRequired) uses K9 to display passphrase screen`() {
        notifyHandshake(myself, partner, SyncHandshakeSignal.SyncPassphraseRequired)


        verify { k9.showPassphraseDialogForSync() }
    }

    @Test
    fun `notifyHandshake(DistributionNotifyGroupInvite) uses MessagingController to notify and join group`() {
        every { preferences.defaultAccount }.returns(account)


        notifyHandshake(myself, partner, SyncHandshakeSignal.DistributionNotifyGroupInvite)


        val slot = slot<GroupMailSignal>()
        verify { messagingController.notifyPlanckGroupInviteAndJoinGroup(account, capture(slot)) }
        val groupMailSignal = slot.captured
        assertEquals(UUID, groupMailSignal.accountUuid)
        assertEquals(myself, groupMailSignal.groupIdentity)
        assertEquals(partner, groupMailSignal.senderIdentity)
    }

    @Test
    fun `notifyHandshake(SyncNotifyAcceptedDeviceAdded) sets state to Done and sets grouped to true`() {
        syncRepository.isGrouped = false


        notifyHandshake(myself, partner, SyncHandshakeSignal.SyncNotifyAcceptedDeviceAdded)


        assertStates(SyncState.Idle, SyncState.Done)
        assertTrue(syncRepository.isGrouped)
    }

    @Test
    fun `notifyHandshake(SyncNotifyAcceptedGroupCreated) sets state to Done and sets grouped to true`() {
        syncRepository.isGrouped = false


        notifyHandshake(myself, partner, SyncHandshakeSignal.SyncNotifyAcceptedGroupCreated)


        assertStates(SyncState.Idle, SyncState.Done)
        assertTrue(syncRepository.isGrouped)
    }

    @Test
    fun `notifyHandshake(SyncNotifyAcceptedDeviceAccepted) sets state to Done and sets grouped to true`() {
        syncRepository.isGrouped = false


        notifyHandshake(myself, partner, SyncHandshakeSignal.SyncNotifyAcceptedDeviceAccepted)


        assertStates(SyncState.Idle, SyncState.Done)
        assertTrue(syncRepository.isGrouped)
    }

    @Test
    fun `notifyHandshake(SyncNotifySole) sets grouped to false and restarts timer if current state allows it`() {
        syncRepository.isGrouped = true
        syncRepository.setCurrentState(SyncState.AwaitingOtherDevice(inCatchupAllowancePeriod = false))


        notifyHandshake(myself, partner, SyncHandshakeSignal.SyncNotifySole)


        verify { timer.startOrReset(any(), any()) }
        assertFalse(syncRepository.isGrouped)
    }

    @Test
    fun `notifyHandshake(SyncNotifySole) does not restart timer if current state does not allow it`() {
        syncRepository.isGrouped = true
        syncRepository.setCurrentState(SyncState.AwaitingOtherDevice(inCatchupAllowancePeriod = true))


        notifyHandshake(myself, partner, SyncHandshakeSignal.SyncNotifySole)


        verify { timer.wasNot(called) }
        assertFalse(syncRepository.isGrouped)
    }

    @Test
    fun `notifyHandshake(SyncNotifyInGroup) marks sync as enabled, sets grouped to true and restarts timer if current state allows it`() {
        syncRepository.isGrouped = false
        syncRepository.setCurrentState(SyncState.AwaitingOtherDevice(inCatchupAllowancePeriod = false))


        notifyHandshake(myself, partner, SyncHandshakeSignal.SyncNotifyInGroup)


        verify { k9.markSyncEnabled(true) }
        verify { timer.startOrReset(any(), any()) }
        assertTrue(syncRepository.isGrouped)
    }

    @Test
    fun `notifyHandshake(SyncNotifyInGroup) does not restart timer if current state does not allow it`() {
        syncRepository.isGrouped = false
        syncRepository.setCurrentState(SyncState.AwaitingOtherDevice(inCatchupAllowancePeriod = true))


        notifyHandshake(myself, partner, SyncHandshakeSignal.SyncNotifyInGroup)


        verify { k9.markSyncEnabled(true) }
        verify { timer.wasNot(called) }
        assertTrue(syncRepository.isGrouped)
    }

    @Test
    fun `notifyHandshake(SyncNotifyInitAddOurDevice) sets state to HandshakeReadyAwaitingUser if handshake is not locked and device is not grouped`() {
        syncRepository.isGrouped = false


        notifyHandshake(myself, partner, SyncHandshakeSignal.SyncNotifyInitAddOurDevice)


        verify { timer.cancel() }
        assertStates(
            SyncState.Idle,
            SyncState.HandshakeReadyAwaitingUser(myself, partner, false)
        )
    }

    @Test
    fun `notifyHandshake(SyncNotifyInitAddOurDevice) does not set state to HandshakeReadyAwaitingUser if handshake is locked`() {
        syncRepository.isGrouped = false
        syncRepository.lockHandshake()


        notifyHandshake(myself, partner, SyncHandshakeSignal.SyncNotifyInitAddOurDevice)


        assertStates(SyncState.Idle)
    }

    @Test
    fun `notifyHandshake(SyncNotifyInitAddOurDevice) does not set state to HandshakeReadyAwaitingUser if device is already grouped`() {
        syncRepository.isGrouped = true


        notifyHandshake(myself, partner, SyncHandshakeSignal.SyncNotifyInitAddOurDevice)


        assertStates(
            SyncState.Idle,
        )
    }

    @Test
    fun `notifyHandshake(SyncNotifyInitAddOtherDevice) sets state to HandshakeReadyAwaitingUser if handshake is not locked and device is grouped`() {
        syncRepository.isGrouped = true


        notifyHandshake(myself, partner, SyncHandshakeSignal.SyncNotifyInitAddOtherDevice)


        verify { timer.cancel() }
        assertStates(
            SyncState.Idle,
            SyncState.HandshakeReadyAwaitingUser(myself, partner, false)
        )
    }

    @Test
    fun `notifyHandshake(SyncNotifyInitAddOtherDevice) does not set state to HandshakeReadyAwaitingUser if handshake is locked`() {
        syncRepository.isGrouped = true
        syncRepository.lockHandshake()


        notifyHandshake(myself, partner, SyncHandshakeSignal.SyncNotifyInitAddOtherDevice)


        assertStates(SyncState.Idle)
    }

    @Test
    fun `notifyHandshake(SyncNotifyInitAddOtherDevice) does not set state to HandshakeReadyAwaitingUser if device is not grouped`() {
        syncRepository.isGrouped = false


        notifyHandshake(myself, partner, SyncHandshakeSignal.SyncNotifyInitAddOtherDevice)


        assertStates(SyncState.Idle)
    }

    @Test
    fun `notifyHandshake(SyncNotifyInitFormGroup) sets state to HandshakeReadyAwaitingUser if handshake is not locked and device is not grouped`() {
        syncRepository.isGrouped = false


        notifyHandshake(myself, partner, SyncHandshakeSignal.SyncNotifyInitFormGroup)


        verify { timer.cancel() }
        assertStates(
            SyncState.Idle,
            SyncState.HandshakeReadyAwaitingUser(myself, partner, true)
        )
    }

    @Test
    fun `notifyHandshake(SyncNotifyInitFormGroup) does not set state to HandshakeReadyAwaitingUser if handshake is locked`() {
        syncRepository.isGrouped = false
        syncRepository.lockHandshake()


        notifyHandshake(myself, partner, SyncHandshakeSignal.SyncNotifyInitFormGroup)


        assertStates(SyncState.Idle)
    }

    @Test
    fun `notifyHandshake(SyncNotifyInitFormGroup) does not set state to HandshakeReadyAwaitingUser if device is already grouped`() {
        syncRepository.isGrouped = true


        notifyHandshake(myself, partner, SyncHandshakeSignal.SyncNotifyInitFormGroup)


        assertStates(SyncState.Idle)
    }

    private fun notifyHandshake(myself: Identity, partner: Identity, signal: SyncHandshakeSignal) {
        syncRepository.notifyHandshakeCallback.notifyHandshake(myself, partner, signal)
    }

    private fun observeSyncDelegateStates() {
        CoroutineScope(UnconfinedTestDispatcher()).launch {
            syncRepository.syncStateFlow.collect {
                syncStates.add(it)
            }
        }
    }

    private fun assertStates(vararg states: SyncAppState) {
        assertEquals(states.toList(), syncStates)
    }

    private fun assertFirstStates(vararg states: SyncAppState) {
        states.forEachIndexed { index, syncAppState ->
            assertEquals(syncAppState, syncStates[index])
        }
    }
}