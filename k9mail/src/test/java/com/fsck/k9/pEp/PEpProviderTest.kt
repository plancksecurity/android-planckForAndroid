package com.fsck.k9.pEp

import android.content.Context
import com.fsck.k9.pEp.infrastructure.threading.EngineThreadLocal
import com.fsck.k9.pEp.infrastructure.threading.PostExecutionThread
import foundation.pEp.jniadapter.interfaces.EngineInterface
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class PEpProviderTest {
    private val engineThreadLocal: EngineThreadLocal = mockk(relaxed = true)
    private val postExecutionThread: PostExecutionThread = mockk()
    private val context: Context = mockk()
    private val engine: EngineInterface = mockk(relaxed = true)

    private val provider: PEpProvider = PEpProviderImplKotlin(
        postExecutionThread,
        context,
        engineThreadLocal
    )

    @Before
    fun setUp() {
        every { engineThreadLocal.get() }.returns(engine)
    }

    @Test
    fun `provider calls to EngineThreadLocal close it`() {
        provider.cancelSync()


        verify { engineThreadLocal.close() }
    }
}