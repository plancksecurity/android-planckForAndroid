package security.planck.ui.passphrase.models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList

sealed interface PassphraseMgmtState : PassphraseState {
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
    ) : PassphraseMgmtState, PassphraseStateWithStatus() {
        val oldPasswordStates: SnapshotStateList<AccountTextFieldState> =
            mutableStateListOf<AccountTextFieldState>().also { list ->
                list.addAll(accounts.filter { it.usesPassphrase }
                    .map { acc -> AccountTextFieldState(acc.account) })
            }
        val accountsWithNoPassphrase: List<String> =
            accounts.filter { !it.usesPassphrase }.map { acc -> acc.account }

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
