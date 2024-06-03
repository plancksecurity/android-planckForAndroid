package security.planck.ui.passphrase.manage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fsck.k9.Preferences
import com.fsck.k9.planck.PlanckProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import foundation.pEp.jniadapter.Pair
import kotlinx.coroutines.launch
import security.planck.passphrase.PassphraseFormatValidator
import security.planck.ui.passphrase.PassphraseDialogMode
import security.planck.ui.passphrase.models.PassphraseVerificationStatus
import security.planck.ui.passphrase.models.TextFieldStateContract
import javax.inject.Inject

@HiltViewModel
class PassphraseManagementViewModel @Inject constructor(
    private val planckProvider: PlanckProvider,
    private val preferences: Preferences,
    private val passphraseFormatValidator: PassphraseFormatValidator,
) : ViewModel() {
    private val stateLiveData: MutableLiveData<PassphraseMgmtState> =
        MutableLiveData(PassphraseMgmtState.Loading)
    val state: LiveData<PassphraseMgmtState> = stateLiveData
    lateinit var mode: PassphraseDialogMode

    fun start() {
        loadAccountsForManagement()
    }

    fun accountClicked(account: SelectableItem<AccountUsesPassphrase>) {
        val state = stateLiveData.value
        if (state is PassphraseMgmtState.ChoosingAccountsToManage) {
            if (state.actionMode) {
                account.selected = !account.selected
            } else {
                selectAccountsToManagePassphrase(listOf(account.data))
            }
        }
    }

    fun accountLongClicked(account: SelectableItem<AccountUsesPassphrase>) {
        val state = stateLiveData.value
        if (state is PassphraseMgmtState.ChoosingAccountsToManage) {
            account.selected = !account.selected
        }
    }

    fun selectAccountsToManagePassphrase(accounts: List<AccountUsesPassphrase>) {
        stateLiveData.value = PassphraseMgmtState.ManagingAccounts(accounts = accounts)
    }

    fun validateInput(textFieldState: TextFieldStateContract) {
        val errorState = passphraseFormatValidator.validatePassphrase(textFieldState.textState)
        textFieldState.errorState = errorState
        if (errorState == TextFieldStateContract.ErrorStatus.ERROR) {
            error(PassphraseVerificationStatus.WRONG_FORMAT)
        } else {
            clearErrorStatusIfNeeded()
        }
    }

    fun validateNewPassphrase(state: PassphraseMgmtState.ManagingAccounts) {
        val validationErrorState = passphraseFormatValidator.validatePassphrase(state.newPasswordState.textState)
        state.newPasswordVerificationState.errorState = validationErrorState
        val verificationErrorState = passphraseFormatValidator.verifyNewPassphrase(state.newPasswordState.textState, state.newPasswordVerificationState.textState)
        state.newPasswordVerificationState.errorState = verificationErrorState
        if (verificationErrorState == TextFieldStateContract.ErrorStatus.ERROR) {
            error(PassphraseVerificationStatus.NEW_PASSPHRASE_DOES_NOT_MATCH)
        } else if (validationErrorState == TextFieldStateContract.ErrorStatus.ERROR) {
            error(PassphraseVerificationStatus.WRONG_FORMAT)
        } else {
            clearErrorStatusIfNeeded()
        }
    }

    fun verifyNewPassphrase(state: PassphraseMgmtState.ManagingAccounts) {
        val errorState = passphraseFormatValidator.verifyNewPassphrase(state.newPasswordState.textState, state.newPasswordVerificationState.textState)
        state.newPasswordVerificationState.errorState = errorState
        if (errorState == TextFieldStateContract.ErrorStatus.ERROR) {
            error(PassphraseVerificationStatus.NEW_PASSPHRASE_DOES_NOT_MATCH)
        } else {
            clearErrorStatusIfNeeded()
        }
    }



    fun goBackToChoosingAccounts() {
        loadAccountsForManagement()
        //stateLiveData.value = PassphraseMgmtState.ChoosingAccountsToManage()
    }

    private fun clearErrorStatusIfNeeded() {
        doWithManagingPassphrasesState {
            it.clearErrorStatusIfNeeded()
        }
    }

    fun setNewPassphrase(state: PassphraseMgmtState.ManagingAccounts) {
        val accountsWithOldPassphrase = state.oldPasswordStates
        val newPassphrase = state.newPasswordVerificationState
        val accountsToChange = accountsWithOldPassphrase
            .filter { it.email != newPassphrase.textState }
            .map { inputState -> Pair(inputState.email, inputState.textState) }
            .let { ArrayList(it) }
            .apply {
                addAll(state.accounts.filter { !it.usesPassphrase }.map { acc -> Pair(acc.account, "") })
            }
        viewModelScope.launch {
            planckProvider.managePassphrase(accountsToChange, newPassphrase.textState).onFailure {
                error(PassphraseVerificationStatus.CORE_ERROR)
            }.onSuccess {
                stateLiveData.value = PassphraseMgmtState.Dismiss
            }
        }
    }

    private fun loadAccountsForManagement() {
        viewModelScope.launch {
            stateLiveData.value = PassphraseMgmtState.Loading
            getAccountsUsingOrNotPassphrase().onFailure {
                stateLiveData.value = PassphraseMgmtState.CoreError(it)
            }.onSuccess { accounts ->
                stateLiveData.value = PassphraseMgmtState.ChoosingAccountsToManage().also { it.accountsUsingPassphrase.addAll(accounts.map { account -> SelectableItem(account) }) }
            }
        }
    }

    private suspend fun getAccountsUsingOrNotPassphrase(): Result<List<AccountUsesPassphrase>> {
        val accountsUsePassphrase = preferences.availableAccounts.map { account ->
            planckProvider.hasPassphrase(account.email).fold(
                onFailure = {
                    return Result.failure(it)
                },
                onSuccess = {
                    AccountUsesPassphrase(account.email, true)
                }
            )
        }
        return Result.success(accountsUsePassphrase)
    }

    private fun error(
        errorType: PassphraseVerificationStatus,
        accountsWithErrors: List<String>? = null
    ) {
        doWithManagingPassphrasesState { it.error(errorType, accountsWithErrors) }
    }
    private fun doWithManagingPassphrasesState(block: (PassphraseMgmtState.ManagingAccounts) -> Unit) {
        val state = stateLiveData.value
        if (state is PassphraseMgmtState.ManagingAccounts) {
            block(state)
        }
    }

}
