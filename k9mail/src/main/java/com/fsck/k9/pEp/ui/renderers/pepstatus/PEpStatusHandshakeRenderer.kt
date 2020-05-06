package com.fsck.k9.pEp.ui.renderers.pepstatus

import com.fsck.k9.pEp.ui.privacy.status.PEpStatusHandshakeView
import com.fsck.k9.pEp.ui.privacy.status.PEpStatusRendererAdapter
import com.fsck.k9.pEp.ui.privacy.status.PEpStatusIdentityPresenter

abstract class PEpStatusHandshakeRenderer constructor(
        identityPresenter: PEpStatusIdentityPresenter
): PEpStatusBaseRenderer(identityPresenter), PEpStatusHandshakeView {

    lateinit var handshakeResultListener: PEpStatusRendererAdapter.HandshakeResultListener
    lateinit var myself: String

    fun initialize(
            myself: String,
            resetClickListener: PEpStatusRendererAdapter.ResetClickListener,
            handshakeResultListener: PEpStatusRendererAdapter.HandshakeResultListener
    ) {
        initialize(resetClickListener)
        this.handshakeResultListener = handshakeResultListener
        this.myself = myself
        identityPresenter.initializeHandshakeView(myself, this)
    }
}