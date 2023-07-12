package com.fsck.k9.notification

class TestNotificationResourceProvider : NotificationResourceProvider {
    override val iconWarning: Int = 1
    override val iconMarkAsRead: Int = 2
    override val iconDelete: Int = 3
    override val iconReply: Int = 4
    override val iconNewMail: Int = 5
    override val iconGroupMail: Int = 13
    override val iconSendingMail: Int = 6
    override val iconCheckingMail: Int = 7
    override val wearIconMarkAsRead: Int = 8
    override val wearIconDelete: Int = 9
    override val wearIconArchive: Int = 10
    override val wearIconReplyAll: Int = 11
    override val wearIconMarkAsSpam: Int = 12

    override val messagesChannelName = "Messages"
    override val messagesChannelDescription = "Notifications related to messages"
    override val miscellaneousChannelName = "Miscellaneous"
    override val miscellaneousChannelDescription = "Miscellaneous notifications like errors etc."

    override fun authenticationErrorTitle(): String = "Authentication failed"

    override fun authenticationErrorBody(accountName: String): String =
        "Authentication failed for $accountName. Update your server settings."

    override fun notifyErrorTitle(): String = "Notification error"

    override fun notifyErrorText(): String {
        return "An error has occurred while trying to create a system notification for a new message. " +
            "The reason is most likely a missing notification sound.\n" +
            "\n" +
            "Tap to open notification settings."
    }

    override fun certificateErrorTitle(): String = "Certificate error"

    override fun certificateErrorTitle(accountName: String): String = "Certificate error for $accountName"

    override fun certificateErrorBody(): String = "Check your server settings"

    override fun newMessagesTitle(newMessagesCount: Int): String = when (newMessagesCount) {
        1 -> "1 new message"
        else -> "$newMessagesCount new messages"
    }

    override fun newGroupMailEventsTitle(groupMailEventsCount: Int): String {
        return "group mail events count: $groupMailEventsCount"
    }

    override fun newMailTitle(): String {
        return "new mail"
    }

    override fun unreadMessagesCountText(unreadMessageCount: Int, accountName: String): String {
        return "$unreadMessageCount unread in $accountName"
    }

    override fun additionalMessages(overflowMessagesCount: Int, accountName: String): String =
        "+ $overflowMessagesCount more on $accountName"

    override fun additionalGroupMailEvents(
        overflowGroupMailEventsCount: Int,
        accountName: String
    ): String {
        return "+ $overflowGroupMailEventsCount more on $accountName"
    }

    override fun previewEncrypted(): String = "*Encrypted*"

    override fun noSubject(): String = "(No subject)"

    override fun recipientDisplayName(recipientDisplayName: String): String = "To:$recipientDisplayName"

    override fun noSender(): String = "No sender"

    override fun sendFailedTitle(): String = "Failed to send some messages"
    override fun sendFailedCouldNotEncryptTitle(): String {
        return "Could not encrypt title"
    }

    override fun sendFailedCouldNotEncryptText(): String {
        return "Could not encrypt text"
    }

    override fun sendingMailTitle(): String = "Sending mail"

    override fun sendingMailBody(accountName: String): String = "Sending mail: $accountName"

    override fun checkingMailTicker(accountName: String, folderName: String): String =
        "Checking mail: $accountName:$folderName"

    override fun checkingMailTitle(): String = "Checking mail"

    override fun checkingMailSeparator(): String = ":"

    override fun actionMarkAsRead(): String = "Mark Read"

    override fun actionMarkAllAsRead(): String = "Mark All Read"

    override fun actionDelete(): String = "Delete"

    override fun actionDeleteAll(): String = "Delete All"

    override fun actionReply(): String = "Reply"

    override fun actionArchive(): String = "Archive"

    override fun actionArchiveAll(): String = "Archive All"

    override fun actionMarkAsSpam(): String = "Spam"
    override fun getGroupMailInviteSubject(sender: String): String {
        return "group mail invite subject"
    }

    override fun getGroupMailInviteSummary(group: String, sender: String): CharSequence {
        return "group mail invite summary"
    }
}
