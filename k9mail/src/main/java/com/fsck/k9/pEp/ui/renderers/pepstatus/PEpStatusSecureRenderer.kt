package com.fsck.k9.pEp.ui.renderers.pepstatus

import android.view.View
import android.widget.*
import androidx.appcompat.widget.PopupMenu
import butterknife.Bind
import butterknife.OnClick
import butterknife.OnLongClick
import com.fsck.k9.R
import com.fsck.k9.pEp.models.PEpIdentity
import com.fsck.k9.pEp.ui.privacy.status.PEpStatusPEpIdentityView
import com.fsck.k9.pEp.ui.privacy.status.PEpStatusIdentityPresenter
import com.fsck.k9.pEp.ui.tools.FeedbackTools
import javax.inject.Inject


class PEpStatusSecureRenderer @Inject constructor(
        identityPresenter: PEpStatusIdentityPresenter
)
    : PEpStatusHandshakeRenderer(identityPresenter), PEpStatusPEpIdentityView {

    override fun getLayout() = R.layout.pep_recipient_row_with_trustwords


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

    private fun doLoadTrustWords(identity: PEpIdentity) {
        identityPresenter.loadHandshakeData(identity)
    }

    @OnClick(R.id.trustwords)
    fun doShowFullTrustwords() {
        identityPresenter.changeTrustwordsSize(content, false)
    }


    @OnLongClick(R.id.trustwords)
    fun doShowShortTrustwords() : Boolean {
        identityPresenter.changeTrustwordsSize(content, true)
        return true
    }

    @OnClick(R.id.change_language)
    fun onChangeLanguageClicked() {
        showLanguageSelectionPopup(changeLanguageImage)
    }

    @OnClick(R.id.rejectHandshake)
    fun onRejectTrustwordsClicked() {
        identityPresenter.rejectHandshake(content)
        handshakeResultListener.onHandshakeResult(content, false)
    }

    @OnClick(R.id.confirmHandshake)
    fun onConfirmTrustwordsClicked() {
        identityPresenter.confirmHandshake(content)
        handshakeResultListener.onHandshakeResult(content, true)
    }

    private fun showLanguageSelectionPopup(v: View) {
        val popup = PopupMenu(context, v)
        val langArray = identityPresenter.getLanguageList()
        for(language in langArray) {
            popup.menu.add(language)
        }
        popup.setOnMenuItemClickListener {
            item -> identityPresenter.changeTrustwordsLanguage(content, item.title.toString())
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