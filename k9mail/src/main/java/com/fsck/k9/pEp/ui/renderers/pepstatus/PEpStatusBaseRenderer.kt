package com.fsck.k9.pEp.ui.renderers.pepstatus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.annotation.Nullable
import butterknife.Bind
import butterknife.ButterKnife
import butterknife.OnClick
import com.fsck.k9.K9
import com.fsck.k9.R
import com.fsck.k9.helper.Contacts
import com.fsck.k9.helper.MessageHelper
import com.fsck.k9.helper.Utility
import com.fsck.k9.mail.Address
import com.fsck.k9.pEp.PePUIArtefactCache
import com.fsck.k9.pEp.models.PEpIdentity
import com.fsck.k9.pEp.ui.PEpContactBadge
import com.fsck.k9.pEp.ui.privacy.status.PEpStatusRendererBuilder
import com.fsck.k9.ui.contacts.ContactPictureLoader
import com.pedrogomez.renderers.Renderer
import foundation.pEp.jniadapter.Rating
import security.pEp.permissions.PermissionChecker
import security.pEp.ui.permissions.PEpPermissionChecker

abstract class PEpStatusBaseRenderer(val contactsPictureLoader: ContactPictureLoader) : Renderer<PEpIdentity>() {
    lateinit var resetClickListener: PEpStatusRendererBuilder.ResetClickListener

    @Bind(R.id.tvUsername)
    lateinit var identityUserName: TextView

    @Bind(R.id.tvRatingStatus)
    lateinit var ratingStatusTV: TextView

    @Nullable @Bind(R.id.status_explanation_text)
    lateinit var statusExplanationTV: TextView

    @Bind(R.id.status_badge)
    lateinit var badge: PEpContactBadge

    @Nullable @Bind(R.id.button_identity_key_reset)
    lateinit var resetDataButton: Button

    @Nullable @Bind(R.id.resetDataLayout)
    lateinit var resetDataLayout: View

    override fun onCreate(content: PEpIdentity?, layoutInflater: LayoutInflater?, parent: ViewGroup?) {
        super.onCreate(content, layoutInflater, parent)
    }

     fun setUp(resetClickListener: PEpStatusRendererBuilder.ResetClickListener) {
        this.resetClickListener = resetClickListener
    }
    protected lateinit var permissionChecker: PermissionChecker

    override fun inflate(inflater: LayoutInflater?, parent: ViewGroup?): View {
        permissionChecker = PEpPermissionChecker(parent!!.context.applicationContext)
        val view: View = inflater!!.inflate(getLayout(), parent, false)
        ButterKnife.bind(this, view)
        //badge.enableStatusBadge()
        return view
    }

    @LayoutRes abstract fun getLayout(): Int

    override fun render() {
        val identity: PEpIdentity = content
        renderRating(identity.rating)
        renderBadge(identity)
    }

    private fun renderRating(rating: Rating) {
        val artefactCache = PePUIArtefactCache.getInstance(context)
        ratingStatusTV.text = artefactCache.getTitle(rating)
        if (::statusExplanationTV.isInitialized) statusExplanationTV.text = artefactCache.getSuggestion(rating)

        if (rating == Rating.pEpRatingCannotDecrypt || rating == Rating.pEpRatingHaveNoKey) {
            resetDataLayout.visibility = View.GONE
        } else {
            resetDataLayout.visibility = View.VISIBLE
        }
    }

    private fun renderBadge(identity: PEpIdentity) {
        val realAddress = Address(identity.address, identity.username)
        if (K9.showContactPicture()) {
            Utility.setContactForBadge(badge, realAddress)
            contactsPictureLoader.setContactPicture(badge, realAddress)
            badge.setPepRating(identity.rating, true)
        }
        val contacts = if (permissionChecker.hasContactsPermission() &&
                K9.showContactName()) Contacts.getInstance(context) else null
        renderContact(realAddress, contacts)
    }

    private fun renderContact(realAddress: Address, contacts: Contacts?) {
        val partner = MessageHelper.toFriendly(realAddress, contacts)
        identityUserName.text = partner
    }

    @Nullable @OnClick(R.id.button_identity_key_reset)
    fun onResetClicked() {
        resetClickListener.keyReset(content)
    }

    override fun hookListeners(rootView: View?) {
        //NOP
    }

    override fun setUpView(rootView: View?) {
        ButterKnife.bind(this, rootView)
    }
}