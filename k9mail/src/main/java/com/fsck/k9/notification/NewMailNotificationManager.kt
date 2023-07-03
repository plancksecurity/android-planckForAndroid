package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.Clock
import com.fsck.k9.activity.MessageReference

internal class NewMailNotificationManager(
    contentCreator: NotificationContentCreator,
    notificationRepository: NotificationRepository<MessageReference, NewMailNotificationContent>,
    baseNotificationDataCreator: BaseNotificationDataCreator<MessageReference, NewMailNotificationContent>,
    singleMessageNotificationDataCreator: SingleGroupedNotificationDataCreator<MessageReference, NewMailNotificationContent>,
    summaryNotificationDataCreator: SummaryGroupedNotificationDataCreator<MessageReference, NewMailNotificationContent>,
    clock: Clock
): NotificationGroupManager<MessageReference, NewMailNotificationContent>(
    contentCreator,
    notificationRepository,
    baseNotificationDataCreator,
    singleMessageNotificationDataCreator,
    summaryNotificationDataCreator,
    clock
) {
    override fun getSummaryNotificationId(account: Account): Int {
        return NotificationIds.getNewMailSummaryNotificationId(account)
    }

    override fun getAllNotificationIds(account: Account): List<Int> {
        return NotificationIds.getAllMessageNotificationIds(account)
    }
}