package com.fsck.k9.notification

import com.fsck.k9.K9

internal class SummaryGroupMailNotificationDataCreator(
    private val singleMessageNotificationDataCreator: SingleGroupMailNotificationDataCreator
) {
    fun createSummaryNotificationData(data: NotificationDataGroupMail, silent: Boolean): GroupMailSummaryNotificationData {
        val timestamp = data.latestTimestamp
        val shouldBeSilent = silent || K9.isQuietTime()
        return if (data.isSingleNotification) {
            createSummarySingleNotificationData(data, timestamp, shouldBeSilent)
        } else {
            createSummaryInboxNotificationData(data, timestamp, shouldBeSilent)
        }
    }

    private fun createSummarySingleNotificationData(
        data: NotificationDataGroupMail,
        timestamp: Long,
        silent: Boolean
    ): GroupMailSummaryNotificationData {
        return singleMessageNotificationDataCreator.createSummarySingleNotificationData(data, timestamp, silent)
    }

    private fun createSummaryInboxNotificationData(
        data: NotificationDataGroupMail,
        timestamp: Long,
        silent: Boolean
    ): GroupMailSummaryNotificationData {
        return GroupMailSummaryInboxNotificationData(
            notificationId = NotificationIds.getGroupMailSummaryNotificationId(data.account),
            isSilent = silent,
            timestamp = timestamp,
            content = data.summaryContent,
            nonVisibleNotificationsCount = data.nonVisibleNotificationsCount,
        )
    }

    private val NotificationDataGroupMail.latestTimestamp: Long
        get() = activeNotifications.first().timestamp

    private val NotificationDataGroupMail.summaryContent: List<CharSequence>
        get() {
            return activeNotifications.asSequence()
                .map { it.content.groupAddress }
                .take(MAX_NUMBER_OF_GROUP_NOTIFICATIONS)
                .toList()
        }

    private val NotificationDataGroupMail.nonVisibleNotificationsCount: Int
        get() = (notificationsCount - MAX_NUMBER_OF_GROUP_NOTIFICATIONS).coerceAtLeast(0)
}