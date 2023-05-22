package com.fsck.k9.planck.ui.renderers.pepstatus

import android.view.View
import android.widget.*
import butterknife.Bind
import butterknife.OnClick
import com.fsck.k9.R
import com.fsck.k9.planck.models.PEpIdentity
import com.fsck.k9.planck.ui.privacy.status.PEpStatusIdentityView
import com.fsck.k9.planck.ui.privacy.status.PEpStatusRendererBuilder
import com.fsck.k9.planck.ui.privacy.status.PEpStatusTrustwordsPresenter
import com.fsck.k9.planck.ui.tools.FeedbackTools
import com.fsck.k9.ui.contacts.ContactPictureLoader
import javax.inject.Inject


class PEpStatusPGPIdentityRenderer @Inject constructor(contactsPictureLoader: ContactPictureLoader)
    : PEpStatusBaseRenderer(contactsPictureLoader), PEpStatusIdentityView {

    override fun getLayout() = R.layout.planck_recipient_row_with_fingerprints
    //FIXME Abstract between this and PEPStatusSecureRenderer

    private lateinit var handshakeResultListener: PEpStatusRendererBuilder.HandshakeResultListener
    private lateinit var myself: String
    private lateinit var trustwordsPresenter: PEpStatusTrustwordsPresenter

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


    fun setUp(resetClickListener: PEpStatusRendererBuilder.ResetClickListener,
             handshakeResultListener: PEpStatusRendererBuilder.HandshakeResultListener,
             myself: String
    ) {
        setUp(resetClickListener)
        this.myself = myself
        this.handshakeResultListener = handshakeResultListener
    }

    override fun render() {
        super.render()
        doLoadFingerPrints(content)
        myselfLabel.text = myself
        partnerLabel.text = content.address
    }

    override fun setUpView(rootView: View?) {
        trustwordsPresenter = PEpStatusTrustwordsPresenter(myself,context, this, permissionChecker)
    }

    private fun doLoadFingerPrints(identity: PEpIdentity) {
        trustwordsPresenter.loadHandshakeData(identity)
    }

    @OnClick(R.id.rejectHandshake)
    fun onRejectTrustwordsClicked() {
        trustwordsPresenter.rejectHandshake(content)
        handshakeResultListener.onHandshakeResult(content, false)
    }

    @OnClick(R.id.confirmHandshake)
    fun onConfirmTrustwordsClicked() {
        trustwordsPresenter.confirmHandshake(content)
        handshakeResultListener.onHandshakeResult(content, true)
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