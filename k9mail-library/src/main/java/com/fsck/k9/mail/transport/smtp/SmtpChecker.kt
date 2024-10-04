package com.fsck.k9.mail.transport.smtp

import com.fsck.k9.mail.Transport

class SmtpChecker {
    companion object {
        // Set of SMTP hosts that automatically append sent messages to the Sent folder
        private val smtpHostsWithSentAppend = setOf(
            "smtp.gmail.com",
            "smtp.office365.com",
            "smtp-mail.outlook.com",
            "smtp.mail.yahoo.com",
            "smtp.mail.me.com",
            "smtp.fastmail.com",
            "127.0.0.1", // For ProtonMail Bridge
            "smtp.zoho.com",
            "smtp.gmx.com",
            "smtp.aol.com",
            "smtp.yandex.com",
            "smtp.mail.com"
        )

        /**
         * Checks if the given SMTP host automatically appends sent messages to the remote Sent folder.
         *
         * @param transportUri: The transport uri to check.
         * @return true if the host appends sent messages to the remote Sent folder, false otherwise.
         */
        fun doesAppendSentMessages(transportUri: String?): Boolean {
            transportUri ?: return false
            val settings = kotlin.runCatching { Transport.decodeTransportUri(transportUri) }
                .getOrElse { return false }
            val smtpHost = settings.host ?: return false
            return smtpHostsWithSentAppend.contains(smtpHost)
        }
    }
}