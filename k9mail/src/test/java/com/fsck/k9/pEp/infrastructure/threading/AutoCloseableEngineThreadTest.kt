package com.fsck.k9.pEp.infrastructure.threading

import com.fsck.k9.pEp.infrastructure.assertWithTimeout
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class AutoCloseableEngineThreadTest {

    private val engine: EngineThreadLocal = mockk(relaxed = true)

    private lateinit var thread: AutoCloseableEngineThread

    @Test
    fun `thread start will eventually call EngineThreadLocal_close()`() {
        thread = AutoCloseableEngineThread(engine) { println("running") }
        thread.start()


        assertWithTimeout(100) { verify { engine.close() } }
    }

    @Test
    fun `thread start will eventually call EngineThreadLocal_close() even if the task throws an exception`() {
        thread = AutoCloseableEngineThread(engine) { error("test") }
        thread.start()


        assertWithTimeout(100) { verify { engine.close() } }
    }
}