package com.fsck.k9.activity.compose

import com.fsck.k9.mail.Address
import com.fsck.k9.pEp.PEpProvider
import com.fsck.k9.pEp.PEpUtils
import foundation.pEp.jniadapter.Rating
import javax.inject.Inject
import javax.inject.Named

class RecipientSelectViewPresenter @Inject constructor(
    @Named("MainUI") private val pEp: PEpProvider,
) {
    private val unsecureAddresses = mutableListOf<Address>()

    fun getRecipientRating(
        address: Address,
        isPEpPrivacyProtected: Boolean,
        callback: PEpProvider.ResultCallback<Rating?>
    ) {
        pEp.getRating(address, object : PEpProvider.ResultCallback<Rating> {
            override fun onLoaded(rating: Rating) {
                if (isPEpPrivacyProtected && PEpUtils.isRatingUnsecure(rating)) {
                    addUnsecureAddress(address)
                }
                callback.onLoaded(rating)
            }

            override fun onError(throwable: Throwable) {
                addUnsecureAddress(address)
                callback.onError(throwable)
            }
        })
    }

    private fun addUnsecureAddress(address: Address) {
        if (!unsecureAddresses.contains(address)) {
            unsecureAddresses.add(address)
        }
    }

    fun removeUnsecureAddress(address: Address) {
        unsecureAddresses.remove(address)
    }

    fun hasUnsecureAddresses(): Boolean {
        return unsecureAddresses.isNotEmpty()
    }

    fun hasUnsecureAddresses(addresses: Array<Address>, count: Int): Boolean {
        for (address in unsecureAddresses) {
            val start = addresses.size - count
            if (addresses.indexOf(address) >= start) {
                return true
            }
        }
        return false
    }
}
