package com.fsck.k9.notification

import androidx.core.app.NotificationCompat
import com.fsck.k9.Account
import security.planck.notification.GroupMailInvite

internal class SingleGroupMailNotificationCreator(
    notificationHelper: NotificationHelper,
    actionCreator: NotificationActionCreator,
    resourceProvider: NotificationResourceProvider,
    lockScreenNotificationCreator: LockScreenNotificationCreator<GroupMailInvite>
) : SingleGroupedNotificationCreator<GroupMailInvite>(
    notificationHelper,
    actionCreator,
    resourceProvider,
    lockScreenNotificationCreator,
) {
    override fun getNotificationBuilder(
        account: Account,
        singleNotificationData: SingleNotificationData<GroupMailInvite>
    ): NotificationCompat.Builder {
        val content = singleNotificationData.content
        return notificationHelper.createNotificationBuilder(account, NotificationChannelManager.ChannelType.MESSAGES)
            .setTicker(content.summary)
            .setCategory(NotificationCompat.CATEGORY_EMAIL)
            .setSmallIcon(resourceProvider.iconGroupMail)
            .setContentTitle(content.subject)
            .setContentText(content.summary)
            .setBigText(content.summary)
            .setContentIntent(actionCreator.createViewFolderPendingIntent(account, account.inboxFolderName))
            .setDeleteIntent(actionCreator.createDismissGroupMailNotificationPendingIntent(content.reference))
    }
}