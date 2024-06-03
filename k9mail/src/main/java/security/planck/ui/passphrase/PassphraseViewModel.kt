package security.planck.ui.passphrase

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.delay
import security.planck.passphrase.PassphraseFormatValidator
import security.planck.ui.passphrase.models.PassphraseLoading
import security.planck.ui.passphrase.models.PassphraseState
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

    protected val stateLiveData: MutableLiveData<PassphraseState> =
        MutableLiveData(PassphraseState.Loading)
    val state: LiveData<PassphraseState> = stateLiveData

    fun validateInput(textFieldState: TextFieldStateContract) {
        val errorState = passphraseFormatValidator.validatePassphrase(textFieldState.textState)
        textFieldState.errorState = errorState
        if (errorState == TextFieldStateContract.ErrorStatus.ERROR) {
            error(PassphraseVerificationStatus.WRONG_FORMAT)
        } else {
            clearErrorStatusIfNeeded()
        }
    }

    protected suspend fun handleFailedVerificationAttempt(accountsWithError: List<String>) {
        failedUnlockAttempts++
        if (failedUnlockAttempts >= MAX_ATTEMPTS_STOP_APP) {
            stateLiveData.value = PassphraseState.TooManyFailedAttempts
        } else {
            if (failedUnlockAttempts >= RETRY_WITH_DELAY_AFTER) {
                val timeToWait = RETRY_DELAY * 2.0.pow(delayStep).toLong()
                loading(PassphraseLoading.WaitAfterFailedAttempt(timeToWait / 1000))
                delay(timeToWait)
            }
            error(
                errorType = PassphraseVerificationStatus.WRONG_PASSPHRASE,
                accountsWithErrors = accountsWithError
            )
        }
    }

    abstract fun error(
        errorType: PassphraseVerificationStatus,
        accountsWithErrors: List<String>? = null
    )

    abstract fun loading(loading: PassphraseLoading)

    abstract fun clearErrorStatusIfNeeded()

}