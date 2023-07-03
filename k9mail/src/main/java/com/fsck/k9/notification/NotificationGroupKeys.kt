package com.fsck.k9.notification

import com.fsck.k9.Account

object NotificationGroupKeys {
    private const val NEW_MAIL_NOTIFICATION_GROUP_KEY_PREFIX = "newMailNotifications-"
    private const val GROUP_MAIL_NOTIFICATION_GROUP_KEY_PREFIX = "groupMailNotifications-"

    fun getGroupKey(account: Account, groupType: NotificationGroupType): String {
        return when(groupType) {
            NotificationGroupType.NEW_MAIL ->
                NEW_MAIL_NOTIFICATION_GROUP_KEY_PREFIX + account.accountNumber
            NotificationGroupType.GROUP_MAIL ->
                GROUP_MAIL_NOTIFICATION_GROUP_KEY_PREFIX + account.accountNumber
        }
    }
}
