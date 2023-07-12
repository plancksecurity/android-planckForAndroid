package com.fsck.k9.planck.manualsync

import android.os.CountDownTimer
import com.fsck.k9.K9
import com.fsck.k9.planck.PlanckProvider

private const val ONE_MINUTE_IN_MILLIS = 60000L
private const val MANUAL_SYNC_TIME_LIMIT = ONE_MINUTE_IN_MILLIS
private const val MANUAL_SYNC_CHECK_INTERVAL: Long = 200

class ManualSyncCountDownTimer
@JvmOverloads
constructor(
    private val k9: K9,
    private val planckProvider: PlanckProvider,
    private var manualSyncCountDownTimer: CountDownTimer = getCountDownTimer(k9)
) {
    fun startOrReset() {
        cancel()
        when (planckProvider.isSyncRunning) {
            true -> planckProvider.syncReset()
            else -> planckProvider.startSync()
        }

        manualSyncCountDownTimer.start()
    }

    fun cancel() {
        manualSyncCountDownTimer.cancel()
    }

    companion object {
        fun getCountDownTimer(k9: K9): CountDownTimer {
            return object : CountDownTimer(
                MANUAL_SYNC_TIME_LIMIT,
                MANUAL_SYNC_CHECK_INTERVAL
            ) {
                override fun onTick(millisUntilFinished: Long) {

                }

                override fun onFinish() {
                    k9.disallowSync()
                }
            }
        }

    }
}