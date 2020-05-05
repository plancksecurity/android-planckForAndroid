package com.fsck.k9.pEp.ui.renderers.pepstatus

import com.fsck.k9.pEp.ui.privacy.status.PEpStatusIdentityView
import com.fsck.k9.pEp.ui.privacy.status.PEpStatusRendererBuilder
import com.fsck.k9.pEp.ui.privacy.status.PEpStatusTrustwordsPresenter
import security.pEp.permissions.PermissionChecker

abstract class PEpStatusHandshakeRenderer constructor(
        permissionChecker: PermissionChecker,
        val trustwordsPresenter: PEpStatusTrustwordsPresenter
): PEpStatusBaseRenderer(permissionChecker), PEpStatusIdentityView {
    var myself: String = ""
        set(value) {
            field = value
            trustwordsPresenter.initialize(context, value, this)
        }

    lateinit var handshakeResultListener: PEpStatusRendererBuilder.HandshakeResultListener

    fun initialize(
            myself: String,
            resetClickListener: PEpStatusRendererBuilder.ResetClickListener,
            handshakeResultListener: PEpStatusRendererBuilder.HandshakeResultListener
    ) {
        this.myself = myself
        this.resetClickListener = resetClickListener
        this.handshakeResultListener = handshakeResultListener
    }
}