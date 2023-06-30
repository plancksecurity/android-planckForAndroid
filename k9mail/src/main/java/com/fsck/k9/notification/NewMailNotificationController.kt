package com.fsck.k9.notification

import androidx.core.app.NotificationManagerCompat
import com.fsck.k9.Account
import com.fsck.k9.activity.MessageReference
import com.fsck.k9.mailstore.LocalMessage

private const val FIRST_POSITION = 0

/**
 * Handle notifications for new messages.
 */
internal class NewMailNotificationController(
    private val notificationManager: NotificationManagerCompat,
    private val newMailNotificationManager: NewMailNotificationManager,
    private val summaryNotificationCreator: SummaryNotificationCreator,
    private val singleMessageNotificationCreator: SingleMessageNotificationCreator
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
        val notificationData = newMailNotificationManager.addNewMailNotification(account, message, silent)

        if (notificationData != null) {
            processNewMailNotificationData(notificationData)
        }
    }

    @Synchronized
    fun removeNewMailNotifications(
        account: Account,
        selector: (List<MessageReference>) -> List<MessageReference>
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
    fun clearNewMailNotifications(account: Account, folderName: String) {
        removeNewMailNotifications(account) {
            it.filter { messageReference -> messageReference.folderName == folderName }
        }
    }

    @Synchronized
    fun removeNewMailNotification(account: Account, messageReference: MessageReference) {
        removeNewMailNotifications(account) {
            it.filter { reference -> reference == messageReference }
        }
    }

    @Synchronized
    fun clearNewMailNotifications(account: Account) {
        val cancelNotificationIds = newMailNotificationManager.clearNewMailNotifications(account)

        cancelNotifications(cancelNotificationIds)
    }

    private fun processNewMailNotificationData(notificationData: NewMailNotificationData) {
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
            notificationManager.cancel(notificationId)
        }
    }

    private fun createSingleNotification(
        baseNotificationData: BaseNotificationData,
        singleNotificationData: SingleNotificationData
    ) {
        singleMessageNotificationCreator.createSingleNotification(baseNotificationData, singleNotificationData)
    }

    private fun createSummaryNotification(
        baseNotificationData: BaseNotificationData,
        summaryNotificationData: SummaryNotificationData
    ) {
        summaryNotificationCreator.createSummaryNotification(baseNotificationData, summaryNotificationData)
    }
}
