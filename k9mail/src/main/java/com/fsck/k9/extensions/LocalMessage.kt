package com.fsck.k9.extensions

import com.fsck.k9.Preferences
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

fun LocalMessage.isValidForHandshake(preferences: Preferences) =
    (account?.isPlanckPrivacyProtected ?: false)
            && hasSingleSenderNotMe(preferences)
            && hasSingleToRecipient()
            && PlanckUtils.isRatingReliable(planckRating)

fun LocalMessage.isValidForPartnerKeyReset(preferences: Preferences) =
    (account?.isPlanckPrivacyProtected ?: false)
            && hasSingleSenderNotMe(preferences)
            && hasSingleToRecipient()

private fun LocalMessage.hasSingleToRecipient() =
    getRecipients(Message.RecipientType.CC).isNullOrEmpty() // no recipients in CC
            && getRecipients(Message.RecipientType.BCC).isNullOrEmpty() // no recipients in BCC
            && getRecipients(Message.RecipientType.TO).size == 1 // only one recipient in TO

private fun LocalMessage.hasSingleSenderNotMe(preferences: Preferences): Boolean =
    from != null
            && from.size == 1 // only one sender
            && preferences.availableAccounts.none {
        it.email.equals(from.first().address, true)
    } // sender not one of my own accounts

private fun isMessageFullDownloaded(localMessage: LocalMessage): Boolean {
    return localMessage.isSet(Flag.X_DOWNLOADED_FULL) && !MessageExtractor.hasMissingParts(
        localMessage
    )
}