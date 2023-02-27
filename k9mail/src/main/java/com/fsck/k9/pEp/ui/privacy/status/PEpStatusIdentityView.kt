package com.fsck.k9.pEp.ui.privacy.status

interface PEpStatusIdentityView {
    fun reportError(errorMessage: String?)
    fun enableButtons(enabled: Boolean)
    fun setLabelTexts(myselfLabelText: String, partnerLabelText: String)
    fun setFingerPrintTexts(myselfFprText: String, partnerFprText: String)
}

interface PEpStatusPEpIdentityView : PEpStatusIdentityView {
    fun setLongTrustwords(newTrustwords: String)
    fun setShortTrustwords(newTrustwords: String)
}
