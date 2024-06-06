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

        override val allTextFieldStates: List<TextFieldStateContract> get() = oldPasswordStates.toList() + newPasswordState + newPasswordVerificationState

        override fun clearItemErrorStatusIfPossible() {
            var success = 0
            var verificationSuccess = 0
            for (state in allTextFieldStates) {
                if (state.errorState == TextFieldStateContract.ErrorStatus.ERROR) {
                    return
                } else {
                    if (state == newPasswordState || state == newPasswordVerificationState) {
                        verificationSuccess++
                    }
                    if (state.errorState == TextFieldStateContract.ErrorStatus.SUCCESS) {
                        success++
                    }
                }
            }
            this.status.value =
                if (success == allTextFieldStates.size) PassphraseVerificationStatus.SUCCESS
                else if (success > 0 && success + verificationSuccess == allTextFieldStates.size) PassphraseVerificationStatus.SUCCESS_EMPTY
                else PassphraseVerificationStatus.NONE
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
