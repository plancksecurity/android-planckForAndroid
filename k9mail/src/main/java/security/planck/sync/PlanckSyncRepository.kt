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
import com.fsck.k9.planck.manualsync.ManualSyncCountDownTimer
import com.fsck.k9.planck.manualsync.SyncAppState
import com.fsck.k9.planck.manualsync.SyncState
import dagger.Lazy
import foundation.pEp.jniadapter.Identity
import foundation.pEp.jniadapter.Sync.NotifyHandshakeCallback
import foundation.pEp.jniadapter.SyncHandshakeSignal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import security.planck.notification.GroupMailSignal.Companion.fromSignal
import security.planck.sync.KeySyncCleaner.Companion.queueAutoConsumeMessages
import timber.log.Timber
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

private const val POLLING_INTERVAL = 2000

@Singleton
class PlanckSyncRepository @Inject constructor(
    private val k9: K9,
    private val preferences: Preferences,
    private val planckProvider: PlanckProvider,
    private val messagingController: Provider<MessagingController>,
    private val manualSyncCountDownTimer: Lazy<ManualSyncCountDownTimer>,
    private val pollerFactory: PollerFactory,
) : SyncRepository {
    private val timer: Timer = Timer()
    private var task: MyTask? = null
    private val syncStateMutableFlow: MutableStateFlow<SyncAppState> =
        MutableStateFlow(SyncState.Idle)
    override val syncStateFlow = syncStateMutableFlow.asStateFlow()
    private val syncState: SyncAppState
        get() = syncStateMutableFlow.value
    override var isGrouped = false
    override var planckSyncEnvironmentInitialized = false
        private set
    private var isPollingMessages = false
    private var poller: Poller? = null

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
                    startOrResetManualSyncCountDownTimer()
                }
            }

            SyncHandshakeSignal.SyncNotifyInGroup -> {
                isGrouped = true
                k9.markSyncEnabled(true)
                if (syncState.allowSyncNewDevices) {
                    startOrResetManualSyncCountDownTimer()
                }
            }

            SyncHandshakeSignal.SyncNotifyBackToStart -> {
                task?.let {
                    task?.cancel()
                    task = null
                    syncStateMutableFlow.value = SyncState.AwaitingOtherDevice
                    manualSyncCountDownTimer.get().startOrReset()
                }
            }

            SyncHandshakeSignal.SyncNotifyCancelled -> {
                task?.cancel()
                task = null
                cancelManualSyncCountDown()
                syncStateMutableFlow.value = SyncState.Cancelled
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

    inner class MyTask(val handhsake: SyncState.HandshakeReadyAwaitingUser): TimerTask() {
        override fun run() {
            syncStateMutableFlow.value = handhsake
            task = null
        }
    }

    private fun foundPartnerDevice(
        myself: Identity,
        partner: Identity,
        formingGroup: Boolean,
        groupedCondition: Boolean,
    ) {
        if (syncState.allowToStartHandshake && isGrouped == groupedCondition) {
            task?.cancel()
            task = null
            cancelManualSyncCountDown()
            syncStateMutableFlow.value = SyncState.HandshakeReadyAwaitingUser(
                myself,
                partner,
                formingGroup,
                false,
            )
            scheduleHandshakeReady(myself, partner, formingGroup)
        }
    }

    private fun scheduleHandshakeReady(
        myself: Identity,
        partner: Identity,
        formingGroup: Boolean
    ) {
        task = MyTask(
            SyncState.HandshakeReadyAwaitingUser(
                myself,
                partner,
                formingGroup,
                true,
            )
        )
        timer.schedule(
            task,
            20000L
        )
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
        planckSyncEnvironmentInitialized = true
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

    private fun startOrResetManualSyncCountDownTimer() {
        manualSyncCountDownTimer.get().startOrReset()
    }

    override fun allowTimedManualSync() {
        when (planckProvider.isSyncRunning) {
            true -> planckProvider.syncReset()
            else -> planckProvider.startSync()
        }
        startOrResetManualSyncCountDownTimer()
    }

    override fun cancelSync() {
        task?.cancel()
        task = null
        cancelManualSyncCountDown()
        syncStateMutableFlow.value = SyncState.Cancelled
    }

    override fun syncStartTimeout() {
        syncStateMutableFlow.value = SyncState.SyncStartTimeout
    }

    private fun cancelManualSyncCountDown() {
        manualSyncCountDownTimer.get().cancel()
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
        if (syncState != SyncState.Idle) {
            Timber.e("unexpected initial state: $syncState")
        }
        syncStateMutableFlow.value = SyncState.AwaitingOtherDevice
        allowTimedManualSync()
    }

    override fun userDisconnected() {
        task?.cancel()
        task = null
        syncStateMutableFlow.value = SyncState.Idle
    }
}