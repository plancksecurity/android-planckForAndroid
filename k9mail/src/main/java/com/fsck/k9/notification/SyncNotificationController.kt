package com.fsck.k9.notification

import android.app.Notification
import androidx.core.app.NotificationCompat
import com.fsck.k9.Account
import com.fsck.k9.mail.Folder

internal class SyncNotificationController(
    private val notificationHelper: NotificationHelper,
    private val actionBuilder: NotificationActionCreator,
    private val resourceProvider: NotificationResourceProvider
) {
    fun showSendingNotification(account: Account) {
        val accountName = account.name
        val title = resourceProvider.sendingMailTitle()
        val tickerText = resourceProvider.sendingMailBody(accountName)

        val notificationId = NotificationIds.getFetchingMailNotificationId(account)
        val outboxFolderName = account.outboxFolderName ?: error("Outbox folder not configured")
        val showMessageListPendingIntent = actionBuilder.createViewFolderPendingIntent(account, outboxFolderName)

        val notificationBuilder = notificationHelper
            .createNotificationBuilder(account, NotificationChannelManager.ChannelType.MISCELLANEOUS)
            .setSmallIcon(resourceProvider.iconSendingMail)
            .setColor(account.chipColor)
            .setWhen(System.currentTimeMillis())
            .setOngoing(true)
            .setTicker(tickerText)
            .setContentTitle(title)
            .setContentText(accountName)
            .setContentIntent(showMessageListPendingIntent)
            .setPublicVersion(createSendingLockScreenNotification(account))

        notificationHelper.notify(account, notificationId, notificationBuilder.build())
    }

    fun clearSendingNotification(account: Account) {
        val notificationId = NotificationIds.getFetchingMailNotificationId(account)
        notificationHelper.cancel(notificationId)
    }

    fun showFetchingMailNotification(account: Account, folder: Folder<*>) {
        val accountName = account.name
        val folderName = folder.name
        val tickerText = resourceProvider.checkingMailTicker(accountName, folderName)
        val title = resourceProvider.checkingMailTitle()

        // TODO: Use format string from resources
        val text = accountName + resourceProvider.checkingMailSeparator() + folderName

        val notificationId = NotificationIds.getFetchingMailNotificationId(account)
        val showMessageListPendingIntent = actionBuilder.createViewFolderPendingIntent(account, folderName)

        val notificationBuilder = notificationHelper
            .createNotificationBuilder(account, NotificationChannelManager.ChannelType.MISCELLANEOUS)
            .setSmallIcon(resourceProvider.iconCheckingMail)
            .setColor(account.chipColor)
            .setWhen(System.currentTimeMillis())
            .setOngoing(true)
            .setTicker(tickerText)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(showMessageListPendingIntent)
            .setPublicVersion(createFetchingMailLockScreenNotification(account))
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        notificationHelper.notify(account, notificationId, notificationBuilder.build())
    }

    fun clearFetchingMailNotification(account: Account) {
        val notificationId = NotificationIds.getFetchingMailNotificationId(account)
        notificationHelper.cancel(notificationId)
    }

    private fun createSendingLockScreenNotification(account: Account): Notification {
        return notificationHelper
            .createNotificationBuilder(account, NotificationChannelManager.ChannelType.MISCELLANEOUS)
            .setSmallIcon(resourceProvider.iconSendingMail)
            .setColor(account.chipColor)
            .setWhen(System.currentTimeMillis())
            .setContentTitle(resourceProvider.sendingMailTitle())
            .build()
    }

    private fun createFetchingMailLockScreenNotification(account: Account): Notification {
        return notificationHelper
            .createNotificationBuilder(account, NotificationChannelManager.ChannelType.MISCELLANEOUS)
            .setSmallIcon(resourceProvider.iconCheckingMail)
            .setColor(account.chipColor)
            .setWhen(System.currentTimeMillis())
            .setContentTitle(resourceProvider.checkingMailTitle())
            .build()
    }
}
