package com.fsck.k9.activity.compose

import com.fsck.k9.pEp.PEpProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

class UnsecureAddressHelper @Inject constructor(
    @Named("MainUI") private val pEp: PEpProvider,
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    fun sortRecipientsByRating(
        recipients: Array<Recipient>,
        recipientsReadyListener: RecipientsReadyListener
    ) {
        coroutineScope.launch {
            recipients.map { recipient ->
                Pair(recipient, pEp.getRating(recipient.address))
            }.sortedBy { pair ->
                pair.second
            }.map { pair ->
                pair.first
            }.also { recipientsReadyListener.recipientsReady(it.toMutableList()) }
        }
    }

    fun rateRecipients(
        recipients: List<Recipient>,
        ratedRecipientsReadyListener: RatedRecipientsReadyListener
    ) {
        coroutineScope.launch {
            recipients.map { recipient ->
                recipient.toRatedRecipient(pEp.getRating(recipient.address))
            }.also { ratedRecipientsReadyListener.ratedRecipientsReady(it.toMutableList()) }
        }
    }
}

interface RecipientsReadyListener {
    fun recipientsReady(recipients: MutableList<Recipient>)
}

interface RatedRecipientsReadyListener {
    fun ratedRecipientsReady(recipients: MutableList<RatedRecipient>)
}
