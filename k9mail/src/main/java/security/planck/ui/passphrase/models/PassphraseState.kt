package security.planck.ui.passphrase.models

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

sealed interface PassphraseState {
    object TooManyFailedAttempts : PassphraseState
    object Loading : PassphraseState
    object Dismiss : PassphraseState
    data class CoreError(val error: Throwable?) : PassphraseState
}

sealed class PassphraseStateWithStatus : PassphraseState {
    val status: MutableState<PassphraseVerificationStatus> = mutableStateOf(
        PassphraseVerificationStatus.NONE
    )
    val loading: MutableState<PassphraseLoading?> = mutableStateOf(null)
}

sealed interface PassphraseLoading {
    object Processing : PassphraseLoading
    data class WaitAfterFailedAttempt(val seconds: Long) : PassphraseLoading
}