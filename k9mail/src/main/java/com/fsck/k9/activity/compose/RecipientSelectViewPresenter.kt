package com.fsck.k9.activity.compose

import com.fsck.k9.mail.Message
import com.fsck.k9.pEp.PEpProvider
import foundation.pEp.jniadapter.Rating

class RecipientSelectViewPresenter (
    private val presenter: RecipientPresenter,
    private val type: Message.RecipientType
) {
    fun hasUnsecureRecipients(count: Int): Boolean {
        return presenter.hasUnsecureAddresses(count, type)
    }
    fun getRecipientRating(
        recipient: Recipient?,
        callback: PEpProvider.ResultCallback<Rating?>?
    ) {
        presenter.getRecipientRating(recipient, type, callback)
    }
}
