package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.activity.MessageReference
import com.fsck.k9.mailstore.LocalMessage
import com.fsck.k9.planck.infrastructure.FIRST_POSITION
import security.planck.notification.GroupMailInvite
import security.planck.notification.GroupMailSignal

/**
 * Handle notifications for notification groups. Current notification groups:
 * - new message notifications
 * - group mail notifications
 */
internal class GroupedNotificationController(
    private val notificationHelper: NotificationHelper,
    private val groupedNotificationManager: GroupedNotificationManager,
    private val newMailSummaryNotificationCreator: SummaryGroupedNotificationCreator<MessageReference>,
    private val newMailSingleNotificationCreator: SingleGroupedNotificationCreator<MessageReference>,
    private val groupMailSummaryNotificationCreator: SummaryGroupedNotificationCreator<GroupMailInvite>,
    private val groupMailSingleNotificationCreator: SingleGroupedNotificationCreator<GroupMailInvite>,
) {

    @Synchronized
    fun addNewMailsNotification(
        account: Account,
        messages: List<LocalMessage>
    ) {
        for (position in messages.indices) {
            val message = messages[position]
            addNewMailNotification(
                account,
                message,
                position != FIRST_POSITION
            )
        }
    }

    @Synchronized
    fun addNewMailNotification(account: Account, message: LocalMessage, silent: Boolean) {
        groupedNotificationManager.addNewMailNotification(account, message, silent)?.let {
            processNewMailNotificationData(it)
        }
    }

    @Synchronized
    fun addGroupMailNotification(account: Account, groupMailSignal: GroupMailSignal, silent: Boolean) {
        groupedNotificationManager.addGroupMailNotification(account, groupMailSignal, silent)?.let {
            processGroupMailNotificationData(it)
        }
    }

    @Synchronized
    fun removeNewMailNotifications(
        account: Account,
        selector: (List<MessageReference>) -> List<MessageReference>
    ) {
        val notificationData = groupedNotificationManager.removeNewMailNotifications(
            account,
            selector
        )

        if (notificationData != null) {
            processNewMailNotificationData(notificationData)
        }
    }

    @Synchronized
    fun removeGroupMailNotifications(
        account: Account,
        selector: (List<GroupMailInvite>) -> List<GroupMailInvite>
    ) {
        val notificationData = groupedNotificationManager.removeGroupMailNotifications(
            account,
            selector
        )

        if (notificationData != null) {
            processGroupMailNotificationData(notificationData)
        }
    }

    @Synchronized
    fun clearNewMailNotifications(account: Account) {
        val cancelNotificationIds = groupedNotificationManager.clearNewMailNotifications(account)

        cancelNotifications(cancelNotificationIds)
    }

    @Synchronized
    fun clearGroupMailNotifications(account: Account) {
        val cancelNotificationIds = groupedNotificationManager.clearGroupMailNotifications(account)

        cancelNotifications(cancelNotificationIds)
    }

    private fun  processNewMailNotificationData(
        notificationData: GroupedNotificationData<MessageReference>
    ) {
        cancelNotifications(notificationData.cancelNotificationIds)

        for (singleNotificationData in notificationData.singleNotificationData) {
            createSingleNewMailNotification(notificationData.baseNotificationData, singleNotificationData)
        }

        notificationData.summaryNotificationData?.let { summaryNotificationData ->
            createNewMailSummaryNotification(notificationData.baseNotificationData, summaryNotificationData)
        }
    }

    private fun  processGroupMailNotificationData(
        notificationData: GroupedNotificationData<GroupMailInvite>
    ) {
        cancelNotifications(notificationData.cancelNotificationIds)

        for (singleNotificationData in notificationData.singleNotificationData) {
            createSingleGroupMailNotification(notificationData.baseNotificationData, singleNotificationData)
        }

        notificationData.summaryNotificationData?.let { summaryNotificationData ->
            createGroupMailSummaryNotification(notificationData.baseNotificationData, summaryNotificationData)
        }
    }

    private fun cancelNotifications(notificationIds: List<Int>) {
        for (notificationId in notificationIds) {
            notificationHelper.cancel(notificationId)
        }
    }

    private fun createSingleNewMailNotification(
        baseNotificationData: BaseNotificationData,
        singleNotificationData: SingleNotificationData<MessageReference>
    ) {
        newMailSingleNotificationCreator.createSingleNotification(baseNotificationData, singleNotificationData)
    }

    private fun createNewMailSummaryNotification(
        baseNotificationData: BaseNotificationData,
        summaryNotificationData: SummaryNotificationData<MessageReference>
    ) {
        newMailSummaryNotificationCreator.createSummaryNotification(baseNotificationData, summaryNotificationData)
    }

    private fun createSingleGroupMailNotification(
        baseNotificationData: BaseNotificationData,
        singleNotificationData: SingleNotificationData<GroupMailInvite>
    ) {
        groupMailSingleNotificationCreator.createSingleNotification(baseNotificationData, singleNotificationData)
    }

    private fun createGroupMailSummaryNotification(
        baseNotificationData: BaseNotificationData,
        summaryNotificationData: SummaryNotificationData<GroupMailInvite>
    ) {
        groupMailSummaryNotificationCreator.createSummaryNotification(baseNotificationData, summaryNotificationData)
    }
}
