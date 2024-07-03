package security.planck.ui.passphrase.models

sealed interface PassphraseState {
    object TooManyFailedAttempts : PassphraseState
    object Processing : PassphraseState
    data class WaitAfterFailedAttempt(val seconds: Int) : PassphraseState
    data class CreatingAccount(
        val newPasswordState: AccountTextFieldState,
        val newPasswordVerificationState: TextFieldState = TextFieldState(),
        override val status: PassphraseVerificationStatus = PassphraseVerificationStatus.NONE,
    ) : PassphraseState, PassphraseStateWithStatus() {
        override fun copyWith(status: PassphraseVerificationStatus): PassphraseStateWithStatus {
            return copy(status = status)
        }
    }

    object Success : PassphraseState
    data class CoreError(val error: Throwable?) : PassphraseState
}

sealed class PassphraseStateWithStatus : PassphraseState {
    open val status: PassphraseVerificationStatus = PassphraseVerificationStatus.NONE

    abstract fun copyWith(status: PassphraseVerificationStatus): PassphraseStateWithStatus
}
