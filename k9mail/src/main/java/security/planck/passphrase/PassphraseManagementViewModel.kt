package security.planck.passphrase

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.planck.DispatcherProvider
import com.fsck.k9.planck.PlanckProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import foundation.pEp.jniadapter.Pair
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.pow

private const val ACCEPTED_SYMBOLS = """@\$!%*+\-_#?&\[\]\{\}\(\)\.:;,<>~"'\\/"""
private const val PASSPHRASE_REGEX =
    """^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[$ACCEPTED_SYMBOLS])[A-Za-z\d$ACCEPTED_SYMBOLS]{12,}$"""
private const val RETRY_DELAY = 10000 // 10 seconds
private const val RETRY_WITH_DELAY_AFTER = 3
private const val MAX_ATTEMPTS_STOP_APP = 5

@HiltViewModel
class PassphraseManagementViewModel @Inject constructor(
    private val planckProvider: PlanckProvider,
    private val preferences: Preferences,
    private val k9: K9,
    private val passphraseRepository: PassphraseRepository,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {
    private val stateLiveData: MutableLiveData<PassphraseMgmtState> =
        MutableLiveData(PassphraseMgmtState.Idle)
    val state: LiveData<PassphraseMgmtState> = stateLiveData
    lateinit var mode: PassphraseDialogMode

    //val passwordStates = mutableStateListOf<TextFieldState>()

    private var failedUnlockAttempts = 0
    private val delayStep get() = failedUnlockAttempts - RETRY_WITH_DELAY_AFTER

    fun start(
        mode: PassphraseDialogMode,
    ) {
        this.mode = mode
        when (mode) {
            PassphraseDialogMode.MANAGE -> loadAccountsForManagement()
            PassphraseDialogMode.UNLOCK -> loadAccountsForUnlocking()
        }
    }

    private fun loadAccountsForManagement() {
        //viewModelScope.launch {
        //    stateLiveData.value = PassphraseMgmtState.Loading
        //    getAccountsUsingOrNotPassphrase().onFailure {
        //        stateLiveData.value = PassphraseMgmtState.CoreError(it)
        //    }.onSuccess {
        //        stateLiveData.value = PassphraseMgmtState.ManagingAccounts(it)
        //    }
        //}
    }

    private fun loadAccountsForUnlocking() {
        viewModelScope.launch {
            stateLiveData.value = PassphraseMgmtState.UnlockingPassphrases().also { updateWithUnlockLoading(PassphraseUnlockLoading.Processing) }
            passphraseRepository.getAccountsWithPassPhrase().onFailure {
                updateWithUnlockErrors(errorType = PassphraseUnlockErrorType.CORE_ERROR)
            }.onSuccess { accountsWithPassphrase ->
                initializePasswordStatesIfNeeded(accountsWithPassphrase)
            }
        }
    }

    private fun updateWithUnlockErrors(errorType: PassphraseUnlockErrorType, accountsWithErrors: List<String>? = null) {
        val state = stateLiveData.value
        if (state is PassphraseMgmtState.UnlockingPassphrases) {
            state.updateWithUnlockErrors(errorType, accountsWithErrors)
        }
    }

    private fun updateWithUnlockLoading(loading: PassphraseUnlockLoading) {
        val state = stateLiveData.value
        if (state is PassphraseMgmtState.UnlockingPassphrases) {
            state.updateWithUnlockLoading(loading)
        }
    }

    private fun initializePasswordStatesIfNeeded(
        accountsUsingPassphrase: List<Account>,
    ) {
        val state = stateLiveData.value
        if (state is PassphraseMgmtState.UnlockingPassphrases) {
            state.initializePasswordStatesIfNeeded(accountsUsingPassphrase)
        }
    }

    private suspend fun getAccountsUsingOrNotPassphrase(): Result<List<AccountUsesPassphrase>> {
        val accountsUsePassphrase = preferences.availableAccounts.map { account ->
            planckProvider.hasPassphrase(account.email).fold(
                onFailure = {
                    return Result.failure(it)
                },
                onSuccess = {
                    AccountUsesPassphrase(account, it)
                }
            )
        }
        return Result.success(accountsUsePassphrase)
    }

    fun unlockKeysWithPassphrase(states: List<TextFieldState>) {
        Timber.e("EFA-601 UNLOCKING KEYS WITH PASSPHRASE: ${states.map { "${it.email} : ${it.textState}" }}")
        viewModelScope.launch {
            //updateWithUnlockLoading(PassphraseUnlockLoading.Processing) // too short, flickering...
            val keysWithPassphrase =
                states.map { state -> Pair(state.email, state.textState) }
            planckProvider.unlockKeysWithPassphrase(ArrayList(keysWithPassphrase)).onFailure {
                Timber.e("EFA-601 RESULT ERROR: ${it.stackTraceToString()}")
                //handleFailedUnlockAttempt()
                updateWithUnlockErrors(errorType = PassphraseUnlockErrorType.CORE_ERROR) // we should notify of an error...? // should we really retry here...?
            }.onSuccess { list ->
                Timber.e("EFA-601 RESULT: $list")
                if (list.isNullOrEmpty()) {
                    passphraseRepository.unlockPassphrase()
                    stateLiveData.value = PassphraseMgmtState.Dismiss
                } else {
                    handleFailedUnlockAttempt(list)
                    //stateLiveData.value = PassphraseMgmtState.UnlockingPassphrases()
                }
            }
        }
    }

    fun validateInput(textFieldState: TextFieldState) {
        val validPassphrase = textFieldState.textState.isValidPassphrase()
        textFieldState.errorState = !validPassphrase
        resetPassphraseUnlockNonFatalErrorIfNeeded()
    }

    private fun resetPassphraseUnlockNonFatalErrorIfNeeded() {
        val state = stateLiveData.value
        if (state is PassphraseMgmtState.UnlockingPassphrases) {
            state.resetNonFatalErrorIfNeeded()
        }
    }

    private fun String.isValidPassphrase(): Boolean {
        return matches(PASSPHRASE_REGEX.toRegex())
    }

    private suspend fun handleFailedUnlockAttempt(accountsWithError: List<String>) {
        failedUnlockAttempts ++
        if (failedUnlockAttempts >= MAX_ATTEMPTS_STOP_APP) {
            stateLiveData.value = PassphraseMgmtState.TooManyFailedAttempts
        } else {
            if (failedUnlockAttempts >= RETRY_WITH_DELAY_AFTER) {
                val timeToWait = RETRY_DELAY * 2.0.pow(delayStep).toLong()
                updateWithUnlockLoading(PassphraseUnlockLoading.WaitAfterFailedAttempt(timeToWait/1000))
                delay(timeToWait)
            }
            updateWithUnlockErrors(errorType = PassphraseUnlockErrorType.WRONG_PASSPHRASE, accountsWithErrors = accountsWithError)
        }
    }
}

data class TextFieldState(
    val email: String,
    private val text: String = "",
    private val isError: Boolean = false,
) {
    var textState by mutableStateOf(text)
    var errorState by mutableStateOf(isError)
}

enum class PassphraseUnlockErrorType {
    WRONG_FORMAT, WRONG_PASSPHRASE, CORE_ERROR,
}

sealed interface PassphraseUnlockLoading {
    object Processing: PassphraseUnlockLoading
    data class WaitAfterFailedAttempt(val seconds: Long): PassphraseUnlockLoading
}