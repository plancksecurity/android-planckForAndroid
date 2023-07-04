package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.K9

private const val MAX_NUMBER_OF_NOTIFICATIONS_FOR_SUMMARY_NOTIFICATION = 5

internal class SummaryGroupedNotificationDataCreator(
    private val singleMessageNotificationDataCreator: SingleGroupedNotificationDataCreator
) {
    fun <Reference: NotificationReference> createSummaryNotificationData(
        data: NotificationData<Reference>, silent: Boolean
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
        data: NotificationData<Reference>,
        timestamp: Long,
        silent: Boolean
    ): SummaryNotificationData<Reference> {
        return singleMessageNotificationDataCreator.createSummarySingleNotificationData(data, timestamp, silent)
    }

    private fun <Reference: NotificationReference> createSummaryInboxNotificationData(
        data: NotificationData<Reference>,
        timestamp: Long,
        silent: Boolean
    ): SummaryNotificationData<Reference> {
        val groupType = data.notificationGroupType
        val isNewMail = groupType == NotificationGroupType.NEW_MAIL
        return SummaryInboxNotificationData(
            notificationId = NotificationIds.getSummaryGroupedNotificationId(data.account, groupType),
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

    private val <Reference: NotificationReference> NotificationData<Reference>.latestTimestamp: Long
        get() = activeNotifications.first().timestamp

    private val <Reference: NotificationReference> NotificationData<Reference>.summaryContent: List<CharSequence>
        get() {
            return activeNotifications.asSequence()
                .map { it.content.summary }
                .take(MAX_NUMBER_OF_NOTIFICATIONS_FOR_SUMMARY_NOTIFICATION)
                .toList()
        }

    private val <Reference: NotificationReference> NotificationData<Reference>.additionalNotificationsCount: Int
        get() = (notificationsCount - MAX_NUMBER_OF_NOTIFICATIONS_FOR_SUMMARY_NOTIFICATION).coerceAtLeast(0)
}
