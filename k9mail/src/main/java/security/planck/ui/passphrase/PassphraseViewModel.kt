package security.planck.ui.passphrase

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import security.planck.passphrase.PassphraseFormatValidator
import security.planck.ui.passphrase.models.AccountTextFieldState
import security.planck.ui.passphrase.models.PassphraseState
import security.planck.ui.passphrase.models.PassphraseStateWithStatus
import security.planck.ui.passphrase.models.PassphraseVerificationStatus
import security.planck.ui.passphrase.models.TextFieldStateContract
import kotlin.math.pow

private const val RETRY_DELAY = 10000 // 10 seconds
private const val RETRY_WITH_DELAY_AFTER = 3
private const val MAX_ATTEMPTS_STOP_APP = 10

abstract class PassphraseViewModel(
    protected val passphraseFormatValidator: PassphraseFormatValidator,
) : ViewModel() {
    private var failedUnlockAttempts = 0
    private val delayStep get() = failedUnlockAttempts - RETRY_WITH_DELAY_AFTER

    protected val textFieldStates: MutableList<TextFieldStateContract> = mutableListOf()

    protected val stateLiveData: MutableLiveData<PassphraseState> =
        MutableLiveData(PassphraseState.Processing)
    val state: LiveData<PassphraseState> = stateLiveData

    fun updateAndValidateText(position: Int, text: String) {
        val errorStatus = updateAndValidateInput(position = position, text = text)
            .also { updateState() }
        updateStatusIfNeeded(errorStatus)
    }

    /**
     * Returns the [PassphraseVerificationStatus] according to the format error if any or null.
     */
    protected open fun updateAndValidateInput(
        position: Int,
        text: String
    ): PassphraseVerificationStatus? {
        return passphraseFormatValidator.validatePassphrase(text).let {
            textFieldStates[position] =
                textFieldStates[position].copyWith(newText = text, errorStatus = it)
            if (it.isError) {
                PassphraseVerificationStatus.WRONG_FORMAT
            } else null
        }
    }

    private suspend fun handleFailedVerificationAttempt(accountsWithError: List<String>) {
        failedUnlockAttempts++
        if (failedUnlockAttempts >= MAX_ATTEMPTS_STOP_APP) {
            stateLiveData.value = PassphraseState.TooManyFailedAttempts
        } else {
            if (failedUnlockAttempts >= RETRY_WITH_DELAY_AFTER) {
                val timeToWait = RETRY_DELAY * 2.0.pow(delayStep).toLong()
                stateLiveData.value = PassphraseState.WaitAfterFailedAttempt(timeToWait / 1000)
                delay(timeToWait)
            }
            error(
                errorType = PassphraseVerificationStatus.WRONG_PASSPHRASE,
                accountsWithErrors = accountsWithError
            )
        }
    }

    fun error(
        errorType: PassphraseVerificationStatus,
        accountsWithErrors: List<String>? = null,
    ) {
        setErrorsPerAccount(accountsWithErrors)
        updateState(errorType)
    }

    protected fun getCurrentStatusOrDefault(): PassphraseVerificationStatus {
        val state = stateLiveData.value
        return if (state is PassphraseStateWithStatus) {
            state.status
        } else PassphraseVerificationStatus.NONE
    }

    abstract fun calculateNewOverallStatus(): PassphraseVerificationStatus

    private fun setErrorsPerAccount(accountsWithErrors: List<String>?) {
        accountsWithErrors?.let {
            textFieldStates.forEachIndexed { index, state ->
                if (state is AccountTextFieldState && accountsWithErrors.contains(state.email)) {
                    textFieldStates[index] =
                        state.copy(errorStatus = TextFieldStateContract.ErrorStatus.ERROR)
                }
            }
        }
    }

    private fun updateStatusIfNeeded(newErrorStatus: PassphraseVerificationStatus?) {
        doWithState { state ->
            if (!state.status.isPersistentError) {
                if (newErrorStatus != null) {
                    if (state.status != newErrorStatus) {
                        updateState(newErrorStatus)
                    }
                } else {
                    calculateNewOverallStatus().let { newStatus ->
                        if (state.status != newStatus) {
                            updateState(newStatus)
                        }
                    }
                }
            }
        }
    }

    protected abstract fun updateState(errorType: PassphraseVerificationStatus? = null)

    private inline fun doWithState(block: (PassphraseStateWithStatus) -> Unit) {
        val state = stateLiveData.value
        if (state is PassphraseStateWithStatus) {
            block(state)
        }
    }

    protected open fun passphraseOperation(
        onSuccess: () -> Unit = {},
        bloc: suspend () -> Result<List<String>?>,
    ) {
        viewModelScope.launch {
            bloc().onFailure {
                error(PassphraseVerificationStatus.CORE_ERROR)
            }.onSuccess { list ->
                if (list.isNullOrEmpty()) {
                    onSuccess()
                    stateLiveData.value = PassphraseState.Success
                } else {
                    handleFailedVerificationAttempt(list)
                }
            }
        }
    }
}