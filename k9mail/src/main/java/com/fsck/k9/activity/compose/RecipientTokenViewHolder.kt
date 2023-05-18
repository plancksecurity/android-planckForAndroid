package com.fsck.k9.activity.compose

import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.R
import com.fsck.k9.planck.PEpUtils
import com.fsck.k9.planck.ui.PEpContactBadge
import com.fsck.k9.planck.ui.tools.ThemeManager
import com.fsck.k9.ui.contacts.ContactPictureLoader
import foundation.pEp.jniadapter.Rating
import security.planck.ui.doOnLayout
import security.planck.ui.doOnNextLayout

const val ELLIPSIS = "…"

class RecipientTokenViewHolder internal constructor(
    private val view: View,
    private val contactPictureLoader: ContactPictureLoader,
    private val account: Account,
    private val cryptoProvider: String?
) {

    private val name: TextView = view.findViewById(android.R.id.text1)
    private val contactPhoto: PEpContactBadge = view.findViewById(R.id.contact_photo)
    private val cryptoStatusRed: View = view.findViewById(R.id.contact_crypto_status_red)
    private val cryptoStatusOrange: View = view.findViewById(R.id.contact_crypto_status_orange)
    private val cryptoStatusGreen: View = view.findViewById(R.id.contact_crypto_status_green)
    private lateinit var recipient: Recipient
    var removeButtonLocation: ViewLocation? = null
        private set

    private val removeButton = view.findViewById<ImageView>(R.id.remove_button).also {
        it.doOnLayout {
            setRemoveButtonLocationData()
        }
    }

    fun bind(recipient: Recipient) {
        this.recipient = recipient
        name.text = recipient.displayNameOrAddress
        contactPictureLoader.setContactPicture(contactPhoto, recipient.address)
    }

    fun truncateName(newLimit: Int) {
        if (newLimit > 0 && newLimit <= recipient.displayNameOrAddress.length) {
            updateName(recipient.displayNameOrAddress.substring(0, newLimit) + ELLIPSIS)
        }
    }

    fun restoreNameSize() {
        updateName(recipient.displayNameOrAddress)
    }

    private fun updateName(newName: String) {
        name.text = newName
        name.width = name.paint.measureText(name.text.toString()).toInt()
        +name.paddingStart + name.paddingEnd
        removeButton.doOnNextLayout {
            setRemoveButtonLocationData()
        }
    }

    private fun setRemoveButtonLocationData() {
        removeButtonLocation = ViewLocation(
            removeButton.measuredWidth,
            removeButton.measuredHeight,
            removeButton.x,
            removeButton.y
        )
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
        if (K9.ispEpForwardWarningEnabled()) {
            if (account.ispEpPrivacyProtected() && PEpUtils.isRatingUnsecure(rating)) {
                view.setBackgroundResource(R.drawable.recipient_unsecure_token_shape)
                val warningColor = ContextCompat.getColor(
                    name.context,
                    R.color.compose_unsecure_delivery_warning
                )
                name.setTextColor(warningColor)
                ImageViewCompat.setImageTintList(
                    removeButton,
                    ColorStateList.valueOf(warningColor)
                )
            } else {
                view.setBackgroundResource(R.drawable.recipient_token_shape)
                name.setTextColor(
                    ThemeManager.getColorFromAttributeResource(
                        name.context,
                        android.R.attr.textColorSecondary
                    )
                )
                ImageViewCompat.setImageTintList(
                    removeButton,
                    null
                )
            }
        }
    }
    class ViewLocation(
        val width: Int,
        val height: Int,
        val x: Float,
        val y: Float,
    )
}
