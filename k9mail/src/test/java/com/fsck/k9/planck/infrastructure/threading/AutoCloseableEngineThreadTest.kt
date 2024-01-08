package com.fsck.k9.planck.infrastructure.threading

import com.fsck.k9.Globals
import com.fsck.k9.K9
import com.fsck.k9.planck.infrastructure.assertWithTimeout
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class AutoCloseableEngineThreadTest {

    private val k9: K9 = mockk()
    private val engine: EngineThreadLocal = mockk(relaxed = true)
    private lateinit var thread: AutoCloseableEngineThread

    @Before
    fun setUp() {
        mockkStatic(Globals::class)
        every { Globals.getContext() }.returns(k9)
        mockkObject(EngineThreadLocal.Companion)
        every { EngineThreadLocal.Companion.getInstance(k9) }.returns(engine)
    }

    @After
    fun tearDown() {
        unmockkStatic(Globals::class)
        unmockkObject(EngineThreadLocal.Companion)
    }

    @Test
    fun `thread start will eventually call EngineThreadLocal_close()`() {
        thread = AutoCloseableEngineThread { println("running") }
        thread.start()


        assertWithTimeout(100) { verify { engine.close() } }
    }

    @Test
    fun `thread start will eventually call EngineThreadLocal_close() even if the task throws an exception`() {
        thread = AutoCloseableEngineThread { error("test") }
        thread.start()


        assertWithTimeout(100) { verify { engine.close() } }
    }
}