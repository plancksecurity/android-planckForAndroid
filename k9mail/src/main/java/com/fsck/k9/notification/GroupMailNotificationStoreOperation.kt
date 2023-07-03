package com.fsck.k9.notification

import security.planck.notification.GroupMailInvite


sealed interface GroupMailNotificationStoreOperation {
    data class Add(
        val content: GroupMailInvite,
        val notificationId: Int,
        val timestamp: Long
    ) : GroupMailNotificationStoreOperation

    data class Remove(val content: GroupMailInvite) : GroupMailNotificationStoreOperation

    data class ChangeToInactive(val content: GroupMailInvite) : GroupMailNotificationStoreOperation

    data class ChangeToActive(
        val content: GroupMailInvite,
        val notificationId: Int
    ) : GroupMailNotificationStoreOperation
}
