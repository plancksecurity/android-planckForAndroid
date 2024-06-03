package security.planck.ui.passphrase.models

sealed interface PassphraseState {
    object TooManyFailedAttempts : PassphraseState
    object Loading : PassphraseState
    object Dismiss : PassphraseState
    data class CoreError(val error: Throwable?) : PassphraseState
}

sealed interface PassphraseLoading {
    object Processing : PassphraseLoading
    data class WaitAfterFailedAttempt(val seconds: Long) : PassphraseLoading
}