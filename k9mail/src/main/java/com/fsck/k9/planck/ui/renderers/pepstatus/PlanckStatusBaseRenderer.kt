package com.fsck.k9.planck.ui.renderers.pepstatus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.LayoutRes
import butterknife.Bind
import butterknife.ButterKnife
import com.fsck.k9.K9
import com.fsck.k9.R
import com.fsck.k9.helper.Contacts
import com.fsck.k9.helper.MessageHelper
import com.fsck.k9.helper.Utility
import com.fsck.k9.mail.Address
import com.fsck.k9.planck.models.PlanckIdentity
import com.fsck.k9.planck.ui.PlanckContactBadge
import com.fsck.k9.ui.contacts.ContactPictureLoader
import com.pedrogomez.renderers.Renderer
import security.planck.permissions.PermissionChecker
import security.planck.ui.permissions.PlanckPermissionChecker

abstract class PlanckStatusBaseRenderer(val contactsPictureLoader: ContactPictureLoader) : Renderer<PlanckIdentity>() {
    @Bind(R.id.tvUsername)
    lateinit var identityUserName: TextView

    @Bind(R.id.status_badge)
    lateinit var badge: PlanckContactBadge


    override fun onCreate(content: PlanckIdentity?, layoutInflater: LayoutInflater?, parent: ViewGroup?) {
        super.onCreate(content, layoutInflater, parent)
    }

    protected lateinit var permissionChecker: PermissionChecker

    override fun inflate(inflater: LayoutInflater?, parent: ViewGroup?): View {
        permissionChecker = PlanckPermissionChecker(parent!!.context.applicationContext)
        val view: View = inflater!!.inflate(getLayout(), parent, false)
        ButterKnife.bind(this, view)
        //badge.enableStatusBadge()
        return view
    }

    @LayoutRes abstract fun getLayout(): Int

    override fun render() {
        renderBadge(content)
    }

    private fun renderBadge(identity: PlanckIdentity) {
        val realAddress = Address(identity.address, identity.username)
        if (K9.showContactPicture()) {
            Utility.setContactForBadge(badge, realAddress)
            contactsPictureLoader.setContactPicture(badge, realAddress)
            badge.setPlanckRating(identity.rating, true)
        }
        val contacts = if (permissionChecker.hasContactsPermission() &&
                K9.showContactName()) Contacts.getInstance(context) else null
        renderContact(realAddress, contacts)
    }

    private fun renderContact(realAddress: Address, contacts: Contacts?) {
        val partner = MessageHelper.toFriendly(realAddress, contacts)
        identityUserName.text = partner
    }

    override fun hookListeners(rootView: View?) {
        //NOP
    }

    override fun setUpView(rootView: View?) {
        ButterKnife.bind(this, rootView)
    }
}