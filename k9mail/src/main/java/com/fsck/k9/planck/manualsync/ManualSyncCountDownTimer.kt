package com.fsck.k9.planck.manualsync

import com.fsck.k9.planck.PlanckProvider
import security.planck.sync.SyncDelegate
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject
import javax.inject.Provider
import kotlin.concurrent.schedule

private const val TWO_MINUTE_IN_MILLIS = 120000L
private const val MANUAL_SYNC_TIME_LIMIT = TWO_MINUTE_IN_MILLIS

class ManualSyncCountDownTimer(
    private val syncDelegate: Provider<SyncDelegate>,
    private val planckProvider: Provider<PlanckProvider>,
    private val timer: Timer = Timer(),
    private val timeout: Long = MANUAL_SYNC_TIME_LIMIT,
) {
    @Inject
    constructor(
        syncDelegate: Provider<SyncDelegate>,
        planckProvider: Provider<PlanckProvider>,
    ) : this(syncDelegate, planckProvider, Timer(), MANUAL_SYNC_TIME_LIMIT)

    private var syncStartTimeoutTask: TimerTask? = null

    fun startOrReset() {
        cancel()
        start()
    }

    private fun start() {
        when (planckProvider.get().isSyncRunning) {
            true -> planckProvider.get().syncReset()
            else -> planckProvider.get().startSync()
        }
        syncStartTimeoutTask = timer.schedule(timeout) { syncDelegate.get().syncStartTimeout() }
    }

    fun cancel() {
        syncStartTimeoutTask?.cancel()
        syncStartTimeoutTask = null
    }
}