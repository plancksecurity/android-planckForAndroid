package com.fsck.k9.activity.compose

import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.fsck.k9.Account
import com.fsck.k9.BuildConfig
import com.fsck.k9.R
import com.fsck.k9.pEp.PEpUtils
import com.fsck.k9.pEp.ui.PEpContactBadge
import com.fsck.k9.ui.contacts.ContactPictureLoader
import foundation.pEp.jniadapter.Rating

class RecipientTokenViewHolder internal constructor(
    private val view: View,
    private val contactPictureLoader: ContactPictureLoader,
    private val account: Account,
    private val cryptoProvider: String?
) {

    private val name: TextView = view.findViewById(android.R.id.text1)
    private val contactPhoto: PEpContactBadge = view.findViewById(R.id.contact_photo)
    private val cryptoStatusRed: View =
        view.findViewById(R.id.contact_crypto_status_red)
    private val cryptoStatusOrange: View =
        view.findViewById(R.id.contact_crypto_status_orange)
    private val cryptoStatusGreen: View =
        view.findViewById(R.id.contact_crypto_status_green)
    private lateinit var recipient: Recipient

    fun bind(recipient: Recipient) {
        this.recipient = recipient
        name.text = recipient.displayNameOrAddress
        contactPictureLoader.setContactPicture(contactPhoto, recipient.address)
    }

    fun updateRating(rating: Rating) {
        setpEpRating(rating)
        val hasCryptoProvider = cryptoProvider != null
        if (!hasCryptoProvider) {
            cryptoStatusRed.visibility = View.GONE
            cryptoStatusOrange.visibility = View.GONE
            cryptoStatusGreen.visibility = View.GONE
        } else
            when (recipient.cryptoStatus) {
                RecipientSelectView.RecipientCryptoStatus.UNAVAILABLE -> {
                    cryptoStatusRed.visibility = View.VISIBLE
                    cryptoStatusOrange.visibility = View.GONE
                    cryptoStatusGreen.visibility = View.GONE
                }
                RecipientSelectView.RecipientCryptoStatus.AVAILABLE_UNTRUSTED -> {
                    cryptoStatusRed.visibility = View.GONE
                    cryptoStatusOrange.visibility = View.VISIBLE
                    cryptoStatusGreen.visibility = View.GONE
                }
                RecipientSelectView.RecipientCryptoStatus.AVAILABLE_TRUSTED -> {
                    cryptoStatusRed.visibility = View.GONE
                    cryptoStatusOrange.visibility = View.GONE
                    cryptoStatusGreen.visibility = View.VISIBLE
                }
                RecipientSelectView.RecipientCryptoStatus.UNDEFINED -> {
                    cryptoStatusRed.visibility = View.GONE
                    cryptoStatusOrange.visibility = View.GONE
                    cryptoStatusGreen.visibility = View.GONE
                }
            }
    }

    private fun setpEpRating(rating: Rating) {
        when {
            account.ispEpPrivacyProtected() && PEpUtils.isRatingUnsecure(rating) -> {
                view.setBackgroundResource(R.drawable.recipient_unsecure_token_shape)
                name.setTextColor(ContextCompat.getColor(name.context, R.color.pep_red))
            }
            else -> {
                view.setBackgroundResource(R.drawable.recipient_token_shape)
            }
        }
        if (BuildConfig.SHOW_PEP_STATUS_IN_RECIPIENT) {
            contactPhoto.setPepRating(rating, account.ispEpPrivacyProtected())
        }
    }
}