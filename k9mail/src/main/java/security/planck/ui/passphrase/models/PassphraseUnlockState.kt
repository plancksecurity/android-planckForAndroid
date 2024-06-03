package security.planck.ui.passphrase.models

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.fsck.k9.Account

sealed interface PassphraseUnlockState: PassphraseState {
    data class UnlockingPassphrases(
        val passwordStates: SnapshotStateList<AccountTextFieldState> = mutableStateListOf(),
        val status: MutableState<PassphraseVerificationStatus> = mutableStateOf(PassphraseVerificationStatus.NONE),
        val loading: MutableState<PassphraseLoading?> = mutableStateOf(PassphraseLoading.Processing)
    ) : PassphraseUnlockState {
        fun initializePasswordStatesIfNeeded(
            accountsUsingPassphrase: List<Account>,
        ) {
            loading.value = null
            if (passwordStates.isEmpty()) {
                passwordStates.addAll(accountsUsingPassphrase.map {
                    AccountTextFieldState(email = it.email, errorStatus = TextFieldStateContract.ErrorStatus.NONE)
                })
                status.value = PassphraseVerificationStatus.NONE
            }
        }

        /**
         * Show error status
         */
        fun error(
            errorType: PassphraseVerificationStatus,
            accountsWithErrors: List<String>? = null
        ) {
            accountsWithErrors?.let {
                passwordStates.forEachIndexed { index, state ->
                    if (accountsWithErrors.contains(state.email)) {
                        passwordStates[index].errorState = TextFieldStateContract.ErrorStatus.ERROR
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
            if (status.value.isItemError) {
                clearItemErrorStatusIfPossible()
            }
        }

        private fun clearItemErrorStatusIfPossible() {
            var success = 0
            for (state in passwordStates) {
                if (state.errorState == TextFieldStateContract.ErrorStatus.ERROR) {
                    return
                } else if (state.errorState == TextFieldStateContract.ErrorStatus.SUCCESS) {
                    success++
                }
            }
            this.status.value =
                if (success == passwordStates.size) PassphraseVerificationStatus.SUCCESS else PassphraseVerificationStatus.NONE
        }
    }
}
