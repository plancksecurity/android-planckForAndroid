package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.activity.MessageReference
import security.planck.notification.GroupMailInvite

internal class GroupMailNotificationRepository(
    private val notificationDataStore: GroupMailNotificationDataStore = GroupMailNotificationDataStore()
) {
    @Synchronized
    fun addNotification(account: Account, content: GroupMailInvite, timestamp: Long): AddGroupMailNotificationResult? {
        initializeDataStoreIfNeeded(account)
        return notificationDataStore.addNotification(account, content, timestamp)
    }

    @Synchronized
    fun removeNotifications(
        account: Account,
        selector: (List<GroupMailInvite>) -> List<GroupMailInvite>
    ): RemoveGroupMailNotificationsResult? {
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
