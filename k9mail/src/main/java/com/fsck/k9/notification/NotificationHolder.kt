package com.fsck.k9.notification

internal data class NotificationHolder<out Content>(
    val notificationId: Int,
    val timestamp: Long,
    val content: Content
)

internal data class InactiveNotificationHolder<out Content>(
    val timestamp: Long,
    val content: Content
)
