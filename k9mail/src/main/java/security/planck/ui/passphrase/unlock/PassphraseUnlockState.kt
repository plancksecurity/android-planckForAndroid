package security.planck.ui.passphrase.unlock

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.fsck.k9.Account

sealed interface PassphraseUnlockState {
    object Dismiss : PassphraseUnlockState
    object TooManyFailedAttempts : PassphraseUnlockState

    data class UnlockingPassphrases(
        val passwordStates: SnapshotStateList<TextFieldState> = mutableStateListOf(),
        val status: MutableState<PassphraseUnlockStatus> = mutableStateOf(PassphraseUnlockStatus.NONE),
        val loading: MutableState<PassphraseUnlockLoading?> = mutableStateOf(PassphraseUnlockLoading.Processing)
    ) : PassphraseUnlockState {
        fun initializePasswordStatesIfNeeded(
            accountsUsingPassphrase: List<Account>,
        ) {
            loading.value = null
            if (passwordStates.isEmpty()) {
                passwordStates.addAll(accountsUsingPassphrase.map {
                    TextFieldState(email = it.email, errorStatus = TextFieldState.ErrorStatus.NONE)
                })
                status.value = PassphraseUnlockStatus.NONE
            }
        }

        /**
         * Show error status
         */
        fun error(
            errorType: PassphraseUnlockStatus,
            accountsWithErrors: List<String>? = null
        ) {
            accountsWithErrors?.let {
                passwordStates.forEachIndexed { index, state ->
                    if (accountsWithErrors.contains(state.email)) {
                        passwordStates[index].errorState = TextFieldState.ErrorStatus.ERROR
                    }
                }
            }
            this.status.value = errorType
            loading.value = null
        }

        /**
         * Show loading status
         */
        fun loading(loading: PassphraseUnlockLoading) {
            this.loading.value = loading
            status.value = PassphraseUnlockStatus.NONE
        }

        fun clearErrorStatusIfNeeded() {
            if (status.value.isItemError) {
                clearItemErrorStatusIfPossible()
            }
        }

        private fun clearItemErrorStatusIfPossible() {
            var success = 0
            for (state in passwordStates) {
                if (state.errorState == TextFieldState.ErrorStatus.ERROR) {
                    return
                } else if (state.errorState == TextFieldState.ErrorStatus.SUCCESS) {
                    success++
                }
            }
            this.status.value =
                if (success == passwordStates.size) PassphraseUnlockStatus.SUCCESS else PassphraseUnlockStatus.NONE
        }
    }
}
