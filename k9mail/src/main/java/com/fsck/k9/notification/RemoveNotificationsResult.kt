package com.fsck.k9.notification

internal data class RemoveNotificationsResult<out Reference: NotificationReference>(
    val notificationData: NotificationData<Reference>,
    val notificationHolders: List<NotificationHolder<Reference>>,
    val cancelNotificationIds: List<Int>
)
