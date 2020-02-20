package com.fsck.k9.pEp.ui.renderers.pepstatus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import butterknife.Bind
import butterknife.ButterKnife
import com.fsck.k9.R
import butterknife.OnClick



class PEpStatusSecureRenderer : PEpStatusBaseRenderer() {

    @Bind(R.id.button_identity_key_reset)
    lateinit var resetDataButton: Button

    override fun inflate(inflater: LayoutInflater?, parent: ViewGroup?): View {
        val view : View = inflater!!.inflate(R.layout.pep_recipient_row_with_trustwords, parent, false)
        ButterKnife.bind(this, view)
        return view
    }

    override fun hookListeners(rootView: View?) {

    }

    override fun setUpView(rootView: View?) {

    }

}