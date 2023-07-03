package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.K9

internal class SingleGroupedNotificationDataCreator {

    fun <Reference: NotificationReference, Content: NotificationContent<Reference>> createSingleNotificationData(
        account: Account,
        notificationId: Int,
        content: Content,
        timestamp: Long,
        addLockScreenNotification: Boolean
    ): SingleNotificationData<Content> {
        val needsActions = content is NewMailNotificationContent
        return SingleNotificationData(
            notificationId = notificationId,
            isSilent = true,
            timestamp = timestamp,
            content = content,
            actions = if (needsActions) createSingleNotificationActions() else emptyList(),
            wearActions = if (needsActions) createSingleNotificationWearActions(account) else emptyList(),
            addLockScreenNotification = addLockScreenNotification
        )
    }

    fun <Reference: NotificationReference, Content: NotificationContent<Reference>> createSummarySingleNotificationData(
        data: NotificationData<Reference, Content>,
        timestamp: Long,
        silent: Boolean
    ): SummarySingleNotificationData<Reference, Content> {
        val isNewMail = data.activeNotifications.first().content is NewMailNotificationContent
        val notificationId = if (isNewMail)
            NotificationIds.getNewMailSummaryNotificationId(data.account)
        else NotificationIds.getGroupMailSummaryNotificationId(data.account)
        return SummarySingleNotificationData(
            SingleNotificationData(
                notificationId = notificationId,
                isSilent = silent,
                timestamp = timestamp,
                content = data.activeNotifications.first().content,
                actions = if (isNewMail) createSingleNotificationActions() else emptyList(),
                wearActions = if (isNewMail) createSingleNotificationWearActions(data.account) else emptyList(),
                addLockScreenNotification = false,
            ),
        )
    }

    private fun createSingleNotificationActions(): List<NotificationAction> {
        return buildList {
            add(NotificationAction.Reply)
            add(NotificationAction.MarkAsRead)

            if (isDeleteActionEnabled()) {
                add(NotificationAction.Delete)
            }
        }
    }

    private fun createSingleNotificationWearActions(account: Account): List<WearNotificationAction> {
        return buildList {
            add(WearNotificationAction.Reply)
            add(WearNotificationAction.MarkAsRead)

            if (isDeleteActionAvailableForWear()) {
                add(WearNotificationAction.Delete)
            }

            if (account.hasArchiveFolder()) {
                add(WearNotificationAction.Archive)
            }

            if (isSpamActionAvailableForWear(account)) {
                add(WearNotificationAction.Spam)
            }
        }
    }

    private fun isDeleteActionEnabled(): Boolean {
        return K9.getNotificationQuickDeleteBehaviour() != K9.NotificationQuickDelete.NEVER
    }

    // We don't support confirming actions on Wear devices. So don't show the action when confirmation is enabled.
    private fun isDeleteActionAvailableForWear(): Boolean {
        return isDeleteActionEnabled() && !K9.confirmDeleteFromNotification()
    }

    // We don't support confirming actions on Wear devices. So don't show the action when confirmation is enabled.
    private fun isSpamActionAvailableForWear(account: Account): Boolean {
        return account.hasSpamFolder() && !K9.confirmSpam()
    }
}
