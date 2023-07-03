package com.fsck.k9.notification

import com.fsck.k9.Account

internal object NotificationIds {
    private const val OFFSET_SEND_FAILED_NOTIFICATION = 0
    private const val OFFSET_CERTIFICATE_ERROR_INCOMING = 1
    private const val OFFSET_CERTIFICATE_ERROR_OUTGOING = 2
    private const val OFFSET_AUTHENTICATION_ERROR_INCOMING = 3
    private const val OFFSET_AUTHENTICATION_ERROR_OUTGOING = 4
    private const val OFFSET_FETCHING_MAIL = 5
    private const val OFFSET_NEW_MAIL_SUMMARY = 6
    private const val OFFSET_NEW_MAIL_SINGLE = 7
    private const val OFFSET_GROUP_MAIL_SUMMARY = 8
    private const val OFFSET_GROUP_MAIL_SINGLE = 9
    private const val NUMBER_OF_MISC_ACCOUNT_NOTIFICATIONS = 9
    private const val NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS = MAX_NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS
    private const val NUMBER_OF_GROUP_MAIL_NOTIFICATIONS = MAX_NUMBER_OF_GROUP_NOTIFICATIONS
    private const val NUMBER_OF_NOTIFICATIONS_PER_ACCOUNT =
        NUMBER_OF_MISC_ACCOUNT_NOTIFICATIONS + NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS + NUMBER_OF_GROUP_MAIL_NOTIFICATIONS

    fun getNewMailSummaryNotificationId(account: Account): Int {
        return getBaseNotificationId(account) + OFFSET_NEW_MAIL_SUMMARY
    }

    fun getSingleMessageNotificationId(account: Account, index: Int): Int {
        require(index in 0 until NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS) { "Invalid index: $index" }

        return getBaseNotificationId(account) + OFFSET_NEW_MAIL_SINGLE + index
    }

    fun getAllMessageNotificationIds(account: Account): List<Int> {
        val singleMessageNotificationIdRange = (0 until NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS).map { index ->
            getBaseNotificationId(account) + OFFSET_NEW_MAIL_SINGLE + index
        }

        return singleMessageNotificationIdRange.toList() + getNewMailSummaryNotificationId(account)
    }

    fun getGroupMailSummaryNotificationId(account: Account): Int {
        return getBaseNotificationId(account) + OFFSET_GROUP_MAIL_SUMMARY
    }

    fun getGroupMailSingleMessageNotificationId(account: Account, index: Int): Int {
        require(index in 0 until NUMBER_OF_GROUP_MAIL_NOTIFICATIONS) { "Invalid index: $index" }

        return getBaseNotificationId(account) + OFFSET_GROUP_MAIL_SINGLE + index
    }

    fun getAllGroupMailNotificationIds(account: Account): List<Int> {
        val singleGroupMailNotificationIdRange = (0 until NUMBER_OF_GROUP_MAIL_NOTIFICATIONS).map { index ->
            getBaseNotificationId(account) + OFFSET_GROUP_MAIL_SINGLE + index
        }

        return singleGroupMailNotificationIdRange.toList() + getGroupMailSummaryNotificationId(account)
    }

    fun getFetchingMailNotificationId(account: Account): Int {
        return getBaseNotificationId(account) + OFFSET_FETCHING_MAIL
    }

    fun getSendFailedNotificationId(account: Account): Int {
        return getBaseNotificationId(account) + OFFSET_SEND_FAILED_NOTIFICATION
    }

    fun getCertificateErrorNotificationId(account: Account, incoming: Boolean): Int {
        val offset = if (incoming) OFFSET_CERTIFICATE_ERROR_INCOMING else OFFSET_CERTIFICATE_ERROR_OUTGOING

        return getBaseNotificationId(account) + offset
    }

    fun getAuthenticationErrorNotificationId(account: Account, incoming: Boolean): Int {
        val offset = if (incoming) OFFSET_AUTHENTICATION_ERROR_INCOMING else OFFSET_AUTHENTICATION_ERROR_OUTGOING

        return getBaseNotificationId(account) + offset
    }

    private fun getBaseNotificationId(account: Account): Int {
        return account.accountNumber * NUMBER_OF_NOTIFICATIONS_PER_ACCOUNT
    }
}
