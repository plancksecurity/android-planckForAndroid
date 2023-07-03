package com.fsck.k9.notification

import com.fsck.k9.Account

/**
 * Holds information about active and inactive new message notifications of an account.
 */
internal data class NotificationData<out Reference: NotificationReference, out Content: NotificationContent<Reference>>(
    val account: Account,
    val activeNotifications: List<NotificationHolder<Content>>,
    val inactiveNotifications: List<InactiveNotificationHolder<Content>>
) {
    val notificationsCount: Int
        get() = activeNotifications.size + inactiveNotifications.size

    val isSingleMessageNotification: Boolean
        get() = activeNotifications.size == 1

    val references: List<Reference>
        get() {
            return buildList(capacity = notificationsCount) {
                for (activeNotification in activeNotifications) {
                    add(activeNotification.content.reference)
                }
                for (inactiveNotification in inactiveNotifications) {
                    add(inactiveNotification.content.reference)
                }
            }
        }

    fun isEmpty() = activeNotifications.isEmpty()

    companion object {
        fun <Reference: NotificationReference, Content: NotificationContent<Reference>> create(account: Account): NotificationData<Reference, Content> {
            return NotificationData(account, activeNotifications = emptyList(), inactiveNotifications = emptyList())
        }
    }
}
