package com.fsck.k9.notification

internal class AddGroupMailNotificationResult private constructor(
    val notificationData: NotificationDataGroupMail,
    val notificationStoreOperations: List<GroupMailNotificationStoreOperation>,
    val notificationHolder: GroupMailNotificationHolder,
    val shouldCancelNotification: Boolean
) {
    val cancelNotificationId: Int
        get() {
            check(shouldCancelNotification) { "shouldCancelNotification == false" }
            return notificationHolder.notificationId
        }

    companion object {
        fun newNotification(
            notificationData: NotificationDataGroupMail,
            notificationStoreOperations: List<GroupMailNotificationStoreOperation>,
            notificationHolder: GroupMailNotificationHolder
        ): AddGroupMailNotificationResult {
            return AddGroupMailNotificationResult(
                notificationData,
                notificationStoreOperations,
                notificationHolder,
                shouldCancelNotification = false
            )
        }

        fun replaceNotification(
            notificationData: NotificationDataGroupMail,
            notificationStoreOperations: List<GroupMailNotificationStoreOperation>,
            notificationHolder: GroupMailNotificationHolder
        ): AddGroupMailNotificationResult {
            return AddGroupMailNotificationResult(
                notificationData,
                notificationStoreOperations,
                notificationHolder,
                shouldCancelNotification = true
            )
        }
    }
}
