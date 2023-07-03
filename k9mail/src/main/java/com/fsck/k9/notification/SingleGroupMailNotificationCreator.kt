package com.fsck.k9.notification

import androidx.core.app.NotificationCompat
import com.fsck.k9.Account
import security.planck.notification.GroupMailInvite

internal class SingleGroupMailNotificationCreator(
    notificationHelper: NotificationHelper,
    actionCreator: NotificationActionCreator,
    resourceProvider: NotificationResourceProvider,
    lockScreenNotificationCreator: LockScreenNotificationCreator
) : SingleGroupedNotificationCreator<GroupMailInvite, GroupMailNotificationContent>(
    notificationHelper,
    actionCreator,
    resourceProvider,
    lockScreenNotificationCreator,
) {
    override fun getNotificationBuilder(
        account: Account,
        singleNotificationData: SingleNotificationData<GroupMailNotificationContent>
    ): NotificationCompat.Builder {
        val content = singleNotificationData.content
        return notificationHelper.createNotificationBuilder(account, NotificationChannelManager.ChannelType.MESSAGES)
            .setTicker(content.summary)
            .setCategory(NotificationCompat.CATEGORY_EMAIL)
            .setSmallIcon(resourceProvider.iconNewMail) // change icon!!
            .setContentTitle("group mail notification")
            .setContentText("you were added to the group mail ${content.reference.groupAddress}")
            .setBigText("group mail notification")
            .setContentIntent(actionCreator.createViewFolderPendingIntent(account, account.inboxFolderName))
            .setDeleteIntent(actionCreator.createDismissGroupMailNotificationPendingIntent(content.reference))
    }
}