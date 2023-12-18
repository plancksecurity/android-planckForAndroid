package security.planck.ui.removeaccount

sealed interface RemoveAccountState {
    object Idle : RemoveAccountState
    data class AccountNotAvailable(val accountDescription: String) : RemoveAccountState
    data class RemoveAccountConfirmation(val accountDescription: String) : RemoveAccountState
    object RemovingAccount : RemoveAccountState
    data class Done(val accountDescription: String) : RemoveAccountState
    data class Finish(val removed: Boolean) : RemoveAccountState
}