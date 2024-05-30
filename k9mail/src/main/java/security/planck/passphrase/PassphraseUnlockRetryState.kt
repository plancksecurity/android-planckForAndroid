package security.planck.passphrase

sealed interface PassphraseUnlockRetryState {
    object Idle: PassphraseUnlockRetryState
    object FinishApp: PassphraseUnlockRetryState
    data class TimeToRetry(val accountsWithError: List<String>): PassphraseUnlockRetryState
    object TimeToStart: PassphraseUnlockRetryState
}