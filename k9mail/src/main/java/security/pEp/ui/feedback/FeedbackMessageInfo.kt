package security.pEp.ui.feedback

import com.fsck.k9.mail.Message

data class FeedbackMessageInfo(
    val subject: String,
    val from: String,
    val recipients: FeedbackMessageRecipients,
    val date: String,
) {
    companion object {
        @JvmStatic
        fun fromMessage(message: Message): FeedbackMessageInfo {
            return FeedbackMessageInfo(
                message.subject,
                message.from.joinToString(", ") { it.address },
                getRecipientsText(message),
                message.sentDate.toString()
            )
        }

        private fun getRecipientsText(message: Message): FeedbackMessageRecipients {
            val to = getRecipientsTextOfType(message, Message.RecipientType.TO)
            val cc = getRecipientsTextOfType(message, Message.RecipientType.CC)
            val bcc = getRecipientsTextOfType(message, Message.RecipientType.BCC)
            return FeedbackMessageRecipients(to, cc, bcc)
        }

        private fun getRecipientsTextOfType(message: Message, type: Message.RecipientType): String =
            message.getRecipients(type).joinToString(", ") { it.address }

    }

    data class FeedbackMessageRecipients(val to: String, val cc: String, val bcc: String)
}