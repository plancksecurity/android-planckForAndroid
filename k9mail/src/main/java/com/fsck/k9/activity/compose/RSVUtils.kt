package com.fsck.k9.activity.compose

import com.fsck.k9.pEp.PEpProvider
import foundation.pEp.jniadapter.Rating
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


fun List<Recipient>.withRatedRecipientsSorted(
    pEp: PEpProvider,
    ratedRecipientsReadyListener: RatedRecipientsReadyListener
) {
    CoroutineScope(Dispatchers.Main).launch {
        map { recipient ->
            recipient.toRatedRecipient(pEp.getRating(recipient.address))
        }.sortedBy {
            it.rating.value
        }.also { ratedRecipientsReadyListener.ratedRecipientsReady(it.toMutableList()) }
    }
}

fun Recipient.toRatedRecipient(rating: Rating) = RatedRecipient(this, rating)

interface RatedRecipientsReadyListener {
    fun ratedRecipientsReady(recipients: MutableList<Recipient>)
}
