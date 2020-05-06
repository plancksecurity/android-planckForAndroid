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
import com.fsck.k9.R
import com.fsck.k9.pEp.models.PEpIdentity
import com.fsck.k9.pEp.ui.PEpContactBadge
import com.fsck.k9.pEp.ui.privacy.status.PEpStatusIdentityView
import com.fsck.k9.pEp.ui.privacy.status.PEpStatusRendererAdapter
import com.fsck.k9.pEp.ui.privacy.status.PEpStatusIdentityPresenter
import com.pedrogomez.renderers.Renderer

abstract class PEpStatusBaseRenderer (
        val identityPresenter: PEpStatusIdentityPresenter
) : Renderer<PEpIdentity>(), PEpStatusIdentityView {

    lateinit var resetClickListener: PEpStatusRendererAdapter.ResetClickListener

    @Bind(R.id.tvUsername)
    lateinit var identityUserName: TextView

    @Bind(R.id.tvRatingStatus)
    lateinit var ratingStatusTV: TextView

    @Bind(R.id.status_explanation_text)
    lateinit var statusExplanationTV: TextView

    @Bind(R.id.status_badge)
    lateinit var badge: PEpContactBadge

    @Nullable @Bind(R.id.button_identity_key_reset)
    lateinit var resetDataButton: Button

    override fun inflate(inflater: LayoutInflater?, parent: ViewGroup?): View {
        val view : View = inflater!!.inflate(getLayout(), parent, false)
        ButterKnife.bind(this, view)
        return view
    }

    @LayoutRes abstract fun getLayout(): Int

    override fun render() {
        identityPresenter.initialize(content, badge, this)
    }

    override fun showRatingStatus(status: String) {
        ratingStatusTV.text = status
    }

    override fun showSuggestion(suggestion: String) {
        statusExplanationTV.text = suggestion
    }

    override fun showPartnerIdentity(partnerInfoToShow: CharSequence?) {
        identityUserName.text = partnerInfoToShow
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

    fun initialize(resetClickListener: PEpStatusRendererAdapter.ResetClickListener) {
        this.resetClickListener = resetClickListener
    }
}