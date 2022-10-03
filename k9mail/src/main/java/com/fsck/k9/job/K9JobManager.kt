package com.fsck.k9.job

import androidx.work.WorkManager
import com.fsck.k9.Preferences
import timber.log.Timber

class K9JobManager(
    private val workManager: WorkManager,
    private val preferences: Preferences,
    private val mailSyncWorkerManager: MailSyncWorkerManager,
    private val pusherRefreshWorkerManager: PusherRefreshWorkerManager,
) {

    fun scheduleAllMailJobs() {
        Timber.v("scheduling all jobs")
        scheduleMailSync()
        schedulePusherRefresh()
    }

    fun scheduleMailSync() {
        cancelAllMailSyncJobs()

        preferences.availableAccounts?.forEach { account ->
            mailSyncWorkerManager.scheduleMailSync(account)
        }
    }

    fun schedulePusherRefresh() {
        cancelAllPusherRefreshJobs()

        preferences.availableAccounts?.forEach { account ->
            pusherRefreshWorkerManager.scheduleJob(account)
        }
    }

    fun cancelAllMailSyncJobs() {
        Timber.v("canceling mail sync job")
        workManager.cancelAllWorkByTag(MailSyncWorkerManager.MAIL_SYNC_TAG)
    }

    fun cancelAllPusherRefreshJobs() {
        Timber.v("canceling pusher refresh job")
        workManager.cancelAllWorkByTag(PusherRefreshWorkerManager.PUSHER_REFRESH_TAG)
    }

    companion object {
        const val EXTRA_KEY_ACCOUNT_UUID = "param_key_account_uuid"
    }

}