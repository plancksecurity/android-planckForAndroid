package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.activity.MessageReference
import security.planck.notification.GroupMailInvite

internal class GroupMailNotificationDataStore:
    NotificationDataStore<GroupMailInvite, GroupMailNotificationContent>(
    ) {
    override fun getNewNotificationId(account: Account, index: Int): Int {
        return NotificationIds.getSingleGroupMailNotificationId(account, index)
    }
}
