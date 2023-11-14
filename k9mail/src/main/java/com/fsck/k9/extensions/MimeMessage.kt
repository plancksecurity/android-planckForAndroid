package com.fsck.k9.extensions

import com.fsck.k9.mail.internet.MimeHeader
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.mail.internet.MimeMultipart

private const val MIME_TYPE_X_SMIME_SIGNATURE = "application/x-pkcs7-signature"
private const val MIME_TYPE_SMIME_SIGNATURE = "application/pkcs7-signature"
private const val MIME_TYPE_SMIME = "application/pkcs7-mime"
private const val MIME_TYPE_SMIME_10 = "application/pkcs10"

fun MimeMessage.isSmime(): Boolean =
    body is MimeMultipart && (body as MimeMultipart).bodyParts.any { part ->
        part.getHeader(MimeHeader.HEADER_CONTENT_TYPE).any {
            it.contains(MIME_TYPE_SMIME_SIGNATURE)
                    || it.contains(MIME_TYPE_X_SMIME_SIGNATURE)
                    || it.contains(MIME_TYPE_SMIME)
                    || it.contains(MIME_TYPE_SMIME_10)
        }
    }

