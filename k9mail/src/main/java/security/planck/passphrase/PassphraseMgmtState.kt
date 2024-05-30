package security.planck.passphrase

import com.fsck.k9.Account

sealed interface PassphraseMgmtState {
    object Idle: PassphraseMgmtState
    object Loading: PassphraseMgmtState
    object Finish: PassphraseMgmtState
    data class CoreError(val error: Throwable?): PassphraseMgmtState
    data class ManagingAccounts(val accountsUsingPassphrase: List<AccountUsesPassphrase>): PassphraseMgmtState
    data class UnlockingPassphrases(val accountsUsingPassphrase: List<String>): PassphraseMgmtState
}

data class AccountUsesPassphrase(
    val account: Account,
    val usesPassphrase: Boolean,
)