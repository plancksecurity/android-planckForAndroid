package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.Clock
import com.fsck.k9.activity.MessageReference
import security.planck.notification.GroupMailInvite

internal class GroupMailNotificationManager(
    private val notificationRepository: GroupMailNotificationRepository,
    private val baseGroupMailNotificationDataCreator: BaseGroupMailNotificationDataCreator,
    private val summaryGroupMailDataCreator: SummaryGroupMailNotificationDataCreator,
    private val clock: Clock
) {
    fun addGroupMailInviteNotification(account: Account, content: GroupMailInvite, silent: Boolean): GroupMailNotificationData? {

        val result = notificationRepository.addNotification(account, content, timestamp = now()) ?: return null

        val singleNotificationData = createSingleNotificationData(
            account = account,
            notificationId = result.notificationHolder.notificationId,
            content = result.notificationHolder.content,
            timestamp = result.notificationHolder.timestamp,
            addLockScreenNotification = result.notificationData.isSingleNotification
        )

        return GroupMailNotificationData(
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

    fun removeNewMailNotifications(
        account: Account,
        selector: (List<GroupMailInvite>) -> List<GroupMailInvite>
    ): GroupMailNotificationData? {
        val result = notificationRepository.removeNotifications(account, selector) ?: return null

        val cancelNotificationIds = when {
            result.notificationData.isEmpty() -> {
                result.cancelNotificationIds + NotificationIds.getNewMailSummaryNotificationId(account)
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
                addLockScreenNotification = result.notificationData.isSingleNotification
            )
        }

        return GroupMailNotificationData(
            cancelNotificationIds = cancelNotificationIds,
            baseNotificationData = createBaseNotificationData(result.notificationData),
            singleNotificationData = singleNotificationData,
            summaryNotificationData = createSummaryNotificationData(result.notificationData, silent = true)
        )
    }

    fun clearNewMailNotifications(account: Account): List<Int> {
        notificationRepository.clearNotifications(account)
        return NotificationIds.getAllMessageNotificationIds(account)
    }

    private fun createBaseNotificationData(notificationData: NotificationDataGroupMail): BaseNotificationData {
        return baseGroupMailNotificationDataCreator.createBaseNotificationData(notificationData)
    }

    private fun createSingleNotificationData(
        account: Account,
        notificationId: Int,
        content: GroupMailInvite,
        timestamp: Long,
        addLockScreenNotification: Boolean
    ): GroupMailSingleNotificationData {
        return GroupMailSingleNotificationData(
            notificationId,
            true,
            timestamp,
            content,
            addLockScreenNotification
        )
    }

    private fun createSummaryNotificationData(data: NotificationDataGroupMail, silent: Boolean): GroupMailSummaryNotificationData? {
        return if (data.isEmpty()) {
            null
        } else {
            summaryGroupMailDataCreator.createSummaryNotificationData(data, silent)
        }
    }

    private fun now(): Long = clock.time
}