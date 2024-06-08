package com.fsck.k9.job

import android.content.ContentResolver
import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessagingController
import security.planck.passphrase.PassphraseRepository
import timber.log.Timber

class MailSyncWorker(
    context: Context,
    parameters: WorkerParameters
) : Worker(context, parameters) {

    override fun doWork(): Result {
        if (!PassphraseRepository.passphraseUnlocked) {
            Timber.d("App is locked by passphrase. Skipping mail sync.")
            return Result.success()
        }
        val accountUuid = inputData.getString(EXTRA_ACCOUNT_UUID)
        requireNotNull(accountUuid)

        Timber.d("Executing periodic mail sync for account %s", accountUuid)

        if (isBackgroundSyncDisabled()) {
            Timber.d("Background sync is disabled. Skipping mail sync.")
            return Result.success()
        }

        val preferences = Preferences.getPreferences(applicationContext)
        val messagingController = MessagingController.getInstance(applicationContext)

        val account = preferences.getAccount(accountUuid)
        if (account == null) {
            Timber.e("Account %s not found. Can't perform mail sync.", accountUuid)
            return Result.failure()
        }

        if (account.isPeriodicMailSyncDisabled) {
            Timber.d("Periodic mail sync has been disabled for this account. Skipping mail sync.")
            return Result.success()
        }

        messagingController.performPeriodicMailSync(account)

        return Result.success()
    }

    private fun isBackgroundSyncDisabled(): Boolean {
        val backgroundOps = K9.getBackgroundOps() ?: return false
        return when (backgroundOps) {
            K9.BACKGROUND_OPS.NEVER -> true
            K9.BACKGROUND_OPS.ALWAYS -> false
            K9.BACKGROUND_OPS.WHEN_CHECKED_AUTO_SYNC -> !ContentResolver.getMasterSyncAutomatically()
        }
    }

    private val Account.isPeriodicMailSyncDisabled
        get() = automaticCheckIntervalMinutes <= 0

    companion object {
        const val EXTRA_ACCOUNT_UUID = "accountUuid"
    }
}
