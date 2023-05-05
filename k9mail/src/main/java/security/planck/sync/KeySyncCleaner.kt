package security.planck.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.fsck.k9.controller.MessagingController
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit


//TODO: Cleanup and take worker infra from k9.
private const val WAIT_APP_INITIALIZATION_STEP = 100L
private const val MAX_TRIES_APP_INIT = 50

class CleanWorker(appContext: Context, workerParams: WorkerParameters)
    : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        var messagingController: MessagingController?
        messagingController = kotlin.runCatching { MessagingController.getInstance() }
            .getOrNull()
        var tries = 0
        while (messagingController == null && tries++ < MAX_TRIES_APP_INIT) {
            messagingController = kotlin.runCatching { MessagingController.getInstance() }
                .getOrNull()
            runBlocking { delay(WAIT_APP_INITIALIZATION_STEP) }
        }

        messagingController?.consumeMessages(applicationContext)
            ?: Log.e("CleanWorker", "MessagingController is null, will not do work")
        return Result.success()
    }
}

class KeySyncCleaner {
    private val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresStorageNotLow(false)
            .build()

    private fun autoConsumeRequest(): PeriodicWorkRequest {
        return PeriodicWorkRequestBuilder<CleanWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .addTag(AUTO_CONSUME_CLEAN_TAG)
                .build()
    }

    companion object {
        const val AUTO_CONSUME_CLEAN_TAG = "AutoConsumeCleanUp"

        @JvmStatic
        fun queueAutoConsumeMessages() {
            // Just have one cleanup job enqueued
            WorkManager.getInstance().enqueueUniquePeriodicWork(AUTO_CONSUME_CLEAN_TAG,
                    ExistingPeriodicWorkPolicy.KEEP,
                    KeySyncCleaner().autoConsumeRequest())
        }
    }
}
