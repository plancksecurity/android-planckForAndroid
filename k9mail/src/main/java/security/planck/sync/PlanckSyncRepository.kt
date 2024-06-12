package security.planck.sync

import android.util.Log
import com.fsck.k9.BuildConfig
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.planck.DispatcherProvider
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.PlanckProvider.CompletedCallback
import com.fsck.k9.planck.infrastructure.Poller
import com.fsck.k9.planck.infrastructure.PollerFactory
import com.fsck.k9.planck.manualsync.SyncAppState
import com.fsck.k9.planck.manualsync.SyncState
import foundation.pEp.jniadapter.Identity
import foundation.pEp.jniadapter.Sync.NotifyHandshakeCallback
import foundation.pEp.jniadapter.SyncHandshakeSignal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import security.planck.notification.GroupMailSignal.Companion.fromSignal
import security.planck.sync.KeySyncCleaner.Companion.queueAutoConsumeMessages
import security.planck.timer.Timer
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

private const val POLLING_INTERVAL = 2000L
internal const val MANUAL_SYNC_TIME_LIMIT = 120000L // 2 minutes
internal const val INITIAL_HANDSHAKE_AVAILABLE_WAIT = 20000L // 20 seconds

@Singleton
class PlanckSyncRepository @Inject constructor(
    private val k9: K9,
    private val preferences: Preferences,
    private val planckProvider: PlanckProvider,
    private val messagingController: Provider<MessagingController>,
    private val timer: Timer,
    private val pollerFactory: PollerFactory,
    dispatcherProvider: DispatcherProvider,
) : SyncRepository {
    private val syncStateMutableFlow: MutableStateFlow<SyncAppState> =
        MutableStateFlow(SyncState.Idle)
    override val syncStateFlow = syncStateMutableFlow.asStateFlow()
    private val syncState: SyncAppState
        get() = syncStateMutableFlow.value
    override var isGrouped = false
    private var isPollingMessages = false
    private var poller: Poller? = null
    private val engineScope = CoroutineScope(dispatcherProvider.planckDispatcher())

    @Volatile
    private var handshakeLocked = false

    override val notifyHandshakeCallback = NotifyHandshakeCallback { myself, partner, signal ->
        k9.showHandshakeSignalOnDebug(signal.name)
        when (signal) {
            SyncHandshakeSignal.SyncNotifyInitAddOurDevice ->
                foundPartnerDevice(
                    myself,
                    partner,
                    formingGroup = false,
                    groupedCondition = false
                )

            SyncHandshakeSignal.SyncNotifyInitAddOtherDevice -> {
                foundPartnerDevice(
                    myself,
                    partner,
                    formingGroup = false,
                    groupedCondition = true
                )
            }

            SyncHandshakeSignal.SyncNotifyInitFormGroup -> {
                foundPartnerDevice(
                    myself,
                    partner,
                    formingGroup = true,
                    groupedCondition = false
                )
            }

            SyncHandshakeSignal.SyncNotifyTimeout -> { //Close handshake
                syncStateMutableFlow.value = SyncState.TimeoutError
            }

            SyncHandshakeSignal.SyncNotifyAcceptedDeviceAdded,
            SyncHandshakeSignal.SyncNotifyAcceptedGroupCreated,
            SyncHandshakeSignal.SyncNotifyAcceptedDeviceAccepted -> {
                syncStateMutableFlow.value = SyncState.Done
                isGrouped = true
                messagingController.get().tryToDecryptMessagesThatCouldNotDecryptBefore()
            }

            SyncHandshakeSignal.SyncNotifySole -> {
                isGrouped = false
                if (syncState.allowSyncNewDevices) {
                    startOrResetManualSyncTimer()
                } else {
                    cancelIfCancelledByPartner()
                }
            }

            SyncHandshakeSignal.SyncNotifyInGroup -> {
                isGrouped = true
                k9.markSyncEnabled(true)
                if (syncState.allowSyncNewDevices) {
                    startOrResetManualSyncTimer()
                } else {
                    cancelIfCancelledByPartner()
                }
            }

            SyncHandshakeSignal.SyncPassphraseRequired -> {
                k9.showPassphraseDialogForSync("android04@planck.dev")
            }

            SyncHandshakeSignal.DistributionNotifyGroupInvite -> {
                preferences.defaultAccount?.let { account ->
                    messagingController.get()
                        .notifyPlanckGroupInviteAndJoinGroup(
                            account,
                            fromSignal(myself, partner, account)
                        )
                }
            }

            else -> {}
        }
    }

    private fun cancelIfCancelledByPartner() {
        engineScope.launch {
            if (soleOrInGroupMeansCancel()) {
                cancelSync()
            }
        }
    }

    private fun soleOrInGroupMeansCancel() = (syncState is SyncState.HandshakeReadyAwaitingUser
            || syncState is SyncState.PerformingHandshake)

    private fun foundPartnerDevice(
        myself: Identity,
        partner: Identity,
        formingGroup: Boolean,
        groupedCondition: Boolean,
    ) {
        if (!handshakeLocked && isGrouped == groupedCondition) {
            cancelTimer()
            syncStateMutableFlow.value = SyncState.HandshakeReadyAwaitingUser(
                myself,
                partner,
                formingGroup
            )
        }
    }

    override fun setCurrentState(state: SyncAppState) {
        syncStateMutableFlow.value = state
    }

    override fun setupFastPoller() {
        if (poller == null) {
            poller = pollerFactory.createPoller()
            poller?.init(POLLING_INTERVAL.toLong()) { polling() }
        } else {
            poller?.stopPolling()
        }
        poller?.startPolling()
    }

    private fun polling() {
        if (syncState.needsFastPolling && !isPollingMessages) {
            if (BuildConfig.DEBUG) {
                Log.d("pEpDecrypt", "Entering looper")
            }
            isPollingMessages = true
            messagingController.get().checkpEpSyncMail(k9, object : CompletedCallback {
                override fun onComplete() {
                    isPollingMessages = false
                }

                override fun onError(throwable: Throwable) {
                    if (BuildConfig.DEBUG) {
                        Log.e("pEpSync", "onError: ", throwable)
                    }
                    isPollingMessages = false
                }
            })
        }
    }


    override fun setPlanckSyncEnabled(enabled: Boolean) {
        runBlocking {
            setPlanckSyncEnabledSuspend(enabled)
        }
    }

    private suspend fun setPlanckSyncEnabledSuspend(enabled: Boolean) {
        if (enabled) {
            planckInitSyncEnvironment()
        } else {
            if (isGrouped) {
                leaveDeviceGroup()
            }
            shutdownSync()
        }
    }

    override fun planckInitSyncEnvironment() {
        queueAutoConsumeMessages()
        if (preferences.accounts.isNotEmpty()) {
            if (K9.isPlanckSyncEnabled()) {
                initSync()
            }
        } else {
            if (BuildConfig.DEBUG) {
                Log.e("pEpEngine-app", "There is no accounts set up, not trying to start sync")
            }
        }
    }

    private fun initSync() {
        engineScope.launch {
            planckProvider.updateSyncAccountsConfig()
            if (!planckProvider.isSyncRunning()) {
                planckProvider.startSync()
            }
        }
    }

    private fun startOrResetManualSyncTimer() {
        timer.startOrReset(MANUAL_SYNC_TIME_LIMIT) { syncStartTimeout() }
    }

    override fun allowTimedManualSync() {
        engineScope.launch {
            when (planckProvider.isSyncRunning()) {
                true -> planckProvider.syncReset()
                else -> planckProvider.startSync()
            }
            startOrResetManualSyncTimer()
        }
    }

    override fun cancelSync() {
        cancelTimer()
        syncStateMutableFlow.value = SyncState.Cancelled
    }

    override fun syncStartTimeout() {
        syncStateMutableFlow.value = SyncState.SyncStartTimeout
    }

    private fun cancelTimer() {
        timer.cancel()
    }

    private suspend fun leaveDeviceGroup() {
        if (planckProvider.isSyncRunning()) {
            planckProvider.leaveDeviceGroup()
                .onSuccess { isGrouped = false }
                .onFailure {
                    if (BuildConfig.DEBUG) {
                        Log.e("pEpEngine", "error calling leaveDeviceGroup", it)
                    }
                }
        }
    }

    override suspend fun shutdownSync() {
        if (BuildConfig.DEBUG) {
            Log.e("pEpEngine", "shutdownSync: start")
        }
        planckProvider.stopSync()
        k9.markSyncEnabled(false)
        if (BuildConfig.DEBUG) {
            Log.e("pEpEngine", "shutdownSync: end")
        }
    }

    override suspend fun userConnected() {
        val initialState = syncStateMutableFlow.value
        if (initialState != SyncState.Idle) {
            Timber.e("unexpected initial state: ${syncStateMutableFlow.value}")
        }
        if (initialState !is SyncState.HandshakeReadyAwaitingUser) {
            resetSyncIfDeviceGroupLeft()
            syncStateMutableFlow.value =
                SyncState.AwaitingOtherDevice(inCatchupAllowancePeriod = true)
            startCatchupAllowancePeriod()
        }
    }

    private suspend fun resetSyncIfDeviceGroupLeft() {
        if (k9.deviceJustLeftGroup()) {
            planckProvider.stopSync()
            k9.markDeviceJustLeftGroup(false)
            setPlanckSyncEnabledSuspend(true)
        }
    }

    private fun startCatchupAllowancePeriod() {
        timer.startOrReset(INITIAL_HANDSHAKE_AVAILABLE_WAIT) {
            if (syncState !is SyncState.HandshakeReadyAwaitingUser) {
                syncStateMutableFlow.value =
                    SyncState.AwaitingOtherDevice(inCatchupAllowancePeriod = false)
                allowTimedManualSync()
            }
        }
    }

    override fun lockHandshake() {
        handshakeLocked = true
    }

    private fun unlockHandshake() {
        handshakeLocked = false
    }

    override fun userDisconnected() {
        cancelTimer()
        syncStateMutableFlow.value = SyncState.Idle
        unlockHandshake()
    }
}