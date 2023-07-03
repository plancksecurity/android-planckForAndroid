package com.fsck.k9.notification

import com.fsck.k9.activity.MessageReference

internal data class NotificationContent constructor(
    val messageReference: MessageReference,
    val sender: String,
    val subject: String,
    val preview: CharSequence,
    val summary: CharSequence
)
