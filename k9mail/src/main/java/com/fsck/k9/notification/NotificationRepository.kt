package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.activity.MessageReference

internal class NotificationRepository<Reference: NotificationReference, Content: NotificationContent<Reference>>(
    private val notificationDataStore: NotificationDataStore<Reference, Content>
) {
    @Synchronized
    fun addNotification(account: Account, content: Content, timestamp: Long): AddNotificationResult<Reference, Content>? {
        initializeDataStoreIfNeeded(account)
        return notificationDataStore.addNotification(account, content, timestamp)
    }

    @Synchronized
    fun removeNotifications(
        account: Account,
        selector: (List<Reference>) -> List<Reference>
    ): RemoveNotificationsResult<Reference, Content>? {
        initializeDataStoreIfNeeded(account)
        return notificationDataStore.removeNotifications(account, selector)
    }

    @Synchronized
    fun clearNotifications(account: Account) {
        initializeDataStoreIfNeeded(account)
        notificationDataStore.clearNotifications(account)
    }

    private fun initializeDataStoreIfNeeded(account: Account) {
        if (!notificationDataStore.isAccountInitialized(account)) {
            notificationDataStore.initializeAccount(account, emptyList(), emptyList())
        }
    }
}
