package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.K9

private const val MAX_NUMBER_OF_NOTIFICATIONS_FOR_SUMMARY_NOTIFICATION = 5

internal class SummaryGroupedNotificationDataCreator(
    private val singleMessageNotificationDataCreator: SingleGroupedNotificationDataCreator
) {
    fun <Reference: NotificationReference, Content: NotificationContent<Reference>> createSummaryNotificationData(
        data: NotificationData<Reference, Content>, silent: Boolean
    ): SummaryNotificationData<Reference> {
        val timestamp = data.latestTimestamp
        val shouldBeSilent = silent || K9.isQuietTime()
        return if (data.isSingleMessageNotification) {
            createSummarySingleNotificationData(data, timestamp, shouldBeSilent)
        } else {
            createSummaryInboxNotificationData(data, timestamp, shouldBeSilent)
        }
    }

    private fun <Reference: NotificationReference, Content: NotificationContent<Reference>> createSummarySingleNotificationData(
        data: NotificationData<Reference, Content>,
        timestamp: Long,
        silent: Boolean
    ): SummaryNotificationData<Reference> {
        return singleMessageNotificationDataCreator.createSummarySingleNotificationData(data, timestamp, silent)
    }

    private fun <Reference: NotificationReference, Content: NotificationContent<Reference>> createSummaryInboxNotificationData(
        data: NotificationData<Reference, Content>,
        timestamp: Long,
        silent: Boolean
    ): SummaryNotificationData<Reference> {
        val isNewMail = data.activeNotifications.first().content is NewMailNotificationContent
        val notificationId = if (isNewMail)
            NotificationIds.getNewMailSummaryNotificationId(data.account)
        else NotificationIds.getGroupMailSummaryNotificationId(data.account)
        return SummaryInboxNotificationData(
            notificationId = notificationId,
            isSilent = silent,
            timestamp = timestamp,
            content = data.summaryContent,
            nonVisibleNotificationsCount = data.additionalNotificationsCount,
            references = data.references,
            actions = if (isNewMail) createSummaryNotificationActions() else emptyList(),
            wearActions = if (isNewMail) createSummaryWearNotificationActions(data.account) else emptyList()
        )
    }

    private fun createSummaryNotificationActions(): List<SummaryNotificationAction> {
        return buildList {
            add(SummaryNotificationAction.MarkAsRead)

            if (isDeleteActionEnabled()) {
                add(SummaryNotificationAction.Delete)
            }
        }
    }

    private fun createSummaryWearNotificationActions(account: Account): List<SummaryWearNotificationAction> {
        return buildList {
            add(SummaryWearNotificationAction.MarkAsRead)

            if (isDeleteActionAvailableForWear()) {
                add(SummaryWearNotificationAction.Delete)
            }

            if (account.hasArchiveFolder()) {
                add(SummaryWearNotificationAction.Archive)
            }
        }
    }

    private fun isDeleteActionEnabled(): Boolean {
        return K9.getNotificationQuickDeleteBehaviour() == K9.NotificationQuickDelete.ALWAYS
    }

    // We don't support confirming actions on Wear devices. So don't show the action when confirmation is enabled.
    private fun isDeleteActionAvailableForWear(): Boolean {
        return isDeleteActionEnabled() && !K9.confirmDeleteFromNotification()
    }

    private val <Reference: NotificationReference, Content: NotificationContent<Reference>> NotificationData<Reference, Content>.latestTimestamp: Long
        get() = activeNotifications.first().timestamp

    private val <Reference: NotificationReference, Content: NotificationContent<Reference>> NotificationData<Reference, Content>.summaryContent: List<CharSequence>
        get() {
            return activeNotifications.asSequence()
                .map { it.content.summary }
                .take(MAX_NUMBER_OF_NOTIFICATIONS_FOR_SUMMARY_NOTIFICATION)
                .toList()
        }

    private val <Reference: NotificationReference, Content: NotificationContent<Reference>> NotificationData<Reference, Content>.additionalNotificationsCount: Int
        get() = (newMessagesCount - MAX_NUMBER_OF_NOTIFICATIONS_FOR_SUMMARY_NOTIFICATION).coerceAtLeast(0)
}
