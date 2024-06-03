package security.planck.ui.passphrase.manage

import androidx.lifecycle.viewModelScope
import com.fsck.k9.Preferences
import com.fsck.k9.planck.PlanckProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import foundation.pEp.jniadapter.Pair
import kotlinx.coroutines.launch
import security.planck.passphrase.PassphraseFormatValidator
import security.planck.ui.passphrase.PassphraseViewModel
import security.planck.ui.passphrase.models.AccountTextFieldState
import security.planck.ui.passphrase.models.AccountUsesPassphrase
import security.planck.ui.passphrase.models.PassphraseMgmtState
import security.planck.ui.passphrase.models.PassphraseState
import security.planck.ui.passphrase.models.PassphraseLoading
import security.planck.ui.passphrase.models.PassphraseVerificationStatus
import security.planck.ui.passphrase.models.SelectableItem
import security.planck.ui.passphrase.models.TextFieldStateContract
import javax.inject.Inject

@HiltViewModel
class PassphraseManagementViewModel @Inject constructor(
    private val planckProvider: PlanckProvider,
    private val preferences: Preferences,
    private val passphraseFormatValidator: PassphraseFormatValidator,
) : PassphraseViewModel() {

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
        val validationErrorState =
            passphraseFormatValidator.validatePassphrase(state.newPasswordState.textState)
        state.newPasswordState.errorState = validationErrorState
        val verificationErrorState = passphraseFormatValidator.verifyNewPassphrase(
            state.newPasswordState.textState,
            state.newPasswordVerificationState.textState
        )
        state.newPasswordVerificationState.errorState = verificationErrorState
        if (validationErrorState == TextFieldStateContract.ErrorStatus.ERROR) {
            error(PassphraseVerificationStatus.WRONG_FORMAT)
        } else if (verificationErrorState == TextFieldStateContract.ErrorStatus.ERROR) {
            error(PassphraseVerificationStatus.NEW_PASSPHRASE_DOES_NOT_MATCH)
        } else {
            clearErrorStatusIfNeeded()
        }
    }

    fun verifyNewPassphrase(state: PassphraseMgmtState.ManagingAccounts) {
        val errorState = passphraseFormatValidator.verifyNewPassphrase(
            state.newPasswordState.textState,
            state.newPasswordVerificationState.textState
        )
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
        val newPassphrase = state.newPasswordVerificationState
        val accountsToChange = state.accounts.map { account ->
            Pair(account.account, state.oldPasswordStates.find { it.email == account.account }?.textState.orEmpty())
        }.let { ArrayList(it) }
        viewModelScope.launch {
            planckProvider.managePassphrase(accountsToChange, newPassphrase.textState).onFailure {
                error(PassphraseVerificationStatus.CORE_ERROR)
            }.onSuccess {  list ->
                if (list.isNullOrEmpty()) {
                    stateLiveData.value = PassphraseState.Dismiss
                } else {
                    handleFailedVerificationAttempt(list)
                }
            }
        }
    }

    private fun loadAccountsForManagement() {
        viewModelScope.launch {
            stateLiveData.value = PassphraseState.Loading
            getAccountsUsingOrNotPassphrase().onFailure {
                stateLiveData.value = PassphraseState.CoreError(it)
            }.onSuccess { accounts ->
                stateLiveData.value = PassphraseMgmtState.ChoosingAccountsToManage().also {
                    it.accountsUsingPassphrase.addAll(accounts.map { account ->
                        SelectableItem(account)
                    })
                }
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
                    AccountUsesPassphrase(account.email, it)
                }
            )
        }
        return Result.success(accountsUsePassphrase)
    }

    override fun error(
        errorType: PassphraseVerificationStatus,
        accountsWithErrors: List<String>?,
    ) {
        doWithManagingPassphrasesState { it.error(errorType, accountsWithErrors) }
    }

    override fun loading(loading: PassphraseLoading) {
        doWithManagingPassphrasesState { it.loading(loading) }
    }

    private fun doWithManagingPassphrasesState(block: (PassphraseMgmtState.ManagingAccounts) -> Unit) {
        val state = stateLiveData.value
        if (state is PassphraseMgmtState.ManagingAccounts) {
            block(state)
        }
    }

}
