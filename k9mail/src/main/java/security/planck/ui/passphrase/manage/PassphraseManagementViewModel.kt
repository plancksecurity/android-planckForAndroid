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
import security.planck.ui.passphrase.models.PassphraseVerificationStatus
import security.planck.ui.passphrase.models.SelectableItem
import security.planck.ui.passphrase.models.TextFieldState
import security.planck.ui.passphrase.models.TextFieldStateContract
import javax.inject.Inject

@HiltViewModel
class PassphraseManagementViewModel @Inject constructor(
    private val planckProvider: PlanckProvider,
    private val preferences: Preferences,
    passphraseFormatValidator: PassphraseFormatValidator,
) : PassphraseViewModel(passphraseFormatValidator) {

    private var accountsUsePassphrase: MutableList<AccountUsesPassphrase> = mutableListOf()

    private val selectableAccounts
        get() =
            accountsUsePassphrase.mapIndexed { i, acc ->
                SelectableItem(
                    acc.account,
                    i in selectedIndexes
                )
            }

    private val accountsWithNoPassphrase
        get() = selectedAccounts.filter { !it.usesPassphrase }
            .map { it.account }
    private val accountsWithPassphrase
        get() = selectedAccounts.filter { it.usesPassphrase }
            .map { it.account }

    private val newPasswordIndex: Int get() = textFieldStates.lastIndex - 1
    private val newPasswordVerificationIndex: Int get() = textFieldStates.lastIndex
    private val oldPasswordStates get() = textFieldStates.filterIsInstance<AccountTextFieldState>()
    private val newPasswordState: TextFieldState
        get() = textFieldStates[newPasswordIndex] as TextFieldState
    private val newPasswordVerificationState: TextFieldState
        get() = textFieldStates[newPasswordVerificationIndex] as TextFieldState

    private var selectedIndexes: MutableSet<Int> = mutableSetOf()
    private val selectedAccounts: List<AccountUsesPassphrase>
        get() = accountsUsePassphrase.filterIndexed { index, acc -> index in selectedIndexes }


    fun start() {
        loadAccountsForManagement()
    }

    fun accountClicked(index: Int) {
        doWithChoosingAccountsState { state ->
            if (state.actionMode) {
                if (selectedIndexes.contains(index)) {
                    selectedIndexes.remove(index)
                } else {
                    selectedIndexes.add(index)
                }
                stateLiveData.value = state.copy(
                    accounts = selectableAccounts
                )
            } else {
                selectedIndexes = mutableSetOf(index)
                goToManagePassphrase()
            }
        }
    }

    fun accountLongClicked(index: Int) {
        doWithChoosingAccountsState { state ->
            if (selectedIndexes.contains(index)) {
                selectedIndexes.remove(index)
            } else {
                selectedIndexes.add(index)
            }
            stateLiveData.value = state.copy(
                accounts = selectableAccounts
            )
        }
    }

    private fun doWithChoosingAccountsState(block: (PassphraseMgmtState.ChoosingAccountsToManage) -> Unit) {
        val state = stateLiveData.value
        if (state is PassphraseMgmtState.ChoosingAccountsToManage) {
            block(state)
        }
    }

    fun goToManagePassphrase() {
        textFieldStates.clear()
        textFieldStates.addAll(
            accountsWithPassphrase.map { AccountTextFieldState(it) }
        )
        textFieldStates.addAll(listOf(TextFieldState(), TextFieldState()))
        updateState()
    }

    override fun updateState(errorType: PassphraseVerificationStatus?) {
        stateLiveData.value = PassphraseMgmtState.ManagingAccounts(
            accountsWithNoPassphrase = accountsWithNoPassphrase,
            oldPasswordStates = oldPasswordStates,
            newPasswordState = newPasswordState,
            newPasswordVerificationState = newPasswordVerificationState,
            status = errorType ?: getCurrentStatusOrDefault()
        )
    }

    override fun updateAndValidateInput(
        position: Int,
        text: String
    ): PassphraseVerificationStatus? {
        return when (position) {
            newPasswordIndex -> {
                val npStatus = updateAndValidateNewPassphraseText(text)
                val npVerificationStatus = updateAndValidateNewPassphraseVerificationText(
                    text,
                    newPasswordVerificationState.text
                )
                if (npStatus == TextFieldStateContract.ErrorStatus.ERROR) {
                    PassphraseVerificationStatus.WRONG_FORMAT
                } else if (npVerificationStatus == TextFieldStateContract.ErrorStatus.ERROR) {
                    PassphraseVerificationStatus.NEW_PASSPHRASE_DOES_NOT_MATCH
                } else null
            }

            newPasswordVerificationIndex -> {
                val status =
                    updateAndValidateNewPassphraseVerificationText(newPasswordState.text, text)
                if (status == TextFieldStateContract.ErrorStatus.ERROR) {
                    PassphraseVerificationStatus.NEW_PASSPHRASE_DOES_NOT_MATCH
                } else {
                    null
                }
            }

            else -> {
                super.updateAndValidateInput(position, text)
            }
        }
    }

    private fun updateAndValidateNewPassphraseText(text: String): TextFieldStateContract.ErrorStatus {
        val newPasswordState = newPasswordState
        val status = passphraseFormatValidator.validatePassphrase(text)
        textFieldStates[newPasswordIndex] =
            newPasswordState.copyWith(newText = text, errorStatus = status)
        return status
    }

    private fun updateAndValidateNewPassphraseVerificationText(
        passphrase: String,
        verification: String
    ): TextFieldStateContract.ErrorStatus {
        val newPasswordState = newPasswordVerificationState
        val status = passphraseFormatValidator.verifyNewPassphrase(passphrase, verification)
        textFieldStates[newPasswordVerificationIndex] =
            newPasswordState.copyWith(newText = verification, errorStatus = status)
        return status
    }

    override fun calculateNewOverallStatus(): PassphraseVerificationStatus? {
        var success = 0
        var verificationSuccess = 0
        textFieldStates.forEachIndexed { index, state ->
            if (state.errorStatus == TextFieldStateContract.ErrorStatus.ERROR) {
                return null
            } else {
                if (index >= newPasswordIndex) {
                    verificationSuccess++
                }
                if (state.errorStatus == TextFieldStateContract.ErrorStatus.SUCCESS) {
                    success++
                }
            }
        }
        return if (success == textFieldStates.size) PassphraseVerificationStatus.SUCCESS
        else if (success > 0 && success + verificationSuccess == textFieldStates.size) PassphraseVerificationStatus.SUCCESS_EMPTY
        else PassphraseVerificationStatus.NONE
    }

    fun setNewPassphrase() {
        stateLiveData.value = PassphraseState.Processing
        val newPassphrase = newPasswordVerificationState
        val accountsToChange = selectedAccounts.map { account ->
            Pair(
                account.account,
                oldPasswordStates.find { it.email == account.account }?.text.orEmpty()
            )
        }.let { ArrayList(it) }
        passphraseOperation {
            planckProvider.managePassphrase(accountsToChange, newPassphrase.text)
        }
    }

    private fun loadAccountsForManagement() {
        viewModelScope.launch {
            stateLiveData.value = PassphraseState.Processing
            getAccountsUsingOrNotPassphrase().onFailure {
                stateLiveData.value = PassphraseState.CoreError(it)
            }.onSuccess { accounts ->
                accountsUsePassphrase = accounts.toMutableList()
                stateLiveData.value = PassphraseMgmtState.ChoosingAccountsToManage(
                    accounts.map { SelectableItem(it.account) }
                )
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
}
