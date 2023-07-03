package com.fsck.k9.notification

import security.planck.notification.GroupMailInvite

internal data class GroupMailNotificationHolder(
    val notificationId: Int,
    val timestamp: Long,
    val content: GroupMailInvite
)

internal data class InactiveGroupMailNotificationHolder(
    val timestamp: Long,
    val content: GroupMailInvite
)
