package security.planck.sync

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.fsck.k9.BuildConfig
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.PlanckProvider.CompletedCallback
import com.fsck.k9.planck.infrastructure.Poller
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
import security.planck.ui.passphrase.PassphraseActivity.Companion.notifyRequest
import security.planck.ui.passphrase.PassphraseRequirementType
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private const val PASSPHRASE_DELAY: Long = 4000L
private const val POLLING_INTERVAL = 2000

@Singleton
class SyncDelegate @Inject constructor(
    private val k9: K9,
    private val preferences: Preferences,
    private val planckProvider: PlanckProvider,
    private val manualSyncCountDownTimer: Lazy<ManualSyncCountDownTimer>,
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

    val notifyHandshakeCallback =
        NotifyHandshakeCallback { myself, partner, signal ->
            k9.showHandshakeSignalOnDebug(signal.name)
            when (signal) {
                SyncHandshakeSignal.SyncNotifyInitAddOurDevice,
                SyncHandshakeSignal.SyncNotifyInitAddOtherDevice -> {
                    if (syncState.allowSyncNewDevices) {
                        setHandshakeReadyStateAndNotify(
                            myself,
                            partner,
                            false
                        )
                        cancelManualSyncCountDown()
                    }
                }

                SyncHandshakeSignal.SyncNotifyInitFormGroup -> {
                    if (syncState.allowSyncNewDevices) {
                        setHandshakeReadyStateAndNotify(
                            myself,
                            partner,
                            true
                        )
                        cancelManualSyncCountDown()
                    }
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
                    Timber.e("Showing passphrase dialog for sync")
                    Handler(Looper.getMainLooper()).postDelayed({
                        notifyRequest(
                            k9,
                            PassphraseRequirementType.SYNC_PASSPHRASE
                        )
                    }, PASSPHRASE_DELAY)
                }

                SyncHandshakeSignal.DistributionNotifyGroupInvite -> {
                    preferences.defaultAccount?.let { account ->
                        MessagingController.getInstance(k9)
                            .notifyPlanckGroupInviteAndJoinGroup(
                                account,
                                fromSignal(myself, partner, account)
                            )
                    }
                }

                else -> {}
            }
        }

    fun setCurrentState(state: SyncAppState) {
        syncStateMutableFlow.value = state
    }

    fun setupFastPoller() {
        if (poller == null) {
            poller = Poller(Handler(Looper.getMainLooper()))
            poller!!.init(POLLING_INTERVAL.toLong()) { polling() }
        } else {
            poller!!.stopPolling()
        }
        poller!!.startPolling()
    }

    private fun polling() {
        if (syncState.needsFastPolling && !isPollingMessages) {
            Log.d("pEpDecrypt", "Entering looper")
            isPollingMessages = true
            val messagingController = MessagingController.getInstance(k9)
            messagingController.checkpEpSyncMail(k9, object : CompletedCallback {
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
        if (planckSyncEnvironmentInitialized) return
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

    private fun setHandshakeReadyStateAndNotify(
        myself: Identity,
        partner: Identity,
        formingGroup: Boolean
    ) {
        syncStateMutableFlow.value = SyncState.HandshakeReadyAwaitingUser(
            myself,
            partner,
            formingGroup
        )
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
        planckProvider.leaveDeviceGroup()
            .onSuccess { isGrouped = false }
            .onFailure {
                if (BuildConfig.DEBUG) {
                    Log.e("pEpEngine", "error calling leaveDeviceGroup", it)
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