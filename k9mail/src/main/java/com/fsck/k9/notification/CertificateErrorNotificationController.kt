package com.fsck.k9.notification

import android.app.Notification
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import com.fsck.k9.Account

internal open class CertificateErrorNotificationController(
    private val notificationHelper: NotificationHelper,
    private val actionCreator: NotificationActionCreator,
    private val resourceProvider: NotificationResourceProvider
) {
    fun showCertificateErrorNotification(account: Account, incoming: Boolean) {
        val notificationId = NotificationIds.getCertificateErrorNotificationId(account, incoming)
        val editServerSettingsPendingIntent = createContentIntent(account, incoming)
        val title = resourceProvider.certificateErrorTitle(account.name)
        val text = resourceProvider.certificateErrorBody()

        val notificationBuilder = notificationHelper
            .createNotificationBuilder(account, NotificationChannelManager.ChannelType.MISCELLANEOUS)
            .setSmallIcon(resourceProvider.iconWarning)
            .setColor(account.chipColor)
            .setWhen(System.currentTimeMillis())
            .setAutoCancel(true)
            .setTicker(title)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(editServerSettingsPendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPublicVersion(createLockScreenNotification(account))
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setErrorAppearance()

        notificationHelper.notify(account, notificationId, notificationBuilder.build())
    }

    fun clearCertificateErrorNotifications(account: Account, incoming: Boolean) {
        val notificationId = NotificationIds.getCertificateErrorNotificationId(account, incoming)
        notificationHelper.cancel(notificationId)
    }

    protected open fun createContentIntent(account: Account, incoming: Boolean): PendingIntent {
        return actionCreator.getEditServerSettingsIntent(account, incoming)
    }

    private fun createLockScreenNotification(account: Account): Notification {
        return notificationHelper
            .createNotificationBuilder(account, NotificationChannelManager.ChannelType.MISCELLANEOUS)
            .setSmallIcon(resourceProvider.iconWarning)
            .setColor(account.chipColor)
            .setWhen(System.currentTimeMillis())
            .setContentTitle(resourceProvider.certificateErrorTitle())
            .build()
    }
}
