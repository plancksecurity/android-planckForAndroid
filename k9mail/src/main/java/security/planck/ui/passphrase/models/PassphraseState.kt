package security.planck.ui.passphrase.models

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

sealed interface PassphraseState {
    object TooManyFailedAttempts : PassphraseState
    object Processing : PassphraseState
    data class WaitAfterFailedAttempt(val seconds: Int) : PassphraseState
    //data class NewAccountPassphrase(val seconds: Int) : PassphraseState
    object Success : PassphraseState
    data class CoreError(val error: Throwable?) : PassphraseState
}

sealed class PassphraseStateWithStatus : PassphraseState {
    open val status: PassphraseVerificationStatus = PassphraseVerificationStatus.NONE

    abstract fun copyWith(status: PassphraseVerificationStatus): PassphraseStateWithStatus
}
