package security.planck.ui.passphrase.models

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import security.planck.ui.passphrase.models.AccountTextFieldState
import security.planck.ui.passphrase.models.PassphraseVerificationStatus
import security.planck.ui.passphrase.models.TextFieldState
import security.planck.ui.passphrase.models.TextFieldStateContract

sealed interface PassphraseMgmtState: PassphraseState {
    data class ChoosingAccountsToManage(
        val accountsUsingPassphrase: MutableList<SelectableItem<AccountUsesPassphrase>> = mutableStateListOf(),
    ) : PassphraseMgmtState {
        private val selectedCount: Int get() = accountsUsingPassphrase.count { it.selected }
        val actionMode: Boolean get() = selectedCount > 0
        val selectedAccounts: List<AccountUsesPassphrase>
            get() = accountsUsingPassphrase.filter { it.selected }.map { sel -> sel.data }
    }

    data class ManagingAccounts(
        val accounts: List<AccountUsesPassphrase>,
        val newPasswordState: TextFieldState = TextFieldState(),
        val newPasswordVerificationState: TextFieldState = TextFieldState(),
        val status: MutableState<PassphraseVerificationStatus> = mutableStateOf(
            PassphraseVerificationStatus.NONE
        ),
        val loading: MutableState<PassphraseLoading?> = mutableStateOf(null)
    ) : PassphraseMgmtState {
        val oldPasswordStates: SnapshotStateList<AccountTextFieldState> =
            mutableStateListOf<AccountTextFieldState>().also { list ->
                list.addAll(accounts.filter { it.usesPassphrase }
                    .map { acc -> AccountTextFieldState(acc.account) })
            }

        private val allTextFieldStates: List<TextFieldStateContract> get() = oldPasswordStates.toList() + newPasswordState + newPasswordVerificationState

        /**
         * Show error status
         */
        fun error(
            errorType: PassphraseVerificationStatus,
            accountsWithErrors: List<String>? = null
        ) {
            accountsWithErrors?.let {
                oldPasswordStates.forEach { state ->
                    if (accountsWithErrors.contains(state.email)) {
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
            if (status.value.isItemError) {
                clearItemErrorStatusIfPossible()
            }
        }

        private fun clearItemErrorStatusIfPossible() {
            val allTextFieldStates = this.allTextFieldStates
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
}

data class SelectableItem<T>(
    val data: T,
) {
    var selected: Boolean by mutableStateOf(false)
}

data class AccountUsesPassphrase(
    val account: String,
    val usesPassphrase: Boolean,
)
