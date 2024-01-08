package com.fsck.k9.activity.compose

import com.fsck.k9.K9
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Message.RecipientType
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.PlanckUtils
import foundation.pEp.jniadapter.Rating
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class RecipientSelectPresenter @Inject constructor(
    private val planck: PlanckProvider,
) {
    private val unsecureAddresses = mutableSetOf<Address>()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private lateinit var view: RecipientSelectContract
    private lateinit var presenter: RecipientPresenter

    val unsecureAddressChannelCount: Int
        get() = unsecureAddresses.size

    val addresses: List<Address>
        get() = recipients.map { it.address }

    val recipients: List<Recipient>
        get() = view.recipients

    fun initialize(view: RecipientSelectContract) {
        this.view = view
    }

    fun setPresenter(presenter: RecipientPresenter, type: RecipientType) {
        this.presenter = presenter
        presenter.setPresenter(this, type)
    }

    fun clearUnsecureAddresses() {
        recipients.forEach { recipient ->
            if (isUnsecure(recipient.address)) {
                view.removeRecipient(recipient)
            }
        }
        view.resetCollapsedViewIfNeeded()
    }

    private fun addRecipient(recipient: Recipient) {
        view.addRecipient(recipient)
    }

    fun addRecipients(vararg recipients: Recipient) {
        if (recipients.size == 1) {
            addRecipient(recipients.first())
        } else {
            coroutineScope.launch {
                sortRecipientsByRatingSuspend(recipients.toList()).onEach { addRecipient(it) }
            }
        }
    }

    private fun removeRecipient(recipient: Recipient) {
        view.removeRecipient(recipient)
    }

    fun showNoRecipientsError() {
        view.showNoRecipientsError()
    }

    fun reportedUncompletedRecipients(): Boolean {
        return view.hasUncompletedRecipients().also {
            if (it) {
                view.showUncompletedError()
            }
        }
    }

    fun tryPerformCompletion(): Boolean = view.tryPerformCompletion()

    fun notifyRecipientsChanged() {
        for (recipient in recipients) {
            removeRecipient(recipient)
            addRecipient(recipient)
        }
        view.restoreFirstRecipientTruncation()
    }

    private suspend fun sortRecipientsByRatingSuspend(
        recipients: List<Recipient>,
    ): List<Recipient> = recipients.map { recipient ->
        val rating = planck.getRating(recipient.address)
            .onFailure { presenter.showError(it) }
            .getOrDefault(Rating.pEpRatingUndefined)
        Pair(recipient, rating)
    }.sortedBy { pair ->
        pair.second
    }.map { pair ->
        pair.first
    }

    fun updateRecipientsFromMessage() {
        coroutineScope.launch {
            view.recipients
                .filter { it.address in unsecureAddresses }
                .map { recipient ->
                    recipient
                        .toRatedRecipient(
                            planck.getRating(recipient.address)
                                .onFailure { presenter.showError(it) }
                                .getOrDefault(Rating.pEpRatingUndefined)
                        )
                        .also {
                            if (!PlanckUtils.isRatingUnsecure(it.rating)) {
                                removeUnsecureAddressChannel(it.baseRecipient.address)
                            }
                        }
                }.also {
                    view.updateRecipients(it.toMutableList())
                    presenter.handleUnsecureDeliveryWarning()
                }
        }
    }

    fun rateAlternateRecipients(
        recipients: List<Recipient>,
    ) {
        coroutineScope.launch {
            recipients.map { recipient ->
                recipient.toRatedRecipient(
                    planck.getRating(recipient.address)
                        .onFailure { presenter.showError(it) }
                        .getOrDefault(Rating.pEpRatingUndefined)
                )
            }.also { view.showAlternatesPopup(it.toMutableList()) }
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
                presenter.showError(throwable)
                callback.onError(throwable)
            }
        })
    }

    fun removeUnsecureAddressChannel(address: Address) {
        unsecureAddresses.remove(address)
    }

    private fun isUnsecure(address: Address): Boolean {
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

    fun onRecipientsChanged() {
        presenter.onRecipientsChanged()
    }

    fun handleUnsecureTokenWarning() {
        presenter.handleUnsecureDeliveryWarning()
    }
}
