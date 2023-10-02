package com.fsck.k9.planck.manualsync

import android.os.CountDownTimer
import com.fsck.k9.planck.PlanckProvider
import security.planck.sync.SyncDelegate
import javax.inject.Inject
import javax.inject.Provider

private const val TWO_MINUTE_IN_MILLIS = 120000L
private const val MANUAL_SYNC_TIME_LIMIT = TWO_MINUTE_IN_MILLIS
private const val MANUAL_SYNC_CHECK_INTERVAL: Long = 200

class ManualSyncCountDownTimer
@Inject constructor(
    private val syncDelegate: Provider<SyncDelegate>,
    private val planckProvider: Provider<PlanckProvider>,
) {
    private val countDownTimer: CountDownTimer = object : CountDownTimer(
        MANUAL_SYNC_TIME_LIMIT,
        MANUAL_SYNC_CHECK_INTERVAL
    ) {
        override fun onTick(millisUntilFinished: Long) {

        }

        override fun onFinish() {
            syncDelegate.get().syncStartTimeout()
        }
    }

    fun startOrReset() {
        cancel()
        when (planckProvider.get().isSyncRunning) {
            true -> planckProvider.get().syncReset()
            else -> planckProvider.get().startSync()
        }

        countDownTimer.start()
    }

    fun cancel() {
        countDownTimer.cancel()
    }
}