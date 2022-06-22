package com.fsck.k9.activity.compose

import com.fsck.k9.K9
import com.fsck.k9.mail.Address
import com.fsck.k9.pEp.PEpProvider
import com.fsck.k9.pEp.PEpUtils
import foundation.pEp.jniadapter.Rating
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

class UnsecureAddressHelper @Inject constructor(
    @Named("MainUI") private val pEp: PEpProvider,
) {
    private val unsecureAddresses = mutableSetOf<Address>()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private lateinit var view: RecipientSelectViewContract

    fun initialize (view: RecipientSelectViewContract) {
        this.view = view
    }

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

    fun getRecipientRating(
        recipient: Recipient,
        isPEpPrivacyProtected: Boolean,
        callback: PEpProvider.ResultCallback<Rating>
    ) {
        val address = recipient.address
        pEp.getRating(address, object : PEpProvider.ResultCallback<Rating> {
            override fun onLoaded(rating: Rating) {
                if (isPEpPrivacyProtected && PEpUtils.isRatingUnsecure(rating)
                    && view.hasRecipient(recipient)) {
                    addUnsecureAddressChannel(address)
                }
                callback.onLoaded(rating)
            }

            override fun onError(throwable: Throwable) {
                if (isPEpPrivacyProtected && view.hasRecipient(recipient)) {
                    addUnsecureAddressChannel(address)
                }
                callback.onError(throwable)
            }
        })
    }

    fun removeUnsecureAddressChannel(address: Address) {
        unsecureAddresses.remove(address)
    }

    fun isUnsecureChannel(): Boolean {
        return unsecureAddresses.isNotEmpty()
    }

    fun hasHiddenUnsecureAddressChannel(
        addresses: Array<Address>,
        hiddenAddresses: Int
    ): Boolean {
        for (address in unsecureAddresses) {
            val start = addresses.size - hiddenAddresses
            if (addresses.indexOf(address) >= start) {
                return true
            }
        }
        return false
    }

    private fun addUnsecureAddressChannel(address: Address) {
        if (K9.ispEpForwardWarningEnabled()) {
            unsecureAddresses.add(address)
        }
    }
}

interface RecipientsReadyListener {
    fun recipientsReady(recipients: MutableList<Recipient>)
}

interface RatedRecipientsReadyListener {
    fun ratedRecipientsReady(recipients: MutableList<RatedRecipient>)
}
