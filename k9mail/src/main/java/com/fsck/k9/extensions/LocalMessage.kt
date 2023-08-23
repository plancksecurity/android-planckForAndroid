package com.fsck.k9.extensions

import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.internet.MessageExtractor
import com.fsck.k9.mailstore.LocalMessage
import com.fsck.k9.message.extractors.EncryptionVerifier

fun LocalMessage.hasToBeDecrypted(): Boolean {
    return EncryptionVerifier.isEncrypted(this) && isMessageFullDownloaded(this)
}

fun LocalMessage.isMessageIncomplete(): Boolean {
    return !isSet(Flag.X_DOWNLOADED_FULL) && !isSet(Flag.X_DOWNLOADED_PARTIAL)
}

private fun isMessageFullDownloaded(localMessage: LocalMessage): Boolean {
    return localMessage.isSet(Flag.X_DOWNLOADED_FULL) && !MessageExtractor.hasMissingParts(
        localMessage
    )
}