package com.fsck.k9.planck.ui.renderers.pepstatus

import com.fsck.k9.R
import com.fsck.k9.ui.contacts.ContactPictureLoader
import javax.inject.Inject


class PlanckStatusUnsecureRenderer @Inject constructor(contactsPictureLoader: ContactPictureLoader)
    : PlanckStatusBaseRenderer(contactsPictureLoader) {
    override fun getLayout() = R.layout.planck_recipient_row_only_text_and_reset

}