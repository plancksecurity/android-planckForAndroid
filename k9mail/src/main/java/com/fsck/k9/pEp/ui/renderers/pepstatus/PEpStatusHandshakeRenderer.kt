package com.fsck.k9.pEp.ui.renderers.pepstatus

import com.fsck.k9.pEp.ui.privacy.status.PEpStatusIdentityView
import com.fsck.k9.pEp.ui.privacy.status.PEpStatusRendererAdapter
import com.fsck.k9.pEp.ui.privacy.status.PEpStatusTrustwordsPresenter
import security.pEp.permissions.PermissionChecker

abstract class PEpStatusHandshakeRenderer constructor(
        permissionChecker: PermissionChecker,
        val trustwordsPresenter: PEpStatusTrustwordsPresenter
): PEpStatusBaseRenderer(permissionChecker), PEpStatusIdentityView {
    lateinit var myself: String

    lateinit var handshakeResultListener: PEpStatusRendererAdapter.HandshakeResultListener

    fun initialize(
            myself: String,
            resetClickListener: PEpStatusRendererAdapter.ResetClickListener,
            handshakeResultListener: PEpStatusRendererAdapter.HandshakeResultListener
    ) {
        initialize(resetClickListener)
        this.myself = myself
        this.handshakeResultListener = handshakeResultListener
        trustwordsPresenter.initialize(myself, this)
    }
}