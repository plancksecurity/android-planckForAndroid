package security.planck.serversettings

import android.content.Context
import androidx.annotation.WorkerThread
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.activity.setup.AccountSetupCheckSettings.CheckDirection
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.helper.Utility
import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.CertificateValidationException
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.planck.infrastructure.exceptions.DeviceOfflineException
import com.fsck.k9.planck.infrastructure.extensions.mapError
import timber.log.Timber

class ServerSettingsChecker(
    private val controller: MessagingController,
    private val preferences: Preferences,
) {
    private lateinit var account: Account

    @WorkerThread
    fun checkServerSettings(
        context: Context,
        account: Account,
        direction: CheckDirection,
        edit: Boolean
    ): Result<Unit> = kotlin.runCatching {
        this.account = account
        clearCertificateErrorNotifications(direction)
        checkServerSettings(direction)
        if (edit) {
            saveAccountAndRestartServices(context, account)
        }
    }.mapError { throwable ->
        when {
            throwable is AuthenticationFailedException ||
                    throwable is CertificateValidationException -> {
                throwable
            }
            throwable is MessagingException && !Utility.hasConnectivity(context) -> {
                DeviceOfflineException()
            }
            else -> {
                Timber.e(throwable, "Error while testing settings")
                throwable
            }
        }
    }

    private fun saveAccountAndRestartServices(
        context: Context,
        account: Account
    ) {
        account.save(preferences)
        K9.setServicesEnabled(context)
    }

    private fun clearCertificateErrorNotifications(
        direction: CheckDirection
    ) {
        controller.clearCertificateErrorNotifications(account, direction)
    }

    private fun checkServerSettings(direction: CheckDirection) {
        when (direction) {
            CheckDirection.INCOMING -> checkIncoming()
            CheckDirection.OUTGOING -> checkOutgoing()
        }
    }

    private fun checkOutgoing() {
        controller.checkOutgoingServerSettings(account)
    }

    private fun checkIncoming() {
        controller.checkIncomingServerSettings(account)
    }
}