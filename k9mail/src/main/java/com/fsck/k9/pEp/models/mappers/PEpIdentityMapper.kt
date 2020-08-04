package com.fsck.k9.pEp.models.mappers

import com.fsck.k9.pEp.PEpProvider
import com.fsck.k9.pEp.models.PEpIdentity

import foundation.pEp.jniadapter.Identity

import javax.inject.Inject
import javax.inject.Named

class PEpIdentityMapper @Inject constructor(@param:Named("NewInstance") private val pEpProvider: PEpProvider) {

    fun mapRecipients(recipients: List<Identity>): List<PEpIdentity> {
        return recipients.map { recipient ->
            mapRecipient(pEpProvider.updateIdentity(recipient))
        }
    }

    fun mapRecipient(recipient: Identity): PEpIdentity {
        return PEpIdentity().apply {
            address = recipient.address
            comm_type = recipient.comm_type
            flags = recipient.flags
            fpr = recipient.fpr
            lang = recipient.lang
            user_id = recipient.user_id
            username = recipient.username
            me = recipient.me
            rating = pEpProvider.getRating(recipient)
        }
    }
}
