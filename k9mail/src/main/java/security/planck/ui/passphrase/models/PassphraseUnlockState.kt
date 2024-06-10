package security.planck.ui.passphrase.models

sealed interface PassphraseUnlockState : PassphraseState {
    data class UnlockingPassphrases(
        val passwordStates: List<AccountTextFieldState>,
        override val status: PassphraseVerificationStatus = PassphraseVerificationStatus.NONE,
    ) : PassphraseUnlockState, PassphraseStateWithStatus() {
        override fun copyWith(status: PassphraseVerificationStatus): PassphraseStateWithStatus {
            return copy(status = status)
        }
    }
}
