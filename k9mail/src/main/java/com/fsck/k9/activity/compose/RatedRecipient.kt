package com.fsck.k9.activity.compose

import android.content.Context
import android.net.Uri
import com.fsck.k9.mail.Address
import foundation.pEp.jniadapter.Rating

class RatedRecipient private constructor(
    private val baseRecipient: Recipient
) : Recipient(baseRecipient.address) {

    lateinit var rating: Rating
        private set

    constructor(
        baseRecipient: Recipient,
        rating: Rating
    ) : this(baseRecipient) {
        this.rating = rating
    }

    override fun getAddress(): Address {
        return baseRecipient.address
    }

    override fun setAddress(address: Address) {
        baseRecipient.address = address
    }

    override fun getAddressLabel(): String? {
        return baseRecipient.addressLabel
    }

    override fun setAddressLabel(addressLabel: String?) {
        baseRecipient.addressLabel = addressLabel
    }

    internal override fun getDisplayNameOrAddress(): String? {
        return baseRecipient.displayNameOrAddress
    }

    override fun getDisplayNameOrUnknown(context: Context?): String? {
        return baseRecipient.getDisplayNameOrUnknown(context)
    }

    override fun getNameOrUnknown(context: Context?): String? {
        return baseRecipient.getNameOrUnknown(context)
    }

    override fun getCryptoStatus(): RecipientSelectView.RecipientCryptoStatus {
        return baseRecipient.cryptoStatus
    }

    override fun setCryptoStatus(cryptoStatus: RecipientSelectView.RecipientCryptoStatus) {
        baseRecipient.cryptoStatus = cryptoStatus
    }

    override fun getContactLookupUri(): Uri? {
        return baseRecipient.contactLookupUri
    }

    override fun isValidEmailAddress(): Boolean {
        return super.isValidEmailAddress()
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is RatedRecipient -> baseRecipient == other.baseRecipient
            else -> baseRecipient == other
        }
    }

    override fun hashCode(): Int {
        var result = baseRecipient.hashCode()
        result = 31 * result + rating.hashCode()
        return result
    }
}
