package com.fsck.k9.planck.infrastructure.threading

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import junit.framework.TestCase.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
class AutoCloseableEngineThreadAndroidTest {

    private val engine = EngineThreadLocal.getInstance(ApplicationProvider.getApplicationContext())
    var latch = CountDownLatch(1)

    @Test
    fun thread_start_will_eventually_call_EngineThreadLocal_close() {
        testWithLock {
            TestAutoCloseableEngineThread {
                engine.get().isSyncRunning
            }.start()
        }
    }

    @Test
    fun thread_start_will_eventually_call_EngineThreadLocal_close_even_if_the_task_throws_an_exception() {
        testWithLock {
            TestAutoCloseableEngineThread {
                engine.get().isSyncRunning
                error("test")
            }.start()
        }
    }

    private fun testWithLock(test: () -> Unit) {
        test()
        latch.await()
    }

    inner class TestAutoCloseableEngineThread(
        r: Runnable?
    ) : AutoCloseableEngineThread(r) {
        override fun run() {
            try {
                super.run()
            } catch (ignored: IllegalStateException) {
            } finally {
                assertTrue(engine.isEmpty())
                latch.countDown()
            }
        }
    }
}

