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
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

private const val POLLING_INTERVAL = 2000

@Singleton
class SyncDelegate @Inject constructor(
    private val k9: K9,
    private val preferences: Preferences,
    private val planckProvider: PlanckProvider,
    private val messagingController: Provider<MessagingController>,
    private val manualSyncCountDownTimer: Lazy<ManualSyncCountDownTimer>,
    private val pollerFactory: PollerFactory,
) {
    private val syncStateMutableFlow: MutableStateFlow<SyncAppState> =
        MutableStateFlow(SyncState.Idle)
    val syncStateFlow = syncStateMutableFlow.asStateFlow()
    private val syncState: SyncAppState
        get() = syncStateMutableFlow.value
    var isGrouped = false
    var planckSyncEnvironmentInitialized = false
        private set
    private var isPollingMessages = false
    private var poller: Poller? = null

    val notifyHandshakeCallback = NotifyHandshakeCallback { myself, partner, signal ->
        k9.showHandshakeSignalOnDebug(signal.name)
        when (signal) {
            SyncHandshakeSignal.SyncNotifyInitAddOurDevice,
            SyncHandshakeSignal.SyncNotifyInitAddOtherDevice -> {
                foundPartnerDevice(myself, partner, false)
            }

            SyncHandshakeSignal.SyncNotifyInitFormGroup -> {
                foundPartnerDevice(myself, partner, true)
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
    ) {
        if (syncState == SyncState.AwaitingOtherDevice) {
            syncStateMutableFlow.value = SyncState.HandshakeReadyAwaitingUser(
                myself,
                partner,
                formingGroup
            )
            cancelManualSyncCountDown()
        }
    }

    fun setCurrentState(state: SyncAppState) {
        syncStateMutableFlow.value = state
    }

    fun setupFastPoller() {
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
            Log.d("pEpDecrypt", "Entering looper")
            isPollingMessages = true
            messagingController.get().checkpEpSyncMail(k9, object : CompletedCallback {
                override fun onComplete() {
                    isPollingMessages = false
                }

                override fun onError(throwable: Throwable) {
                    Log.e("pEpSync", "onError: ", throwable)
                    isPollingMessages = false
                }
            })
        }
    }


    fun setPlanckSyncEnabled(enabled: Boolean) {
        if (enabled) {
            planckInitSyncEnvironment()
        } else if (isGrouped) {
            leaveDeviceGroup()
        } else {
            shutdownSync()
        }
    }

    fun planckInitSyncEnvironment() {
        planckSyncEnvironmentInitialized = true
        queueAutoConsumeMessages()
        if (preferences.accounts.isNotEmpty()) {
            if (K9.isPlanckSyncEnabled()) {
                initSync()
            }
        } else {
            Log.e("pEpEngine-app", "There is no accounts set up, not trying to start sync")
        }
    }

    private fun initSync() {
        planckProvider.updateSyncAccountsConfig()
        //updateDeviceGrouped()
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

    fun allowManualSync() {
        startOrResetManualSyncCountDownTimer()
    }

    fun cancelSync() {
        cancelManualSyncCountDown()
        syncStateMutableFlow.value = SyncState.Cancelled
    }

    fun syncStartTimeout() {
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

    fun shutdownSync() {
        Log.e("pEpEngine", "shutdownSync: start")
        if (planckProvider.isSyncRunning) {
            planckProvider.stopSync()
        }
        k9.markSyncEnabled(false)
        Log.e("pEpEngine", "shutdownSync: end")
    }
}