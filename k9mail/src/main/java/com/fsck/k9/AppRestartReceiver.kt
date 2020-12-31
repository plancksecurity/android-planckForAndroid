package com.fsck.k9

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import timber.log.Timber

class AppRestartReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Timber.e("received app restart broadcast")
    }

    companion object {
        private const val APP_RESTART_DELAY = 10000

        @JvmStatic
        fun scheduleAppRestart(context: Context) {
            val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val appRestartIntent: Intent = Intent(context, AppRestartReceiver::class.java)
            val alarmIntent = PendingIntent.getBroadcast(context, 0, appRestartIntent, 0)

            alarmMgr.set(AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime() +
                            APP_RESTART_DELAY, alarmIntent)
        }
    }
}