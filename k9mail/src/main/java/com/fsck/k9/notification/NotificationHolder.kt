package com.fsck.k9.notification

internal data class NotificationHolder<out Reference: NotificationReference>(
    val notificationId: Int,
    val timestamp: Long,
    val content: NotificationContent<Reference>
)

internal data class InactiveNotificationHolder<out Reference: NotificationReference>(
    val timestamp: Long,
    val content: NotificationContent<Reference>
)
