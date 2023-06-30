package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.activity.MessageReference

internal class NotificationRepository(
    private val notificationDataStore: NotificationDataStore = NotificationDataStore()
) {
    @Synchronized
    fun addNotification(account: Account, content: NotificationContent, timestamp: Long): AddNotificationResult? {
        initializeDataStoreIfNeeded(account)
        return notificationDataStore.addNotification(account, content, timestamp)
    }

    @Synchronized
    fun removeNotifications(
        account: Account,
        selector: (List<MessageReference>) -> List<MessageReference>
    ): RemoveNotificationsResult? {
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
