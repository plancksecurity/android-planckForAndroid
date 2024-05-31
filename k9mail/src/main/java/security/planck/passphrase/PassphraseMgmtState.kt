package security.planck.passphrase

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
        val errorType: MutableState<PassphraseUnlockErrorType?> = mutableStateOf(null),
        val loading: MutableState<PassphraseUnlockLoading?> = mutableStateOf(null)
    ) : PassphraseMgmtState {
        fun initializePasswordStatesIfNeeded(
            accountsUsingPassphrase: List<Account>,
        ) {
            loading.value = null
            if (passwordStates.isEmpty()) {
                passwordStates.addAll(accountsUsingPassphrase.map {
                    TextFieldState(email = it.email, isError = true)
                })
                errorType.value = PassphraseUnlockErrorType.WRONG_FORMAT
            }
        }

        fun updateWithUnlockErrors(
            errorType: PassphraseUnlockErrorType,
            accountsWithErrors: List<String>? = null
        ) {
            accountsWithErrors?.let {
                passwordStates.forEachIndexed { index, state ->
                    if (accountsWithErrors.contains(state.email)) {
                        passwordStates[index].errorState = true
                    }
                }
            }
            this.errorType.value = errorType
            loading.value = null
        }

        fun updateWithUnlockLoading(loading: PassphraseUnlockLoading) {
            this.loading.value = loading
            errorType.value = null
        }

        fun resetNonFatalErrorIfNeeded() {
            val errorType = errorType.value
            if (errorType == PassphraseUnlockErrorType.WRONG_PASSPHRASE
                || (errorType == PassphraseUnlockErrorType.WRONG_FORMAT
                        && passwordStates.none { it.errorState })
            ) {
                this.errorType.value = null
            }
        }
    }
}

data class AccountUsesPassphrase(
    val account: Account,
    val usesPassphrase: Boolean,
)