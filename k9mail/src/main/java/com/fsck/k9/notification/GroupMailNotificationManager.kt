package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.Clock
import com.fsck.k9.activity.MessageReference
import security.planck.notification.GroupMailInvite

internal class GroupMailNotificationManager(
    contentCreator: NotificationContentCreator,
    notificationRepository: NotificationRepository<GroupMailInvite, GroupMailNotificationContent>,
    baseNotificationDataCreator: BaseNotificationDataCreator<GroupMailInvite, GroupMailNotificationContent>,
    singleMessageNotificationDataCreator: SingleGroupedNotificationDataCreator<GroupMailInvite, GroupMailNotificationContent>,
    summaryNotificationDataCreator: SummaryGroupedNotificationDataCreator<GroupMailInvite, GroupMailNotificationContent>,
    clock: Clock
): NotificationGroupManager<GroupMailInvite, GroupMailNotificationContent>(
    contentCreator,
    notificationRepository,
    baseNotificationDataCreator,
    singleMessageNotificationDataCreator,
    summaryNotificationDataCreator,
    clock
) {
    override fun getSummaryNotificationId(account: Account): Int {
        return NotificationIds.getGroupMailSummaryNotificationId(account)
    }

    override fun getAllNotificationIds(account: Account): List<Int> {
        return NotificationIds.getAllGroupMailNotificationIds(account)
    }
}