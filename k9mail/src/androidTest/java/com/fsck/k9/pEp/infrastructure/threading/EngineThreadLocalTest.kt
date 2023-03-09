package com.fsck.k9.pEp.infrastructure.threading

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fsck.k9.K9
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Executors

@RunWith(AndroidJUnit4::class)
class EngineThreadLocalTest {
    private val app = ApplicationProvider.getApplicationContext<K9>()

    @Test
    fun get_returns_a_different_Engine_instance_per_thread() = runBlocking {
        val engineThreadLocal = EngineThreadLocal.getInstance(app)
        val singleThreadDispatcher1 = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        val singleThreadDispatcher2 = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        val singleThreadDispatcher3 = Executors.newSingleThreadExecutor().asCoroutineDispatcher()


        val engine1 = withContext(singleThreadDispatcher1) {
            engineThreadLocal.get()
        }.also { it.close() }
        val engine2 = withContext(singleThreadDispatcher2) {
            engineThreadLocal.get()
        }.also { it.close() }
        val engine3 = withContext(singleThreadDispatcher3) {
            engineThreadLocal.get()
        }.also { it.close() }


        assertNotEquals(engine1, engine2)
        assertNotEquals(engine1, engine3)
        assertNotEquals(engine2, engine3)
    }

    @Test
    fun get_holds_same_new_Engine_instance_per_thread() = runBlocking {
        val engineThreadLocal = EngineThreadLocal.getInstance(app)
        val singleThreadDispatcher1 = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        val singleThreadDispatcher2 = Executors.newSingleThreadExecutor().asCoroutineDispatcher()


        val engine1 = withContext(singleThreadDispatcher1) {
            engineThreadLocal.get()
        }
        val engine2 = withContext(singleThreadDispatcher2) {
            engineThreadLocal.get()
        }
        val engine1Again = withContext(singleThreadDispatcher1) {
            engineThreadLocal.get().also { it.close() }
        }
        val engine2Again = withContext(singleThreadDispatcher2) {
            engineThreadLocal.get().also { it.close() }
        }


        assertNotEquals(engine1, engine2)
        assertEquals(engine1Again, engine1)
        assertEquals(engine2Again, engine2)
    }

    @Test
    fun get_on_different_instances_returns_same_engine_instance_on_same_thread() = runBlocking {
        val engineThreadLocal1 = EngineThreadLocal.getInstance(app)
        val engineThreadLocal2 = EngineThreadLocal.getInstance(app)
        val singleThreadDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()


        val engine1 = withContext(singleThreadDispatcher) {
            engineThreadLocal1.get()
        }
        val engine2 = withContext(singleThreadDispatcher) {
            engineThreadLocal2.get().also { it.close() }
        }


        assertEquals(engineThreadLocal1, engineThreadLocal2)
        assertEquals(engine1, engine2)
    }

    @Test
    fun closed_instance_can_be_used_again_with_new_engine() = runBlocking {
        val engineThreadLocal = EngineThreadLocal.getInstance(app)
        val singleThreadDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()


        val engine1 = withContext(singleThreadDispatcher) {
            engineThreadLocal.get().also { it.close() }
        }

        val engine2 = withContext(singleThreadDispatcher) {
            engineThreadLocal.get().also { it.close() }
        }


        assertNotEquals(engine1, engine2)
    }
}