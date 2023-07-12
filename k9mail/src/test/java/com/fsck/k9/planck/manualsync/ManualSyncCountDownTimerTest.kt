package com.fsck.k9.planck.manualsync

import android.os.CountDownTimer
import com.fsck.k9.K9
import com.fsck.k9.planck.PlanckProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class ManualSyncCountDownTimerTest {
    private val k9: K9 = mockk()
    private val planckProvider: PlanckProvider = mockk(relaxed = true)
    private val countDownTimer: CountDownTimer = mockk(relaxed = true)
    private val timer = ManualSyncCountDownTimer(k9, planckProvider, countDownTimer)

    @Test
    fun `timer cancel() should call system timer cancel()`() {
        timer.cancel()

        verify { countDownTimer.cancel() }
    }

    @Test
    fun `timer startOrReset() calls system timer start()`() {
        timer.startOrReset()

        verify { countDownTimer.start() }
    }

    @Test
    fun `timer startOrReset() calls PlanckProvider startSync if sync is not running`() {
        every { planckProvider.isSyncRunning }.returns(false)

        timer.startOrReset()

        verify { planckProvider.startSync() }
    }

    @Test
    fun `timer startOrReset() calls PlanckProvider syncReset if sync is running`() {
        every { planckProvider.isSyncRunning }.returns(true)

        timer.startOrReset()

        verify { planckProvider.syncReset() }
    }
}