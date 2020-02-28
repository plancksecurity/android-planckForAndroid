package com.fsck.k9.pEp.ui.renderers.pepstatus

import android.view.View
import android.widget.*
import androidx.appcompat.widget.PopupMenu
import butterknife.Bind
import butterknife.OnClick
import butterknife.OnLongClick
import com.fsck.k9.R
import com.fsck.k9.pEp.models.PEpIdentity
import com.fsck.k9.pEp.ui.privacy.status.PEpStatusRendererBuilder
import com.fsck.k9.pEp.ui.privacy.status.PEpStatusTrustwordsPresenter
import com.fsck.k9.pEp.ui.tools.FeedbackTools


class PEpStatusSecureRenderer(
        resetClickListener: PEpStatusRendererBuilder.ResetClickListener,
        private val handshakeResultListener: PEpStatusRendererBuilder.HandshakeResultListener,
        private val myself: String
)
    : PEpStatusBaseRenderer(resetClickListener) {

    override fun getLayout() = R.layout.pep_recipient_row_with_trustwords


    private lateinit var trustwordsPresenter: PEpStatusTrustwordsPresenter

    @Bind(R.id.trustwords)
    lateinit var trustwordsTv: TextSwitcher

    @Bind(R.id.change_language)
    lateinit var changeLanguageImage: ImageView

    @Bind(R.id.rejectHandshake)
    lateinit var rejectTrustwordsButton: Button

    @Bind(R.id.confirmHandshake)
    lateinit var confirmTrustwordsButton: Button

    override fun hookListeners(rootView: View?) {
        trustwordsTv.setInAnimation(context, android.R.anim.fade_in)
        trustwordsTv.setOutAnimation(context, android.R.anim.fade_out)
    }

    override fun render() {
        super.render()
        doLoadTrustWords(content)
    }

    override fun setUpView(rootView: View?) {
        trustwordsPresenter = PEpStatusTrustwordsPresenter(myself,context,
                object : PEpStatusTrustwordsPresenter.PEpStatusPEpIdentityView {

                    override fun setLongTrustwords(newTrustwords: String) {
                        trustwordsTv.setText(newTrustwords)
                        enableButtons(true)
                    }

                    override fun setShortTrustwords(newTrustwords: String) {
                        trustwordsTv.setText(context.getString(R.string.ellipsized_text, newTrustwords))
                        enableButtons(true)
                    }

                    override fun reportError(errorMessage: String?) {
                        enableButtons(true)
                        FeedbackTools.showShortFeedback(rootView, errorMessage)
                    }

                    override fun enableButtons(enabled: Boolean) {
                        rejectTrustwordsButton.isEnabled = enabled
                        confirmTrustwordsButton.isEnabled = enabled
                    }

                })
    }

    private fun doLoadTrustWords(identity: PEpIdentity) {
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

}