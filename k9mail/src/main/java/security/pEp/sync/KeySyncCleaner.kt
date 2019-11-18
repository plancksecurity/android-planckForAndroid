package security.pEp.sync

import android.content.Context
import androidx.work.*
import com.fsck.k9.controller.MessagingController

class CleanWorker(appContext: Context, workerParams: WorkerParameters)
    : Worker(appContext, workerParams) {

    override fun doWork(): Result {

        MessagingController.getInstance().consumeMessages(applicationContext)
        return Result.success()
    }
}

class KeySyncCleaner {

    companion object {
        private fun autoConsumeRequest(): WorkRequest {
            return OneTimeWorkRequest.Builder(CleanWorker::class.java).build()
        }

        @JvmStatic
        fun queueAutoConsumeMessages() {
            WorkManager.getInstance().enqueue(autoConsumeRequest())
        }
    }
}
