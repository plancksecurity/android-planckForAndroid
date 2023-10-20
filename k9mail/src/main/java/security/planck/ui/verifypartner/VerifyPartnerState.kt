package security.planck.ui.verifypartner

sealed interface VerifyPartnerState {
    object Idle : VerifyPartnerState
    object LoadingHandshakeData : VerifyPartnerState
    object ErrorLoadingMessage : VerifyPartnerState
    object ErrorGettingTrustwords : VerifyPartnerState
    data class HandshakeReady(
        val ownFpr: String,
        val partnerFpr: String,
        val trustwords: String,
    ) : VerifyPartnerState

    object ConfirmTrust : VerifyPartnerState
    object ConfirmMistrust : VerifyPartnerState
    object TrustProgress : VerifyPartnerState
    object MistrustProgress : VerifyPartnerState
    object TrustDone : VerifyPartnerState
    object MistrustDone : VerifyPartnerState
    object ErrorTrusting : VerifyPartnerState
    object ErrorMistrusting : VerifyPartnerState
}