package com.fsck.k9.planck.ui.renderers.pepstatus

import android.view.View
import android.widget.*
import butterknife.Bind
import butterknife.OnClick
import com.fsck.k9.R
import com.fsck.k9.planck.models.PlanckIdentity
import com.fsck.k9.planck.ui.privacy.status.PlanckStatusIdentityView
import com.fsck.k9.planck.ui.privacy.status.PlanckStatusRendererBuilder
import com.fsck.k9.planck.ui.privacy.status.PlanckStatusTrustwordsPresenter
import com.fsck.k9.planck.ui.tools.FeedbackTools
import com.fsck.k9.ui.contacts.ContactPictureLoader
import javax.inject.Inject


class PlanckStatusPGPIdentityRenderer @Inject constructor(contactsPictureLoader: ContactPictureLoader)
    : PlanckStatusBaseRenderer(contactsPictureLoader), PlanckStatusIdentityView {

    override fun getLayout() = R.layout.planck_recipient_row_with_fingerprints
    //FIXME Abstract between this and PEPStatusSecureRenderer

    private lateinit var handshakeListener: PlanckStatusRendererBuilder.HandshakeListener
    private lateinit var myself: String
    private lateinit var trustwordsPresenter: PlanckStatusTrustwordsPresenter

    @Bind(R.id.rejectHandshake)
    lateinit var rejectFingerPrintsButton: Button

    @Bind(R.id.confirmHandshake)
    lateinit var confirmFingerPrintsButton: Button

    @Bind(R.id.partnerLabel)
    internal lateinit var partnerLabel: TextView

    @Bind(R.id.partnerFpr)
    internal lateinit var partnerFpr: TextView

    @Bind(R.id.myselfLabel)
    internal lateinit var myselfLabel: TextView

    @Bind(R.id.myselfFpr)
    internal lateinit var myselfFpr: TextView


    fun setUp(
        handshakeListener: PlanckStatusRendererBuilder.HandshakeListener,
        myself: String
    ) {
        this.myself = myself
        this.handshakeListener = handshakeListener
    }

    override fun render() {
        super.render()
        doLoadFingerPrints(content)
        myselfLabel.text = myself
        partnerLabel.text = content.address
    }

    override fun setUpView(rootView: View?) {
        trustwordsPresenter = PlanckStatusTrustwordsPresenter(myself,context, this, permissionChecker)
    }

    private fun doLoadFingerPrints(identity: PlanckIdentity) {
        trustwordsPresenter.loadHandshakeData(identity)
    }

    @OnClick(R.id.rejectHandshake)
    fun onRejectTrustwordsClicked() {
        handshakeListener.startHandshake(content, false)
    }

    @OnClick(R.id.confirmHandshake)
    fun onConfirmTrustwordsClicked() {
        handshakeListener.startHandshake(content, true)
    }


    override fun reportError(errorMessage: String?) {
        enableButtons(true)
        FeedbackTools.showShortFeedback(rootView, errorMessage)
    }

    override fun enableButtons(enabled: Boolean) {
        rejectFingerPrintsButton.isEnabled = enabled
        confirmFingerPrintsButton.isEnabled = enabled
    }

    override fun setFingerPrintTexts(myselfFprText: String, partnerFprText: String) {
        myselfFpr.text = myselfFprText
        partnerFpr.text = partnerFprText
    }

    override fun setLabelTexts(myselfLabelText: String, partnerLabelText: String) {
        myselfLabel.text = myselfLabelText
        partnerLabel.text = partnerLabelText
    }
}