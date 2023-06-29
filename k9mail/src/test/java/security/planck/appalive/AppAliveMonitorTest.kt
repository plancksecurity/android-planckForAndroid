package security.planck.appalive

import com.fsck.k9.preferences.Storage
import com.fsck.k9.preferences.StorageEditor
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class AppAliveMonitorTest {
    private val storage: Storage = mockk()
    private val storageEditor: StorageEditor = mockk(relaxed = true)
    private val appAliveMonitor = AppAliveMonitor(storage)
    private val mockCalendar: Calendar = mockk()

    @Before
    fun setUp() {
        every { storage.edit() }.returns(storageEditor)
        every { mockCalendar.timeInMillis }.returns(WRITE_TIME * 1000)
        mockkStatic(Calendar::class)
        every { Calendar.getInstance() }.returns(mockCalendar)
    }

    @After
    fun tearDown() {
        unmockkStatic(Calendar::class)
    }

    @Test
    fun `getLastAppAliveMonitoredTime retrieves time from Storage`() {
        every { storage.lastAppAliveMonitoredTime }.returns(555)
        val time = appAliveMonitor.getLastAppAliveMonitoredTime()


        verify { storage.lastAppAliveMonitoredTime }
        assertEquals(555, time)
    }

    @Test
    fun `startAppAliveMonitor saves last known app alive time every few seconds`() {
        var timesWritten = 0
        val timesTested = 1 // set to one so the tests run faster, change the value for a longer check
        val timeout = 10000
        val initialTime = System.currentTimeMillis()


        appAliveMonitor.startAppAliveMonitor()


        var error: Throwable? = null
        while (timesWritten < timesTested && System.currentTimeMillis() - initialTime < timeout) {
            kotlin.runCatching {
                verify(exactly = timesWritten + 1) {
                    storage.edit()
                    storageEditor.setLastAppAliveMonitoredTime(WRITE_TIME)
                }
                timesWritten ++
                if (timesWritten == timesTested) return
            }.onFailure {
                error = it
                println(error)
            }
            runBlocking { delay(500) }
        }
        error?.let { throw it }
    }

    companion object {
        private const val WRITE_TIME = 555L
    }
}