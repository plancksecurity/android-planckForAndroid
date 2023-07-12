package com.fsck.k9.notification

import androidx.core.app.NotificationCompat
import com.fsck.k9.Account
import com.fsck.k9.notification.NotificationChannelManager.ChannelType
import security.planck.notification.GroupMailInvite

internal class SummaryGroupMailNotificationCreator(
    notificationHelper: NotificationHelper,
    actionCreator: NotificationActionCreator,
    lockScreenNotificationCreator: LockScreenNotificationCreator<GroupMailInvite>,
    singleNotificationCreator: SingleGroupedNotificationCreator<GroupMailInvite>,
    resourceProvider: NotificationResourceProvider
): SummaryGroupedNotificationCreator<GroupMailInvite>(
    notificationHelper,
    actionCreator,
    lockScreenNotificationCreator,
    singleNotificationCreator,
    resourceProvider
) {

    override fun getNotificationBuilder(
        account: Account,
        notificationData: SummaryInboxNotificationData<GroupMailInvite>,
        baseNotificationData: BaseNotificationData,
        summary: String
    ): NotificationCompat.Builder {
        val title = resourceProvider.newGroupMailEventsTitle(baseNotificationData.notificationsCount)
        return notificationHelper.createNotificationBuilder(account, ChannelType.MESSAGES)
            .setCategory(NotificationCompat.CATEGORY_EMAIL)
            .setSmallIcon(resourceProvider.iconGroupMail)
            .setContentTitle(title)
            .setInboxStyle(title, summary, notificationData.content)
            .setContentIntent(actionCreator.createViewFolderPendingIntent(account, account.inboxFolderName))
            .setDeleteIntent(actionCreator.createDismissAllGroupMailNotificationsPendingIntent(account))
    }

    override fun getInboxSummaryText(accountName: String, count: Int): String {
        return resourceProvider.additionalGroupMailEvents(count, accountName)
    }
}
