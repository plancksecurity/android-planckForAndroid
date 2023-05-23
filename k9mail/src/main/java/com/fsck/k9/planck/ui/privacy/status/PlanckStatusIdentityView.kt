package com.fsck.k9.planck.ui.privacy.status

interface PlanckStatusIdentityView {
    fun reportError(errorMessage: String?)
    fun enableButtons(enabled: Boolean)
    fun setLabelTexts(myselfLabelText: String, partnerLabelText: String)
    fun setFingerPrintTexts(myselfFprText: String, partnerFprText: String)
}

interface PlanckStatusPlanckIdentityView : PlanckStatusIdentityView {
    fun setLongTrustwords(newTrustwords: String)
    fun setShortTrustwords(newTrustwords: String)
}
