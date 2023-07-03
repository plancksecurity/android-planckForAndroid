package com.fsck.k9.notification

import com.fsck.k9.Account
import security.planck.notification.GroupMailInvite

/**
 * Holds information about active and inactive new message notifications of an account.
 */
internal data class NotificationDataGroupMail(
    val account: Account,
    val activeNotifications: List<GroupMailNotificationHolder>,
    val inactiveNotifications: List<InactiveGroupMailNotificationHolder>
) {
    val notificationsCount: Int
        get() = activeNotifications.size + inactiveNotifications.size

    val isSingleNotification: Boolean
        get() = activeNotifications.size == 1

    fun isEmpty() = activeNotifications.isEmpty()

    val notificationReferences: List<GroupMailInvite>
        get() {
            return activeNotifications.map { it.content } +
                    inactiveNotifications.map { it.content }
        }

    companion object {
        fun create(account: Account): NotificationDataGroupMail {
            return NotificationDataGroupMail(account, activeNotifications = emptyList(), inactiveNotifications = emptyList())
        }
    }
}
