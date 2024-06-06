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
        val newPasswordState: TextFieldState = TextFieldState(errorStatus = TextFieldStateContract.ErrorStatus.SUCCESS),
        val newPasswordVerificationState: TextFieldState = TextFieldState(errorStatus = TextFieldStateContract.ErrorStatus.SUCCESS),
    ) : PassphraseMgmtState, PassphraseStateWithStatus() {
        val oldPasswordStates: SnapshotStateList<AccountTextFieldState> =
            mutableStateListOf<AccountTextFieldState>().also { list ->
                list.addAll(accounts.filter { it.usesPassphrase }
                    .map { acc -> AccountTextFieldState(acc.account) })
            }
        val accountsWithNoPassphrase: List<String> =
            accounts.filter { !it.usesPassphrase }.map { acc -> acc.account }

        override val allTextFieldStates: List<TextFieldStateContract> get() = oldPasswordStates.toList() + newPasswordState + newPasswordVerificationState
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
