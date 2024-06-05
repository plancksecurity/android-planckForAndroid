package security.planck.ui.passphrase.models

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

sealed interface PassphraseState {
    object TooManyFailedAttempts : PassphraseState
    object Loading : PassphraseState
    object Success : PassphraseState
    data class CoreError(val error: Throwable?) : PassphraseState
}

sealed class PassphraseStateWithStatus : PassphraseState {
    val status: MutableState<PassphraseVerificationStatus> = mutableStateOf(
        PassphraseVerificationStatus.NONE
    )
    val loading: MutableState<PassphraseLoading?> = mutableStateOf(null)

    protected abstract val allTextFieldStates: List<TextFieldStateContract>

    /**
     * Show error status
     */
    fun error(
        errorType: PassphraseVerificationStatus,
        accountsWithErrors: List<String>? = null
    ) {
        accountsWithErrors?.let {
            allTextFieldStates.forEach { state ->
                if (state is AccountTextFieldState && accountsWithErrors.contains(state.email)) {
                    state.errorState = TextFieldStateContract.ErrorStatus.ERROR
                }
            }
        }
        this.status.value = errorType
        loading.value = null
    }

    /**
     * Show loading status
     */
    fun loading(loading: PassphraseLoading) {
        this.loading.value = loading
        status.value = PassphraseVerificationStatus.NONE
    }

    fun clearErrorStatusIfNeeded() {
        if (!status.value.isPersistentError) {
            clearItemErrorStatusIfPossible()
        }
    }

    private fun clearItemErrorStatusIfPossible() {
        var success = 0
        for (state in allTextFieldStates) {
            if (state.errorState == TextFieldStateContract.ErrorStatus.ERROR) {
                return
            } else if (state.errorState == TextFieldStateContract.ErrorStatus.SUCCESS) {
                success++
            }
        }
        this.status.value =
            if (success == allTextFieldStates.size) PassphraseVerificationStatus.SUCCESS else PassphraseVerificationStatus.NONE
    }
}

sealed interface PassphraseLoading {
    object Processing : PassphraseLoading
    data class WaitAfterFailedAttempt(val seconds: Long) : PassphraseLoading
}