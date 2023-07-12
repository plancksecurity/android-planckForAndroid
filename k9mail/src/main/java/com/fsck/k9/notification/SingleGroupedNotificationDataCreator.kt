package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.activity.MessageReference

internal class SingleGroupedNotificationDataCreator {

    fun <Reference: NotificationReference> createSingleNotificationData(
        account: Account,
        notificationId: Int,
        content: NotificationContent<Reference>,
        timestamp: Long,
        addLockScreenNotification: Boolean
    ): SingleNotificationData<Reference> {
        val needsActions = content.reference is MessageReference
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

    fun <Reference: NotificationReference> createSummarySingleNotificationData(
        data: NotificationData<Reference>,
        timestamp: Long,
        silent: Boolean
    ): SummarySingleNotificationData<Reference> {
        val groupType = data.notificationGroupType
        val needsActions = groupType == NotificationGroupType.NEW_MAIL
        return SummarySingleNotificationData(
            SingleNotificationData(
                notificationId = NotificationIds.getSummaryGroupedNotificationId(data.account, groupType),
                isSilent = silent,
                timestamp = timestamp,
                content = data.activeNotifications.first().content,
                actions = if (needsActions) createSingleNotificationActions() else emptyList(),
                wearActions = if (needsActions) createSingleNotificationWearActions(data.account) else emptyList(),
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
