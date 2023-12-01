package com.fsck.k9.extensions

import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.internet.MessageExtractor
import com.fsck.k9.mailstore.LocalMessage
import com.fsck.k9.message.extractors.EncryptionVerifier
import com.fsck.k9.planck.PlanckUtils

fun LocalMessage.hasToBeDecrypted(): Boolean {
    return EncryptionVerifier.isEncrypted(this) && isMessageFullDownloaded(this)
}

fun LocalMessage.isMessageIncomplete(): Boolean {
    return !isSet(Flag.X_DOWNLOADED_FULL) && !isSet(Flag.X_DOWNLOADED_PARTIAL)
}

fun LocalMessage?.isValidForHandshake() = this != null
        && (account?.isPlanckPrivacyProtected ?: false)
        && from != null
        && from.size == 1
        && PlanckUtils.isRatingReliable(planckRating)
        && getRecipients(Message.RecipientType.CC).isNullOrEmpty()
        && getRecipients(Message.RecipientType.BCC).isNullOrEmpty()
        && !getRecipients(Message.RecipientType.TO).isNullOrEmpty()
        && !from.first().address.equals(account.email, ignoreCase = true) // sender not my own account

private fun isMessageFullDownloaded(localMessage: LocalMessage): Boolean {
    return localMessage.isSet(Flag.X_DOWNLOADED_FULL) && !MessageExtractor.hasMissingParts(
        localMessage
    )
}