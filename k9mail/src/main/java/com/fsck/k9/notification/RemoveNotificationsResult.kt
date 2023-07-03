package com.fsck.k9.notification

internal data class RemoveNotificationsResult<out Reference: NotificationReference, out Content: NotificationContent<Reference>>(
    val notificationData: NotificationData<out Reference, out Content>,
    val notificationHolders: List<NotificationHolder<Content>>,
    val cancelNotificationIds: List<Int>
)
