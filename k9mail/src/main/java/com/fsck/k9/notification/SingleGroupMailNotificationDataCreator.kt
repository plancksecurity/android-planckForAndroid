package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.K9
import security.planck.notification.GroupMailInvite

internal class SingleGroupMailNotificationDataCreator {

    fun createSingleNotificationData(
        account: Account,
        notificationId: Int,
        content: GroupMailInvite,
        timestamp: Long,
        addLockScreenNotification: Boolean
    ): GroupMailSingleNotificationData {
        return GroupMailSingleNotificationData(
            notificationId = notificationId,
            isSilent = true,
            timestamp = timestamp,
            content = content,
            addLockScreenNotification = addLockScreenNotification
        )
    }

    fun createSummarySingleNotificationData(
        data: NotificationDataGroupMail,
        timestamp: Long,
        silent: Boolean
    ): GroupMailSummarySingleNotificationData {
        return GroupMailSummarySingleNotificationData(
            GroupMailSingleNotificationData(
                notificationId = NotificationIds.getNewMailSummaryNotificationId(data.account),
                isSilent = silent,
                timestamp = timestamp,
                content = data.activeNotifications.first().content,
                addLockScreenNotification = false,
            ),
        )
    }
}
