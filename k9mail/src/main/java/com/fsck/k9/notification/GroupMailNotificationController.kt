package com.fsck.k9.notification

import com.fsck.k9.Account
import security.planck.notification.GroupMailInvite

internal class GroupMailNotificationController(
    private val notificationHelper: NotificationHelper,
    private val groupMailNotificationManager: GroupMailNotificationManager,
    private val singleNotificationCreator: SingleGroupMailNotificationCreator,
    private val summaryNotificationCreator: SummaryGroupMailNotificationCreator,
) {
    @Synchronized
    fun addGroupMailInviteNotification(account: Account, groupInvite: GroupMailInvite, silent: Boolean) {
        val notificationData = groupMailNotificationManager.addGroupMailInviteNotification(account, groupInvite, silent)

        if (notificationData != null) {
            processNotificationData(notificationData)
        }
    }

    @Synchronized
    fun removeGroupMailNotifications(
        account: Account,
        selector: (List<GroupMailInvite>) -> List<GroupMailInvite>
    ) {
        val notificationData = groupMailNotificationManager.removeNewMailNotifications(
            account,
            selector
        )

        if (notificationData != null) {
            processNotificationData(notificationData)
        }
    }

    @Synchronized
    fun removeGroupMailNotification(account: Account, groupInvite: GroupMailInvite) {
        removeGroupMailNotifications(account) {
            it.filter { reference -> reference == groupInvite }
        }
    }

    @Synchronized
    fun clearGroupMailNotifications(account: Account) {
        val cancelNotificationIds = groupMailNotificationManager.clearNewMailNotifications(account)

        cancelNotifications(cancelNotificationIds)
    }

    private fun processNotificationData(notificationData: GroupMailNotificationData) {
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
        singleNotificationData: GroupMailSingleNotificationData
    ) {
        singleNotificationCreator.createSingleNotification(baseNotificationData, singleNotificationData)
    }

    private fun createSummaryNotification(
        baseNotificationData: BaseNotificationData,
        summaryNotificationData: GroupMailSummaryNotificationData
    ) {
        summaryNotificationCreator.createSummaryNotification(baseNotificationData, summaryNotificationData)
    }
}