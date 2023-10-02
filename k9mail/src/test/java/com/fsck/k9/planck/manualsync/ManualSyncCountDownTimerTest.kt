package com.fsck.k9.planck.manualsync

import com.fsck.k9.planck.PlanckProvider
import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import security.planck.sync.SyncDelegate
import java.util.Timer
import javax.inject.Provider

class ManualSyncCountDownTimerTest {
    private val syncDelegate: SyncDelegate = mockk(relaxed = true)
    private val planckProvider: PlanckProvider = mockk(relaxed = true)
    private val syncDelegateProvider: Provider<SyncDelegate> = mockk {
        every { get() }.returns(syncDelegate)
    }
    private val planckProviderProvider: Provider<PlanckProvider> = mockk {
        every { get() }.returns(planckProvider)
    }

    private val jTimer = Timer()
    private val timeout = 0L
    private val timer =
        ManualSyncCountDownTimer(syncDelegateProvider, planckProviderProvider, jTimer, timeout)

    @Test
    fun `timer cancel() should cancel timer task`() {
        timer.startOrReset()
        timer.cancel()

        verify { syncDelegate.wasNot(called) }
    }

    @Test
    fun `SyncDelegate calls syncStartTimeout() on timer timeout`() {
        timer.startOrReset()

        verify { syncDelegate.syncStartTimeout() }
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