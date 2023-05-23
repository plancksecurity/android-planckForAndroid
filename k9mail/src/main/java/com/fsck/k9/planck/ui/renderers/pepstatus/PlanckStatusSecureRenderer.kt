package com.fsck.k9.planck.ui.renderers.pepstatus

import android.view.View
import android.widget.*
import androidx.appcompat.widget.PopupMenu
import butterknife.Bind
import butterknife.OnClick
import butterknife.OnLongClick
import com.fsck.k9.R
import com.fsck.k9.planck.models.PlanckIdentity
import com.fsck.k9.planck.ui.privacy.status.*
import com.fsck.k9.planck.ui.tools.FeedbackTools
import com.fsck.k9.ui.contacts.ContactPictureLoader
import javax.inject.Inject


class PlanckStatusSecureRenderer  @Inject constructor(contactsPictureLoader: ContactPictureLoader)
    : PlanckStatusBaseRenderer(contactsPictureLoader), PlanckStatusPlanckIdentityView {

    override fun getLayout() = R.layout.planck_recipient_row_with_trustwords

    private lateinit var handshakeResultListener: PlanckStatusRendererBuilder.HandshakeResultListener
    private lateinit var myself: String
    private lateinit var trustwordsPresenter: PlanckStatusTrustwordsPresenter

    @Bind(R.id.trustwords)
    lateinit var trustwordsTv: TextSwitcher

    @Bind(R.id.change_language)
    lateinit var changeLanguageImage: ImageView

    @Bind(R.id.fpr_container)
    lateinit var fingerPrintContainer: View

    @Bind(R.id.fpr_partner_account_title)
    lateinit var fingerPrintPartnerAccountTitle: TextView

    @Bind(R.id.fpr_partner_account_value)
    lateinit var fingerPrintPartnerAccountValue: TextView

    @Bind(R.id.fpr_current_account_title)
    lateinit var fingerPrintCurrentAccountTitle: TextView

    @Bind(R.id.fpr_current_account_value)
    lateinit var fingerPrintCurrentAccountValue: TextView

    @Bind(R.id.rejectHandshake)
    lateinit var rejectTrustwordsButton: Button

    @Bind(R.id.confirmHandshake)
    lateinit var confirmTrustwordsButton: Button

    fun setUp(resetClickListener: PlanckStatusRendererBuilder.ResetClickListener,
              handshakeResultListener: PlanckStatusRendererBuilder.HandshakeResultListener,
              myself: String) {
        setUp(resetClickListener)
        this.myself = myself
        this.handshakeResultListener = handshakeResultListener
    }

    override fun hookListeners(rootView: View?) {
        trustwordsTv.setInAnimation(context, android.R.anim.fade_in)
        trustwordsTv.setOutAnimation(context, android.R.anim.fade_out)
    }

    override fun render() {
        super.render()
        doLoadTrustWords(content)
    }

    override fun setUpView(rootView: View?) {
        trustwordsPresenter = PlanckStatusTrustwordsPresenter(myself,context, this, permissionChecker)
    }

    private fun doLoadTrustWords(identity: PlanckIdentity) {
        trustwordsPresenter.loadHandshakeData(identity)
    }

    @OnClick(R.id.trustwords)
    fun doShowFullTrustwords() {
        trustwordsPresenter.changeTrustwordsSize(content, false)
    }


    @OnLongClick(R.id.trustwords)
    fun doShowShortTrustwords() : Boolean {
        trustwordsPresenter.changeTrustwordsSize(content, true)
        return true
    }

    @OnClick(R.id.change_language)
    fun onChangeLanguageClicked() {
        showLanguageSelectionPopup(changeLanguageImage)
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

    private fun showLanguageSelectionPopup(v: View) {
        val popup = PopupMenu(context, v)
        val langArray = trustwordsPresenter.getLanguageList()
        for(language in langArray) {
            popup.menu.add(language)
        }
        popup.setOnMenuItemClickListener {
            item -> trustwordsPresenter.changeTrustwordsLanguage(content, item.title.toString())
            true
        }
        popup.show()
    }

    override fun setLongTrustwords(newTrustwords: String) {
        trustwordsTv.setText(newTrustwords)
    }

    override fun setShortTrustwords(newTrustwords: String) {
        trustwordsTv.setText(context.getString(R.string.ellipsized_text, newTrustwords))
    }

    override fun setLabelTexts(myselfLabelText: String, partnerLabelText: String) {
        fingerPrintPartnerAccountTitle.text = partnerLabelText
        fingerPrintCurrentAccountTitle.text = myselfLabelText
    }

    override fun setFingerPrintTexts(myselfFprText: String, partnerFprText: String) {
        fingerPrintPartnerAccountValue.text = partnerFprText
        fingerPrintCurrentAccountValue.text = myselfFprText
    }

    override fun reportError(errorMessage: String?) {
        enableButtons(true)
        FeedbackTools.showShortFeedback(rootView, errorMessage)
    }

    override fun enableButtons(enabled: Boolean) {
        rejectTrustwordsButton.isEnabled = enabled
        confirmTrustwordsButton.isEnabled = enabled
        resetDataButton.isEnabled = enabled
    }

}