package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.mailstore.LocalMessage

private const val FIRST_POSITION = 0

/**
 * Handle notifications for new messages.
 */
internal class GroupedNotificationController(
    private val notificationHelper: NotificationHelper,
    private val newMailNotificationManager: NotificationGroupManager,
    private val summaryNotificationCreator: SummaryGroupedNotificationCreator<>,
    private val singleNotificationCreator: SingleGroupedNotificationCreator<Reference, Content>
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
    fun addNewMailNotification(account: Account, content: Content, silent: Boolean) {
        val notificationData = newMailNotificationManager.addNewMailNotification(account, content, silent)

        if (notificationData != null) {
            processNewMailNotificationData(notificationData)
        }
    }

    @Synchronized
    fun removeNewMailNotifications(
        account: Account,
        selector: (List<Reference>) -> List<Reference>
    ) {
        val notificationData = newMailNotificationManager.removeNewMailNotifications(
            account,
            selector
        )

        if (notificationData != null) {
            processNewMailNotificationData(notificationData)
        }
    }

    @Synchronized
    fun clearNewMailNotifications(account: Account) {
        val cancelNotificationIds = newMailNotificationManager.clearNewMailNotifications(account)

        cancelNotifications(cancelNotificationIds)
    }

    private fun processNewMailNotificationData(notificationData: GroupedNotificationData<Reference, Content>) {
        cancelNotifications(notificationData.cancelNotificationIds)

        for (singleNotificationData in notificationData.singleNotificationData) {
            createSingleNotification(notificationData.baseNotificationData, singleNotificationData)
        }

        notificationData.summaryNotificationData?.let { summaryNotificationData ->
            createSummaryNotification(notificationData.baseNotificationData, summaryNotificationData)
        }
    }

    private fun cancelNotifications(notificationIds: List<Int>) {
        for (notificationId in notificationIds) {
            notificationHelper.cancel(notificationId)
        }
    }

    private fun createSingleNotification(
        baseNotificationData: BaseNotificationData,
        singleNotificationData: SingleNotificationData<Content>
    ) {
        singleNotificationCreator.createSingleNotification(baseNotificationData, singleNotificationData)
    }

    private fun createSummaryNotification(
        baseNotificationData: BaseNotificationData,
        summaryNotificationData: SummaryNotificationData<Reference>
    ) {
        summaryNotificationCreator.createSummaryNotification(baseNotificationData, summaryNotificationData)
    }
}
