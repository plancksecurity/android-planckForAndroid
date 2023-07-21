package security.planck.appalive

import com.fsck.k9.preferences.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Calendar
import javax.inject.Inject

private const val APP_ALIVE_MONITOR_FREQUENCY = 3000L

class AppAliveMonitor @Inject constructor(private val storage: Storage) {

    fun getLastAppAliveMonitoredTime(): Long = runBlocking(Dispatchers.IO) {
        storage.lastAppAliveMonitoredTime
    }

    fun startAppAliveMonitor() {
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                storage.edit()
                    .setLastAppAliveMonitoredTime(Calendar.getInstance().timeInMillis / 1000)
                delay(APP_ALIVE_MONITOR_FREQUENCY)
            }
        }
    }
}