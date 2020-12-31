package com.fsck.k9.job

import com.evernote.android.job.Job
import com.evernote.android.job.JobCreator

class K9JobCreator(
        private val mailSyncJobManager: MailSyncJobManager,
        private val pusherRefreshJobManager: PusherRefreshJobManager,
        private val awakeAppJobManager: AwakeAppJobManager
) : JobCreator {

    override fun create(tag: String): Job? {
        return when (tag) {
            MailSyncJob.TAG -> mailSyncJobManager.getJob()
            PusherRefreshJob.TAG -> pusherRefreshJobManager.getJob()
            AwakeAppJob.TAG -> awakeAppJobManager.getJob()
            else -> null
        }
    }

} 