package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.activity.MessageReference

internal class NewMailNotificationDataStore:
    NotificationDataStore<MessageReference, NewMailNotificationContent>(
) {
    override fun getNewNotificationId(account: Account, index: Int): Int {
        return NotificationIds.getSingleMessageNotificationId(account, index)
    }
}