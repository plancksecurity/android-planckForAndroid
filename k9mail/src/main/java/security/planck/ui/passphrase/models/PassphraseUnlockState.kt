package security.planck.ui.passphrase.models

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.fsck.k9.Account

sealed interface PassphraseUnlockState: PassphraseState {
    data class UnlockingPassphrases(
        val passwordStates: SnapshotStateList<AccountTextFieldState> = mutableStateListOf(),
    ) : PassphraseUnlockState, PassphraseStateWithStatus() {
        override val allTextFieldStates: List<TextFieldStateContract> get() = passwordStates
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
    }
}
