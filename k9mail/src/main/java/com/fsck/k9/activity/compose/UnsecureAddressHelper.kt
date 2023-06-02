package com.fsck.k9.activity.compose

import com.fsck.k9.K9
import com.fsck.k9.mail.Address
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.PlanckUtils
import foundation.pEp.jniadapter.Rating
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class UnsecureAddressHelper @Inject constructor(
    private val planck: PlanckProvider,
) {
    private val unsecureAddresses = mutableSetOf<Address>()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private lateinit var view: RecipientSelectViewContract

    val unsecureAddressChannelCount: Int
        get() = unsecureAddresses.size

    fun initialize(view: RecipientSelectViewContract) {
        this.view = view
    }

    fun sortRecipientsByRating(
        recipients: Array<Recipient>,
        recipientsReadyListener: RecipientsReadyListener
    ) {
        coroutineScope.launch {
            recipients.map { recipient ->
                val rating = planck.getRating(recipient.address)
                    .onFailure { view.showError(it) }
                    .getOrDefault(Rating.pEpRatingUndefined)
                Pair(recipient, rating)
            }.sortedBy { pair ->
                pair.second
            }.map { pair ->
                pair.first
            }.also { recipientsReadyListener.recipientsReady(it.toMutableList()) }
        }
    }

    fun updateRecipientsFromEcho(
        recipients: List<Recipient>,
        echoSender: String,
        ratedRecipientsReadyListener: RatedRecipientsReadyListener
    ) {
        coroutineScope.launch {
            recipients
                .filter { it.address.address.equals(echoSender, true) }
                .filter { it.address in unsecureAddresses }
                .map { recipient ->
                    recipient
                        .toRatedRecipient(
                            planck.getRating(recipient.address)
                                .onFailure { view.showError(it) }
                                .getOrDefault(Rating.pEpRatingUndefined)
                        )
                        .also {
                            if (!PlanckUtils.isRatingUnsecure(it.rating)) {
                                removeUnsecureAddressChannel(it.baseRecipient.address)
                            }
                        }
                }.also {
                    ratedRecipientsReadyListener.ratedRecipientsReady(it.toMutableList())
                }
        }
    }

    fun rateRecipients(
        recipients: List<Recipient>,
        ratedRecipientsReadyListener: RatedRecipientsReadyListener
    ) {
        coroutineScope.launch {
            recipients.map { recipient ->
                recipient.toRatedRecipient(
                    planck.getRating(recipient.address)
                        .onFailure { view.showError(it) }
                        .getOrDefault(Rating.pEpRatingUndefined)
                )
            }.also { ratedRecipientsReadyListener.ratedRecipientsReady(it.toMutableList()) }
        }
    }

    fun getRecipientRating(
        recipient: Recipient,
        isPEpPrivacyProtected: Boolean,
        callback: PlanckProvider.ResultCallback<Rating>
    ) {
        val address = recipient.address
        planck.getRating(address, object : PlanckProvider.ResultCallback<Rating> {
            override fun onLoaded(rating: Rating) {
                val viewRating =
                    if (K9.isPlanckForwardWarningEnabled() && view.isAlwaysUnsecure) {
                        Rating.pEpRatingUnencrypted
                    } else {
                        rating
                    }
                if (isPEpPrivacyProtected && PlanckUtils.isRatingUnsecure(viewRating)
                    && view.hasRecipient(recipient)
                ) {
                    addUnsecureAddressChannel(address)
                }
                callback.onLoaded(viewRating)
            }

            override fun onError(throwable: Throwable) {
                if (isPEpPrivacyProtected && view.hasRecipient(recipient)) {
                    addUnsecureAddressChannel(address)
                }
                view.showError(throwable)
                callback.onError(throwable)
            }
        })
    }

    fun removeUnsecureAddressChannel(address: Address) {
        unsecureAddresses.remove(address)
    }

    fun isUnsecure(address: Address): Boolean {
        return unsecureAddresses.contains(address)
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
        if (K9.isPlanckForwardWarningEnabled()) {
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
