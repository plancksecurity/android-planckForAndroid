package security.planck.ui.passphrase.unlock

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fsck.k9.Account
import com.fsck.k9.planck.PlanckProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import foundation.pEp.jniadapter.Pair
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import security.planck.passphrase.PassphraseFormatValidator
import security.planck.passphrase.PassphraseRepository
import security.planck.passphrase.extensions.isValidPassphrase
import security.planck.ui.passphrase.models.AccountTextFieldState
import security.planck.ui.passphrase.models.PassphraseVerificationStatus
import security.planck.ui.passphrase.models.TextFieldStateContract
import javax.inject.Inject
import kotlin.math.pow

private const val RETRY_DELAY = 10000 // 10 seconds
private const val RETRY_WITH_DELAY_AFTER = 3
private const val MAX_ATTEMPTS_STOP_APP = 10

@HiltViewModel
class PassphraseUnlockViewModel @Inject constructor(
    private val planckProvider: PlanckProvider,
    private val passphraseRepository: PassphraseRepository,
    private val passphraseFormatValidator: PassphraseFormatValidator,
) : ViewModel() {
    private val stateLiveData: MutableLiveData<PassphraseUnlockState> =
        MutableLiveData(PassphraseUnlockState.UnlockingPassphrases())
    val state: LiveData<PassphraseUnlockState> = stateLiveData

    private var failedUnlockAttempts = 0
    private val delayStep get() = failedUnlockAttempts - RETRY_WITH_DELAY_AFTER

    fun start() {
        loadAccountsForUnlocking()
    }

    private fun loadAccountsForUnlocking() {
        viewModelScope.launch {
            passphraseRepository.getAccountsWithPassPhrase().onFailure {
                error(errorType = PassphraseVerificationStatus.CORE_ERROR)
            }.onSuccess { accountsWithPassphrase ->
                initializePasswordStatesIfNeeded(accountsWithPassphrase)
            }
        }
    }

    private fun error(
        errorType: PassphraseVerificationStatus,
        accountsWithErrors: List<String>? = null
    ) {
        doWithUnlockingPassphrasesState { it.error(errorType, accountsWithErrors) }
    }

    private fun loading(loading: PassphraseUnlockLoading) {
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
        }
    }

    fun unlockKeysWithPassphrase(states: List<AccountTextFieldState>) {
        viewModelScope.launch {
            val keysWithPassphrase =
                states.map { state -> Pair(state.email, state.textState) }
            planckProvider.unlockKeysWithPassphrase(ArrayList(keysWithPassphrase)).onFailure {
                error(errorType = PassphraseVerificationStatus.CORE_ERROR)
            }.onSuccess { list ->
                if (list.isNullOrEmpty()) {
                    passphraseRepository.unlockPassphrase()
                    stateLiveData.value = PassphraseUnlockState.Dismiss
                } else {
                    handleFailedUnlockAttempt(list)
                }
            }
        }
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

    private fun clearErrorStatusIfNeeded() {
        doWithUnlockingPassphrasesState {
            it.clearErrorStatusIfNeeded()
        }
    }

    private suspend fun handleFailedUnlockAttempt(accountsWithError: List<String>) {
        failedUnlockAttempts++
        if (failedUnlockAttempts >= MAX_ATTEMPTS_STOP_APP) {
            stateLiveData.value = PassphraseUnlockState.TooManyFailedAttempts
        } else {
            if (failedUnlockAttempts >= RETRY_WITH_DELAY_AFTER) {
                val timeToWait = RETRY_DELAY * 2.0.pow(delayStep).toLong()
                loading(PassphraseUnlockLoading.WaitAfterFailedAttempt(timeToWait / 1000))
                delay(timeToWait)
            }
            error(
                errorType = PassphraseVerificationStatus.WRONG_PASSPHRASE,
                accountsWithErrors = accountsWithError
            )
        }
    }
}

sealed interface PassphraseUnlockLoading {
    object Processing : PassphraseUnlockLoading
    data class WaitAfterFailedAttempt(val seconds: Long) : PassphraseUnlockLoading
}