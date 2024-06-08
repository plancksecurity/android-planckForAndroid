package security.planck.ui.passphrase.unlock

import androidx.lifecycle.viewModelScope
import com.fsck.k9.Account
import com.fsck.k9.planck.PlanckProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import foundation.pEp.jniadapter.Pair
import kotlinx.coroutines.launch
import security.planck.passphrase.PassphraseFormatValidator
import security.planck.passphrase.PassphraseRepository
import security.planck.ui.passphrase.PassphraseViewModel
import security.planck.ui.passphrase.models.AccountTextFieldState
import security.planck.ui.passphrase.models.PassphraseLoading
import security.planck.ui.passphrase.models.PassphraseState
import security.planck.ui.passphrase.models.PassphraseUnlockState
import security.planck.ui.passphrase.models.PassphraseVerificationStatus
import javax.inject.Inject

@HiltViewModel
class PassphraseUnlockViewModel @Inject constructor(
    private val planckProvider: PlanckProvider,
    private val passphraseRepository: PassphraseRepository,
    passphraseFormatValidator: PassphraseFormatValidator,
) : PassphraseViewModel(passphraseFormatValidator) {
    fun start() {
        loadAccountsForUnlocking()
    }

    private fun loadAccountsForUnlocking() {
        viewModelScope.launch {
            passphraseRepository.getAccountsWithPassPhrase().onFailure {
                stateLiveData.value = PassphraseState.CoreError(it)
            }.onSuccess { accountsWithPassphrase ->
                initializePasswordStatesIfNeeded(accountsWithPassphrase)
            }
        }
    }

    override fun error(
        errorType: PassphraseVerificationStatus,
        accountsWithErrors: List<String>?,
    ) {
        doWithUnlockingPassphrasesState { it.error(errorType, accountsWithErrors) }
    }

    override fun loading(loading: PassphraseLoading) {
        doWithUnlockingPassphrasesState { it.loading(loading) }
    }

    private fun doWithUnlockingPassphrasesState(block: (PassphraseUnlockState.UnlockingPassphrases) -> Unit) {
        val state = stateLiveData.value
        if (state is PassphraseUnlockState.UnlockingPassphrases) {
            block(state)
        }
    }

    private fun initializePasswordStatesIfNeeded(
        accountsUsingPassphrase: List<Account>,
    ) {
        val state = stateLiveData.value
        if (state is PassphraseUnlockState.UnlockingPassphrases) {
            state.initializePasswordStatesIfNeeded(accountsUsingPassphrase)
        } else {
            stateLiveData.value = PassphraseUnlockState.UnlockingPassphrases().also {
                it.initializePasswordStatesIfNeeded(accountsUsingPassphrase)
            }
        }
    }

    fun unlockKeysWithPassphrase(states: List<AccountTextFieldState>) {
        viewModelScope.launch {
            loading(PassphraseLoading.Processing)
            val keysWithPassphrase =
                states.map { state -> Pair(state.email, state.textState) }
            planckProvider.unlockKeysWithPassphrase(ArrayList(keysWithPassphrase)).onFailure {
                error(errorType = PassphraseVerificationStatus.CORE_ERROR)
            }.onSuccess { list ->
                if (list.isNullOrEmpty()) {
                    passphraseRepository.unlockPassphrase()
                    stateLiveData.value = PassphraseState.Success
                } else {
                    handleFailedVerificationAttempt(list)
                }
            }
        }
    }

    override fun clearErrorStatusIfNeeded() {
        doWithUnlockingPassphrasesState {
            it.clearErrorStatusIfNeeded()
        }
    }
}
