package com.fsck.k9.notification

internal data class NotificationContent<out Reference: NotificationReference>(
    val sender: String,
    val subject: String,
    val summary: CharSequence,
    val reference: Reference,
    val preview: CharSequence = summary,
)

internal interface NotificationReference


