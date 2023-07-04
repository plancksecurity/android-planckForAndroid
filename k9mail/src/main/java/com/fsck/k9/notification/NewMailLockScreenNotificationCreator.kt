package com.fsck.k9.notification

import androidx.core.app.NotificationCompat
import com.fsck.k9.activity.MessageReference

internal class NewMailLockScreenNotificationCreator(
    notificationHelper: NotificationHelper,
    resourceProvider: NotificationResourceProvider,
): LockScreenNotificationCreator<MessageReference>(notificationHelper, resourceProvider) {
    override fun getNotificationBuilder(
        baseNotificationData: BaseNotificationData,
    ): NotificationCompat.Builder {
        val account = baseNotificationData.account
        return notificationHelper.createNotificationBuilder(account, NotificationChannelManager.ChannelType.MESSAGES)
            .setSmallIcon(resourceProvider.iconNewMail)
            .setContentTitle(resourceProvider.newMessagesTitle(baseNotificationData.notificationsCount))
            .setCategory(NotificationCompat.CATEGORY_EMAIL)
    }
}