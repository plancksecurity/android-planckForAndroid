package com.fsck.k9.service


import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.fsck.k9.K9
import com.fsck.k9.helper.K9AlarmManager
import com.fsck.k9.helper.PendingIntentCompat.FLAG_IMMUTABLE
import com.fsck.k9.job.K9JobManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import timber.log.Timber

class BootReceiver : CoreReceiver() {

    private val jobManager: K9JobManager? = K9.jobManager

    override fun receive(context: Context, intent: Intent, _tmpWakeLockId: Int?): Int? {
        var tmpWakeLockId = _tmpWakeLockId
        Timber.i("BootReceiver.onReceive %s", intent)

        val action = intent.action
        if (Intent.ACTION_BOOT_COMPLETED == action) {
            //K9.setServicesEnabled(context, tmpWakeLockId);
            //tmpWakeLockId = null;
        } else if ("com.android.sync.SYNC_CONN_STATUS_CHANGED" == action) {
            val bOps = K9.getBackgroundOps()
            if (bOps == K9.BACKGROUND_OPS.WHEN_CHECKED_AUTO_SYNC) {
                var jobManager = jobManager
                var tries = 0
                while(jobManager == null && tries++ < MAX_TRIES_APP_INIT) {
                    jobManager = K9.jobManager
                    runBlocking { delay(WAIT_APP_INITIALIZATION_STEP) }
                }

                jobManager?.scheduleAllMailJobs()
                    ?: Log.e("BootReceiver", "Job Manager is null, will not do schedule")
            }
        } else if (FIRE_INTENT == action) {
            val alarmedIntent = intent.getParcelableExtra<Intent>(ALARMED_INTENT)
            val alarmedAction = alarmedIntent?.action
            Timber.i("BootReceiver Got alarm to fire alarmedIntent %s", alarmedAction)
            alarmedIntent?.putExtra(CoreReceiver.WAKE_LOCK_ID, tmpWakeLockId)
            tmpWakeLockId = null
            context.startService(alarmedIntent)
        } else if (SCHEDULE_INTENT == action) {
            val atTime = intent.getLongExtra(AT_TIME, -1)
            val alarmedIntent = intent.getParcelableExtra<Intent>(ALARMED_INTENT)
            Timber.i("BootReceiver Scheduling intent %s for %tc", alarmedIntent, atTime)

            val pi = buildPendingIntent(context, intent)
            val alarmMgr = K9AlarmManager.getAlarmManager(context)

            alarmMgr.set(AlarmManager.RTC_WAKEUP, atTime, pi)
        }


        return tmpWakeLockId
    }

    private fun buildPendingIntent(context: Context, intent: Intent): PendingIntent {
        val alarmedIntent = intent.getParcelableExtra<Intent>(ALARMED_INTENT)
        val alarmedAction = alarmedIntent?.action

        val i = Intent(context, BootReceiver::class.java)
        i.action = FIRE_INTENT
        i.putExtra(ALARMED_INTENT, alarmedIntent)
        val uri = Uri.parse("action://" + alarmedAction!!)
        i.data = uri
        return PendingIntent.getBroadcast(context, 0, i, FLAG_IMMUTABLE)
    }

    companion object {

        const val FIRE_INTENT = "com.fsck.k9.service.BroadcastReceiver.fireIntent"
        const val SCHEDULE_INTENT = "com.fsck.k9.service.BroadcastReceiver.scheduleIntent"
        const val ALARMED_INTENT = "com.fsck.k9.service.BroadcastReceiver.pendingIntent"
        const val AT_TIME = "com.fsck.k9.service.BroadcastReceiver.atTime"
        private const val WAIT_APP_INITIALIZATION_STEP = 100L
        private const val MAX_TRIES_APP_INIT = 50

        @JvmStatic
        fun scheduleIntent(context: Context, atTime: Long, alarmedIntent: Intent) {
            Timber.i("BootReceiver Got request to schedule alarmedIntent %s", alarmedIntent.action)

            val i = Intent()
            i.setClass(context, BootReceiver::class.java)
            i.action = SCHEDULE_INTENT
            i.putExtra(ALARMED_INTENT, alarmedIntent)
            i.putExtra(AT_TIME, atTime)
            context.sendBroadcast(i)
        }

        /**
         * Cancel any scheduled alarm.
         *
         * @param context
         */
        @JvmStatic
        fun purgeSchedule(context: Context) {
            val alarmService = K9AlarmManager.getAlarmManager(context)
            alarmService.cancel(PendingIntent.getBroadcast(context, 0, object : Intent() {
                override fun filterEquals(other: Intent): Boolean {
                    // we want to match all intents
                    return true
                }
            }, FLAG_IMMUTABLE))
        }
    }

}