package com.fsck.k9.extensions

import com.fsck.k9.mail.Message
import com.fsck.k9.mail.Message.RecipientType
import com.fsck.k9.mail.internet.MimeHeader
import com.fsck.k9.mailstore.LocalMessage
import com.fsck.k9.planck.PlanckUtils

fun Message.getRatingFromHeader() =
    getHeader(MimeHeader.HEADER_PEP_RATING).firstOrNull()?.let { PlanckUtils.stringToRating(it) }

fun LocalMessage?.isValidForHandshake() = this != null
        && (account?.isPlanckPrivacyProtected ?: false)
        && from != null
        && from.size == 1
        && PlanckUtils.isRatingReliable(planckRating)
        && getRecipients(RecipientType.CC).isNullOrEmpty()
        && getRecipients(RecipientType.BCC).isNullOrEmpty()
        && !getRecipients(RecipientType.TO).isNullOrEmpty()
        && !from.first().address.equals(account.email, ignoreCase = true) // sender not my own account
