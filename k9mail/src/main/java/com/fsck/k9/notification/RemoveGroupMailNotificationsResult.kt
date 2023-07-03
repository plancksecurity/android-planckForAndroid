package com.fsck.k9.notification

internal data class RemoveGroupMailNotificationsResult(
    val notificationData: NotificationDataGroupMail,
    val notificationStoreOperations: List<GroupMailNotificationStoreOperation>,
    val notificationHolders: List<GroupMailNotificationHolder>,
    val cancelNotificationIds: List<Int>
)
