package com.fsck.k9.job

import com.evernote.android.job.Job


class AwakeAppJob : Job() {

    override fun onRunJob(params: Params): Result {
        return Result.SUCCESS
    }

    companion object {
        const val TAG: String = "AwakeAppJob"
    }

}