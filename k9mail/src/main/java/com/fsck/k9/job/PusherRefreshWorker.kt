package com.fsck.k9.job

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessagingController
import security.planck.passphrase.PassphraseRepository
import timber.log.Timber


class PusherRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : Worker(appContext, params) {

    override fun doWork(): Result {
        if (!PassphraseRepository.passphraseUnlocked) {
            Timber.d("App is locked by passphrase. Skipping push refresh.")
            return Result.success()
        }
        inputData.getString(EXTRA_ACCOUNT_UUID)
            ?.let { accountUuid ->
                Timber.d("Executing periodic push refresh for account %s", accountUuid)

                val preferences = Preferences.getPreferences(applicationContext)
                val messagingController = MessagingController.getInstance(applicationContext)

                preferences.getAccount(accountUuid)?.let { account ->
                    try {
                        // Refresh pushers
                        Timber.i("Refreshing pusher for ${account.description}")
                        messagingController.getPusher(account)?.refresh()

                        // Whenever we refresh our pushers, send any unsent messages
                        Timber.d("trying to send mail in all folders!")
                        messagingController.sendPendingMessages(null)

                    } catch (e: Exception) {
                        Timber.e(e, "Exception while refreshing pushers")
                        Result.retry()
                    }
                }
            }

        return Result.success()
    }


    companion object {
        const val EXTRA_ACCOUNT_UUID = "accountUuid"
    }

}