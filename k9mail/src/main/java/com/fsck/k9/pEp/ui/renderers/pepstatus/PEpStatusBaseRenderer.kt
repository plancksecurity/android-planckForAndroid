package com.fsck.k9.pEp.ui.renderers.pepstatus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import butterknife.Bind
import butterknife.ButterKnife
import com.fsck.k9.K9
import com.fsck.k9.R
import com.fsck.k9.helper.ContactPicture
import com.fsck.k9.helper.Contacts
import com.fsck.k9.helper.MessageHelper
import com.fsck.k9.helper.Utility
import com.fsck.k9.mail.Address
import com.fsck.k9.pEp.PePUIArtefactCache
import com.fsck.k9.pEp.models.PEpIdentity
import com.fsck.k9.pEp.ui.PEpContactBadge
import com.pedrogomez.renderers.Renderer
import foundation.pEp.jniadapter.Identity
import foundation.pEp.jniadapter.Rating
import timber.log.Timber

abstract class PEpStatusBaseRenderer : Renderer<PEpIdentity>() {

    @Bind(R.id.tvUsername)
    lateinit var identityUserName: TextView

    @Bind(R.id.tvRatingStatus)
    lateinit var ratingStatusTV: TextView

    @Bind(R.id.status_explanation_text)
    lateinit var statusExplanationTV: TextView

    lateinit var identityAdress: TextView

    @Bind(R.id.status_badge)
    lateinit var badge: PEpContactBadge

    override fun render() {
        val identity: PEpIdentity = content
        renderRating(identity.rating)
        renderBadge(identity.address, identity.rating)
        //renderIdentity(identity)
    }

    private fun renderRating(rating: Rating) {
        val artefactCache = PePUIArtefactCache.getInstance(context)
        ratingStatusTV.text = artefactCache.getTitle(rating)
        statusExplanationTV.text = artefactCache.getExplanation(rating)
        Timber.e("==== rating text is ${PePUIArtefactCache.getInstance(context).getTitle(rating)}")
    }

    protected fun renderBadge(address: String, rating: Rating) {
        val realAddress = Address(address)
        if (K9.showContactPicture()) {
            Utility.setContactForBadge(badge, realAddress)
            val mContactsPictureLoader = ContactPicture.getContactPictureLoader(context)
            mContactsPictureLoader.loadContactPicture(realAddress, badge)
            badge.setPepRating(rating, true)
        }
        val contacts = if (K9.showContactName()) Contacts.getInstance(context) else null
        val partner = MessageHelper.toFriendly(realAddress, contacts)
        identityUserName.text = partner
    }



    fun renderIdentity(identity: Identity) {



        if (identity.username != null && identity.address != identity.username && !identity.username.isEmpty()) {
            identityUserName.text = identity.username
            if (identity.address != null) {
                identityAdress.visibility = View.VISIBLE
                identityAdress.text = identity.address
            } else {
                identityAdress.visibility = View.VISIBLE
            }

        } else {
            identityUserName.visibility = View.GONE
            identityAdress.visibility = View.VISIBLE
            identityAdress.text = identity.address
        }
    }
}