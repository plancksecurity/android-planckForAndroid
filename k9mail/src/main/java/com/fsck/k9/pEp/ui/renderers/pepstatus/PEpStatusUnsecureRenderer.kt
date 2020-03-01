package com.fsck.k9.pEp.ui.renderers.pepstatus

import com.fsck.k9.R
import com.fsck.k9.pEp.ui.privacy.status.PEpStatusRendererBuilder


class PEpStatusUnsecureRenderer(resetClickListener: PEpStatusRendererBuilder.ResetClickListener)
    : PEpStatusBaseRenderer(resetClickListener) {
    override fun getLayout() = R.layout.pep_recipient_row_only_text_and_reset

}