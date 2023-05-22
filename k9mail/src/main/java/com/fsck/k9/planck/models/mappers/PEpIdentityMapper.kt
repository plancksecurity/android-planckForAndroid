package com.fsck.k9.planck.models.mappers

import com.fsck.k9.planck.PEpProvider
import com.fsck.k9.planck.models.PEpIdentity
import foundation.pEp.jniadapter.Identity
import javax.inject.Inject

class PEpIdentityMapper @Inject constructor(private val pEpProvider: PEpProvider) {

    fun mapRecipients(recipients: List<Identity>): List<PEpIdentity> {
        return recipients.map { recipient -> mapRecipient(pEpProvider.updateIdentity(recipient)) }
    }

    private fun mapRecipient(recipient: Identity): PEpIdentity {
        return PEpIdentity().apply {
            address = recipient.address
            comm_type = recipient.comm_type
            flags = recipient.flags
            fpr = recipient.fpr
            lang = recipient.lang
            user_id = recipient.user_id
            username = recipient.username
            me = recipient.me
            rating = recipient.rating
        }
    }
}
