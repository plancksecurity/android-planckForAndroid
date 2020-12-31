package com.fsck.k9.job

import com.evernote.android.job.JobRequest

class AwakeAppJobManager {

    fun getJob() = AwakeAppJob()

    fun scheduleJob() {
        val jobRequest = JobRequest.Builder(MailSyncJob.TAG)
                .setExact(DELAY_MILLISECONDS)
                .build()

        jobRequest.schedule()
    }

    companion object {
        private const val DELAY_MILLISECONDS = 10000L
    }
}