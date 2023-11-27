package security.planck.sync

import android.util.Log
import com.fsck.k9.BuildConfig
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.PlanckProvider.CompletedCallback
import com.fsck.k9.planck.infrastructure.Poller
import com.fsck.k9.planck.infrastructure.PollerFactory
import com.fsck.k9.planck.manualsync.SyncAppState
import com.fsck.k9.planck.manualsync.SyncState
import foundation.pEp.jniadapter.Identity
import foundation.pEp.jniadapter.Sync.NotifyHandshakeCallback
import foundation.pEp.jniadapter.SyncHandshakeSignal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
) : SyncRepository {
    private val syncStateMutableFlow: MutableStateFlow<SyncAppState> =
        MutableStateFlow(SyncState.Idle)
    override val syncStateFlow = syncStateMutableFlow.asStateFlow()
    private val syncState: SyncAppState
        get() = syncStateMutableFlow.value
    override var isGrouped = false
    private var isPollingMessages = false
    private var poller: Poller? = null

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
            }

            SyncHandshakeSignal.SyncNotifySole -> {
                isGrouped = false
                if (syncState.allowSyncNewDevices) {
                    startOrResetManualSyncTimer()
                }
            }

            SyncHandshakeSignal.SyncNotifyInGroup -> {
                isGrouped = true
                k9.markSyncEnabled(true)
                if (syncState.allowSyncNewDevices) {
                    startOrResetManualSyncTimer()
                }
            }

            SyncHandshakeSignal.SyncPassphraseRequired -> {
                k9.showPassphraseDialogForSync()
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
        if (enabled) {
            planckInitSyncEnvironment()
        } else if (isGrouped) {
            leaveDeviceGroup()
        } else {
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
        planckProvider.updateSyncAccountsConfig()
        if (!planckProvider.isSyncRunning) {
            planckProvider.startSync()
        }
    }

    private fun updateDeviceGrouped() {
        isGrouped = planckProvider.isDeviceGrouped
    }

    private fun startOrResetManualSyncTimer() {
        timer.startOrReset(MANUAL_SYNC_TIME_LIMIT) { syncStartTimeout() }
    }

    override fun allowTimedManualSync() {
        when (planckProvider.isSyncRunning) {
            true -> planckProvider.syncReset()
            else -> planckProvider.startSync()
        }
        startOrResetManualSyncTimer()
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

    private fun leaveDeviceGroup() {
        if (planckProvider.isSyncRunning) {
            planckProvider.leaveDeviceGroup()
                .onSuccess { isGrouped = false }
                .onFailure {
                    if (BuildConfig.DEBUG) {
                        Log.e("pEpEngine", "error calling leaveDeviceGroup", it)
                    }
                }
        }
    }

    override fun shutdownSync() {
        if (BuildConfig.DEBUG) {
            Log.e("pEpEngine", "shutdownSync: start")
        }
        if (planckProvider.isSyncRunning) {
            planckProvider.stopSync()
            k9.markSyncEnabled(false)
        }
        if (BuildConfig.DEBUG) {
            Log.e("pEpEngine", "shutdownSync: end")
        }
    }

    override fun userConnected() {
        val initialState = syncStateMutableFlow.value
        if (initialState != SyncState.Idle) {
            Timber.e("unexpected initial state: ${syncStateMutableFlow.value}")
        }
        if (initialState !is SyncState.HandshakeReadyAwaitingUser) {
            if (k9.deviceJustLeftGroup()) {
                planckProvider.stopSync()
                k9.markDeviceJustLeftGroup(false)
                setPlanckSyncEnabled(true)
            }
            syncStateMutableFlow.value =
                SyncState.AwaitingOtherDevice(inCatchupAllowancePeriod = true)
            startCatchupAllowancePeriod()
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