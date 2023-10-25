package com.fsck.k9.planck.models.mappers

import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.models.PlanckIdentity
import foundation.pEp.jniadapter.Identity
import javax.inject.Inject

class PlanckIdentityMapper @Inject constructor(private val planckProvider: PlanckProvider) {

    fun mapRecipients(recipients: List<Identity>): List<PlanckIdentity> {
        return recipients.map { recipient -> updateAndMapRecipient(recipient) }
    }

    fun updateAndMapRecipient(recipient: Identity): PlanckIdentity {
        val updatedRecipient = planckProvider.updateIdentity(recipient)
        return PlanckIdentity().apply {
            address = updatedRecipient.address
            comm_type = updatedRecipient.comm_type
            flags = updatedRecipient.flags
            fpr = updatedRecipient.fpr
            lang = updatedRecipient.lang
            user_id = updatedRecipient.user_id
            username = updatedRecipient.username
            me = updatedRecipient.me
            rating = updatedRecipient.rating
        }
    }
}
