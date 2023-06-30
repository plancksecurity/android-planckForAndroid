package com.fsck.k9.notification

import android.app.Notification
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import com.fsck.k9.Account
import com.fsck.k9.activity.MessageReference
import com.fsck.k9.helper.ExceptionHelper
import com.fsck.k9.mail.Flag
import com.fsck.k9.planck.infrastructure.exceptions.AppDidntEncryptMessageException

internal class SendFailedNotificationController(
    private val notificationHelper: NotificationHelper,
    private val actionBuilder: NotificationActionCreator,
    private val resourceProvider: NotificationResourceProvider
) {
    fun showSendFailedNotification(account: Account, exception: Exception) {
        val title: String
        val text: String

        val notificationId = NotificationIds.getSendFailedNotificationId(account)

        val pendingIntent: PendingIntent
        if (exception is AppDidntEncryptMessageException) {
            title = resourceProvider.sendFailedCouldNotEncryptTitle()
            text = resourceProvider.sendFailedCouldNotEncryptText()
            val messageReference = MessageReference(
                account.uuid,
                account.draftsFolderName,
                exception.mimeMessage.uid,
                Flag.X_PEP_WASNT_ENCRYPTED
            )
            pendingIntent =
                actionBuilder.createMessageComposePendingIntent(messageReference)
        } else {
            title = resourceProvider.sendFailedTitle()
            text = ExceptionHelper.getRootCauseMessage(exception)
            pendingIntent = actionBuilder.createViewFolderPendingIntent(
                account,
                account.outboxFolderName
            )
        }

        val notificationBuilder = notificationHelper
            .createNotificationBuilder(account, NotificationChannelManager.ChannelType.MISCELLANEOUS)
            .setSmallIcon(resourceProvider.iconWarning)
            .setColor(account.chipColor)
            .setWhen(System.currentTimeMillis())
            .setAutoCancel(true)
            .setTicker(title)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPublicVersion(createLockScreenNotification(account))
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setErrorAppearance()

        notificationHelper.notify(account, notificationId, notificationBuilder.build())
    }

    fun clearSendFailedNotification(account: Account) {
        val notificationId = NotificationIds.getSendFailedNotificationId(account)
        notificationHelper.cancel(notificationId)
    }

    private fun createLockScreenNotification(account: Account): Notification {
        return notificationHelper
            .createNotificationBuilder(account, NotificationChannelManager.ChannelType.MISCELLANEOUS)
            .setSmallIcon(resourceProvider.iconWarning)
            .setColor(account.chipColor)
            .setWhen(System.currentTimeMillis())
            .setContentTitle(resourceProvider.sendFailedTitle())
            .build()
    }
}
