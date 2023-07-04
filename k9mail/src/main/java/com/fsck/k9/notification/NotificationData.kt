package com.fsck.k9.notification

import com.fsck.k9.Account

/**
 * Holds information about active and inactive new message notifications of an account.
 */
internal data class NotificationData<out Reference: NotificationReference>(
    val account: Account,
    val activeNotifications: List<NotificationHolder<Reference>>,
    val inactiveNotifications: List<InactiveNotificationHolder<Reference>>,
    val notificationGroupType: NotificationGroupType
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
        fun <Reference: NotificationReference> create(
            account: Account, type: NotificationGroupType
        ): NotificationData<Reference> {
            return NotificationData(account, activeNotifications = emptyList(), inactiveNotifications = emptyList(), type)
        }
    }
}
