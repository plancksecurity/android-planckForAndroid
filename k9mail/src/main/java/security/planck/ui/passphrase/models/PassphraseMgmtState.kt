package security.planck.ui.passphrase.models

sealed interface PassphraseMgmtState : PassphraseState {
    data class ChoosingAccountsToManage(
        val accounts: List<SelectableItem<String>>,
    ) : PassphraseMgmtState {
        private val selectedCount: Int get() = accounts.count { it.selected }
        val actionMode: Boolean get() = selectedCount > 0
    }

    data class ManagingAccounts(
        val accountsWithNoPassphrase: List<String>,
        val oldPasswordStates: List<AccountTextFieldState>,
        val newPasswordState: TextFieldState = TextFieldState(),
        val newPasswordVerificationState: TextFieldState = TextFieldState(),
        override val status: PassphraseVerificationStatus = PassphraseVerificationStatus.NONE,
    ) : PassphraseMgmtState, PassphraseStateWithStatus() {
        override fun copyWith(status: PassphraseVerificationStatus): PassphraseStateWithStatus {
            return copy(status = status)
        }
    }
}

data class SelectableItem<T>(
    val data: T,
    val selected: Boolean = false,
)

data class AccountUsesPassphrase(
    val account: String,
    val usesPassphrase: Boolean,
)
