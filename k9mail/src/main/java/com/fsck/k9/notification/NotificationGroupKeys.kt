package com.fsck.k9.notification

import com.fsck.k9.Account

object NotificationGroupKeys {
    private const val NEW_MAIL_NOTIFICATION_GROUP_KEY_PREFIX = "newMailNotifications-"
    private const val GROUP_MAIL_NOTIFICATION_GROUP_KEY_PREFIX = "groupMailNotifications-"

    fun getNewMailGroupKey(account: Account): String {
        return NEW_MAIL_NOTIFICATION_GROUP_KEY_PREFIX + account.accountNumber
    }

    fun getGroupMailGroupKey(account: Account): String {
        return GROUP_MAIL_NOTIFICATION_GROUP_KEY_PREFIX + account.accountNumber
    }
}
