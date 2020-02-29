package com.fsck.k9.pEp.ui.renderers.pepstatus

import android.view.View
import android.widget.*
import butterknife.Bind
import butterknife.OnClick
import com.fsck.k9.R
import com.fsck.k9.pEp.models.PEpIdentity
import com.fsck.k9.pEp.ui.privacy.status.PEpStatusPGPIdentityView
import com.fsck.k9.pEp.ui.privacy.status.PEpStatusRendererBuilder
import com.fsck.k9.pEp.ui.privacy.status.PEpStatusTrustwordsPresenter
import com.fsck.k9.pEp.ui.tools.FeedbackTools


class PEpStatusPGPIdentityRenderer(
        resetClickListener: PEpStatusRendererBuilder.ResetClickListener,
        private val handshakeResultListener: PEpStatusRendererBuilder.HandshakeResultListener,
        private val myself: String
)
    : PEpStatusBaseRenderer(resetClickListener), PEpStatusPGPIdentityView {

    override fun getLayout() = R.layout.pep_recipient_row_with_fingerprints


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

    override fun render() {
        super.render()
        doLoadFingerPrints(content)
        myselfLabel.text = myself
        partnerLabel.text = content.address
    }

    override fun setUpView(rootView: View?) {
        trustwordsPresenter = PEpStatusTrustwordsPresenter(myself,context, this)
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