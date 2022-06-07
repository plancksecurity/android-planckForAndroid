package com.fsck.k9.activity.compose

import com.fsck.k9.pEp.PEpProvider
import foundation.pEp.jniadapter.Rating
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


fun List<Recipient>.withRatedRecipients(
    pEp: PEpProvider,
    ratedRecipientsReadyListener: RecipientsReadyListener
) {
    CoroutineScope(Dispatchers.Main).launch {
        map { recipient ->
            recipient.toRatedRecipient(pEp.getRating(recipient.address))
        }.also { ratedRecipientsReadyListener.recipientsReady(it.toMutableList()) }
    }
}

fun Array<Recipient>.withRecipientsSortedByRating(
    pEp: PEpProvider,
    recipientsReadyListener: RecipientsReadyListener
) {
    CoroutineScope(Dispatchers.Main).launch {
        map { recipient ->
            Pair(recipient, pEp.getRating(recipient.address))
        }.sortedBy { pair ->
            pair.second
        }.map { pair ->
            pair.first
        }.also { recipientsReadyListener.recipientsReady(it.toMutableList()) }
    }
}

fun Recipient.toRatedRecipient(rating: Rating) = RatedRecipient(this, rating)

interface RecipientsReadyListener {
    fun recipientsReady(recipients: MutableList<Recipient>)
}
