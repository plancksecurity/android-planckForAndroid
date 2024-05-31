package security.planck.ui.passphrase

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.fsck.k9.Account

sealed interface PassphraseMgmtState {
    object Idle : PassphraseMgmtState
    object Loading : PassphraseMgmtState
    object Dismiss : PassphraseMgmtState
    object TooManyFailedAttempts : PassphraseMgmtState
    data class CoreError(val error: Throwable?) : PassphraseMgmtState
    data class ManagingAccounts(val accountsUsingPassphrase: List<AccountUsesPassphrase>) :
        PassphraseMgmtState

    data class UnlockingPassphrases(
        val passwordStates: SnapshotStateList<TextFieldState> = mutableStateListOf(),
        val status: MutableState<PassphraseUnlockStatus> = mutableStateOf(PassphraseUnlockStatus.NONE),
        val loading: MutableState<PassphraseUnlockLoading?> = mutableStateOf(null)
    ) : PassphraseMgmtState {
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

        fun updateWithUnlockErrors(
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

        fun updateWithUnlockLoading(loading: PassphraseUnlockLoading) {
            this.loading.value = loading
            status.value = PassphraseUnlockStatus.NONE
        }

        fun updateNonFatalErrorIfNeeded(wrongPassphraseFormat: Boolean) {
            val errorType = status.value
            if (wrongPassphraseFormat) {
                this.status.value = PassphraseUnlockStatus.WRONG_FORMAT
            } else {
                if (errorType.isItemError) {
                    clearItemErrorStatusIfPossible()
                }
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

data class AccountUsesPassphrase(
    val account: Account,
    val usesPassphrase: Boolean,
)