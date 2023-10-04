package com.fsck.k9.planck.manualsync

import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import security.planck.sync.SyncRepository
import java.util.Timer
import javax.inject.Provider

class ManualSyncCountDownTimerTest {
    private val syncRepository: SyncRepository = mockk(relaxed = true)
    private val syncRepositoryProvider: Provider<SyncRepository> = mockk {
        every { get() }.returns(syncRepository)
    }

    private val jTimer = Timer()
    private val timeout = 0L
    private val timer =
        ManualSyncCountDownTimer(syncRepositoryProvider, jTimer, timeout)

    @Test
    fun `timer cancel() should cancel timer task`() {
        timer.startOrReset()
        timer.cancel()

        verify { syncRepository.wasNot(called) }
    }

    @Test
    fun `SyncDelegate calls syncStartTimeout() on timer timeout`() {
        timer.startOrReset()

        verify { syncRepository.syncStartTimeout() }
    }
}