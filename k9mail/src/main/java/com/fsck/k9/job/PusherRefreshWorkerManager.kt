package com.fsck.k9.job

import android.content.Context
import androidx.work.*
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessagingController
import timber.log.Timber
import java.util.concurrent.TimeUnit

class PusherRefreshWorkerManager(
    private val workManager: WorkManager,
    private val context: Context,
    private val messagingController: MessagingController,
) {

    fun scheduleJob(account: Account) {

        if (!isPushEnabled(account)) {
            return
        }

        getPushIntervalInMillisecondsIfEnabled(account)?.let { syncInterval ->

            Timber.v("scheduling pusher refresh job for ${account.description}")

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresStorageNotLow(true)
                //.setTriggerContentMaxDelay(5, TimeUnit.SECONDS)
                .build()

            val data = workDataOf(PusherRefreshWorker.EXTRA_ACCOUNT_UUID to account.uuid)
            val pusherRefreshRequest =
                PeriodicWorkRequestBuilder<PusherRefreshWorker>(syncInterval, TimeUnit.MILLISECONDS)
                    .addTag(PUSHER_REFRESH_TAG)
                    .setConstraints(constraints)
                    .setInputData(data)
                    .build()

            val uniqueWorkName = createUniqueWorkName(account.uuid)
            workManager.enqueueUniquePeriodicWork(
                uniqueWorkName,
                ExistingPeriodicWorkPolicy.UPDATE,
                pusherRefreshRequest
            )
        }

    }

    private fun getPushIntervalInMillisecondsIfEnabled(account: Account): Long? {
        val intervalMinutes = account.idleRefreshMinutes

        if (intervalMinutes <= Account.INTERVAL_MINUTES_NEVER) {
            return null
        }

        return (intervalMinutes * 60 * 1000).toLong()
    }

    private fun isPushEnabled(account: Account): Boolean {
        if (account.isEnabled && account.isAvailable(context)) {
            Timber.i("Setting up pushers for account %s", account.description)
            return messagingController.setupPushing(account)
        }

        return false
    }

    private fun createUniqueWorkName(accountUuid: String): String {
        return "$PUSHER_REFRESH_TAG:$accountUuid"
    }

    companion object {
        const val PUSHER_REFRESH_TAG: String = "PusherRefresh"
    }
}