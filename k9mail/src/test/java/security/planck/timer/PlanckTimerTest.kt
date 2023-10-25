package security.planck.timer

import io.mockk.spyk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class PlanckTimerTest {

    private val timeout = 10L
    private val timer = PlanckTimer()
    private var taskResult = spyk(Box(0))

    @Before
    fun setUp() {
        taskResult.value = 0
    }

    @Test
    fun `timer cancel() cancels timer task`() {
        timer.startOrReset(timeout) { taskResult.value = 1 }
        timer.cancel()


        runBlocking { delay(30) }
        assertEquals(0, taskResult.value)
    }

    @Test
    fun `provided lambda is called on timer timeout`() {
        timer.startOrReset(timeout) { taskResult.value = 1 }


        runBlocking { delay(30) }
        assertEquals(1, taskResult.value)
    }

    @Test
    fun `timer startOrReset() cancels scheduled task before scheduling the new one`() {
        timer.startOrReset(timeout) { taskResult.value = 1 }
        timer.startOrReset(timeout) { taskResult.value = 2 }


        runBlocking { delay(30) }
        verify(exactly = 0) { taskResult.value = 1 }
        assertEquals(2, taskResult.value)
    }

    private data class Box(var value: Int)
}