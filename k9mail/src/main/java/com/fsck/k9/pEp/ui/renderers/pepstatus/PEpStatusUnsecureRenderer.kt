package com.fsck.k9.pEp.ui.renderers.pepstatus

import com.fsck.k9.R
import com.fsck.k9.pEp.ui.privacy.status.PEpStatusRendererBuilder
import security.pEp.permissions.PermissionChecker
import javax.inject.Inject


class PEpStatusUnsecureRenderer @Inject constructor(
        permissionChecker: PermissionChecker
)
    : PEpStatusBaseRenderer(permissionChecker) {
    override fun getLayout() = R.layout.pep_recipient_row_only_text_and_reset

}