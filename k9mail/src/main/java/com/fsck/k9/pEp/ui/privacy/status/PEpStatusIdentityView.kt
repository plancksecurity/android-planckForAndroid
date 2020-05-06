package com.fsck.k9.pEp.ui.privacy.status

interface PEpStatusIdentityView {
    fun showRatingStatus(status: String)
    fun showSuggestion(suggestion: String)
    fun showPartnerIdentity(partnerInfoToShow: CharSequence?)
}

interface PEpStatusHandshakeView {
    fun reportError(errorMessage: String?)
    fun enableButtons(enabled: Boolean)
}

interface PEpStatusPEpIdentityView: PEpStatusHandshakeView  {
    fun setLongTrustwords(newTrustwords: String)
    fun setShortTrustwords(newTrustwords: String)
}

interface PEpStatusPGPIdentityView: PEpStatusHandshakeView {
    fun setLabelTexts(
            myselfAddress: String, myselfLabelText: CharSequence, partnerAddress: String, partnerLabelText: CharSequence)
    fun setFingerPrintTexts(myselfFprText: String, partnerFprText: String)
}