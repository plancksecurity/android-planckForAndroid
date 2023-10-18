package com.fsck.k9.planck.models.mappers

import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.models.PlanckIdentity
import foundation.pEp.jniadapter.Identity
import javax.inject.Inject

class PlanckIdentityMapper @Inject constructor(private val planckProvider: PlanckProvider) {

    fun mapRecipients(recipients: List<Identity>): List<PlanckIdentity> {
        return recipients.map { recipient -> mapRecipient(planckProvider.updateIdentity(recipient)) }
    }

    private fun mapRecipient(recipient: Identity): PlanckIdentity {
        return PlanckIdentity().apply {
            address = recipient.address
            comm_type = recipient.comm_type
            flags = recipient.flags
            fpr = recipient.fpr
            lang = recipient.lang
            user_id = recipient.user_id
            username = recipient.username
            me = recipient.me
            rating = recipient.rating
            major_ver = recipient.major_ver
            minor_ver = recipient.minor_ver
            enc_format = recipient.enc_format;
        }
    }
}
