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
import com.fsck.k9.planck.manualsync.ManualSyncCountDownTimer
import com.fsck.k9.planck.manualsync.SyncAppState
import com.fsck.k9.planck.manualsync.SyncState
import dagger.Lazy
import foundation.pEp.jniadapter.Identity
import foundation.pEp.jniadapter.SyncHandshakeSignal
import io.mockk.called
import io.mockk.clearMocks
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
import org.junit.After
import org.junit.Before
import org.junit.Test
import security.planck.notification.GroupMailSignal
import javax.inject.Provider

private const val UUID = "uuid1"

@ExperimentalCoroutinesApi
class SyncRepositoryTest : RobolectricTest() {
    private val k9: K9 = mockk(relaxed = true)
    private val account: Account = mockk {
        every { uuid }.returns(UUID)
    }
    private val preferences: Preferences = mockk {
        every { accounts }.returns(listOf(account))
    }
    private val planckProvider: PlanckProvider = mockk(relaxed = true)
    private val messagingController: MessagingController = mockk(relaxed = true)
    private val messagingControllerProvider: Provider<MessagingController> = mockk {
        every { get() }.returns(messagingController)
    }
    private val countDownTimer: ManualSyncCountDownTimer = mockk(relaxed = true)
    private val countDownTimerLazy: Lazy<ManualSyncCountDownTimer> = mockk {
        every { get() }.returns(countDownTimer)
    }
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
        countDownTimerLazy,
        pollerFactory,
    )

    private val syncStates = mutableListOf<SyncAppState>()

    @Before
    fun setUp() {
        mockkStatic(K9::class)
        every { K9.isPlanckSyncEnabled() }.returns(true)
        mockkObject(KeySyncCleaner.Companion)
        every { KeySyncCleaner.queueAutoConsumeMessages() }.just(runs)
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
        syncRepository.setCurrentState(SyncState.AwaitingOtherDevice)
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
    fun `setPlanckSyncEnabled(true) initializes sync if there any accounts, sync is enabled in app settings and not running`() {
        every { planckProvider.isSyncRunning }.returns(false)


        syncRepository.setPlanckSyncEnabled(true)


        verify { planckProvider.updateSyncAccountsConfig() }
        verify { planckProvider.startSync() }
    }

    @Test
    fun `setPlanckSyncEnabled(true) does not start sync if sync is already running`() {
        every { planckProvider.isSyncRunning }.returns(true)


        syncRepository.setPlanckSyncEnabled(true)


        verify { planckProvider.updateSyncAccountsConfig() }
        verify(exactly = 0) { planckProvider.startSync() }
    }

    @Test
    fun `setPlanckSyncEnabled(true) does not initialize sync if there are no accounts setup in device`() {
        every { planckProvider.isSyncRunning }.returns(false)
        every { preferences.accounts }.returns(emptyList())


        syncRepository.setPlanckSyncEnabled(true)


        verify { planckProvider.wasNot(called) }
    }

    @Test
    fun `setPlanckSyncEnabled(true) does not initialize sync if sync is disabled in app settings`() {
        every { K9.isPlanckSyncEnabled() }.returns(false)
        every { planckProvider.isSyncRunning }.returns(false)


        syncRepository.setPlanckSyncEnabled(true)


        verify { planckProvider.wasNot(called) }
    }

    @Test
    fun `setPlanckSyncEnabled(false) shuts down sync if device is not grouped and sync is running`() {
        every { planckProvider.isSyncRunning }.returns(true)
        syncRepository.isGrouped = false


        syncRepository.setPlanckSyncEnabled(false)


        verify { planckProvider.stopSync() }
        verify { k9.markSyncEnabled(false) }
    }

    @Test
    fun `setPlanckSyncEnabled(false) does not shut down sync if device is not grouped and sync is not running`() {
        every { planckProvider.isSyncRunning }.returns(false)
        syncRepository.isGrouped = false


        syncRepository.setPlanckSyncEnabled(false)


        verify(exactly = 0) { planckProvider.stopSync() }
        verify(exactly = 0) { k9.markSyncEnabled(false) }
    }

    @Test
    fun `setPlanckSyncEnabled(false) leaves device group if device is grouped and sync is running`() {
        every { planckProvider.isSyncRunning }.returns(true)
        syncRepository.isGrouped = true
        every { planckProvider.leaveDeviceGroup() }.returns(ResultCompat.success(Unit))

        syncRepository.setPlanckSyncEnabled(false)


        verify { planckProvider.leaveDeviceGroup() }
        assertEquals(false, syncRepository.isGrouped)
    }

    @Test
    fun `setPlanckSyncEnabled(false) does not leave device group if sync is not running`() {
        every { planckProvider.isSyncRunning }.returns(false)
        syncRepository.isGrouped = true
        every { planckProvider.leaveDeviceGroup() }.returns(ResultCompat.success(Unit))

        syncRepository.setPlanckSyncEnabled(false)


        verify(exactly = 0) { planckProvider.leaveDeviceGroup() }
        assertEquals(true, syncRepository.isGrouped)
    }

    @Test
    fun `setPlanckSyncEnabled(false) does not leave device group if PlanckProvider_leaveDeviceGroup() fails`() {
        every { planckProvider.isSyncRunning }.returns(true)
        syncRepository.isGrouped = true
        every { planckProvider.leaveDeviceGroup() }.returns(ResultCompat.failure(RuntimeException("test")))

        syncRepository.setPlanckSyncEnabled(false)


        assertEquals(true, syncRepository.isGrouped)
    }

    @Test
    fun `allowManualSync() starts or reset countdown timer`() {
        syncRepository.allowTimedManualSync()


        verify { countDownTimer.startOrReset() }
    }

    @Test
    fun `cancelSync() cancels countdown timer and sets state to Cancelled`() {
        syncRepository.cancelSync()


        verify { countDownTimer.cancel() }
        assertStates(SyncState.Idle, SyncState.Cancelled)
    }

    @Test
    fun `syncStartTimeout() sets state to SyncStartTimeout`() {
        syncRepository.syncStartTimeout()


        assertStates(SyncState.Idle, SyncState.SyncStartTimeout)
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
        syncRepository.setCurrentState(SyncState.AwaitingOtherDevice)
        notifyHandshake(myself, partner, SyncHandshakeSignal.SyncNotifySole)


        verify { countDownTimer.startOrReset() }
        assertFalse(syncRepository.isGrouped)
    }

    @Test
    fun `notifyHandshake(SyncNotifySole) does not restart timer if current state does not allow it`() {
        syncRepository.isGrouped = true
        notifyHandshake(myself, partner, SyncHandshakeSignal.SyncNotifySole)


        verify { countDownTimer.wasNot(called) }
        assertFalse(syncRepository.isGrouped)
    }

    @Test
    fun `notifyHandshake(SyncNotifyInGroup) marks sync as enabled, sets grouped to true and restarts timer if current state allows it`() {
        syncRepository.isGrouped = false
        syncRepository.setCurrentState(SyncState.AwaitingOtherDevice)
        notifyHandshake(myself, partner, SyncHandshakeSignal.SyncNotifyInGroup)


        verify { k9.markSyncEnabled(true) }
        verify { countDownTimer.startOrReset() }
        assertTrue(syncRepository.isGrouped)
    }

    @Test
    fun `notifyHandshake(SyncNotifyInGroup) does not restart timer if current state does not allow it`() {
        syncRepository.isGrouped = false
        notifyHandshake(myself, partner, SyncHandshakeSignal.SyncNotifyInGroup)


        verify { k9.markSyncEnabled(true) }
        verify { countDownTimer.wasNot(called) }
        assertTrue(syncRepository.isGrouped)
    }

    @Test
    fun `notifyHandshake(SyncNotifyInitAddOurDevice) sets state to HandshakeReadyAwaitingUser if current state is AwaitingOtherDevice and device is not grouped`() {
        syncRepository.isGrouped = false
        syncRepository.setCurrentState(SyncState.AwaitingOtherDevice)
        notifyHandshake(myself, partner, SyncHandshakeSignal.SyncNotifyInitAddOurDevice)


        assertStates(
            SyncState.Idle,
            SyncState.AwaitingOtherDevice,
            SyncState.HandshakeReadyAwaitingUser(myself, partner, false)
        )
    }

    @Test
    fun `notifyHandshake(SyncNotifyInitAddOurDevice) sets state to HandshakeReadyAwaitingUser if current state is Idle and device is not grouped`() {
        syncRepository.isGrouped = false
        syncRepository.setCurrentState(SyncState.Idle)
        notifyHandshake(myself, partner, SyncHandshakeSignal.SyncNotifyInitAddOurDevice)


        assertStates(
            SyncState.Idle,
            SyncState.HandshakeReadyAwaitingUser(myself, partner, false)
        )
    }

    @Test
    fun `notifyHandshake(SyncNotifyInitAddOurDevice) does not set state to HandshakeReadyAwaitingUser if current state is not AwaitingOtherDevice or Idle`() {
        syncRepository.isGrouped = false
        syncRepository.setCurrentState(SyncState.SyncStartTimeout)
        notifyHandshake(myself, partner, SyncHandshakeSignal.SyncNotifyInitAddOurDevice)


        assertStates(SyncState.Idle, SyncState.SyncStartTimeout)
    }

    @Test
    fun `notifyHandshake(SyncNotifyInitAddOurDevice) does not set state to HandshakeReadyAwaitingUser if device is already grouped`() {
        syncRepository.isGrouped = true
        syncRepository.setCurrentState(SyncState.AwaitingOtherDevice)
        notifyHandshake(myself, partner, SyncHandshakeSignal.SyncNotifyInitAddOurDevice)


        assertStates(SyncState.Idle, SyncState.AwaitingOtherDevice)
    }

    @Test
    fun `notifyHandshake(SyncNotifyInitAddOtherDevice) sets state to HandshakeReadyAwaitingUser if current state is AwaitingOtherDevice and device is grouped`() {
        syncRepository.isGrouped = true
        syncRepository.setCurrentState(SyncState.AwaitingOtherDevice)
        notifyHandshake(myself, partner, SyncHandshakeSignal.SyncNotifyInitAddOtherDevice)


        assertStates(
            SyncState.Idle,
            SyncState.AwaitingOtherDevice,
            SyncState.HandshakeReadyAwaitingUser(myself, partner, false)
        )
    }

    @Test
    fun `notifyHandshake(SyncNotifyInitAddOtherDevice) sets state to HandshakeReadyAwaitingUser if current state is Idle and device is grouped`() {
        syncRepository.isGrouped = true
        syncRepository.setCurrentState(SyncState.Idle)
        notifyHandshake(myself, partner, SyncHandshakeSignal.SyncNotifyInitAddOtherDevice)


        assertStates(
            SyncState.Idle,
            SyncState.HandshakeReadyAwaitingUser(myself, partner, false)
        )
    }

    @Test
    fun `notifyHandshake(SyncNotifyInitAddOtherDevice) does not set state to HandshakeReadyAwaitingUser if current state is not AwaitingOtherDevice or idle`() {
        syncRepository.isGrouped = true
        syncRepository.setCurrentState(SyncState.TimeoutError)
        notifyHandshake(myself, partner, SyncHandshakeSignal.SyncNotifyInitAddOtherDevice)


        assertStates(SyncState.Idle, SyncState.TimeoutError)
    }

    @Test
    fun `notifyHandshake(SyncNotifyInitAddOtherDevice) does not set state to HandshakeReadyAwaitingUser if device is not grouped`() {
        syncRepository.isGrouped = false
        syncRepository.setCurrentState(SyncState.AwaitingOtherDevice)
        notifyHandshake(myself, partner, SyncHandshakeSignal.SyncNotifyInitAddOtherDevice)


        assertStates(SyncState.Idle, SyncState.AwaitingOtherDevice)
    }

    @Test
    fun `notifyHandshake(SyncNotifyInitFormGroup) sets state to HandshakeReadyAwaitingUser if current state is AwaitingOtherDevice and device is not grouped`() {
        syncRepository.isGrouped = false
        syncRepository.setCurrentState(SyncState.AwaitingOtherDevice)
        notifyHandshake(myself, partner, SyncHandshakeSignal.SyncNotifyInitFormGroup)


        assertStates(
            SyncState.Idle,
            SyncState.AwaitingOtherDevice,
            SyncState.HandshakeReadyAwaitingUser(myself, partner, true)
        )
    }

    @Test
    fun `notifyHandshake(SyncNotifyInitFormGroup) sets state to HandshakeReadyAwaitingUser if current state is Idle and device is not grouped`() {
        syncRepository.isGrouped = false
        syncRepository.setCurrentState(SyncState.Idle)
        notifyHandshake(myself, partner, SyncHandshakeSignal.SyncNotifyInitFormGroup)


        assertStates(
            SyncState.Idle,
            SyncState.HandshakeReadyAwaitingUser(myself, partner, true)
        )
    }

    @Test
    fun `notifyHandshake(SyncNotifyInitFormGroup) does not set state to HandshakeReadyAwaitingUser if current state is not AwaitingOtherDevice or Idle`() {
        syncRepository.isGrouped = false
        syncRepository.setCurrentState(SyncState.TimeoutError)
        notifyHandshake(myself, partner, SyncHandshakeSignal.SyncNotifyInitFormGroup)


        assertStates(SyncState.Idle, SyncState.TimeoutError)
    }

    @Test
    fun `notifyHandshake(SyncNotifyInitFormGroup) does not set state to HandshakeReadyAwaitingUser if device is already grouped`() {
        syncRepository.isGrouped = true
        syncRepository.setCurrentState(SyncState.AwaitingOtherDevice)
        notifyHandshake(myself, partner, SyncHandshakeSignal.SyncNotifyInitFormGroup)


        assertStates(SyncState.Idle, SyncState.AwaitingOtherDevice)
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
}