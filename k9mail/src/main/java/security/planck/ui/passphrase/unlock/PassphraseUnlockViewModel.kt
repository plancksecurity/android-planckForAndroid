package security.planck.ui.passphrase.unlock

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import security.planck.passphrase.PassphraseRepository
import javax.inject.Inject
import kotlin.math.pow

private const val ACCEPTED_SYMBOLS = """@\$!%*+\-_#?&\[\]\{\}\(\)\.:;,<>~"'\\/"""
private const val PASSPHRASE_REGEX =
    """^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[$ACCEPTED_SYMBOLS])[A-Za-z\d$ACCEPTED_SYMBOLS]{12,}$"""
private const val RETRY_DELAY = 10000 // 10 seconds
private const val RETRY_WITH_DELAY_AFTER = 3
private const val MAX_ATTEMPTS_STOP_APP = 10

@HiltViewModel
class PassphraseUnlockViewModel @Inject constructor(
    private val planckProvider: PlanckProvider,
    private val passphraseRepository: PassphraseRepository,
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
                error(errorType = PassphraseUnlockStatus.CORE_ERROR)
            }.onSuccess { accountsWithPassphrase ->
                initializePasswordStatesIfNeeded(accountsWithPassphrase)
            }
        }
    }

    private fun error(
        errorType: PassphraseUnlockStatus,
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

    fun unlockKeysWithPassphrase(states: List<TextFieldState>) {
        viewModelScope.launch {
            val keysWithPassphrase =
                states.map { state -> Pair(state.email, state.textState) }
            planckProvider.unlockKeysWithPassphrase(ArrayList(keysWithPassphrase)).onFailure {
                error(errorType = PassphraseUnlockStatus.CORE_ERROR)
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

    fun validateInput(textFieldState: TextFieldState) {
        val validPassphraseFormat = textFieldState.textState.isValidPassphrase()
        textFieldState.errorState =
            when {
                validPassphraseFormat -> {
                    TextFieldState.ErrorStatus.SUCCESS.also {
                        clearErrorStatusIfNeeded()
                    }
                }

                textFieldState.textState.isEmpty() -> {
                    TextFieldState.ErrorStatus.NONE.also {
                        clearErrorStatusIfNeeded()
                    }
                }

                else -> {
                    error(PassphraseUnlockStatus.WRONG_FORMAT)
                    TextFieldState.ErrorStatus.ERROR
                }
            }
    }

    private fun clearErrorStatusIfNeeded() {
        doWithUnlockingPassphrasesState {
            it.clearErrorStatusIfNeeded()
        }
    }

    private fun String.isValidPassphrase(): Boolean {
        return matches(PASSPHRASE_REGEX.toRegex())
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
                errorType = PassphraseUnlockStatus.WRONG_PASSPHRASE,
                accountsWithErrors = accountsWithError
            )
        }
    }
}

data class TextFieldState(
    val email: String,
    private val text: String = "",
    private val errorStatus: ErrorStatus = ErrorStatus.NONE,
) {
    var textState by mutableStateOf(text)
    var errorState by mutableStateOf(errorStatus)

    enum class ErrorStatus {
        NONE, ERROR, SUCCESS
    }
}

enum class PassphraseUnlockStatus {
    WRONG_FORMAT, WRONG_PASSPHRASE, CORE_ERROR, NONE, SUCCESS;

    val isError: Boolean get() = this != NONE && this != SUCCESS

    /**
     * itemError is not fatal, and it's an error per account/mail address.
     */
    val isItemError: Boolean get() = this == WRONG_FORMAT || this == WRONG_PASSPHRASE
}

sealed interface PassphraseUnlockLoading {
    object Processing : PassphraseUnlockLoading
    data class WaitAfterFailedAttempt(val seconds: Long) : PassphraseUnlockLoading
}