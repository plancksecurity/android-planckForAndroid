package com.fsck.k9.extensions

import com.fsck.k9.mail.Message
import com.fsck.k9.mail.Message.RecipientType
import com.fsck.k9.mail.internet.MimeHeader
import com.fsck.k9.mailstore.LocalMessage
import com.fsck.k9.planck.PlanckUtils

fun Message.getRatingFromHeader() =
    getHeader(MimeHeader.HEADER_PEP_RATING).firstOrNull()?.let { PlanckUtils.stringToRating(it) }
