package com.fsck.k9.pEp.ui.renderers.pepstatus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import butterknife.Bind
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.OnLongClick
import com.fsck.k9.R
import com.fsck.k9.pEp.models.PEpIdentity
import com.fsck.k9.pEp.ui.privacy.status.PEpStatusRendererBuilder
import com.fsck.k9.pEp.ui.privacy.status.PEpStatusTrustwordsPresenter
import com.fsck.k9.pEp.ui.tools.FeedbackTools


class PEpStatusSecureRenderer(
        resetClickListener: PEpStatusRendererBuilder.ResetClickListener,
        private val myself: String
)
    : PEpStatusBaseRenderer(resetClickListener) {

    private lateinit var trustwordsPresenter: PEpStatusTrustwordsPresenter

    @Bind(R.id.trustwords)
    lateinit var trustwordsTv: TextView

    @Bind(R.id.change_language)
    lateinit var changeLanguageImage: ImageView

    @Bind(R.id.show_long_trustwords)
    lateinit var showFullTrustwordsButton: ImageView

    @Bind(R.id.wrongTrustwords)
    lateinit var rejectTrustwordsButton: Button

    @Bind(R.id.confirmTrustWords)
    lateinit var confirmTrustwordsButton: Button

    override fun inflate(inflater: LayoutInflater?, parent: ViewGroup?): View {
        val view : View = inflater!!.inflate(R.layout.pep_recipient_row_with_trustwords, parent, false)
        ButterKnife.bind(this, view)
        return view
    }

    override fun hookListeners(rootView: View?) {

    }

    override fun render() {
        super.render()
        doLoadTrustWords(content)
    }

    override fun setUpView(rootView: View?) {
        trustwordsPresenter = PEpStatusTrustwordsPresenter(myself,context,
                object : PEpStatusTrustwordsPresenter.PEpStatusTrustwordsView {
                    override fun setTrustwords(newTrustwords: String) {
                        trustwordsTv.text = newTrustwords
                        enableButtons(true)
                    }

                    override fun reportError(errorMessage: String?) {
                        enableButtons(true)
                        FeedbackTools.showShortFeedback(rootView, errorMessage)
                    }

                })
    }

    private fun doLoadTrustWords(identity: PEpIdentity) {
        enableButtons(false)
        trustwordsPresenter.loadTrustwords(identity)
    }

    @OnClick(R.id.show_long_trustwords)
    fun doShowFullTrustwords() {
        showFullTrustwordsButton.setVisibility(View.GONE)
        enableButtons(false)
        trustwordsPresenter.changeTrustwordsSize(content, false)
    }


    @OnLongClick(R.id.trustwords)
    fun doShowShortTrustwords() : Boolean {
        showFullTrustwordsButton.setVisibility(View.VISIBLE)
        enableButtons(false)
        trustwordsPresenter.changeTrustwordsSize(content, true)
        return true
    }

    @OnClick(R.id.change_language)
    fun onChangeLanguageClicked() {
        showLanguageSelectionPopup(changeLanguageImage)
    }

    private fun enableButtons(b: Boolean) {
        rejectTrustwordsButton.isEnabled = b
        confirmTrustwordsButton.isEnabled = b
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