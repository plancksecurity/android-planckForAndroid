package security.pEp.sync

import android.content.Context
import androidx.work.*
import com.fsck.k9.controller.MessagingController
import java.util.concurrent.TimeUnit


//TODO: Cleanup and take worker infra from k9.

class CleanWorker(appContext: Context, workerParams: WorkerParameters)
    : Worker(appContext, workerParams) {

    override fun doWork(): Result {

        MessagingController.getInstance().consumeMessages(applicationContext)
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
