package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.Clock
import com.fsck.k9.activity.MessageReference
import com.fsck.k9.mailstore.LocalMessage
import security.planck.notification.GroupMailInvite

/**
 * Manages notifications for new messages
 */
internal class NotificationGroupManager(
    private val contentCreator: NotificationContentCreator,
    private val newMailNotificationRepository: NotificationRepository<MessageReference, NewMailNotificationContent>,
    private val groupMailNotificationRepository: NotificationRepository<GroupMailInvite, GroupMailNotificationContent>,
    private val baseNotificationDataCreator: BaseNotificationDataCreator,
    private val singleMessageNotificationDataCreator: SingleGroupedNotificationDataCreator,
    private val summaryNotificationDataCreator: SummaryGroupedNotificationDataCreator,
    private val clock: Clock
) {

    fun addNewMailNotification(account: Account, message: LocalMessage, silent: Boolean): GroupedNotificationData<MessageReference, NewMailNotificationContent>? {
        val content = contentCreator.createFromMessage(account, message)

        val result = newMailNotificationRepository.addNotification(account, content, timestamp = now()) ?: return null

        return addNotification(account, result, silent)
    }

    fun removeNewMailNotifications(
        account: Account,
        selector: (List<MessageReference>) -> List<MessageReference>
    ): GroupedNotificationData<MessageReference, NewMailNotificationContent>? {
        val result = newMailNotificationRepository.removeNotifications(account, selector) ?: return null
        return removeNotification(account, result) { getNewMailSummaryNotificationId(account) }
    }

    fun removeGroupMailNotifications(
        account: Account,
        selector: (List<GroupMailInvite>) -> List<GroupMailInvite>
    ): GroupedNotificationData<GroupMailInvite, GroupMailNotificationContent>? {
        val result = groupMailNotificationRepository.removeNotifications(account, selector) ?: return null
        return removeNotification(account, result) { getGroupMailSummaryNotificationId(account) }
    }

    fun clearNewMailNotifications(account: Account): List<Int> {
        newMailNotificationRepository.clearNotifications(account)
        return getAllNewMailNotificationIds(account)
    }

    fun clearGroupMailNotifications(account: Account): List<Int> {
        groupMailNotificationRepository.clearNotifications(account)
        return getAllGroupMailNotificationIds(account)
    }

    private fun <Reference: NotificationReference, Content: NotificationContent<Reference>> addNotification(
        account: Account,
        result: AddNotificationResult<Reference, Content>,
        silent: Boolean
    ): GroupedNotificationData<Reference, Content> {
        val singleNotificationData = createSingleNotificationData(
            account = account,
            notificationId = result.notificationHolder.notificationId,
            content = result.notificationHolder.content,
            timestamp = result.notificationHolder.timestamp,
            addLockScreenNotification = result.notificationData.isSingleMessageNotification
        )

        return GroupedNotificationData(
            cancelNotificationIds = if (result.shouldCancelNotification) {
                listOf(result.cancelNotificationId)
            } else {
                emptyList()
            },
            baseNotificationData = createBaseNotificationData(result.notificationData),
            singleNotificationData = listOf(singleNotificationData),
            summaryNotificationData = createSummaryNotificationData(result.notificationData, silent)
        )
    }

    private fun <Reference: NotificationReference, Content: NotificationContent<Reference>> removeNotification(
        account: Account,
        result: RemoveNotificationsResult<Reference, Content>,
        notificationId: () -> Int
    ): GroupedNotificationData<Reference, Content> {
        val cancelNotificationIds = when {
            result.notificationData.isEmpty() -> {
                result.cancelNotificationIds + notificationId()
            }
            else -> {
                result.cancelNotificationIds
            }
        }

        val singleNotificationData = result.notificationHolders.map { notificationHolder ->
            createSingleNotificationData(
                account = account,
                notificationId = notificationHolder.notificationId,
                content = notificationHolder.content,
                timestamp = notificationHolder.timestamp,
                addLockScreenNotification = result.notificationData.isSingleMessageNotification
            )
        }

        return GroupedNotificationData(
            cancelNotificationIds = cancelNotificationIds,
            baseNotificationData = createBaseNotificationData(result.notificationData),
            singleNotificationData = singleNotificationData,
            summaryNotificationData = createSummaryNotificationData(result.notificationData, silent = true)
        )
    }

    private fun <Reference: NotificationReference, Content: NotificationContent<Reference>> createBaseNotificationData(
        notificationData: NotificationData<Reference, Content>
    ): BaseNotificationData {
        return baseNotificationDataCreator.createBaseNotificationData(notificationData)
    }

    private fun getNewMailSummaryNotificationId(account: Account): Int {
        return NotificationIds.getNewMailSummaryNotificationId(account)
    }

    private fun getAllNewMailNotificationIds(account: Account): List<Int> {
        return NotificationIds.getAllMessageNotificationIds(account)
    }

    private fun getGroupMailSummaryNotificationId(account: Account): Int {
        return NotificationIds.getNewMailSummaryNotificationId(account)
    }

    private fun getAllGroupMailNotificationIds(account: Account): List<Int> {
        return NotificationIds.getAllMessageNotificationIds(account)
    }

    private fun <Reference: NotificationReference, Content: NotificationContent<Reference>> createSingleNotificationData(
        account: Account,
        notificationId: Int,
        content: Content,
        timestamp: Long,
        addLockScreenNotification: Boolean
    ): SingleNotificationData<Content> {
        return singleMessageNotificationDataCreator.createSingleNotificationData(
            account,
            notificationId,
            content,
            timestamp,
            addLockScreenNotification
        )
    }

    private fun <Reference: NotificationReference, Content: NotificationContent<Reference>> createSummaryNotificationData(
        data: NotificationData<Reference, Content>, silent: Boolean
    ): SummaryNotificationData<Reference>? {
        return if (data.isEmpty()) {
            null
        } else {
            summaryNotificationDataCreator.createSummaryNotificationData(data, silent)
        }
    }

    private fun now(): Long = clock.time
}
