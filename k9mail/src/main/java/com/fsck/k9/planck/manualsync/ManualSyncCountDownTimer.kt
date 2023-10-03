package com.fsck.k9.planck.manualsync

import com.fsck.k9.planck.PlanckProvider
import security.planck.sync.SyncRepository
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject
import javax.inject.Provider
import kotlin.concurrent.schedule

private const val TWO_MINUTE_IN_MILLIS = 120000L
private const val MANUAL_SYNC_TIME_LIMIT = TWO_MINUTE_IN_MILLIS

class ManualSyncCountDownTimer(
    private val syncRepository: Provider<SyncRepository>,
    private val planckProvider: Provider<PlanckProvider>,
    private val timer: Timer = Timer(),
    private val timeout: Long = MANUAL_SYNC_TIME_LIMIT,
) {
    @Inject
    @Suppress("unused")
    constructor(
        syncRepository: Provider<SyncRepository>,
        planckProvider: Provider<PlanckProvider>,
    ) : this(syncRepository, planckProvider, Timer(), MANUAL_SYNC_TIME_LIMIT)

    private var syncStartTimeoutTask: TimerTask? = null

    fun startOrReset() {
        cancel()
        start()
    }

    private fun start() {
        syncStartTimeoutTask = timer.schedule(timeout) { syncRepository.get().syncStartTimeout() }
    }

    fun cancel() {
        syncStartTimeoutTask?.cancel()
        syncStartTimeoutTask = null
    }
}