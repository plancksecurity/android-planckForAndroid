package com.fsck.k9.pEp.ui.privacy.status

interface PEpStatusIdentityView {
    fun reportError(errorMessage: String?)
    fun enableButtons(enabled: Boolean)
}

interface PEpStatusPEpIdentityView : PEpStatusIdentityView {
    fun setLongTrustwords(newTrustwords: String)
    fun setShortTrustwords(newTrustwords: String)
}

interface PEpStatusPGPIdentityView : PEpStatusIdentityView {
    fun setLabelTexts(
            myselfAddress: String, myselfLabelText: CharSequence, partnerAddress: String, partnerLabelText: CharSequence)
    fun setFingerPrintTexts(myselfFprText: String, partnerFprText: String)
}