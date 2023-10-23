package security.planck.ui.verifypartner

sealed class VerifyPartnerState {
    open val partner: String = ""

    object Idle : VerifyPartnerState()
    object LoadingHandshakeData : VerifyPartnerState()
    object ErrorLoadingMessage : VerifyPartnerState()
    object DeletedMessage : VerifyPartnerState()
    object ErrorGettingTrustwords : VerifyPartnerState()
    data class HandshakeReady(
        val myself: String,
        override val partner: String,
        val ownFpr: String,
        val partnerFpr: String,
        val trustwords: String,
        val shortTrustwords: Boolean,
        val allowChangeTrust: Boolean,
    ) : VerifyPartnerState()

    data class ConfirmTrust(override val partner: String) : VerifyPartnerState()
    data class ConfirmMistrust(override val partner: String) : VerifyPartnerState()
    data class TrustProgress(override val partner: String) : VerifyPartnerState()
    data class MistrustProgress(override val partner: String) : VerifyPartnerState()
    data class TrustDone(override val partner: String) : VerifyPartnerState()
    data class MistrustDone(override val partner: String) : VerifyPartnerState()
    data class ErrorTrusting(override val partner: String) : VerifyPartnerState()
    data class ErrorMistrusting(override val partner: String) : VerifyPartnerState()
}